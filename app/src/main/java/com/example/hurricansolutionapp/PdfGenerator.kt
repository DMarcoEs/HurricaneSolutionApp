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
    val margin = 32f
    val headerBarHeight = 90f
    val bottomBarHeight = 60f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Recorta bordes totalmente transparentes del bitmap (escudo/logo sin margen extra)
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
                if (alpha > 0) { // hay contenido visible
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        // Si por alguna razón todo es transparente, regresamos el original
        if (maxX < 0 || maxY < 0) return bitmap

        val cropWidth = maxX - minX + 1
        val cropHeight = maxY - minY + 1
        return Bitmap.createBitmap(bitmap, minX, minY, cropWidth, cropHeight)
    }

    fun drawHeader(canvas: Canvas) {

        try {
            val options = BitmapFactory.Options().apply { inScaled = false }

            val rawLogo = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.logo_header_new,
                options
            )

            // Recortamos márgenes transparentes
            val croppedLogo = cropTransparent(rawLogo)

            // 🔹 MISMO alto que ya elegiste (no cambiamos tu tamaño)
            val targetHeight = 45f

            // NO creamos un bitmap reescalado, usamos el original HD
            val aspectRatio = croppedLogo.width.toFloat() / croppedLogo.height.toFloat()
            val destHeight = targetHeight
            val destWidth = destHeight * aspectRatio

            // Misma banda y misma posición que ya tenías
            val bandTop = 0f
            val bandBottom = headerBarHeight       // 90f
            val bandCenterY = (bandTop + bandBottom) / 2f

            val logoTop = bandCenterY - destHeight / 2f
            val logoLeft = margin

            // ⬇️ Aquí dibujamos el bitmap ORIGINAL en un rectángulo más pequeño
            val destRect = RectF(
                logoLeft,
                logoTop,
                logoLeft + destWidth,
                logoTop + destHeight
            )

            canvas.drawBitmap(croppedLogo, null, destRect, null)

            // ---------- Lema ----------
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

        // ---------- Folio ----------
        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.RIGHT

        val folioTexto = "Folio: ${cotizacion.folio}"

        canvas.drawText(
            folioTexto,
            pageWidth.toFloat() - margin,
            24f,
            paint
        )
    }

    fun drawFooter(canvas: Canvas) {
        val bottomBarTop = pageHeight.toFloat() - bottomBarHeight

        val pf = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            0f,
            bottomBarTop,
            pageWidth.toFloat(),
            bottomBarTop + bottomBarHeight,
            pf
        )

        pf.color = Color.WHITE
        pf.textSize = 8f
        pf.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        pf.textAlign = Paint.Align.LEFT

        val footerLine1 = "administraciondeventas@hurricanesolution.com"
        val footerLine2 = "protegiendo@hurricanesolution.com"
        val footerLine3 = "www.hurricanesolution.com   9848035014 / 9841478271"

        val footerTextLeft = margin
        val footerTextTop = bottomBarTop + 15f

        canvas.drawText(footerLine1, footerTextLeft, footerTextTop, pf)
        canvas.drawText(footerLine2, footerTextLeft, footerTextTop + 12f, pf)
        canvas.drawText(footerLine3, footerTextLeft, footerTextTop + 24f, pf)
    }

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
    // ---------- Productos seleccionados para la tabla de precios ----------
    val productosSeleccionados: List<TipoProducto> = run {
        val base = try {
            cotizacion.productos
        } catch (e: Exception) {
            emptyList<TipoProducto>()
        }

        val lista = if (base.isNotEmpty()) base else listOf(cotizacion.producto)

        lista
            .distinct()
            .sortedBy { p ->
                when (p) {
                    TipoProducto.HS875 -> 0
                    TipoProducto.HS1250 -> 1
                    TipoProducto.HS1500 -> 2
                    TipoProducto.PERSONALIZADO -> 3
                }
            }
    }
    // ---------- PRE-CÁLCULO DE FILAS DE TABLA ----------

// Config tabla
    val tableLeft = margin
    val tableRight = pageWidth.toFloat() - margin
    val headerHeight = 42f
    val bodyTextSize = 9f
    val cellPadding = 4f
    val cellLineHeight = 12f

// 👇 Reducimos un poco estas columnas para darle MÁS espacio a los HS
    val colAreaW = 150f
    val colAreaTotalW = 50f
    val colMontajeW = 70f
    val colAdecuacionesW = 70f

