package com.example.hurricansolutionapp

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para gestionar el mapeo entre nombres de GoHighLevel y usuarios de Supabase
 */
object UserGHLMappingRepository {

    /**
     * Obtiene el UUID de un usuario de Supabase a partir de su nombre en GoHighLevel
     *
     * @param ghlName Nombre exacto como aparece en GoHighLevel (ej: "Marco Canche")
     * @return UUID del usuario en Supabase, o null si no existe mapeo
     */
    suspend fun getUserIdByGHLName(ghlName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                val result = client.from("user_ghl_mapping")
                    .select {
                        filter {
                            eq("ghl_name", ghlName)
                        }
                    }
                    .decodeSingleOrNull<UserGHLMapping>()

                result?.userId

            } catch (e: Exception) {
                android.util.Log.e("UserGHLMapping", "Error obteniendo mapping para '$ghlName': ${e.message}")
                null
            }
        }
    }

    /**
     * Obtiene todos los mapeos de un usuario específico
     *
     * @param userId UUID del usuario en Supabase
     * @return Lista de nombres de GoHighLevel asociados a este usuario
     */
    suspend fun getGHLNamesByUserId(userId: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                val results = client.from("user_ghl_mapping")
                    .select {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<UserGHLMapping>()

                results.map { it.ghlName }

            } catch (e: Exception) {
                android.util.Log.e("UserGHLMapping", "Error obteniendo nombres GHL para usuario $userId: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Obtiene todos los mapeos existentes
     *
     * @return Lista de todos los mapeos
     */
    suspend fun getAllMappings(): List<UserGHLMapping> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                client.from("user_ghl_mapping")
                    .select()
                    .decodeList<UserGHLMapping>()

            } catch (e: Exception) {
                android.util.Log.e("UserGHLMapping", "Error obteniendo todos los mapeos: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Crea un nuevo mapeo (solo ADMIN)
     *
     * @param userId UUID del usuario en Supabase
     * @param ghlName Nombre como aparece en GoHighLevel
     * @return Result con éxito o error
     */
    suspend fun createMapping(userId: String, ghlName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                val mapping = UserGHLMappingInsert(
                    userId = userId,
                    ghlName = ghlName
                )

                client.from("user_ghl_mapping")
                    .insert(mapping)

                android.util.Log.d("UserGHLMapping", "Mapeo creado: $ghlName -> $userId")
                Result.success(Unit)

            } catch (e: Exception) {
                android.util.Log.e("UserGHLMapping", "Error creando mapeo: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina un mapeo (solo ADMIN)
     *
     * @param ghlName Nombre de GoHighLevel a eliminar
     * @return Result con éxito o error
     */
    suspend fun deleteMapping(ghlName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                client.from("user_ghl_mapping")
                    .delete {
                        filter {
                            eq("ghl_name", ghlName)
                        }
                    }

                android.util.Log.d("UserGHLMapping", "Mapeo eliminado: $ghlName")
                Result.success(Unit)

            } catch (e: Exception) {
                android.util.Log.e("UserGHLMapping", "Error eliminando mapeo: ${e.message}")
                Result.failure(e)
            }
        }
    }
}