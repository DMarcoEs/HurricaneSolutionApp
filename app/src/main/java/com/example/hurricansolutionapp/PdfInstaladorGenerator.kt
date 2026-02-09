package com.example.hurricansolutionapp

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfInstaladorGenerator {
    private const val TAG = "PdfInstaladorGenerator"

    fun generarPdfOrdenInstalacion(
        context: Context,
        cotizacion: Cotizacion,
        sistemaSeleccionado: String,
        colorSeleccionado: String? = null,
        instaladorDatos: InstaladorDatos? = null,
        medidasRectificadas: List<MedidaInstalador>? = null,
        fechaSolicitadaManual: String? = null,
        observacionesManuales: String? = null
    ): File? {
        return try {
            android.util.Log.d(TAG, "Generando PDF de instalación para: ${cotizacion.folio}")

            val pageWidth = 595
            val pageHeight = 842
            val pdfDocument = PdfDocument()
            val margin = 32f
            val headerBarHeight = 90f

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Color gris para bordes punteados
            val grayBorderColor = Color.parseColor("#9CA3AF")

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

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // HEADER
            fun drawHeader(canvasRef: Canvas) {
                try {
                    val options = BitmapFactory.Options().apply { inScaled = false }
                    val rawLogo = BitmapFactory.decodeResource(context.resources, R.drawable.logo_header_new, options)
                    val croppedLogo = cropTransparent(rawLogo)
                    val targetHeight = 45f
                    val aspectRatio = croppedLogo.width.toFloat() / croppedLogo.height.toFloat()
                    val bandCenterY = headerBarHeight / 2f
                    val logoTop = bandCenterY - targetHeight / 2f
                    val destRect = RectF(margin, logoTop, margin + targetHeight * aspectRatio, logoTop + targetHeight)
                    canvasRef.drawBitmap(croppedLogo, null, destRect, null)

                    val rawUsa = BitmapFactory.decodeResource(context.resources, R.drawable.made_in_usa, options)
                    val usaCropped = cropTransparent(rawUsa)
                    val usaTargetHeight = 38f
                    val usaAspect = usaCropped.width.toFloat() / usaCropped.height.toFloat()
                    val usaTop = bandCenterY - usaTargetHeight / 2f - 3f
                    val usaRight = pageWidth.toFloat() - margin
                    val usaRect = RectF(usaRight - usaTargetHeight * usaAspect, usaTop, usaRight, usaTop + usaTargetHeight)
                    val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
                    val usaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
                    canvasRef.drawBitmap(usaCropped, null, usaRect, usaPaint)

                    paint.color = Color.BLACK
                    paint.textSize = 14f
                    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    paint.textAlign = Paint.Align.CENTER
                    paint.letterSpacing = 0f
                    canvasRef.drawText("Instalación de Protección Contra Huracanes", pageWidth / 2f, bandCenterY - (paint.descent() + paint.ascent()) / 2f, paint)
                } catch (e: Exception) { e.printStackTrace() }
            }

            // FOLIO BOX
            fun drawFolioBox(canvasRef: Canvas, titleY: Float) {
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
                canvasRef.drawRect(boxLeft, boxTop, boxRight, boxTop + boxHeight, boxPaint)
                boxPaint.style = Paint.Style.STROKE; boxPaint.color = Color.DKGRAY; boxPaint.strokeWidth = 0.8f
                canvasRef.drawRect(boxLeft, boxTop, boxRight, boxTop + boxHeight, boxPaint)
                paint.color = Color.BLACK
                canvasRef.drawText(folioTexto, boxLeft + boxPaddingH, boxTop + boxHeight / 2f - (paint.descent() + paint.ascent()) / 2f, paint)
            }

            drawHeader(canvas)

            paint.color = Color.BLACK
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            paint.letterSpacing = 0f
            val tituloY = 105f
            canvas.drawText("ORDEN DE INSTALACIÓN", pageWidth / 2f, tituloY, paint)
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

            // Columna izquierda - Datos del cliente
            var leftY = y
            leftY = drawInfoRowCentered(canvas, leftX, leftY, "Cliente:", cotizacion.clienteNombre, leftBlockWidth)

            if (cotizacion.clienteTelefono.isNotBlank()) {
                leftY = drawInfoRowCentered(canvas, leftX, leftY, "Teléfono:", cotizacion.clienteTelefono, leftBlockWidth)
            }

            if (cotizacion.ciudad.isNotBlank()) {
                leftY = drawInfoRowCentered(canvas, leftX, leftY, "Ciudad:", cotizacion.ciudad, leftBlockWidth)
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

            if (colonia.isNotBlank()) leftY = drawInfoRowCentered(canvas, leftX, leftY, "Colonia:", colonia, leftBlockWidth)
            if (calleNumero.isNotBlank()) leftY = drawInfoRowCentered(canvas, leftX, leftY, "Dirección:", calleNumero, leftBlockWidth)

            // Columna derecha - Datos del proyecto
            var rightY = y

            // Sistema HS
            val sistemaDisplay = when {
                sistemaSeleccionado.contains("875", ignoreCase = true) -> "HS-875"
                sistemaSeleccionado.contains("1250", ignoreCase = true) -> "HS-1250"
                sistemaSeleccionado.contains("1500", ignoreCase = true) -> "HS-1500"
                else -> sistemaSeleccionado
            }
            rightY = drawInfoRowCentered(canvas, rightX, rightY, "Sistema:", sistemaDisplay, rightBlockWidth)

            // Color de tela
            val colorTela = colorSeleccionado ?: when {
                sistemaSeleccionado.contains("875", ignoreCase = true) -> "Negro / Café"
                sistemaSeleccionado.contains("1250", ignoreCase = true) -> "Blanco / Beige"
                sistemaSeleccionado.contains("1500", ignoreCase = true) -> "Café"
                else -> "N/A"
            }
            rightY = drawInfoRowCentered(canvas, rightX, rightY, "Color De Tela:", colorTela, rightBlockWidth)

            // Fecha
            val fechaInstalacion = fechaSolicitadaManual
                ?: instaladorDatos?.fechaSolicitada
                ?: cotizacion.fecha
            rightY = drawInfoRowCentered(canvas, rightX, rightY, "Fecha:", fechaInstalacion, rightBlockWidth)

            // Metraje total
            val metrajeFinal = if (medidasRectificadas != null && medidasRectificadas.isNotEmpty()) {
                medidasRectificadas.sumOf { it.alto * it.ancho * it.cantidad }
            } else {
                cotizacion.ventanas.sumOf { it.areaM2 }
            }
            rightY = drawInfoRowCentered(canvas, rightX, rightY, "Metraje Total:", "%.2f M²".format(metrajeFinal), rightBlockWidth)

            y = maxOf(leftY, rightY) + 12f

            // TABLA DE MEDIDAS - Sin precios, con bordes punteados gris
            val tableLeft = margin
            val tableRight = pageWidth.toFloat() - margin
            val headerHeight = 28f
            val bodyTextSize = 9f
            val cellPadding = 4f
            val cellLineHeight = 12f

            // Columnas para instalador (sin precios)
            val colNumeroW = 25f
            val colAreaW = 120f
            val colCantidadW = 45f
            val colAnchoW = 50f
            val colAltoW = 50f
            val colM2W = 55f
            val colMontajeW = 75f
            val colAdecuacionesW = tableRight - tableLeft - colNumeroW - colAreaW - colCantidadW - colAnchoW - colAltoW - colM2W - colMontajeW

            val zonaTitleHeight = 14f

            // Preparar datos de la tabla
            data class RowLayout(
                val zona: String,
                val descripcion: String,
                val cantidad: Int,
                val ancho: Double,
                val alto: Double,
                val m2: Double,
                val tipoMontaje: String,
                val adecuacion: String,
                val height: Float
            )

            val filas = mutableListOf<RowLayout>()

            if (medidasRectificadas != null && medidasRectificadas.isNotEmpty()) {
                medidasRectificadas.forEach { m ->
                    val area = m.alto * m.ancho * m.cantidad
                    filas.add(RowLayout(
                        zona = capitalizeWords(m.getZonaSegura().ifBlank { "General" }),
                        descripcion = capitalizeWords(m.descripcion),
                        cantidad = m.cantidad,
                        ancho = m.ancho,
                        alto = m.alto,
                        m2 = area,
                        tipoMontaje = capitalizeWords(m.getTipoMontajeSeguro().ifBlank { "Flush Mount" }),
                        adecuacion = if (m.requiereAdecuacion) "Si" else "No",
                        height = cellLineHeight + 8f
                    ))
                }
            } else {
                cotizacion.ventanas.forEach { v ->
                    filas.add(RowLayout(
                        zona = capitalizeWords(v.zona.ifBlank { "General" }),
                        descripcion = capitalizeWords(v.descripcion),
                        cantidad = 1,
                        ancho = v.ancho,
                        alto = v.alto,
                        m2 = v.areaM2,
                        tipoMontaje = capitalizeWords(v.tipoMontaje.ifBlank { "Flush Mount" }),
                        adecuacion = if (v.adecuacion != "No" && v.adecuacion.isNotBlank()) "Si" else "No",
                        height = cellLineHeight + 8f
                    ))
                }
            }

            // Agrupar por zona
            val zonasEnOrden = filas.map { it.zona }.distinct()
            val filasAgrupadasPorZona = zonasEnOrden.associateWith { zona -> filas.filter { it.zona == zona } }

            // Espacio para observaciones al final
            val obsBoxHeight = 85f
            val obsTotalHeight = 115f
            val bottomLimit = pageHeight.toFloat() - margin - obsTotalHeight - 20f

            // Dibujar header de tabla
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
                    paint.color = Color.BLACK
                    x += width
                }

                drawHeaderCell("#", colNumeroW)
                drawHeaderCell("Área a proteger", colAreaW)
                drawHeaderCell("Cant.", colCantidadW)
                drawHeaderCell("Ancho", colAnchoW)
                drawHeaderCell("Alto", colAltoW)
                drawHeaderCell("M²", colM2W)
                drawHeaderCell("Montaje", colMontajeW)
                drawHeaderCell("Adec.", colAdecuacionesW)

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

            filasAgrupadasPorZona.forEach { (zona, filasDeZona) ->
                if (y + zonaTitleHeight > bottomLimit) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader(canvas)
                    y = headerBarHeight + 20f
                    y = drawTableHeader(y)
                }

                y = drawZonaTitle(zona, y)

                filasDeZona.forEach { fila ->
                    val rowH = fila.height

                    if (y + rowH > bottomLimit) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        drawHeader(canvas)
                        y = headerBarHeight + 20f
                        y = drawTableHeader(y)
                        y = drawZonaTitle("$zona (cont.)", y)
                    }

                    val rowTop = y
                    val rowBottom = y + rowH

                    // Fondo blanco
                    paint.style = Paint.Style.FILL
                    paint.color = Color.WHITE
                    canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, paint)

                    // Bordes punteados gris sutil
                    paint.style = Paint.Style.STROKE
                    paint.color = grayBorderColor
                    paint.strokeWidth = 0.8f
                    paint.pathEffect = DashPathEffect(floatArrayOf(2f, 4f), 0f)
                    canvas.drawRect(tableLeft, rowTop, tableRight, rowBottom, paint)
                    paint.pathEffect = null

                    // Líneas verticales punteadas gris
                    var xCol = tableLeft
                    fun drawCellBorder(width: Float) {
                        paint.style = Paint.Style.STROKE
                        paint.color = grayBorderColor
                        paint.pathEffect = DashPathEffect(floatArrayOf(2f, 4f), 0f)
                        canvas.drawLine(xCol + width, rowTop, xCol + width, rowBottom, paint)
                        paint.pathEffect = null
                        // Restaurar color negro para el texto
                        paint.style = Paint.Style.FILL
                        paint.color = Color.BLACK
                    }

                    // Contenido de las celdas
                    paint.style = Paint.Style.FILL
                    paint.color = Color.BLACK
                    paint.textSize = bodyTextSize
                    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    paint.letterSpacing = 0f
                    paint.textAlign = Paint.Align.CENTER

                    val textCenterY = rowTop + rowH / 2f - (paint.descent() + paint.ascent()) / 2f

                    // #
                    canvas.drawText("#${filaIndexGlobal + 1}", xCol + colNumeroW / 2f, textCenterY, paint)
                    drawCellBorder(colNumeroW)
                    xCol += colNumeroW

                    canvas.drawText(fila.descripcion.take(20), xCol + colAreaW / 2f, textCenterY, paint)
                    drawCellBorder(colAreaW)
                    xCol += colAreaW

                    // Cantidad
                    canvas.drawText(fila.cantidad.toString(), xCol + colCantidadW / 2f, textCenterY, paint)
                    drawCellBorder(colCantidadW)
                    xCol += colCantidadW

                    // Ancho
                    canvas.drawText(String.format("%.2f", fila.ancho), xCol + colAnchoW / 2f, textCenterY, paint)
                    drawCellBorder(colAnchoW)
                    xCol += colAnchoW

                    // Alto
                    canvas.drawText(String.format("%.2f", fila.alto), xCol + colAltoW / 2f, textCenterY, paint)
                    drawCellBorder(colAltoW)
                    xCol += colAltoW

                    // MÂ²
                    canvas.drawText(String.format("%.2f", fila.m2), xCol + colM2W / 2f, textCenterY, paint)
                    drawCellBorder(colM2W)
                    xCol += colM2W

                    // Montaje
                    canvas.drawText(fila.tipoMontaje.take(12), xCol + colMontajeW / 2f, textCenterY, paint)
                    drawCellBorder(colMontajeW)
                    xCol += colMontajeW

                    canvas.drawText(fila.adecuacion, xCol + colAdecuacionesW / 2f, textCenterY, paint)

                    y = rowBottom
                    filaIndexGlobal++
                }

                y += 6f
            }

            val observaciones = observacionesManuales?.ifBlank { null }
                ?: instaladorDatos?.getObservacionesSeguras()
                ?: ""

            val obsStartY = pageHeight.toFloat() - margin - obsTotalHeight

            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Observaciones:", margin, obsStartY, paint)

            // Caja de observaciones
            val boxTop = obsStartY + 10f
            val boxLeft = margin
            val boxRight = pageWidth.toFloat() - margin
            val boxBottom = boxTop + obsBoxHeight

            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            paint.strokeWidth = 1.2f
            canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, paint)

            // Texto dentro de la caja
            if (observaciones.isNotBlank()) {
                paint.style = Paint.Style.FILL
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

                val padding = 6f
                var textY = boxTop + padding + paint.textSize
                val maxWidth = (boxRight - boxLeft) - (padding * 2)

                val words = observaciones.split(" ")
                var currentLine = ""

                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (paint.measureText(testLine) < maxWidth) {
                        currentLine = testLine
                    } else {
                        if (textY <= boxBottom - padding) {
                            canvas.drawText(currentLine, boxLeft + padding, textY, paint)
                            textY += 11f
                        }
                        currentLine = word
                    }
                }
                if (currentLine.isNotEmpty() && textY <= boxBottom - padding) {
                    canvas.drawText(currentLine, boxLeft + padding, textY, paint)
                }
            }

            pdfDocument.finishPage(page)

            // Guardar archivo
            val fileName = getFileName(cotizacion.clienteNombre, cotizacion.folio)
            val outputDir = File(context.filesDir, "pdfs_instalador")
            if (!outputDir.exists()) outputDir.mkdirs()

            val outputFile = File(outputDir, fileName)
            FileOutputStream(outputFile).use { pdfDocument.writeTo(it) }
            pdfDocument.close()

            android.util.Log.d(TAG, "PDF generado: ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error generando PDF: ${e.message}", e)
            null
        }
    }

    fun getFileName(clienteNombre: String, folio: String): String {
        val clienteFormateado = clienteNombre.trim().split("\\s+".toRegex()).take(2)
            .joinToString("_") { it.replaceFirstChar { c -> c.uppercase() } }
            .replace("[^A-Za-z0-9_]".toRegex(), "")
        return "Instaladores_${clienteFormateado}_${folio}.pdf"
    }
}