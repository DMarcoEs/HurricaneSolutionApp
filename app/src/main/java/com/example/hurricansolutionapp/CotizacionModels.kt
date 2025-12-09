package com.example.hurricansolutionapp

// 🔹 Tipo de producto (opción de cotización)
enum class TipoProducto(val etiqueta: String) {
    HS875("HS-875 (Polipropileno)"),
    HS1250("HS-1250 (Poliester y Aramida)"),
    HS1500("HS-1500 (Nylon Balístico)"),
    PERSONALIZADO("Otro precio")
}

// 🔹 Precios por m² por defecto (AJÚSTALOS A LOS REALES)
const val HS875_DEFAULT_PRICE = 150.0
const val HS1250_DEFAULT_PRICE = 180.0
const val HS1500_DEFAULT_PRICE = 210.0

// 🔹 Medida individual (ventana / área a proteger)
data class Ventana(
    val descripcion: String,
    val alto: Double,
    val ancho: Double,
    val precioM2: Double,
    val adecuacion: String = "Por revisar"
) {
    // Área en m²
    val areaM2: Double get() = alto * ancho

    // Subtotal usando el precioM2 que se guardó (producto principal)
    val subtotal: Double get() = areaM2 * precioM2

    // Subtotal de ESTA ventana usando el precio por m² según el producto
    fun subtotalPorProducto(producto: TipoProducto): Double {
        val precio = when (producto) {
            TipoProducto.HS875 -> HS875_DEFAULT_PRICE
            TipoProducto.HS1250 -> HS1250_DEFAULT_PRICE
            TipoProducto.HS1500 -> HS1500_DEFAULT_PRICE
            TipoProducto.PERSONALIZADO -> precioM2  // usa el precio personalizado que capturaste
        }
        return areaM2 * precio
    }
}

// 🔹 Cotización completa
data class Cotizacion(
    val id: Long = 0L,
    val folio: String = "",
    val clienteNombre: String,
    val clienteTelefono: String,
    val ubicacion: String,
    val especialista: String,
    val fecha: String,
    val producto: TipoProducto,
    val descuentoDolaresPorM2: Double = 0.0,
    val productos: List<TipoProducto> = listOf(producto),
    val ventanas: List<Ventana>,

    // NUEVO
    val tipoMontaje: String = "Flush Mount"
) {
    // Subtotal actual (con el precioM2 guardado en cada ventana)
    val subtotal: Double get() = ventanas.sumOf { it.subtotal }

    val iva: Double get() = 0.0        // por ahora sin IVA
    val total: Double get() = subtotal

    // Total por producto usando los precios por m² de cada tipo
    fun totalPorProducto(producto: TipoProducto): Double =
        ventanas.sumOf { it.subtotalPorProducto(producto) }
}
