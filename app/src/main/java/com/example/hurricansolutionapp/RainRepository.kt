package com.example.hurricansolutionapp

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * RAIN REPOSITORY - CONEXIÓN CON SUPABASE
 * ═══════════════════════════════════════════════════════════════════════════════
 */
object RainRepository {

    private val client get() = SupabaseClientProvider.client

    // ═══════════════════════════════════════════════════════════════════════════
    // PRECIOS Y DESCUENTOS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene todos los precios de componentes Rain
     */
    suspend fun getPrecios(): List<PrecioRainComponente> = withContext(Dispatchers.IO) {
        try {
            client.from("precios_rain")
                .select()
                .decodeList<PrecioRainComponente>()
        } catch (e: Exception) {
            android.util.Log.e("RainRepository", "Error obteniendo precios: ${e.message}")
            emptyList()
        }
    }

    /**
     * Obtiene los descuentos por zona
     */
    suspend fun getDescuentos(): List<RainDescuentoRemoto> = withContext(Dispatchers.IO) {
        try {
            client.from("rain_descuentos")
                .select()
                .decodeList<RainDescuentoRemoto>()
        } catch (e: Exception) {
            android.util.Log.e("RainRepository", "Error obteniendo descuentos: ${e.message}")
            emptyList()
        }
    }

