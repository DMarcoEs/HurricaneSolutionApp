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
    val headerBarHeight = 65f
    val bottomBarHeight = 55f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ═══════════════════════════════════════════════════════════════════════════
    // COLORES PRINCIPALES
    // ═══════════════════════════════════════════════════════════════════════════
    val colorGrayCell = Color.parseColor("#C4C4C4")
    val colorBlue = Color.parseColor("#2984D1")
    val colorBlueDark = Color.parseColor("#1B6AAA")
    val colorRed = Color.parseColor("#CC0000")

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
    // HEADER — Banner full-width (rain_header_banner)
    // ═══════════════════════════════════════════════════════════════════════════

    fun drawHeader(canvas: Canvas) {
        try {
            val options = BitmapFactory.Options().apply { inScaled = false }
            // Intenta cargar el banner Rain nuevo; si no existe, usa el anterior
            val headerId = context.resources.getIdentifier("rain_header_banner", "drawable", context.packageName)
            if (headerId != 0) {
                val bannerImg = BitmapFactory.decodeResource(context.resources, headerId, options)
                if (bannerImg != null) {
                    val imgAR = bannerImg.width.toFloat() / bannerImg.height.toFloat()
                    val finalWidth = pageWidth.toFloat()
                    val finalHeight = finalWidth / imgAR
                    val destRect = RectF(0f, 0f, finalWidth, finalHeight)
                    canvas.drawBitmap(bannerImg, null, destRect, null)
                    return
                }
            }
            // Fallback: logo_header_new + made_in_usa (diseño anterior)
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
    // FOOTER — Banner full-width (rain_footer_banner)
    // ═══════════════════════════════════════════════════════════════════════════

    fun drawFooter(canvas: Canvas) {
        try {
            val options = BitmapFactory.Options().apply { inScaled = false }
            // Intenta cargar el footer Rain nuevo; si no existe, usa el anterior
            val footerId = context.resources.getIdentifier("rain_footer_banner", "drawable", context.packageName)
            val resId = if (footerId != 0) footerId else R.drawable.footer_nuevo
            val footerImg = BitmapFactory.decodeResource(context.resources, resId, options)
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
        paint.color = colorGrayCell
        paint.strokeWidth = 2.5f
        canvas.drawLine(margin, yStart, pageWidth.toFloat() - margin, yStart, paint)
        paint.color = colorBlue
        paint.strokeWidth = 1.5f
        canvas.drawLine(margin, yStart + 4f, pageWidth.toFloat() - margin, yStart + 4f, paint)
        paint.style = Paint.Style.FILL
        return yStart + 8f
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE TABLA — Columnas según PDF de referencia
    // Sin columna # | Área | Cantidad | Tela(Ancho,Alto,m²) | Control(Manivela,Remoto) | Comentarios | TOTAL(Manual,Eléctrico)
    // ═══════════════════════════════════════════════════════════════════════════

    val tableLeft = margin
    val tableRight = pageWidth.toFloat() - margin
    val tableWidth = tableRight - tableLeft
    val bodyTextSize = 9f
    val cellPadding = 4f
    val cellLineHeight = 12f

    // Determinar si la cotización tiene Manual, Eléctrico o ambos
    val tieneManual = cotizacion.tieneManual()
    val tieneElectrico = cotizacion.tieneElectrico()
    val tieneAmbos = tieneManual && tieneElectrico

    // Anchos de columna según el PDF de referencia (sin Comentarios)
    val colAreaW = 115f
    val colCantidadW = 45f
    val colAnchoW = 45f
    val colAltoW = 45f
    val colM2W = 50f
    val colManivelaW = 50f
    val colRemotoW = 50f
    // TOTAL: si tiene ambos, dividir en 2 sub-columnas; si solo uno, 1 columna
    val colTotalManualW = if (tieneAmbos) 66f else 0f
    val colTotalElectricoW = if (tieneAmbos) 65f else 0f
    val colTotalUnicoW = if (!tieneAmbos) (tableWidth - colAreaW - colCantidadW - colAnchoW - colAltoW - colM2W - colManivelaW - colRemotoW) else 0f
    val totalColumnsW = if (tieneAmbos) colTotalManualW + colTotalElectricoW else colTotalUnicoW

    // ═══════════════════════════════════════════════════════════════════════════
    // PREPARAR FILAS DE DATOS
    // ═══════════════════════════════════════════════════════════════════════════

    data class RainRowLayout(
        val descripcion: String,
        val aperturaLabel: String,
        val ancho: String,
        val alto: String,
        val m2: String,
        val piezas: String,
        val manivelas: String,
        val remotos: String,
        val subtotalManual: String,
        val subtotalElectrico: String,
        val subtotalUnico: String,
        val linesDescripcion: List<String>,
        val height: Float
    )

    paint.textSize = bodyTextSize
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    paint.letterSpacing = 0f

    val filas = cotizacion.medidas.mapIndexed { index, medida ->
        val txtDesc = capitalizeWords(medida.descripcion)
        val txtAncho = "%.2f".format(medida.ancho)
        val txtAlto = "%.2f".format(medida.alto)
        val txtM2 = "%.2f".format(medida.areaM2)
        val txtPiezas = medida.piezas.toString()

        // Manivelas y remotos por apertura
        val manivelas = if (medida.incluyeManual) medida.piezas else 0
        val remotos = if (medida.incluyeElectrico) medida.piezas else 0

        // Precios separados
        val txtSubManual = if (medida.incluyeManual) "$ %,.2f".format(medida.subtotalManual) else ""
        val txtSubElectrico = if (medida.incluyeElectrico) "$ %,.2f".format(medida.subtotalElectrico) else ""
        // Precio único (cuando la cotización solo tiene un tipo)
        val txtSubUnico = if (!tieneAmbos) {
            "$ %,.2f".format(if (medida.incluyeManual) medida.subtotalManual else medida.subtotalElectrico)
        } else ""

        val linesDesc = wrapText(txtDesc, colAreaW - cellPadding * 2, paint)
        val rowH = (linesDesc.size * cellLineHeight + 4f).coerceAtLeast(cellLineHeight * 2 + 4f) // Mínimo 2 líneas para nombre + apertura

        RainRowLayout(
            descripcion = txtDesc,
            aperturaLabel = "Apertura ${index + 1}",
            ancho = txtAncho,
            alto = txtAlto,
            m2 = txtM2,
            piezas = txtPiezas,
            manivelas = manivelas.toString(),
            remotos = remotos.toString(),
            subtotalManual = txtSubManual,
            subtotalElectrico = txtSubElectrico,
            subtotalUnico = txtSubUnico,
            linesDescripcion = linesDesc,
            height = rowH
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CÁLCULOS DE RESUMEN — Calculados desde las medidas directamente para evitar $0
    // ═══════════════════════════════════════════════════════════════════════════

    // Calcular desde medidas directamente (más confiable que los campos del cotizacion)
    val subtotalManual = cotizacion.medidas.sumOf { it.subtotalManual }
    val subtotalElectrico = cotizacion.medidas.sumOf { it.subtotalElectrico }
    val subtotalGeneral = subtotalManual + subtotalElectrico
    val descuentoPorcentaje = cotizacion.descuentoPorcentaje

    val descuentoMontoManual = subtotalManual * (descuentoPorcentaje / 100.0)
    val descuentoMontoElectrico = subtotalElectrico * (descuentoPorcentaje / 100.0)
    val descuentoMontoTotal = subtotalGeneral * (descuentoPorcentaje / 100.0)

    val sub2Manual = subtotalManual - descuentoMontoManual
    val sub2Electrico = subtotalElectrico - descuentoMontoElectrico
    val sub2Total = subtotalGeneral - descuentoMontoTotal

    val ivaManual = sub2Manual * RAIN_IVA_RATE
    val ivaElectrico = sub2Electrico * RAIN_IVA_RATE
    val ivaTotal = sub2Total * RAIN_IVA_RATE

    val totalManual = sub2Manual + ivaManual
    val totalElectrico = sub2Electrico + ivaElectrico
    val totalFinal = sub2Total + ivaTotal

    // ═══════════════════════════════════════════════════════════════════════════
    // DIMENSIONES PAGINACIÓN
    // ═══════════════════════════════════════════════════════════════════════════

    val headerTableRow1H = 20f
    val headerTableRow2H = 20f
    val headerTableTotalH = headerTableRow1H + headerTableRow2H

    val resumenBlockHeight = 14f * 7
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
    // FOLIO — esquina superior derecha, sobre/debajo del header
    // ═══════════════════════════════════════════════════════════════════════════

    val folioY = headerBarHeight + 14f
    drawFolioBox(canvas, folioY)

    // ═══════════════════════════════════════════════════════════════════════════
    // INFO DEL CLIENTE — Según PDF de referencia
    // Izq: Nombre, Dirección, Teléfono, Municipio, Zona
    // Der: Fecha, Especialista, Tipo Propiedad, Rectificación
    // ═══════════════════════════════════════════════════════════════════════════

    var y = headerBarHeight + 28f
    val leftX = margin
    val leftBlockWidth = 270f
    val rightBlockWidth = 230f
    val rightX = pageWidth.toFloat() - margin - rightBlockWidth

    fun drawInfoRow(
        canvasRef: Canvas, x: Float, yTop: Float,
        label: String, value: String, blockW: Float
    ): Float {
        val labelW = 110f; val gapW = 6f; val paddingX = 6f; val lineHeightText = 10f
        val valueX = x + labelW + gapW; val valueW = blockW - labelW - gapW
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.letterSpacing = 0f
        val capitalizedValue = capitalizeWords(value)
        val wrappedLines = wrapText(capitalizedValue, valueW - paddingX * 2, paint, maxLines = 2)
        val numLines = wrappedLines.size.coerceIn(1, 2)
        val rowH = (numLines * lineHeightText + 8f).coerceAtLeast(20f)
        val yBottom = yTop + rowH

        // Label: texto azul bold
        paint.style = Paint.Style.FILL
        paint.color = colorBlue
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 8f
        paint.textAlign = Paint.Align.LEFT
        canvasRef.drawText(label, x + paddingX, yTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        // Value: fondo gris, borde azul
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

    // Izquierda: Nombre, Dirección, Teléfono, Municipio, Zona
    var leftY = y; var rightY = y

    leftY = drawInfoRow(canvas, leftX, leftY, "Nombre del Cliente:", cotizacion.clienteNombre, leftBlockWidth)

    // Dirección: concatenar colonia + calle
    val ubicacionCompleta = cotizacion.ubicacion
    val ciudadCompleta = cotizacion.ciudad
    var restoDireccion = ubicacionCompleta
    if (ciudadCompleta.isNotBlank() && ubicacionCompleta.contains(ciudadCompleta)) {
        restoDireccion = ubicacionCompleta.replace(ciudadCompleta, "").trim()
        restoDireccion = restoDireccion.trimStart(',').trim()
    }
    if (restoDireccion.isNotBlank()) {
        leftY = drawInfoRow(canvas, leftX, leftY, "Dirección:", restoDireccion, leftBlockWidth)
    }

    // Teléfono
    if (cotizacion.clienteTelefono.isNotBlank()) {
        leftY = drawInfoRow(canvas, leftX, leftY, "Teléfono:", cotizacion.clienteTelefono, leftBlockWidth)
    }

    // Municipio (ciudad)
    if (cotizacion.ciudad.isNotBlank()) {
        leftY = drawInfoRow(canvas, leftX, leftY, "Municipio:", cotizacion.ciudad, leftBlockWidth)
    }

    // Zona geográfica
    val zonaTexto = when (cotizacion.zonaGeografica) {
        ZonaGeografica.CONTINENTAL -> "Continental Q.Roo."
        ZonaGeografica.ISLAS -> "Islas"
        ZonaGeografica.FORANEA -> "Foránea"
    }
    leftY = drawInfoRow(canvas, leftX, leftY, "Zona:", zonaTexto, leftBlockWidth)

    // Derecha: Fecha, Especialista, Tipo Propiedad, Rectificación
    rightY = drawInfoRow(canvas, rightX, rightY, "Fecha:", cotizacion.fecha, rightBlockWidth)
    rightY = drawInfoRow(canvas, rightX, rightY, "Especialista:", cotizacion.especialista, rightBlockWidth)

    // Tipo Propiedad
    val tipoPropTexto = when (cotizacion.tipoPropiedad.uppercase()) {
        "CASA" -> "Casa"
        "DEPARTAMENTO" -> "Departamento"
        "COMERCIAL" -> "Comercial"
        "HOTEL" -> "Hotel"
        else -> cotizacion.tipoPropiedad.ifBlank { "-" }
    }
    rightY = drawInfoRow(canvas, rightX, rightY, "Tipo Propiedad:", tipoPropTexto, rightBlockWidth)

    // Rectificación
    val rectificacion = if (cotizacion.fueEditada()) "SI" else "NO"
    rightY = drawInfoRow(canvas, rightX, rightY, "Rectificación:", rectificacion, rightBlockWidth)

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

    val textW = paint.measureText("PRESUPUESTO")
    paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; paint.strokeWidth = 0.8f
    canvas.drawLine(pageWidth / 2f - textW / 2f, presupuestoY + 3f, pageWidth / 2f + textW / 2f, presupuestoY + 3f, paint)
    paint.style = Paint.Style.FILL; paint.textAlign = Paint.Align.LEFT

    y = presupuestoY + 14f

    // ═══════════════════════════════════════════════════════════════════════════
    // TABLA HEADER — Sin #, con Control(Manivela/Remoto), TOTAL(Manual/Eléctrico)
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

        var xCol = tableLeft

        // Área a Proteger (gris completo, texto azul)
        paint.color = colorGrayCell
        canvas.drawRect(xCol, row1Top, xCol + colAreaW, row2Bottom, paint)
        canvas.drawRect(xCol, row1Top, xCol + colAreaW, row2Bottom, borderP)
        paint.color = colorBlue
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = 8f
        canvas.drawText("Área a Proteger", xCol + colAreaW / 2f, row1Top + headerTableTotalH / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
        xCol += colAreaW

        // Cantidad (gris completo, texto azul)
        paint.color = colorGrayCell
        canvas.drawRect(xCol, row1Top, xCol + colCantidadW, row2Bottom, paint)
        canvas.drawRect(xCol, row1Top, xCol + colCantidadW, row2Bottom, borderP)
        paint.color = colorBlue; paint.textSize = 8f
        canvas.drawText("Cantidad", xCol + colCantidadW / 2f, row1Top + headerTableTotalH / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
        xCol += colCantidadW

        // Tela (fila 1 gris, texto azul)
        val telaW = colAnchoW + colAltoW + colM2W
        paint.color = colorGrayCell
        canvas.drawRect(xCol, row1Top, xCol + telaW, row1Bottom, paint)
        canvas.drawRect(xCol, row1Top, xCol + telaW, row1Bottom, borderP)
        paint.color = colorBlue; paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas.drawText("Tela", xCol + telaW / 2f, row1Top + headerTableRow1H / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        // Sub-columnas Tela (fila 2 blanco, texto azul)
        listOf(
            Pair(colAnchoW, "Ancho"),
            Pair(colAltoW, "Alto"),
            Pair(colM2W, "m² Total")
        ).fold(xCol) { acc, (w, text) ->
            paint.style = Paint.Style.FILL; paint.color = Color.WHITE
            canvas.drawRect(acc, row2Top, acc + w, row2Bottom, paint)
            canvas.drawRect(acc, row2Top, acc + w, row2Bottom, borderP)
            paint.style = Paint.Style.FILL; paint.color = colorBlue; paint.textSize = 7.5f
            canvas.drawText(text, acc + w / 2f, row2Top + headerTableRow2H / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
            acc + w
        }
        xCol += telaW

        // Control (fila 1 gris, texto azul) — antes era "Mecanismo"
        val controlW = colManivelaW + colRemotoW
        paint.style = Paint.Style.FILL; paint.color = colorGrayCell
        canvas.drawRect(xCol, row1Top, xCol + controlW, row1Bottom, paint)
        canvas.drawRect(xCol, row1Top, xCol + controlW, row1Bottom, borderP)
        paint.color = colorBlue; paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas.drawText("Control", xCol + controlW / 2f, row1Top + headerTableRow1H / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

        // Sub-columnas Control (fila 2 blanco, texto azul): Manivela | Remoto
        listOf(
            Pair(colManivelaW, "Manivela"),
            Pair(colRemotoW, "Remoto")
        ).fold(xCol) { acc, (w, text) ->
            paint.style = Paint.Style.FILL; paint.color = Color.WHITE
            canvas.drawRect(acc, row2Top, acc + w, row2Bottom, paint)
            canvas.drawRect(acc, row2Top, acc + w, row2Bottom, borderP)
            paint.style = Paint.Style.FILL; paint.color = colorBlue; paint.textSize = 7.5f
            canvas.drawText(text, acc + w / 2f, row2Top + headerTableRow2H / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
            acc + w
        }
        xCol += controlW

        // TOTAL (AZUL fondo, texto blanco)
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        if (tieneAmbos) {
            // Fila 1: "TOTAL" abarca las 2 sub-columnas
            paint.style = Paint.Style.FILL; paint.color = colorBlue
            canvas.drawRect(xCol, row1Top, xCol + totalColumnsW, row1Bottom, paint)
            canvas.drawRect(xCol, row1Top, xCol + totalColumnsW, row1Bottom, borderP)
            paint.color = Color.WHITE; paint.textSize = 9f
            canvas.drawText("TOTAL", xCol + totalColumnsW / 2f, row1Top + headerTableRow1H / 2f - (paint.descent() + paint.ascent()) / 2f, paint)

            // Fila 2: "Manual" | "Eléctrico" sub-columnas con fondo azul oscuro
            listOf(
                Pair(colTotalManualW, "Manual"),
                Pair(colTotalElectricoW, "Eléctrico")
            ).fold(xCol) { acc, (w, text) ->
                paint.style = Paint.Style.FILL; paint.color = colorBlueDark
                canvas.drawRect(acc, row2Top, acc + w, row2Bottom, paint)
                canvas.drawRect(acc, row2Top, acc + w, row2Bottom, borderP)
                paint.style = Paint.Style.FILL; paint.color = Color.WHITE; paint.textSize = 7.5f
                canvas.drawText(text, acc + w / 2f, row2Top + headerTableRow2H / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
                acc + w
            }
        } else {
            // Solo 1 columna TOTAL
            paint.style = Paint.Style.FILL; paint.color = colorBlue
            canvas.drawRect(xCol, row1Top, xCol + colTotalUnicoW, row2Bottom, paint)
            canvas.drawRect(xCol, row1Top, xCol + colTotalUnicoW, row2Bottom, borderP)
            paint.color = Color.WHITE; paint.textSize = 9f
            canvas.drawText("TOTAL", xCol + colTotalUnicoW / 2f, row1Top + headerTableTotalH / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
        }

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

        // Fondo blanco de la fila
        paint.style = Paint.Style.FILL; paint.color = Color.WHITE
        canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, paint)

        // Línea separadora inferior
        paint.style = Paint.Style.STROKE; paint.color = Color.parseColor("#E0E0E0"); paint.strokeWidth = 0.5f
        canvas.drawLine(tableLeft, rowBottom, tableRight, rowBottom, paint)

        paint.style = Paint.Style.FILL; paint.color = Color.BLACK
        paint.textSize = bodyTextSize
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.letterSpacing = 0f

        val numCenterY = rowTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f
        var xCol = tableLeft

        // Área a Proteger — Nombre del área (bold) + "Apertura N" debajo
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val descLine1Y = rowTop + cellLineHeight
        if (fila.linesDescripcion.size == 1) {
            canvas.drawText(fila.linesDescripcion.first(), xCol + colAreaW / 2f, descLine1Y, paint)
        } else {
            var textY = rowTop + cellPadding + cellLineHeight - paint.descent()
            fila.linesDescripcion.forEach { line ->
                canvas.drawText(line, xCol + colAreaW / 2f, textY, paint); textY += cellLineHeight
            }
        }
        // "Apertura N" debajo del nombre
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.textSize = 7.5f
        canvas.drawText(fila.aperturaLabel, xCol + colAreaW / 2f, descLine1Y + cellLineHeight, paint)
        paint.textSize = bodyTextSize
        xCol += colAreaW

        // Cantidad
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        canvas.drawText(fila.piezas, xCol + colCantidadW / 2f, numCenterY, paint)
        xCol += colCantidadW

        // Ancho
        canvas.drawText(fila.ancho, xCol + colAnchoW / 2f, numCenterY, paint)
        xCol += colAnchoW

        // Alto
        canvas.drawText(fila.alto, xCol + colAltoW / 2f, numCenterY, paint)
        xCol += colAltoW

        // m² Total
        canvas.drawText(fila.m2, xCol + colM2W / 2f, numCenterY, paint)
        xCol += colM2W

        // Manivela
        canvas.drawText(fila.manivelas, xCol + colManivelaW / 2f, numCenterY, paint)
        xCol += colManivelaW

        // Remoto
        canvas.drawText(fila.remotos, xCol + colRemotoW / 2f, numCenterY, paint)
        xCol += colRemotoW

        // TOTAL
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        if (tieneAmbos) {
            // Manual
            if (fila.subtotalManual.isNotBlank()) {
                canvas.drawText(fila.subtotalManual, xCol + colTotalManualW - 4f, numCenterY, paint)
            }
            xCol += colTotalManualW
            // Eléctrico
            if (fila.subtotalElectrico.isNotBlank()) {
                canvas.drawText(fila.subtotalElectrico, xCol + colTotalElectricoW - 4f, numCenterY, paint)
            }
        } else {
            canvas.drawText(fila.subtotalUnico, xCol + colTotalUnicoW - 4f, numCenterY, paint)
        }
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        y = rowBottom
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RESUMEN — Dual columnas (Manual + Eléctrico) o única
    // ═══════════════════════════════════════════════════════════════════════════

    val labelWidth = 100f
    val valueColumnWidth = 90f
    val rowHeightResumen = 14f
    val resumenTextSize = 9f

    val numValueCols = if (tieneAmbos) 2 else 1
    val totalResumenWidth = labelWidth + valueColumnWidth * numValueCols
    val margenSobreFooter = 8f
    val resumenTopDesdeAbajo = pageHeight.toFloat() - bottomBarHeight - margenSobreFooter - rowHeightResumen * 7
    val resumenTop = maxOf(y + 15f, resumenTopDesdeAbajo)

    val resumenRight = tableRight
    val resumenLeft = resumenRight - totalResumenWidth
    var filaTop = resumenTop

    fun drawResumenRowDual(
        label: String,
        valueManual: String,
        valueElectrico: String,
        valueUnico: String = "",
        isBold: Boolean = false,
        drawLineAbove: Boolean = false,
        labelColor: Int = Color.BLACK,
        valueColor: Int = Color.BLACK
    ) {
        val filaBottom = filaTop + rowHeightResumen
        if (drawLineAbove) {
            paint.style = Paint.Style.STROKE; paint.color = Color.BLACK; paint.strokeWidth = 1f
            canvas.drawLine(resumenLeft, filaTop, resumenRight, filaTop, paint)
        }
        val centerY = filaTop + rowHeightResumen / 2f - (paint.descent() + paint.ascent()) / 2f

        // Label
        paint.style = Paint.Style.FILL; paint.color = labelColor
        paint.textAlign = Paint.Align.LEFT; paint.textSize = resumenTextSize
        paint.typeface = if (isBold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.letterSpacing = 0f
        canvas.drawText(label, resumenLeft + 2f, centerY, paint)

        // Valores
        paint.color = valueColor; paint.textAlign = Paint.Align.RIGHT
        if (tieneAmbos) {
            val col1Right = resumenLeft + labelWidth + valueColumnWidth
            canvas.drawText(valueManual, col1Right - 4f, centerY, paint)
            canvas.drawText(valueElectrico, resumenRight - 4f, centerY, paint)
        } else {
            canvas.drawText(valueUnico, resumenRight - 4f, centerY, paint)
        }
        filaTop = filaBottom
    }

    // Header del resumen con etiquetas de columna
    if (tieneAmbos) {
        val headerBottom = filaTop + rowHeightResumen
        paint.style = Paint.Style.FILL; paint.textSize = resumenTextSize
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.color = colorBlue; paint.textAlign = Paint.Align.RIGHT
        paint.letterSpacing = 0f
        val centerY = filaTop + rowHeightResumen / 2f - (paint.descent() + paint.ascent()) / 2f
        val col1Right = resumenLeft + labelWidth + valueColumnWidth
        canvas.drawText("Manual", col1Right - 4f, centerY, paint)
        canvas.drawText("Eléctrico", resumenRight - 4f, centerY, paint)
        filaTop = headerBottom
    }

    // Sub-Total 1
    drawResumenRowDual(
        label = "Sub- Total 1",
        valueManual = "$ %,.2f".format(subtotalManual),
        valueElectrico = "$ %,.2f".format(subtotalElectrico),
        valueUnico = "$ %,.2f".format(subtotalGeneral),
        drawLineAbove = true
    )
    // Descuento
    drawResumenRowDual(
        label = "Descuento  %.2f%%".format(descuentoPorcentaje),
        valueManual = "-$ %,.2f".format(descuentoMontoManual),
        valueElectrico = "-$ %,.2f".format(descuentoMontoElectrico),
        valueUnico = "-$ %,.2f".format(descuentoMontoTotal),
        labelColor = colorRed, valueColor = colorRed
    )
    // Sub-Total 2
    drawResumenRowDual(
        label = "Sub-Total 2",
        valueManual = "$ %,.2f".format(sub2Manual),
        valueElectrico = "$ %,.2f".format(sub2Electrico),
        valueUnico = "$ %,.2f".format(sub2Total)
    )
    // IVA
    drawResumenRowDual(
        label = "Iva",
        valueManual = "$ %,.2f".format(ivaManual),
        valueElectrico = "$ %,.2f".format(ivaElectrico),
        valueUnico = "$ %,.2f".format(ivaTotal)
    )
    // Total
    drawResumenRowDual(
        label = "Total",
        valueManual = "$ %,.2f".format(totalManual),
        valueElectrico = "$ %,.2f".format(totalElectrico),
        valueUnico = "$ %,.2f".format(totalFinal),
        isBold = true, drawLineAbove = true
    )

    // Etiquetas finales "Manual" / "Eléctrico" debajo del total
    if (tieneAmbos) {
        val labelBottom = filaTop + rowHeightResumen
        paint.style = Paint.Style.FILL; paint.textSize = resumenTextSize
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.color = colorBlue; paint.textAlign = Paint.Align.RIGHT
        paint.letterSpacing = 0f
        val centerY = filaTop + rowHeightResumen / 2f - (paint.descent() + paint.ascent()) / 2f
        val col1Right = resumenLeft + labelWidth + valueColumnWidth
        canvas.drawText("Manual", col1Right - 4f, centerY, paint)
        canvas.drawText("Eléctrico", resumenRight - 4f, centerY, paint)
        filaTop = labelBottom
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONDICIONES COMERCIALES — Imagen (rain_condiciones_comerciales)
    // ═══════════════════════════════════════════════════════════════════════════

    val condicionesLeft = margin
    val condicionesRight = resumenLeft - 10f
    val condicionesAvailableWidth = condicionesRight - condicionesLeft
    val resumenBottom = filaTop
    val condicionesAvailableHeight = (resumenBottom - resumenTop) * 1.15f

    try {
        val options = BitmapFactory.Options().apply { inScaled = false }
        // Intenta cargar la imagen de condiciones Rain; si no existe, usa la de Hurricane
        val condRainId = context.resources.getIdentifier("rain_condiciones_comerciales", "drawable", context.packageName)
        val condResId = if (condRainId != 0) condRainId else R.drawable.condiciones_comerciales
        val condicionesImg = BitmapFactory.decodeResource(context.resources, condResId, options)
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