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

fun generarPdfCotizacion(
    context: Context,
    cotizacion: Cotizacion
): File? {

    // Tamaño A4 en puntos (aprox. 72 dpi)
    val pageWidth = 595
    val pageHeight = 842

    val pdfDocument = PdfDocument()

    val pageInfo = PdfDocument.PageInfo.Builder(
        pageWidth,
        pageHeight,
        1
    ).create()

    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val margin = 32f

    // --------- FOLIO (también lo usamos para el nombre del archivo) ----------
    val timeStamp = LocalDateTime.now().format(
        DateTimeFormatter.ofPattern("yyyyMMdd")
    )

    // ---------- ENCABEZADO NEGRO ----------
    val headerBarHeight = 90f    // altura de la franja negra

    paint.color = Color.BLACK
    canvas.drawRect(
        0f,
        0f,
        pageWidth.toFloat(),
        headerBarHeight,
        paint
    )

    // ---- LOGO dentro del header, a la izquierda ----
    try {
        val rawLogo = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.logo_header  // tu recurso de logo
        )

        // Escalamos manteniendo proporción → aprox. 70 px de alto
        val targetLogoHeight = 70f
        val scale = targetLogoHeight / rawLogo.height.toFloat()
        val logoWidth = (rawLogo.width * scale).toInt()
        val logoHeight = targetLogoHeight.toInt()

        val logoBitmap = Bitmap.createScaledBitmap(
            rawLogo,
            logoWidth,
            logoHeight,
            true
        )

        val logoLeft = margin
        val logoTop = (headerBarHeight - targetLogoHeight) / 2f

        canvas.drawBitmap(logoBitmap, logoLeft, logoTop, null)

    } catch (e: Exception) {
        e.printStackTrace()
    }

    // ---------- LEMA DENTRO DEL HEADER ----------
    paint.color = Color.WHITE
    paint.textSize = 14f
    paint.typeface = Typeface.create("times new roman", Typeface.NORMAL)

    val lema = "Instalación de Protección Contra huracanes"
    val lemaWidth = paint.measureText(lema)
    val lemaX = (pageWidth - lemaWidth) / 2f
    val lemaY = headerBarHeight / 2f + paint.textSize / 2f - 3f

    canvas.drawText(
        lema,
        lemaX,
        lemaY,
        paint
    )

// ---------- FOLIO ARRIBA A LA DERECHA (DENTRO DEL HEADER) ----------
    paint.color = Color.WHITE
    paint.textSize = 10f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

