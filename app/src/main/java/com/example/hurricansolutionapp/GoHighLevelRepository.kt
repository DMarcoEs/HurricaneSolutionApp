package com.example.hurricansolutionapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para operaciones con GoHighLevel CRM
 *
 * Este repositorio maneja:
 * - Sincronización de leads desde GoHighLevel → Supabase
 * - Actualización de pipeline stages cuando se genera una cotización
 * - Agregar tags y notas a contactos/oportunidades
 */
object GoHighLevelRepository {

    // ═══════════════════════════════════════════════════════════════════════════════
    // SINCRONIZACIÓN DE LEADS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Sincroniza leads desde GoHighLevel a Supabase
     *
     * Flujo:
     * 1. Obtiene contactos de la ubicación (incluyen datos de oportunidades)
     * 2. Mapea cada contacto a LeadInsert
     * 3. Hace upsert en Supabase (tabla leads)
     *
     * @param context Contexto de la aplicación
     * @return Result con número de leads sincronizados o error
     */
    suspend fun syncLeadsFromGHL(context: Context): Result<SyncResult> {
        return withContext(Dispatchers.IO) {
            try {
                if (!ApiConfig.GHL_ENABLED) {
                    return@withContext Result.failure(
                        Exception("Integración con GoHighLevel deshabilitada")
                    )
                }

                log("Iniciando sincronización de leads...")

                // 1. Obtener contactos (incluyen información de oportunidades)
                val contactsResult = GoHighLevelApi.getContacts()

                if (contactsResult.isFailure) {
                    return@withContext Result.failure(
                        contactsResult.exceptionOrNull() ?: Exception("Error obteniendo contactos")
                    )
                }

                val contacts = contactsResult.getOrNull()?.contacts ?: emptyList()
                log("Contactos obtenidos: ${contacts.size}")

                if (contacts.isEmpty()) {
                    return@withContext Result.success(
                        SyncResult(
                            total = 0,
                            synced = 0,
                            errors = 0,
                            message = "No hay contactos en GoHighLevel"
                        )
                    )
                }

                // 2. Para cada contacto, sincronizar
                var syncedCount = 0
                var errorCount = 0

                for (contact in contacts) {
                    try {
                        // Validar que tenga teléfono (requerido como ID)
                        if (contact.phone.isNullOrBlank()) {
                            log("⚠️ Contacto ${contact.id} sin teléfono - skip")
                            errorCount++
                            continue
                        }

                        // ⚠️ TEMPORAL: Búsqueda de oportunidades deshabilitada
                        // La API v2 requiere autenticación diferente que investigaremos
                        // Por ahora, asignar leads manualmente desde Supabase

                        // TODO: Habilitar cuando tengamos acceso a API v2
                        // val opportunityResult = GoHighLevelApi.getOpportunityByContactId(contact.id)

                        var assignedToName: String? = null
                        var assignedToUserId: String? = null

                        log("⚠️ Asignación automática deshabilitada - asignar manualmente desde Supabase")

                        // Mapear a LeadInsert
                        val leadInsert = ghlContactToLead(contact).copy(
                            assignedToUserId = assignedToUserId,
                            assignedToName = assignedToName
                        )

                        // Upsert en Supabase
                        val upsertResult = LeadsRepository.upsertLead(leadInsert)

                        if (upsertResult.isSuccess) {
                            syncedCount++
                            log("✅ Lead sincronizado: ${contact.getFullName()} (${contact.phone}) → Asignar manualmente")
                        } else {
                            errorCount++
                            log("❌ Error sincronizando lead: ${upsertResult.exceptionOrNull()?.message}")
                        }

                    } catch (e: Exception) {
                        errorCount++
                        log("❌ Error procesando contacto ${contact.id}: ${e.message}")
                    }
                }

                val result = SyncResult(
                    total = contacts.size,
                    synced = syncedCount,
                    errors = errorCount,
                    message = "Sincronizados: $syncedCount/${contacts.size} (Errores: $errorCount)"
                )

                log("✅ Sincronización completada: ${result.message}")

                Result.success(result)

            } catch (e: Exception) {
                logError("Error en sincronización: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ACTUALIZACIÓN DE PIPELINE
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Actualiza el pipeline stage de una oportunidad a "Seguimiento Medidas"
     * Se llama cuando se genera una cotización desde un Cliente Actual
     *
     * @param ghlOpportunityId ID de la oportunidad en GoHighLevel
     * @param newStage Nombre del nuevo stage (default: "Seguimiento Medidas")
     * @return Result con éxito o error
     */
    suspend fun updateOpportunityStage(
        ghlOpportunityId: String,
        newStage: String = "Seguimiento Medidas"
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!ApiConfig.GHL_ENABLED) {
                    log("⚠️ GoHighLevel deshabilitado - skip actualización de stage")
                    return@withContext Result.success(Unit)
                }

                log("Actualizando stage de oportunidad $ghlOpportunityId a '$newStage'")

                val stageId = getStageId(newStage)

                val request = GHLUpdateOpportunityRequest(
                    pipelineStageId = stageId
                )

                val result = GoHighLevelApi.updateOpportunity(ghlOpportunityId, request)

                if (result.isSuccess) {
                    log("✅ Stage actualizado correctamente")

                    // También actualizar en Supabase
                    // (esto se hace en paralelo en AutoUploadManager)
                }

                result

            } catch (e: Exception) {
                logError("Error actualizando stage: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Agrega el tag "Cotizado" al contacto cuando se genera una cotización
     *
     * @param ghlContactId ID del contacto en GoHighLevel
     * @return Result con éxito o error
     */
    suspend fun tagContactAsCotizado(ghlContactId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!ApiConfig.GHL_ENABLED) {
                    log("⚠️ GoHighLevel deshabilitado - skip tag")
                    return@withContext Result.success(Unit)
                }

                GoHighLevelApi.addTagToContact(ghlContactId, "Cotizado")

            } catch (e: Exception) {
                logError("Error agregando tag: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Agrega una nota a la oportunidad con detalles de la cotización generada
     *
     * @param ghlOpportunityId ID de la oportunidad
     * @param folio Folio de la cotización
     * @param total Total de la cotización
     * @param pdfUrl URL del PDF en Supabase (opcional)
     * @return Result con éxito o error
     */
    suspend fun addCotizacionNote(
        ghlOpportunityId: String,
        folio: String,
        total: Double,
        pdfUrl: String? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!ApiConfig.GHL_ENABLED) {
                    log("⚠️ GoHighLevel deshabilitado - skip nota")
                    return@withContext Result.success(Unit)
                }

                val note = buildString {
                    appendLine("📄 Cotización generada")
                    appendLine("Folio: $folio")
                    appendLine("Total: $${String.format("%,.2f", total)}")
                    if (!pdfUrl.isNullOrBlank()) {
                        appendLine("PDF: $pdfUrl")
                    }
                    appendLine("Generado desde: Hurricane Solution App")
                }

                GoHighLevelApi.addNoteToOpportunity(ghlOpportunityId, note)

            } catch (e: Exception) {
                logError("Error agregando nota: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ACTUALIZACIÓN COMPLETA DESPUÉS DE GENERAR COTIZACIÓN
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Ejecuta todas las actualizaciones necesarias en GoHighLevel
     * cuando se genera una cotización
     *
     * @param ghlOpportunityId ID de la oportunidad
     * @param ghlContactId ID del contacto
     * @param folio Folio de la cotización
     * @param total Total de la cotización
     * @param pdfUrl URL del PDF (opcional)
     * @return Result con éxito o error
     */
    suspend fun onCotizacionGenerated(
        ghlOpportunityId: String,
        ghlContactId: String,
        folio: String,
        total: Double,
        pdfUrl: String? = null
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                log("Procesando actualizaciones en GHL para cotización $folio")

                // 1. Actualizar stage
                updateOpportunityStage(ghlOpportunityId, "Seguimiento Medidas")

                // 2. Agregar tag "Cotizado"
                tagContactAsCotizado(ghlContactId)

                // 3. Agregar nota con detalles
                addCotizacionNote(ghlOpportunityId, folio, total, pdfUrl)

                log("✅ Actualizaciones en GHL completadas")
                Result.success(Unit)

            } catch (e: Exception) {
                logError("Error en actualizaciones GHL: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════════════════

    private fun log(message: String) {
        if (ApiConfig.ENABLE_DETAILED_LOGS) {
            android.util.Log.d(ApiConfig.LOG_TAG_GHL, message)
        }
    }

    private fun logError(message: String, exception: Throwable? = null) {
        android.util.Log.e(ApiConfig.LOG_TAG_GHL, message, exception)
    }
}

/**
 * Resultado de sincronización de leads
 */
data class SyncResult(
    val total: Int,
    val synced: Int,
    val errors: Int,
    val message: String
)