    /**
     * Actualiza un precio de componente (solo admin)
     */
    suspend fun updatePrecio(componente: String, precio: Double, userId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                client.from("precios_rain")
                    .update({
                        set("precio", precio)
                        set("updated_by", userId)
                    }) {
                        filter { eq("componente", componente) }
                    }
                true
            } catch (e: Exception) {
                android.util.Log.e("RainRepository", "Error actualizando precio: ${e.message}")
                false
            }
        }

    /**
     * Actualiza un descuento por zona (solo admin)
     */
    suspend fun updateDescuento(zona: String, descuento: Double, userId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                client.from("rain_descuentos")
                    .update({
                        set("descuento", descuento)
                        set("updated_by", userId)
                    }) {
                        filter { eq("zona", zona) }
                    }
                true
            } catch (e: Exception) {
                android.util.Log.e("RainRepository", "Error actualizando descuento: ${e.message}")
                false
            }
        }

    // ═══════════════════════════════════════════════════════════════════════════
    // COTIZACIONES RAIN
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Guarda una nueva cotización Rain
     */
    suspend fun saveCotizacion(cotizacion: CotizacionRainInsert): Result<Long> =
        withContext(Dispatchers.IO) {
            try {
                val result = client.from("cotizaciones_rain")
                    .insert(cotizacion) {
                        select()
                    }
                    .decodeSingle<CotizacionRainRemota>()

                Result.success(result.id ?: 0L)
            } catch (e: Exception) {
                android.util.Log.e("RainRepository", "Error guardando cotización: ${e.message}")
                Result.failure(e)
            }
        }

    /**
     * Actualiza una cotización Rain existente
     */
    suspend fun updateCotizacion(id: Long, cotizacion: CotizacionRainUpdate): Boolean =
        withContext(Dispatchers.IO) {
            try {
                client.from("cotizaciones_rain")
                    .update(cotizacion) {
                        filter { eq("id", id) }
                    }
                true
            } catch (e: Exception) {
                android.util.Log.e("RainRepository", "Error actualizando cotización: ${e.message}")
                false
            }
        }

    /**
     * Obtiene cotizaciones Rain por usuario
     */
    suspend fun getCotizacionesByUser(userId: String): List<CotizacionRainRemota> =
        withContext(Dispatchers.IO) {
            try {
                client.from("cotizaciones_rain")
                    .select {
                        filter { eq("user_id", userId) }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<CotizacionRainRemota>()
            } catch (e: Exception) {
                android.util.Log.e("RainRepository", "Error obteniendo cotizaciones: ${e.message}")
                emptyList()
            }
        }

    /**
     * Obtiene todas las cotizaciones Rain (para admin)
     */
    suspend fun getAllCotizaciones(): List<CotizacionRainRemota> =
        withContext(Dispatchers.IO) {
            try {
                client.from("cotizaciones_rain")
                    .select {
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<CotizacionRainRemota>()
            } catch (e: Exception) {
                android.util.Log.e("RainRepository", "Error obteniendo todas las cotizaciones: ${e.message}")
                emptyList()
            }
        }

    /**
     * Obtiene una cotización Rain por folio
     */
    suspend fun getCotizacionByFolio(folio: String): CotizacionRainRemota? =
        withContext(Dispatchers.IO) {
            try {
                client.from("cotizaciones_rain")
                    .select {
                        filter { eq("folio", folio) }
                    }
                    .decodeSingleOrNull<CotizacionRainRemota>()
            } catch (e: Exception) {
                android.util.Log.e("RainRepository", "Error obteniendo cotización por folio: ${e.message}")
                null
            }
        }

    /**
     * Elimina una cotización Rain
     */
    suspend fun deleteCotizacion(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            client.from("cotizaciones_rain")
                .delete {
                    filter { eq("id", id) }
                }
            true
        } catch (e: Exception) {
            android.util.Log.e("RainRepository", "Error eliminando cotización: ${e.message}")
            false
        }
    }

    /**
     * Actualiza el path del PDF
     */
    suspend fun updatePdfPath(id: Long, pdfPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            client.from("cotizaciones_rain")
                .update({
                    set("pdf_path", pdfPath)
                }) {
                    filter { eq("id", id) }
                }
            true
        } catch (e: Exception) {
            android.util.Log.e("RainRepository", "Error actualizando pdf_path: ${e.message}")
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FOLIOS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene el máximo folio Rain para un prefijo
     */
    suspend fun getMaxFolioForPrefix(prefix: String): Int = withContext(Dispatchers.IO) {
        try {
            val cotizaciones = client.from("cotizaciones_rain")
                .select(columns = Columns.list("folio")) {
                    filter {
                        like("folio", "$prefix%")
                    }
                }
                .decodeList<FolioOnlyRain>()

            var maxNum = 0
            for (cot in cotizaciones) {
                // Formato: SU001-RP
                val regex = Regex("^${prefix}(\\d+)-RP$")
                regex.find(cot.folio)?.let { match ->
                    val num = match.groupValues[1].toIntOrNull() ?: 0
                    if (num > maxNum) maxNum = num
                }
            }
            maxNum
        } catch (e: Exception) {
            android.util.Log.e("RainRepository", "Error obteniendo max folio: ${e.message}")
            0
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODELOS PARA SUPABASE
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class PrecioRainComponente(
    val id: Int = 0,
    val componente: String,
    val unidad: String,
    val precio: Double,
    val descripcion: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null
)

@Serializable
data class RainDescuentoRemoto(
    val id: Int = 0,
    val zona: String,
    val descuento: Double,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("updated_by") val updatedBy: String? = null
)

@Serializable
data class CotizacionRainRemota(
    val id: Long? = null,
    val folio: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("especialista_nombre") val especialistaNombre: String = "",
    @SerialName("cliente_nombre") val clienteNombre: String = "",
    @SerialName("cliente_telefono") val clienteTelefono: String? = null,
    val ubicacion: String? = null,
    val ciudad: String? = null,
    val colonia: String? = null,
    val calle: String? = null,
    val numero: String? = null,
    val fecha: String = "",
    @SerialName("zona_geografica") val zonaGeografica: String = "continental",
    @SerialName("tipo_propiedad") val tipoPropiedad: String? = null,
    val medidas: String? = null, // JSON string
    val subtotal: Double = 0.0,
    @SerialName("descuento_porcentaje") val descuentoPorcentaje: Double = 0.0,
    @SerialName("descuento_monto") val descuentoMonto: Double = 0.0,
    val total: Double = 0.0,
    @SerialName("total_areas") val totalAreas: Int = 0,
    @SerialName("areas_electricas") val areasElectricas: Int = 0,
    @SerialName("areas_manuales") val areasManuales: Int = 0,
    @SerialName("pdf_path") val pdfPath: String? = null,
    @SerialName("lead_id") val leadId: String? = null,
    val observaciones: String? = null,
    @SerialName("controles_adicionales") val controlesAdicionales: Int = 0,
    @SerialName("manivelas_adicionales") val manivelasAdicionales: Int = 0,
    @SerialName("costo_accesorios") val costoAccesorios: Double = 0.0,
    @SerialName("tipo_control") val tipoControl: String = "multicanal",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    /**
     * Convierte a modelo local
     */
    fun toCotizacionRainLocal(): CotizacionRain {
        val medidasList = try {
            if (medidas.isNullOrBlank()) {
                emptyList()
            } else {
                Json.decodeFromString<List<MedidaRainJson>>(medidas).map { it.toMedidaRain() }
            }
        } catch (e: Exception) {
            android.util.Log.e("CotizacionRainRemota", "Error parseando medidas: ${e.message}")
            emptyList()
        }

        return CotizacionRain(
            id = id ?: 0L,
            folio = folio,
            clienteNombre = clienteNombre,
            clienteTelefono = clienteTelefono ?: "",
            ubicacion = ubicacion ?: "",
            ciudad = ciudad ?: "",
            especialista = especialistaNombre,
            fecha = fecha,
            medidas = medidasList,
            zonaGeografica = ZonaGeografica.fromId(zonaGeografica),
            tipoPropiedad = tipoPropiedad ?: "",
            subtotal = subtotal,
            descuentoPorcentaje = descuentoPorcentaje,
            descuentoMonto = descuentoMonto,
            total = total,
            totalAreas = totalAreas,
            areasElectricas = areasElectricas,
            areasManuales = areasManuales,
            pdfPath = pdfPath,
            observaciones = observaciones ?: "",
            controlesAdicionales = controlesAdicionales,
            manivelasAdicionales = manivelasAdicionales,
            costoAccesorios = costoAccesorios,
            tipoControl = tipoControl,
            updatedAt = try {
                if (updatedAt != null && updatedAt != createdAt) {
                    java.time.Instant.parse(updatedAt).toEpochMilli()
                } else 0L
            } catch (e: Exception) { 0L }
        )
    }
}

@Serializable
data class CotizacionRainInsert(
    val folio: String,
    @SerialName("user_id") val userId: String,
    @SerialName("especialista_nombre") val especialistaNombre: String,
    @SerialName("cliente_nombre") val clienteNombre: String,
    @SerialName("cliente_telefono") val clienteTelefono: String? = null,
    val ubicacion: String? = null,
    val ciudad: String? = null,
    val colonia: String? = null,
    val calle: String? = null,
    val numero: String? = null,
    val fecha: String,
    @SerialName("zona_geografica") val zonaGeografica: String = "continental",
    @SerialName("tipo_propiedad") val tipoPropiedad: String? = null,
    val medidas: String, // JSON string
    val subtotal: Double = 0.0,
    @SerialName("descuento_porcentaje") val descuentoPorcentaje: Double = 0.0,
    @SerialName("descuento_monto") val descuentoMonto: Double = 0.0,
    val total: Double = 0.0,
    @SerialName("total_areas") val totalAreas: Int = 0,
    @SerialName("areas_electricas") val areasElectricas: Int = 0,
    @SerialName("areas_manuales") val areasManuales: Int = 0,
    @SerialName("pdf_path") val pdfPath: String? = null,
    @SerialName("lead_id") val leadId: String? = null,
    val observaciones: String? = null,
    @SerialName("controles_adicionales") val controlesAdicionales: Int = 0,
    @SerialName("manivelas_adicionales") val manivelasAdicionales: Int = 0,
    @SerialName("costo_accesorios") val costoAccesorios: Double = 0.0,
    @SerialName("tipo_control") val tipoControl: String = "multicanal"
)

@Serializable
data class CotizacionRainUpdate(
    @SerialName("cliente_nombre") val clienteNombre: String,
    @SerialName("cliente_telefono") val clienteTelefono: String? = null,
    val ubicacion: String? = null,
    val ciudad: String? = null,
    val colonia: String? = null,
    val medidas: String,
    val subtotal: Double = 0.0,
    @SerialName("descuento_porcentaje") val descuentoPorcentaje: Double = 0.0,
    @SerialName("descuento_monto") val descuentoMonto: Double = 0.0,
    val total: Double = 0.0,
    @SerialName("total_areas") val totalAreas: Int = 0,
    @SerialName("areas_electricas") val areasElectricas: Int = 0,
    @SerialName("areas_manuales") val areasManuales: Int = 0,
    val observaciones: String? = null,
    @SerialName("controles_adicionales") val controlesAdicionales: Int = 0,
    @SerialName("manivelas_adicionales") val manivelasAdicionales: Int = 0,
    @SerialName("costo_accesorios") val costoAccesorios: Double = 0.0,
    @SerialName("tipo_control") val tipoControl: String = "multicanal"
)

@Serializable
data class MedidaRainJson(
    val nombre: String,
    val alto: Double,
    val ancho: Double,
    val piezas: Int = 1,
    @SerialName("incluye_manual") val incluyeManual: Boolean = true,
    @SerialName("incluye_electrico") val incluyeElectrico: Boolean = false,
    @SerialName("subtotal_manual") val subtotalManual: Double = 0.0,
    @SerialName("subtotal_electrico") val subtotalElectrico: Double = 0.0,
    // Campos legacy para compatibilidad con cotizaciones antiguas
    @SerialName("tipo_mecanismo") val tipoMecanismo: String? = null,
    val subtotal: Double? = null
) {
    fun toMedidaRain(): MedidaRain {
        // Si tiene campos nuevos, usarlos
        // Si solo tiene campos legacy, convertir
        val usaManual = incluyeManual || tipoMecanismo == "manual"
        val usaElectrico = incluyeElectrico || tipoMecanismo == "electrico"

        val subManual = if (subtotalManual > 0) subtotalManual
        else if (tipoMecanismo == "manual" && subtotal != null) subtotal
        else 0.0

        val subElectrico = if (subtotalElectrico > 0) subtotalElectrico
        else if (tipoMecanismo == "electrico" && subtotal != null) subtotal
        else 0.0

        return MedidaRain(
            descripcion = nombre,
            alto = alto,
            ancho = ancho,
            piezas = piezas,
            incluyeManual = usaManual,
            incluyeElectrico = usaElectrico,
            subtotalManual = subManual,
            subtotalElectrico = subElectrico
        )
    }
}

@Serializable
data class FolioOnlyRain(val folio: String)