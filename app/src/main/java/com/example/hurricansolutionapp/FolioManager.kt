package com.example.hurricansolutionapp

import android.content.Context

object FolioManager {

    private const val PREFS_NAME = "folios_prefs"
    private const val KEY_PREFIX = "folio_counter_"

    /**
     * Genera el siguiente número de folio para un prefijo dado.
     * @param context Contexto de la aplicación
     * @param prefijo Prefijo del folio (ej: "MC" para Marco Canche)
     * @return El siguiente número consecutivo
     */
    fun nextFolioForPrefix(context: Context, prefijo: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_PREFIX + prefijo

        val last = prefs.getInt(key, 0)
        val next = last + 1

        prefs.edit().putInt(key, next).apply()
        return next
    }

    /**
     * Genera el siguiente folio para un especialista.
     * El folio se forma con las iniciales del nombre y primer apellido + número consecutivo.
     * Ejemplo: "Marco Alejandro Canche Kantun" -> "MC-0001"
     *
     * @param context Contexto de la aplicación
     * @param nombreCompleto Nombre completo del especialista
     * @return Folio generado (ej: "MC-0001")
     */
    fun nextFolioForEspecialista(
        context: Context,
        nombreCompleto: String
    ): String {
        // Tomamos la primera letra del nombre y la primera letra del primer apellido
        // "Marco Alejandro Canche Kantun" -> ["Marco", "Alejandro", "Canche", "Kantun"]
        // Tomamos: M (de Marco) + C (de Canche, que es el primer apellido)
        val palabras = nombreCompleto
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }

        val prefijo = when {
            palabras.size >= 3 -> {
                // Nombre + Primer apellido (asumiendo que el tercer elemento es apellido)
                // Para "Marco Alejandro Canche Kantun": M + C
                "${palabras[0].first().uppercase()}${palabras[2].first().uppercase()}"
            }

            palabras.size == 2 -> {
                // Solo nombre y apellido
                "${palabras[0].first().uppercase()}${palabras[1].first().uppercase()}"
            }

            palabras.size == 1 -> {
                // Solo un nombre
                palabras[0].take(2).uppercase()
            }

            else -> "XX"
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_PREFIX + prefijo

        val last = prefs.getInt(key, 0)
        val next = last + 1

        prefs.edit().putInt(key, next).apply()

        val numero = String.format("%04d", next)
        return "$prefijo-$numero"
    }

    /**
     * Obtiene el último número usado para un prefijo sin incrementar.
     */
    fun getLastNumberForPrefix(context: Context, prefijo: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_PREFIX + prefijo
        return prefs.getInt(key, 0)
    }

    /**
     * Verifica si un folio ya existe.
     */
    fun folioExists(context: Context, folio: String): Boolean {
        val cotizaciones = obtenerCotizacionesLocal(context)
        return cotizaciones.any { it.folio == folio }
    }

    /**
     * Genera un folio único, verificando que no exista.
     * Si ya existe, incrementa hasta encontrar uno disponible.
     */
    fun generateUniqueFolio(context: Context, nombreCompleto: String): String {
        var folio = nextFolioForEspecialista(context, nombreCompleto)

        // En caso muy raro de colisión, incrementar
        var attempts = 0
        while (folioExists(context, folio) && attempts < 100) {
            folio = nextFolioForEspecialista(context, nombreCompleto)
            attempts++
        }

        return folio
    }

    /**
     * Resetea el contador para un prefijo específico (útil para pruebas).
     */
    fun resetCounter(context: Context, prefijo: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_PREFIX + prefijo
        prefs.edit().remove(key).apply()
    }

    /**
     * Obtiene las iniciales de un especialista.
     */
    fun getInitials(nombreCompleto: String): String {
        val palabras = nombreCompleto
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }

        return when {
            palabras.size >= 3 -> "${palabras[0].first().uppercase()}${
                palabras[2].first().uppercase()
            }"

            palabras.size == 2 -> "${palabras[0].first().uppercase()}${
                palabras[1].first().uppercase()
            }"

            palabras.size == 1 -> palabras[0].take(2).uppercase()
            else -> "XX"
        }
    }
}