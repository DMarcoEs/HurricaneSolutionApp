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

    // ======================= ENCABEZADO (LOGOS + LEMA) =======================
    fun drawHeader(canvas: Canvas) {

        try {
            val options = BitmapFactory.Options().apply { inScaled = false }

            // ================== LOGO HURRICANE (IZQUIERDA) ==================
            val rawLogo = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.logo_header_new,
                options
            )

            // Recortamos márgenes transparentes del logo HS
            val croppedLogo = cropTransparent(rawLogo)

            // MISMO alto que ya probaste (no cambiamos tamaño visual)
            val targetHeight = 45f
            val aspectRatio = croppedLogo.width.toFloat() / croppedLogo.height.toFloat()
            val destHeight = targetHeight
            val destWidth = destHeight * aspectRatio

            // Banda superior donde viven los logos
            val bandTop = 0f
            val bandBottom = headerBarHeight       // 90f
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

            // ================== LOGO MADE IN USA (DERECHA) ==================
            val rawUsa = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.made_in_usa,   // recurso en drawable
                options
            )

            // También recortamos márgenes transparentes
            val usaCropped = cropTransparent(rawUsa)

            // Un poquito más pequeño que antes
            val usaTargetHeight = 38f
            val usaAspect = usaCropped.width.toFloat() / usaCropped.height.toFloat()
            val usaDestHeight = usaTargetHeight
            val usaDestWidth = usaDestHeight * usaAspect

            // Misma banda, pero lo subimos un pelín (-3f)
            val bandCenterYUsa = bandCenterY
            val usaTop = bandCenterYUsa - usaDestHeight / 2f - 3f
            val usaRight = pageWidth.toFloat() - margin
            val usaLeft = usaRight - usaDestWidth

            val usaRect = RectF(
                usaLeft,
                usaTop,
                usaRight,
                usaTop + usaDestHeight
            )

            // Escala de grises (para que combine con el PDF)
            val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
            val usaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }

            canvas.drawBitmap(usaCropped, null, usaRect, usaPaint)

            // ================== LEMA CENTRADO ==================
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

    // ======================= CAJA DEL FOLIO (AL LADO DEL TÍTULO) =======================
    fun drawFolioBox(canvas: Canvas, titleY: Float) {
        val folioTexto = "Folio: ${cotizacion.folio}"

        // Config texto
        paint.color = Color.BLACK
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textAlign = Paint.Align.LEFT

        val textWidth = paint.measureText(folioTexto)

        val boxPaddingH = 6f
        val boxHeight = 18f

        // Queremos que el centro de la caja quede alineado con el título
        val boxCenterY = titleY
        val boxTop = boxCenterY - boxHeight / 2f
        val boxBottom = boxTop + boxHeight

        // Alineado al margen derecho
        val boxRight = pageWidth.toFloat() - margin
        val boxLeft = boxRight - textWidth - boxPaddingH * 2

        // Fondo blanco de la caja
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, boxPaint)

        // Borde gris
        boxPaint.style = Paint.Style.STROKE
        boxPaint.color = Color.DKGRAY
        boxPaint.strokeWidth = 0.8f
        canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, boxPaint)

        // Texto dentro de la caja
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.LEFT

        val textX = boxLeft + boxPaddingH
        val textY = boxTop + boxHeight / 2f - (paint.descent() + paint.ascent()) / 2f

        canvas.drawText(folioTexto, textX, textY, paint)
    }

    // ======================= FOOTER =======================
    fun drawFooter(canvas: Canvas) {
        val bottomBarTop = pageHeight.toFloat() - bottomBarHeight

        // Barra negra inferior
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            0f,
            bottomBarTop,
            pageWidth.toFloat(),
            bottomBarTop + bottomBarHeight,
            barPaint
        )

        // Texto blanco
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        // --------- Cargar íconos del footer ----------
        val iconOptions = BitmapFactory.Options().apply { inScaled = false }
        val iconSize = 10f

        fun loadIcon(resId: Int): Bitmap? =
            BitmapFactory.decodeResource(context.resources, resId, iconOptions)

        val iconMail = loadIcon(R.drawable.ic_footer_mail)
        val iconWeb = loadIcon(R.drawable.ic_footer_web)
        val iconWhatsRaw = loadIcon(R.drawable.ic_footer_whatsapp)
        val iconLocation = loadIcon(R.drawable.ic_footer_location)

        // Pintar WhatsApp en blanco (el PNG original es negro)
        val iconPaintWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        }

        fun drawIcon(
            bitmap: Bitmap?,
            centerX: Float,
            centerY: Float,
            size: Float = iconSize,
            paint: Paint? = null
        ) {
            if (bitmap == null) return
            val half = size / 2f
            val rect = RectF(
                centerX - half,
                centerY - half,
                centerX + half,
                centerY + half
            )
            canvas.drawBitmap(bitmap, null, rect, paint)
        }

        // --------- Textos de contacto ----------
        val email1 = "administraciondeventas@hurricanesolution.com"
        val email2 = "protegiendo@hurricanesolution.com"
        val web = "www.hurricanesolution.com"
        val phone = "9848035014 / 9987052145"
        val address = "Dirección: Av. XXX, Playa del Carmen, Q. Roo" // luego la cambias