// Todo el espacio disponible para precios (uno o varios productos)
    val priceColumnsCount = productosSeleccionados.size.coerceAtLeast(1)
    val colPricesTotalW = tableRight - tableLeft -
            (colAreaW + colAreaTotalW + colMontajeW + colAdecuacionesW)
    val colPriceW = colPricesTotalW / priceColumnsCount

    // 👇 X donde empiezan las columnas de precios en la tabla principal
    val startPreciosX =
        tableLeft + colAreaW + colAreaTotalW + colMontajeW + colAdecuacionesW


    data class RowLayout(
        val linesArea: List<String>,
        val linesAreaTotal: List<String>,
        val linesMontaje: List<String>,
        val linesAdecuaciones: List<String>,
        val linesPreciosPorProducto: List<List<String>>, // una lista por producto
        val height: Float
    )

    paint.textSize = bodyTextSize
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    val filas = mutableListOf<RowLayout>()

    cotizacion.ventanas.forEach { v ->
        val txtArea = v.descripcion
        val txtAreaTotal = "%.2f".format(v.areaM2)
        val txtMontaje = "Flush Mount"
        val txtAdecuaciones = v.adecuacion

        // 🔹 Precio calculado por cada producto seleccionado
        val preciosPorProducto: List<String> = productosSeleccionados.map { producto ->
            val monto = v.subtotalPorProducto(producto)   // usa HS875/1250/1500 reales
            "USD " + "%,.2f".format(monto)
        }

        val linesArea = wrapText(txtArea, colAreaW - cellPadding * 2, paint)
        val linesAreaTotal = wrapText(txtAreaTotal, colAreaTotalW - cellPadding * 2, paint)
        val linesMontaje = wrapText(txtMontaje, colMontajeW - cellPadding * 2, paint)
        val linesAdecuaciones = wrapText(txtAdecuaciones, colAdecuacionesW - cellPadding * 2, paint)

        // Por ahora cada precio es una sola línea, pero lo dejamos preparado
        val linesPreciosPorProducto: List<List<String>> =
            preciosPorProducto.map { listOf(it) }

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


    // Altura mínima que necesitamos para Resumen + Condiciones
    val condLineCount = 12
    val condLineHeight = 9f
    val condicionesMinBlock = 18f + condLineCount * condLineHeight + 80f // aprox
    val resumenBlockHeight = 18f * 3
    val extraSpaceNeededLastPage = condicionesMinBlock + resumenBlockHeight + 20f

    // ---------- Empezamos a dibujar páginas ----------

    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    // HEADER
    drawHeader(canvas)

    // Título + datos cliente SOLO en primera página
// Título "COTIZACIÓN DE PROYECTO" centrado
    paint.color = Color.BLACK
    paint.textSize = 16f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER   // el X será el centro del texto

    val titulo = "COTIZACIÓN DE PROYECTO"

    canvas.drawText(
        titulo,
        pageWidth / 2f,        // centro horizontal de la página
        120f,                  // misma altura que ya tenías
        paint
    )


    // Bloques cliente / especialista / fecha / metraje
    paint.textSize = 10f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    var y = 160f
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
        paint.textAlign = Paint.Align.LEFT          // 👈
        canvas.drawText(label, leftX + 4f, startY - 5f, paint)

        // Texto valor
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT          // 👈
        canvas.drawText(value, leftX + 115f, startY - 5f, paint)

        return startY + 6f
    }


    y = drawLabelValue("Nombre del Cliente:", cotizacion.clienteNombre, y)
    y = drawLabelValue("Dirección:", cotizacion.ubicacion, y + 10f)

    val rightBlockX = rightX + 10f
    var rightY = 160f

    fun drawRightRow(label: String, value: String, startY: Float): Float {
        val rowHeight = 18f

        paint.color = Color.BLACK
        paint.style = Paint.Style.FILL
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
        paint.textAlign = Paint.Align.LEFT          // 👈
        canvas.drawText(label, rightBlockX + 4f, startY - 5f, paint)

        // Valor
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT          // 👈
        canvas.drawText(value, rightBlockX + 115f, startY - 5f, paint)

        return startY + 6f
    }

    val metrajeFinal = cotizacion.ventanas.sumOf { it.areaM2 }

// 1) Especialista
    rightY = drawRightRow("Especialista:", cotizacion.especialista, rightY)

// 2) Fecha
    rightY = drawRightRow("Fecha:", cotizacion.fecha, rightY + 10f)

