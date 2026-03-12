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

private const val RAIN_IVA_RATE = 0.16

fun generarPdfRainCotizacion(
    context: Context,
    cotizacion: CotizacionRain,
    skipEnqueue: Boolean = false
): File? {

    val pageWidth = 595
    val pageHeight = 842

    val pdfDocument = PdfDocument()
    val margin = 32f
    val headerBarHeight = 90f
    val bottomBarHeight = 45f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ═══════════════════════════════════════════════════════════════════════════
    // COLORES PRINCIPALES
    // ═══════════════════════════════════════════════════════════════════════════
    val colorGrayCell = Color.parseColor("#C4C4C4")
    val colorBlue = Color.parseColor("#2984D1")

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

    // ═══════════════════════════════════════════════════════════════════════════
    // HEADER
    // ═══════════════════════════════════════════════════════════════════════════

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
            val usaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }
            canvas.drawBitmap(usaCropped, null, usaRect, usaPaint)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FOLIO BOX
    // ═══════════════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════════════
    // FOOTER
    // ═══════════════════════════════════════════════════════════════════════════

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
            android.util.Log.e("PdfRainGenerator", "Error cargando footer: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEPARADOR DOBLE LÍNEA (gris + azul)
    // ═══════════════════════════════════════════════════════════════════════════

    fun drawDoubleLineSeparator(canvas: Canvas, yStart: Float): Float {
        paint.style = Paint.Style.STROKE
        // Línea gris (#C4C4C4) arriba - más gruesa
        paint.color = colorGrayCell
        paint.strokeWidth = 2.5f
        canvas.drawLine(margin, yStart, pageWidth.toFloat() - margin, yStart, paint)
        // Línea azul (#2984D1) abajo - más delgada
        paint.color = colorBlue
        paint.strokeWidth = 1.5f
        canvas.drawLine(margin, yStart + 4f, pageWidth.toFloat() - margin, yStart + 4f, paint)
        paint.style = Paint.Style.FILL
        return yStart + 8f
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE TABLA
    // ═══════════════════════════════════════════════════════════════════════════

    val tableLeft = margin
    val tableRight = pageWidth.toFloat() - margin
    val tableWidth = tableRight - tableLeft
    val bodyTextSize = 9f
    val cellPadding = 4f
    val cellLineHeight = 12f

    val colNumeroW = 25f
    val colAreaW = 100f
    val colCantidadW = 40f
    val colAnchoW = 42f
    val colAltoW = 42f
    val colM2W = 48f
    val colManualW = 45f
    val colElectricoW = 50f
    val colComentariosW = tableWidth - colNumeroW - colAreaW - colCantidadW - colAnchoW - colAltoW - colM2W - colManualW - colElectricoW - 65f
    val colTotalW = 65f

    // ═══════════════════════════════════════════════════════════════════════════
    // PREPARAR FILAS DE DATOS
    // ═══════════════════════════════════════════════════════════════════════════

    data class RainRowLayout(
        val descripcion: String,
        val ancho: String,
        val alto: String,
        val m2: String,
        val piezas: String,
        val esManual: Boolean,
        val esElectrico: Boolean,
        val subtotal: String,
        val linesDescripcion: List<String>,
        val height: Float
    )

    paint.textSize = bodyTextSize
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.letterSpacing = 0f

    val filas = cotizacion.medidas.map { medida ->
        val txtDesc = capitalizeWords(medida.descripcion)
        val txtAncho = "%.2f".format(medida.ancho)
        val txtAlto = "%.2f".format(medida.alto)
        val txtM2 = "%.2f".format(medida.areaM2)
        val txtSubtotal = "$ %,.2f".format(medida.subtotal)
        val txtPiezas = medida.piezas.toString()

        val linesDesc = wrapText(txtDesc, colAreaW - cellPadding * 2, paint)
        val rowH = linesDesc.size * cellLineHeight + 4f

        RainRowLayout(
            descripcion = txtDesc,
            ancho = txtAncho,
            alto = txtAlto,
            m2 = txtM2,
            piezas = txtPiezas,
            esManual = medida.tipoMecanismo == TipoMecanismo.MANUAL,
            esElectrico = medida.tipoMecanismo == TipoMecanismo.ELECTRICO,
            subtotal = txtSubtotal,
            linesDescripcion = linesDesc,
            height = rowH.coerceAtLeast(cellLineHeight + 4f)
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CÁLCULOS DE RESUMEN
    // ═══════════════════════════════════════════════════════════════════════════

    val subtotalGeneral = cotizacion.subtotal
    val descuentoPorcentaje = cotizacion.descuentoPorcentaje
    val descuentoMonto = cotizacion.descuentoMonto
    val subtotalConDescuento = subtotalGeneral - descuentoMonto
    val iva = subtotalConDescuento * RAIN_IVA_RATE
    val totalFinal = subtotalConDescuento + iva

    // ═══════════════════════════════════════════════════════════════════════════
    // DIMENSIONES PAGINACIÓN
    // ═══════════════════════════════════════════════════════════════════════════

    val headerTableRow1H = 20f
    val headerTableRow2H = 20f
    val headerTableTotalH = headerTableRow1H + headerTableRow2H

    val resumenBlockHeight = 14f * 6
    val extraSpaceNeededLastPage = resumenBlockHeight + 80f

    // ═══════════════════════════════════════════════════════════════════════════
    // CREAR PRIMERA PÁGINA
    // ═══════════════════════════════════════════════════════════════════════════

    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    drawHeader(canvas)

    // ═══════════════════════════════════════════════════════════════════════════
    // TÍTULO: "Instalación de Protección Contra Lluvia"
    // ═══════════════════════════════════════════════════════════════════════════

    paint.color = Color.BLACK
    paint.textSize = 14f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    paint.letterSpacing = 0f
    val tituloY = 105f
    canvas.drawText("INSTALACIÓN DE PROTECCIÓN CONTRA LLUVIA", pageWidth / 2f, tituloY, paint)
    drawFolioBox(canvas, tituloY)
    paint.textAlign = Paint.Align.LEFT

    // ═══════════════════════════════════════════════════════════════════════════
    // INFO DEL CLIENTE — Labels azul #2984D1 sin fondo, valores fondo #C4C4C4
    // ═══════════════════════════════════════════════════════════════════════════

    var y = 130f
    val leftX = margin
    val leftBlockWidth = 270f
    val rightBlockWidth = 230f
    val rightX = pageWidth.toFloat() - margin - rightBlockWidth

    fun drawInfoRow(
        canvasRef: Canvas, x: Float, yTop: Float,
        label: String, value: String, blockW: Float
    ): Float {
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

        // Label: SIN fondo (transparente), texto azul #2984D1 bold
        paint.style = Paint.Style.FILL
        paint.color = colorBlue
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 8f
        paint.textAlign = Paint.Align.LEFT
        canvasRef.drawText(label, x + paddingX, yTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        // Value: fondo gris #C4C4C4, borde azul #2984D1
        paint.style = Paint.Style.FILL; paint.color = colorGrayCell
        canvasRef.drawRect(valueX, yTop, valueX + valueW, yBottom, paint)
        paint.style = Paint.Style.STROKE; paint.color = colorBlue; paint.strokeWidth = 0.6f
        canvasRef.drawRect(valueX, yTop, valueX + valueW, yBottom, paint)

        // Texto del valor: negro normal
        paint.style = Paint.Style.FILL; paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.textSize = 8f
        val totalTextHeight = numLines * lineHeightText
        var textY = yTop + (rowH - totalTextHeight) / 2f + lineHeightText - paint.descent()
        wrappedLines.forEach { line ->
            canvasRef.drawText(line, valueX + paddingX, textY, paint)
            textY += lineHeightText
        }
        return yBottom + 6f
    }

    var leftY = y; var rightY = y
    leftY = drawInfoRow(canvas, leftX, leftY, "Nombre del Cliente:", cotizacion.clienteNombre, leftBlockWidth)

    if (cotizacion.ciudad.isNotBlank()) {
        leftY = drawInfoRow(canvas, leftX, leftY, "Ciudad:", cotizacion.ciudad, leftBlockWidth)
    }

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

    if (colonia.isNotBlank()) leftY = drawInfoRow(canvas, leftX, leftY, "Colonia:", colonia, leftBlockWidth)
    if (calleNumero.isNotBlank()) leftY = drawInfoRow(canvas, leftX, leftY, "Calle y Número:", calleNumero, leftBlockWidth)

    val totalAreasM2 = cotizacion.medidas.sumOf { it.areaM2 }
    rightY = drawInfoRow(canvas, rightX, rightY, "Especialista:", cotizacion.especialista, rightBlockWidth)
    rightY = drawInfoRow(canvas, rightX, rightY, "Fecha:", cotizacion.fecha, rightBlockWidth)
    rightY = drawInfoRow(canvas, rightX, rightY, "Total Áreas:", "${cotizacion.medidas.size}", rightBlockWidth)
    rightY = drawInfoRow(canvas, rightX, rightY, "m² Totales:", "%.2f M²".format(totalAreasM2), rightBlockWidth)

    y = maxOf(leftY, rightY) + 8f

    // ═══════════════════════════════════════════════════════════════════════════
    // DOBLE LÍNEA + "PRESUPUESTO" centrado, bold, subrayado
    // ═══════════════════════════════════════════════════════════════════════════

    y = drawDoubleLineSeparator(canvas, y)
    y += 4f

    paint.color = Color.BLACK
    paint.textSize = 12f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    paint.letterSpacing = 0f
    val presupuestoY = y + 12f
    canvas.drawText("PRESUPUESTO", pageWidth / 2f, presupuestoY, paint)

    // Subrayado
    val textW = paint.measureText("PRESUPUESTO")
    paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; paint.strokeWidth = 0.8f
    canvas.drawLine(pageWidth / 2f - textW / 2f, presupuestoY + 3f, pageWidth / 2f + textW / 2f, presupuestoY + 3f, paint)
    paint.style = Paint.Style.FILL; paint.textAlign = Paint.Align.LEFT

    y = presupuestoY + 14f

    // ═══════════════════════════════════════════════════════════════════════════
    // TABLA — Gris #C4C4C4, sub-celdas blancas, TOTAL azul #2984D1
    // ═══════════════════════════════════════════════════════════════════════════

    fun drawRainTableHeader(startY: Float): Float {
        paint.letterSpacing = 0f
        paint.textAlign = Paint.Align.CENTER
        paint.style = Paint.Style.FILL

        val row1Top = startY
        val row1Bottom = startY + headerTableRow1H
        val row2Top = row1Bottom
        val row2Bottom = row2Top + headerTableRow2H

        val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; color = colorGrayCell; strokeWidth = 0.5f
        }

        // # (gris, vacío)
        paint.color = colorGrayCell
        canvas.drawRect(tableLeft, row1Top, tableLeft + colNumeroW, row2Bottom, paint)
        canvas.drawRect(tableLeft, row1Top, tableLeft + colNumeroW, row2Bottom, borderP)

        // Área a Proteger (gris, texto azul #2984D1)
        val areaX = tableLeft + colNumeroW
        paint.color = colorGrayCell
        canvas.drawRect(areaX, row1Top, areaX + colAreaW, row2Bottom, paint)
        canvas.drawRect(areaX, row1Top, areaX + colAreaW, row2Bottom, borderP)
        paint.color = colorBlue
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 8f
        canvas.drawText("Área a Proteger", areaX + colAreaW / 2f, row1Top + headerTableTotalH / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        // Cantidad (gris, texto azul)
        val cantX = areaX + colAreaW
        paint.color = colorGrayCell
        canvas.drawRect(cantX, row1Top, cantX + colCantidadW, row2Bottom, paint)
        canvas.drawRect(cantX, row1Top, cantX + colCantidadW, row2Bottom, borderP)
        paint.color = colorBlue
        paint.textSize = 8f
        canvas.drawText("Cantidad", cantX + colCantidadW / 2f, row1Top + headerTableTotalH / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        // Tela (fila 1 gris, texto azul)
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val telaX = cantX + colCantidadW
        val telaW = colAnchoW + colAltoW + colM2W
        paint.color = colorGrayCell
        canvas.drawRect(telaX, row1Top, telaX + telaW, row1Bottom, paint)
        canvas.drawRect(telaX, row1Top, telaX + telaW, row1Bottom, borderP)
        paint.color = colorBlue
        paint.textSize = 9f
        canvas.drawText("Tela", telaX + telaW / 2f, row1Top + headerTableRow1H / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        // Sub-columnas Tela (fila 2 blanco, texto azul)
        listOf(
            Triple(telaX, colAnchoW, "Ancho"),
            Triple(telaX + colAnchoW, colAltoW, "Alto"),
            Triple(telaX + colAnchoW + colAltoW, colM2W, "m² Total")
        ).forEach { (x, w, text) ->
            paint.style = Paint.Style.FILL; paint.color = Color.WHITE
            canvas.drawRect(x, row2Top, x + w, row2Bottom, paint)
            canvas.drawRect(x, row2Top, x + w, row2Bottom, borderP)
            paint.style = Paint.Style.FILL; paint.color = colorBlue; paint.textSize = 7.5f
            canvas.drawText(text, x + w / 2f, row2Top + headerTableRow2H / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
        }

        // Mecanismo (fila 1 gris, texto azul)
        val mecX = telaX + telaW
        val mecW = colManualW + colElectricoW
        paint.color = colorGrayCell
        canvas.drawRect(mecX, row1Top, mecX + mecW, row1Bottom, paint)
        canvas.drawRect(mecX, row1Top, mecX + mecW, row1Bottom, borderP)
        paint.color = colorBlue; paint.textSize = 9f
        canvas.drawText("Mecanismo", mecX + mecW / 2f, row1Top + headerTableRow1H / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        // Sub-columnas Mecanismo (fila 2 blanco, texto azul)
        listOf(
            Triple(mecX, colManualW, "Manual"),
            Triple(mecX + colManualW, colElectricoW, "Eléctrico")
        ).forEach { (x, w, text) ->
            paint.style = Paint.Style.FILL; paint.color = Color.WHITE
            canvas.drawRect(x, row2Top, x + w, row2Bottom, paint)
            canvas.drawRect(x, row2Top, x + w, row2Bottom, borderP)
            paint.style = Paint.Style.FILL; paint.color = colorBlue; paint.textSize = 7.5f
            canvas.drawText(text, x + w / 2f, row2Top + headerTableRow2H / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
        }

        // Comentarios (gris completo, texto azul)
        val comX = mecX + mecW
        paint.style = Paint.Style.FILL; paint.color = colorGrayCell
        canvas.drawRect(comX, row1Top, comX + colComentariosW, row2Bottom, paint)
        canvas.drawRect(comX, row1Top, comX + colComentariosW, row2Bottom, borderP)
        paint.color = colorBlue; paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas.drawText("Comentarios", comX + colComentariosW / 2f, row1Top + headerTableTotalH / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        // TOTAL (AZUL #2984D1, texto blanco)
        val totalX = comX + colComentariosW
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.style = Paint.Style.FILL; paint.color = colorBlue
        canvas.drawRect(totalX, row1Top, totalX + colTotalW, row2Bottom, paint)
        paint.color = Color.WHITE; paint.textSize = 9f
        canvas.drawText("TOTAL", totalX + colTotalW / 2f, row1Top + headerTableTotalH / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        return row2Bottom
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FILAS DE DATOS
    // ═══════════════════════════════════════════════════════════════════════════

    y = drawRainTableHeader(y)

    val totalFilas = filas.size

    filas.forEachIndexed { index, fila ->
        val rowH = fila.height
        val isLast = index == totalFilas - 1
        val footerTop = pageHeight.toFloat() - bottomBarHeight

        if (isLast) {
            if (y + rowH + extraSpaceNeededLastPage > footerTop - 10f) {
                drawFooter(canvas); pdfDocument.finishPage(page); pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo); canvas = page.canvas
                drawHeader(canvas); y = headerBarHeight + 20f; y = drawRainTableHeader(y)
            }
        } else {
            if (y + rowH > footerTop - 10f) {
                drawFooter(canvas); pdfDocument.finishPage(page); pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo); canvas = page.canvas
                drawHeader(canvas); y = headerBarHeight + 20f; y = drawRainTableHeader(y)
            }
        }

        val rowTop = y
        val rowBottom = y + rowH

        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, paint)

        paint.style = Paint.Style.STROKE; paint.color = Color.parseColor("#E0E0E0"); paint.strokeWidth = 0.5f
        canvas.drawLine(tableLeft, rowBottom, tableRight, rowBottom, paint)

        paint.style = Paint.Style.FILL; paint.color = Color.BLACK
        paint.textSize = bodyTextSize
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.letterSpacing = 0f

        val numCenterY = rowTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f
        var xCol = tableLeft

        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("#${index + 1}", xCol + colNumeroW / 2f, numCenterY, paint)
        xCol += colNumeroW

        val areaCenterX = xCol + colAreaW / 2f
        if (fila.linesDescripcion.size == 1) {
            canvas.drawText(fila.linesDescripcion.first(), areaCenterX, numCenterY, paint)
        } else {
            var textY = rowTop + cellPadding + cellLineHeight - paint.descent()
            fila.linesDescripcion.forEach { line ->
                canvas.drawText(line, areaCenterX, textY, paint); textY += cellLineHeight
            }
        }
        xCol += colAreaW

        canvas.drawText(fila.piezas, xCol + colCantidadW / 2f, numCenterY, paint)
        xCol += colCantidadW

        canvas.drawText(fila.ancho, xCol + colAnchoW / 2f, numCenterY, paint)
        xCol += colAnchoW

        canvas.drawText(fila.alto, xCol + colAltoW / 2f, numCenterY, paint)
        xCol += colAltoW

        canvas.drawText(fila.m2, xCol + colM2W / 2f, numCenterY, paint)
        xCol += colM2W

        // Mecanismo — texto centrado entre las 2 columnas (Manual + Eléctrico combinadas)
        val mecAreaX = xCol
        val mecAreaW = colManualW + colElectricoW
        val mecText = if (fila.esManual) "Manual" else "Eléctrico"
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(mecText, mecAreaX + mecAreaW / 2f, numCenterY, paint)
        xCol += mecAreaW

        xCol += colComentariosW

        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas.drawText(fila.subtotal, xCol + colTotalW - 6f, numCenterY, paint)
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        y = rowBottom
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESUMEN (Sub-Total 1, Descuento, Sub-Total 2, IVA, Total)
    // ═══════════════════════════════════════════════════════════════════════════

    val labelWidth = 180f
    val valueColumnWidth = 100f
    val rowHeightResumen = 14f
    val resumenTextSize = 9f

    val margenSobreFooter = 8f
    val resumenTopDesdeAbajo = pageHeight.toFloat() - bottomBarHeight - margenSobreFooter - rowHeightResumen * 5
    val resumenTop = maxOf(y + 15f, resumenTopDesdeAbajo)

    val resumenRight = tableRight
    val resumenLeft = resumenRight - labelWidth - valueColumnWidth
    var filaTop = resumenTop

    fun drawResumenRow(
        label: String, value: String, isBold: Boolean = false,
        drawLineAbove: Boolean = false, labelColor: Int = Color.BLACK, valueColor: Int = Color.BLACK
    ) {
        val filaBottom = filaTop + rowHeightResumen
        if (drawLineAbove) {
            paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; paint.strokeWidth = 1f
            canvas.drawLine(resumenLeft, filaTop, resumenRight, filaTop, paint)
        }
        paint.style = Paint.Style.FILL; paint.color = labelColor
        paint.textAlign = Paint.Align.LEFT; paint.textSize = resumenTextSize
        paint.typeface = if (isBold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.letterSpacing = 0f
        canvas.drawText(label, resumenLeft + 2f, filaTop + rowHeightResumen / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
        paint.color = valueColor; paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(value, resumenRight - 8f, filaTop + rowHeightResumen / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
        filaTop = filaBottom
    }

    drawResumenRow("Sub-Total 1", "$ %,.2f".format(subtotalGeneral), drawLineAbove = true)
    drawResumenRow("Descuento  -%.2f%%".format(descuentoPorcentaje), "-$ %,.2f".format(descuentoMonto), labelColor = Color.RED, valueColor = Color.RED)
    drawResumenRow("Sub-Total 2", "$ %,.2f".format(subtotalConDescuento))
    drawResumenRow("IVA", "$ %,.2f".format(iva))
    drawResumenRow("Total", "$ %,.2f".format(totalFinal), isBold = true, drawLineAbove = true)

    // ═══════════════════════════════════════════════════════════════════════════
    // CONDICIONES COMERCIALES
    // ═══════════════════════════════════════════════════════════════════════════

    val condicionesLeft = margin
    val condicionesRight = resumenLeft - 10f
    val condicionesAvailableWidth = condicionesRight - condicionesLeft
    val resumenBottom = filaTop
    val condicionesAvailableHeight = (resumenBottom - resumenTop) * 1.15f

    try {
        val options = BitmapFactory.Options().apply { inScaled = false }
        val condicionesImg = BitmapFactory.decodeResource(context.resources, R.drawable.condiciones_comerciales, options)
        if (condicionesImg != null) {
            val imgW = condicionesImg.width.toFloat()
            val imgH = condicionesImg.height.toFloat()
            val imgAR = imgW / imgH
            var fW = condicionesAvailableWidth * 2f
            var fH = fW / imgAR
            if (fH > condicionesAvailableHeight) { fH = condicionesAvailableHeight; fW = fH * imgAR }
            canvas.drawBitmap(condicionesImg, null, RectF(condicionesLeft, resumenTop - 5f, condicionesLeft + fW, resumenTop - 5f + fH), null)
        }
    } catch (e: Exception) {
        android.util.Log.e("PdfRainGenerator", "Error cargando condiciones: ${e.message}")
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FOOTER Y GUARDAR
    // ═══════════════════════════════════════════════════════════════════════════

    drawFooter(canvas)
    pdfDocument.finishPage(page)

    val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    if (docsDir == null) { pdfDocument.close(); return null }
    if (!docsDir.exists()) docsDir.mkdirs()

    fun formatNameForFile(input: String): String {
        val normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return normalized.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            .joinToString("_") { word -> word.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() } }
            .replace("[^A-Za-z0-9_]+".toRegex(), "").take(30)
    }

    val clienteFormateado = formatNameForFile(cotizacion.clienteNombre)
    val folioParaNombre = cotizacion.folio.ifBlank { "SIN_FOLIO" }
    val fechaParaNombre = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    val fileName = "Rain_${clienteFormateado}_${folioParaNombre}_${fechaParaNombre}.pdf"
    val file = File(docsDir, fileName)

    return try {
        FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
        pdfDocument.close()
        if (!skipEnqueue) {
            UploadQueueStorage.enqueue(context, PendingUpload(
                id = java.util.UUID.randomUUID().toString(),
                cotizacionId = cotizacion.folio.ifBlank { cotizacion.id.toString() },
                clienteNombre = cotizacion.clienteNombre,
                createdByNombre = cotizacion.especialista,
                filePath = file.absolutePath,
                status = "PENDING"
            ))
        }
        file
    } catch (e: IOException) { e.printStackTrace(); pdfDocument.close(); null }
}