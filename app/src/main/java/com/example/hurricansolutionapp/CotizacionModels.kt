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
// PRECIOS POR M² - VALORES POR DEFECTO (fallback si no hay conexión)
// Estos valores ya NO se usan directamente, solo como fallback
// ═══════════════════════════════════════════════════════════════════════════════
const val HS875_SELL_PRICE = 150.0
const val HS1250_SELL_PRICE = 180.0
const val HS1500_SELL_PRICE = 210.0

const val HS875_BASE_PRICE = 130.0
const val HS1250_BASE_PRICE = 150.0
const val HS1500_BASE_PRICE = 170.0

// Legacy (para compatibilidad)
const val HS875_DEFAULT_PRICE = HS875_SELL_PRICE
const val HS1250_DEFAULT_PRICE = HS1250_SELL_PRICE
const val HS1500_DEFAULT_PRICE = HS1500_SELL_PRICE

// ═══════════════════════════════════════════════════════════════════════════════
// FUNCIONES DE PRECIOS - AHORA USAN PRICEMANAGER (DINÁMICOS)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Obtiene el precio de venta DINÁMICO desde PriceManager
 */
fun TipoProducto.getPrecioVenta(): Double = PriceManager.getPrecioVenta(this)

/**
 * Obtiene el precio base DINÁMICO desde PriceManager
 */
fun TipoProducto.getPrecioBase(): Double = PriceManager.getPrecioBase(this)

/**
 * Obtiene el descuento máximo permitido (diferencia entre venta y base)
 */
fun TipoProducto.getMaxDescuento(): Double = PriceManager.getMaxDescuento(this)

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
        val precio = producto.getPrecioVenta()  // Ahora usa precio dinámico
        return areaM2 * precio
    }

    fun subtotalConDescuento(producto: TipoProducto, descuentoPorM2: Double): Double {
        val precioVenta = producto.getPrecioVenta()  // Ahora usa precio dinámico
        val precioBase = producto.getPrecioBase()    // Ahora usa precio dinámico
        val descuentoAplicado = descuentoPorM2.coerceAtMost(precioVenta - precioBase)
        val precioFinal = (precioVenta - descuentoAplicado).coerceAtLeast(precioBase)
        return areaM2 * precioFinal
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DRAFT DE COTIZACIÓN (lo que se va llenando paso a paso)
// ═══════════════════════════════════════════════════════════════════════════════
data class CotizacionDraft(
    var id: Long = 0L,
    var folio: String = "",
    var nombre: String = "",
    var telefono: String = "",
    var ciudad: String = "",
    var colonia: String = "",
    var direccionDetalle: String = "",
    var fecha: String = "",

    // ✅ NUEVO: ID del lead si viene desde CRM
    var leadId: String? = null,
    var esClienteActual: Boolean = false,  // True si viene de CRM

    var ventanasForm: MutableList<VentanaFormState> = mutableListOf(VentanaFormState()),
    var tipoMontaje: String = "Flush Mount",
    var productosSeleccionados: MutableList<TipoProducto> = mutableListOf(TipoProducto.HS875),
    var aplicaDescuento: Boolean = false,
    var descuentoTexto: String = "0",
    var descuentoHS875: Double = 0.0,
    var descuentoHS1250: Double = 0.0,
    var descuentoHS1500: Double = 0.0
) {
    fun clear() {
        id = 0L
        folio = ""
        nombre = ""
        telefono = ""
        ciudad = ""
        colonia = ""
        direccionDetalle = ""
        fecha = ""
        leadId = null  // ✅ NUEVO
        esClienteActual = false  // ✅ NUEVO
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

    fun cargarDesdeCotizacion(cotizacion: Cotizacion) {
        id = cotizacion.id
        folio = cotizacion.folio
        nombre = cotizacion.clienteNombre
        telefono = cotizacion.clienteTelefono
        ciudad = cotizacion.ciudad

        val partes = cotizacion.ubicacion.split(",").map { it.trim() }
        colonia = partes.getOrNull(1) ?: ""
        direccionDetalle = partes.getOrNull(2) ?: ""

        fecha = cotizacion.fecha
        tipoMontaje = cotizacion.tipoMontaje
        productosSeleccionados = cotizacion.productos.toMutableList()

        descuentoHS875 = cotizacion.descuentoHS875
        descuentoHS1250 = cotizacion.descuentoHS1250
        descuentoHS1500 = cotizacion.descuentoHS1500
        aplicaDescuento = descuentoHS875 > 0 || descuentoHS1250 > 0 || descuentoHS1500 > 0

        ventanasForm = cotizacion.ventanas.map { v ->
            VentanaFormState(
                descripcion = v.descripcion,
                alto = String.format("%.2f", v.alto),
                ancho = String.format("%.2f", v.ancho),
                adecuacion = if (v.adecuacion == "No" || v.adecuacion.isBlank()) "No" else "Sí",
                tipoMontaje = v.tipoMontaje,
                adecuacionDetalle = if (v.adecuacion != "No" && v.adecuacion.isNotBlank()) v.adecuacion else ""
            )
        }.toMutableList()
    }

    fun esEdicion(): Boolean = id > 0L || folio.isNotBlank()
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
    val descuentoHS875: Double = 0.0,
    val descuentoHS1250: Double = 0.0,
    val descuentoHS1500: Double = 0.0,
    val descuentoDolaresPorM2: Double = 0.0,
    val updatedAt: Long = 0L
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
        val precioVenta = producto.getPrecioVenta()  // Ahora usa precio dinámico
        return if (precioVenta > 0) (descuento / precioVenta) * 100 else 0.0
    }

    fun productosOrdenados(): List<TipoProducto> = productos.sortedBy { it.getPrecioVenta() }

    fun fueEditada(): Boolean = updatedAt > 0L

    fun getUpdatedAtFormatted(): String {
        if (updatedAt == 0L) return ""
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(updatedAt))
    }
}