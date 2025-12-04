package com.example.hurricansolutionapp

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "cotizaciones_prefs"
private const val KEY_COTIZACIONES = "cotizaciones_json"

private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

fun guardarCotizacionLocal(context: Context, cotizacion: Cotizacion) {
    val prefs = getPrefs(context)

    val listaJson = prefs.getString(KEY_COTIZACIONES, "[]") ?: "[]"
    val array = JSONArray(listaJson)

    val nuevoArray = JSONArray()

    val idParaGuardar = if (cotizacion.id == 0L) {
        System.currentTimeMillis()
    } else {
        cotizacion.id
    }

    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val idExistente = obj.optLong("id", -1L)

        if (idExistente != idParaGuardar) {
            nuevoArray.put(obj)
        }
    }

    val cotizacionJson = JSONObject().apply {
        put("id", idParaGuardar)
        put("folio", cotizacion.folio)
        put("clienteNombre", cotizacion.clienteNombre)
        put("clienteTelefono", cotizacion.clienteTelefono)
        put("ubicacion", cotizacion.ubicacion)
        put("especialista", cotizacion.especialista)
        put("fecha", cotizacion.fecha)
        put("producto", cotizacion.producto.name)

        val ventanasArray = JSONArray()
        cotizacion.ventanas.forEach { v ->
            val vObj = JSONObject().apply {
                put("descripcion", v.descripcion)
                put("alto", v.alto)
                put("ancho", v.ancho)
                put("precioM2", v.precioM2)
                // si luego quieres, aquí también puedes agregar "adecuacion"
            }
            ventanasArray.put(vObj)
        }
        put("ventanas", ventanasArray)
    }


    nuevoArray.put(cotizacionJson)

    prefs.edit()
        .putString(KEY_COTIZACIONES, nuevoArray.toString())
        .apply()
}


fun obtenerCotizacionesLocal(context: Context): List<Cotizacion> {
    val prefs = getPrefs(context)
    val listaJson = prefs.getString(KEY_COTIZACIONES, "[]") ?: "[]"
    val array = JSONArray(listaJson)

    val resultado = mutableListOf<Cotizacion>()

    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)

        val id = obj.optLong("id", System.currentTimeMillis())
        val clienteNombre = obj.optString("clienteNombre", "")
        val clienteTelefono = obj.optString("clienteTelefono", "")
        val ubicacion = obj.optString("ubicacion", "")
        val especialista = obj.optString("especialista", "")
        val fecha = obj.optString("fecha", "")

        val productoName = obj.optString("producto", TipoProducto.HS875.name)
        val producto = runCatching { TipoProducto.valueOf(productoName) }
            .getOrElse { TipoProducto.HS875 }

        // Ventanas
        val ventanasJson = obj.optJSONArray("ventanas") ?: JSONArray()
        val ventanas = mutableListOf<Ventana>()
        for (j in 0 until ventanasJson.length()) {
            val vObj = ventanasJson.getJSONObject(j)
            val descripcion = vObj.optString("descripcion", "Apertura")
            val alto = vObj.optDouble("alto", 0.0)
            val ancho = vObj.optDouble("ancho", 0.0)
            val precioM2 = vObj.optDouble("precioM2", HS875_DEFAULT_PRICE)

            ventanas.add(
                Ventana(
                    descripcion = descripcion,
                    alto = alto,
                    ancho = ancho,
                    precioM2 = precioM2
                    // Si luego tenemos adecuación, aquí la pasamos
                )
            )
        }

        val folio = obj.optString("folio", "")

        val cotizacion = Cotizacion(
            id = id,
            folio = folio,
            clienteNombre = clienteNombre,
            clienteTelefono = clienteTelefono,
            ubicacion = ubicacion,
            especialista = especialista,
            fecha = fecha,
            producto = producto,
            ventanas = ventanas)

        resultado.add(cotizacion)
    }

    // Ordenamos por fecha de creación (opcional)
    return resultado.reversed()
}

/**
 * Elimina UNA cotización por id.
 */
fun borrarCotizacionLocal(context: Context, id: Long) {
    val prefs = getPrefs(context)
    val listaJson = prefs.getString(KEY_COTIZACIONES, "[]") ?: "[]"
    val array = JSONArray(listaJson)

    val nuevoArray = JSONArray()
    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val idExistente = obj.optLong("id", -1L)
        if (idExistente != id) {
            nuevoArray.put(obj)
        }
    }

    prefs.edit()
        .putString(KEY_COTIZACIONES, nuevoArray.toString())
        .apply()
}
