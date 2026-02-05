package com.example.hurricansolutionapp

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.util.Locale


object PdfInstaladorGenerator {
    private const val TAG = "PdfInstaladorGenerator"

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    // Secciones
    private const val HEADER_HEIGHT = 45f
    private const val ROW_HEIGHT = 20f
    private const val TABLE_ROW_HEIGHT = 18f

    // Footer Observaciones (altura fija)
    private const val OBS_BOX_HEIGHT = 85f
    private const val OBS_TITLE_GAP = 10f
    private const val OBS_BOTTOM_GAP = 12f
    private const val OBS_FOOTER_TOTAL_HEIGHT = 115f

    // Colores de tela por sistema HS
    private fun getColoresTela(sistema: String): String {
        return when {
            sistema.contains("875", ignoreCase = true) -> "Negro y Café"
            sistema.contains("1250", ignoreCase = true) -> "Blanco y Beige"
            sistema.contains("1500", ignoreCase = true) -> "Café"
            else -> "N/A"
        }
    }

    /**
     * Genera PDF de Orden de Instalación
     *
     * @param context Contexto de Android
     * @param cotizacion Datos de la cotización
     * @param sistemaSeleccionado Sistema seleccionado (HS875, HS1250, HS1500)
     * @param colorSeleccionado Color de tela seleccionado (opcional)
     * @param instaladorDatos Datos del instalador (opcional, de Supabase)
     * @param medidasRectificadas Medidas rectificadas (opcional)
     * @param fechaSolicitadaManual Fecha de instalación manual (cuando no hay instaladorDatos)
     * @param observacionesManuales Observaciones manuales (cuando no hay instaladorDatos)
     */
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
            android.util.Log.d(TAG, "Sistema: $sistemaSeleccionado")
            android.util.Log.d(TAG, "Color: $colorSeleccionado")
            android.util.Log.d(TAG, "Fecha manual: $fechaSolicitadaManual")
            android.util.Log.d(TAG, "Ventanas: ${cotizacion.ventanas.size}")

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var yPosition = 0f

            // 1) Header
            yPosition = drawHeader(context, canvas)

            // 2) Título
            yPosition = drawTitle(canvas, yPosition)

            // 3) Datos cliente - con Sistema HS y Color HS
            yPosition = drawClienteSection(
                canvas = canvas,
                startY = yPosition,
                cotizacion = cotizacion,
                sistemaSeleccionado = sistemaSeleccionado,
                colorSeleccionado = colorSeleccionado,
                instaladorDatos = instaladorDatos,
                fechaSolicitadaManual = fechaSolicitadaManual
            )

            // 4) Reservar footer fijo para Observaciones (SIEMPRE)
            val footerTopY = PAGE_HEIGHT - MARGIN - OBS_FOOTER_TOTAL_HEIGHT
            val tableBottomLimit = footerTopY - 10f

            // 5) Tabla (sin columna Tipo Sistema)
            yPosition = drawMedidasTable(
                canvas = canvas,
                startY = yPosition,
                bottomLimit = tableBottomLimit,
                cotizacion = cotizacion,
                medidasRectificadas = medidasRectificadas
            )

            // 6) Observaciones
            val obs = observacionesManuales?.ifBlank { null }
                ?: instaladorDatos?.getObservacionesSeguras()
                ?: ""
            drawObservaciones(canvas, footerTopY, obs)

            pdfDocument.finishPage(page)

            // Guardar
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

    // Función para capitalizar palabras profesionalmente
    private fun capitalizeWords(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            word.lowercase(Locale.getDefault()).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
    }


    // HEADER


