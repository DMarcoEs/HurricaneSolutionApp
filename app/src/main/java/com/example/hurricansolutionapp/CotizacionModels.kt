package com.example.hurricansolutionapp

// 🔹 Tipo de producto (opción de cotización)
enum class TipoProducto(val etiqueta: String) {
    HS875("HS-875 (Polipropileno)"),
    HS1250("HS-1250 (Poliester y Aramida)"),
    HS1500("HS-1500 (Nylon Balístico)"),
    PERSONALIZADO("Otro precio")
}

// 🔹 Precios por m² por defecto (AJUSTA ESTOS VALORES A LOS REALES)
const val HS875_DEFAULT_PRICE = 150.0
const val HS1250_DEFAULT_PRICE = 180.0
const val HS1500_DEFAULT_PRICE = 210.0

data class Ventana(
    val descripcion: String,
    val alto: Double,
    val ancho: Double,
    val precioM2: Double
) {
    val areaM2: Double
        get() = alto * ancho

    val subtotal: Double
        get() = areaM2 * precioM2
}

data class Cotizacion(
    // Identificador único
    val id: Long = System.currentTimeMillis(),

    // Datos del cliente / proyecto
    val clienteNombre: String,
    val clienteTelefono: String,
    val ubicacion: String,
    val especialista: String,
    val fecha: String,

    // Tipo de producto seleccionado
    val producto: TipoProducto = TipoProducto.HS875,

    // Ventanas / aperturas
    val ventanas: List<Ventana>,

    // Impuestos
    val ivaPorcentaje: Double = 0.16,

    // Estado de sincronización con CRM
    val sincronizada: Boolean = false
) {
    val subtotal: Double
        get() = ventanas.sumOf { it.subtotal }

    val iva: Double
        get() = subtotal * ivaPorcentaje

    val total: Double
        get() = subtotal + iva
}