package com.example.hurricansolutionapp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import android.os.Environment

private const val PREFS_NAME = "cotizaciones_prefs"
private const val KEY_COTIZACIONES = "cotizaciones_json"

private fun leerListaInterna(context: Context): MutableList<Cotizacion> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_COTIZACIONES, null) ?: return mutableListOf()

    return try {
        val gson = Gson()
        val type = object : TypeToken<List<Cotizacion>>() {}.type
        val lista = gson.fromJson<List<Cotizacion>>(json, type) ?: emptyList()
        lista.toMutableList()
    } catch (e: Exception) {
        mutableListOf()
    }
}

private fun guardarListaInterna(context: Context, lista: List<Cotizacion>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val gson = Gson()
    val json = gson.toJson(lista)
    prefs.edit().putString(KEY_COTIZACIONES, json).apply()
}

/**
 * 🔹 Agrega una nueva cotización al almacenamiento local.
 */
fun guardarCotizacionLocal(context: Context, nuevaCotizacion: Cotizacion) {
    val listaActual = leerListaInterna(context)
    listaActual.add(nuevaCotizacion)
    guardarListaInterna(context, listaActual)
}

/**
 * 🔹 Devuelve TODAS las cotizaciones guardadas.
 */
fun obtenerCotizacionesLocal(context: Context): List<Cotizacion> {
    return leerListaInterna(context)
}

/**
 * 🔹 Borra TODAS las cotizaciones.
 */
fun borrarTodasLasCotizacionesLocal(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .remove(KEY_COTIZACIONES)
        .apply()
}

/**
 * 🔹 Devuelve SOLO las cotizaciones que NO se han sincronizado con el CRM.
 */
fun obtenerCotizacionesPendientes(context: Context): List<Cotizacion> {
    return leerListaInterna(context).filter { !it.sincronizada }
}

/**
 * 🔹 Reemplaza la lista completa de cotizaciones.
 */
fun guardarListaCotizacionesLocal(context: Context, lista: List<Cotizacion>) {
    guardarListaInterna(context, lista)
}

/**
 * 🔹 Marca una cotización como sincronizada (por id) y la guarda.
 */
fun marcarCotizacionSincronizada(context: Context, id: Long): Cotizacion? {
    val lista = leerListaInterna(context)
    val index = lista.indexOfFirst { it.id == id }

    if (index == -1) return null

    val cotizacionOriginal = lista[index]
    val cotizacionActualizada = cotizacionOriginal.copy(sincronizada = true)

    lista[index] = cotizacionActualizada
    guardarListaInterna(context, lista)

    return cotizacionActualizada
}

/**
 * 🔹 Borra SOLO una cotización por id.
 */
fun borrarCotizacionLocal(context: Context, id: Long) {
    val lista = leerListaInterna(context)
    val nuevaLista = lista.filter { it.id != id }
    guardarListaInterna(context, nuevaLista)

    fun getPdfFileForCotizacion(context: Context, cotizacion: Cotizacion): File {
        // Carpeta: /Android/data/tu.paquete/files/Documents/
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        if (dir != null && !dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "Cotizacion_${cotizacion.id}.pdf")
    }

    fun generarPdfCotizacion(context: Context, cotizacion: Cotizacion): File? {
        return try {
            val pdf = PdfDocument()

            // Tamaño carta aprox
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Margen izquierdo y posición inicial
            var x = 40f
            var y = 40f

            // 🔵 Encabezado
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("HURRICANE SOLUTION", x, y, paint)
            y += 28f

            paint.textSize = 14f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Cotización de proyecto", x, y, paint)
            y += 30f

            // 🔵 Datos del cliente
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Datos del cliente", x, y, paint)
            y += 18f

            paint.typeface = Typeface.DEFAULT
            canvas.drawText("Cliente: ${cotizacion.clienteNombre}", x, y, paint); y += 16f
            canvas.drawText("Teléfono: ${cotizacion.clienteTelefono}", x, y, paint); y += 16f
            canvas.drawText("Ubicación: ${cotizacion.ubicacion}", x, y, paint); y += 16f
            canvas.drawText("Especialista: ${cotizacion.especialista}", x, y, paint); y += 16f
            canvas.drawText("Fecha: ${cotizacion.fecha}", x, y, paint); y += 24f

            // 🔵 Aperturas (en lista)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Aperturas / Áreas a proteger", x, y, paint)
            y += 20f
            paint.typeface = Typeface.DEFAULT

            cotizacion.ventanas.forEachIndexed { index, v ->
                canvas.drawText("Apertura ${index + 1}", x, y, paint); y += 16f
                canvas.drawText("• Descripción: ${v.descripcion}", x + 16f, y, paint); y += 16f
                canvas.drawText("• Medidas: ${v.alto} x ${v.ancho} m", x + 16f, y, paint); y += 16f
                canvas.drawText("• Área: ${"%.2f".format(v.areaM2)} m²", x + 16f, y, paint); y += 16f
                canvas.drawText("• Precio m²: \$${"%.2f".format(v.precioM2)}", x + 16f, y, paint); y += 16f
                canvas.drawText("• Subtotal: \$${"%,.2f".format(v.subtotal)}", x + 16f, y, paint); y += 22f
            }

            // 🔵 Totales
            y += 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(
                "Subtotal: \$${"%,.2f".format(cotizacion.subtotal)}",
                x,
                y,
                paint
            )
            y += 16f
            canvas.drawText(
                "IVA (${(cotizacion.ivaPorcentaje * 100).toInt()}%): \$${"%,.2f".format(cotizacion.iva)}",
                x,
                y,
                paint
            )
            y += 16f
            canvas.drawText(
                "TOTAL: \$${"%,.2f".format(cotizacion.total)}",
                x,
                y,
                paint
            )

            pdf.finishPage(page)

            // 📁 Guardar en /Android/data/…/files/Documents/Cotizacion_ID.pdf
            val file = getPdfFileForCotizacion(context, cotizacion)
            FileOutputStream(file).use { out ->
                pdf.writeTo(out)
            }
            pdf.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
