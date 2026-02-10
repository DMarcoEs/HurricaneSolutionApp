package com.example.hurricansolutionapp

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.graphics.BitmapFactory
import java.util.Locale

private const val IVA_RATE = 0.16

fun generarPdfCotizacion(
    context: Context,
    cotizacion: Cotizacion
): File? {

    val pageWidth = 595
    val pageHeight = 842

    val pdfDocument = PdfDocument()
    val margin = 32f
    val headerBarHeight = 90f
    val bottomBarHeight = 45f  // Ajustado para la imagen del footer

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun capitalizeWords(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }

    fun cropTransparent(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        var minX = width; var minY = height; var maxX = -1; var maxY = -1
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                val alpha = (pixels[offset + x] ushr 24) and 0xFF
                if (alpha > 0) {
                    if (x < minX) minX = x; if (x > maxX) maxX = x
                    if (y < minY) minY = y; if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < 0 || maxY < 0) return bitmap
        return Bitmap.createBitmap(bitmap, minX, minY, maxX - minX + 1, maxY - minY + 1)
    }

    fun wrapText(text: String, maxWidth: Float, paint: Paint, maxLines: Int = 2): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()
        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine.clear().append(candidate)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    if (lines.size >= maxLines) return lines
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty() && lines.size < maxLines) lines.add(currentLine.toString())
        return lines
    }

    fun drawHeader(canvas: Canvas) {
        try {
            val options = BitmapFactory.Options().apply { inScaled = false }
            val rawLogo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_header_new, options)
            val croppedLogo = cropTransparent(rawLogo)
            val targetHeight = 45f
            val aspectRatio = croppedLogo.width.toFloat() / croppedLogo.height.toFloat()
            val bandCenterY = headerBarHeight / 2f
            val logoTop = bandCenterY - targetHeight / 2f
            val destRect = RectF(margin, logoTop, margin + targetHeight * aspectRatio, logoTop + targetHeight)
            canvas.drawBitmap(croppedLogo, null, destRect, null)

            val rawUsa = BitmapFactory.decodeResource(context.resources, R.drawable.made_in_usa, options)
            val usaCropped = cropTransparent(rawUsa)
            val usaTargetHeight = 38f
            val usaAspect = usaCropped.width.toFloat() / usaCropped.height.toFloat()
            val usaTop = bandCenterY - usaTargetHeight / 2f - 3f
            val usaRight = pageWidth.toFloat() - margin
            val usaRect = RectF(usaRight - usaTargetHeight * usaAspect, usaTop, usaRight, usaTop + usaTargetHeight)
            val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
            val usaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
            canvas.drawBitmap(usaCropped, null, usaRect, usaPaint)
            // Título eliminado según solicitud
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun drawFolioBox(canvas: Canvas, titleY: Float) {
        val folioTexto = "Folio: ${cotizacion.folio}"
        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
        paint.letterSpacing = 0f
        val textWidth = paint.measureText(folioTexto)
        val boxPaddingH = 6f; val boxHeight = 18f
        val boxTop = titleY - boxHeight / 2f
        val boxRight = pageWidth.toFloat() - margin
        val boxLeft = boxRight - textWidth - boxPaddingH * 2
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE }
        canvas.drawRect(boxLeft, boxTop, boxRight, boxTop + boxHeight, boxPaint)
        boxPaint.style = Paint.Style.STROKE; boxPaint.color = Color.DKGRAY; boxPaint.strokeWidth = 0.8f
        canvas.drawRect(boxLeft, boxTop, boxRight, boxTop + boxHeight, boxPaint)
        paint.color = Color.BLACK
        canvas.drawText(folioTexto, boxLeft + boxPaddingH, boxTop + boxHeight / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
    }

    // FOOTER USANDO IMAGEN
    fun drawFooter(canvas: Canvas) {
        try {
            val options = BitmapFactory.Options().apply { inScaled = false }
            val footerImg = BitmapFactory.decodeResource(context.resources, R.drawable.footer_nuevo, options)
            if (footerImg != null) {
                val imgWidth = footerImg.width.toFloat()
                val imgHeight = footerImg.height.toFloat()
                val imgAspectRatio = imgWidth / imgHeight

                val finalWidth = pageWidth.toFloat()
                val finalHeight = finalWidth / imgAspectRatio

                val footerTop = pageHeight.toFloat() - finalHeight

                val destRect = RectF(0f, footerTop, finalWidth, pageHeight.toFloat())
                canvas.drawBitmap(footerImg, null, destRect, null)
            }
        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "Error cargando footer: ${e.message}")
        }
    }

    val productosSeleccionados: List<TipoProducto> = run {
        val lista = cotizacion.productos.ifEmpty { listOf(cotizacion.producto) }
        lista.distinct().sortedBy { p -> when (p) { TipoProducto.HS875 -> 0; TipoProducto.HS1250 -> 1; TipoProducto.HS1500 -> 2; TipoProducto.PERSONALIZADO -> 3 } }
    }

    val zonaGeografica = cotizacion.zonaGeografica
    val tableLeft = margin; val tableRight = pageWidth.toFloat() - margin
    val headerHeight = 20f; val bodyTextSize = 9f; val cellPadding = 4f; val cellLineHeight = 12f
    val colNumeroW = 25f; val colAreaW = 130f; val colAreaTotalW = 50f; val colMontajeW = 65f; val colAdecuacionesW = 65f
    val priceColumnsCount = productosSeleccionados.size.coerceAtLeast(1)
    val colPricesTotalW = tableRight - tableLeft - (colNumeroW + colAreaW + colAreaTotalW + colMontajeW + colAdecuacionesW)
    val colPriceW = colPricesTotalW / priceColumnsCount
    val startPreciosX = tableLeft + colNumeroW + colAreaW + colAreaTotalW + colMontajeW + colAdecuacionesW

    val colorHS875 = Color.parseColor("#D9D9D9")
    val colorHS1250 = Color.parseColor("#898989")
    val colorHS1500 = Color.parseColor("#494949")

    data class RowLayout(
        val zona: String,
        val linesArea: List<String>,
        val linesAreaTotal: List<String>,
        val linesMontaje: List<String>,
        val linesAdecuaciones: List<String>,
        val linesPreciosPorProducto: List<List<String>>,
        val height: Float
    )

    paint.textSize = bodyTextSize
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.letterSpacing = 0f
    val filas = mutableListOf<RowLayout>()

    cotizacion.ventanas.forEach { v ->
        val txtArea = capitalizeWords(v.descripcion)
        val txtZona = capitalizeWords(v.zona.trim().ifBlank { "General" })
        val txtAreaTotal = "%.2f".format(v.areaM2)
        val txtMontaje = capitalizeWords(v.tipoMontaje.ifBlank { cotizacion.tipoMontaje })
        val txtAdecuaciones = if (v.adecuacion == "No" || v.adecuacion.isBlank()) "Ninguna" else capitalizeWords(v.adecuacion)
        val preciosPorProducto = productosSeleccionados.map { "$ " + "%,.2f".format(v.subtotalPorProducto(it, zonaGeografica)) }
        val linesArea = wrapText(txtArea, colAreaW - cellPadding * 2, paint)
        val linesAreaTotal = wrapText(txtAreaTotal, colAreaTotalW - cellPadding * 2, paint)
        val linesMontaje = wrapText(txtMontaje, colMontajeW - cellPadding * 2, paint)
        val linesAdecuaciones = wrapText(txtAdecuaciones, colAdecuacionesW - cellPadding * 2, paint)
        val linesPreciosPorProducto = preciosPorProducto.map { listOf(it) }
        val maxLines = listOf(linesArea.size, linesAreaTotal.size, linesMontaje.size, linesAdecuaciones.size, linesPreciosPorProducto.maxOfOrNull { it.size } ?: 1).maxOrNull() ?: 1
        filas.add(RowLayout(txtZona, linesArea, linesAreaTotal, linesMontaje, linesAdecuaciones, linesPreciosPorProducto, maxLines * cellLineHeight + 4f))
    }

    val zonasEnOrden = filas.map { it.zona }.distinct()
    val filasAgrupadasPorZona = zonasEnOrden.associateWith { zona -> filas.filter { it.zona == zona } }

    val condLineCount = 14; val condLineHeight = 7.5f
    val condicionesMinBlock = 16f + condLineCount * condLineHeight + 45f
    val resumenBlockHeight = 14f * 6  // Reducido
    val extraSpaceNeededLastPage = condicionesMinBlock + resumenBlockHeight + 16f

    val zonaTitleHeight = 14f

    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    drawHeader(canvas)
    paint.color = Color.BLACK
    paint.textSize = 16f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    paint.letterSpacing = 0f
    val tituloY = 105f
    canvas.drawText("COTIZACIÓN DE PROYECTO", pageWidth / 2f, tituloY, paint)
    drawFolioBox(canvas, tituloY)
    paint.textAlign = Paint.Align.LEFT

    var y = 140f
    val leftX = margin; val leftBlockWidth = 270f; val rightBlockWidth = 230f
    val rightX = pageWidth.toFloat() - margin - rightBlockWidth

    fun drawInfoRowCentered(canvasRef: Canvas, x: Float, yTop: Float, label: String, value: String, blockW: Float): Float {
        val labelW = 100f; val gapW = 8f; val paddingX = 6f; val lineHeightText = 10f
        val valueX = x + labelW + gapW; val valueW = blockW - labelW - gapW
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.letterSpacing = 0f
        val capitalizedValue = capitalizeWords(value)
        val wrappedLines = wrapText(capitalizedValue, valueW - paddingX * 2, paint, maxLines = 2)
        val numLines = wrappedLines.size.coerceIn(1, 2)
        val rowH = (numLines * lineHeightText + 8f).coerceAtLeast(20f)
        val yBottom = yTop + rowH

        paint.style = Paint.Style.FILL; paint.color = Color.BLACK
        canvasRef.drawRect(x, yTop, x + labelW, yBottom, paint)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 8f
        paint.textAlign = Paint.Align.LEFT
        canvasRef.drawText(label, x + paddingX, yTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        canvasRef.drawRect(valueX, yTop, valueX + valueW, yBottom, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; paint.strokeWidth = 0.6f
        canvasRef.drawRect(valueX, yTop, valueX + valueW, yBottom, paint)

        paint.style = Paint.Style.FILL; paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.textSize = 8f
        val totalTextHeight = numLines * lineHeightText
        var textY = yTop + (rowH - totalTextHeight) / 2f + lineHeightText - paint.descent()
        wrappedLines.forEach { line -> canvasRef.drawText(line, valueX + paddingX, textY, paint); textY += lineHeightText }
        return yBottom + 6f
    }

    var leftY = y; var rightY = y
    leftY = drawInfoRowCentered(canvas, leftX, leftY, "Nombre del Cliente:", cotizacion.clienteNombre, leftBlockWidth)
    if (cotizacion.ciudad.isNotBlank()) leftY = drawInfoRowCentered(canvas, leftX, leftY, "Ciudad:", cotizacion.ciudad, leftBlockWidth)

    val ubicacionCompleta = cotizacion.ubicacion
    val ciudadCompleta = cotizacion.ciudad

    var restoDireccion = ubicacionCompleta
    if (ciudadCompleta.isNotBlank() && ubicacionCompleta.contains(ciudadCompleta)) {
        restoDireccion = ubicacionCompleta.replace(ciudadCompleta, "").trim()
        restoDireccion = restoDireccion.trimStart(',').trim()
    }

    val partesRestantes = restoDireccion.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val colonia = partesRestantes.getOrNull(0) ?: ""
    val calleNumero = partesRestantes.getOrNull(1) ?: ""

    if (colonia.isNotBlank()) leftY = drawInfoRowCentered(canvas, leftX, leftY, "Colonia:", colonia, leftBlockWidth)
    if (calleNumero.isNotBlank()) leftY = drawInfoRowCentered(canvas, leftX, leftY, "Calle y Número:", calleNumero, leftBlockWidth)

    val metrajeFinal = cotizacion.ventanas.sumOf { it.areaM2 }
    rightY = drawInfoRowCentered(canvas, rightX, rightY, "Especialista:", cotizacion.especialista, rightBlockWidth)
    rightY = drawInfoRowCentered(canvas, rightX, rightY, "Fecha:", cotizacion.fecha, rightBlockWidth)
    rightY = drawInfoRowCentered(canvas, rightX, rightY, "Metraje Total:", "%.2f M²".format(metrajeFinal), rightBlockWidth)
    y = maxOf(leftY, rightY) + 12f

    fun getHSHeaderColor(producto: TipoProducto): Int {
        return when (producto) {
            TipoProducto.HS875 -> colorHS875
            TipoProducto.HS1250 -> colorHS1250
            TipoProducto.HS1500 -> colorHS1500
            TipoProducto.PERSONALIZADO -> Color.BLACK
        }
    }

    fun getHSTextColor(producto: TipoProducto): Int {
        return when (producto) {
            TipoProducto.HS875 -> Color.BLACK
            TipoProducto.HS1250 -> Color.BLACK
            TipoProducto.HS1500 -> Color.WHITE
            TipoProducto.PERSONALIZADO -> Color.WHITE
        }
    }

    fun drawTableHeader(startY: Float): Float {
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.letterSpacing = 0f
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER
        var x = tableLeft

        fun drawHeaderCell(text: String, width: Float) {
            canvas.drawRect(x, startY, x + width, startY + headerHeight, paint)
            paint.color = Color.WHITE
            canvas.drawText(text, x + width / 2f, startY + headerHeight / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
            paint.color = Color.BLACK; x += width
        }

        drawHeaderCell("", colNumeroW)
        drawHeaderCell("Área a proteger", colAreaW)
        drawHeaderCell("Área total", colAreaTotalW)
        drawHeaderCell("Tipo de montaje", colMontajeW)
        drawHeaderCell("Adecuaciones", colAdecuacionesW)

        productosSeleccionados.forEach { producto ->
            val hsLabel = when (producto) {
                TipoProducto.HS875 -> "HS-875"
                TipoProducto.HS1250 -> "HS-1250"
                TipoProducto.HS1500 -> "HS-1500"
                TipoProducto.PERSONALIZADO -> "Pers."
            }
            val bgColor = getHSHeaderColor(producto)
            val textColor = getHSTextColor(producto)

            paint.style = Paint.Style.FILL
            paint.color = bgColor
            canvas.drawRect(x, startY, x + colPriceW, startY + headerHeight, paint)

            paint.color = textColor
            paint.textSize = 10f
            canvas.drawText(hsLabel, x + colPriceW / 2f, startY + headerHeight / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

            paint.color = Color.BLACK
            x += colPriceW
        }
        return startY + headerHeight
    }

    fun drawZonaTitle(zona: String, startY: Float): Float {
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.textSize = bodyTextSize
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.LEFT
        paint.letterSpacing = 0f

        canvas.drawText(zona.uppercase(), tableLeft, startY + zonaTitleHeight - 4f, paint)

        return startY + zonaTitleHeight
    }

    y = drawTableHeader(y)

    var filaIndexGlobal = 0
    val totalFilas = filas.size

    filasAgrupadasPorZona.forEach { (zona, filasDeZona) ->
        val footerTop = pageHeight.toFloat() - bottomBarHeight

        if (y + zonaTitleHeight > footerTop - 10f) {
            drawFooter(canvas); pdfDocument.finishPage(page); pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo); canvas = page.canvas
            drawHeader(canvas); y = headerBarHeight + 20f; y = drawTableHeader(y)
        }

        y = drawZonaTitle(zona, y)

        filasDeZona.forEachIndexed { indexEnZona, fila ->
            val rowH = fila.height
            val isLastGlobal = filaIndexGlobal == totalFilas - 1

            if (isLastGlobal) {
                if (y + rowH + extraSpaceNeededLastPage > footerTop - 10f) {
                    drawFooter(canvas); pdfDocument.finishPage(page); pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo); canvas = page.canvas
                    drawHeader(canvas); y = headerBarHeight + 20f; y = drawTableHeader(y)
                    if (indexEnZona > 0) {
                        y = drawZonaTitle(zona + " (cont.)", y)
                    } else {
                        y = drawZonaTitle(zona, y)
                    }
                }
            } else {
                if (y + rowH > footerTop - 10f) {
                    drawFooter(canvas); pdfDocument.finishPage(page); pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo); canvas = page.canvas
                    drawHeader(canvas); y = headerBarHeight + 20f; y = drawTableHeader(y)
                    y = drawZonaTitle(zona + " (cont.)", y)
                }
            }

            val rowTop = y; val rowBottom = y + rowH
            paint.style = Paint.Style.FILL; paint.color = Color.WHITE
            canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, paint)

            // Bordes punteados gris sutil para la fila
            paint.style = Paint.Style.STROKE
            paint.color = Color.parseColor("#9CA3AF")  // Gris sutil
            paint.strokeWidth = 0.8f
            paint.pathEffect = DashPathEffect(floatArrayOf(2f, 4f), 0f)
            canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, paint)
            paint.pathEffect = null

            var xCol = tableLeft
            fun drawCellBorder(left: Float, width: Float) {
                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#9CA3AF")  // Gris sutil
                paint.pathEffect = DashPathEffect(floatArrayOf(2f, 4f), 0f)
                canvas.drawLine(left + width, rowTop, left + width, rowBottom, paint)
                paint.pathEffect = null
                // Restaurar color negro para el texto
                paint.style = Paint.Style.FILL
                paint.color = Color.BLACK
            }

            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.textSize = bodyTextSize
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            paint.letterSpacing = 0f
            paint.textAlign = Paint.Align.CENTER

            val numCenterY = rowTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText("#${filaIndexGlobal + 1}", xCol + colNumeroW / 2f, numCenterY, paint)
            drawCellBorder(xCol, colNumeroW); xCol += colNumeroW

            paint.textAlign = Paint.Align.CENTER
            val areaCenterX = xCol + colAreaW / 2f
            if (fila.linesArea.size == 1) {
                canvas.drawText(fila.linesArea.first(), areaCenterX, numCenterY, paint)
            } else {
                var textY = rowTop + cellPadding + cellLineHeight - paint.descent()
                fila.linesArea.forEach { line ->
                    canvas.drawText(line, areaCenterX, textY, paint)
                    textY += cellLineHeight
                }
            }
            drawCellBorder(xCol, colAreaW); xCol += colAreaW

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(fila.linesAreaTotal.firstOrNull() ?: "", xCol + colAreaTotalW / 2f, numCenterY, paint)
            drawCellBorder(xCol, colAreaTotalW); xCol += colAreaTotalW
            canvas.drawText(fila.linesMontaje.firstOrNull() ?: "", xCol + colMontajeW / 2f, numCenterY, paint)
            drawCellBorder(xCol, colMontajeW); xCol += colMontajeW
            canvas.drawText(fila.linesAdecuaciones.firstOrNull() ?: "", xCol + colAdecuacionesW / 2f, numCenterY, paint)
            drawCellBorder(xCol, colAdecuacionesW); xCol += colAdecuacionesW

            fila.linesPreciosPorProducto.forEachIndexed { idx, lines ->
                canvas.drawText(lines.firstOrNull() ?: "", xCol + colPriceW / 2f, numCenterY, paint)
                drawCellBorder(xCol, colPriceW); xCol += colPriceW
            }
            y = rowBottom
            filaIndexGlobal++
        }

        // Espacio entre zonas
        y += 6f
    }

    val areaTotal = cotizacion.ventanas.sumOf { it.areaM2 }
    fun descuentoPorM2(producto: TipoProducto): Double = when (producto) {
        TipoProducto.HS875 -> cotizacion.descuentoHS875
        TipoProducto.HS1250 -> cotizacion.descuentoHS1250
        TipoProducto.HS1500 -> cotizacion.descuentoHS1500
        TipoProducto.PERSONALIZADO -> 0.0
    }

    val totalesPorProducto = productosSeleccionados.associateWith { producto ->
        cotizacion.ventanas.sumOf { it.subtotalPorProducto(producto, zonaGeografica) }
    }
    val descuentosPorcentaje = productosSeleccionados.associateWith { producto ->
        val subtotalProducto = totalesPorProducto[producto] ?: 0.0
        val descM2 = descuentoPorM2(producto)
        if (subtotalProducto == 0.0) 0.0 else (areaTotal * descM2 / subtotalProducto) * 100.0
    }
    val preciosFinalesPorProducto = productosSeleccionados.associateWith { producto ->
        val subtotalProducto = totalesPorProducto[producto] ?: 0.0
        val descM2 = descuentoPorM2(producto)
        (subtotalProducto - areaTotal * descM2).coerceAtLeast(0.0)
    }

    val labelWidth = 160f
    val valueColumnWidth = colPriceW
    val rowHeightResumen = 14f
    val resumenTextSize = 9f

    val footerTop = pageHeight.toFloat() - bottomBarHeight
    val margenSobreFooter = 8f
    val resumenTopDesdeAbajo = pageHeight.toFloat() - bottomBarHeight - margenSobreFooter - rowHeightResumen * 5
    val resumenTop = maxOf(y + 15f, resumenTopDesdeAbajo)
    val resumenBottom = resumenTop + rowHeightResumen * 5

    var filaTop = resumenTop
    val resumenLeft = startPreciosX - labelWidth

    fun drawResumenRowSimple(
        label: String,
        getTextForCol: (Int, TipoProducto) -> String,
        isBold: Boolean = false,
        drawLineAbove: Boolean = false
    ) {
        val filaBottom = filaTop + rowHeightResumen

        if (drawLineAbove) {
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            paint.strokeWidth = 1f
            canvas.drawLine(resumenLeft, filaTop, resumenLeft + labelWidth + (valueColumnWidth * productosSeleccionados.size), filaTop, paint)
        }

        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = resumenTextSize
        paint.typeface = if (isBold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.letterSpacing = 0f
        canvas.drawText(label, resumenLeft + 2f, filaTop + rowHeightResumen / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        productosSeleccionados.forEachIndexed { index, producto ->
            val left = resumenLeft + labelWidth + index * valueColumnWidth
            val rightEdge = left + valueColumnWidth - 8f  // Margen derecho

            // Formato contabilidad: alineado a la derecha
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = resumenTextSize
            paint.typeface = if (isBold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            canvas.drawText(getTextForCol(index, producto), rightEdge, filaTop + rowHeightResumen / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
        }

        filaTop = filaBottom
    }

    drawResumenRowSimple("Subtotal USD", { _, p -> "$ " + "%,.2f".format(totalesPorProducto[p] ?: 0.0) }, isBold = false)
    drawResumenRowSimple("Descuento", { _, p -> String.format("%.2f%%", descuentosPorcentaje[p] ?: 0.0) }, isBold = false)
    drawResumenRowSimple("Subtotal con Descuento USD", { _, p -> "$ " + "%,.2f".format(preciosFinalesPorProducto[p] ?: 0.0) }, isBold = false)
    drawResumenRowSimple("IVA", { _, p -> "$ " + "%,.2f".format((preciosFinalesPorProducto[p] ?: 0.0) * IVA_RATE) }, isBold = false)
    drawResumenRowSimple("Precio Final USD", { _, p -> "$ " + "%,.2f".format((preciosFinalesPorProducto[p] ?: 0.0) * (1.0 + IVA_RATE)) },
        isBold = true, drawLineAbove = true)

    val condicionesLeft = margin
    val condicionesRight = resumenLeft - 10f
    val condicionesAvailableWidth = condicionesRight - condicionesLeft
    // Aumentar altura disponible para condiciones comerciales (15% más)
    val condicionesAvailableHeight = (resumenBottom - resumenTop) * 1.15f

    try {
        val options = BitmapFactory.Options().apply { inScaled = false }
        val condicionesImg = BitmapFactory.decodeResource(context.resources, R.drawable.condiciones_comerciales, options)
        if (condicionesImg != null) {
            val imgWidth = condicionesImg.width.toFloat()
            val imgHeight = condicionesImg.height.toFloat()
            val imgAspectRatio = imgWidth / imgHeight

            // Usar más espacio para que se vea más grande
            var finalWidth = condicionesAvailableWidth * 1.05f
            var finalHeight = finalWidth / imgAspectRatio

            if (finalHeight > condicionesAvailableHeight) {
                finalHeight = condicionesAvailableHeight
                finalWidth = finalHeight * imgAspectRatio
            }

            val destRect = RectF(
                condicionesLeft,
                resumenTop - 5f,  // Subir un poco
                condicionesLeft + finalWidth,
                resumenTop - 5f + finalHeight
            )
            canvas.drawBitmap(condicionesImg, null, destRect, null)
        }
    } catch (e: Exception) {
        android.util.Log.e("PdfGenerator", "Error cargando imagen de condiciones: ${e.message}")
    }

    drawFooter(canvas)
    pdfDocument.finishPage(page)

    val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    if (docsDir == null) { pdfDocument.close(); return null }
    if (!docsDir.exists()) docsDir.mkdirs()

    val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val fileName = "Cotizacion_${timeStamp}_${System.currentTimeMillis()}.pdf"
    val file = File(docsDir, fileName)

    return try {
        FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
        pdfDocument.close()
        UploadQueueStorage.enqueue(context, PendingUpload(
            id = java.util.UUID.randomUUID().toString(),
            cotizacionId = cotizacion.folio.ifBlank { cotizacion.id.toString() },
            clienteNombre = cotizacion.clienteNombre,
            createdByNombre = cotizacion.especialista,
            filePath = file.absolutePath,
            status = "PENDING"
        ))
        file
    } catch (e: IOException) { e.printStackTrace(); pdfDocument.close(); null }
}