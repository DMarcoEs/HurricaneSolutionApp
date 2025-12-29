package com.example.hurricansolutionapp

// ═══════════════════════════════════════════════════════════════════════════════
// TIPO DE PRODUCTO (Sistema de protección)
// ═══════════════════════════════════════════════════════════════════════════════
enum class TipoProducto(val etiqueta: String, val etiquetaCorta: String) {
    HS875("HS-875 (Polipropileno)", "HS-875"),
    HS1250("HS-1250 (Poliester y Aramida)", "HS-1250"),
    HS1500("HS-1500 (Nylon Balístico)", "HS-1500"),
    PERSONALIZADO("Otro precio", "Personalizado")
}

// ═══════════════════════════════════════════════════════════════════════════════
// PRECIOS POR M² - VENTA (lo que se cobra al cliente)
// ═══════════════════════════════════════════════════════════════════════════════
const val HS875_SELL_PRICE = 150.0
const val HS1250_SELL_PRICE = 180.0
const val HS1500_SELL_PRICE = 210.0

// ═══════════════════════════════════════════════════════════════════════════════
// PRECIOS POR M² - BASE/COSTO (mínimo, no se puede bajar de aquí con descuentos)
// ═══════════════════════════════════════════════════════════════════════════════
const val HS875_BASE_PRICE = 130.0
const val HS1250_BASE_PRICE = 150.0
const val HS1500_BASE_PRICE = 170.0

// ═══════════════════════════════════════════════════════════════════════════════
// PRECIOS LEGACY (para compatibilidad con código existente)
// ═══════════════════════════════════════════════════════════════════════════════
const val HS875_DEFAULT_PRICE = HS875_SELL_PRICE
const val HS1250_DEFAULT_PRICE = HS1250_SELL_PRICE
const val HS1500_DEFAULT_PRICE = HS1500_SELL_PRICE

// ═══════════════════════════════════════════════════════════════════════════════
// FUNCIONES AUXILIARES DE PRECIOS
// ═══════════════════════════════════════════════════════════════════════════════
fun TipoProducto.getPrecioVenta(): Double = when (this) {
    TipoProducto.HS875 -> HS875_SELL_PRICE
    TipoProducto.HS1250 -> HS1250_SELL_PRICE
    TipoProducto.HS1500 -> HS1500_SELL_PRICE
    TipoProducto.PERSONALIZADO -> HS875_SELL_PRICE
}

fun TipoProducto.getPrecioBase(): Double = when (this) {
    TipoProducto.HS875 -> HS875_BASE_PRICE
    TipoProducto.HS1250 -> HS1250_BASE_PRICE
    TipoProducto.HS1500 -> HS1500_BASE_PRICE
    TipoProducto.PERSONALIZADO -> HS875_BASE_PRICE
}

fun TipoProducto.getMaxDescuento(): Double = getPrecioVenta() - getPrecioBase()

// ═══════════════════════════════════════════════════════════════════════════════
// ESTADO DEL FORMULARIO PARA UNA MEDIDA
// ═══════════════════════════════════════════════════════════════════════════════
data class VentanaFormState(
    var descripcion: String = "",
    var alto: String = "",
    var ancho: String = "",
    var adecuacion: String = "No",
    var tipoMontaje: String = "Flush Mount",
    var adecuacionDetalle: String = ""
)

