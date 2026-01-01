package com.example.hurricansolutionapp

import android.content.Context
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para operaciones de administrador
 */
object AdminRepository {

    // ═══════════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE PRECIOS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene la configuración actual de precios desde Supabase
     */
    suspend fun getAppConfig(): AppConfig {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("app_config")
                    .select()
                    .decodeSingle<AppConfig>()
            } catch (e: Exception) {
                e.printStackTrace()
                // Retornar valores por defecto si falla
                AppConfig()
            }
        }
    }

    /**
     * Actualiza la configuración de precios (solo ADMIN)
     */
    suspend fun updateAppConfig(
        context: Context,
        config: AppConfigUpdate
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                val userId = SessionManager.getUserId(context)

                if (userId.isBlank()) {
                    return@withContext Result.failure(Exception("No hay sesión activa"))
                }

                val configWithUser = config.copy(updatedBy = userId)

                client.from("app_config")
                    .update(configWithUser) {
                        filter { eq("id", 1) }
                    }

                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // COTIZACIONES (ADMIN VE TODAS)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene todas las cotizaciones (para admin)
     */
    suspend fun getAllCotizaciones(): List<CotizacionRemota> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("cotizaciones")
                    .select()
                    .decodeList<CotizacionRemota>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Obtiene cotizaciones filtradas por usuario
     */
    suspend fun getCotizacionesByUser(userId: String): List<CotizacionRemota> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("cotizaciones")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<CotizacionRemota>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Guarda una cotización en Supabase
     */
    suspend fun saveCotizacion(cotizacion: CotizacionInsert): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                // Usar insert normal - si falla por duplicado, actualizar
                client.from("cotizaciones")
                    .insert(cotizacion)
                Result.success(Unit)
            } catch (e: Exception) {
                // Si ya existe, intentar actualizar
                try {
                    val client = SupabaseClientProvider.client
                    client.from("cotizaciones")
                        .update(cotizacion) {
                            filter { eq("folio", cotizacion.folio) }
                        }
                    Result.success(Unit)
                } catch (updateError: Exception) {
                    updateError.printStackTrace()
                    Result.failure(updateError)
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // GESTIÓN DE USUARIOS/EMPLEADOS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene todos los perfiles de usuarios
     */
    suspend fun getAllUsers(): List<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("profiles")
                    .select()
                    .decodeList<UserProfile>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Obtiene solo los especialistas (no admin)
     */
    suspend fun getEspecialistas(): List<UserProfile> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("profiles")
                    .select {
                        filter { eq("role", "SPECIALIST") }
                    }
                    .decodeList<UserProfile>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Activa o desactiva un usuario
     */
    suspend fun setUserActive(userId: String, isActive: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("profiles")
                    .update(mapOf("is_active" to isActive)) {
                        filter { eq("id", userId) }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ESTADÍSTICAS (DASHBOARD)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Datos para el dashboard de admin
     */
    data class DashboardStats(
        val totalCotizaciones: Int = 0,
        val cotizacionesHoy: Int = 0,
        val cotizacionesMes: Int = 0,
        val totalMetrosCuadrados: Double = 0.0,
        val empleadosActivos: Int = 0,
        val cotizacionesPorEmpleado: Map<String, Int> = emptyMap()
    )

    /**
     * Obtiene estadísticas para el dashboard
     */
    suspend fun getDashboardStats(): DashboardStats {
        return withContext(Dispatchers.IO) {
            try {
                val cotizaciones = getAllCotizaciones()
                val usuarios = getEspecialistas()

                val hoy = java.time.LocalDate.now().toString()
                val mesActual = java.time.YearMonth.now().toString()

                DashboardStats(
                    totalCotizaciones = cotizaciones.size,
                    cotizacionesHoy = cotizaciones.count { it.createdAt?.startsWith(hoy) == true },
                    cotizacionesMes = cotizaciones.count { it.createdAt?.startsWith(mesActual) == true },
                    totalMetrosCuadrados = cotizaciones.sumOf { it.areaTotal },
                    empleadosActivos = usuarios.count { it.isActive },
                    cotizacionesPorEmpleado = cotizaciones.groupBy { it.especialistaNombre }
                        .mapValues { it.value.size }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                DashboardStats()
            }
        }
    }
}