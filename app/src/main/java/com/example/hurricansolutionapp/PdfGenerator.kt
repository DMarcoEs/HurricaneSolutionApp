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

private const val IVA_RATE = 0.16   // 16% de IVA

fun generarPdfCotizacion(
    context: Context,
    cotizacion: Cotizacion
): File? {

    // Tamaño A4 en puntos (aprox. 72 dpi)
    val pageWidth = 595
    val pageHeight = 842

    val pdfDocument = PdfDocument()
    val margin = 32f
    val headerBarHeight = 90f
    val bottomBarHeight = 50f  // Reducido porque ya no hay franja negra

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ======================= UTILIDADES =======================

    // Función para capitalizar texto correctamente (Primera Letra De Cada Palabra)
    fun capitalizeWords(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }

    // Recorta bordes totalmente transparentes del bitmap
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

    // Wrap de texto para celdas
    fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) {
                word
            } else {
                currentLine.toString() + " " + word
            }

            if (paint.measureText(candidate) <= maxWidth) {
                currentLine.clear()
                currentLine.append(candidate)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines
    }

    // ======================= ENCABEZADO (LOGOS + LEMA) =======================
    fun drawHeader(canvas: Canvas) {
        try {
            val options = BitmapFactory.Options().apply { inScaled = false }

            // Logo Hurricane (izquierda)
            val rawLogo = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.logo_header_new,
                options
            )
            val croppedLogo = cropTransparent(rawLogo)

            val targetHeight = 45f
            val aspectRatio = croppedLogo.width.toFloat() / croppedLogo.height.toFloat()
            val destHeight = targetHeight
            val destWidth = destHeight * aspectRatio

            val bandTop = 0f
            val bandBottom = headerBarHeight
            val bandCenterY = (bandTop + bandBottom) / 2f

            val logoTop = bandCenterY - destHeight / 2f
            val logoLeft = margin

            val destRect = RectF(
                logoLeft,
                logoTop,
                logoLeft + destWidth,
                logoTop + destHeight
            )
            canvas.drawBitmap(croppedLogo, null, destRect, null)

            // Logo Made in USA (derecha)
            val rawUsa = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.made_in_usa,
                options
            )
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

            // Lema centrado
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

    // ======================= CAJA DEL FOLIO =======================
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

    // ======================= FOOTER (SIN FRANJA NEGRA) =======================
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

        // Pintar iconos en gris oscuro (sin franja negra)
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

        fun iconCenterYForText(baselineY: Float): Float =
            baselineY - textPaint.textSize / 2f + 1f

        // ===== LADO IZQUIERDO =====
        val leftStartX = margin
        val leftIconCenterX = leftStartX + iconSize / 2f
        val leftTextStartX = leftStartX + iconSize + 4f

        drawIcon(iconMail, leftIconCenterX, iconCenterYForText(line1Y), iconSize, iconPaintGray)
        canvas.drawText(email1, leftTextStartX, line1Y, textPaint)

        drawIcon(iconMail, leftIconCenterX, iconCenterYForText(line2Y), iconSize, iconPaintGray)
        canvas.drawText(email2, leftTextStartX, line2Y, textPaint)

        drawIcon(iconLocation, leftIconCenterX, iconCenterYForText(line3Y), iconSize, iconPaintGray)
        canvas.drawText(address, leftTextStartX, line3Y, textPaint)

        // ===== LADO DERECHO (Alineado al margen derecho) =====
        val rightTextEndX = pageWidth.toFloat() - margin

        // Web - primera línea derecha
        textPaint.textAlign = Paint.Align.LEFT
        val webTextWidth = textPaint.measureText(web)
        val webIconX = rightTextEndX - webTextWidth - iconSize - 4f
        drawIcon(iconWeb, webIconX + iconSize / 2f, iconCenterYForText(line1Y), iconSize, iconPaintGray)
        canvas.drawText(web, webIconX + iconSize + 4f, line1Y, textPaint)

        // Teléfonos - segunda línea derecha
        val phoneTextWidth = textPaint.measureText(phone)
        val phoneIconX = rightTextEndX - phoneTextWidth - iconSize - 4f
        drawIcon(iconWhatsRaw, phoneIconX + iconSize / 2f, iconCenterYForText(line2Y), iconSize, iconPaintGray)
        canvas.drawText(phone, phoneIconX + iconSize + 4f, line2Y, textPaint)

        // Redes sociales - tercera línea derecha
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

    // ======================= PRODUCTOS SELECCIONADOS =======================
    // CORREGIDO: Ahora lee correctamente los productos de la cotización
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

    // ======================= CONFIG TABLA =======================
    val tableLeft = margin
    val tableRight = pageWidth.toFloat() - margin
    val headerHeight = 42f
    val bodyTextSize = 9f
    val cellPadding = 4f
    val cellLineHeight = 12f

    // Columna de número de apertura
    val colNumeroW = 25f
    val colAreaW = 130f
    val colAreaTotalW = 50f
    val colMontajeW = 65f
    val colAdecuacionesW = 65f

    val priceColumnsCount = productosSeleccionados.size.coerceAtLeast(1)
    val colPricesTotalW = tableRight - tableLeft -
            (colNumeroW + colAreaW + colAreaTotalW + colMontajeW + colAdecuacionesW)
    val colPriceW = colPricesTotalW / priceColumnsCount

    val startPreciosX = tableLeft + colNumeroW + colAreaW + colAreaTotalW + colMontajeW + colAdecuacionesW

    // ======================= PRE-CÁLCULO DE FILAS =======================
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
            val monto = v.subtotalPorProducto(producto)
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

        filas.add(
            RowLayout(
                linesArea,
                linesAreaTotal,
                linesMontaje,
                linesAdecuaciones,
                linesPreciosPorProducto,
                rowHeightDynamic
            )
        )
    }

    // Espacio para Resumen + Condiciones
    val condLineCount = 14
    val condLineHeight = 7.5f
    val condicionesMinBlock = 16f + condLineCount * condLineHeight + 45f
    val resumenBlockHeight = 18f * 5
    val extraSpaceNeededLastPage = condicionesMinBlock + resumenBlockHeight + 16f

    // ======================= EMPEZAR A DIBUJAR =======================
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    drawHeader(canvas)

    // Título
    paint.color = Color.BLACK
    paint.textSize = 16f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER

    val titulo = "COTIZACIÓN DE PROYECTO"
    val tituloY = 105f

    canvas.drawText(titulo, pageWidth / 2f, tituloY, paint)
    drawFolioBox(canvas, tituloY)

    // ======================= DATOS DEL CLIENTE =======================
    paint.textAlign = Paint.Align.LEFT

    var y = 140f

    val leftX = margin
    // Bloques más anchos para que quepa la dirección
    val leftBlockWidth = 270f
    val rightBlockWidth = 230f
    val rightX = pageWidth.toFloat() - margin - rightBlockWidth

    // Función mejorada para dibujar filas de info con múltiples líneas
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

        // Configurar paint para medir texto
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        // Capitalizar el texto y hacer wrap
        val capitalizedValue = capitalizeWords(value)
        val wrappedLines = wrapText(capitalizedValue, valueW - paddingX * 2, paint)

        // Calcular altura dinámica
        val numLines = wrappedLines.size.coerceAtLeast(1)
        val rowH = (numLines * lineHeightText + 8f).coerceAtLeast(18f)
        val rowGap = 6f

        val yBottom = yTop + rowH

        // Bloque negro (label)
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvasRef.drawRect(x, yTop, x + labelW, yBottom, paint)

        // Texto del label
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8f
        paint.textAlign = Paint.Align.LEFT
        val labelY = yTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f
        canvasRef.drawText(label, x + paddingX, labelY, paint)

        // Celda valor (fondo blanco + borde)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvasRef.drawRect(valueX, yTop, valueX + valueW, yBottom, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = 0.6f
        canvasRef.drawRect(valueX, yTop, valueX + valueW, yBottom, paint)

        // Texto del valor (con múltiples líneas)
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

    // Construir dirección completa
    val direccionCompleta = buildString {
        if (cotizacion.ciudad.isNotBlank()) {
            append(cotizacion.ciudad)
        }
        val partes = cotizacion.ubicacion.split(",").map { it.trim() }
        partes.forEach { parte ->
            if (parte.isNotBlank() && parte != cotizacion.ciudad) {
                if (isNotEmpty()) append(", ")
                append(parte)
            }
        }
    }.ifBlank { cotizacion.ubicacion }

    var leftY = y
    var rightY = y

    // IZQUIERDA - Con soporte multilinea
    leftY = drawInfoRowMultiline(canvas, leftX, leftY, "Nombre del Cliente:", cotizacion.clienteNombre, leftBlockWidth)
    leftY = drawInfoRowMultiline(canvas, leftX, leftY, "Dirección:", direccionCompleta, leftBlockWidth)

    // DERECHA
    val metrajeFinal = cotizacion.ventanas.sumOf { it.areaM2 }
    rightY = drawInfoRowMultiline(canvas, rightX, rightY, "Especialista:", cotizacion.especialista, rightBlockWidth)
    rightY = drawInfoRowMultiline(canvas, rightX, rightY, "Fecha:", cotizacion.fecha, rightBlockWidth)
    rightY = drawInfoRowMultiline(canvas, rightX, rightY, "Metraje Total:", "%.2f m²".format(metrajeFinal), rightBlockWidth)

    y = maxOf(leftY, rightY) + 12f

    // ======================= ENCABEZADO DE TABLA =======================
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

        // Columnas de productos: SOLO HS-875, HS-1250, HS-1500
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

    paint.textSize = bodyTextSize
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textAlign = Paint.Align.CENTER

    val maxTableBottomPerPage = pageHeight - bottomBarHeight - 20f

    // ======================= DIBUJAR FILAS =======================
    filas.forEachIndexed { index, rowLayout ->
        val isLastRow = index == filas.lastIndex
        val extraNeeded = if (isLastRow) extraSpaceNeededLastPage else 0f

        if (y + rowLayout.height + extraNeeded > maxTableBottomPerPage) {
            drawFooter(canvas)
            pdfDocument.finishPage(page)

            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas

            drawHeader(canvas)

            y = 95f
            y = drawTableHeader(y)

            paint.textSize = bodyTextSize
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textAlign = Paint.Align.CENTER
        }

        var x = tableLeft

        fun drawBodyCell(lines: List<String>, width: Float) {
            paint.style = Paint.Style.STROKE
            paint.color = Color.LTGRAY
            paint.strokeWidth = 0.5f
            canvas.drawRect(x, y, x + width, y + rowLayout.height, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.CENTER

            val centerX = x + width / 2f
            val totalTextHeight = lines.size * cellLineHeight
            var textY = y + (rowLayout.height - totalTextHeight) / 2f + cellLineHeight - 3f

            lines.forEach { line ->
                canvas.drawText(line, centerX, textY, paint)
                textY += cellLineHeight
            }

            x += width
        }

        // COLUMNA DE NÚMERO DE APERTURA
        drawBodyCell(listOf("#${index + 1}"), colNumeroW)

        drawBodyCell(rowLayout.linesArea, colAreaW)
        drawBodyCell(rowLayout.linesAreaTotal, colAreaTotalW)
        drawBodyCell(rowLayout.linesMontaje, colMontajeW)
        drawBodyCell(rowLayout.linesAdecuaciones, colAdecuacionesW)

        rowLayout.linesPreciosPorProducto.forEach { priceLines ->
            drawBodyCell(priceLines, colPriceW)
        }

        y += rowLayout.height
    }

    // ======================= RESUMEN DE PRECIOS =======================
    val totalesPorProducto: Map<TipoProducto, Double> =
        productosSeleccionados.associateWith { producto ->
            cotizacion.totalPorProducto(producto)
        }

    val areaTotal = cotizacion.ventanas.sumOf { it.areaM2 }

    fun descuentoPorM2(producto: TipoProducto): Double = when (producto) {
        TipoProducto.HS875 -> cotizacion.descuentoHS875
        TipoProducto.HS1250 -> cotizacion.descuentoHS1250
        TipoProducto.HS1500 -> cotizacion.descuentoHS1500
        TipoProducto.PERSONALIZADO -> cotizacion.descuentoDolaresPorM2
    }

    val descuentosPorcentaje: Map<TipoProducto, Double> =
        productosSeleccionados.associateWith { producto ->
            val subtotalProducto = totalesPorProducto[producto] ?: 0.0
            val descM2 = descuentoPorM2(producto)

            if (areaTotal <= 0.0 || subtotalProducto <= 0.0 || descM2 <= 0.0) {
                0.0
            } else {
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

    val labelWidth = 110f
    val boxLeft = startPreciosX - labelWidth
    val boxRight = tableRight

    val valueColumnsCount = productosSeleccionados.size
    val valueColumnWidth = colPriceW

    val rowHeightResumen = 16f
    val totalResumenRows = 5
    val bloqueAltura = rowHeightResumen * totalResumenRows

    val footerTop = pageHeight.toFloat() - bottomBarHeight

    val margenSobreFooter = 10f
    val resumenTopDesdeAbajo = pageHeight.toFloat() - bottomBarHeight - margenSobreFooter - bloqueAltura

    val espacioEntreTablaYResumen = 20f
    val resumenTop = maxOf(y + espacioEntreTablaYResumen, resumenTopDesdeAbajo)

    var filaTop = resumenTop

    fun drawResumenDataRow(
        label: String,
        getTextForCol: (Int, TipoProducto) -> String,
        boldLabel: Boolean
    ) {
        val filaBottom = filaTop + rowHeightResumen

        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawRect(boxLeft, filaTop, boxLeft + labelWidth, filaBottom, paint)

        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = if (label.length > 18) 7f else if (label.length > 14) 8f else 9f
        paint.typeface = if (boldLabel)
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        else
            Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        canvas.drawText(label, boxLeft + 4f, filaBottom - 4f, paint)

        productosSeleccionados.forEachIndexed { index, producto ->
            val left = boxLeft + labelWidth + index * valueColumnWidth
            val right = left + valueColumnWidth

            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            canvas.drawRect(left, filaTop, right, filaBottom, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.RIGHT

            val text = getTextForCol(index, producto)
            paint.textSize = if (text.length > 12) 7.5f else 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            canvas.drawText(text, right - 4f, filaBottom - 4f, paint)
        }

        filaTop = filaBottom
    }

    // FILA 1: Subtotal
    drawResumenDataRow(
        label = "Subtotal",
        getTextForCol = { _, producto ->
            val subtotalProducto = totalesPorProducto[producto] ?: 0.0
            "$ " + "%,.2f".format(subtotalProducto)
        },
        boldLabel = true
    )

    // FILA 2: Total IVA sin descuento
    drawResumenDataRow(
        label = "Total IVA sin descuento",
        getTextForCol = { _, producto ->
            val subtotalProducto = totalesPorProducto[producto] ?: 0.0
            val subtotalConIvaSinDesc = subtotalProducto * (1.0 + IVA_RATE)
            "$ " + "%,.2f".format(subtotalConIvaSinDesc)
        },
        boldLabel = false
    )

    // FILA 3: Descuento (%)
    drawResumenDataRow(
        label = "Descuento",
        getTextForCol = { _, producto ->
            val pct = descuentosPorcentaje[producto] ?: 0.0
            String.format("%.2f%%", pct)
        },
        boldLabel = false
    )

    // FILA 4: Subtotal con descuento
    drawResumenDataRow(
        label = "Subtotal con descuento",
        getTextForCol = { _, producto ->
            val subtotalConDescSinIva = preciosFinalesPorProducto[producto] ?: 0.0
            "$ " + "%,.2f".format(subtotalConDescSinIva)
        },
        boldLabel = true
    )

    // FILA 5: Precio Final con IVA
    drawResumenDataRow(
        label = "Precio Final con IVA",
        getTextForCol = { _, producto ->
            val precioSinIva = preciosFinalesPorProducto[producto] ?: 0.0
            val precioConIva = precioSinIva * (1.0 + IVA_RATE)
            "$ " + "%,.2f".format(precioConIva)
        },
        boldLabel = true
    )

    // ======================= CONDICIONES COMERCIALES =======================
    val condicionesLeft = margin
    val condicionesRight = boxLeft - 12f
    val maxCondicionesWidth = condicionesRight - condicionesLeft

    val condicionesLineas = listOf(
        "Precios cotizados en Dólares Americanos.",
        "Los precios ya incluyen IVA.",
        "Para pago en Moneda Nacional aplicará el T.C. vigente al día de pago según Banco de México.",
        "Se requiere 50% anticipo para la programación de instalación.",
        "No hay reembolso por Cancelación después de 3 días del pago de anticipo.",
        "Vigencia de la cotización: 15 días.",
        "El precio incluye instalación dentro de la zona continental de Quintana Roo. Para proyectos ubicados fuera de esta zona se hará un cargo extra por concepto de viáticos.",
        "Las medidas contempladas en esta propuesta pueden variar después de la rectificación.",
        "La instalación se programará con base en la agenda y todo proyecto entrará a una fila de instalación. Los tiempos de instalación serán de acuerdo a las fechas que se tengan programadas. En caso de existir algún espacio disponible antes del periodo máximo, se le notificará al cliente.",
        "Aplicará la garantía de acuerdo al Sistema Contratado y siempre y cuando se cumplan los cuidados y recomendaciones entregadas al término de la instalación.",
        "Los descuentos concedidos en esta cotización podrán modificarse si el metraje total disminuye o se cancela alguna área.",
        "El costo de adecuaciones o modificaciones estructurales como instalación de PTR o cajillos en prefabricados NO ESTÁN INCLUIDOS."
    )

    val tituloExtra = 18f
    val neededHeight = tituloExtra + condicionesLineas.size * condLineHeight

    val margenSobreFooterCond = 56.5f
    val condicionesTop = minOf(resumenTop, footerTop - neededHeight - margenSobreFooterCond)

    paint.textAlign = Paint.Align.LEFT
    paint.color = Color.BLACK
    paint.textSize = 8.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    var condicionesY = condicionesTop + 12f
    canvas.drawText("Condiciones Comerciales:", condicionesLeft, condicionesY, paint)

    paint.textSize = 6.5f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
    condicionesY += condLineHeight

    fun drawConditionWithNumber(index: Int, text: String, startY: Float): Float {
        val numberPrefix = "$index.- "
        val numberWidth = paint.measureText(numberPrefix)
        val maxTextWidth = maxCondicionesWidth - numberWidth

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = ""

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) > maxTextWidth) {
                if (current.isNotEmpty()) {
                    lines.add(current)
                }
                current = word
            } else {
                current = candidate
            }
        }
        if (current.isNotEmpty()) lines.add(current)

        var currentY = startY

        if (lines.isNotEmpty()) {
            canvas.drawText(numberPrefix, condicionesLeft, currentY, paint)
            canvas.drawText(lines[0], condicionesLeft + numberWidth, currentY, paint)
            currentY += condLineHeight

            for (i in 1 until lines.size) {
                canvas.drawText(lines[i], condicionesLeft + numberWidth, currentY, paint)
                currentY += condLineHeight
            }
        }
        return currentY
    }

    condicionesLineas.forEachIndexed { index, linea ->
        condicionesY = drawConditionWithNumber(index + 1, linea, condicionesY)
    }

    drawFooter(canvas)
    pdfDocument.finishPage(page)

    // ======================= GUARDAR ARCHIVO =======================
    val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    if (docsDir == null) {
        pdfDocument.close()
        return null
    }

    if (!docsDir.exists()) {
        docsDir.mkdirs()
    }

    val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val fileName = "Cotizacion_${timeStamp}_${System.currentTimeMillis()}.pdf"
    val file = File(docsDir, fileName)

    return try {
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        // ✅ ENCOLAR ANTES DE REGRESAR
        UploadQueueStorage.enqueue(
            context,
            PendingUpload(
                id = java.util.UUID.randomUUID().toString(),
                cotizacionId = cotizacion.id.toString(),
                clienteNombre = cotizacion.clienteNombre,
                createdByNombre = SessionManager.getNombre(context),// ✅ AQUÍ
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