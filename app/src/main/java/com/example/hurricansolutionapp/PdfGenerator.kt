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
    val bottomBarHeight = 65f

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

            paint.color = Color.BLACK; paint.textSize = 14f
            paint.typeface = Typeface.create("times new roman", Typeface.NORMAL)
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Instalación de Protección Contra huracanes", pageWidth / 2f, bandCenterY - (paint.descent() + paint.ascent()) / 2f, paint)
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun drawFolioBox(canvas: Canvas, titleY: Float) {
        val folioTexto = "Folio: ${cotizacion.folio}"
        paint.color = Color.BLACK; paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT
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

    fun drawFooter(canvas: Canvas) {
        val footerTop = pageHeight.toFloat() - bottomBarHeight
        val lightGray = Color.parseColor("#E5E7EB")
        val mediumGray = Color.parseColor("#D1D5DB")
        val darkGray = Color.parseColor("#9CA3AF")

        val path = Path()
        val diagonalOffset = 15f
        path.moveTo(0f, footerTop + diagonalOffset)
        path.lineTo(pageWidth.toFloat(), footerTop)
        path.lineTo(pageWidth.toFloat(), pageHeight.toFloat())
        path.lineTo(0f, pageHeight.toFloat())
        path.close()

        val gradient = LinearGradient(0f, footerTop, pageWidth.toFloat(), pageHeight.toFloat(),
            intArrayOf(lightGray, mediumGray, darkGray), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; shader = gradient }
        canvas.drawPath(path, footerPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = darkGray; strokeWidth = 1f; style = Paint.Style.STROKE }
        canvas.drawLine(0f, footerTop + diagonalOffset, pageWidth.toFloat(), footerTop, linePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#374151"); textSize = 7f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); textAlign = Paint.Align.LEFT
        }

        val iconOptions = BitmapFactory.Options().apply { inScaled = false }
        val iconSize = 9f
        fun loadIcon(resId: Int): Bitmap? = BitmapFactory.decodeResource(context.resources, resId, iconOptions)
        val iconMail = loadIcon(R.drawable.ic_footer_mail)
        val iconWeb = loadIcon(R.drawable.ic_footer_web)
        val iconWhatsapp = loadIcon(R.drawable.ic_footer_whatsapp)
        val iconLocation = loadIcon(R.drawable.ic_footer_location)
        val iconPaintGray = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = PorterDuffColorFilter(Color.parseColor("#374151"), PorterDuff.Mode.SRC_IN) }

        fun drawIcon(bitmap: Bitmap?, centerX: Float, centerY: Float, size: Float = iconSize, iconPaint: Paint? = null) {
            if (bitmap == null) return
            val half = size / 2f
            canvas.drawBitmap(bitmap, null, RectF(centerX - half, centerY - half, centerX + half, centerY + half), iconPaint)
        }

        val line1Y = footerTop + 18f; val line2Y = line1Y + 12f; val line3Y = line2Y + 12f
        fun iconCenterY(baselineY: Float): Float = baselineY - textPaint.textSize / 2f + 1f

        val leftStartX = margin
        drawIcon(iconMail, leftStartX + iconSize / 2f, iconCenterY(line1Y), iconSize, iconPaintGray)
        canvas.drawText("administraciondeventas@hurricanesolution.com", leftStartX + iconSize + 4f, line1Y, textPaint)
        drawIcon(iconMail, leftStartX + iconSize / 2f, iconCenterY(line2Y), iconSize, iconPaintGray)
        canvas.drawText("protegiendo@hurricanesolution.com", leftStartX + iconSize + 4f, line2Y, textPaint)
        drawIcon(iconLocation, leftStartX + iconSize / 2f, iconCenterY(line3Y), iconSize, iconPaintGray)
        canvas.drawText("Dirección: Av. 10Nte, Plaza Tukan 258, Playa del Carmen, Q. Roo", leftStartX + iconSize + 4f, line3Y, textPaint)

        val rightTextEndX = pageWidth.toFloat() - margin
        val web = "www.hurricanesolution.com"; val phone = "9848035014 / 9987052145"
        val webTextWidth = textPaint.measureText(web); val webIconX = rightTextEndX - webTextWidth - iconSize - 4f
        drawIcon(iconWeb, webIconX + iconSize / 2f, iconCenterY(line1Y), iconSize, iconPaintGray)
        canvas.drawText(web, webIconX + iconSize + 4f, line1Y, textPaint)
        val phoneTextWidth = textPaint.measureText(phone); val phoneIconX = rightTextEndX - phoneTextWidth - iconSize - 4f
        drawIcon(iconWhatsapp, phoneIconX + iconSize / 2f, iconCenterY(line2Y), iconSize, iconPaintGray)
        canvas.drawText(phone, phoneIconX + iconSize + 4f, line2Y, textPaint)

        val iconFacebook = loadIcon(R.drawable.ic_footer_facebook)
        val iconLinkedIn = loadIcon(R.drawable.ic_footer_linkedin)
        val iconYoutube = loadIcon(R.drawable.ic_footer_youtube)
        val iconTikTok = loadIcon(R.drawable.ic_footer_tiktok)
        var socialX = rightTextEndX - 5f; val socialGap = 14f
        drawIcon(iconTikTok, socialX, iconCenterY(line3Y), 9f, iconPaintGray); socialX -= socialGap
        drawIcon(iconYoutube, socialX, iconCenterY(line3Y), 9f, iconPaintGray); socialX -= socialGap
        drawIcon(iconLinkedIn, socialX, iconCenterY(line3Y), 9f, iconPaintGray); socialX -= socialGap
        drawIcon(iconFacebook, socialX, iconCenterY(line3Y), 9f, iconPaintGray)
    }

    val productosSeleccionados: List<TipoProducto> = run {
        val lista = cotizacion.productos.ifEmpty { listOf(cotizacion.producto) }
        lista.distinct().sortedBy { p -> when (p) { TipoProducto.HS875 -> 0; TipoProducto.HS1250 -> 1; TipoProducto.HS1500 -> 2; TipoProducto.PERSONALIZADO -> 3 } }
    }

    val zonaGeografica = cotizacion.zonaGeografica
    val tableLeft = margin; val tableRight = pageWidth.toFloat() - margin
    val headerHeight = 42f; val bodyTextSize = 9f; val cellPadding = 4f; val cellLineHeight = 12f
    val colNumeroW = 25f; val colAreaW = 130f; val colAreaTotalW = 50f; val colMontajeW = 65f; val colAdecuacionesW = 65f
    val priceColumnsCount = productosSeleccionados.size.coerceAtLeast(1)
    val colPricesTotalW = tableRight - tableLeft - (colNumeroW + colAreaW + colAreaTotalW + colMontajeW + colAdecuacionesW)
    val colPriceW = colPricesTotalW / priceColumnsCount
    val startPreciosX = tableLeft + colNumeroW + colAreaW + colAreaTotalW + colMontajeW + colAdecuacionesW

    data class RowLayout(val linesArea: List<String>, val linesAreaTotal: List<String>, val linesMontaje: List<String>,
                         val linesAdecuaciones: List<String>, val linesPreciosPorProducto: List<List<String>>, val height: Float)

    paint.textSize = bodyTextSize; paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    val filas = mutableListOf<RowLayout>()

    cotizacion.ventanas.forEach { v ->
        val txtArea = v.descripcion; val txtAreaTotal = "%.2f".format(v.areaM2)
        val txtMontaje = v.tipoMontaje.ifBlank { cotizacion.tipoMontaje }
        val txtAdecuaciones = if (v.adecuacion == "No" || v.adecuacion.isBlank()) "Ninguna" else v.adecuacion
        val preciosPorProducto = productosSeleccionados.map { "$ " + "%,.2f".format(v.subtotalPorProducto(it, zonaGeografica)) }
        val linesArea = wrapText(txtArea, colAreaW - cellPadding * 2, paint)
        val linesAreaTotal = wrapText(txtAreaTotal, colAreaTotalW - cellPadding * 2, paint)
        val linesMontaje = wrapText(txtMontaje, colMontajeW - cellPadding * 2, paint)
        val linesAdecuaciones = wrapText(txtAdecuaciones, colAdecuacionesW - cellPadding * 2, paint)
        val linesPreciosPorProducto = preciosPorProducto.map { listOf(it) }
        val maxLines = listOf(linesArea.size, linesAreaTotal.size, linesMontaje.size, linesAdecuaciones.size, linesPreciosPorProducto.maxOfOrNull { it.size } ?: 1).maxOrNull() ?: 1
        filas.add(RowLayout(linesArea, linesAreaTotal, linesMontaje, linesAdecuaciones, linesPreciosPorProducto, maxLines * cellLineHeight + 4f))
    }

    val condLineCount = 14; val condLineHeight = 7.5f
    val condicionesMinBlock = 16f + condLineCount * condLineHeight + 45f
    val resumenBlockHeight = 22f * 5  // Actualizado: 22f por fila (antes era 18f)
    val extraSpaceNeededLastPage = condicionesMinBlock + resumenBlockHeight + 16f

    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    drawHeader(canvas)
    paint.color = Color.BLACK; paint.textSize = 16f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); paint.textAlign = Paint.Align.CENTER
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
        paint.textSize = 8f; paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val capitalizedValue = capitalizeWords(value)
        val wrappedLines = wrapText(capitalizedValue, valueW - paddingX * 2, paint, maxLines = 2)
        val numLines = wrappedLines.size.coerceIn(1, 2)
        val rowH = (numLines * lineHeightText + 8f).coerceAtLeast(20f)
        val yBottom = yTop + rowH

        paint.style = Paint.Style.FILL; paint.color = Color.BLACK
        canvasRef.drawRect(x, yTop, x + labelW, yBottom, paint)
        paint.color = Color.WHITE; paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); paint.textSize = 8f; paint.textAlign = Paint.Align.LEFT
        canvasRef.drawText(label, x + paddingX, yTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        canvasRef.drawRect(valueX, yTop, valueX + valueW, yBottom, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; paint.strokeWidth = 0.6f
        canvasRef.drawRect(valueX, yTop, valueX + valueW, yBottom, paint)

        paint.style = Paint.Style.FILL; paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); paint.textSize = 8f
        val totalTextHeight = numLines * lineHeightText
        var textY = yTop + (rowH - totalTextHeight) / 2f + lineHeightText - paint.descent()
        wrappedLines.forEach { line -> canvasRef.drawText(line, valueX + paddingX, textY, paint); textY += lineHeightText }
        return yBottom + 6f
    }

    var leftY = y; var rightY = y
    leftY = drawInfoRowCentered(canvas, leftX, leftY, "Nombre del Cliente:", cotizacion.clienteNombre, leftBlockWidth)
    if (cotizacion.ciudad.isNotBlank()) leftY = drawInfoRowCentered(canvas, leftX, leftY, "Ciudad:", cotizacion.ciudad, leftBlockWidth)

    // Extraer colonia y calle correctamente
    // ubicacion viene como: "Ciudad, Estado, Colonia, Calle" o "Ciudad, Colonia, Calle"
    // Pero ciudad ya contiene "Ciudad, Estado", así que debemos extraer después de eso
    val ubicacionCompleta = cotizacion.ubicacion
    val ciudadCompleta = cotizacion.ciudad

    // Remover la ciudad de la ubicación para obtener colonia y calle
    var restoDireccion = ubicacionCompleta
    if (ciudadCompleta.isNotBlank() && ubicacionCompleta.contains(ciudadCompleta)) {
        // Quitar la ciudad del inicio
        restoDireccion = ubicacionCompleta.replace(ciudadCompleta, "").trim()
        // Quitar comas al inicio si quedaron
        restoDireccion = restoDireccion.trimStart(',').trim()
    }

    // Ahora restoDireccion debería ser "Colonia, Calle" o solo uno de ellos
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

    fun drawTableHeader(startY: Float): Float {
        paint.textSize = 9f; paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.BLACK; paint.style = Paint.Style.FILL; paint.textAlign = Paint.Align.CENTER
        var x = tableLeft

        fun drawHeaderCell(text: String, width: Float) {
            canvas.drawRect(x, startY, x + width, startY + headerHeight, paint)
            paint.color = Color.WHITE
            canvas.drawText(text, x + width / 2f, startY + headerHeight / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
            paint.color = Color.BLACK; x += width
        }

        drawHeaderCell("", colNumeroW); drawHeaderCell("Área a proteger", colAreaW)
        drawHeaderCell("Área total", colAreaTotalW); drawHeaderCell("Tipo de montaje", colMontajeW)
        drawHeaderCell("Adecuaciones", colAdecuacionesW)

        productosSeleccionados.forEach { producto ->
            val hsLabel = when (producto) { TipoProducto.HS875 -> "HS-875"; TipoProducto.HS1250 -> "HS-1250"; TipoProducto.HS1500 -> "HS-1500"; TipoProducto.PERSONALIZADO -> "Pers." }
            canvas.drawRect(x, startY, x + colPriceW, startY + headerHeight, paint)
            paint.color = Color.WHITE; paint.textSize = 10f
            canvas.drawText(hsLabel, x + colPriceW / 2f, startY + headerHeight / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
            paint.color = Color.BLACK; x += colPriceW
        }
        return startY + headerHeight
    }

    y = drawTableHeader(y)

    var filaIndex = 0
    filas.forEach { fila ->
        val rowH = fila.height
        val footerTop = pageHeight.toFloat() - bottomBarHeight
        val isLastPage = filaIndex == filas.size - 1

        if (isLastPage) {
            if (y + rowH + extraSpaceNeededLastPage > footerTop - 10f) {
                drawFooter(canvas); pdfDocument.finishPage(page); pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo); canvas = page.canvas
                drawHeader(canvas); y = headerBarHeight + 20f; y = drawTableHeader(y)
            }
        } else {
            if (y + rowH > footerTop - 10f) {
                drawFooter(canvas); pdfDocument.finishPage(page); pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo); canvas = page.canvas
                drawHeader(canvas); y = headerBarHeight + 20f; y = drawTableHeader(y)
            }
        }

        val rowTop = y; val rowBottom = y + rowH
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, paint)
        paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; paint.strokeWidth = 0.5f
        canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, paint)

        var xCol = tableLeft
        fun drawCellBorder(left: Float, width: Float) { canvas.drawLine(left + width, rowTop, left + width, rowBottom, paint) }

        paint.style = Paint.Style.FILL; paint.color = Color.BLACK; paint.textSize = bodyTextSize
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); paint.textAlign = Paint.Align.CENTER

        val numCenterY = rowTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText("#${filaIndex + 1}", xCol + colNumeroW / 2f, numCenterY, paint)
        drawCellBorder(xCol, colNumeroW); xCol += colNumeroW

        paint.textAlign = Paint.Align.LEFT
        var textY = rowTop + cellPadding + cellLineHeight - paint.descent()
        fila.linesArea.forEach { line -> canvas.drawText(line, xCol + cellPadding, textY, paint); textY += cellLineHeight }
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
        y = rowBottom; filaIndex++
    }

    val areaTotal = cotizacion.ventanas.sumOf { it.areaM2 }
    fun descuentoPorM2(producto: TipoProducto): Double = when (producto) {
        TipoProducto.HS875 -> cotizacion.descuentoHS875; TipoProducto.HS1250 -> cotizacion.descuentoHS1250
        TipoProducto.HS1500 -> cotizacion.descuentoHS1500; TipoProducto.PERSONALIZADO -> 0.0
    }

    val totalesPorProducto = productosSeleccionados.associateWith { producto -> cotizacion.ventanas.sumOf { it.subtotalPorProducto(producto, zonaGeografica) } }
    val descuentosPorcentaje = productosSeleccionados.associateWith { producto ->
        val subtotalProducto = totalesPorProducto[producto] ?: 0.0; val descM2 = descuentoPorM2(producto)
        if (subtotalProducto == 0.0) 0.0 else (areaTotal * descM2 / subtotalProducto) * 100.0
    }
    val preciosFinalesPorProducto = productosSeleccionados.associateWith { producto ->
        val subtotalProducto = totalesPorProducto[producto] ?: 0.0; val descM2 = descuentoPorM2(producto)
        (subtotalProducto - areaTotal * descM2).coerceAtLeast(0.0)
    }

    val labelWidth = 160f; val boxLeft = startPreciosX - labelWidth; val boxRight = tableRight
    val valueColumnWidth = colPriceW; val rowHeightResumen = 22f

    val footerTop = pageHeight.toFloat() - bottomBarHeight; val margenSobreFooter = 10f
    val resumenTopDesdeAbajo = pageHeight.toFloat() - bottomBarHeight - margenSobreFooter - rowHeightResumen * 5
    val resumenTop = maxOf(y + 20f, resumenTopDesdeAbajo); val resumenBottom = resumenTop + rowHeightResumen * 5
    var filaTop = resumenTop

    // Nuevos colores según el diseño
    val colorSubtotal = Color.parseColor("#D9D9D9")
    val colorSubtotalConDescuento = Color.parseColor("#898989")
    val colorPrecioFinal = Color.parseColor("#494949")
    val colorBlanco = Color.WHITE

    // Calcular el área de valores (para el borde exterior)
    val valuesBoxLeft = boxLeft + labelWidth
    val valuesBoxRight = valuesBoxLeft + (valueColumnWidth * productosSeleccionados.size)

    /**
     * Dibuja una fila del resumen de precios con el nuevo diseño:
     * - Borde negro exterior alrededor de toda la sección de valores
     * - Sin cuadrículas internas entre celdas
     * - Colores de fondo específicos por fila
     * - Texto en negrita solo para SUBTOTAL, SUBTOTAL CON DESCUENTO y PRECIO FINAL
     * - PRECIO FINAL con texto blanco
     */
    fun drawResumenRowNuevoDiseno(
        label: String,
        getTextForCol: (Int, TipoProducto) -> String,
        labelBold: Boolean,
        valueBold: Boolean,
        bgColor: Int,
        textColor: Int = Color.BLACK,
        isFirstRow: Boolean = false,
        isLastRow: Boolean = false
    ) {
        val filaBottom = filaTop + rowHeightResumen

        // Caja negra del label (siempre negra)
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawRect(boxLeft, filaTop, boxLeft + labelWidth, filaBottom, paint)

        // Texto del label (siempre blanco)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 7.5f
        paint.typeface = if (labelBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(label.uppercase(), boxLeft + 8f, filaTop + rowHeightResumen / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        // Columnas de valores
        productosSeleccionados.forEachIndexed { index, producto ->
            val left = boxLeft + labelWidth + index * valueColumnWidth
            val right = left + valueColumnWidth

            // Fondo de la celda
            paint.style = Paint.Style.FILL
            paint.color = bgColor
            canvas.drawRect(left, filaTop, right, filaBottom, paint)

            // Texto del valor (tamaño reducido para mejor perspectiva)
            paint.style = Paint.Style.FILL
            paint.color = textColor
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 7.5f
            paint.typeface = if (valueBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(getTextForCol(index, producto), left + valueColumnWidth / 2f, filaTop + rowHeightResumen / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
        }

        // Dibujar borde negro exterior de la sección de valores
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = .95f

        // Borde izquierdo (siempre)
        canvas.drawLine(valuesBoxLeft, filaTop, valuesBoxLeft, filaBottom, paint)
        // Borde derecho (siempre)
        canvas.drawLine(valuesBoxRight, filaTop, valuesBoxRight, filaBottom, paint)
        // Borde superior (solo primera fila)
        if (isFirstRow) {
            canvas.drawLine(valuesBoxLeft, filaTop, valuesBoxRight, filaTop, paint)
        }
        // Borde inferior (solo última fila)
        if (isLastRow) {
            canvas.drawLine(valuesBoxLeft, filaBottom, valuesBoxRight, filaBottom, paint)
        }

        filaTop = filaBottom
    }

    // SUBTOTAL - Fondo #D9D9D9, label en negrita, valores en negrita, texto negro
    drawResumenRowNuevoDiseno("Subtotal USD", { _, p -> "$ " + "%,.2f".format(totalesPorProducto[p] ?: 0.0) },
        labelBold = true, valueBold = true, bgColor = colorSubtotal, textColor = Color.BLACK, isFirstRow = true)

    // DESCUENTO - Fondo blanco, sin negrita, texto negro
    drawResumenRowNuevoDiseno("Descuento", { _, p -> String.format("%.2f%%", descuentosPorcentaje[p] ?: 0.0) },
        labelBold = false, valueBold = false, bgColor = colorBlanco, textColor = Color.BLACK)

    // SUBTOTAL CON DESCUENTO - Fondo #898989, label en negrita, valores en negrita, texto negro
    drawResumenRowNuevoDiseno("Subtotal con Descuento USD", { _, p -> "$ " + "%,.2f".format(preciosFinalesPorProducto[p] ?: 0.0) },
        labelBold = true, valueBold = true, bgColor = colorSubtotalConDescuento, textColor = Color.BLACK)

    // IVA - Fondo blanco, sin negrita, texto negro
    drawResumenRowNuevoDiseno("IVA", { _, p -> "$ " + "%,.2f".format((preciosFinalesPorProducto[p] ?: 0.0) * IVA_RATE) },
        labelBold = false, valueBold = false, bgColor = colorBlanco, textColor = Color.BLACK)

    // PRECIO FINAL CON IVA - Fondo #494949, label en negrita, valores en negrita, texto BLANCO
    drawResumenRowNuevoDiseno("Precio Final USD", { _, p -> "$ " + "%,.2f".format((preciosFinalesPorProducto[p] ?: 0.0) * (1.0 + IVA_RATE)) },
        labelBold = true, valueBold = true, bgColor = colorPrecioFinal, textColor = Color.WHITE, isLastRow = true)

    // CONDICIONES COMERCIALES - USANDO IMAGEN (Manteniendo proporción)
    val condicionesLeft = margin
    val condicionesRight = boxLeft - 15f
    val condicionesAvailableWidth = condicionesRight - condicionesLeft
    val condicionesAvailableHeight = resumenBottom - resumenTop

    try {
        val options = BitmapFactory.Options().apply { inScaled = false }
        val condicionesImg = BitmapFactory.decodeResource(context.resources, R.drawable.condiciones_comerciales, options)
        if (condicionesImg != null) {
            // Calcular el tamaño manteniendo la proporción de la imagen
            val imgWidth = condicionesImg.width.toFloat()
            val imgHeight = condicionesImg.height.toFloat()
            val imgAspectRatio = imgWidth / imgHeight

            // Ajustar al espacio disponible manteniendo proporción
            var finalWidth = condicionesAvailableWidth
            var finalHeight = finalWidth / imgAspectRatio

            // Si la altura calculada es mayor que la disponible, ajustar por altura
            if (finalHeight > condicionesAvailableHeight) {
                finalHeight = condicionesAvailableHeight
                finalWidth = finalHeight * imgAspectRatio
            }

            // Dibujar la imagen con su proporción correcta (alineada arriba-izquierda)
            val destRect = RectF(
                condicionesLeft,
                resumenTop,
                condicionesLeft + finalWidth,
                resumenTop + finalHeight
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