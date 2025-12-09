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

/**
 * Guarda o actualiza una cotización en SharedPreferences.
 */
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

    // Copiamos todas las cotizaciones excepto la que tenga el mismo id (para actualizarla)
    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val idExistente = obj.optLong("id", -1L)
        if (idExistente != idParaGuardar) {
            nuevoArray.put(obj)
        }
    }

    // Objeto JSON de la nueva/actualizada cotización
    val cotizacionJson = JSONObject().apply {
        put("id", idParaGuardar)
        put("folio", cotizacion.folio)
        put("clienteNombre", cotizacion.clienteNombre)
        put("clienteTelefono", cotizacion.clienteTelefono)
        put("ubicacion", cotizacion.ubicacion)
        put("especialista", cotizacion.especialista)
        put("fecha", cotizacion.fecha)
        put("producto", cotizacion.producto.name)

        // 🔹 lista de productos
        val productosArray = JSONArray()
        cotizacion.productos.forEach { p ->
            productosArray.put(p.name)
        }
        put("productos", productosArray)

        // 🔹 descuento en dólares por m²
        put("descuentoDolaresPorM2", cotizacion.descuentoDolaresPorM2)
        put("tipoMontaje", cotizacion.tipoMontaje)
        // 🔹 ventanas
        val ventanasArray = JSONArray()
        cotizacion.ventanas.forEach { v ->
            val vObj = JSONObject().apply {
                put("descripcion", v.descripcion)
                put("alto", v.alto)
                put("ancho", v.ancho)
                put("precioM2", v.precioM2)
            }
            ventanasArray.put(vObj)
        }
        put("ventanas", ventanasArray)
    }

    // Añadimos la cotización al arreglo final y guardamos
    nuevoArray.put(cotizacionJson)

    prefs.edit()
        .putString(KEY_COTIZACIONES, nuevoArray.toString())
        .apply()
}

/**
 * Recupera todas las cotizaciones guardadas.
 */
fun obtenerCotizacionesLocal(context: Context): List<Cotizacion> {
    val prefs = getPrefs(context)
    val listaJson = prefs.getString(KEY_COTIZACIONES, "[]") ?: "[]"
    val array = JSONArray(listaJson)
    val resultado = mutableListOf<Cotizacion>()

    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)

        val id = obj.optLong("id", System.currentTimeMillis())
        val folio = obj.optString("folio", "")
        val clienteNombre = obj.optString("clienteNombre", "")
        val clienteTelefono = obj.optString("clienteTelefono", "")
        val ubicacion = obj.optString("ubicacion", "")
        val especialista = obj.optString("especialista", "")
        val fecha = obj.optString("fecha", "")

        val productoName = obj.optString("producto", TipoProducto.HS875.name)
        val producto = runCatching { TipoProducto.valueOf(productoName) }
            .getOrElse { TipoProducto.HS875 }

        val productosArrayJson = obj.optJSONArray("productos")
        val productos: List<TipoProducto> =
            if (productosArrayJson != null && productosArrayJson.length() > 0) {
                (0 until productosArrayJson.length()).mapNotNull { idx ->
                    val name = productosArrayJson.optString(idx, null)
                    name?.let {
                        runCatching { TipoProducto.valueOf(it) }.getOrNull()
                    }
                }.ifEmpty { listOf(producto) }
            } else {
                listOf(producto)
            }

        // 🔹 leemos el descuento guardado
        val descuentoDolaresPorM2 = obj.optDouble("descuentoDolaresPorM2", 0.0)

        val tipoMontaje = obj.optString("tipoMontaje", "Flush Mount")

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
                )
            )
        }

        val cotizacion = Cotizacion(
            id = id,
            folio = folio,
            clienteNombre = clienteNombre,
            clienteTelefono = clienteTelefono,
            ubicacion = ubicacion,
            especialista = especialista,
            fecha = fecha,
            producto = producto,
            productos = productos,
            descuentoDolaresPorM2 = descuentoDolaresPorM2,
            tipoMontaje = tipoMontaje,
            ventanas = ventanas
        )
        resultado.add(cotizacion)
    }

    // opcional: orden inverso (más recientes primero)
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