// Coordenadas base (vertical)
// Todas las líneas con la MISMA separación para que queden como en tus líneas 1, 2 y 3
        val line1Y = bottomBarTop + 14f      // 1) correo 1 / web
        val line2Y = line1Y + 19f            // 2) correo 2 / teléfono
        val line3Y = line2Y + 20f            // 3) dirección (misma distancia que entre 1 y 2)

        // Para alinear icono con el centro visual del texto
        fun iconCenterYForText(baselineY: Float): Float =
            baselineY - textPaint.textSize / 2f + 1f

        // ===================== LADO IZQUIERDO =====================
        val leftStartX = margin
        val leftIconCenterX = leftStartX + iconSize / 2f
        val leftTextStartX = leftStartX + iconSize + 4f

        // Email 1
        drawIcon(
            iconMail,
            leftIconCenterX,
            iconCenterYForText(line1Y)
        )
        canvas.drawText(email1, leftTextStartX, line1Y, textPaint)

        // Email 2
        drawIcon(
            iconMail,
            leftIconCenterX,
            iconCenterYForText(line2Y)
        )
        canvas.drawText(email2, leftTextStartX, line2Y, textPaint)

        // Dirección (con ícono de ubicación)
        drawIcon(
            iconLocation,
            leftIconCenterX,
            iconCenterYForText(line3Y)
        )
        canvas.drawText(address, leftTextStartX, line3Y, textPaint)

// ===================== LADO DERECHO =====================
        textPaint.textAlign = Paint.Align.LEFT

        val rightStartX = pageWidth.toFloat() - margin - 240f
        val rightIconCenterX = rightStartX + iconSize / 2f
        val rightTextStartX = rightStartX + iconSize + 4f

// Web alineada con el primer correo
        drawIcon(
            iconWeb,
            rightIconCenterX,
            iconCenterYForText(line1Y)
        )
        canvas.drawText(web, rightTextStartX, line1Y, textPaint)

// Teléfono (Whats) alineado con el segundo correo
        drawIcon(
            iconWhatsRaw,
            rightIconCenterX,
            iconCenterYForText(line2Y)
        )
        canvas.drawText(phone, rightTextStartX, line2Y, textPaint)


        // ===================== REDES SOCIALES (debajo del teléfono) =====================

        // Cargar íconos de redes (lado derecho, debajo del teléfono)
        val iconFacebook = loadIcon(R.drawable.ic_footer_facebook)
        val iconLinkedIn = loadIcon(R.drawable.ic_footer_linkedin)
        val iconYoutube = loadIcon(R.drawable.ic_footer_youtube)
        val iconTikTok = loadIcon(R.drawable.ic_footer_tiktok)

        // Coordenada base para la fila de íconos (un poco debajo de la línea 2)
        val socialY = line2Y + 18f    // 18 px debajo del número

        // Punto base alineado con la columna derecha
        var socialX = pageWidth.toFloat() - margin - 10f

        // Función para dibujar iconos pequeños a la derecha
        fun drawSocialIcon(bitmap: Bitmap?, centerX: Float, offsetY: Float = 0f) {
            val cy = iconCenterYForText(socialY) + offsetY
            drawIcon(bitmap, centerX, cy, size = 10f, paint = iconPaintWhite)
        }

        // Distancia entre iconos
        val gap = 16f