// Tomamos el folio que viene dentro de la cotización
    val folioTexto = "Folio: ${cotizacion.folio}"

    val folioWidth = paint.measureText(folioTexto)
    canvas.drawText(
        folioTexto,
        pageWidth - folioWidth - margin,
        24f,   // un poco abajo del borde superior
        paint
    )


    // ---------- TÍTULO "COTIZACIÓN DE PROYECTO" ----------
    paint.color = Color.BLACK
    paint.textSize = 16f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    val titulo = "COTIZACIÓN DE PROYECTO"
    val tituloWidth = paint.measureText(titulo)
    canvas.drawText(
        titulo,
        (pageWidth - tituloWidth) / 2f,
        120f,
        paint
    )

    // ---------- BLOQUE CLIENTE (IZQUIERDA) ----------
    var y = 160f
    paint.textSize = 10f
    val leftX = margin
    val rightX = pageWidth / 2f

    fun drawLabelValue(label: String, value: String, startY: Float): Float {
        val rowHeight = 18f

        // Fondo del label
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawRect(
            leftX,
            startY - rowHeight,
            leftX + 110f,
            startY,
            paint
        )

        // Texto label
        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(label, leftX + 4f, startY - 5f, paint)

        // Texto valor
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(value, leftX + 115f, startY - 5f, paint)

        return startY + 6f
    }

    y = drawLabelValue("Nombre del Cliente:", cotizacion.clienteNombre, y)
    y = drawLabelValue("Dirección:", cotizacion.ubicacion, y + 10f)

    // ---------- BLOQUE DERECHA: ESPECIALISTA / FECHA / METRAJE ----------
    val rightBlockX = rightX + 10f
    var rightY = 160f

    fun drawRightRow(label: String, value: String, startY: Float): Float {
        val rowHeight = 18f

        // Fondo label
        paint.color = Color.BLACK
        canvas.drawRect(
            rightBlockX,
            startY - rowHeight,
            rightBlockX + 110f,
            startY,
            paint
        )

        // Label
        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(label, rightBlockX + 4f, startY - 5f, paint)

        // Valor
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(value, rightBlockX + 115f, startY - 5f, paint)

        return startY + 6f
    }

    val metrajeFinal = cotizacion.ventanas.sumOf { it.areaM2 }

    rightY = drawRightRow("Especialista:", cotizacion.especialista, rightY)
    rightY = drawRightRow("Fecha:", cotizacion.fecha, rightY + 10f)
    rightY = drawRightRow(
        "Metraje final:",
        "%.2f m²".format(metrajeFinal),
        rightY + 10f
    )

    // Helper para partir texto en varias líneas según el ancho máximo
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

    // ---------- TABLA PRINCIPAL ----------
    y = maxOf(y, rightY) + 30f

    // Encabezados
    paint.textSize = 10f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.BLACK

    val tableLeft = margin
    val tableRight = pageWidth.toFloat() - margin
    val headerHeight = 18f

    // Para el cuerpo de la tabla
    val bodyTextSize = 9f
    val cellPadding = 4f
    val cellLineHeight = 12f   // separación entre líneas dentro de la celda

    // Anchos de columnas
    val colAreaW = 150f              // Área a proteger
    val colAreaTotalW = 60f          // Área total
    val colMontajeW = 80f            // Tipo de montaje
    val colAdecuacionesW = 80f       // Adecuaciones
    val colProductoW = 110f          // Tipo de producto
    val colPrecioW = tableRight - tableLeft -
            (colAreaW + colAreaTotalW + colMontajeW + colAdecuacionesW + colProductoW)

    var x = tableLeft

    fun drawHeaderCell(text: String, width: Float) {
        paint.color = Color.BLACK
        canvas.drawRect(x, y, x + width, y + headerHeight, paint)

        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        val centerX = x + width / 2f
        val centerY = y + headerHeight / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, centerX, centerY, paint)

        x += width
    }

    drawHeaderCell("Área a proteger", colAreaW)
    drawHeaderCell("Área total", colAreaTotalW)
    drawHeaderCell("Tipo de montaje", colMontajeW)
    drawHeaderCell("Adecuaciones", colAdecuacionesW)
    drawHeaderCell("Tipo de producto", colProductoW)
    drawHeaderCell("Precio", colPrecioW)

    // Preparar cuerpo
    paint.textSize = bodyTextSize
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textAlign = Paint.Align.CENTER
    paint.color = Color.BLACK

    y += headerHeight

    // ----- Filas -----
    cotizacion.ventanas.forEach { v ->
        x = tableLeft

        val txtArea = v.descripcion
        val txtAreaTotal = "%.2f".format(v.areaM2)
        val txtMontaje = "Flush Mount"
        val txtAdecuaciones = v.adecuacion
        val txtProducto = when (cotizacion.producto) {
            TipoProducto.HS875 -> "HS-875 (Polipropileno)"
            TipoProducto.HS1250 -> "HS-1250 (Polinet y Armado)"
            TipoProducto.HS1500 -> "HS-1500"
            TipoProducto.PERSONALIZADO -> "Personalizado"
        }
        val txtPrecio = "$ " + "%,.2f".format(v.subtotal)

        val linesArea = wrapText(txtArea, colAreaW - cellPadding * 2, paint)
        val linesAreaTotal = wrapText(txtAreaTotal, colAreaTotalW - cellPadding * 2, paint)
        val linesMontaje = wrapText(txtMontaje, colMontajeW - cellPadding * 2, paint)
        val linesAdecuaciones = wrapText(txtAdecuaciones, colAdecuacionesW - cellPadding * 2, paint)
        val linesProducto = wrapText(txtProducto, colProductoW - cellPadding * 2, paint)
        val linesPrecio = wrapText(txtPrecio, colPrecioW - cellPadding * 2, paint)

        val maxLines = listOf(
            linesArea.size,
            linesAreaTotal.size,
            linesMontaje.size,
            linesAdecuaciones.size,
            linesProducto.size,
            linesPrecio.size
        ).maxOrNull() ?: 1

        val rowHeightDynamic = maxLines * cellLineHeight + 4f

        fun drawBodyCell(lines: List<String>, width: Float) {
            paint.style = Paint.Style.STROKE
            paint.color = Color.LTGRAY
            paint.strokeWidth = 0.5f
            canvas.drawRect(x, y, x + width, y + rowHeightDynamic, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.CENTER

            val centerX = x + width / 2f
            val totalTextHeight = lines.size * cellLineHeight
            var textY = y + (rowHeightDynamic - totalTextHeight) / 2f + cellLineHeight - 3f

            lines.forEach { line ->
                canvas.drawText(line, centerX, textY, paint)
                textY += cellLineHeight
            }

            x += width
        }

        drawBodyCell(linesArea, colAreaW)
        drawBodyCell(linesAreaTotal, colAreaTotalW)
        drawBodyCell(linesMontaje, colMontajeW)
        drawBodyCell(linesAdecuaciones, colAdecuacionesW)
        drawBodyCell(linesProducto, colProductoW)
        drawBodyCell(linesPrecio, colPrecioW)

        y += rowHeightDynamic
    }

    y += 25f

    // ---------- BLOQUE SUBTOTAL / DESCUENTO / TOTAL ----------
    val subtotal = cotizacion.subtotal
    val descuentoEsp = 0.0
    val total = subtotal - descuentoEsp

    val boxLeft = tableRight - 220f
    val labelWidth = 140f
    val valueWidth = 80f
    val totalRowHeight = 18f

    val bloqueAltura = totalRowHeight * 3

    // ---------- FOOTER INFERIOR ----------
    val bottomBarHeight = 60f
    val bottomBarTop = pageHeight.toFloat() - bottomBarHeight

    val paintFooter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    canvas.drawRect(
        0f,
        bottomBarTop,
        pageWidth.toFloat(),
        bottomBarTop + bottomBarHeight,
        paintFooter
    )

    paintFooter.color = Color.WHITE
    paintFooter.textSize = 8f
    paintFooter.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paintFooter.textAlign = Paint.Align.LEFT

    val footerLine1 = "administraciondeventas@hurricanesolution.com"
    val footerLine2 = "protegiendo@hurricanesolution.com"
    val footerLine3 = "www.hurricanesolution.com   9848035014 / 9841478271"

    val footerTextLeft = margin
    val footerTextTop = bottomBarTop + 15f

    canvas.drawText(footerLine1, footerTextLeft, footerTextTop, paintFooter)
    canvas.drawText(footerLine2, footerTextLeft, footerTextTop + 12f, paintFooter)
    canvas.drawText(footerLine3, footerTextLeft, footerTextTop + 24f, paintFooter)

    // ---------- POSICIÓN DEL BLOQUE DE RESUMEN (encima del footer) ----------
    val espacioEntreTablaYResumen = 20f
    val margenSobreFooter = 10f

    val resumenTopDesdeAbajo =
        pageHeight.toFloat() - bottomBarHeight - margenSobreFooter - bloqueAltura

    val resumenTop = maxOf(
        y + espacioEntreTablaYResumen,
        resumenTopDesdeAbajo
    )

    var filaTop = resumenTop

    fun drawResumenRow(
        label: String,
        value: Double,
        isHeader: Boolean
    ) {
        val filaBottom = filaTop + totalRowHeight

        paint.style = Paint.Style.FILL
        paint.color = if (isHeader) Color.BLACK else Color.WHITE
        canvas.drawRect(
            boxLeft,
            filaTop,
            boxLeft + labelWidth,
            filaBottom,
            paint
        )

        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        canvas.drawRect(
            boxLeft + labelWidth,
            filaTop,
            boxLeft + labelWidth + valueWidth,
            filaBottom,
            paint
        )

        paint.style = Paint.Style.FILL
        paint.color = if (isHeader) Color.WHITE else Color.BLACK
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 10f
        paint.typeface = if (isHeader)
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        else
            Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        canvas.drawText(
            label,
            boxLeft + 6f,
            filaBottom - 5f,
            paint
        )

        val texto = "$ " + "%,.2f".format(value)
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.BLACK
        canvas.drawText(
            texto,
            boxLeft + labelWidth + valueWidth - 6f,
            filaBottom - 5f,
            paint
        )

        filaTop = filaBottom
    }

    drawResumenRow("Subtotal", subtotal, true)
    drawResumenRow("Desc. Esp. Adicional", descuentoEsp, false)
    drawResumenRow("Total", total, true)

    // ---------- CONDICIONES COMERCIALES (texto pequeño) ----------
    val condicionesLeft = margin
    val condicionesRight = boxLeft - 12f
    val maxCondicionesWidth = condicionesRight - condicionesLeft

    // Texto SIN numeración (el número lo dibujamos aparte)
    val condicionesLineas = listOf(
        "Precios cotizados en Dólares Americanos.",
        "No incluye IVA.",
        "Para pago en Moneda Nacional aplicará el T.C. vigente al día de pago según Banco de México.",
        "Se requiere 50% anticipo para la programación de instalación.",
        "No hay reembolso por Cancelación después de 3 días del pago de anticipo.",
        "Vigencia de la cotización: 15 días.",
        "El precio incluye instalación dentro de la zona continental de Quintana Roo. " +
                "Para proyectos ubicados fuera de esta zona se hará un cargo extra por concepto de viáticos.",
        "Las medidas contempladas en esta propuesta pueden variar después de la rectificación.",
        "La instalación se programará con base en la agenda y todo proyecto entrará a una fila de instalación. " +
                "Los tiempos de instalación serán de acuerdo a las fechas que se tengan programadas. " +
                "En caso de existir algún espacio disponible antes del periodo máximo, se le notificará al cliente.",
        "Aplicará la garantía de acuerdo al Sistema Contratado y siempre y cuando se cumplan los cuidados " +
                "y recomendaciones entregadas al término de la instalación.",
        "Los descuentos concedidos en esta cotización podrán modificarse si el metraje total disminuye " +
                "o se cancela alguna área.",
        "El costo de adecuaciones o modificaciones estructurales como instalación de PTR o cajillos " +
                "en prefabricados NO ESTÁN INCLUIDOS."
    )

    val condLineHeight = 10f          // alto de cada renglón pequeño
    val tituloExtra = 18f
    val footerTop = pageHeight.toFloat() - bottomBarHeight
    val condicionesLineCount = condicionesLineas.size
    val neededHeight = tituloExtra + condicionesLineCount * condLineHeight
    val margenFooterCond = 24f        // espacio libre respecto al footer

    // Punto Y donde inicia el bloque (que no se pegue al footer)
    val condicionesTop = minOf(
        resumenTop,
        footerTop - neededHeight - 115
    )

    // --- Título ---
    paint.textAlign = Paint.Align.LEFT
    paint.color = Color.BLACK
    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    var condicionesY = condicionesTop + 12f
    canvas.drawText(
        "Condiciones Comerciales:",
        condicionesLeft,
        condicionesY,
        paint
    )

    // --- Texto normal ---
    paint.textSize = 8f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    condicionesY += condLineHeight

    // Dibuja un renglón con número, y hace wrap del texto alineado debajo del número
    fun drawConditionWithNumber(
        index: Int,
        text: String,
        startY: Float
    ): Float {
        val numberPrefix = "$index.- "
        val numberWidth = paint.measureText(numberPrefix)
        val maxTextWidth = maxCondicionesWidth - numberWidth

        // Partimos el TEXTO (sin número) en varias líneas
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) > maxTextWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            } else {
                currentLine = candidate
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        var currentY = startY

        if (lines.isNotEmpty()) {
            // Primera línea: número + texto
            canvas.drawText(numberPrefix, condicionesLeft, currentY, paint)
            canvas.drawText(
                lines[0],
                condicionesLeft + numberWidth,
                currentY,
                paint
            )
            currentY += condLineHeight

            // Resto de líneas: solo texto, alineado después del número
            for (i in 1 until lines.size) {
                canvas.drawText(
                    lines[i],
                    condicionesLeft + numberWidth,
                    currentY,
                    paint
                )
                currentY += condLineHeight
            }
        }

        return currentY
    }

    // Dibujar las 12 condiciones
    condicionesLineas.forEachIndexed { index, linea ->
        condicionesY = drawConditionWithNumber(index + 1, linea, condicionesY)
    }

    // FIN DE LA PÁGINA
    pdfDocument.finishPage(page)

    // ---------- GUARDAR ARCHIVO ----------
    val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
    if (docsDir == null) {
        pdfDocument.close()
        return null
    }

    if (!docsDir.exists()) {
        docsDir.mkdirs()
    }

    val fileName = "Cotizacion_${timeStamp}.pdf"
    val file = File(docsDir, fileName)

    return try {
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        file
    } catch (e: IOException) {
        e.printStackTrace()
        pdfDocument.close()
        null
    }
}
