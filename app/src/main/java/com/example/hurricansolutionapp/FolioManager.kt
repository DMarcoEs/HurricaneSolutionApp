package com.example.hurricansolutionapp

import android.content.Context

object FolioManager {

    private const val PREFS_NAME = "folios_prefs"
    private const val KEY_PREFIX = "folio_counter_"

    fun nextFolioForPrefix(context: Context, prefijo: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_PREFIX + prefijo

        val last = prefs.getInt(key, 0)
        val next = last + 1

        prefs.edit().putInt(key, next).apply()
        return next
    }

    fun nextFolioForEspecialista(
        context: Context,
        nombreCompleto: String
    ): String {
        // Tomamos las 2 primeras palabras (nombre y primer apellido)
        val prefijo = nombreCompleto
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)                        // Fernando + Loria
            .joinToString("") { it.first().uppercase() } // "FL"

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_PREFIX + prefijo

        val last = prefs.getInt(key, 0)     // último número usado
        val next = last + 1                 // siguiente

        prefs.edit().putInt(key, next).apply()

        val numero = String.format("%04d", next) // 0001, 0002, ...
        return "$prefijo-$numero"                // FL-0001
    }
}