// Dibujar íconos de derecha a izquierda: TikTok, YouTube, LinkedIn, Facebook
        drawSocialIcon(iconTikTok, socialX)
        socialX -= gap

// Bajamos ligeramente YouTube para alinearlo visualmente con los demás
        drawSocialIcon(iconYoutube, socialX, offsetY = 1.5f)
        socialX -= gap

        drawSocialIcon(iconLinkedIn, socialX)
        socialX -= gap

        drawSocialIcon(iconFacebook, socialX)

    }

    // ======================= UTILIDAD PARA HACER WRAP DE TEXTO =======================
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
        val txtMontaje = cotizacion.tipoMontaje
        val txtAdecuaciones = v.adecuacion

        val preciosPorProducto: List<String> = productosSeleccionados.map { producto ->
            val monto = v.subtotalPorProducto(producto)
            "$ " + "%,.2f".format(monto)
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
    val condLineCount = 14            // súbelo un poquito (por los wraps reales)
    val condLineHeight = 7.5f         // déjalo igual (tú ya lo dejaste bien)
    val condicionesMinBlock = 16f + condLineCount * condLineHeight + 45f
    val resumenBlockHeight = 18f * 3
    val extraSpaceNeededLastPage = condicionesMinBlock + resumenBlockHeight + 16f

    // ---------- Empezamos a dibujar páginas ----------
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas = page.canvas

    // HEADER
    drawHeader(canvas)

    // =================== TÍTULO Y CAJA DE FOLIO ===================,
    // Título "COTIZACIÓN DE PROYECTO" centrado
    paint.color = Color.BLACK
    paint.textSize = 16f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER   // el X será el centro del texto

    val titulo = "COTIZACIÓN DE PROYECTO"
    val tituloY = 105f

    canvas.drawText(
        titulo,
        pageWidth / 2f,        // centro horizontal de la página
        tituloY,               // altura del título
        paint
    )

    // Caja de folio alineada verticalmente con el título
    drawFolioBox(canvas, tituloY)

// =================== BLOQUES CLIENTE / ESPECIALISTA (DISEÑO TIPO HOJA) ===================
    paint.textAlign = Paint.Align.LEFT

    var y = 140f

    val leftX = margin

// ✅ Tamaños más “como hoja” (no estorbosos)
    val leftBlockWidth = 255f
    val rightBlockWidth = 245f

// ✅ Con esto el bloque derecho se pega al margen derecho y queda un “hueco” al centro
    val rightX = pageWidth.toFloat() - margin - rightBlockWidth

    val rowH = 16f
    val rowGap = 3f

// ✅ Label más angosto para que el valor tenga más espacio
    val gap = 8f // separación entre el bloque negro y la celda del valor
    val labelW = 95f
    val paddingX = 6f

    val borderColor = Color.DKGRAY
    val labelBg = Color.BLACK   // ✅ NEGRO real (como todo tu PDF)

    fun drawInfoRow(
        x: Float,
        yTop: Float,
        label: String,
        value: String,
        blockW: Float
    ): Float {

        val rowH = 18f
        val rowGap = 6f

        // 🔧 Ajusta estos 3 para afinar el look
        val labelW = 135f          // más ancho para que no se corte “Nombre del Cliente:”
        val gapW = 8f              // separación blanca entre negro y la celda del valor
        val paddingX = 6f

        val yBottom = yTop + rowH

        val valueX = x + labelW + gapW
        val valueW = blockW - labelW - gapW

        // ---------- 1) BLOQUE NEGRO (solo fill, SIN borde) ----------
        val p = paint
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        canvas.drawRect(x, yTop, x + labelW, yBottom, p)

        // Texto del label (blanco bold)
        p.color = Color.WHITE
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 9f
        p.textAlign = Paint.Align.LEFT
        val textY = yTop + rowH / 2f - (p.descent() + p.ascent()) / 2f
        canvas.drawText(label, x + paddingX, textY, p)

        // ---------- 2) CELDA VALOR (fondo blanco + borde delgado) ----------
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        canvas.drawRect(valueX, yTop, valueX + valueW, yBottom, p)

        p.style = Paint.Style.STROKE
        p.color = Color.BLACK          // ✅ nada de gris
        p.strokeWidth = 0.6f           // ✅ borde delgado
        canvas.drawRect(valueX, yTop, valueX + valueW, yBottom, p)

        // Texto del valor (negro normal) con CLIP para que no invada otras celdas
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        p.textSize = 9f

        canvas.save()
        canvas.clipRect(valueX + 2f, yTop + 1f, valueX + valueW - 2f, yBottom - 1f)
        canvas.drawText(value, valueX + paddingX, textY, p)
        canvas.restore()

        return yBottom + rowGap
    }



// Misma altura para ambos bloques
    var leftY = y
    var rightY = y

// IZQUIERDA
    leftY = drawInfoRow(leftX, leftY, "Nombre del Cliente:", cotizacion.clienteNombre, leftBlockWidth)
    leftY = drawInfoRow(leftX, leftY, "Dirección:", cotizacion.ubicacion, leftBlockWidth)

// DERECHA
    val metrajeFinal = cotizacion.ventanas.sumOf { it.areaM2 }
    rightY = drawInfoRow(rightX, rightY, "Especialista:", cotizacion.especialista, rightBlockWidth)
    rightY = drawInfoRow(rightX, rightY, "Fecha:", cotizacion.fecha, rightBlockWidth)
    rightY = drawInfoRow(rightX, rightY, "Metraje Total:", "%.2f m²".format(metrajeFinal), rightBlockWidth)

// ✅ Arrancamos la tabla debajo del bloque más largo (sin estorbar)
    y = maxOf(leftY, rightY) + 12f



    // =================== ENCABEZADO TABLA ===================
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
            val centerY =
                startY + headerHeight / 2f - (paint.descent() + paint.ascent()) / 2f
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
            pageInfo =
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas

            // Header (sin datos de cliente en páginas siguientes)
            drawHeader(canvas)

            // Nuevo inicio de tabla en esta página
            y = 95f
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
            var textY =
                y + (rowLayout.height - totalTextHeight) / 2f + cellLineHeight - 3f

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

        // Una celda por cada producto en esta medida
        rowLayout.linesPreciosPorProducto.forEach { priceLines ->
            drawBodyCell(priceLines, colPriceW)
        }

        y += rowLayout.height
    }

