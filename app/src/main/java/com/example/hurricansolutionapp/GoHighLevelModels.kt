package com.example.hurricansolutionapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════════
// RESPONSE MODELS - GoHighLevel API
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Respuesta de lista de contactos
 */
@Serializable
data class GHLContactsResponse(
    val contacts: List<GHLContact> = emptyList(),
    val total: Int = 0
)

/**
 * Respuesta de lista de oportunidades
 */
@Serializable
data class GHLOpportunitiesResponse(
    val opportunities: List<GHLOpportunity> = emptyList(),
    val total: Int = 0
)

/**
 * Modelo de Oportunidad en GoHighLevel
 */
@Serializable
data class GHLOpportunity(
    val id: String,
    val name: String? = null,

    @SerialName("contact_id")
    val contactId: String? = null,

    @SerialName("pipeline_id")
    val pipelineId: String? = null,

    @SerialName("pipeline_stage_id")
    val pipelineStageId: String? = null,

    @SerialName("assigned_to")
    val assignedTo: String? = null,

    val status: String? = null,

    @SerialName("monetary_value")
    val monetaryValue: Double? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Modelo de campo personalizado en GoHighLevel
 */
@Serializable
data class GHLCustomField(
    val id: String,
    val value: String? = null
)

/**
 * Modelo de Contacto en GoHighLevel
 */
@Serializable
data class GHLContact(
    val id: String,

    val locationId: String? = null,

    val firstName: String? = null,

    val lastName: String? = null,

    val contactName: String? = null,

    val name: String? = null,

    val companyName: String? = null,

    val email: String? = null,

    val phone: String? = null,

    val dnd: Boolean? = null,

    val type: String? = null,

    val source: String? = null,

    val assignedTo: String? = null,

    val address1: String? = null,

    val city: String? = null,

    val state: String? = null,

    val country: String? = null,

    val postalCode: String? = null,

    val dateAdded: String? = null,

    val dateUpdated: String? = null,

    val dateOfBirth: String? = null,

    val website: String? = null,

    val timezone: String? = null,

    val lastActivity: Long? = null,

    // Opportunity data (viene incluido en el contacto)
    @SerialName("opportunity_id")
    val opportunityId: String? = null,

    @SerialName("pipeline_id")
    val pipelineId: String? = null,

    @SerialName("pipeline_stage_id")
    val pipelineStageId: String? = null,

    // Custom fields como array dinámico (viene como lista de objetos)
    val customField: List<GHLCustomField>? = null,

    val tags: List<String>? = null,

    @SerialName("created_at")
    val createdAt: String? = null
) {
    /**
     * Retorna el nombre completo del contacto
     */
    fun getFullName(): String {
        return when {
            !contactName.isNullOrBlank() -> contactName
            !name.isNullOrBlank() -> name
            !firstName.isNullOrBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName"
            !firstName.isNullOrBlank() -> firstName
            !lastName.isNullOrBlank() -> lastName
            else -> "Sin nombre"
        }
    }

    /**
     * Extrae campo personalizado por ID
     */
    fun getCustomField(fieldId: String): String? {
        return customField?.find { it.id == fieldId }?.value
    }

    /**
     * Obtiene custom field por nombre común (busca en los IDs conocidos)
     */
    fun getCustomFieldByName(fieldName: String): String? {
        // Mapeo de nombres comunes a IDs conocidos
        // Estos IDs pueden variar por cuenta, ajustar según sea necesario
        val fieldMapping = mapOf(
            "ciudad" to listOf("ciudad", "city", "Ciudad"),
            "colonia" to listOf("colonia", "Colonia"),
            "calle" to listOf("calle", "street", "Calle"),
            "numero" to listOf("numero", "number", "Numero", "Número")
        )

        val possibleIds = fieldMapping[fieldName.lowercase()] ?: listOf(fieldName)

        return customField?.firstOrNull { field ->
            possibleIds.any { id -> field.id.contains(id, ignoreCase = true) }
        }?.value
    }
}

/**
 * Modelo de Pipeline
 */
@Serializable
data class GHLPipeline(
    val id: String,
    val name: String,
    val stages: List<GHLStage> = emptyList()
)

/**
 * Modelo de Stage (etapa) del pipeline
 */
@Serializable
data class GHLStage(
    val id: String,
    val name: String,
    val position: Int? = null
)

/**
 * Modelo de Usuario (especialista asignado)
 */
@Serializable
data class GHLUser(
    val id: String,
    val name: String,
    val email: String? = null,
    val role: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// REQUEST MODELS - Para enviar datos a GoHighLevel
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Request para buscar oportunidades (API v2)
 */
@Serializable
data class GHLSearchOpportunitiesRequestV2(
    @SerialName("location_id")
    val locationId: String,

    @SerialName("contact_id")
    val contactId: String,

    val limit: Int = 1
)

/**
 * Request para buscar oportunidades
 */
@Serializable
data class GHLSearchOpportunitiesRequest(
    @SerialName("location_id")
    val locationId: String,

    @SerialName("pipeline_id")
    val pipelineId: String,

    val limit: Int = 100
)

// ═══════════════════════════════════════════════════════════════════════════════
// REQUEST MODELS - Para actualizar datos en GoHighLevel
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Request para actualizar una oportunidad
 */
@Serializable
data class GHLUpdateOpportunityRequest(
    @SerialName("pipeline_stage_id")
    val pipelineStageId: String? = null,

    val status: String? = null,

    @SerialName("monetary_value")
    val monetaryValue: Double? = null,

    val name: String? = null
)

/**
 * Request para actualizar un contacto
 */
@Serializable
data class GHLUpdateContactRequest(
    @SerialName("first_name")
    val firstName: String? = null,

    @SerialName("last_name")
    val lastName: String? = null,

    val email: String? = null,

    val phone: String? = null,

    val tags: List<String>? = null,

    @SerialName("custom_fields")
    val customFields: Map<String, String>? = null
)

/**
 * Request para agregar una nota a un contacto u oportunidad
 */
@Serializable
data class GHLAddNoteRequest(
    val body: String,

    @SerialName("contact_id")
    val contactId: String? = null,

    @SerialName("opportunity_id")
    val opportunityId: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// MAPPERS - Conversión entre modelos GHL y modelos de la app
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Convierte un contacto de GHL a un Lead de la app
 */
fun ghlContactToLead(contact: GHLContact): LeadInsert {
    return LeadInsert(
        id = contact.phone ?: contact.id, // Usar teléfono como ID primario
        ghlContactId = contact.id,
        ghlOpportunityId = contact.opportunityId,
        nombreCompleto = contact.getFullName(),
        telefono = contact.phone ?: "",
        email = contact.email,
        ciudad = contact.getCustomFieldByName("ciudad") ?: contact.city,
        colonia = contact.getCustomFieldByName("colonia"),
        calle = contact.getCustomFieldByName("calle"),
        numero = contact.getCustomFieldByName("numero"),
        direccionCompleta = contact.address1,
        assignedToUserId = null, // Se mapea después con la tabla de usuarios
        assignedToName = null, // Los contactos no tienen assigned_to directamente
        pipelineStage = getStageName(contact.pipelineStageId)
    )
}

/**
 * Convierte una oportunidad + contacto de GHL a un Lead de la app
 * (Mantenido por compatibilidad)
 */
fun ghlToLead(opportunity: GHLOpportunity, contact: GHLContact): LeadInsert {
    return LeadInsert(
        id = contact.phone ?: contact.id, // Usar teléfono como ID primario
        ghlContactId = contact.id,
        ghlOpportunityId = opportunity.id,
        nombreCompleto = contact.getFullName(),
        telefono = contact.phone ?: "",
        email = contact.email,
        ciudad = contact.getCustomFieldByName("ciudad") ?: contact.city,
        colonia = contact.getCustomFieldByName("colonia"),
        calle = contact.getCustomFieldByName("calle"),
        numero = contact.getCustomFieldByName("numero"),
        direccionCompleta = contact.address1,
        assignedToUserId = null, // Se mapea después con la tabla de usuarios
        assignedToName = opportunity.assignedTo,
        pipelineStage = getStageName(opportunity.pipelineStageId)
    )
}

/**
 * Mapea el ID del stage a su nombre legible
 * IMPORTANTE: Estos IDs deben coincidir con los de ApiConfig.GHLStages
 */
fun getStageName(stageId: String?): String {
    if (stageId.isNullOrBlank()) return "Leads Nuevos"

    return when (stageId) {
        ApiConfig.GHLStages.LEADS_NUEVOS -> "Leads Nuevos"
        ApiConfig.GHLStages.MEDIDAS -> "Medidas"
        ApiConfig.GHLStages.SEGUIMIENTO_MEDIDAS -> "Seguimiento Medidas"
        ApiConfig.GHLStages.ASIGNACION -> "Asignación"
        ApiConfig.GHLStages.CITAS -> "Citas"
        ApiConfig.GHLStages.SEGUIMIENTO_CITAS -> "Seguimiento Citas"
        ApiConfig.GHLStages.PROYECTO_COTIZADO -> "Proyecto Cotizado"
        ApiConfig.GHLStages.SEGUIMIENTO_PROYECTO -> "Seguimiento Proyecto Cotizado"
        else -> "Desconocido"
    }
}

/**
 * Mapea el nombre del stage a su ID
 */
fun getStageId(stageName: String): String {
    return when (stageName) {
        "Leads Nuevos" -> ApiConfig.GHLStages.LEADS_NUEVOS
        "Medidas" -> ApiConfig.GHLStages.MEDIDAS
        "Seguimiento Medidas" -> ApiConfig.GHLStages.SEGUIMIENTO_MEDIDAS
        "Asignación" -> ApiConfig.GHLStages.ASIGNACION
        "Citas" -> ApiConfig.GHLStages.CITAS
        "Seguimiento Citas" -> ApiConfig.GHLStages.SEGUIMIENTO_CITAS
        "Proyecto Cotizado" -> ApiConfig.GHLStages.PROYECTO_COTIZADO
        "Seguimiento Proyecto Cotizado" -> ApiConfig.GHLStages.SEGUIMIENTO_PROYECTO
        else -> ApiConfig.GHLStages.LEADS_NUEVOS
    }
}