    private fun drawHeader(context: Context, canvas: Canvas): Float {
        val headerTop = 0f
        val headerBottom = HEADER_HEIGHT

        val leftZoneWidth = PAGE_WIDTH * 0.20f
        val centerZoneWidth = PAGE_WIDTH * 0.60f
        val rightZoneWidth = PAGE_WIDTH * 0.20f

        val centerZoneLeft = leftZoneWidth
        val centerZoneRight = centerZoneLeft + centerZoneWidth

        val bgPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawRect(centerZoneLeft, headerTop, centerZoneRight, headerBottom, bgPaint)

        try {
            val options = BitmapFactory.Options().apply { inScaled = false }

            // Logo Izquierda (20%)
            val logoHurricane = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.logo_header_new,
                options
            )
            if (logoHurricane != null) {
                val croppedLogo = cropTransparent(logoHurricane)

                var logoHeight = 28f
                val aspect = croppedLogo.width.toFloat() / croppedLogo.height.toFloat()
                var logoWidth = logoHeight * aspect

                val maxWidth = leftZoneWidth * 0.90f
                if (logoWidth > maxWidth) {
                    val s = maxWidth / logoWidth
                    logoWidth *= s
                    logoHeight *= s
                }

                val centerX = leftZoneWidth / 2f
                val left = centerX - (logoWidth / 2f)
                val top = (headerBottom - logoHeight) / 2f

                val rect = RectF(left, top, left + logoWidth, top + logoHeight)
                canvas.drawBitmap(croppedLogo, null, rect, null)
            }

            // Logo Derecha (20%)
            val logoUsa = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.made_in_usa,
                options
            )
            if (logoUsa != null) {
                val croppedUsa = cropTransparent(logoUsa)

                var usaHeight = 28f
                val aspect = croppedUsa.width.toFloat() / croppedUsa.height.toFloat()
                var usaWidth = usaHeight * aspect

                val maxWidth = rightZoneWidth * 0.90f
                if (usaWidth > maxWidth) {
                    val s = maxWidth / usaWidth
                    usaWidth *= s
                    usaHeight *= s
                }

                val centerX = centerZoneRight + (rightZoneWidth / 2f)
                val left = centerX - (usaWidth / 2f)
                val top = (headerBottom - usaHeight) / 2f

                val rect = RectF(left, top, left + usaWidth, top + usaHeight)

                val grayMatrix = ColorMatrix().apply { setSaturation(0f) }
                val usaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = ColorMatrixColorFilter(grayMatrix)
                }
                canvas.drawBitmap(croppedUsa, null, rect, usaPaint)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error cargando logos: ${e.message}")
        }

        // Texto centrado
        val sloganPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.02f
        }
        val sloganText = "INSTALACIÓN DE PROTECCIONES CONTRA HURACANES"
        val sloganY = headerBottom / 2f - (sloganPaint.descent() + sloganPaint.ascent()) / 2f
        val centerTextX = (centerZoneLeft + centerZoneRight) / 2f
        canvas.drawText(sloganText, centerTextX, sloganY, sloganPaint)

        return headerBottom
    }


    // TITLE


    private fun drawTitle(canvas: Canvas, startY: Float): Float {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val titleY = startY + 40f
        canvas.drawText("ORDEN DE INSTALACION", PAGE_WIDTH / 2f, titleY, titlePaint)
        return titleY + 25f
    }


    // CLIENT SECTION (formulario) - MODIFICADO con Sistema HS y Color HS


    private fun drawClienteSection(
        canvas: Canvas,
        startY: Float,
        cotizacion: Cotizacion,
        sistemaSeleccionado: String,
        colorSeleccionado: String?,
        instaladorDatos: InstaladorDatos?,
        fechaSolicitadaManual: String? = null
    ): Float {
        var y = startY

        val labelBgPaint = Paint().apply {
            color = Color.parseColor("#353535")
            style = Paint.Style.FILL
        }
        val valueBgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val valueTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Dimensiones de columnas
        val col1LabelWidth = 90f
        val col1ValueWidth = 175f
        val gapBetweenColumns = 15f
        val col2LabelWidth = 90f
        val col2ValueWidth = 85f

        val col1Start = MARGIN
        val col1ValueStart = col1Start + col1LabelWidth
        val col2Start = col1ValueStart + col1ValueWidth + gapBetweenColumns
        val col2ValueStart = col2Start + col2LabelWidth

        // Extraer datos de ubicación de la cotización
        val ubicacionPartes = cotizacion.ubicacion.split(",").map { it.trim() }
        val direccionExtraida = ubicacionPartes.lastOrNull() ?: cotizacion.ubicacion
        val coloniaExtraida = if (ubicacionPartes.size >= 2) ubicacionPartes.dropLast(1).lastOrNull() ?: "" else ""

        // Calcular nivel desde las zonas de las ventanas
        val zonasUnicas = cotizacion.ventanas.mapNotNull { it.zona.ifBlank { null } }.distinct()
        val nivelCalculado = if (zonasUnicas.isNotEmpty()) {
            zonasUnicas.joinToString(", ")
        } else {
            "Nivel 1"
        }

        // Determinar fecha a mostrar (manual > instaladorDatos)
        val fechaMostrar = fechaSolicitadaManual?.ifBlank { null }
            ?: instaladorDatos?.getFechaSolicitadaSegura()
            ?: ""

        // Obtener nombre del sistema y color
        val sistemaDisplayName = sistemaSeleccionado.getSistemaDisplayName()
        val colorTela = colorSeleccionado ?: getColoresTela(sistemaSeleccionado)

        val leftData = listOf(
            "Nombre del Cliente:" to capitalizeWords(cotizacion.clienteNombre),
            "Dirección:" to capitalizeWords(instaladorDatos?.getDireccionSegura()?.take(35)
                ?: direccionExtraida.take(35)),
            "Fraccionamiento:" to capitalizeWords(instaladorDatos?.getColoniaSegura()?.ifBlank { coloniaExtraida }
                ?: coloniaExtraida),
            "Municipio:" to capitalizeWords(instaladorDatos?.getCiudadSegura()?.ifBlank { cotizacion.ciudad }
                ?: cotizacion.ciudad),
            "Especialista:" to capitalizeWords(cotizacion.especialista),
            "Sistema HS:" to sistemaDisplayName  // NUEVO
        )

        val rightData = listOf(
            "Rectificadas:" to if (instaladorDatos?.rectificadas == true) "Sí" else "No",
            "Tipo de Propiedad:" to capitalizeWords(instaladorDatos?.getTipoPropiedadSegura()?.ifBlank { "Casa" }
                ?: "Casa"),
            "Nivel:" to capitalizeWords(instaladorDatos?.getNivelSeguro()?.ifBlank { nivelCalculado }
                ?: nivelCalculado),
            "Requiere Andamios:" to if (instaladorDatos?.requiereAndamios == true) "Sí" else "No",
            "Fecha Solicitada:" to fechaMostrar,
            "Color de Tela:" to colorTela  // NUEVO
        )

        for (i in leftData.indices) {
            val rowTop = y
            val rowBottom = y + ROW_HEIGHT

            // Izquierda: label
            canvas.drawRect(col1Start, rowTop, col1Start + col1LabelWidth, rowBottom, labelBgPaint)
            canvas.drawRect(col1Start, rowTop, col1Start + col1LabelWidth, rowBottom, borderPaint)

            // Izquierda: value
            canvas.drawRect(
                col1ValueStart,
                rowTop,
                col1ValueStart + col1ValueWidth,
                rowBottom,
                valueBgPaint
            )
            canvas.drawRect(
                col1ValueStart,
                rowTop,
                col1ValueStart + col1ValueWidth,
                rowBottom,
                borderPaint
            )

            val textY =
                rowTop + ROW_HEIGHT / 2f - (labelTextPaint.descent() + labelTextPaint.ascent()) / 2f
            canvas.drawText(leftData[i].first, col1Start + 3f, textY, labelTextPaint)
            canvas.drawText(leftData[i].second.take(35), col1ValueStart + 3f, textY, valueTextPaint)

            // Derecha
            if (i < rightData.size) {
                canvas.drawRect(
                    col2Start,
                    rowTop,
                    col2Start + col2LabelWidth,
                    rowBottom,
                    labelBgPaint
                )
                canvas.drawRect(
                    col2Start,
                    rowTop,
                    col2Start + col2LabelWidth,
                    rowBottom,
                    borderPaint
                )

                canvas.drawRect(
                    col2ValueStart,
                    rowTop,
                    col2ValueStart + col2ValueWidth,
                    rowBottom,
                    valueBgPaint
                )
                canvas.drawRect(
                    col2ValueStart,
                    rowTop,
                    col2ValueStart + col2ValueWidth,
                    rowBottom,
                    borderPaint
                )

                canvas.drawText(rightData[i].first, col2Start + 3f, textY, labelTextPaint)
                canvas.drawText(
                    rightData[i].second.take(18),
                    col2ValueStart + 3f,
                    textY,
                    valueTextPaint
                )
            }

            y = rowBottom
        }

        return y + 18f
    }


    // TABLE (header gris + grid, SIN columna Tipo Sistema)


    private fun drawMedidasTable(
        canvas: Canvas,
        startY: Float,
        bottomLimit: Float,
        cotizacion: Cotizacion,
        medidasRectificadas: List<MedidaInstalador>?
    ): Float {
        var y = startY

        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#E6E6E6")
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val zonaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8f
            textAlign = Paint.Align.LEFT
        }
        val cellCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1.0f
        }

        val tableLeft = MARGIN
        val tableRight = PAGE_WIDTH - MARGIN

        // Columnas MODIFICADAS - Sin Tipo Sistema
        // Redistribuir el espacio de forma más equilibrada
        val colAreaProteger = tableLeft
        val colCantidad = tableLeft + 150f      // Más espacio para área a proteger
        val colAncho = tableLeft + 200f
        val colAlto = tableLeft + 250f
        val colAreaTotal = tableLeft + 300f
        val colTipoMontaje = tableLeft + 360f
        val colAdecuaciones = tableLeft + 450f  // Columna final

        val cols = floatArrayOf(
            colAreaProteger, colCantidad, colAncho, colAlto,
            colAreaTotal, colTipoMontaje, colAdecuaciones
        )

        // Headers SIN "Tipo Sistema"
        val headers = listOf(
            "Área a Proteger", "Cantidad", "Ancho", "Alto",
            "Área Total", "Tipo de Montaje", "Adecuaciones"
        )

        // HEADER
        if (y + TABLE_ROW_HEIGHT > bottomLimit) return y

        // Fondo del header para "Área a Proteger"
        canvas.drawRect(
            cols[0],
            y,
            cols[1],
            y + TABLE_ROW_HEIGHT,
            Paint().apply { color = Color.parseColor("#b7b7b7"); style = Paint.Style.FILL })
        // Fondo del header para el resto
        canvas.drawRect(cols[1], y, tableRight, y + TABLE_ROW_HEIGHT, headerBgPaint)

        // Borde exterior + grid header
        canvas.drawLine(tableLeft, y, tableRight, y, linePaint)
        canvas.drawLine(
            tableLeft,
            y + TABLE_ROW_HEIGHT,
            tableRight,
            y + TABLE_ROW_HEIGHT,
            linePaint
        )
        canvas.drawLine(tableLeft, y, tableLeft, y + TABLE_ROW_HEIGHT, linePaint)
        canvas.drawLine(tableRight, y, tableRight, y + TABLE_ROW_HEIGHT, linePaint)
        cols.forEach { x -> canvas.drawLine(x, y, x, y + TABLE_ROW_HEIGHT, linePaint) }

        val headerTextY =
            y + TABLE_ROW_HEIGHT / 2f - (headerTextPaint.descent() + headerTextPaint.ascent()) / 2f
        headers.forEachIndexed { index, header ->
            val left = cols[index]
            val right = if (index == cols.size - 1) tableRight else cols[index + 1]
            val cx = (left + right) / 2f
            canvas.drawText(header, cx, headerTextY, headerTextPaint)
        }
        y += TABLE_ROW_HEIGHT

        val itemsAgrupados = when {
            medidasRectificadas?.isNotEmpty() == true ->
                medidasRectificadas.groupBy { capitalizeWords(it.getZonaSegura().ifBlank { "Sin zona" }) }

            else ->
                cotizacion.ventanas.groupBy { capitalizeWords(it.zona.ifBlank { "Sin zona" }) }
        }

        itemsAgrupados.forEach { (zona, itemsEnZona) ->
            // --- Zona row ---
            if (y + TABLE_ROW_HEIGHT > bottomLimit) return y

            val zonaTop = y
            val zonaBottom = y + TABLE_ROW_HEIGHT

            // Bordes de zona
            canvas.drawLine(tableLeft, zonaTop, tableRight, zonaTop, linePaint)
            canvas.drawLine(tableLeft, zonaTop, tableLeft, zonaBottom, linePaint)
            canvas.drawLine(tableRight, zonaTop, tableRight, zonaBottom, linePaint)

            // Grid vertical
            cols.forEach { x -> canvas.drawLine(x, zonaTop, x, zonaBottom, linePaint) }

            val zonaTextY =
                zonaTop + TABLE_ROW_HEIGHT / 2f - (zonaPaint.descent() + zonaPaint.ascent()) / 2f
            canvas.drawText(zona, cols[0] + 2f, zonaTextY, zonaPaint)

            y = zonaBottom

            // --- Items rows ---
            itemsEnZona.forEachIndexed { index, item ->
                if (y + TABLE_ROW_HEIGHT > bottomLimit) return y

                val rowTop = y
                val rowBottom = y + TABLE_ROW_HEIGHT
                val textY =
                    rowTop + TABLE_ROW_HEIGHT / 2f - (cellPaint.descent() + cellPaint.ascent()) / 2f

                // Bordes
                canvas.drawLine(tableLeft, rowTop, tableLeft, rowBottom, linePaint)
                canvas.drawLine(tableRight, rowTop, tableRight, rowBottom, linePaint)
                canvas.drawLine(tableLeft, rowBottom, tableRight, rowBottom, linePaint)

                if (index != 0) {
                    canvas.drawLine(tableLeft, rowTop, tableRight, rowTop, linePaint)
                }

                // Grid vertical
                cols.forEach { x -> canvas.drawLine(x, rowTop, x, rowBottom, linePaint) }

                when (item) {
                    is MedidaInstalador -> {
                        val area = item.alto * item.ancho
                        // Área a Proteger - Centrado y con formato profesional
                        canvas.drawText(
                            capitalizeWords(item.descripcion.take(30)),
                            (cols[0] + cols[1]) / 2f,
                            textY,
                            cellCenterPaint
                        )
                        canvas.drawText(
                            item.cantidad.toString(),
                            (cols[1] + cols[2]) / 2f,
                            textY,
                            cellCenterPaint
                        )
                        canvas.drawText(
                            String.format("%.2f", item.ancho),
                            (cols[2] + cols[3]) / 2f,
                            textY,
                            cellCenterPaint
                        )
                        canvas.drawText(
                            String.format("%.2f", item.alto),
                            (cols[3] + cols[4]) / 2f,
                            textY,
                            cellCenterPaint
                        )
                        canvas.drawText(
                            String.format("%.2f", area),
                            (cols[4] + cols[5]) / 2f,
                            textY,
                            cellCenterPaint
                        )
                        canvas.drawText(
                            capitalizeWords(item.getTipoMontajeSeguro().take(15)),
                            (cols[5] + cols[6]) / 2f,
                            textY,
                            cellCenterPaint
                        )
                        canvas.drawText(
                            if (item.requiereAdecuacion) "Sí" else "No",
                            (cols[6] + tableRight) / 2f,
                            textY,
                            cellCenterPaint
                        )
                    }

                    is Ventana -> {
                        val area = item.alto * item.ancho
                        // Área a Proteger - Centrado y con formato profesional
                        canvas.drawText(
                            capitalizeWords(item.descripcion.take(30)),
                            (cols[0] + cols[1]) / 2f,
                            textY,
                            cellCenterPaint
                        )
                        canvas.drawText("1", (cols[1] + cols[2]) / 2f, textY, cellCenterPaint)
                        canvas.drawText(
                            String.format("%.2f", item.ancho),
                            (cols[2] + cols[3]) / 2f,
                            textY,
                            cellCenterPaint
                        )
                        canvas.drawText(
                            String.format("%.2f", item.alto),
                            (cols[3] + cols[4]) / 2f,
                            textY,
                            cellCenterPaint
                        )
                        canvas.drawText(
                            String.format("%.2f", area),
                            (cols[4] + cols[5]) / 2f,
                            textY,
                            cellCenterPaint
                        )
                        canvas.drawText(
                            capitalizeWords(item.tipoMontaje.take(15)),
                            (cols[5] + cols[6]) / 2f,
                            textY,
                            cellCenterPaint
                        )
                        canvas.drawText(
                            if (item.adecuacion != "No" && item.adecuacion.isNotBlank()) "Sí" else "No",
                            (cols[6] + tableRight) / 2f,
                            textY,
                            cellCenterPaint
                        )
                    }
                }

                y = rowBottom
            }

            // Separación entre grupos
            y += 6f
        }

        return y + 10f
    }

    // OBSERVACIONES (footer fijo)


    private fun drawObservaciones(canvas: Canvas, startY: Float, observaciones: String): Float {
        var y = startY

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 8.5f
        }
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }

        canvas.drawText("Observaciones:", MARGIN, y, titlePaint)
        y += OBS_TITLE_GAP

        val boxTop = y
        val boxLeft = MARGIN
        val boxRight = PAGE_WIDTH - MARGIN
        val boxBottom = boxTop + OBS_BOX_HEIGHT

        canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, borderPaint)

        // Texto dentro (wrap) con padding
        val padding = 6f
        var textY = boxTop + padding + textPaint.textSize
        val maxWidth = (boxRight - boxLeft) - (padding * 2)

        if (observaciones.isNotBlank()) {
            val words = observaciones.split(" ")
            var currentLine = ""

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (textPaint.measureText(testLine) < maxWidth) {
                    currentLine = testLine
                } else {
                    if (textY <= boxBottom - padding) {
                        canvas.drawText(currentLine, boxLeft + padding, textY, textPaint)
                        textY += 11f
                    }
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty() && textY <= boxBottom - padding) {
                canvas.drawText(currentLine, boxLeft + padding, textY, textPaint)
            }
        }

        return boxBottom + OBS_BOTTOM_GAP
    }


    // UTILIDADES


    private fun cropTransparent(bitmap: Bitmap): Bitmap {
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

    private fun String.getSistemaDisplayName(): String = when {
        contains("875", ignoreCase = true) -> "HS-875"
        contains("1250", ignoreCase = true) -> "HS-1250"
        contains("1500", ignoreCase = true) -> "HS-1500"
        else -> this
    }
}