// ═══════════════════════════════════════════════════════════════════════════════
// MEDIDA INDIVIDUAL (Ventana / Área a proteger)
// ═══════════════════════════════════════════════════════════════════════════════
data class Ventana(
    val descripcion: String,
    val alto: Double,
    val ancho: Double,
    val precioM2: Double,
    val adecuacion: String = "No",
    val tipoMontaje: String = "Flush Mount"
) {
    val areaM2: Double get() = alto * ancho
    val subtotal: Double get() = areaM2 * precioM2

    fun subtotalPorProducto(producto: TipoProducto): Double {
        val precio = producto.getPrecioVenta()
        return areaM2 * precio
    }

    fun subtotalConDescuento(producto: TipoProducto, descuentoPorM2: Double): Double {
        val precioVenta = producto.getPrecioVenta()
        val precioBase = producto.getPrecioBase()
        val descuentoAplicado = descuentoPorM2.coerceAtMost(precioVenta - precioBase)
        val precioFinal = (precioVenta - descuentoAplicado).coerceAtLeast(precioBase)
        return areaM2 * precioFinal
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DRAFT DE COTIZACIÓN (lo que se va llenando paso a paso)
// ═══════════════════════════════════════════════════════════════════════════════
data class CotizacionDraft(
    var nombre: String = "",
    var telefono: String = "",
    var ciudad: String = "",
    var colonia: String = "",
    var direccionDetalle: String = "",
    var fecha: String = "",
    var ventanasForm: MutableList<VentanaFormState> = mutableListOf(VentanaFormState()),
    var tipoMontaje: String = "Flush Mount",
    var productosSeleccionados: MutableList<TipoProducto> = mutableListOf(TipoProducto.HS875),
    var aplicaDescuento: Boolean = false,
    var descuentoTexto: String = "0",
    // Descuentos por sistema (en dólares por m²)
    var descuentoHS875: Double = 0.0,
    var descuentoHS1250: Double = 0.0,
    var descuentoHS1500: Double = 0.0
) {
    fun clear() {
        nombre = ""
        telefono = ""
        ciudad = ""
        colonia = ""
        direccionDetalle = ""
        fecha = ""
        ventanasForm = mutableListOf(VentanaFormState())
        tipoMontaje = "Flush Mount"
        productosSeleccionados = mutableListOf(TipoProducto.HS875)
        aplicaDescuento = false
        descuentoTexto = "0"
        descuentoHS875 = 0.0
        descuentoHS1250 = 0.0
        descuentoHS1500 = 0.0
    }

    fun getDescuentoPorProducto(producto: TipoProducto): Double = when (producto) {
        TipoProducto.HS875 -> descuentoHS875
        TipoProducto.HS1250 -> descuentoHS1250
        TipoProducto.HS1500 -> descuentoHS1500
        TipoProducto.PERSONALIZADO -> 0.0
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COTIZACIÓN COMPLETA
// ═══════════════════════════════════════════════════════════════════════════════
data class Cotizacion(
    val id: Long = 0L,
    val folio: String = "",
    val clienteNombre: String,
    val clienteTelefono: String,
    val ubicacion: String,
    val ciudad: String = "",
    val especialista: String,
    val fecha: String,
    val producto: TipoProducto,
    val productos: List<TipoProducto> = listOf(producto),
    val tipoMontaje: String = "Flush Mount",
    val ventanas: List<Ventana>,
    // Descuentos por sistema (en dólares por m²)
    val descuentoHS875: Double = 0.0,
    val descuentoHS1250: Double = 0.0,
    val descuentoHS1500: Double = 0.0,
    // Legacy
    val descuentoDolaresPorM2: Double = 0.0
) {
    val areaTotal: Double get() = ventanas.sumOf { it.areaM2 }
    val subtotal: Double get() = ventanas.sumOf { it.subtotal }
    val iva: Double get() = 0.0
    val total: Double get() = subtotal

    fun totalPorProducto(producto: TipoProducto): Double =
        ventanas.sumOf { it.subtotalPorProducto(producto) }

    fun getDescuentoPorProducto(producto: TipoProducto): Double = when (producto) {
        TipoProducto.HS875 -> descuentoHS875
        TipoProducto.HS1250 -> descuentoHS1250
        TipoProducto.HS1500 -> descuentoHS1500
        TipoProducto.PERSONALIZADO -> 0.0
    }

    fun totalConDescuento(producto: TipoProducto): Double {
        val descuento = getDescuentoPorProducto(producto)
        return ventanas.sumOf { it.subtotalConDescuento(producto, descuento) }
    }

    fun getPorcentajeDescuento(producto: TipoProducto): Double {
        val descuento = getDescuentoPorProducto(producto)
        val precioVenta = producto.getPrecioVenta()
        return if (precioVenta > 0) (descuento / precioVenta) * 100 else 0.0
    }

    // Productos ordenados de menor a mayor precio
    fun productosOrdenados(): List<TipoProducto> = productos.sortedBy { it.getPrecioVenta() }
}