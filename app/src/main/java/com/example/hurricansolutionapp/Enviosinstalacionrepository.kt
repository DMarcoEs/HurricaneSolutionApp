package com.example.hurricansolutionapp

import android.content.Context

/**
 * Repositorio para manejar los envíos a instalación
 * Usa SharedPreferences local para tracking de envíos
 */
object EnviosInstalacionRepository {

    private const val TAG = "EnviosInstalacionRepo"
    private const val PREFS_NAME = "envios_instalacion_prefs"
    private const val KEY_ENVIADOS = "folios_enviados"

    /**
     * Obtiene cotizaciones con 1 solo sistema que NO se han enviado a instalación
     * @param context Contexto de Android
     * @param userId Si es null, trae todas (para admin). Si tiene valor, filtra por especialista.
     */
    fun getCotizacionesPendientesEnvio(
        context: Context,
        userId: String? = null
    ): Result<List<Cotizacion>> {
        return try {
            // Obtener todas las cotizaciones locales
            val todasCotizaciones = obtenerCotizacionesLocal(context)

            // Obtener folios ya enviados
            val foliosEnviados = getFoliosEnviados(context)

            // Filtrar:
            // 1. Solo las que tienen 1 sistema
            // 2. No han sido enviadas
            // 3. Si userId != null, filtrar por especialista (opcional, depende de tu lógica)
            val pendientes = todasCotizaciones.filter { cot ->
                cot.productos.size == 1 && !foliosEnviados.contains(cot.folio)
            }

            android.util.Log.d(TAG, "✅ ${pendientes.size} cotizaciones pendientes de envío")
            Result.success(pendientes)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getCotizacionesPendientesEnvio: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Marca una cotización como enviada a instalación (localmente)
     */
    fun marcarComoEnviada(context: Context, folio: String): Result<Unit> {
        return try {
            val foliosEnviados = getFoliosEnviados(context).toMutableSet()
            foliosEnviados.add(folio)
            saveFoliosEnviados(context, foliosEnviados)

            android.util.Log.d(TAG, "✅ Cotización $folio marcada como enviada")
            Result.success(Unit)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error marcarComoEnviada: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verifica si una cotización ya fue enviada
     */
    fun fueEnviada(context: Context, folio: String): Boolean {
        return getFoliosEnviados(context).contains(folio)
    }

    /**
     * Obtiene la lista de folios enviados
     */
    private fun getFoliosEnviados(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_ENVIADOS, emptySet()) ?: emptySet()
    }

    /**
     * Guarda la lista de folios enviados
     */
    private fun saveFoliosEnviados(context: Context, folios: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_ENVIADOS, folios).apply()
    }

    /**
     * Limpia un folio de la lista de enviados (para reenviar)
     */
    fun desmarcarEnviado(context: Context, folio: String): Result<Unit> {
        return try {
            val foliosEnviados = getFoliosEnviados(context).toMutableSet()
            foliosEnviados.remove(folio)
            saveFoliosEnviados(context, foliosEnviados)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}