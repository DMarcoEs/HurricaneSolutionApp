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
