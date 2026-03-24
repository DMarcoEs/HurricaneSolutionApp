package com.example.hurricansolutionapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ═══════════════════════════════════════════════════════════════════════════════
// RAIN PROTECTION - MODELOS DE DATOS
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Tipo de mecanismo para Rain Protection
 */
enum class TipoMecanismo(val id: String, val etiqueta: String) {
    MANUAL("manual", "Manual"),
    ELECTRICO("electrico", "Eléctrico");

    companion object {
        fun fromId(id: String): TipoMecanismo {
            return entries.find { it.id == id.lowercase() } ?: MANUAL
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ESTADO DEL FORMULARIO PARA UNA MEDIDA RAIN
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Estado del formulario para capturar una medida de Rain Protection
 */
data class MedidaRainFormState(
    var zona: String = "",           // Zona del área (ej: Terraza, Sala)
    var descripcion: String = "",    // Área a proteger (ej: Ventana principal)
    var alto: String = "",
    var ancho: String = "",
    var piezas: String = "1",        // Número de piezas (cada pieza lleva su propio mecanismo)
    var tipoMecanismo: TipoMecanismo = TipoMecanismo.MANUAL
) {
    fun isValid(): Boolean {
        val altoNum = alto.toDoubleOrNull() ?: return false
        val anchoNum = ancho.toDoubleOrNull() ?: return false
        val piezasNum = piezas.toIntOrNull() ?: return false
        return zona.isNotBlank() && altoNum > 0 && anchoNum > 0 && piezasNum >= 1
    }

    fun toMedidaRain(): MedidaRain? {
        if (!isValid()) return null
        val altoNum = alto.toDoubleOrNull() ?: return null
        val anchoNum = ancho.toDoubleOrNull() ?: return null
        val piezasNum = piezas.toIntOrNull() ?: 1

        // Calcular subtotal usando RainPriceManager (piezas multiplica todo)
        val subtotal = RainPriceManager.calcularSubtotalArea(altoNum, anchoNum, tipoMecanismo, piezasNum)

        return MedidaRain(
            descripcion = if (descripcion.isNotBlank()) "$zona - $descripcion" else zona,
            alto = altoNum,
            ancho = anchoNum,
            piezas = piezasNum,
            tipoMecanismo = tipoMecanismo,
            subtotal = subtotal
        )
    }
}

/**
 * Alias para compatibilidad con RainMedidasScreen
 */
typealias RainAreaFormState = MedidaRainFormState

// ═══════════════════════════════════════════════════════════════════════════════
// MEDIDA INDIVIDUAL RAIN
// ═══════════════════════════════════════════════════════════════════════════════

data class MedidaRain(
    val descripcion: String,
    val alto: Double,
    val ancho: Double,
    val piezas: Int = 1,
    val tipoMecanismo: TipoMecanismo = TipoMecanismo.MANUAL,
    val subtotal: Double = 0.0
) {
    val areaM2: Double get() = alto * ancho * piezas

    /**
     * Convierte a JSON para guardar en Supabase
     */
    fun toJson(): MedidaRainJson {
        return MedidaRainJson(
            nombre = descripcion,
            alto = alto,
            ancho = ancho,
            piezas = piezas,
            tipoMecanismo = tipoMecanismo.id,
            subtotal = subtotal
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DRAFT DE COTIZACIÓN RAIN
// ═══════════════════════════════════════════════════════════════════════════════

data class CotizacionRainDraft(
    var id: Long = 0L,
    var folio: String = "",
    var nombre: String = "",
    var telefono: String = "",
    var ciudad: String = "",
    var colonia: String = "",
    var direccionDetalle: String = "",
    var fecha: String = "",
    var leadId: String? = null,
    var esClienteActual: Boolean = false,
    var zonaGeografica: ZonaGeografica = ZonaGeografica.CONTINENTAL,
    var tipoPropiedad: String = "",
    var medidasForm: MutableList<MedidaRainFormState> = mutableListOf(MedidaRainFormState()),
    var observaciones: String = "",
    // Accesorios adicionales
    var quiereControles: Boolean = false,
    var cantidadControles: Int = 0,
    var quiereManivelas: Boolean = false,
    var cantidadManivelas: Int = 0
) {
    // Alias para compatibilidad con RainMedidasScreen
    var areas: MutableList<RainAreaFormState>
        get() = medidasForm
        set(value) { medidasForm = value }

    fun clear() {
        id = 0L
        folio = ""
        nombre = ""
        telefono = ""
        ciudad = ""
        colonia = ""
        direccionDetalle = ""
        fecha = ""
        leadId = null
        esClienteActual = false
        zonaGeografica = ZonaGeografica.CONTINENTAL
        tipoPropiedad = ""
        medidasForm = mutableListOf(MedidaRainFormState())
        observaciones = ""
        quiereControles = false
        cantidadControles = 0
        quiereManivelas = false
        cantidadManivelas = 0
    }

    /**
     * Pre-llena desde un CotizacionDraft de huracanes (flujo de agregar otro producto)
     */
    fun cargarDesdeCotizacionHuracan(draft: CotizacionDraft) {
        nombre = draft.nombre
        telefono = draft.telefono
        ciudad = draft.ciudad
        colonia = draft.colonia
        direccionDetalle = draft.direccionDetalle
        leadId = draft.leadId
        esClienteActual = draft.esClienteActual
        zonaGeografica = draft.zonaGeografica
        tipoPropiedad = draft.tipoPropiedad
    }

    /**
     * Carga desde una cotización Rain existente (para edición)
     */
    fun cargarDesdeCotizacionRain(cotizacion: CotizacionRain) {
        id = cotizacion.id
        folio = cotizacion.folio
        nombre = cotizacion.clienteNombre
        telefono = cotizacion.clienteTelefono
        ciudad = cotizacion.ciudad
        zonaGeografica = cotizacion.zonaGeografica
        tipoPropiedad = cotizacion.tipoPropiedad

        val partes = cotizacion.ubicacion.split(",").map { it.trim() }
        colonia = partes.getOrNull(1) ?: ""
        direccionDetalle = partes.getOrNull(2) ?: ""

        fecha = cotizacion.fecha
        observaciones = cotizacion.observaciones

        // Accesorios
        quiereControles = cotizacion.controlesAdicionales > 0
        cantidadControles = cotizacion.controlesAdicionales
        quiereManivelas = cotizacion.manivelasAdicionales > 0
        cantidadManivelas = cotizacion.manivelasAdicionales

        medidasForm = cotizacion.medidas.map { m ->
            val partesMedida = m.descripcion.split(" - ")
            MedidaRainFormState(
                zona = partesMedida.getOrNull(0) ?: m.descripcion,
                descripcion = partesMedida.getOrNull(1) ?: "",
                alto = String.format("%.2f", m.alto),
                ancho = String.format("%.2f", m.ancho),
                piezas = m.piezas.toString(),
                tipoMecanismo = m.tipoMecanismo
            )
        }.toMutableList()

        if (medidasForm.isEmpty()) {
            medidasForm = mutableListOf(MedidaRainFormState())
        }
    }

    fun esEdicion(): Boolean = id > 0L || folio.isNotBlank()

    fun getMedidas(): List<MedidaRain> = medidasForm.mapNotNull { it.toMedidaRain() }

    fun getSubtotal(): Double = getMedidas().sumOf { it.subtotal }

    fun getDescuentoPorcentaje(): Double = RainPriceManager.getDescuentoPorZona(zonaGeografica)

    fun getDescuentoMonto(): Double = getSubtotal() * (getDescuentoPorcentaje() / 100)

    /**
     * Calcula costo de accesorios adicionales (SIN descuento de zona)
     */
    fun getCostoAccesorios(): Double {
        val ctrlCount = if (quiereControles) cantidadControles else 0
        val manCount = if (quiereManivelas) cantidadManivelas else 0
        return RainPriceManager.calcularCostoAccesorios(ctrlCount, manCount)
    }

    /**
     * Total = (Subtotal - Descuento) + Accesorios
     * Los accesorios NO llevan descuento de zona
     */
    fun getTotal(): Double = (getSubtotal() - getDescuentoMonto()) + getCostoAccesorios()

    fun getTotalAreas(): Int = getMedidas().size

    fun getAreasElectricas(): Int = getMedidas().filter { it.tipoMecanismo == TipoMecanismo.ELECTRICO }.sumOf { it.piezas }

    fun getAreasManuales(): Int = getMedidas().filter { it.tipoMecanismo == TipoMecanismo.MANUAL }.sumOf { it.piezas }

    fun getTotalTelas(): Int = getMedidas().sumOf { it.piezas }

    /**
     * Convierte las medidas a JSON string para Supabase
     */
    fun getMedidasJson(): String {
        val medidasJson = getMedidas().map { it.toJson() }
        return Json.encodeToString(medidasJson)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COTIZACIÓN RAIN COMPLETA (modelo local)
// ═══════════════════════════════════════════════════════════════════════════════

data class CotizacionRain(
    val id: Long = 0L,
    val folio: String = "",
    val clienteNombre: String,
    val clienteTelefono: String,
    val ubicacion: String,
    val ciudad: String = "",
    val especialista: String,
    val fecha: String,
    val medidas: List<MedidaRain>,
    val zonaGeografica: ZonaGeografica = ZonaGeografica.CONTINENTAL,
    val tipoPropiedad: String = "",
    val subtotal: Double = 0.0,
    val descuentoPorcentaje: Double = 0.0,
    val descuentoMonto: Double = 0.0,
    val total: Double = 0.0,
    val totalAreas: Int = 0,
    val areasElectricas: Int = 0,
    val areasManuales: Int = 0,
    val pdfPath: String? = null,
    val observaciones: String = "",
    val updatedAt: Long = 0L,
    // Accesorios adicionales
    val controlesAdicionales: Int = 0,
    val manivelasAdicionales: Int = 0,
    val costoAccesorios: Double = 0.0
) {
    fun fueEditada(): Boolean = updatedAt > 0L

    fun getUpdatedAtFormatted(): String {
        if (updatedAt == 0L) return ""
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(updatedAt))
    }

    /**
     * Obtiene el texto para mostrar tipo de mecanismo en cards
     * Si hay varios del mismo tipo, muestra: "Eléctrico: 3, Manual: 2"
     * Si solo hay uno de cada uno, muestra: "Eléctrico, Manual"
     */
    fun getTipoMecanismoDisplay(): String {
        if (areasElectricas == 0 && areasManuales == 0) return "-"

        val parts = mutableListOf<String>()

        if (areasElectricas > 0) {
            parts.add(if (areasElectricas > 1) "Eléctrico: $areasElectricas" else "Eléctrico")
        }
        if (areasManuales > 0) {
            parts.add(if (areasManuales > 1) "Manual: $areasManuales" else "Manual")
        }

        return parts.joinToString(", ")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TIPO DE COTIZACIÓN (para historial unificado)
// ═══════════════════════════════════════════════════════════════════════════════

enum class TipoCotizacion(val etiqueta: String, val prefijo: String) {
    HURRICANE("Hurricane Protection", "HS"),
    RAIN("Rain Protection", "RP")
}

/**
 * Wrapper para mostrar cotizaciones de ambos tipos en un historial unificado
 */
sealed class CotizacionUnificada {
    abstract val id: Long
    abstract val folio: String
    abstract val clienteNombre: String
    abstract val fecha: String
    abstract val tipo: TipoCotizacion
    abstract val fueEditada: Boolean
    abstract val especialista: String

    data class Hurricane(val cotizacion: Cotizacion) : CotizacionUnificada() {
        override val id: Long get() = cotizacion.id
        override val folio: String get() = cotizacion.folio
        override val clienteNombre: String get() = cotizacion.clienteNombre
        override val fecha: String get() = cotizacion.fecha
        override val tipo: TipoCotizacion get() = TipoCotizacion.HURRICANE
        override val fueEditada: Boolean get() = cotizacion.fueEditada()
        override val especialista: String get() = cotizacion.especialista

        // Datos específicos de Hurricane
        val areaTotal: Double get() = cotizacion.areaTotal
        val productos: List<TipoProducto> get() = cotizacion.productos
        val tipoMontaje: String get() = cotizacion.tipoMontaje
        fun totalPorProducto(producto: TipoProducto) = cotizacion.totalPorProducto(producto)
        fun totalConDescuento(producto: TipoProducto) = cotizacion.totalConDescuento(producto)
    }

    data class Rain(val cotizacion: CotizacionRain) : CotizacionUnificada() {
        override val id: Long get() = cotizacion.id
        override val folio: String get() = cotizacion.folio
        override val clienteNombre: String get() = cotizacion.clienteNombre
        override val fecha: String get() = cotizacion.fecha
        override val tipo: TipoCotizacion get() = TipoCotizacion.RAIN
        override val fueEditada: Boolean get() = cotizacion.fueEditada()
        override val especialista: String get() = cotizacion.especialista

        // Datos específicos de Rain
        val totalAreas: Int get() = cotizacion.totalAreas
        val areasElectricas: Int get() = cotizacion.areasElectricas
        val areasManuales: Int get() = cotizacion.areasManuales
        val total: Double get() = cotizacion.total
        val tipoMecanismoDisplay: String get() = cotizacion.getTipoMecanismoDisplay()
    }
}