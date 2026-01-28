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
    val bottomBarHeight = 50f

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

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                val alpha = (pixels[offset + x] ushr 24) and 0xFF
                if (alpha > 0) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < 0 || maxY < 0) return bitmap
        val cropWidth = maxX - minX + 1
        val cropHeight = maxY - minY + 1
        return Bitmap.createBitmap(bitmap, minX, minY, cropWidth, cropHeight)
    }

    fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else currentLine.toString() + " " + word
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine.clear()
                currentLine.append(candidate)
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine.toString())
        return lines
    }

    fun drawHeader(canvas: Canvas) {
        try {
            val options = BitmapFactory.Options().apply { inScaled = false }

            val rawLogo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_header_new, options)
            val croppedLogo = cropTransparent(rawLogo)

            val targetHeight = 45f
            val aspectRatio = croppedLogo.width.toFloat() / croppedLogo.height.toFloat()
            val destHeight = targetHeight
            val destWidth = destHeight * aspectRatio

            val bandCenterY = headerBarHeight / 2f
            val logoTop = bandCenterY - destHeight / 2f
            val logoLeft = margin

            val destRect = RectF(logoLeft, logoTop, logoLeft + destWidth, logoTop + destHeight)
            canvas.drawBitmap(croppedLogo, null, destRect, null)

            val rawUsa = BitmapFactory.decodeResource(context.resources, R.drawable.made_in_usa, options)
            val usaCropped = cropTransparent(rawUsa)

            val usaTargetHeight = 38f
            val usaAspect = usaCropped.width.toFloat() / usaCropped.height.toFloat()
            val usaDestHeight = usaTargetHeight
            val usaDestWidth = usaDestHeight * usaAspect

            val usaTop = bandCenterY - usaDestHeight / 2f - 3f
            val usaRight = pageWidth.toFloat() - margin
            val usaLeft = usaRight - usaDestWidth

            val usaRect = RectF(usaLeft, usaTop, usaRight, usaTop + usaDestHeight)
            val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
            val usaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }
            canvas.drawBitmap(usaCropped, null, usaRect, usaPaint)

            paint.color = Color.BLACK
            paint.textSize = 14f
            paint.typeface = Typeface.create("times new roman", Typeface.NORMAL)
            paint.textAlign = Paint.Align.CENTER

            val lema = "Instalación de Protección Contra huracanes"
            val lemaX = pageWidth / 2f
            val lemaY = headerBarHeight / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(lema, lemaX, lemaY, paint)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun drawFolioBox(canvas: Canvas, titleY: Float) {
        val folioTexto = "Folio: ${cotizacion.folio}"

        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT

        val textWidth = paint.measureText(folioTexto)
        val boxPaddingH = 6f
        val boxHeight = 18f

        val boxCenterY = titleY
        val boxTop = boxCenterY - boxHeight / 2f
        val boxBottom = boxTop + boxHeight
        val boxRight = pageWidth.toFloat() - margin
        val boxLeft = boxRight - textWidth - boxPaddingH * 2

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, boxPaint)

        boxPaint.style = Paint.Style.STROKE
        boxPaint.color = Color.DKGRAY
        boxPaint.strokeWidth = 0.8f
        canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, boxPaint)

        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.LEFT
        val textX = boxLeft + boxPaddingH
        val textY = boxTop + boxHeight / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(folioTexto, textX, textY, paint)
    }

    fun drawFooter(canvas: Canvas) {
        val footerTop = pageHeight.toFloat() - bottomBarHeight

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        val iconOptions = BitmapFactory.Options().apply { inScaled = false }
        val iconSize = 9f

        fun loadIcon(resId: Int): Bitmap? =
            BitmapFactory.decodeResource(context.resources, resId, iconOptions)

        val iconMail = loadIcon(R.drawable.ic_footer_mail)
        val iconWeb = loadIcon(R.drawable.ic_footer_web)
        val iconWhatsRaw = loadIcon(R.drawable.ic_footer_whatsapp)
        val iconLocation = loadIcon(R.drawable.ic_footer_location)

        val iconPaintGray = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN)
        }

        fun drawIcon(bitmap: Bitmap?, centerX: Float, centerY: Float, size: Float = iconSize, iconPaint: Paint? = null) {
            if (bitmap == null) return
            val half = size / 2f
            val rect = RectF(centerX - half, centerY - half, centerX + half, centerY + half)
            canvas.drawBitmap(bitmap, null, rect, iconPaint)
        }

        val email1 = "administraciondeventas@hurricanesolution.com"
        val email2 = "protegiendo@hurricanesolution.com"
        val web = "www.hurricanesolution.com"
        val phone = "9848035014 / 9987052145"
        val address = "Dirección: Av. XXX, Playa del Carmen, Q. Roo"

        val line1Y = footerTop + 12f
        val line2Y = line1Y + 12f
        val line3Y = line2Y + 12f

        fun iconCenterYForText(baselineY: Float): Float = baselineY - textPaint.textSize / 2f + 1f

        val leftStartX = margin
        val leftIconCenterX = leftStartX + iconSize / 2f
        val leftTextStartX = leftStartX + iconSize + 4f

        drawIcon(iconMail, leftIconCenterX, iconCenterYForText(line1Y), iconSize, iconPaintGray)
        canvas.drawText(email1, leftTextStartX, line1Y, textPaint)

        drawIcon(iconMail, leftIconCenterX, iconCenterYForText(line2Y), iconSize, iconPaintGray)
        canvas.drawText(email2, leftTextStartX, line2Y, textPaint)

        drawIcon(iconLocation, leftIconCenterX, iconCenterYForText(line3Y), iconSize, iconPaintGray)
        canvas.drawText(address, leftTextStartX, line3Y, textPaint)

        val rightTextEndX = pageWidth.toFloat() - margin
        textPaint.textAlign = Paint.Align.LEFT

        val webTextWidth = textPaint.measureText(web)
        val webIconX = rightTextEndX - webTextWidth - iconSize - 4f
        drawIcon(iconWeb, webIconX + iconSize / 2f, iconCenterYForText(line1Y), iconSize, iconPaintGray)
        canvas.drawText(web, webIconX + iconSize + 4f, line1Y, textPaint)

        val phoneTextWidth = textPaint.measureText(phone)
        val phoneIconX = rightTextEndX - phoneTextWidth - iconSize - 4f
        drawIcon(iconWhatsRaw, phoneIconX + iconSize / 2f, iconCenterYForText(line2Y), iconSize, iconPaintGray)
        canvas.drawText(phone, phoneIconX + iconSize + 4f, line2Y, textPaint)

        val iconFacebook = loadIcon(R.drawable.ic_footer_facebook)
        val iconLinkedIn = loadIcon(R.drawable.ic_footer_linkedin)
        val iconYoutube = loadIcon(R.drawable.ic_footer_youtube)
        val iconTikTok = loadIcon(R.drawable.ic_footer_tiktok)

        var socialX = rightTextEndX - 5f
        val socialY = line3Y
        fun drawSocialIcon(bitmap: Bitmap?, centerX: Float) {
            drawIcon(bitmap, centerX, iconCenterYForText(socialY), size = 9f, iconPaint = iconPaintGray)
        }
        val socialGap = 14f
        drawSocialIcon(iconTikTok, socialX)
        socialX -= socialGap
        drawSocialIcon(iconYoutube, socialX)
        socialX -= socialGap
        drawSocialIcon(iconLinkedIn, socialX)
        socialX -= socialGap
        drawSocialIcon(iconFacebook, socialX)
    }

    val productosSeleccionados: List<TipoProducto> = run {
        val lista = cotizacion.productos.ifEmpty { listOf(cotizacion.producto) }
        lista.distinct().sortedBy { p ->
            when (p) {
                TipoProducto.HS875 -> 0
                TipoProducto.HS1250 -> 1
                TipoProducto.HS1500 -> 2
                TipoProducto.PERSONALIZADO -> 3
            }
        }
    }

    val zonaGeografica = cotizacion.zonaGeografica

    val tableLeft = margin
    val tableRight = pageWidth.toFloat() - margin
    val headerHeight = 42f
    val bodyTextSize = 9f
    val cellPadding = 4f
    val cellLineHeight = 12f

    val colNumeroW = 25f
    val colAreaW = 130f
    val colAreaTotalW = 50f
    val colMontajeW = 65f
    val colAdecuacionesW = 65f

    val priceColumnsCount = productosSeleccionados.size.coerceAtLeast(1)
    val colPricesTotalW = tableRight - tableLeft - (colNumeroW + colAreaW + colAreaTotalW + colMontajeW + colAdecuacionesW)
    val colPriceW = colPricesTotalW / priceColumnsCount

    val startPreciosX = tableLeft + colNumeroW + colAreaW + colAreaTotalW + colMontajeW + colAdecuacionesW

    data class RowLayout(
        val linesArea: List<String>,
        val linesAreaTotal: List<String>,
        val linesMontaje: List<String>,
        val linesAdecuaciones: List<String>,
        val linesPreciosPorProducto: List<List<String>>,
        val height: Float
    )

    paint.textSize = bodyTextSize
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    val filas = mutableListOf<RowLayout>()

    cotizacion.ventanas.forEach { v ->
        val txtArea = v.descripcion
        val txtAreaTotal = "%.2f".format(v.areaM2)
        val txtMontaje = v.tipoMontaje.ifBlank { cotizacion.tipoMontaje }
        val txtAdecuaciones = if (v.adecuacion == "No" || v.adecuacion.isBlank()) "Ninguna" else v.adecuacion

        val preciosPorProducto: List<String> = productosSeleccionados.map { producto ->
            val monto = v.subtotalPorProducto(producto, zonaGeografica)
            "$ " + "%,.2f".format(monto)
        }

        val linesArea = wrapText(txtArea, colAreaW - cellPadding * 2, paint)
        val linesAreaTotal = wrapText(txtAreaTotal, colAreaTotalW - cellPadding * 2, paint)
        val linesMontaje = wrapText(txtMontaje, colMontajeW - cellPadding * 2, paint)
        val linesAdecuaciones = wrapText(txtAdecuaciones, colAdecuacionesW - cellPadding * 2, paint)
        val linesPreciosPorProducto: List<List<String>> = preciosPorProducto.map { listOf(it) }

        val maxLines = listOf(
            linesArea.size,
            linesAreaTotal.size,
            linesMontaje.size,
            linesAdecuaciones.size,
            linesPreciosPorProducto.maxOfOrNull { it.size } ?: 1
        ).maxOrNull() ?: 1

        val rowHeightDynamic = maxLines * cellLineHeight + 4f

        filas.add(RowLayout(linesArea, linesAreaTotal, linesMontaje, linesAdecuaciones, linesPreciosPorProducto, rowHeightDynamic))
    }

    val condLineCount = 14
    val condLineHeight = 7.5f
    val condicionesMinBlock = 16f + condLineCount * condLineHeight + 45f
    val resumenBlockHeight = 18f * 5
    val extraSpaceNeededLastPage = condicionesMinBlock + resumenBlockHeight + 16f

    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    drawHeader(canvas)

    paint.color = Color.BLACK
    paint.textSize = 16f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER

    val titulo = "COTIZACIÓN DE PROYECTO"
    val tituloY = 105f

    canvas.drawText(titulo, pageWidth / 2f, tituloY, paint)
    drawFolioBox(canvas, tituloY)

    paint.textAlign = Paint.Align.LEFT

    var y = 140f

    val leftX = margin
    val leftBlockWidth = 270f
    val rightBlockWidth = 230f
    val rightX = pageWidth.toFloat() - margin - rightBlockWidth

    fun drawInfoRowMultiline(
        canvasRef: Canvas,
        x: Float,
        yTop: Float,
        label: String,
        value: String,
        blockW: Float
    ): Float {
        val labelW = 90f
        val gapW = 8f
        val paddingX = 6f
        val lineHeightText = 10f

        val valueX = x + labelW + gapW
        val valueW = blockW - labelW - gapW

        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val capitalizedValue = capitalizeWords(value)
        val wrappedLines = wrapText(capitalizedValue, valueW - paddingX * 2, paint)

        val numLines = wrappedLines.size.coerceAtLeast(1)
        val rowH = (numLines * lineHeightText + 8f).coerceAtLeast(18f)
        val rowGap = 6f

        val yBottom = yTop + rowH

        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvasRef.drawRect(x, yTop, x + labelW, yBottom, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        paint.textAlign = Paint.Align.LEFT
        val labelY = yTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f
        canvasRef.drawText(label, x + paddingX, labelY, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvasRef.drawRect(valueX, yTop, valueX + valueW, yBottom, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 0.6f
        canvasRef.drawRect(valueX, yTop, valueX + valueW, yBottom, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f

        var textY = yTop + 4f + lineHeightText - paint.descent()
        wrappedLines.forEach { line ->
            canvasRef.drawText(line, valueX + paddingX, textY, paint)
            textY += lineHeightText
        }

        return yBottom + rowGap
    }

    var leftY = y
    var rightY = y

    leftY = drawInfoRowMultiline(canvas, leftX, leftY, "Nombre del Cliente:", cotizacion.clienteNombre, leftBlockWidth)

    if (cotizacion.ciudad.isNotBlank()) {
        leftY = drawInfoRowMultiline(canvas, leftX, leftY, "Ciudad:", cotizacion.ciudad, leftBlockWidth)
    }

    val partesUbicacion = cotizacion.ubicacion.split(",").map { it.trim() }
    val colonia = partesUbicacion.getOrNull(1) ?: ""
    val calle = partesUbicacion.getOrNull(2) ?: ""

    if (colonia.isNotBlank()) {
        leftY = drawInfoRowMultiline(canvas, leftX, leftY, "Colonia:", colonia, leftBlockWidth)
    }

    if (calle.isNotBlank()) {
        leftY = drawInfoRowMultiline(canvas, leftX, leftY, "Calle y Número:", calle, leftBlockWidth)
    }

    val metrajeFinal = cotizacion.ventanas.sumOf { it.areaM2 }
    rightY = drawInfoRowMultiline(canvas, rightX, rightY, "Especialista:", cotizacion.especialista, rightBlockWidth)
    rightY = drawInfoRowMultiline(canvas, rightX, rightY, "Fecha:", cotizacion.fecha, rightBlockWidth)
    rightY = drawInfoRowMultiline(canvas, rightX, rightY, "Metraje Total:", "%.2f m²".format(metrajeFinal), rightBlockWidth)

    y = maxOf(leftY, rightY) + 12f

    fun drawTableHeader(startY: Float): Float {
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.CENTER

        var x = tableLeft

        fun drawHeaderCell(text: String, width: Float) {
            canvas.drawRect(x, startY, x + width, startY + headerHeight, paint)
            paint.color = Color.WHITE
            val centerX = x + width / 2f
            val centerY = startY + headerHeight / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(text, centerX, centerY, paint)
            paint.color = Color.BLACK
            x += width
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

            canvas.drawRect(x, startY, x + colPriceW, startY + headerHeight, paint)
            val centerX = x + colPriceW / 2f
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            val textY = startY + headerHeight / 2f - (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(hsLabel, centerX, textY, paint)
            paint.color = Color.BLACK
            x += colPriceW
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
                drawFooter(canvas)
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas)
                y = headerBarHeight + 20f
                y = drawTableHeader(y)
            }
        } else {
            if (y + rowH > footerTop - 10f) {
                drawFooter(canvas)
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas)
                y = headerBarHeight + 20f
                y = drawTableHeader(y)
            }
        }

        val rowTop = y
        val rowBottom = y + rowH

        var cellX = tableLeft

        // Número - CENTRADO
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawRect(cellX, rowTop, cellX + colNumeroW, rowBottom, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 0.5f
        canvas.drawRect(cellX, rowTop, cellX + colNumeroW, rowBottom, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = bodyTextSize
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val numY = rowTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText("#${filaIndex + 1}", cellX + colNumeroW / 2f, numY, paint)
        cellX += colNumeroW

        // ========== FUNCIN PARA DIBUJAR CELDA CON TEXTO CENTRADO VERTICAL Y HORIZONTAL ==========
        fun drawCenteredTextCell(lines: List<String>, width: Float) {
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawRect(cellX, rowTop, cellX + width, rowBottom, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            canvas.drawRect(cellX, rowTop, cellX + width, rowBottom, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.CENTER  // CENTRADO HORIZONTAL
            paint.textSize = bodyTextSize

            // Calcular altura total del texto
            val totalTextHeight = lines.size * cellLineHeight
            // Calcular Y inicial para centrar verticalmente
            val startTextY = rowTop + (rowH - totalTextHeight) / 2f + cellLineHeight - paint.descent()

            var textY = startTextY
            lines.forEach { line ->
                canvas.drawText(line, cellX + width / 2f, textY, paint)  // Centro de la celda
                textY += cellLineHeight
            }
            cellX += width
        }

        drawCenteredTextCell(fila.linesArea, colAreaW)
        drawCenteredTextCell(fila.linesAreaTotal, colAreaTotalW)
        drawCenteredTextCell(fila.linesMontaje, colMontajeW)
        drawCenteredTextCell(fila.linesAdecuaciones, colAdecuacionesW)

        // Precios - ya estaban centrados
        fila.linesPreciosPorProducto.forEach { lines ->
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawRect(cellX, rowTop, cellX + colPriceW, rowBottom, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            canvas.drawRect(cellX, rowTop, cellX + colPriceW, rowBottom, paint)

            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.RIGHT
            val textY = rowTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f
            lines.forEach { line ->
                canvas.drawText(line, cellX + colPriceW - cellPadding, textY, paint)
            }
            cellX += colPriceW
        }

        y = rowBottom
        filaIndex++
    }

    // ======================= BLOQUE DE RESUMEN =======================
    val areaTotal = cotizacion.ventanas.sumOf { it.areaM2 }

    fun descuentoPorM2(producto: TipoProducto): Double {
        return cotizacion.getDescuentoPorProducto(producto)
    }

    val totalesPorProducto: Map<TipoProducto, Double> =
        productosSeleccionados.associateWith { producto ->
            cotizacion.ventanas.sumOf { v -> v.subtotalPorProducto(producto, zonaGeografica) }
        }

    val descuentosPorcentaje: Map<TipoProducto, Double> =
        productosSeleccionados.associateWith { producto ->
            val subtotalProducto = totalesPorProducto[producto] ?: 0.0
            val descM2 = descuentoPorM2(producto)
            if (subtotalProducto == 0.0) 0.0 else {
                val descuentoImporte = areaTotal * descM2
                (descuentoImporte / subtotalProducto) * 100.0
            }
        }

    val preciosFinalesPorProducto: Map<TipoProducto, Double> =
        productosSeleccionados.associateWith { producto ->
            val subtotalProducto = totalesPorProducto[producto] ?: 0.0
            val descM2 = descuentoPorM2(producto)
            val descuentoImporte = areaTotal * descM2
            (subtotalProducto - descuentoImporte).coerceAtLeast(0.0)
        }

    val labelWidth = 130f
    val boxLeft = startPreciosX - labelWidth
    val boxRight = tableRight

    val valueColumnWidth = colPriceW

    val rowHeightResumen = 16f
    val totalResumenRows = 5
    val bloqueAltura = rowHeightResumen * totalResumenRows

    val footerTop = pageHeight.toFloat() - bottomBarHeight
    val margenSobreFooter = 10f
    val resumenTopDesdeAbajo = pageHeight.toFloat() - bottomBarHeight - margenSobreFooter - bloqueAltura
    val espacioEntreTablaYResumen = 20f
    val resumenTop = maxOf(y + espacioEntreTablaYResumen, resumenTopDesdeAbajo)
    val resumenBottom = resumenTop + bloqueAltura

    var filaTop = resumenTop

    fun drawResumenDataRow(
        label: String,
        getTextForCol: (Int, TipoProducto) -> String,
        destacar: Boolean,
        isLastRow: Boolean = false
    ) {
        val filaBottom = filaTop + rowHeightResumen

        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawRect(boxLeft, filaTop, boxLeft + labelWidth, filaBottom, paint)

        if (isLastRow) {
            canvas.drawRect(boxLeft, filaBottom - 1f, boxLeft + labelWidth, filaBottom, paint)
        }

        canvas.drawRect(boxLeft, filaTop, boxLeft + labelWidth, filaTop + 1f, paint)

        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 8f
        paint.typeface = if (destacar) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        canvas.drawText(label.uppercase(), boxLeft + 4f, filaBottom - 4f, paint)

        productosSeleccionados.forEachIndexed { index, producto ->
            val left = boxLeft + labelWidth + index * valueColumnWidth
            val right = left + valueColumnWidth

            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawRect(left, filaTop, right, filaBottom, paint)

            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            paint.strokeWidth = 0.5f
            canvas.drawLine(left, filaTop, left, filaBottom, paint)
            canvas.drawLine(right, filaTop, right, filaBottom, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            canvas.drawRect(left, filaTop, right, filaTop + 1f, paint)

            if (isLastRow) {
                canvas.drawRect(left, filaBottom - 1f, right, filaBottom, paint)
            }

            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.RIGHT

            val text = getTextForCol(index, producto)
            paint.textSize = 8f
            paint.typeface = if (destacar) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            canvas.drawText(text, right - 4f, filaBottom - 4f, paint)
        }

        filaTop = filaBottom
    }

    drawResumenDataRow(
        label = "Subtotal",
        getTextForCol = { _, producto ->
            val subtotalProducto = totalesPorProducto[producto] ?: 0.0
            "$ " + "%,.2f".format(subtotalProducto)
        },
        destacar = true
    )

    drawResumenDataRow(
        label = "Descuento",
        getTextForCol = { _, producto ->
            val pct = descuentosPorcentaje[producto] ?: 0.0
            String.format("%.2f%%", pct)
        },
        destacar = false
    )

    drawResumenDataRow(
        label = "Subtotal con descuento",
        getTextForCol = { _, producto ->
            val subtotalConDescSinIva = preciosFinalesPorProducto[producto] ?: 0.0
            "$ " + "%,.2f".format(subtotalConDescSinIva)
        },
        destacar = true
    )

    drawResumenDataRow(
        label = "IVA",
        getTextForCol = { _, producto ->
            val subtotalConDesc = preciosFinalesPorProducto[producto] ?: 0.0
            val iva = subtotalConDesc * IVA_RATE
            "$ " + "%,.2f".format(iva)
        },
        destacar = false
    )

    drawResumenDataRow(
        label = "Precio Final con IVA",
        getTextForCol = { _, producto ->
            val precioSinIva = preciosFinalesPorProducto[producto] ?: 0.0
            val precioConIva = precioSinIva * (1.0 + IVA_RATE)
            "$ " + "%,.2f".format(precioConIva)
        },
        destacar = true,
        isLastRow = true
    )

    // ======================= CONDICIONES COMERCIALES =======================
    // rea disponible: desde margin izquierdo hasta boxLeft, desde resumenTop hasta resumenBottom
    val condicionesLeft = margin
    val condicionesRight = boxLeft - 15f  // Espacio entre condiciones y tabla de precios
    val maxCondicionesWidth = condicionesRight - condicionesLeft

    // Las condiciones empiezan alineadas con el bloque de precios (resumenTop)
    // Y terminan alineadas con el final del bloque de precios (resumenBottom)
    val condicionesTop = resumenTop
    val condicionesBottom = resumenBottom
    val alturaDisponibleCondiciones = condicionesBottom - condicionesTop

    // LAS 12 CONDICIONES EXACTAS DEL TXT
    val condicionesLineas = listOf(
        "Precios cotizados en Dólares Americanos.",
        "Los precios ya incluyen IVA.",
        "Para Pago En Moneda Nacional Aplicará el T.C Vigente Al Día De Pago Según Banco De México.",
        "Se Requiere 50% De Anticipo Para La Programación De Instalación.",
        "No Hay Reembolsos Por Cancelación Después De 3 Días Del Pago De Anticipo.",
        "Vigencia De La Cotización: 15 Días.",
        "El Precio Ya Incluye Instalación Dentro De La Zona Continental De Quintana Roo, Para Proyectos Ubicados Fuera De Esta Zona Se Hará Un Cargo Extra Por Concepto De Viáticos.",
        "Las Medidas Contempladas En Esta Propuesta Pueden Variar Después De La Rectificación.",
        "La Instalación Se Programará Con Base En La Agenda Y Todo Proyecto Entrará A Una Fila De Instalación. Los Tiempos De Instalación Serán De Acuerdo A Las Fechas Que Se Tengan Programadas en Acapulco, En Caso De Existir Algún Espacio Disponible Antes Del Periodo Máximo, Se Le Notificará Al Cliente.",
        "Aplicará La Garantía De Acuerdo Al Sistema Contratado Y Siempre Y Cuando Cumpla Con Los Cuidados Y Recomendaciones Entregadas Al Termino De La Instalación.",
        "Los Descuentos Concedidos En Esta Cotización Podrán Modificarse Si El Metraje Total Disminuye O Se Cancela Algún rea.",
        "El Costo De Adecuaciones O Modificaciones Estructurales Como Instalación De PTR o Cajillos En Prefabricados NO ESTAN INCLUIDOS."
    )

    // Configuración de fuente para condiciones - legible y profesional
    val condTitleSize = 6f
    val condTextSize = 4.5f
    val condLineSpacing = 6f

    paint.textAlign = Paint.Align.LEFT
    paint.color = Color.BLACK

    // Título "Condiciones Comerciales:"
    paint.textSize = condTitleSize
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    var condY = condicionesTop + condTitleSize + 2f
    canvas.drawText("Condiciones Comerciales:", condicionesLeft, condY, paint)
    condY += condLineSpacing

    // Texto de las condiciones
    paint.textSize = condTextSize
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)

    // Función para dibujar una condición con número y wrap de texto
    fun drawCondition(index: Int, text: String, startY: Float): Float {
        val numberPrefix = "$index.- "
        val numberWidth = paint.measureText(numberPrefix)
        val maxTextWidth = maxCondicionesWidth - numberWidth

        // Dividir el texto en líneas que quepan
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) > maxTextWidth) {
                if (current.isNotEmpty()) lines.add(current)
                current = word
            } else {
                current = candidate
            }
        }
        if (current.isNotEmpty()) lines.add(current)

        var currentY = startY

        // Dibujar primera línea con número
        if (lines.isNotEmpty()) {
            canvas.drawText(numberPrefix, condicionesLeft, currentY, paint)
            canvas.drawText(lines[0], condicionesLeft + numberWidth, currentY, paint)
            currentY += condLineSpacing

            // Dibujar líneas adicionales (sin número, indentadas)
            for (i in 1 until lines.size) {
                if (currentY < condicionesBottom - 2f) {
                    canvas.drawText(lines[i], condicionesLeft + numberWidth, currentY, paint)
                    currentY += condLineSpacing
                }
            }
        }

        return currentY
    }

    // Dibujar las 12 condiciones
    condicionesLineas.forEachIndexed { index, linea ->
        if (condY < condicionesBottom - condLineSpacing) {
            condY = drawCondition(index + 1, linea, condY)
        }
    }

    drawFooter(canvas)
    pdfDocument.finishPage(page)

    val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    if (docsDir == null) {
        pdfDocument.close()
        return null
    }

    if (!docsDir.exists()) docsDir.mkdirs()

    val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val fileName = "Cotizacion_${timeStamp}_${System.currentTimeMillis()}.pdf"
    val file = File(docsDir, fileName)

    return try {
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        UploadQueueStorage.enqueue(
            context,
            PendingUpload(
                id = java.util.UUID.randomUUID().toString(),
                cotizacionId = cotizacion.folio.ifBlank { cotizacion.id.toString() },
                clienteNombre = cotizacion.clienteNombre,
                createdByNombre = cotizacion.especialista,
                filePath = file.absolutePath,
                status = "PENDING"
            )
        )

        file
    } catch (e: IOException) {
        e.printStackTrace()
        pdfDocument.close()
        null
    }
}