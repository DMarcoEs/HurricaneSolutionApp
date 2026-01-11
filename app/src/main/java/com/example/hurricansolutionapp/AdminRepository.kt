package com.example.hurricansolutionapp

import android.content.Context
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ADMIN REPOSITORY - OPERACIONES DE ADMINISTRADOR
 * ═══════════════════════════════════════════════════════════════════════════════
 */
object AdminRepository {

    // ═══════════════════════════════════════════════════════════════════════════════
    // PRECIOS POR ZONA GEOGRÁFICA (NUEVO)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene los precios de todas las zonas desde Supabase
     */
    suspend fun getPreciosTodasZonas(): PreciosTodasZonas {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                val precios = client.from("precios_zona")
                    .select()
                    .decodeList<PrecioZona>()

                android.util.Log.d("AdminRepository", "Precios por zona cargados: ${precios.size} zonas")

                // Mapear a las 3 zonas
                val continental = precios.find { it.zona == "continental" } 
                    ?: PrecioZona(zona = "continental", zonaNombre = "Zona Continental")
                val islas = precios.find { it.zona == "islas" } 
                    ?: PrecioZona(zona = "islas", zonaNombre = "Zona Islas")
                val foranea = precios.find { it.zona == "foranea" } 
                    ?: PrecioZona(zona = "foranea", zonaNombre = "Zona Foránea")

                PreciosTodasZonas(
                    continental = continental,
                    islas = islas,
                    foranea = foranea
                )
            } catch (e: Exception) {
                android.util.Log.e("AdminRepository", "Error cargando precios por zona: ${e.message}")
                e.printStackTrace()
                
                // Si falla, intentar usar precios legacy de app_config
                try {
                    val legacyConfig = getAppConfig()
                    val precioBase = PrecioZona(
                        hs875PrecioVenta = legacyConfig.hs875PrecioVenta,
                        hs875PrecioBase = legacyConfig.hs875PrecioBase,
                        hs1250PrecioVenta = legacyConfig.hs1250PrecioVenta,
                        hs1250PrecioBase = legacyConfig.hs1250PrecioBase,
                        hs1500PrecioVenta = legacyConfig.hs1500PrecioVenta,
                        hs1500PrecioBase = legacyConfig.hs1500PrecioBase
                    )
                    android.util.Log.d("AdminRepository", "Usando precios legacy como fallback")
                    PreciosTodasZonas(
                        continental = precioBase.copy(zona = "continental", zonaNombre = "Zona Continental"),
                        islas = precioBase.copy(zona = "islas", zonaNombre = "Zona Islas"),
                        foranea = precioBase.copy(zona = "foranea", zonaNombre = "Zona Foránea")
                    )
                } catch (e2: Exception) {
                    android.util.Log.e("AdminRepository", "Error en fallback: ${e2.message}")
                    PreciosTodasZonas()
                }
            }
        }
    }

    /**
     * Obtiene los precios de una zona específica
     */
    suspend fun getPreciosZona(zona: String): PrecioZona {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("precios_zona")
                    .select {
                        filter { eq("zona", zona) }
                    }
                    .decodeSingle<PrecioZona>()
            } catch (e: Exception) {
                android.util.Log.e("AdminRepository", "Error cargando precios de zona $zona: ${e.message}")
                e.printStackTrace()
                PrecioZona(zona = zona)
            }
        }
    }

    /**
     * Actualiza los precios de una zona específica (solo ADMIN)
     */
    suspend fun updatePreciosZona(
        context: Context,
        zona: String,
        precios: PrecioZonaUpdate
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                val userId = SessionManager.getUserId(context)

                if (userId.isBlank()) {
                    return@withContext Result.failure(Exception("No hay sesión activa"))
                }

                val preciosWithUser = precios.copy(updatedBy = userId)

                client.from("precios_zona")
                    .update(preciosWithUser) {
                        filter { eq("zona", zona) }
                    }

                android.util.Log.d("AdminRepository", "Precios de zona '$zona' actualizados")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("AdminRepository", "Error actualizando precios zona $zona: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE PRECIOS (LEGACY - para compatibilidad)
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene la configuración actual de precios desde Supabase (LEGACY)
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
                AppConfig()
            }
        }
    }

    /**
     * Actualiza la configuración de precios (solo ADMIN) - LEGACY
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
                android.util.Log.d("AdminRepository", "Obteniendo cotizaciones...")

                val result = client.from("cotizaciones")
                    .select()
                    .decodeList<CotizacionRemota>()

                android.util.Log.d("AdminRepository", "Cotizaciones obtenidas: ${result.size}")
                result.forEach { cot ->
                    android.util.Log.d("AdminRepository", "  - ${cot.folio}: ${cot.clienteNombre} (zona: ${cot.zonaGeografica})")
                }

                result
            } catch (e: Exception) {
                android.util.Log.e("AdminRepository", "Error obteniendo cotizaciones: ${e.message}", e)
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
        val cotizacionesPorEmpleado: Map<String, Int> = emptyMap(),
        val cotizacionesPorZona: Map<String, Int> = emptyMap() // NUEVO
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
                        .mapValues { it.value.size },
                    cotizacionesPorZona = cotizaciones.groupBy { it.zonaGeografica ?: "continental" }
                        .mapValues { it.value.size }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                DashboardStats()
            }
        }
    }
}
