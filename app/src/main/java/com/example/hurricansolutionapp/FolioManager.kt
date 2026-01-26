package com.example.hurricansolutionapp

import android.content.Context
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREFS_FOLIO = "folio_prefs"
private const val KEY_SYNCED = "folios_synced"


object FolioManager {


    fun nextFolioForEspecialista(context: Context, especialista: String): String {
        val prefix = getPrefix(especialista)
        val prefs = context.getSharedPreferences(PREFS_FOLIO, Context.MODE_PRIVATE)

        val key = "counter_$prefix"
        val currentCounter = prefs.getInt(key, 0)
        val nextCounter = currentCounter + 1

        prefs.edit().putInt(key, nextCounter).apply()

        // Formato: MC001 (3 dígitos, sin guion)
        return "$prefix${nextCounter.toString().padStart(3, '0')}"
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
     * Útil para sincronización desde Supabase
     */
    fun setCounterForPrefix(context: Context, prefix: String, value: Int) {
        val prefs = context.getSharedPreferences(PREFS_FOLIO, Context.MODE_PRIVATE)
        prefs.edit().putInt("counter_$prefix", value).apply()
        android.util.Log.d("FolioManager", "Counter establecido: $prefix = $value")
    }

    /**
     * Obtiene el contador actual para un prefijo
     */
    fun getCounterForPrefix(context: Context, prefix: String): Int {
        val prefs = context.getSharedPreferences(PREFS_FOLIO, Context.MODE_PRIVATE)
        return prefs.getInt("counter_$prefix", 0)
    }

    /**
     * Sincroniza los contadores de folios desde Supabase
     * Debe llamarse al login para asegurar que no se dupliquen folios
     */
    suspend fun syncFromSupabase(context: Context, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("FolioManager", "Sincronizando folios desde Supabase...")

                val client = SupabaseClientProvider.client

                // Obtener cotizaciones del usuario
                val cotizaciones = client.from("cotizaciones")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<CotizacionRemota>()

                // Agrupar por prefijo y encontrar el máximo
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

                // Actualizar contadores locales
                for ((prefix, maxNumber) in maxByPrefix) {
                    val localCounter = getCounterForPrefix(context, prefix)
                    if (maxNumber > localCounter) {
                        setCounterForPrefix(context, prefix, maxNumber)
                        android.util.Log.d("FolioManager", "Actualizado $prefix: $localCounter $maxNumber")
                    }
                }

                // Marcar como sincronizado
                val prefs = context.getSharedPreferences(PREFS_FOLIO, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_SYNCED, true).apply()

                android.util.Log.d("FolioManager", "[OK] Sincronización completada")

            } catch (e: Exception) {
                android.util.Log.e("FolioManager", "Error sincronizando folios: ${e.message}", e)
            }
        }
    }

    /**
     * Parsea un folio y extrae prefijo y número
     * Soporta ambos formatos: "MC001" y "MC-0001"
     */
    fun parseFolio(folio: String): Pair<String, Int>? {
        if (folio.isBlank()) return null

        // Formato nuevo: MC001
        val regexNew = Regex("^([A-Z]{2})(\\d+)$")
        regexNew.find(folio)?.let { match ->
            val prefix = match.groupValues[1]
            val number = match.groupValues[2].toIntOrNull() ?: return null
            return Pair(prefix, number)
        }

        // Formato viejo: MC-0001
        val regexOld = Regex("^([A-Z]{2})-(\\d+)$")
        regexOld.find(folio)?.let { match ->
            val prefix = match.groupValues[1]
            val number = match.groupValues[2].toIntOrNull() ?: return null
            return Pair(prefix, number)
        }

        return null
    }

    /**
     * Verifica si ya se sincronizó en esta sesión
     */
    fun isSynced(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_FOLIO, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SYNCED, false)
    }

    /**
     * Resetea el flag de sincronización (llamar al logout)
     */
    fun resetSyncFlag(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_FOLIO, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SYNCED, false).apply()
    }
}