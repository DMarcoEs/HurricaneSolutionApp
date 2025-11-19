package com.example.hurricansolutionapp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

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
}
