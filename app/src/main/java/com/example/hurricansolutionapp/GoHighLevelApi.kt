package com.example.hurricansolutionapp

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Cliente HTTP para GoHighLevel API
 *
 * Documentación oficial: https://highlevel.stoplight.io/docs/integrations/
 */
object GoHighLevelApi {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        engine {
            config {
                connectTimeout(ApiConfig.REQUEST_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
                readTimeout(ApiConfig.REQUEST_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
                writeTimeout(ApiConfig.REQUEST_TIMEOUT, java.util.concurrent.TimeUnit.MILLISECONDS)
            }
        }
    }

    // ===============================================================================
    // CONTACTS (Contactos) - ESTRATEGIA PRINCIPAL
    // ===============================================================================

    /**
     * Obtiene todos los contactos de la ubicación
     * Los contactos incluyen información de sus oportunidades asociadas
     *
     * @param limit Número máximo de resultados (default: 100)
     * @return Lista de contactos
     */
    suspend fun getContacts(
        limit: Int = ApiConfig.MAX_LEADS_PER_SYNC
    ): Result<GHLContactsResponse> {
        return try {
            log("Obteniendo contactos (limit=$limit)")

            val url = buildString {
                append("${ApiConfig.GHL_BASE_URL}/contacts/")
                append("?location_id=${ApiConfig.GHL_LOCATION_ID}")
                append("&limit=$limit")
            }

            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${ApiConfig.GHL_API_KEY}")
                }
            }