// ---------- ÚLTIMA PÁGINA: Resumen + Condiciones + Footer ----------

// 1) Totales por producto (usa tu lógica de modelo)
    val totalesPorProducto: Map<TipoProducto, Double> =
        productosSeleccionados.associateWith { producto ->
            cotizacion.totalPorProducto(producto)
        }

// 2) Área total de la cotización (suma de todos los m²)
    val areaTotal = cotizacion.ventanas.sumOf { it.areaM2 }

// 3) Descuento capturado en la cotización: dólares por m²
    val descuentoDolaresPorM2 = cotizacion.descuentoDolaresPorM2

// 4) Mapa de porcentaje de descuento por producto
//    ej. si HS875 = 150 y descuento = 5 => 5/150 = 3.33%
    val descuentosPorcentaje: Map<TipoProducto, Double> =
        productosSeleccionados.associateWith { producto ->
            val subtotalProducto = totalesPorProducto[producto] ?: 0.0

            // si no hay área o subtotal, no aplicamos nada
            if (areaTotal <= 0.0 || subtotalProducto <= 0.0 || descuentoDolaresPorM2 <= 0.0) {
                0.0
            } else {
                val descuentoImporte = areaTotal * descuentoDolaresPorM2
                // porcentaje = descuentoImporte / subtotalProducto * 100
                (descuentoImporte / subtotalProducto) * 100.0
            }
        }

