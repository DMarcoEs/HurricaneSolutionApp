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
 * ACTUALIZADO: Ahora soporta selección múltiple de mecanismos (Manual, Eléctrico, o Ambos)
 */
data class MedidaRainFormState(
    var zona: String = "",           // Zona del área (ej: Terraza, Sala)
    var descripcion: String = "",    // Área a proteger (ej: Ventana principal)
    var alto: String = "",
    var ancho: String = "",
    var piezas: String = "1",        // Número de piezas (cada pieza lleva su propio mecanismo)
    var incluyeManual: Boolean = true,    // Si incluye opción Manual
    var incluyeElectrico: Boolean = false // Si incluye opción Eléctrico
) {
    fun isValid(): Boolean {
        val altoNum = alto.toDoubleOrNull() ?: return false
        val anchoNum = ancho.toDoubleOrNull() ?: return false
        val piezasNum = piezas.toIntOrNull() ?: return false
        // Debe tener al menos un tipo de mecanismo seleccionado
        val tieneAlMenosUnTipo = incluyeManual || incluyeElectrico
        return zona.isNotBlank() && altoNum > 0 && anchoNum > 0 && piezasNum >= 1 && tieneAlMenosUnTipo
    }

    fun toMedidaRain(): MedidaRain? {
        if (!isValid()) return null
        val altoNum = alto.toDoubleOrNull() ?: return null
        val anchoNum = ancho.toDoubleOrNull() ?: return null
        val piezasNum = piezas.toIntOrNull() ?: 1

        // Calcular subtotales usando RainPriceManager
        val subtotalManual = if (incluyeManual) {
            RainPriceManager.calcularSubtotalArea(altoNum, anchoNum, TipoMecanismo.MANUAL, piezasNum)
        } else 0.0

        val subtotalElectrico = if (incluyeElectrico) {
            RainPriceManager.calcularSubtotalArea(altoNum, anchoNum, TipoMecanismo.ELECTRICO, piezasNum)
        } else 0.0

        return MedidaRain(
            descripcion = if (descripcion.isNotBlank()) "$zona - $descripcion" else zona,
            alto = altoNum,
            ancho = anchoNum,
            piezas = piezasNum,
            incluyeManual = incluyeManual,
            incluyeElectrico = incluyeElectrico,
            subtotalManual = subtotalManual,
            subtotalElectrico = subtotalElectrico
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
    val incluyeManual: Boolean = true,
    val incluyeElectrico: Boolean = false,
    val subtotalManual: Double = 0.0,
    val subtotalElectrico: Double = 0.0
) {
    val areaM2: Double get() = alto * ancho * piezas

    // Subtotal combinado (para cálculos que necesiten el total de la apertura)
    val subtotal: Double get() = subtotalManual + subtotalElectrico

    // Para compatibilidad: devuelve el tipo predominante o AMBOS
    val tipoMecanismo: TipoMecanismo get() = when {
        incluyeManual && incluyeElectrico -> TipoMecanismo.MANUAL // Por defecto si ambos
        incluyeElectrico -> TipoMecanismo.ELECTRICO
        else -> TipoMecanismo.MANUAL
    }

    // Helper para saber si incluye ambos tipos
    val incluyeAmbos: Boolean get() = incluyeManual && incluyeElectrico

    /**
     * Convierte a JSON para guardar en Supabase
     */
    fun toJson(): MedidaRainJson {
        return MedidaRainJson(
            nombre = descripcion,
            alto = alto,
            ancho = ancho,
            piezas = piezas,
            incluyeManual = incluyeManual,
            incluyeElectrico = incluyeElectrico,
            subtotalManual = subtotalManual,
            subtotalElectrico = subtotalElectrico
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
    var tipoControl: String = "multicanal",  // "multicanal" (default) o "monocanal"
    var cantidadControles: Int = 0,           // Solo aplica si es monocanal
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
        tipoControl = "multicanal"
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
        tipoControl = cotizacion.tipoControl
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
                incluyeManual = m.incluyeManual,
                incluyeElectrico = m.incluyeElectrico
            )
        }.toMutableList()

        if (medidasForm.isEmpty()) {
            medidasForm = mutableListOf(MedidaRainFormState())
        }
    }

    fun esEdicion(): Boolean = id > 0L || folio.isNotBlank()

    fun getMedidas(): List<MedidaRain> = medidasForm.mapNotNull { it.toMedidaRain() }

    // Subtotal combinado (medidas + accesorios — TODO lleva descuento)
    fun getSubtotal(): Double = getMedidas().sumOf { it.subtotal } + getCostoAccesorios()

    // Subtotales separados por tipo de mecanismo
    fun getSubtotalManual(): Double = getMedidas().sumOf { it.subtotalManual } + getCostoAccesorios()
    fun getSubtotalElectrico(): Double = getMedidas().sumOf { it.subtotalElectrico }

    fun getDescuentoPorcentaje(): Double = RainPriceManager.getDescuentoPorZona(zonaGeografica)

    fun getDescuentoMonto(): Double = getSubtotal() * (getDescuentoPorcentaje() / 100)
    fun getDescuentoMontoManual(): Double = getSubtotalManual() * (getDescuentoPorcentaje() / 100)
    fun getDescuentoMontoElectrico(): Double = getSubtotalElectrico() * (getDescuentoPorcentaje() / 100)

    /**
     * Calcula costo de accesorios adicionales
     * Estos se suman al Sub-Total 1 ANTES del descuento
     */
    fun getCostoAccesorios(): Double {
        val manCount = if (quiereManivelas) cantidadManivelas else 0
        val ctrlCount = if (tipoControl == "monocanal") cantidadControles else 0
        return RainPriceManager.calcularCostoAccesorios(manCount, tipoControl, ctrlCount)
    }

    /**
     * Total = Sub-Total 1 - Descuento + IVA
     * Accesorios ya están incluidos en Sub-Total 1
     */
    fun getTotal(): Double {
        val sub2 = getSubtotal() - getDescuentoMonto()
        return sub2 + (sub2 * 0.16)
    }

    // Totales separados por tipo
    fun getTotalManual(): Double {
        val sub2 = getSubtotalManual() - getDescuentoMontoManual()
        return sub2 + (sub2 * 0.16)
    }
    fun getTotalElectrico(): Double {
        val sub2 = getSubtotalElectrico() - getDescuentoMontoElectrico()
        return sub2 + (sub2 * 0.16)
    }

    fun getTotalAreas(): Int = getMedidas().size

    // Cuenta las piezas que INCLUYEN cada tipo de mecanismo
    fun getAreasElectricas(): Int = getMedidas().filter { it.incluyeElectrico }.sumOf { it.piezas }
    fun getAreasManuales(): Int = getMedidas().filter { it.incluyeManual }.sumOf { it.piezas }

    // Verifica si la cotización tiene algún mecanismo de cada tipo
    fun tieneManual(): Boolean = getMedidas().any { it.incluyeManual }
    fun tieneElectrico(): Boolean = getMedidas().any { it.incluyeElectrico }
    fun tieneAmbos(): Boolean = tieneManual() && tieneElectrico()

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
    val subtotalManual: Double = 0.0,
    val subtotalElectrico: Double = 0.0,
    val descuentoPorcentaje: Double = 0.0,
    val descuentoMonto: Double = 0.0,
    val total: Double = 0.0,
    val totalManual: Double = 0.0,
    val totalElectrico: Double = 0.0,
    val totalAreas: Int = 0,
    val areasElectricas: Int = 0,
    val areasManuales: Int = 0,
    val pdfPath: String? = null,
    val observaciones: String = "",
    val updatedAt: Long = 0L,
    // Accesorios adicionales
    val controlesAdicionales: Int = 0,
    val manivelasAdicionales: Int = 0,
    val costoAccesorios: Double = 0.0,
    val tipoControl: String = "multicanal"
) {
    fun fueEditada(): Boolean = updatedAt > 0L

    fun getUpdatedAtFormatted(): String {
        if (updatedAt == 0L) return ""
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(updatedAt))
    }

    // Verifica si la cotización tiene algún mecanismo de cada tipo
    fun tieneManual(): Boolean = medidas.any { it.incluyeManual }
    fun tieneElectrico(): Boolean = medidas.any { it.incluyeElectrico }
    fun tieneAmbos(): Boolean = tieneManual() && tieneElectrico()

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