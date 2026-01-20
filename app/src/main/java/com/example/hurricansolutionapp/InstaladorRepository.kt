package com.example.hurricansolutionapp

import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositorio para operaciones CRUD de las tablas del instalador
 */
object InstaladorRepository {

    private const val TAG = "InstaladorRepository"

    // ═══════════════════════════════════════════════════════════════════════════════
    // CRUD: instalador_datos
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene todos los datos de instalador asignados a un instalador específico
     */
    suspend fun getDatosForInstalador(instaladorId: String): Result<List<InstaladorDatos>> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                val result = client.from("instalador_datos")
                    .select {
                        filter {
                            or {
                                eq("instalador_id", instaladorId)
                                exact("instalador_id", null)
                            }
                            neq("folio", "")
                        }
                    }
                    .decodeList<InstaladorDatos>()

                Result.success(result.sortedByDescending { it.createdAt })
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error getDatosForInstalador: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene datos de instalador por folio
     */
    suspend fun getDatosByFolio(folio: String): Result<InstaladorDatos?> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                val result = client.from("instalador_datos")
                    .select {
                        filter { eq("folio", folio) }
                    }
                    .decodeSingleOrNull<InstaladorDatos>()

                Result.success(result)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error getDatosByFolio: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene datos completos (instalador_datos + medidas) por folio
     */
    suspend fun getDatosCompletosByFolio(folio: String): Result<Pair<InstaladorDatos, List<MedidaInstalador>>?> {
        return withContext(Dispatchers.IO) {
            try {
                val datosResult = getDatosByFolio(folio)
                if (datosResult.isFailure) {
                    return@withContext Result.failure(
                        datosResult.exceptionOrNull() ?: Exception("Error obteniendo datos")
                    )
                }

                val datos = datosResult.getOrNull()
                if (datos == null) {
                    return@withContext Result.success(null)
                }

                val medidasResult = getMedidasByDatosId(datos.id)
                val medidas = if (medidasResult.isSuccess) {
                    medidasResult.getOrNull() ?: emptyList()
                } else {
                    emptyList()
                }

                Result.success(Pair(datos, medidas))
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error getDatosCompletosByFolio: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Inserta un nuevo registro de instalador_datos
     */
    suspend fun insertDatos(datos: InstaladorDatosInsert): Result<InstaladorDatos> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                val result = client.from("instalador_datos")
                    .insert(datos) {
                        select()
                    }
                    .decodeSingle<InstaladorDatos>()

                android.util.Log.d(TAG, "✅ InstaladorDatos insertado: ${result.folio}")
                Result.success(result)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error insertDatos: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Actualiza un registro de instalador_datos
     */
    suspend fun updateDatos(id: String, update: InstaladorDatosUpdate): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("instalador_datos")
                    .update(update) {
                        filter { eq("id", id) }
                    }

                android.util.Log.d(TAG, "✅ InstaladorDatos actualizado: $id")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error updateDatos: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Marca un registro como rectificado
     */
    suspend fun marcarRectificadas(id: String): Result<Unit> {
        return updateDatos(
            id, InstaladorDatosUpdate(
                rectificadas = true,
                fechaRectificacion = java.time.OffsetDateTime.now().toString()
            )
        )
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════
     * FUNCIÓN PRINCIPAL: Crea registro completo desde cotización
     * Esta función crea instalador_datos + medidas_instalador correctamente
     * ════════════════════════════════════════════════════════════════════════════
     */
    suspend fun crearRegistroDesdeCotizacionCompleto(
        cotizacion: Cotizacion,
        sistemaSeleccionado: String,
        especialistaId: String,
        especialistaNombre: String,
        fechaSolicitada: String? = null  // ✅ NUEVO: Fecha solicitada
    ): Result<InstaladorDatos> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "📝 Creando registro para: ${cotizacion.folio}")
                android.util.Log.d(TAG, "📝 Cliente: ${cotizacion.clienteNombre}")
                android.util.Log.d(TAG, "📝 Ventanas: ${cotizacion.ventanas.size}")

                // 1. Crear el registro principal en instalador_datos
                val datosInsert = InstaladorDatosInsert(
                    cotizacionId = cotizacion.id,
                    folio = cotizacion.folio,
                    nombreCliente = cotizacion.clienteNombre,
                    telefonoCliente = cotizacion.clienteTelefono,
                    direccion = cotizacion.ubicacion,
                    ciudad = cotizacion.ciudad,
                    colonia = null,
                    sistemaSeleccionado = sistemaSeleccionado,
                    especialistaId = especialistaId,
                    especialistaNombre = especialistaNombre,
                    fechaSolicitada = fechaSolicitada  // ✅ NUEVO
                )

                val insertResult = insertDatos(datosInsert)
                if (insertResult.isFailure) {
                    android.util.Log.e(
                        TAG,
                        "❌ Error insertando datos: ${insertResult.exceptionOrNull()?.message}"
                    )
                    return@withContext insertResult
                }

                val datos = insertResult.getOrNull()!!
                android.util.Log.d(TAG, "✅ InstaladorDatos creado con ID: ${datos.id}")

                // 2. Insertar las medidas desde las ventanas de la cotización
                android.util.Log.d(TAG, "════════════════════════════════════════")
                android.util.Log.d(TAG, "📋 Ventanas en cotización: ${cotizacion.ventanas.size}")
                cotizacion.ventanas.forEachIndexed { idx, v ->
                    android.util.Log.d(TAG, "  [$idx] ${v.descripcion}: ${v.alto}x${v.ancho} m")
                }
                android.util.Log.d(TAG, "════════════════════════════════════════")

                if (cotizacion.ventanas.isNotEmpty()) {
                    val medidasInsert = cotizacion.ventanas.mapIndexed { index, ventana ->
                        MedidaInstaladorInsert(
                            instaladorDatosId = datos.id,
                            zona = ventana.zona.ifBlank { null },  // ✅ NUEVO: Zona del especialista
                            descripcion = ventana.descripcion,
                            cantidad = 1,
                            alto = ventana.alto,
                            ancho = ventana.ancho,
                            tipoMontaje = ventana.tipoMontaje,
                            requiereAdecuacion = ventana.adecuacion.isNotBlank() && ventana.adecuacion != "No",
                            adecuacionDetalle = if (ventana.adecuacion != "No") ventana.adecuacion else null,
                            orden = index
                        )
                    }

                    android.util.Log.d(TAG, "📝 Insertando ${medidasInsert.size} medidas para datos_id: ${datos.id}")

                    val medidasResult = insertMedidas(medidasInsert)
                    if (medidasResult.isFailure) {
                        android.util.Log.e(
                            TAG,
                            "⚠️ Error insertando medidas: ${medidasResult.exceptionOrNull()?.message}"
                        )
                        // No fallamos, solo logueamos - el registro principal ya existe
                    } else {
                        android.util.Log.d(TAG, "✅ ${medidasInsert.size} medidas insertadas correctamente")
                    }
                } else {
                    android.util.Log.w(TAG, "⚠️ Cotización sin ventanas - no se insertaron medidas")
                }

                android.util.Log.d(
                    TAG,
                    "✅ Registro de instalador creado desde cotización: ${cotizacion.folio}"
                )
                Result.success(datos)

            } catch (e: Exception) {
                android.util.Log.e(
                    TAG,
                    "Error crearRegistroDesdeCotizacionCompleto: ${e.message}",
                    e
                )
                Result.failure(e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CRUD: medidas_instalador
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene las medidas de un registro de instalador_datos
     */
    suspend fun getMedidasByDatosId(datosId: String): Result<List<MedidaInstalador>> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                val result = client.from("medidas_instalador")
                    .select {
                        filter { eq("instalador_datos_id", datosId) }
                    }
                    .decodeList<MedidaInstalador>()

                Result.success(result.sortedBy { it.orden })
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error getMedidasByDatosId: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Inserta múltiples medidas
     */
    suspend fun insertMedidas(medidas: List<MedidaInstaladorInsert>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (medidas.isEmpty()) return@withContext Result.success(Unit)

                val client = SupabaseClientProvider.client
                client.from("medidas_instalador")
                    .insert(medidas)

                android.util.Log.d(TAG, "✅ ${medidas.size} medidas insertadas")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error insertMedidas: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Actualiza una medida específica
     */
    suspend fun updateMedida(id: String, medida: MedidaInstaladorInsert): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("medidas_instalador")
                    .update(medida) {
                        filter { eq("id", id) }
                    }

                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error updateMedida: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina todas las medidas de un registro y las reemplaza con nuevas
     */
    suspend fun replaceMedidas(
        datosId: String,
        medidas: List<MedidaInstaladorInsert>
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                // Eliminar medidas existentes
                client.from("medidas_instalador")
                    .delete {
                        filter { eq("instalador_datos_id", datosId) }
                    }

                // Insertar nuevas medidas
                if (medidas.isNotEmpty()) {
                    client.from("medidas_instalador")
                        .insert(medidas)
                }

                android.util.Log.d(TAG, "✅ Medidas reemplazadas para datosId: $datosId")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error replaceMedidas: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // CRUD: instalador_pending_uploads
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene todos los pendientes no subidos
     */
    suspend fun getAllPending(): Result<List<InstaladorPendingUpload>> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                val result = client.from("instalador_pending_uploads")
                    .select {
                        filter {
                            neq("status", InstaladorUploadStatus.DONE)
                        }
                    }
                    .decodeList<InstaladorPendingUpload>()

                Result.success(result.sortedByDescending { it.createdAt })
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error getAllPending: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Cuenta los pendientes no subidos
     */
    suspend fun countAllPending(): Int {
        return try {
            val result = getAllPending()
            if (result.isSuccess) {
                result.getOrNull()?.size ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Encola un nuevo PDF pendiente
     */
    suspend fun enqueuePending(pending: InstaladorPendingInsert): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("instalador_pending_uploads")
                    .insert(pending)

                android.util.Log.d(TAG, "✅ PDF encolado: ${pending.fileName}")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error enqueuePending: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Actualiza el estado de un pendiente
     */
    suspend fun updatePendingStatus(id: String, status: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("instalador_pending_uploads")
                    .update(mapOf("status" to status)) {
                        filter { eq("id", id) }
                    }

                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error updatePendingStatus: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Marca un pendiente como subido exitosamente
     */
    suspend fun markPendingDone(
        id: String,
        driveFileId: String,
        driveFolderPath: String?
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("instalador_pending_uploads")
                    .update(
                        mapOf(
                            "status" to InstaladorUploadStatus.DONE,
                            "drive_file_id" to driveFileId,
                            "drive_folder_path" to driveFolderPath,
                            "uploaded_at" to java.time.OffsetDateTime.now().toString()
                        )
                    ) {
                        filter { eq("id", id) }
                    }

                android.util.Log.d(TAG, "✅ Pendiente marcado como subido: $id")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error markPendingDone: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Marca un pendiente con error
     */
    suspend fun markPendingError(id: String, errorMessage: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client

                // Obtener retry_count actual
                val current = client.from("instalador_pending_uploads")
                    .select {
                        filter { eq("id", id) }
                    }
                    .decodeSingleOrNull<InstaladorPendingUpload>()

                val newRetryCount = (current?.retryCount ?: 0) + 1

                client.from("instalador_pending_uploads")
                    .update(
                        mapOf(
                            "status" to InstaladorUploadStatus.ERROR,
                            "error_message" to errorMessage,
                            "retry_count" to newRetryCount
                        )
                    ) {
                        filter { eq("id", id) }
                    }

                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error markPendingError: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Elimina un pendiente
     */
    suspend fun deletePending(id: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val client = SupabaseClientProvider.client
                client.from("instalador_pending_uploads")
                    .delete {
                        filter { eq("id", id) }
                    }

                android.util.Log.d(TAG, "✅ Pendiente eliminado: $id")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error deletePending: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
}