            if (response.status.isSuccess()) {
                val data = response.body<GHLContactsResponse>()
                log("[OK] Contactos obtenidos: ${data.contacts.size}")
                Result.success(data)
            } else {
                val error = "Error HTTP ${response.status.value}: ${response.bodyAsText()}"
                logError(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            logError("Error obteniendo contactos: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ===============================================================================
    // OPPORTUNITIES (Oportunidades) - NO DISPONIBLE EN v1
    // ===============================================================================

    /**
     * NOTA: El endpoint /opportunities/ NO existe en GoHighLevel API v1
     * Usar getContacts() en su lugar, que incluye datos de oportunidades
     */
    @Deprecated(
        message = "Endpoint no disponible en API v1. Usar getContacts() en su lugar",
        level = DeprecationLevel.ERROR
    )

    /**
     * Obtiene todas las oportunidades del pipeline
     *
     * @param limit Número máximo de resultados (default: 100)
     * @param pipelineId ID del pipeline (opcional, usa el de config si no se especifica)
     * @return Lista de oportunidades
     */
    suspend fun getOpportunities(
        limit: Int = ApiConfig.MAX_LEADS_PER_SYNC,
        pipelineId: String? = null
    ): Result<GHLOpportunitiesResponse> {
        return try {
            log("Obteniendo oportunidades (limit=$limit)")

            // Endpoint correcto para v1
            val url = buildString {
                append("${ApiConfig.GHL_BASE_URL}/opportunities/")
                append("?location_id=${ApiConfig.GHL_LOCATION_ID}")
                append("&pipeline_id=${pipelineId ?: ApiConfig.GHL_PIPELINE_ID}")
                append("&limit=$limit")
            }

            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${ApiConfig.GHL_API_KEY}")
                }
            }

            if (response.status.isSuccess()) {
                val data = response.body<GHLOpportunitiesResponse>()
                log("[OK] Oportunidades obtenidas: ${data.opportunities.size}")
                Result.success(data)
            } else {
                val error = "Error HTTP ${response.status.value}: ${response.bodyAsText()}"
                logError(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            logError("Error obteniendo oportunidades: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene la oportunidad asociada a un contacto específico
     * Necesario porque assignedTo viene en la oportunidad, no en el contacto
     *
     * Usa API v2 que tiene mejor soporte para búsquedas
     *
     * @param contactId ID del contacto
     * @return Oportunidad asociada o null si no existe
     */
    suspend fun getOpportunityByContactId(contactId: String): Result<GHLOpportunity?> {
        return try {
            log("Buscando oportunidad para contacto $contactId")

            // Intentar con API v2
            val urlV2 = "https://services.leadconnectorhq.com/opportunities/search"

            val requestBody = GHLSearchOpportunitiesRequestV2(
                locationId = ApiConfig.GHL_LOCATION_ID,
                contactId = contactId,
                limit = 1
            )

            val response: HttpResponse = client.post(urlV2) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${ApiConfig.GHL_API_KEY}")
                    append(HttpHeaders.ContentType, "application/json")
                    append("Version", "2021-07-28")
                }
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                val data = response.body<GHLOpportunitiesResponse>()
                val opportunity = data.opportunities.firstOrNull()

                if (opportunity != null) {
                    log("[OK] Oportunidad encontrada: ${opportunity.id} â†’ Asignado a: ${opportunity.assignedTo ?: "Sin asignar"}")
                } else {
                    log("âš ï¸ No hay oportunidad para contacto $contactId")
                }

                Result.success(opportunity)
            } else {
                val error = "Error HTTP ${response.status.value}: ${response.bodyAsText()}"
                logError(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            logError("Error obteniendo oportunidad para contacto $contactId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza una oportunidad (por ejemplo, cambiar el stage)
     *
     * @param opportunityId ID de la oportunidad
     * @param request Datos a actualizar
     */
    suspend fun updateOpportunity(
        opportunityId: String,
        request: GHLUpdateOpportunityRequest
    ): Result<Unit> {
        return try {
            log("Actualizando oportunidad $opportunityId")

            val url = "${ApiConfig.GHL_BASE_URL}/opportunities/$opportunityId"

            val response: HttpResponse = client.put(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${ApiConfig.GHL_API_KEY}")
                    append(HttpHeaders.ContentType, "application/json")
                }

                setBody(request)
            }

            if (response.status.isSuccess()) {
                log("[OK] Oportunidad actualizada correctamente")
                Result.success(Unit)
            } else {
                val error = "Error HTTP ${response.status.value}: ${response.bodyAsText()}"
                logError(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            logError("Error actualizando oportunidad: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ===============================================================================
    // CONTACTS (Contactos)
    // ===============================================================================

    /**
     * Obtiene un contacto por ID
     *
     * @param contactId ID del contacto
     * @return Datos del contacto
     */
    suspend fun getContact(contactId: String): Result<GHLContact> {
        return try {
            log("Obteniendo contacto $contactId")

            val url = "${ApiConfig.GHL_BASE_URL}/contacts/$contactId"

            val response: HttpResponse = client.get(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${ApiConfig.GHL_API_KEY}")
                }
            }

            if (response.status.isSuccess()) {
                val contact = response.body<GHLContact>()
                log("[OK] Contacto obtenido: ${contact.getFullName()}")
                Result.success(contact)
            } else {
                val error = "Error HTTP ${response.status.value}: ${response.bodyAsText()}"
                logError(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            logError("Error obteniendo contacto: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza un contacto
     *
     * @param contactId ID del contacto
     * @param request Datos a actualizar
     */
    suspend fun updateContact(
        contactId: String,
        request: GHLUpdateContactRequest
    ): Result<Unit> {
        return try {
            log("Actualizando contacto $contactId")

            val url = "${ApiConfig.GHL_BASE_URL}/contacts/$contactId"

            val response: HttpResponse = client.put(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${ApiConfig.GHL_API_KEY}")
                    append(HttpHeaders.ContentType, "application/json")
                }

                setBody(request)
            }

            if (response.status.isSuccess()) {
                log("[OK] Contacto actualizado correctamente")
                Result.success(Unit)
            } else {
                val error = "Error HTTP ${response.status.value}: ${response.bodyAsText()}"
                logError(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            logError("Error actualizando contacto: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Agrega un tag a un contacto
     *
     * @param contactId ID del contacto
     * @param tag Tag a agregar (ej: "Cotizado")
     */
    suspend fun addTagToContact(contactId: String, tag: String): Result<Unit> {
        return try {
            log("Agregando tag '$tag' al contacto $contactId")

            // Primero obtenemos los tags actuales
            val contactResult = getContact(contactId)
            if (contactResult.isFailure) {
                return Result.failure(contactResult.exceptionOrNull() ?: Exception("Error obteniendo contacto"))
            }

            val contact = contactResult.getOrNull()!!
            val currentTags = contact.tags ?: emptyList()

            // Si ya tiene el tag, no hacemos nada
            if (currentTags.contains(tag)) {
                log("â„¹ï¸ El contacto ya tiene el tag '$tag'")
                return Result.success(Unit)
            }

            // Agregamos el nuevo tag
            val newTags = currentTags + tag

            return updateContact(
                contactId = contactId,
                request = GHLUpdateContactRequest(tags = newTags)
            )

        } catch (e: Exception) {
            logError("Error agregando tag: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ===============================================================================
    // NOTES (Notas)
    // ===============================================================================

    /**
     * Agrega una nota a una oportunidad
     *
     * @param opportunityId ID de la oportunidad
     * @param note Texto de la nota
     */
    suspend fun addNoteToOpportunity(opportunityId: String, note: String): Result<Unit> {
        return try {
            log("Agregando nota a oportunidad $opportunityId")

            val url = "${ApiConfig.GHL_BASE_URL}/notes/"

            val response: HttpResponse = client.post(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${ApiConfig.GHL_API_KEY}")
                    append(HttpHeaders.ContentType, "application/json")
                }

                setBody(GHLAddNoteRequest(
                    body = note,
                    opportunityId = opportunityId
                ))
            }

            if (response.status.isSuccess()) {
                log("[OK] Nota agregada correctamente")
                Result.success(Unit)
            } else {
                val error = "Error HTTP ${response.status.value}: ${response.bodyAsText()}"
                logError(error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            logError("Error agregando nota: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ===============================================================================
    // UTILITIES
    // ===============================================================================

    /**
     * Cierra el cliente HTTP (llamar al cerrar la app)
     */
    fun close() {
        client.close()
    }

    /**
     * Logging simplificado
     */
    private fun log(message: String) {
        if (ApiConfig.ENABLE_DETAILED_LOGS) {
            android.util.Log.d(ApiConfig.LOG_TAG_GHL, message)
        }
    }

    private fun logError(message: String, exception: Throwable? = null) {
        android.util.Log.e(ApiConfig.LOG_TAG_GHL, message, exception)
    }
}