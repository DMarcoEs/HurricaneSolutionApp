package com.example.hurricansolutionapp

import java.util.Locale

// Fecha simple en español
fun getSpanishDate(): String {
    val locale = Locale("es", "MX")
    val sdf = java.text.SimpleDateFormat("EEEE, d 'de' MMMM", locale)
    val text = sdf.format(java.util.Date())
    return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

fun filtrarNumeroDecimal(input: String): String =
    input.filter { it.isDigit() || it == '.' }

fun filtrarSoloDigitos(input: String): String =
    input.filter { it.isDigit() }

/**
 * Formatea un nombre completo a "Primer Nombre + Primer Apellido"
 * Ejemplos:
 * - "Fernando Loria Fernandez" -> "Fernando Loria"
 * - "Marco Alejandro Canche Kantun" -> "Marco Canche"
 * - "Juan" -> "Juan"
 * - "Ana María López García" -> "Ana López"
 */
fun formatearNombreCorto(nombreCompleto: String): String {
    val partes = nombreCompleto.trim().split(" ").filter { it.isNotBlank() }

    return when {
        partes.isEmpty() -> ""
        partes.size == 1 -> partes[0] // Solo un nombre
        partes.size == 2 -> partes.joinToString(" ") // Nombre + Apellido
        partes.size == 3 -> "${partes[0]} ${partes[2]}" // Nombre + Apellido (saltando segundo nombre)
        partes.size >= 4 -> "${partes[0]} ${partes[2]}" // Nombre + Primer Apellido (asumiendo: Nombre1 Nombre2 Apellido1 Apellido2)
        else -> partes[0]
    }
}