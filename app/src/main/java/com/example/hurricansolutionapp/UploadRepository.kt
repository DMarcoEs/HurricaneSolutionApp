package com.example.hurricansolutionapp

import android.content.Context
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.storage.storage
import java.io.File
import java.time.format.DateTimeFormatter
import java.text.Normalizer
import java.util.Locale
import java.time.ZoneId
import java.time.Instant

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

        fun slug(input: String): String {
            val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")  // quita acentos
            return normalized
                .lowercase(Locale.getDefault())
                .replace("[^a-z0-9]+".toRegex(), "_")                        // todo lo raro a _
                .trim('_')
                .take(60)                                                    // evita nombres gigantes
        }

        // Ruta en el bucket: <userId>/<nombreArchivo>
        // Fecha (día) a partir de createdAt
        val diaFmt = DateTimeFormatter.ofPattern("dd_MMM_yyyy", Locale("es", "ES"))

        val dia = Instant.ofEpochMilli(item.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(diaFmt)


// Partes del nombre
        val cliente = slug(item.clienteNombre ?: "cliente")
        val usuario = slug(
            (item.createdByNombre ?: SessionManager.getNombre(context))
                .ifBlank { "usuario" }
        )
        val id = slug(item.cotizacionId)

// Nombre final del archivo (lo que verás en Supabase)
        val fileName =
            "Cotizacion_${cliente}_dia_${dia}_ID_${id}.pdf"


// Ruta en el bucket: <userId>/<fileName>
        val remotePath = "$userId/$fileName"


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