// 3) Metraje final
    rightY = drawRightRow(
        "Metraje Total:",
        "%.2f m²".format(metrajeFinal),
        rightY + 10f
    )

    y = maxOf(y, rightY) + 30f

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
            val centerY = startY + headerHeight / 2f -
                    (paint.descent() + paint.ascent()) / 2f
            canvas.drawText(text, centerX, centerY, paint)
            paint.color = Color.BLACK
            x += width
        }

        // columnas fijas
        drawHeaderCell("Área a proteger", colAreaW)
        drawHeaderCell("Área total", colAreaTotalW)
        drawHeaderCell("Tipo de montaje", colMontajeW)
        drawHeaderCell("Adecuaciones", colAdecuacionesW)

        // columnas de productos: HS + material en dos líneas
        productosSeleccionados.forEach { producto ->
            val hsLabel = when (producto) {
                TipoProducto.HS875 -> "HS-875"
                TipoProducto.HS1250 -> "HS-1250"
                TipoProducto.HS1500 -> "HS-1500"
                TipoProducto.PERSONALIZADO -> "Pers."
            }

            val materialLabel = when (producto) {
                TipoProducto.HS875 -> "Polipropileno"
                TipoProducto.HS1250 -> "Poliester y Aramida"
                TipoProducto.HS1500 -> "Nylon Balístico"
                TipoProducto.PERSONALIZADO -> ""
            }

            // fondo negro de la celda
            canvas.drawRect(x, startY, x + colPriceW, startY + headerHeight, paint)

            val centerX = x + colPriceW / 2f
            val centerY = startY + headerHeight / 2f

            // línea 1: HS-875 / HS-1250 / HS-1500
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            canvas.drawText(hsLabel, centerX, centerY - 5f, paint)

            // línea 2: material
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = if (productosSeleccionados.size >= 3) 7.5f else 8.5f
            canvas.drawText(materialLabel, centerX, centerY + 8f, paint)

            paint.color = Color.BLACK
            x += colPriceW
        }

        // 👈 ahora sí el return está fuera del forEach
        return startY + headerHeight
    }

    // Dibujamos encabezado de tabla en la primera página
    y = drawTableHeader(y)

    paint.textSize = bodyTextSize
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textAlign = Paint.Align.CENTER

    val maxTableBottomPerPage = pageHeight - bottomBarHeight - 20f

    // ---------- Dibujar filas con salto de página ----------

    filas.forEachIndexed { index, rowLayout ->
        val isLastRow = index == filas.lastIndex
        val extraNeeded = if (isLastRow) extraSpaceNeededLastPage else 0f

        // ¿Cabe esta fila + (espacio para resumen/condiciones si es la última)?
        if (y + rowLayout.height + extraNeeded > maxTableBottomPerPage) {
            // Cerrar página actual (solo tabla)
            drawFooter(canvas)
            pdfDocument.finishPage(page)

            // Nueva página
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas

            // Header (sin datos de cliente en páginas siguientes)
            drawHeader(canvas)

            // Nuevo inicio de tabla en esta página
            y = 130f
            y = drawTableHeader(y)

            paint.textSize = bodyTextSize
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textAlign = Paint.Align.CENTER
        }

        // Dibujar la fila actual
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

        drawBodyCell(rowLayout.linesArea, colAreaW)
        drawBodyCell(rowLayout.linesAreaTotal, colAreaTotalW)
        drawBodyCell(rowLayout.linesMontaje, colMontajeW)
        drawBodyCell(rowLayout.linesAdecuaciones, colAdecuacionesW)

// 🔹 Una celda por cada producto en esta medida
        rowLayout.linesPreciosPorProducto.forEach { priceLines ->
            drawBodyCell(priceLines, colPriceW)
        }

        y += rowLayout.height
    }

// ---------- ÚLTIMA PÁGINA: Resumen + Condiciones + Footer ----------

// Totales por producto (usa la lógica de tu modelo)
    val totalesPorProducto: Map<TipoProducto, Double> =
        productosSeleccionados.associateWith { producto ->
            cotizacion.totalPorProducto(producto)
        }

// De momento el descuento será 0 % (estructura lista por si luego la cambias)
    val descuentosPorcentaje: Map<TipoProducto, Double> =
        productosSeleccionados.associateWith { 0.0 }

    val preciosFinalesPorProducto: Map<TipoProducto, Double> =
        productosSeleccionados.associateWith { producto ->
            val subtotalProducto = totalesPorProducto[producto] ?: 0.0
            val descuentoPct = descuentosPorcentaje[producto] ?: 0.0
            // Si luego quieres aplicar %, aquí se ajusta
            val descuentoImporte = subtotalProducto * (descuentoPct / 100.0)
            subtotalProducto - descuentoImporte
        }

// --- Geometría del bloque de resumen (derecha, encima del footer) ---

// La parte de precios del resumen debe alinearse con las columnas HS de la tabla
    val labelWidth = 90f                          // 🔹 un poco más ancho para "Precio Final sin IVA"
    val boxLeft = startPreciosX - labelWidth      // sigue alineado con las columnas de precios
    val boxRight = tableRight
    val boxWidth = boxRight - boxLeft

    val valueColumnsCount = productosSeleccionados.size
