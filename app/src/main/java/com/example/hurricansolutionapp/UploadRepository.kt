package com.example.hurricansolutionapp

import android.content.Context
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.storage
import java.io.File

object UploadRepository {

    /**
     * Sube 1 archivo y actualiza la cola:
     * - DONE si se sube
     * - ERROR si falla
     */
    suspend fun uploadOne(context: Context, item: PendingUpload) {
        val client = SupabaseClientProvider.client

        val file = File(item.filePath)
        if (!file.exists()) {
            UploadQueueStorage.markError(context, item.id, "Archivo no existe en el dispositivo")
            return
        }

        val userId = SessionManager.getUserId(context)
        if (userId.isNullOrBlank()) {
            UploadQueueStorage.markError(context, item.id, "No hay sesión activa (SessionManager)")
            return
        }

        // Ruta en el bucket: <userId>/<nombreArchivo>
        val remotePath = "$userId/${file.name}"

        try {
            val bytes = file.readBytes()

            UploadQueueStorage.markUploading(context, item.id)

            // Subida a Storage (bucket privado)
            client.storage
                .from("cotizaciones")
                .upload(
                    path = remotePath,
                    data = bytes,
                    upsert = true
                )

            UploadQueueStorage.markDone(context, item.id)

        } catch (e: Exception) {
            UploadQueueStorage.markError(context, item.id, e.message ?: "Error desconocido")
        }
    }

    /**
     * Reintenta todos los pendientes y errores.
     */
    suspend fun uploadAllPending(context: Context) {
        val items = UploadQueueStorage.getAll(context)
        val targets = items.filter { it.status == "PENDING" || it.status == "ERROR" }

        for (item in targets) {
            uploadOne(context, item)
        }
    }
}
