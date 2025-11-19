package com.example.hurricansolutionapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

fun generarPdfCotizacion(context: Context, cotizacion: Cotizacion): File? {
    return try {
        // 1. Crear documento PDF
        val pdfDocument = PdfDocument()

        // Tamaño de página aproximado A4 en puntos (1/72 pulgadas)
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        paint.textSize = 12f

        var x = 40f
        var y = 40f

        // Título
        paint.textSize = 16f
        canvas.drawText("Hurricane Solutions", x, y, paint)
        y += 24f
        paint.textSize = 14f
        canvas.drawText("Resumen de cotización", x, y, paint)
        y += 24f

        paint.textSize = 12f

        // Datos del cliente
        canvas.drawText("Cliente: ${cotizacion.clienteNombre}", x, y, paint); y += 16f
        canvas.drawText("Tel: ${cotizacion.clienteTelefono}", x, y, paint); y += 16f

        // Proyecto
        canvas.drawText("Ubicación: ${cotizacion.ubicacion}", x, y, paint); y += 16f
        canvas.drawText("Especialista: ${cotizacion.especialista}", x, y, paint); y += 16f
        canvas.drawText("Fecha: ${cotizacion.fecha}", x, y, paint); y += 24f

        // Encabezado de elementos
        canvas.drawText("Elementos cotizados:", x, y, paint); y += 20f

        // Detalle de cada elemento
        cotizacion.ventanas.forEachIndexed { index, ventana ->
            val linea = "${index + 1}) ${ventana.descripcion} - " +
                    "${ventana.alto} x ${ventana.ancho} m - " +
                    String.format("%.2f m² - $%.2f/m² - $%.2f",
                        ventana.areaM2,
                        ventana.precioM2,
                        ventana.subtotal
                    )

            // Si nos acercamos al final de la página, saltamos línea extra
            if (y > 780f) {
                y = 40f
            }
            canvas.drawText(linea, x, y, paint)
            y += 16f
        }

        y += 24f

        // Totales
        canvas.drawText(
            "Subtotal: $${"%,.2f".format(cotizacion.subtotal)}",
            x,
            y,
            paint
        ); y += 16f

        canvas.drawText(
            "IVA: $${"%,.2f".format(cotizacion.iva)}",
            x,
            y,
            paint
        ); y += 16f

        canvas.drawText(
            "Total: $${"%,.2f".format(cotizacion.total)}",
            x,
            y,
            paint
        ); y += 16f

        // Cerrar página
        pdfDocument.finishPage(page)

        // 2. Crear archivo en carpeta de documentos de la app
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (dir != null && !dir.exists()) {
            dir.mkdirs()
        }

        val fileName = "cotizacion_${System.currentTimeMillis()}.pdf"
        val file = File(dir, fileName)

        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        outputStream.close()

        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
