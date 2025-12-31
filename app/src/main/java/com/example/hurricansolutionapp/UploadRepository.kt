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

        /**
         * Formatea el nombre para que sea profesional:
         * - Primera letra de cada palabra en mayúscula
         * - Sin acentos
         * - Espacios reemplazados por guión bajo
         */
        fun formatName(input: String): String {
            val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")  // quita acentos

            return normalized
                .trim()
                .split("\\s+".toRegex())  // dividir por espacios
                .filter { it.isNotBlank() }
                .joinToString("_") { word ->
                    word.lowercase(Locale.getDefault())
                        .replaceFirstChar { it.uppercase() }  // Primera letra mayúscula
                }
                .replace("[^A-Za-z0-9_]+".toRegex(), "")  // quitar caracteres raros
                .take(50)  // limitar longitud
        }

        // Formatear nombre del cliente: "erick hernandez rios" → "Erick_Hernandez_Rios"
        val clienteFormateado = formatName(item.clienteNombre ?: "Cliente")

        // Folio ya viene en formato correcto: "MC-0004"
        val folio = item.cotizacionId.ifBlank { "SIN_FOLIO" }

        // Nombre final: Cotizacion_Erick_Hernandez_Rios_MC-0004.pdf
        val fileName = "Cotizacion_${clienteFormateado}_${folio}.pdf"

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
                    upsert = true  // Si existe, lo reemplaza (útil para ediciones)
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