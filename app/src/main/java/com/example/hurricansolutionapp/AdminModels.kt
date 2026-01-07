package com.example.hurricansolutionapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo que representa la configuración de precios en Supabase
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
 * Modelo para actualizar precios (sin campos de solo lectura)
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

    val ventanas: kotlinx.serialization.json.JsonElement? = null, // Puede ser array o string

    @SerialName("pdf_path")
    val pdfPath: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Modelo para insertar cotización (sin campos auto-generados)
 * Usa tipos que Supabase puede serializar directamente a JSONB
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

    // Totales individuales (columnas separadas en la tabla)
    @SerialName("total_hs875")
    val totalHs875: Double = 0.0,

    @SerialName("total_hs1250")
    val totalHs1250: Double = 0.0,

    @SerialName("total_hs1500")
    val totalHs1500: Double = 0.0,

    // Totales como objeto JSON (respaldo)
    val totales: Map<String, Double> = emptyMap(),

    // Ventanas como lista de objetos JSON
    val ventanas: List<VentanaInsert> = emptyList(),

    @SerialName("pdf_path")
    val pdfPath: String? = null
)

/**
 * Modelo para insertar una ventana en la cotización
 */
@Serializable
data class VentanaInsert(
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