package com.example.hurricansolutionapp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.jan.supabase.postgrest.from

/**
 * Manager para subida automática a Google Drive con sistema de cola
 * Similar a AutoUploadManager pero específico para Drive
 */
object DriveUploadManager {

    private const val TAG = "DriveUploadManager"

    /**
     * Intenta subir un PDF a Google Drive automáticamente
     * Si falla, lo guarda en cola para reintento manual
     *
     * @param context Contexto de la aplicación
     * @param pdfFile Archivo PDF local
     * @param userName Nombre del usuario
     * @param userRole Rol del usuario
     * @param folio Folio de la cotización
     * @return true si se subió exitosamente, false si quedó pendiente
     */
    suspend fun uploadPdfToDriveAuto(
        context: Context,
        pdfFile: java.io.File,
        userName: String,
        userRole: String,
        folio: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Intentando subida automática a Drive: ${pdfFile.name}")

            // Verificar autenticación
            if (!DriveAuthManager.isAuthenticated(context)) {
                Log.w(TAG, "No autenticado - Encolando para subida manual")
                enqueueDriveUpload(context, pdfFile, userName, userRole, folio)
                return@withContext false
            }

            // Intentar subir
            val result = GoogleDriveRepository.uploadPdfToStructuredFolder(
                context = context,
                localPdfFile = pdfFile,
                userName = userName,
                userRole = userRole
            )

            if (result.isSuccess) {
                val uploadResult = result.getOrNull()
                if (uploadResult?.success == true) {
                    Log.d(TAG, "[OK] Subida automática a Drive exitosa: ${pdfFile.name}")
                    return@withContext true
                } else {
                    Log.w(TAG, "Subida falló: ${uploadResult?.error} - Encolando")
                    enqueueDriveUpload(context, pdfFile, userName, userRole, folio, uploadResult?.error)
                    return@withContext false
                }
            } else {
                Log.w(TAG, "Error en subida: ${result.exceptionOrNull()?.message} - Encolando")
                enqueueDriveUpload(context, pdfFile, userName, userRole, folio, result.exceptionOrNull()?.message)
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado: ${e.message} - Encolando", e)
            enqueueDriveUpload(context, pdfFile, userName, userRole, folio, e.message)
            return@withContext false
        }
    }

    /**
     * Encola un PDF para subida manual posterior
     */
    private suspend fun enqueueDriveUpload(
        context: Context,
        pdfFile: java.io.File,
        userName: String,
        userRole: String,
        folio: String,
        error: String? = null
    ) {
        try {
            val userId = SessionManager.getUserId(context)
            val supabase = SupabaseClientProvider.client

            supabase.from("drive_pending_uploads").insert(
                mapOf(
                    "pdf_filename" to pdfFile.name,
                    "supabase_url" to "local://${pdfFile.absolutePath}",
                    "user_id" to userId,
                    "user_name" to userName,
                    "user_role" to userRole,
                    "folio" to folio,
                    "target_folder_path" to "Hurricane Solution/$userRole/$userName",
                    "last_error" to error,
                    "retry_count" to 0
                )
            )

            Log.d(TAG, "PDF encolado para Drive: ${pdfFile.name}")

        } catch (e: Exception) {
            Log.e(TAG, "Error encolando para Drive: ${e.message}", e)
        }
    }

    /**
     * Reintenta subir un pendiente de Drive
     *
     * @param context Contexto
     * @param pending Información del pendiente
     * @return true si se subió exitosamente
     */
    suspend fun retryDriveUpload(
        context: Context,
        pending: DrivePendingUpload
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Reintentando subida a Drive: ${pending.pdfFilename}")

            // Verificar autenticación
            if (!DriveAuthManager.isAuthenticated(context)) {
                updateDriveUploadError(pending.id, "No autenticado con Google Drive")
                return@withContext false
            }

            // Obtener archivo local
            val localPath = pending.supabaseUrl.removePrefix("local://")
            val pdfFile = java.io.File(localPath)

            if (!pdfFile.exists()) {
                updateDriveUploadError(pending.id, "Archivo local no existe")
                return@withContext false
            }

            // Intentar subir
            val result = GoogleDriveRepository.uploadPdfToStructuredFolder(
                context = context,
                localPdfFile = pdfFile,
                userName = pending.userName,
                userRole = pending.userRole
            )

            if (result.isSuccess) {
                val uploadResult = result.getOrNull()
                if (uploadResult?.success == true) {
                    Log.d(TAG, "[OK] Reintento exitoso: ${pending.pdfFilename}")
                    markDriveUploadAsComplete(pending.id)
                    return@withContext true
                } else {
                    Log.w(TAG, "Reintento falló: ${uploadResult?.error}")
                    updateDriveUploadError(pending.id, uploadResult?.error ?: "Error desconocido")
                    return@withContext false
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                Log.w(TAG, "Reintento falló: $error")
                updateDriveUploadError(pending.id, error)
                return@withContext false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error en reintento: ${e.message}", e)
            updateDriveUploadError(pending.id, e.message ?: "Error desconocido")
            return@withContext false
        }
    }

    /**
     * Marca un upload como completado
     */
    private suspend fun markDriveUploadAsComplete(pendingId: String) {
        try {
            val supabase = SupabaseClientProvider.client
            val now = java.time.Instant.now().toString()

            supabase.from("drive_pending_uploads").update(
                mapOf("uploaded_at" to now)
            ) {
                filter {
                    eq("id", pendingId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando estado: ${e.message}", e)
        }
    }

    /**
     * Actualiza el error de un upload
     */
    private suspend fun updateDriveUploadError(pendingId: String, error: String) {
        try {
            val supabase = SupabaseClientProvider.client

            // Primero obtener el retry_count actual
            val current = supabase.from("drive_pending_uploads")
                .select {
                    filter {
                        eq("id", pendingId)
                    }
                }
                .decodeSingleOrNull<DrivePendingUpload>()

            val newRetryCount = (current?.retryCount ?: 0) + 1

            // Actualizar con nuevo retry_count
            supabase.from("drive_pending_uploads").update(
                mapOf(
                    "last_error" to error,
                    "retry_count" to newRetryCount
                )
            ) {
                filter {
                    eq("id", pendingId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando error: ${e.message}", e)
        }
    }

    /**
     * Obtiene todos los pendientes de Drive del usuario actual
     */
    suspend fun getPendingDriveUploads(context: Context): List<DrivePendingUpload> {
        return try {
            val userId = SessionManager.getUserId(context)
            val supabase = SupabaseClientProvider.client

            val result = supabase.from("drive_pending_uploads")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<DrivePendingUpload>()
                .filter { it.uploadedAt == null }  // [OK] Filtrar los no subidos

            // Ordenar en cliente
            result.sortedByDescending { it.createdAt }

        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo pendientes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Elimina un pendiente de Drive
     */
    suspend fun removePendingDriveUpload(pendingId: String): Boolean {
        return try {
            val supabase = SupabaseClientProvider.client
            supabase.from("drive_pending_uploads").delete {
                filter {
                    eq("id", pendingId)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando pendiente: ${e.message}", e)
            false
        }
    }
}