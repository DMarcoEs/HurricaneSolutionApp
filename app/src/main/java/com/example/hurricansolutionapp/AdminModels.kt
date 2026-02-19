package com.example.hurricansolutionapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * MODELOS DE CONFIGURACIÓN Y PRECIOS
 * ═══════════════════════════════════════════════════════════════════════════════
 */

/**
 * Modelo que representa la configuración de precios en Supabase (LEGACY)
 * Se mantiene por compatibilidad pero ahora se usan precios por zona
 */
@Serializable
data class AppConfig(
    val id: Int = 1,

    @SerialName("hs875_precio_venta")
    val hs875PrecioVenta: Double = 150.0,

    @SerialName("hs875_precio_base")
    val hs875PrecioBase: Double = 130.0,

    @SerialName("hs1250_precio_venta")
    val hs1250PrecioVenta: Double = 180.0,

    @SerialName("hs1250_precio_base")
    val hs1250PrecioBase: Double = 150.0,

    @SerialName("hs1500_precio_venta")
    val hs1500PrecioVenta: Double = 210.0,

    @SerialName("hs1500_precio_base")
    val hs1500PrecioBase: Double = 170.0,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    @SerialName("updated_by")
    val updatedBy: String? = null
)

/**
 * Modelo para actualizar precios (sin campos de solo lectura) - LEGACY
 */
@Serializable
data class AppConfigUpdate(
    @SerialName("hs875_precio_venta")
    val hs875PrecioVenta: Double,

    @SerialName("hs875_precio_base")
    val hs875PrecioBase: Double,

    @SerialName("hs1250_precio_venta")
    val hs1250PrecioVenta: Double,

    @SerialName("hs1250_precio_base")
    val hs1250PrecioBase: Double,

    @SerialName("hs1500_precio_venta")
    val hs1500PrecioVenta: Double,

    @SerialName("hs1500_precio_base")
    val hs1500PrecioBase: Double,

    @SerialName("updated_by")
    val updatedBy: String
)

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * NUEVOS MODELOS - PRECIOS POR ZONA GEOGRÁFICA
 * ═══════════════════════════════════════════════════════════════════════════════
 */

/**
 * Modelo que representa los precios de una zona específica
 */
@Serializable
data class PrecioZona(
    val id: Int = 0,

    val zona: String = "continental", // 'continental', 'islas', 'foranea'

    @SerialName("zona_nombre")
    val zonaNombre: String = "Zona Continental",

    @SerialName("hs875_precio_venta")
    val hs875PrecioVenta: Double = 250.0,

    @SerialName("hs875_precio_base")
    val hs875PrecioBase: Double = 130.0,

    @SerialName("hs1250_precio_venta")
    val hs1250PrecioVenta: Double = 300.0,

    @SerialName("hs1250_precio_base")
    val hs1250PrecioBase: Double = 150.0,

    @SerialName("hs1500_precio_venta")
    val hs1500PrecioVenta: Double = 350.0,

    @SerialName("hs1500_precio_base")
    val hs1500PrecioBase: Double = 170.0,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    @SerialName("updated_by")
    val updatedBy: String? = null
) {
    /**
     * Convierte a AppConfig para compatibilidad con código existente
     */
    fun toAppConfig(): AppConfig = AppConfig(
        id = id,
        hs875PrecioVenta = hs875PrecioVenta,
        hs875PrecioBase = hs875PrecioBase,
        hs1250PrecioVenta = hs1250PrecioVenta,
        hs1250PrecioBase = hs1250PrecioBase,
        hs1500PrecioVenta = hs1500PrecioVenta,
        hs1500PrecioBase = hs1500PrecioBase,
        updatedAt = updatedAt,
        updatedBy = updatedBy
    )
}

/**
 * Modelo para actualizar precios de una zona
 */
@Serializable
data class PrecioZonaUpdate(
    @SerialName("hs875_precio_venta")
    val hs875PrecioVenta: Double,

    @SerialName("hs875_precio_base")
    val hs875PrecioBase: Double,

    @SerialName("hs1250_precio_venta")
    val hs1250PrecioVenta: Double,

    @SerialName("hs1250_precio_base")
    val hs1250PrecioBase: Double,

    @SerialName("hs1500_precio_venta")
    val hs1500PrecioVenta: Double,

    @SerialName("hs1500_precio_base")
    val hs1500PrecioBase: Double,

    @SerialName("updated_by")
    val updatedBy: String
)

/**
 * Contenedor de precios para las 3 zonas
 */
