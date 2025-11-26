package com.example.hurricansolutionapp

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.time.format.DateTimeFormatter

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
    var y = 40f

    // ---------- ENCABEZADO NEGRO ----------
    paint.color = Color.BLACK
    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 70f, paint)

    // 👉 LOGO EN EL ENCABEZADO
    try {
        val rawLogo = BitmapFactory.decodeResource(
            context.resources,
            R.drawable.logo_hurricane
        )
        val logo: Bitmap = Bitmap.createScaledBitmap(rawLogo, 120, 40, true)
        canvas.drawBitmap(logo, margin, 15f, null)
    } catch (e: Exception) {
        // Si algo falla con el logo, solo seguimos sin él
        e.printStackTrace()
    }

    // Texto "HURRICANE SOLUTION" a la derecha del logo
    paint.color = Color.WHITE
    paint.textSize = 18f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(
        "HURRICANE SOLUTION",
        margin + 130f,
        45f,
        paint
    )

    // ---------- TÍTULO ----------
    paint.color = Color.BLACK
    paint.textSize = 16f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    val titulo = "COTIZACIÓN DE PROYECTO"
    val tituloWidth = paint.measureText(titulo)
    canvas.drawText(
        titulo,
        (pageWidth - tituloWidth) / 2f,
        100f,
        paint
    )

    y = 130f

    // ---------- BLOQUE CLIENTE (IZQUIERDA) ----------
    paint.textSize = 10f
    val leftX = margin
    val rightX = pageWidth / 2f

    fun drawLabelValue(label: String, value: String, startY: Float): Float {
        val rowHeight = 18f

        // Fondo del label
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(30, 30, 60)
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

        // Texto valor (AHORA SÍ visible)
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(value, leftX + 115f, startY - 5f, paint)

        return startY + 6f
    }

    y = drawLabelValue("Nombre del Cliente:", cotizacion.clienteNombre, y)
    y = drawLabelValue("Dirección:", cotizacion.ubicacion, y + 10f)

    // ---------- BLOQUE DERECHA: ESPECIALISTA / FECHA ----------
    val rightBlockX = rightX + 10f
    var rightY = 130f

    fun drawRightRow(label: String, value: String, startY: Float): Float {
        val rowHeight = 18f

        // Fondo label
        paint.color = Color.rgb(30, 30, 60)
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

    rightY = drawRightRow("Especialista:", cotizacion.especialista, rightY)
    rightY = drawRightRow("Fecha:", cotizacion.fecha, rightY + 10f)

    // ---------- TABLA PRINCIPAL ----------
    y = maxOf(y, rightY) + 30f

    paint.textSize = 10f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.BLACK

    val tableLeft = margin
    val tableRight = pageWidth.toFloat() - margin
    val headerHeight = 18f
    val rowHeight = 16f

    // Columnas
    val colDescripcionW = 160f
    val colCantidadW = 40f
    val colAreaW = 70f
    val colMontajeW = 90f
    val colAdecuacionesW = tableRight - tableLeft -
            (colDescripcionW + colCantidadW + colAreaW + colMontajeW)

    var x = tableLeft

    fun drawHeader(text: String, width: Float) {
        paint.color = Color.rgb(30, 30, 60)
        canvas.drawRect(x, y, x + width, y + headerHeight, paint)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            text,
            x + width / 2f,
            y + headerHeight - 5f,
            paint
        )
        x += width
    }

    drawHeader("Área a proteger", colDescripcionW)
    drawHeader("Cant.", colCantidadW)
    drawHeader("Área total", colAreaW)
    drawHeader("Tipo de montaje", colMontajeW)
    drawHeader("Adecuaciones", colAdecuacionesW)

    paint.textAlign = Paint.Align.LEFT
    paint.color = Color.BLACK
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

    y += headerHeight

    // Filas
    cotizacion.ventanas.forEach { v ->
        x = tableLeft
        val area = "%.2f".format(v.areaM2)
        val cantidad = "1"
        val tipoMontaje = "Flush Mount"   // Aquí luego puedes hacerlo dinámico si lo necesitas
        val adecuaciones = "Por revisar"

        // 👉 SOLO DESCRIPCIÓN (sin “Apertura 1:”)
        canvas.drawText(v.descripcion, x + 4f, y + rowHeight - 4f, paint)
        x += colDescripcionW

        // Cantidad
        canvas.drawText(cantidad, x + 4f, y + rowHeight - 4f, paint)
        x += colCantidadW

        // Área total
        canvas.drawText(area, x + 4f, y + rowHeight - 4f, paint)
        x += colAreaW

        // Tipo de montaje
        canvas.drawText(tipoMontaje, x + 4f, y + rowHeight - 4f, paint)
        x += colMontajeW

        // Adecuaciones
        canvas.drawText(adecuaciones, x + 4f, y + rowHeight - 4f, paint)

        // Línea inferior
        canvas.drawLine(
            tableLeft,
            y + rowHeight,
            tableRight,
            y + rowHeight,
            paint
        )

        y += rowHeight
    }

    y += 25f

    // ---------- TOTAL (simple, sin IVA) ----------
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 12f

    val total = cotizacion.subtotal
    val totalTexto = "Total: $ " + "%,.2f".format(total)

    val totalWidth = paint.measureText(totalTexto)
    canvas.drawText(
        totalTexto,
        tableRight - totalWidth,
        y,
        paint
    )

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
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
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
