package com.example.hurricansolutionapp

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manager para subida automática de PDFs.
 * Cuando se genera un PDF, automáticamente intenta subirlo si hay conexión.
 */
object AutoUploadManager {

    /**
     * Genera el PDF y automáticamente intenta subirlo.
     *
     * @param context Contexto de la aplicación
     * @param cotizacion La cotización a generar
     * @param scope CoroutineScope para la subida asíncrona
     * @param onPdfGenerated Callback cuando el PDF se genera (antes de subir)
     * @param onUploadComplete Callback cuando la subida termina (éxito o error)
     * @return El archivo PDF generado, o null si falló
     */
    fun generarYSubirPdf(
        context: Context,
        cotizacion: Cotizacion,
        scope: CoroutineScope,
        onPdfGenerated: ((java.io.File) -> Unit)? = null,
        onUploadComplete: ((Boolean, String?) -> Unit)? = null
    ): java.io.File? {
        // 1. Generar el PDF (esto ya encola el PendingUpload)
        val pdfFile = generarPdfCotizacion(context, cotizacion)

        if (pdfFile == null) {
            onUploadComplete?.invoke(false, "Error al generar PDF")
            return null
        }

        // Notificar que el PDF se generó
        onPdfGenerated?.invoke(pdfFile)

        // 2. Si hay conexión, intentar subir inmediatamente
        if (isOnline(context)) {
            scope.launch(Dispatchers.IO) {
                try {
                    // Buscar el PendingUpload que acabamos de crear
                    val pendientes = UploadQueueStorage.getAll(context)
                    val pendingItem = pendientes.lastOrNull {
                        it.filePath == pdfFile.absolutePath && it.status == "PENDING"
                    }

                    if (pendingItem != null) {
                        // Intentar subir
                        UploadRepository.uploadOne(context, pendingItem)

                        // Verificar resultado
                        val updated = UploadQueueStorage.getAll(context)
                            .find { it.id == pendingItem.id }

                        val success = updated?.status == "DONE"
                        val error = updated?.lastError

                        withContext(Dispatchers.Main) {
                            onUploadComplete?.invoke(success, error)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onUploadComplete?.invoke(false, "No se encontró el archivo en cola")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onUploadComplete?.invoke(false, e.message)
                    }
                }
            }
        } else {
            // Sin conexión - quedará pendiente para después
            onUploadComplete?.invoke(false, "Sin conexión - quedará pendiente")
        }

        return pdfFile
    }

    /**
     * Intenta subir todos los pendientes en segundo plano.
     * Útil para llamar cuando se detecta conexión a internet.
     */
    fun subirPendientesEnBackground(
        context: Context,
        scope: CoroutineScope,
        onComplete: ((Int, Int) -> Unit)? = null // (exitosos, fallidos)
    ) {
        if (!isOnline(context)) {
            onComplete?.invoke(0, 0)
            return
        }

        scope.launch(Dispatchers.IO) {
            var exitosos = 0
            var fallidos = 0

            val pendientes = UploadQueueStorage.getAll(context)
                .filter { it.status == "PENDING" || it.status == "ERROR" }

            for (item in pendientes) {
                try {
                    UploadRepository.uploadOne(context, item)

                    // Verificar resultado
                    val updated = UploadQueueStorage.getAll(context)
                        .find { it.id == item.id }

                    if (updated?.status == "DONE") {
                        exitosos++
                    } else {
                        fallidos++
                    }
                } catch (e: Exception) {
                    fallidos++
                }
            }

            withContext(Dispatchers.Main) {
                onComplete?.invoke(exitosos, fallidos)
            }
        }
    }

    /**
     * Limpia los uploads completados (DONE) de la cola.
     */
    fun limpiarCompletados(context: Context) {
        UploadQueueStorage.clearDone(context)
    }
}