// Cada columna de valor mide LO MISMO que en la tabla
    val valueColumnWidth = colPriceW


    val rowHeight = 16f
    val totalResumenRows = 3          // 2 encabezados + 3 filas (Subtotal, Descuento, Precio Final)
    val bloqueAltura = rowHeight * totalResumenRows

    val footerTop = pageHeight.toFloat() - bottomBarHeight

// Posición base para el bloque de resumen (encima del footer)
    val margenSobreFooter = 10f
    val resumenTopDesdeAbajo =
        pageHeight.toFloat() - bottomBarHeight - margenSobreFooter - bloqueAltura

    val espacioEntreTablaYResumen = 20f
    val resumenTop = maxOf(
        y + espacioEntreTablaYResumen,
        resumenTopDesdeAbajo
    )

    var filaTop = resumenTop

    // Dibuja una fila de datos: etiqueta a la izquierda, valores a la derecha
    fun drawResumenDataRow(
        label: String,
        getTextForCol: (Int, TipoProducto) -> String,
        boldLabel: Boolean
    ) {
        val filaBottom = filaTop + rowHeight

        // Celda etiqueta (lado izquierdo)
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK
        canvas.drawRect(
            boxLeft,
            filaTop,
            boxLeft + labelWidth,
            filaBottom,
            paint
        )

        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.LEFT

// 🔹 Si la etiqueta es muy larga (ej: "Precio Final sin IVA"), usa fuente un poco más pequeña
        paint.textSize = if (label.length > 16) 8f else 9f

        paint.typeface = if (boldLabel)
            Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        else
            Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        canvas.drawText(
            label,
            boxLeft + 6f,
            filaBottom - 4f,
            paint
        )

        // Celdas de valores (una por producto)
        productosSeleccionados.forEachIndexed { index, producto ->
            val left = boxLeft + labelWidth + index * valueColumnWidth
            val right = left + valueColumnWidth

            paint.style = Paint.Style.STROKE
            paint.color = Color.BLACK
            canvas.drawRect(left, filaTop, right, filaBottom, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.RIGHT
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            val text = getTextForCol(index, producto)

            canvas.drawText(
                text,
                right - 4f,
                filaBottom - 4f,
                paint
            )
        }

        filaTop = filaBottom
    }


// ------------ Filas de datos ------------

// Subtotal por producto
    drawResumenDataRow(
        label = "Subtotal",
        getTextForCol = { _, producto ->
            val subtotalProducto = totalesPorProducto[producto] ?: 0.0
            "$ " + "%,.2f".format(subtotalProducto)
        },
        boldLabel = true
    )

// Descuento (por ahora mostramos 0.00 %)
    drawResumenDataRow(
        label = "Descuento",
        getTextForCol = { _, producto ->
            val pct = descuentosPorcentaje[producto] ?: 0.0
            String.format("%.2f%%", pct)
        },
        boldLabel = false
    )

// Precio Final sin IVA
    drawResumenDataRow(
        label = "Precio Final sin IVA",
        getTextForCol = { _, producto ->
            val finalProducto = preciosFinalesPorProducto[producto] ?: 0.0
            "$ " + "%,.2f".format(finalProducto)
        },
        boldLabel = true
    )


    // ---- Condiciones Comerciales ----
    val condicionesLeft = margin
    val condicionesRight = boxLeft - 12f
    val maxCondicionesWidth = condicionesRight - condicionesLeft

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

    val tituloExtra = 18f
    val neededHeight = tituloExtra + condicionesLineas.size * condLineHeight

    val margenSobreFooterCond = 130f
    val condicionesTop = minOf(
        resumenTop,                           // opción 1: al nivel de la caja de resumen
        footerTop - neededHeight - margenSobreFooterCond  // opción 2: anclado desde abajo
    )

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

    paint.textSize = 7f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)  // 👈 cursiva
    condicionesY += condLineHeight

    fun drawConditionWithNumber(
        index: Int,
        text: String,
        startY: Float
    ): Float {
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
            canvas.drawText(
                lines[0],
                condicionesLeft + numberWidth,
                currentY,
                paint
            )
            currentY += condLineHeight

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

    condicionesLineas.forEachIndexed { index, linea ->
        condicionesY = drawConditionWithNumber(index + 1, linea, condicionesY)
    }

    // Footer sólo en la última página
    drawFooter(canvas)

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

    val timeStamp = LocalDateTime.now().format(
        DateTimeFormatter.ofPattern("yyyyMMdd")
    )

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