data class PreciosTodasZonas(
    val continental: PrecioZona = PrecioZona(zona = "continental", zonaNombre = "Zona Continental"),
    val islas: PrecioZona = PrecioZona(zona = "islas", zonaNombre = "Zona Islas"),
    val foranea: PrecioZona = PrecioZona(zona = "foranea", zonaNombre = "Zona Foránea")
) {
    /**
     * Obtiene los precios de una zona específica
     */
    fun getPreciosZona(zona: ZonaGeografica): PrecioZona = when (zona) {
        ZonaGeografica.CONTINENTAL -> continental
        ZonaGeografica.ISLAS -> islas
        ZonaGeografica.FORANEA -> foranea
    }

    /**
     * Obtiene los precios de una zona por su ID string
     */
    fun getPreciosZona(zonaId: String): PrecioZona = when (zonaId) {
        "continental" -> continental
        "islas" -> islas
        "foranea" -> foranea
        else -> continental
    }
}

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * MODELOS DE COTIZACIÓN
 * ═══════════════════════════════════════════════════════════════════════════════
 */

/**
 * Modelo para cotización en Supabase (sincronización en la nube)
 */
@Serializable
data class CotizacionRemota(
    val id: Long? = null,
    val folio: String = "",

    @SerialName("user_id")
    val userId: String = "",

    @SerialName("especialista_nombre")
    val especialistaNombre: String = "",

    @SerialName("cliente_nombre")
    val clienteNombre: String = "",

    @SerialName("cliente_telefono")
    val clienteTelefono: String? = null,

    val ubicacion: String? = null,
    val ciudad: String? = null,
    val colonia: String? = null,
    val calle: String? = null,
    val numero: String? = null,
    val fecha: String = "",
    val productos: List<String> = listOf("HS875"),

    @SerialName("tipo_montaje")
    val tipoMontaje: String? = "Flush Mount",

    @SerialName("area_total")
    val areaTotal: Double = 0.0,

    @SerialName("descuento_hs875")
    val descuentoHs875: Double? = 0.0,

    @SerialName("descuento_hs1250")
    val descuentoHs1250: Double? = 0.0,

    @SerialName("descuento_hs1500")
    val descuentoHs1500: Double? = 0.0,

    @SerialName("total_hs875")
    val totalHs875: Double? = 0.0,

    @SerialName("total_hs1250")
    val totalHs1250: Double? = 0.0,

    @SerialName("total_hs1500")
    val totalHs1500: Double? = 0.0,

    val totales: Map<String, Double>? = null,

    val ventanas: kotlinx.serialization.json.JsonElement? = null,

    @SerialName("pdf_path")
    val pdfPath: String? = null,

    @SerialName("lead_id")
    val leadId: String? = null,

    @SerialName("fecha_solicitada")
    val fechaSolicitada: String? = null,

    val observaciones: String? = null,

    @SerialName("enviado_instalacion")
    val enviadoInstalacion: Boolean? = false,

    @SerialName("fecha_envio_instalacion")
    val fechaEnvioInstalacion: String? = null,

    // ✅ NUEVO: Zona geográfica
    @SerialName("zona_geografica")
    val zonaGeografica: String? = "continental",

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Modelo para insertar cotización (sin campos auto-generados)
 */
@Serializable
data class CotizacionInsert(
    val folio: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("especialista_nombre")
    val especialistaNombre: String,

    @SerialName("cliente_nombre")
    val clienteNombre: String,

    @SerialName("cliente_telefono")
    val clienteTelefono: String? = null,

    val ubicacion: String? = null,
    val ciudad: String? = null,
    val colonia: String? = null,
    val calle: String? = null,
    val numero: String? = null,
    val fecha: String,
    val productos: List<String> = listOf("HS875"),

    @SerialName("tipo_montaje")
    val tipoMontaje: String = "Flush Mount",

    @SerialName("area_total")
    val areaTotal: Double = 0.0,

    @SerialName("descuento_hs875")
    val descuentoHs875: Double = 0.0,

    @SerialName("descuento_hs1250")
    val descuentoHs1250: Double = 0.0,

    @SerialName("descuento_hs1500")
    val descuentoHs1500: Double = 0.0,

    @SerialName("total_hs875")
    val totalHs875: Double = 0.0,

    @SerialName("total_hs1250")
    val totalHs1250: Double = 0.0,

    @SerialName("total_hs1500")
    val totalHs1500: Double = 0.0,

    val totales: Map<String, Double> = emptyMap(),

    val ventanas: List<VentanaInsert> = emptyList(),

    @SerialName("pdf_path")
    val pdfPath: String? = null,

    @SerialName("lead_id")
    val leadId: String? = null,

    @SerialName("fecha_solicitada")
    val fechaSolicitada: String? = null,

    val observaciones: String? = null,

    // ✅ NUEVO: Zona geográfica
    @SerialName("zona_geografica")
    val zonaGeografica: String = "continental"
)

/**
 * Modelo para insertar una ventana en la cotización
 */
@Serializable
data class VentanaInsert(
    val zona: String = "",
    val descripcion: String,
    val alto: Double,
    val ancho: Double,
    @SerialName("precio_m2")
    val precioM2: Double,
    val adecuacion: String,
    @SerialName("tipo_montaje")
    val tipoMontaje: String
)

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * MODELOS DE USUARIO
 * ═══════════════════════════════════════════════════════════════════════════════
 */

/**
 * Información del perfil de usuario (para lista de empleados)
 */
@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val role: String,
    @SerialName("is_active")
    val isActive: Boolean = true
)