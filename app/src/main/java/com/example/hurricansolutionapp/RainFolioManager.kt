package com.example.hurricansolutionapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor de folios para Rain Protection
 * Formato: SU001-RP, SU002-RP, etc.
 */
object RainFolioManager {

    private const val PREFS_FOLIO_RAIN = "folio_rain_prefs"

    /**
     * Genera el siguiente folio para Rain Protection
     * Formato: XX001-RP (donde XX son las iniciales del especialista)
     */
    fun nextFolioForEspecialista(context: Context, especialista: String): String {
        val prefix = getPrefix(especialista)
        val prefs = context.getSharedPreferences(PREFS_FOLIO_RAIN, Context.MODE_PRIVATE)

        val key = "counter_rain_$prefix"
        val currentCounter = prefs.getInt(key, 0)
        val nextCounter = currentCounter + 1

        prefs.edit().putInt(key, nextCounter).apply()

        // Formato: SU001-RP
        return "$prefix${nextCounter.toString().padStart(3, '0')}-RP"
    }

    private fun getPrefix(nombre: String): String {
        val palabras = nombre.trim().split("\\s+".toRegex())
        return when {
            palabras.size >= 2 -> {
                "${palabras[0].firstOrNull()?.uppercaseChar() ?: 'X'}${palabras[1].firstOrNull()?.uppercaseChar() ?: 'X'}"
            }
            palabras.isNotEmpty() -> {
                val primera = palabras[0]
                if (primera.length >= 2) {
                    "${primera[0].uppercaseChar()}${primera[1].uppercaseChar()}"
                } else {
                    "${primera.firstOrNull()?.uppercaseChar() ?: 'X'}X"
                }
            }
            else -> "XX"
        }
    }

    /**
     * Establece el contador para un prefijo específico
     */
    fun setCounterForPrefix(context: Context, prefix: String, value: Int) {
        val prefs = context.getSharedPreferences(PREFS_FOLIO_RAIN, Context.MODE_PRIVATE)
        prefs.edit().putInt("counter_rain_$prefix", value).apply()
    }

    /**
     * Obtiene el contador actual para un prefijo
     */
    fun getCounterForPrefix(context: Context, prefix: String): Int {
        val prefs = context.getSharedPreferences(PREFS_FOLIO_RAIN, Context.MODE_PRIVATE)
        return prefs.getInt("counter_rain_$prefix", 0)
    }

    /**
     * Sincroniza los contadores de folios Rain desde Supabase
     */
    suspend fun syncFromSupabase(context: Context, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("RainFolioManager", "Sincronizando folios Rain...")

                val cotizaciones = RainRepository.getCotizacionesByUser(userId)

                val maxByPrefix = mutableMapOf<String, Int>()

                for (cot in cotizaciones) {
                    val parsed = parseFolio(cot.folio)
                    if (parsed != null) {
                        val (prefix, number) = parsed
                        val currentMax = maxByPrefix[prefix] ?: 0
                        if (number > currentMax) {
                            maxByPrefix[prefix] = number
                        }
                    }
                }

                for ((prefix, maxNumber) in maxByPrefix) {
                    val localCounter = getCounterForPrefix(context, prefix)
                    if (maxNumber > localCounter) {
                        setCounterForPrefix(context, prefix, maxNumber)
                        android.util.Log.d("RainFolioManager", "Actualizado $prefix: $localCounter → $maxNumber")
                    }
                }

                android.util.Log.d("RainFolioManager", "[OK] Sincronización completada")

            } catch (e: Exception) {
                android.util.Log.e("RainFolioManager", "Error sincronizando folios: ${e.message}", e)
            }
        }
    }

    /**
     * Parsea un folio Rain y extrae prefijo y número
     * Formato: SU001-RP
     */
    fun parseFolio(folio: String): Pair<String, Int>? {
        if (folio.isBlank()) return null

        val regex = Regex("^([A-Z]{2})(\\d+)-RP$")
        regex.find(folio)?.let { match ->
            val prefix = match.groupValues[1]
            val number = match.groupValues[2].toIntOrNull() ?: return null
            return Pair(prefix, number)
        }

        return null
    }

    /**
     * Verifica si un folio es de Rain Protection
     */
    fun isRainFolio(folio: String): Boolean {
        return folio.endsWith("-RP")
    }
}