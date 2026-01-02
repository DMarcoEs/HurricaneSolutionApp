package com.example.hurricansolutionapp

import android.content.Context
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manager para subida automática de PDFs Y sincronización de cotizaciones con Supabase.
 * Cuando se genera un PDF, automáticamente intenta subirlo si hay conexión.
 * También sincroniza los datos de la cotización a Supabase para que el Admin pueda verlos.
 */
object AutoUploadManager {

    /**
     * Genera el PDF, guarda la cotización en Supabase, y sube el PDF.
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

        // 2. Sincronizar cotización a Supabase (para que Admin pueda ver)
        scope.launch(Dispatchers.IO) {
            try {
                sincronizarCotizacionASupabase(context, cotizacion, pdfFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                // No fallamos si la sincronización falla - al menos quedó local
            }
        }

        // 3. Si hay conexión, intentar subir PDF inmediatamente
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

                        // Si el PDF se subió, actualizar la ruta en Supabase
                        if (success && cotizacion.folio.isNotBlank()) {
                            try {
                                val userId = SessionManager.getUserId(context)
                                // Formatear nombre igual que en UploadRepository
                                val clienteFormateado = formatNameForPath(cotizacion.clienteNombre)
                                val pdfRemotePath = "$userId/Cotizacion_${clienteFormateado}_${cotizacion.folio}.pdf"
                                actualizarPdfPathEnSupabase(cotizacion.folio, pdfRemotePath)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

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
     * Sincroniza una cotización a Supabase para que el Admin pueda verla.
     */
    private suspend fun sincronizarCotizacionASupabase(
        context: Context,
        cotizacion: Cotizacion,
        pdfLocalPath: String?
    ) {
        val userId = SessionManager.getUserId(context)
        if (userId.isBlank()) return

        // Convertir ventanas al modelo de inserción
        val ventanasInsert = cotizacion.ventanas.map { v ->
            VentanaInsert(
                descripcion = v.descripcion,
                alto = v.alto,
                ancho = v.ancho,
                precioM2 = v.precioM2,
                adecuacion = v.adecuacion,
                tipoMontaje = v.tipoMontaje
            )
        }

        // Calcular totales (map para JSONB y valores individuales)
        val totales = mutableMapOf<String, Double>()
        var totalHs875 = 0.0
        var totalHs1250 = 0.0
        var totalHs1500 = 0.0

        cotizacion.productos.forEach { producto ->
            val total = cotizacion.totalConDescuento(producto)
            totales[producto.name] = total

            when (producto) {
                TipoProducto.HS875 -> totalHs875 = total
                TipoProducto.HS1250 -> totalHs1250 = total
                TipoProducto.HS1500 -> totalHs1500 = total
                else -> { /* PERSONALIZADO u otros - no hacer nada */ }
            }
        }

        // Crear objeto para insertar en Supabase
        val cotizacionInsert = CotizacionInsert(
            folio = cotizacion.folio,
            userId = userId,
            especialistaNombre = cotizacion.especialista,
            clienteNombre = cotizacion.clienteNombre,
            clienteTelefono = cotizacion.clienteTelefono,
            ciudad = cotizacion.ciudad,
            ubicacion = cotizacion.ubicacion,
            fecha = cotizacion.fecha,
            productos = cotizacion.productos.map { it.name },
            tipoMontaje = cotizacion.tipoMontaje,
            areaTotal = cotizacion.areaTotal,
            descuentoHs875 = cotizacion.descuentoHS875,
            descuentoHs1250 = cotizacion.descuentoHS1250,
            descuentoHs1500 = cotizacion.descuentoHS1500,
            totalHs875 = totalHs875,
            totalHs1250 = totalHs1250,
            totalHs1500 = totalHs1500,
            totales = totales,
            ventanas = ventanasInsert,
            pdfPath = null // Se actualiza después cuando se sube el PDF
        )

        // Guardar en Supabase
        AdminRepository.saveCotizacion(cotizacionInsert)
    }

    /**
     * Actualiza el path del PDF en Supabase después de que se sube.
     */
    private suspend fun actualizarPdfPathEnSupabase(folio: String, pdfPath: String) {
        try {
            val client = SupabaseClientProvider.client
            client.from("cotizaciones")
                .update(mapOf("pdf_path" to pdfPath)) {
                    filter {
                        eq("folio", folio)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    /**
     * Sincroniza todas las cotizaciones locales que aún no están en Supabase.
     * Útil para migrar datos existentes.
     */
    fun sincronizarCotizacionesLocales(
        context: Context,
        scope: CoroutineScope,
        onComplete: ((Int, Int) -> Unit)? = null // (sincronizadas, errores)
    ) {
        if (!isOnline(context)) {
            onComplete?.invoke(0, 0)
            return
        }

        scope.launch(Dispatchers.IO) {
            var sincronizadas = 0
            var errores = 0

            val cotizacionesLocales = obtenerCotizacionesLocal(context)
            val userId = SessionManager.getUserId(context)

            if (userId.isBlank()) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(0, cotizacionesLocales.size)
                }
                return@launch
            }

            for (cotizacion in cotizacionesLocales) {
                if (cotizacion.folio.isBlank()) continue

                try {
                    // Verificar si ya existe en Supabase
                    val existe = verificarExisteEnSupabase(cotizacion.folio)

                    if (!existe) {
                        sincronizarCotizacionASupabase(context, cotizacion, null)
                        sincronizadas++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    errores++
                }
            }

            withContext(Dispatchers.Main) {
                onComplete?.invoke(sincronizadas, errores)
            }
        }
    }

    private suspend fun verificarExisteEnSupabase(folio: String): Boolean {
        return try {
            if (folio.isBlank()) return false

            val client = SupabaseClientProvider.client
            val result = client.from("cotizaciones")
                .select {
                    filter {
                        eq("folio", folio)
                    }
                }
                .decodeList<CotizacionRemota>()

            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Formatea el nombre del cliente para la ruta del archivo.
     * Debe coincidir exactamente con el formato usado en UploadRepository.
     */
    private fun formatNameForPath(input: String): String {
        val normalized = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

        return normalized
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString("_") { word ->
                word.lowercase(java.util.Locale.getDefault())
                    .replaceFirstChar { it.uppercase() }
            }
            .replace("[^A-Za-z0-9_]+".toRegex(), "")
            .take(50)
    }
}