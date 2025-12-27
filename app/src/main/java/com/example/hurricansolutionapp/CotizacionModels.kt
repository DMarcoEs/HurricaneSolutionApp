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

// 🔹 Estado del formulario para UNA medida (lo que escribes antes de convertirlo a Ventana)
data class VentanaFormState(
    var descripcion: String = "",
    var alto: String = "",
    var ancho: String = "",
    var adecuacion: String = "No", // "No" / "Sí" (o el texto que uses)
)

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
            TipoProducto.PERSONALIZADO -> precioM2 // usa el precio personalizado que capturaste
        }
        return areaM2 * precio
    }
}

// 🔹 Draft (lo que vas llenando Cliente -> Medidas)
data class CotizacionDraft(
    var nombre: String = "",
    var telefono: String = "",
    var ciudad: String = "",
    var colonia: String = "",
    var direccionDetalle: String = "",
    var fecha: String = "",

    // medidas (form)
    var ventanasForm: MutableList<VentanaFormState> = mutableListOf(),

    // config medidas
    var tipoMontaje: String = "Flush Mount",
    var productosSeleccionados: MutableList<TipoProducto> = mutableListOf(TipoProducto.HS875),

    // descuento
    var aplicaDescuento: Boolean = false,
    var descuentoTexto: String = "0"
) {
    fun clear() {
        nombre = ""
        telefono = ""
        ciudad = ""
        colonia = ""
        direccionDetalle = ""
        fecha = ""

        ventanasForm = mutableListOf()

        tipoMontaje = "Flush Mount"
        productosSeleccionados = mutableListOf(TipoProducto.HS875)

        aplicaDescuento = false
        descuentoTexto = "0"
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
    val productos: List<TipoProducto> = listOf(producto),
    val tipoMontaje: String = "Flush Mount",
    val descuentoDolaresPorM2: Double = 0.0,
    val ventanas: List<Ventana>
) {
    // Total usando SOLO el producto principal
    val subtotal: Double get() = ventanas.sumOf { it.subtotal }

    val iva: Double get() = 0.0 // por ahora sin IVA
    val total: Double get() = subtotal

    // Total por producto usando los precios por m² de cada tipo
    fun totalPorProducto(producto: TipoProducto): Double =
        ventanas.sumOf { it.subtotalPorProducto(producto) }
}