// 5) Precio final por producto aplicando ese porcentaje
    val preciosFinalesPorProducto: Map<TipoProducto, Double> =
        productosSeleccionados.associateWith { producto ->
            val subtotalProducto = totalesPorProducto[producto] ?: 0.0
            val descuentoPct = descuentosPorcentaje[producto] ?: 0.0
            val descuentoImporte = subtotalProducto * (descuentoPct / 100.0)
            (subtotalProducto - descuentoImporte).coerceAtLeast(0.0)
        }


    // --- Geometría del bloque de resumen (derecha, encima del footer) ---
    val labelWidth = 90f
    val boxLeft = startPreciosX - labelWidth
    val boxRight = tableRight
    val boxWidth = boxRight - boxLeft

    val valueColumnsCount = productosSeleccionados.size
    val valueColumnWidth = colPriceW

    val rowHeightResumen = 16f
    val totalResumenRows = 3
    val bloqueAltura = rowHeightResumen * totalResumenRows

    val footerTop = pageHeight.toFloat() - bottomBarHeight

    val margenSobreFooter = 10f
    val resumenTopDesdeAbajo =
        pageHeight.toFloat() - bottomBarHeight - margenSobreFooter - bloqueAltura

    val espacioEntreTablaYResumen = 20f
    val resumenTop = maxOf(
        y + espacioEntreTablaYResumen,
        resumenTopDesdeAbajo
    )

    var filaTop = resumenTop

    fun drawResumenDataRow(
        label: String,
        getTextForCol: (Int, TipoProducto) -> String,
        boldLabel: Boolean
    ) {
        val filaBottom = filaTop + rowHeightResumen

        // Celda etiqueta
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

        // Celdas de valores
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

// Subtotal por producto
    drawResumenDataRow(
        label = "Subtotal",
        getTextForCol = { _, producto ->
            val subtotalProducto = totalesPorProducto[producto] ?: 0.0
            "$ " + "%,.2f".format(subtotalProducto)
        },
        boldLabel = true
    )

// Descuento (en porcentaje, como ya lo tienes)
    drawResumenDataRow(
        label = "Descuento",
        getTextForCol = { _, producto ->
            val pct = descuentosPorcentaje[producto] ?: 0.0
            String.format("%.2f%%", pct)
        },
        boldLabel = false
    )

// Precio Final **con IVA**
    drawResumenDataRow(
        label = "Precio Final con IVA",
        getTextForCol = { _, producto ->
            // Precio ya con DESCUENTO (lo que ya calculaste antes)
            val precioSinIva = preciosFinalesPorProducto[producto] ?: 0.0

            // Aplicamos el 16% de IVA sobre ese precio final
            val precioConIva = precioSinIva * (1.0 + IVA_RATE)

            "$ " + "%,.2f".format(precioConIva)
        },
        boldLabel = true
    )

    // ---- Condiciones Comerciales ----
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
    val condicionesTop = minOf(
        resumenTop,
        footerTop - neededHeight - margenSobreFooterCond
    )

    paint.textAlign = Paint.Align.LEFT
    paint.color = Color.BLACK
    paint.textSize = 8.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    var condicionesY = condicionesTop + 12f
    canvas.drawText(
        "Condiciones Comerciales:",
        condicionesLeft,
        condicionesY,
        paint
    )

    paint.textSize = 6.5f
    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
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