package com.example.hurricansolutionapp

import android.content.Context
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para gestión de Leads desde GoHighLevel CRM
 */
object LeadsRepository {

    // ═══════════════════════════════════════════════════════════════════════════════
    // OBTENER LEADS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene TODOS los leads (para ADMIN y para mostrar lista completa)
     */
    suspend fun getAllLeads(): List<Lead> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                android.util.Log.d("LeadsRepository", "Obteniendo todos los leads...")

                val result = client.from("leads")
                    .select()
                    .decodeList<Lead>()

                android.util.Log.d("LeadsRepository", "Leads obtenidos: ${result.size}")
                result
            } catch (e: Exception) {
                android.util.Log.e("LeadsRepository", "Error obteniendo leads: ${e.message}", e)
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Obtiene solo los leads asignados a un especialista específico
     */
    suspend fun getLeadsForUser(userId: String): List<Lead> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                android.util.Log.d("LeadsRepository", "Obteniendo leads para usuario: $userId")

                val result = client.from("leads")
                    .select {
                        filter {
                            eq("assigned_to_user_id", userId)
                        }
                    }
                    .decodeList<Lead>()

                android.util.Log.d("LeadsRepository", "Leads asignados obtenidos: ${result.size}")
                result
            } catch (e: Exception) {
                android.util.Log.e("LeadsRepository", "Error obteniendo leads del usuario: ${e.message}", e)
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Busca un lead por teléfono
     */
    suspend fun getLeadByPhone(phone: String): Lead? {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                val result = client.from("leads")
                    .select {
                        filter {
                            eq("telefono", phone)
                        }
                    }
                    .decodeSingleOrNull<Lead>()

                result
            } catch (e: Exception) {
                android.util.Log.e("LeadsRepository", "Error buscando lead por teléfono: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Busca un lead por ID
     */
    suspend fun getLeadById(leadId: String): Lead? {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                val result = client.from("leads")
                    .select {
                        filter {
                            eq("id", leadId)
                        }
                    }
                    .decodeSingleOrNull<Lead>()

                result
            } catch (e: Exception) {
                android.util.Log.e("LeadsRepository", "Error obteniendo lead por ID: ${e.message}", e)
                null
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // SINCRONIZACIÓN
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Fuerza la sincronización de leads desde Make.com
     * Esta función llama al webhook de Make que trae los leads desde GoHighLevel
     *
     * @param context Contexto de la aplicación
     * @return Result con éxito o error
     */
    suspend fun syncLeadsFromCRM(context: Context): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("LeadsRepository", "Sincronización desde GoHighLevel...")

                // ✅ Llamar directamente a GoHighLevel (sin Make.com)
                val result = GoHighLevelRepository.syncLeadsFromGHL(context)

                if (result.isSuccess) {
                    val syncResult = result.getOrNull()!!
                    Result.success(syncResult.message)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Error desconocido"))
                }

            } catch (e: Exception) {
                android.util.Log.e("LeadsRepository", "Error sincronizando: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ACTUALIZAR PIPELINE STAGE
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Actualiza el pipeline stage de un lead a "Seguimiento Medidas"
     * Esto se llama cuando se genera una cotización desde un lead
     */
    suspend fun updateLeadPipelineStage(
        leadId: String,
        newStage: String = "Seguimiento Medidas"
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                android.util.Log.d("LeadsRepository", "Actualizando pipeline stage del lead $leadId a $newStage")

                client.from("leads")
                    .update(LeadPipelineUpdate(pipelineStage = newStage)) {
                        filter {
                            eq("id", leadId)
                        }
                    }

                android.util.Log.d("LeadsRepository", "Pipeline actualizado correctamente")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("LeadsRepository", "Error actualizando pipeline: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // INSERTAR/ACTUALIZAR LEADS (usado por Make.com principalmente)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Inserta o actualiza un lead en la base de datos
     * Esta función es principalmente usada por Make.com al sincronizar
     */
    suspend fun upsertLead(lead: LeadInsert): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                // Intentar insertar
                try {
                    client.from("leads")
                        .insert(lead)

                    android.util.Log.d("LeadsRepository", "Lead insertado: ${lead.nombreCompleto}")
                } catch (insertError: Exception) {
                    // Si falla (ya existe), actualizar
                    client.from("leads")
                        .update(lead) {
                            filter {
                                eq("id", lead.id)
                            }
                        }

                    android.util.Log.d("LeadsRepository", "Lead actualizado: ${lead.nombreCompleto}")
                }

                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("LeadsRepository", "Error en upsert de lead: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // BÚSQUEDA Y FILTRADO
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Busca leads por nombre (case insensitive)
     */
    fun filterLeadsByName(leads: List<Lead>, query: String): List<Lead> {
        if (query.isBlank()) return leads

        val lowerQuery = query.lowercase()
        return leads.filter { lead ->
            lead.nombreCompleto.lowercase().contains(lowerQuery) ||
                    lead.telefono.contains(query) ||
                    lead.email?.lowercase()?.contains(lowerQuery) == true
        }
    }

    /**
     * Filtra leads por especialista asignado
     */
    fun filterLeadsByAssignedUser(leads: List<Lead>, userId: String): List<Lead> {
        return leads.filter { it.assignedToUserId == userId }
    }

    /**
     * Filtra leads por pipeline stage
     */
    fun filterLeadsByPipelineStage(leads: List<Lead>, stage: String): List<Lead> {
        return leads.filter { it.pipelineStage == stage }
    }
    // ═══════════════════════════════════════════════════════════════════════════════
// FUNCIONES DE ASIGNACIÓN DE LEADS (ADMIN)
// ═══════════════════════════════════════════════════════════════════════════════
// AGREGAR ESTAS FUNCIONES AL FINAL DE LeadsRepository.kt

    /**
     * Asigna un lead a un especialista
     *
     * @param leadId ID del lead
     * @param userId ID del especialista
     * @param userName Nombre del especialista
     * @return Result con éxito o error
     */
    suspend fun assignLeadToUser(
        leadId: String,
        userId: String,
        userName: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                android.util.Log.d("LeadsRepository", "Asignando lead $leadId a $userName")

                client.from("leads")
                    .update(
                        mapOf(
                            "assigned_to_user_id" to userId,
                            "assigned_to_name" to userName,

                        )
                    ) {
                        filter {
                            eq("id", leadId)
                        }
                    }

                android.util.Log.d("LeadsRepository", "Lead asignado correctamente")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("LeadsRepository", "Error asignando lead: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Desasigna un lead (lo deja sin asignar)
     *
     * @param leadId ID del lead
     * @return Result con éxito o error
     */
    suspend fun unassignLead(leadId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                android.util.Log.d("LeadsRepository", "Desasignando lead $leadId")

                client.from("leads")
                    .update(
                        mapOf(
                            "assigned_to_user_id" to null,
                            "assigned_to_name" to null
                        )
                    ) {
                        filter {
                            eq("id", leadId)
                        }
                    }

                android.util.Log.d("LeadsRepository", "Lead desasignado correctamente")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("LeadsRepository", "Error desasignando lead: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene leads sin asignar
     *
     * @return Lista de leads sin especialista asignado
     */
    suspend fun getUnassignedLeads(): List<Lead> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                val result = client.from("leads")
                    .select()
                    .decodeList<Lead>()
                    .filter { it.assignedToUserId == null } // Filtrar en cliente

                android.util.Log.d("LeadsRepository", "Leads sin asignar: ${result.size}")
                result
            } catch (e: Exception) {
                android.util.Log.e("LeadsRepository", "Error obteniendo leads sin asignar: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Obtiene leads ya asignados (a cualquier usuario)
     *
     * @return Lista de leads con especialista asignado
     */
    suspend fun getAssignedLeads(): List<Lead> {
        return withContext(Dispatchers.IO) {
            try {
                val allLeads = getAllLeads()
                allLeads.filter { it.assignedToUserId != null }
            } catch (e: Exception) {
                android.util.Log.e("LeadsRepository", "Error obteniendo leads asignados: ${e.message}", e)
                emptyList()
            }
        }
    }
}