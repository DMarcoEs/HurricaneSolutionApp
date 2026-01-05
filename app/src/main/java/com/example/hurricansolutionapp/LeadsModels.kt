package com.example.hurricansolutionapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════════
// MODELO DE LEAD DESDE SUPABASE
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class Lead(
    val id: String,                              // ID único (teléfono)

    @SerialName("ghl_contact_id")
    val ghlContactId: String? = null,

    @SerialName("ghl_opportunity_id")
    val ghlOpportunityId: String? = null,

    // Información del cliente
    @SerialName("nombre_completo")
    val nombreCompleto: String,

    val telefono: String,
    val email: String? = null,

    // Dirección
    val ciudad: String? = null,
    val colonia: String? = null,
    val calle: String? = null,
    val numero: String? = null,

    @SerialName("direccion_completa")
    val direccionCompleta: String? = null,

    // Asignación
    @SerialName("assigned_to_user_id")
    val assignedToUserId: String? = null,

    @SerialName("assigned_to_name")
    val assignedToName: String? = null,

    @SerialName("pipeline_stage")
    val pipelineStage: String? = "Leads Nuevos",

    // Metadata
    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    @SerialName("synced_at")
    val syncedAt: String? = null
) {
    /**
     * Retorna la dirección completa formateada
     */
    fun getDireccionFormateada(): String {
        val partes = mutableListOf<String>()

        if (!calle.isNullOrBlank()) partes.add(calle)
        if (!numero.isNullOrBlank()) partes.add(numero)
        if (!colonia.isNullOrBlank()) partes.add(colonia)

        return if (partes.isNotEmpty()) {
            partes.joinToString(", ")
        } else {
            direccionCompleta ?: ""
        }
    }

    /**
     * Retorna ciudad formateada
     */
    fun getCiudadFormateada(): String {
        return ciudad ?: ""
    }

    /**
     * Verifica si el lead está asignado a un usuario específico
     */
    fun isAssignedTo(userId: String): Boolean {
        return assignedToUserId == userId
    }

    /**
     * Retorna las iniciales del nombre
     */
    fun getInitials(): String {
        val palabras = nombreCompleto.trim().split(" ")
        return when {
            palabras.size >= 2 -> "${palabras[0].first().uppercase()}${palabras[1].first().uppercase()}"
            palabras.size == 1 -> palabras[0].take(2).uppercase()
            else -> "??"
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODELO PARA INSERTAR/ACTUALIZAR LEADS (desde Make.com o app)
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class LeadInsert(
    val id: String,                              // Teléfono como ID

    @SerialName("ghl_contact_id")
    val ghlContactId: String? = null,

    @SerialName("ghl_opportunity_id")
    val ghlOpportunityId: String? = null,

    @SerialName("nombre_completo")
    val nombreCompleto: String,

    val telefono: String,
    val email: String? = null,

    val ciudad: String? = null,
    val colonia: String? = null,
    val calle: String? = null,
    val numero: String? = null,

    @SerialName("direccion_completa")
    val direccionCompleta: String? = null,

    @SerialName("assigned_to_user_id")
    val assignedToUserId: String? = null,

    @SerialName("assigned_to_name")
    val assignedToName: String? = null,

    @SerialName("pipeline_stage")
    val pipelineStage: String? = "Leads Nuevos"
)

// ═══════════════════════════════════════════════════════════════════════════════
// MODELO PARA ACTUALIZAR SOLO EL PIPELINE STAGE
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class LeadPipelineUpdate(
    @SerialName("pipeline_stage")
    val pipelineStage: String
)

// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSIONES PARA CONVERTIR LEAD A COTIZACION DRAFT
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Convierte un Lead a CotizacionDraft, pre-llenando los datos del cliente
 */
fun Lead.toCotizacionDraft(): CotizacionDraft {
    return CotizacionDraft(
        nombre = this.nombreCompleto,
        telefono = this.telefono,
        ciudad = this.ciudad ?: "",
        colonia = this.colonia ?: "",
        direccionDetalle = "${this.calle ?: ""} ${this.numero ?: ""}".trim()
    )
}