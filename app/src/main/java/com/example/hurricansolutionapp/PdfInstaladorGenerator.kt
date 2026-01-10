package com.example.hurricansolutionapp

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfInstaladorGenerator {
    private const val TAG = "PdfInstaladorGenerator"
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    fun generarPdfOrdenInstalacion(
        context: Context,
        cotizacion: Cotizacion,
        sistemaSeleccionado: String,
        instaladorDatos: InstaladorDatos? = null,
        medidasRectificadas: List<MedidaInstalador>? = null
    ): File? {
        return try {
            android.util.Log.d(TAG, "Generando PDF de instalacion para: ${cotizacion.folio}")
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            var yPosition = MARGIN

            yPosition = drawHeader(canvas, yPosition, cotizacion.folio)
            yPosition = drawClienteSection(
                canvas,
                yPosition,
                cotizacion,
                instaladorDatos,
                sistemaSeleccionado
            )
            yPosition = drawMedidasTable(canvas, yPosition, cotizacion, medidasRectificadas)
            val obs = instaladorDatos?.getObservacionesSeguras() ?: ""
            if (obs.isNotBlank()) yPosition = drawObservaciones(canvas, yPosition, obs)
            drawFooter(canvas)

            pdfDocument.finishPage(page)

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

    private fun drawHeader(canvas: Canvas, startY: Float, folio: String): Float {
        var y = startY
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ORDEN DE INSTALACION", PAGE_WIDTH / 2f, y + 20, titlePaint)

        val folioPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val folioBoxPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }

        val folioText = "Folio: $folio"
        val folioWidth = folioPaint.measureText(folioText) + 20
        canvas.drawRect(
            PAGE_WIDTH - MARGIN - folioWidth,
            y + 5,
            PAGE_WIDTH - MARGIN,
            y + 25,
            folioBoxPaint
        )
        canvas.drawText(folioText, PAGE_WIDTH - MARGIN - folioWidth + 10, y + 20, folioPaint)
        y += 45
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
        })
        return y + 15
    }

    private fun drawClienteSection(
        canvas: Canvas,
        startY: Float,
        cotizacion: Cotizacion,
        instaladorDatos: InstaladorDatos?,
        sistemaSeleccionado: String
    ): Float {
        var y = startY
        val labelPaint = Paint().apply { color = Color.GRAY; textSize = 10f }
        val valuePaint = Paint().apply { color = Color.BLACK; textSize = 11f }
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val col1X = MARGIN
        val col2X = PAGE_WIDTH / 2f + 20

        // Fila 1: Cliente | Rectificadas
        canvas.drawText("Nombre del Cliente:", col1X, y, labelPaint)
        canvas.drawText(cotizacion.clienteNombre, col1X + 100, y, boldPaint)
        canvas.drawText("Rectificadas:", col2X, y, labelPaint)
        val rectificadasText = if (instaladorDatos?.rectificadas == true) "Si" else "No"
        canvas.drawText(rectificadasText, col2X + 70, y, valuePaint)
        y += 18

        // Fila 2: Direccion | Tipo Propiedad
        canvas.drawText("Direccion:", col1X, y, labelPaint)
        canvas.drawText(cotizacion.ubicacion.take(35), col1X + 55, y, valuePaint)
        canvas.drawText("Tipo de Propiedad:", col2X, y, labelPaint)
        canvas.drawText(
            instaladorDatos?.getTipoPropiedadSegura()?.ifBlank { "-" } ?: "-",
            col2X + 100,
            y,
            valuePaint
        )
        y += 18

        // Fila 3: Fraccionamiento | Nivel
        canvas.drawText("Fraccionamiento:", col1X, y, labelPaint)
        val colonia = instaladorDatos?.getColoniaSegura()?.ifBlank {
            cotizacion.ubicacion.split(",").getOrNull(1)?.trim() ?: "-"
        } ?: "-"
        canvas.drawText(colonia.take(25), col1X + 90, y, valuePaint)
        canvas.drawText("Nivel:", col2X, y, labelPaint)
        canvas.drawText(
            instaladorDatos?.getNivelSeguro()?.ifBlank { "-" } ?: "-",
            col2X + 35,
            y,
            valuePaint
        )
        y += 18

        // Fila 4: Municipio | Requiere Andamios
        canvas.drawText("Municipio:", col1X, y, labelPaint)
        canvas.drawText(cotizacion.ciudad.take(25), col1X + 55, y, valuePaint)
        canvas.drawText("Requiere Andamios:", col2X, y, labelPaint)
        canvas.drawText(
            if (instaladorDatos?.requiereAndamios == true) "Si" else "No",
            col2X + 105,
            y,
            valuePaint
        )
        y += 18

        // Fila 5: Especialista | Fecha Solicitada
        canvas.drawText("Especialista:", col1X, y, labelPaint)
        canvas.drawText(cotizacion.especialista.take(25), col1X + 65, y, valuePaint)
        canvas.drawText("Fecha Solicitada:", col2X, y, labelPaint)
        canvas.drawText(
            instaladorDatos?.getFechaSolicitadaSegura()?.ifBlank { "-" } ?: "-",
            col2X + 95,
            y,
            valuePaint
        )
        y += 25

        // Sistema
        val sistemaPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "Sistema: ${sistemaSeleccionado.getSistemaDisplayName()}",
            col1X,
            y,
            sistemaPaint
        )
        y += 20
        canvas.drawLine(
            MARGIN,
            y,
            PAGE_WIDTH - MARGIN,
            y,
            Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }
        )
        return y + 10
    }

    private fun drawMedidasTable(
        canvas: Canvas,
        startY: Float,
        cotizacion: Cotizacion,
        medidasRectificadas: List<MedidaInstalador>?
    ): Float {
        var y = startY
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#333333")
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cellPaint = Paint().apply { color = Color.BLACK; textSize = 9f }

        // Encabezados de tabla - ajustados para mejor visualizacion
        val cols = listOf(
            MARGIN,           // Zona
            MARGIN + 50,      // Area a Proteger
            MARGIN + 180,     // Cantidad
            MARGIN + 220,     // Ancho
            MARGIN + 265,     // Alto
            MARGIN + 310,     // Area Total
            MARGIN + 365,     // Tipo de Montaje
            MARGIN + 445,     // Adecuaciones
            MARGIN + 500      // Tipo Sistema
        )

        // Fondo del encabezado
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 18, headerBgPaint)

        val headers = listOf(
            "Zona",
            "Area a Proteger",
            "Cantidad",
            "Ancho",
            "Alto",
            "Area Total",
            "Tipo de Montaje",
            "Adecuaciones",
            "Tipo Sistema"
        )
        headers.forEachIndexed { index, header ->
            canvas.drawText(header, cols[index], y + 13, headerTextPaint)
        }
        y += 20

        // Linea bajo encabezado
        canvas.drawLine(
            MARGIN, y, PAGE_WIDTH - MARGIN, y,
            Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }
        )
        y += 3

        // Datos de medidas
        var totalArea = 0.0
        val items = medidasRectificadas?.takeIf { it.isNotEmpty() } ?: cotizacion.ventanas

        items.forEachIndexed { index, item ->
            val cellY = y + 12
            val rowBg = if (index % 2 == 0) Color.WHITE else Color.parseColor("#FAFAFA")
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 16, Paint().apply {
                color = rowBg
                style = Paint.Style.FILL
            })

            when (item) {
                is MedidaInstalador -> {
                    val area = item.alto * item.ancho
                    totalArea += area
                    canvas.drawText(item.getZonaSegura().take(8), cols[0], cellY, cellPaint)
                    canvas.drawText(item.descripcion.take(20), cols[1], cellY, cellPaint)
                    canvas.drawText(item.cantidad.toString(), cols[2], cellY, cellPaint)
                    canvas.drawText(String.format("%.2f", item.ancho), cols[3], cellY, cellPaint)
                    canvas.drawText(String.format("%.2f", item.alto), cols[4], cellY, cellPaint)
                    canvas.drawText(String.format("%.2f", area), cols[5], cellY, cellPaint)
                    canvas.drawText(item.getTipoMontajeSeguro().take(10), cols[6], cellY, cellPaint)
                    canvas.drawText(
                        if (item.requiereAdecuacion) "Si" else "No",
                        cols[7],
                        cellY,
                        cellPaint
                    )
                }

                is Ventana -> {
                    val area = item.alto * item.ancho
                    totalArea += area
                    canvas.drawText(item.zona.take(8), cols[0], cellY, cellPaint)
                    canvas.drawText(item.descripcion.take(20), cols[1], cellY, cellPaint)
                    canvas.drawText("1", cols[2], cellY, cellPaint)
                    canvas.drawText(String.format("%.2f", item.ancho), cols[3], cellY, cellPaint)
                    canvas.drawText(String.format("%.2f", item.alto), cols[4], cellY, cellPaint)
                    canvas.drawText(String.format("%.2f", area), cols[5], cellY, cellPaint)
                    canvas.drawText(item.tipoMontaje.take(10), cols[6], cellY, cellPaint)
                    canvas.drawText(
                        if (item.adecuacion != "No" && item.adecuacion.isNotBlank()) "Si" else "No",
                        cols[7],
                        cellY,
                        cellPaint
                    )
                }
            }

            // Columna Tipo Sistema (solo en primera fila)
            if (index == 0) {
                canvas.drawText(cotizacion.producto.etiquetaCorta, cols[8], cellY, cellPaint)
            }

            y += 16
        }

        // Linea de cierre
        canvas.drawLine(
            MARGIN, y, PAGE_WIDTH - MARGIN, y,
            Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }
        )
        y += 15

        // Total de area
        val totalPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "AREA TOTAL: ${String.format("%.2f", totalArea)} m2",
            MARGIN,
            y,
            totalPaint
        )
        return y + 20
    }

    private fun drawObservaciones(canvas: Canvas, startY: Float, observaciones: String): Float {
        var y = startY + 10
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint().apply { color = Color.DKGRAY; textSize = 10f }

        canvas.drawText("OBSERVACIONES:", MARGIN, y, titlePaint)
        y += 15

        // Dividir observaciones en lineas si es muy largo
        val maxWidth = PAGE_WIDTH - 2 * MARGIN
        val words = observaciones.split(" ")
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (textPaint.measureText(testLine) < maxWidth) {
                currentLine = testLine
            } else {
                canvas.drawText(currentLine, MARGIN, y, textPaint)
                y += 14
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, MARGIN, y, textPaint)
            y += 14
        }

        return y + 10
    }

    private fun drawFooter(canvas: Canvas) {
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val footerText = "Hurricane Solution | Generado: ${dateFormat.format(Date())}"
        canvas.drawText(footerText, PAGE_WIDTH / 2f, PAGE_HEIGHT - 20f, footerPaint)
    }

    private fun String.getSistemaDisplayName(): String = when {
        contains("875", ignoreCase = true) -> "HS-875"
        contains("1250", ignoreCase = true) -> "HS-1250"
        contains("1500", ignoreCase = true) -> "HS-1500"
        else -> this
    }
}