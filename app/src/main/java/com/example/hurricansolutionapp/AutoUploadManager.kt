package com.example.hurricansolutionapp

import android.content.Context
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*


object AutoUploadManager {

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


        onPdfGenerated?.invoke(pdfFile)

        scope.launch(Dispatchers.IO) {
            try {
                sincronizarCotizacionASupabase(context, cotizacion, pdfFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (isOnline(context)) {
            scope.launch(Dispatchers.IO) {
                try {
                    // Buscar el PendingUpload que acabamos de crear
                    val pendientes = UploadQueueStorage.getAll(context)
                    val pendingItem = pendientes.lastOrNull {
                        it.filePath == pdfFile.absolutePath && it.status == "PENDING"
                    }

                    if (pendingItem != null) {
                        // Intentar subir a Supabase
                        UploadRepository.uploadOne(context, pendingItem)

                        // Verificar resultado de Supabase
                        val updated = UploadQueueStorage.getAll(context)
                            .find { it.id == pendingItem.id }

                        val supabaseSuccess = updated?.status == "DONE"
                        val supabaseError = updated?.lastError

                        if (supabaseSuccess && cotizacion.folio.isNotBlank()) {
                            try {
                                val userId = SessionManager.getUserId(context)
                                val clienteFormateado = formatNameForPath(cotizacion.clienteNombre)
                                val pdfRemotePath = "$userId/Cotizacion_${clienteFormateado}_${cotizacion.folio}.pdf"
                                actualizarPdfPathEnSupabase(cotizacion.folio, pdfRemotePath)

                                val userName = SessionManager.getNombre(context)
                                val userRole = SessionManager.getRole(context)

                                if (userName.isNotBlank() && userRole.isNotBlank()) {
                                    if (DriveAuthManager.isAuthenticated(context)) {
                                        try {
                                            // Marcar como "UPLOADING" antes de subir
                                            UploadQueueStorage.markDriveUploading(context, pendingItem.id)

                                            val driveSuccess = DriveUploadManager.uploadPdfToDriveAuto(
                                                context = context,
                                                pdfFile = pdfFile,
                                                userName = userName,
                                                userRole = userRole,
                                                folio = cotizacion.folio
                                            )

                                            if (driveSuccess) {
                                                UploadQueueStorage.markDriveDone(context, pendingItem.id)
                                                android.util.Log.d("AutoUploadManager", "Drive: Subido y estado actualizado")
                                            } else {
                                                UploadQueueStorage.markDriveError(context, pendingItem.id, "Error al subir a Drive")
                                                android.util.Log.w("AutoUploadManager", "Drive: Fall la subida")
                                            }
                                        } catch (e: Exception) {
                                            UploadQueueStorage.markDriveError(context, pendingItem.id, e.message ?: "Error desconocido")
                                            android.util.Log.e("AutoUploadManager", "Error subiendo a Drive: ${e.message}")
                                        }
                                    } else {
                                        // No autenticado - dejar como PENDING para subida manual
                                        android.util.Log.d("AutoUploadManager", "Drive: No autenticado, quedará pendiente")
                                    }
                                }

                                // Llamar webhook de Make.com (si aplica)
                                val leadId = obtenerLeadIdDeCotizacion(context, cotizacion)
                                if (leadId != null) {
                                    llamarWebhookMakeCom(context, cotizacion, pdfRemotePath, leadId)
                                }

                                val finalItem = UploadQueueStorage.getAll(context).find { it.id == pendingItem.id }
                                if (finalItem?.status == "DONE" && finalItem.driveStatus == "DONE") {
                                    kotlinx.coroutines.delay(500)
                                    UploadQueueStorage.remove(context, pendingItem.id)
                                    android.util.Log.d("AutoUploadManager", "Item limpiado automáticamente (ambos DONE)")
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        withContext(Dispatchers.Main) {
                            onUploadComplete?.invoke(supabaseSuccess, supabaseError)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onUploadComplete?.invoke(false, "No se encontrará el archivo en cola")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onUploadComplete?.invoke(false, e.message)
                    }
                }
            }
        } else {
            onUploadComplete?.invoke(false, "Sin conexión - quedará pendiente")
        }

        return pdfFile
    }


    private suspend fun sincronizarCotizacionASupabase(
        context: Context,
        cotizacion: Cotizacion,
        pdfLocalPath: String?
    ) {
        val userId = SessionManager.getUserId(context)
        if (userId.isBlank()) return

        val ventanasInsert = cotizacion.ventanas.map { v ->
            VentanaInsert(
                zona = v.zona,
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
            ventanas = ventanasInsert,
            totales = totales,
            pdfPath = pdfLocalPath
        )

        try {
            val supabase = SupabaseClientProvider.client
            supabase.from("cotizaciones").upsert(cotizacionInsert) {
            }
            android.util.Log.d("AutoUploadManager", "Cotización sincronizada a Supabase: ${cotizacion.folio}")
        } catch (e: Exception) {
            android.util.Log.e("AutoUploadManager", "Error sincronizando cotización: ${e.message}")
            throw e
        }
    }


    private suspend fun actualizarPdfPathEnSupabase(folio: String, pdfPath: String) {
        try {
            val supabase = SupabaseClientProvider.client
            supabase.from("cotizaciones").update(
                mapOf("pdf_path" to pdfPath)
            ) {
                filter {
                    eq("folio", folio)
                }
            }
            android.util.Log.d("AutoUploadManager", "PDF path actualizado en Supabase: $pdfPath")
        } catch (e: Exception) {
            android.util.Log.e("AutoUploadManager", "Error actualizando pdf_path: ${e.message}")
        }
    }


    fun sincronizarCotizacionesLocales(
        context: Context,
        scope: CoroutineScope,
        onComplete: ((sincronizadas: Int, errores: Int) -> Unit)? = null
    ) {
        scope.launch(Dispatchers.IO) {
            var sincronizadas = 0
            var errores = 0

            val cotizacionesLocales: List<Cotizacion> = obtenerCotizacionesLocal(context)

            if (!isOnline(context)) {
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


    private suspend fun obtenerLeadIdDeCotizacion(
        context: Context,
        cotizacion: Cotizacion
    ): String? {
        return try {

            val lead = LeadsRepository.getLeadByPhone(cotizacion.clienteTelefono)
            lead?.id
        } catch (e: Exception) {
            android.util.Log.e("AutoUploadManager", "Error obteniendo leadId: ${e.message}")
            null
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

    /**
     * Llama al webhook de Make.com para subir PDF a Google Drive
     * y actualizar GoHighLevel
     */
    private suspend fun llamarWebhookMakeCom(
        context: Context,
        cotizacion: Cotizacion,
        pdfRemotePath: String,
        leadId: String?
    ) {
        if (!WebhookConfig.WEBHOOKS_ENABLED) {
            android.util.Log.d("AutoUploadManager", "Webhooks deshabilitados - skip")
            return
        }

        // Si no hay leadId, no hay nada que actualizar en GHL
        if (leadId.isNullOrBlank()) {
            android.util.Log.d("AutoUploadManager", "No hay leadId - skip webhook")
            return
        }

        try {
            android.util.Log.d("AutoUploadManager", "Llamando webhook de Make.com...")

            val pdfPublicUrl = "https://vlorculyexquudkiwxoq.supabase.co/storage/v1/object/public/cotizaciones/$pdfRemotePath"

            // Obtener ghl_opportunity_id del lead
            val lead = LeadsRepository.getLeadById(leadId)
            val ghlOpportunityId = lead?.ghlOpportunityId ?: ""

            // Crear cliente HTTP con Ktor
            val client = HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }

                // Timeout
                engine {
                    config {
                        connectTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS)
                        readTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS)
                        writeTimeout(30000, java.util.concurrent.TimeUnit.MILLISECONDS)
                    }
                }
            }

            // Preparar payload
            val payload = buildJsonObject {
                put("folio", cotizacion.folio)
                put("cliente_nombre", cotizacion.clienteNombre)
                put("especialista_nombre", cotizacion.especialista)
                put("user_role", SessionManager.getRole(context))
                put("pdf_url", pdfPublicUrl)
                put("fecha", cotizacion.fecha)
                put("lead_id", leadId)
                put("ghl_opportunity_id", ghlOpportunityId)
            }

            android.util.Log.d("AutoUploadManager", "Payload: $payload")

            // Hacer la llamada POST al webhook
            val response: HttpResponse = client.post(WebhookConfig.UPLOAD_PDF_TO_DRIVE) {
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }

            // Procesar respuesta
            val statusCode = response.status.value
            val responseBody = response.bodyAsText()

            android.util.Log.d("AutoUploadManager", "Webhook response code: $statusCode")
            android.util.Log.d("AutoUploadManager", "Webhook response body: $responseBody")

            if (response.status.isSuccess()) {
                // Parse respuesta JSON
                try {
                    val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject
                    val driveFileUrl = jsonResponse["drive_file_url"]?.jsonPrimitive?.content
                    val folderPath = jsonResponse["folder_path"]?.jsonPrimitive?.content

                    android.util.Log.d("AutoUploadManager", "PDF subido a Google Drive")
                    android.util.Log.d("AutoUploadManager", "URL: $driveFileUrl")
                    android.util.Log.d("AutoUploadManager", "Carpeta: $folderPath")

                    LeadsRepository.updateLeadPipelineStage(
                        leadId = leadId,
                        newStage = "Seguimiento Medidas"
                    )

                } catch (e: Exception) {
                    android.util.Log.w("AutoUploadManager", "No se pudo parsear respuesta JSON: ${e.message}")
                }
            } else {
                android.util.Log.e("AutoUploadManager", "Error en webhook: HTTP $statusCode")
                android.util.Log.e("AutoUploadManager", "Body: $responseBody")
            }

            client.close()

        } catch (e: Exception) {
            android.util.Log.e("AutoUploadManager", "Error llamando webhook: ${e.message}", e)
            e.printStackTrace()
        }
    }

    suspend fun sincronizarCotizacionEditada(
        context: Context,
        cotizacion: Cotizacion,
        pdfFile: java.io.File?
    ) {
        withContext(Dispatchers.IO) {
            try {
                val userId = SessionManager.getUserId(context)
                if (userId.isBlank()) {
                    android.util.Log.e("AutoUploadManager", "No hay userId para sincronizar")
                    return@withContext
                }

                val ventanasInsert = cotizacion.ventanas.map { v ->
                    VentanaInsert(
                        zona = v.zona,
                        descripcion = v.descripcion,
                        alto = v.alto,
                        ancho = v.ancho,
                        precioM2 = v.precioM2,
                        adecuacion = v.adecuacion,
                        tipoMontaje = v.tipoMontaje
                    )
                }

                // Calcular totales
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
                        else -> { }
                    }
                }

                // Limpiar totales de productos NO seleccionados
                if (!cotizacion.productos.contains(TipoProducto.HS875)) totalHs875 = 0.0
                if (!cotizacion.productos.contains(TipoProducto.HS1250)) totalHs1250 = 0.0
                if (!cotizacion.productos.contains(TipoProducto.HS1500)) totalHs1500 = 0.0

                val zonaStr = cotizacion.zonaGeografica.name.lowercase()

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
                    ventanas = ventanasInsert,
                    totales = totales,
                    pdfPath = pdfFile?.absolutePath,
                    zonaGeografica = zonaStr
                )

                val supabase = SupabaseClientProvider.client

                // Usar UPDATE con filtro por folio para asegurar que se actualiza
                // el registro existente en lugar de crear uno nuevo
                try {
                    supabase.from("cotizaciones").update(cotizacionInsert) {
                        filter {
                            eq("folio", cotizacion.folio)
                        }
                    }
                    android.util.Log.d("AutoUploadManager", "Cotizacion editada ACTUALIZADA en Supabase: ${cotizacion.folio}")
                } catch (updateError: Exception) {
                    // Si falla el update (ej: no existe el registro), intentar upsert como fallback
                    android.util.Log.w("AutoUploadManager", "Update fallo, intentando upsert: ${updateError.message}")
                    supabase.from("cotizaciones").upsert(cotizacionInsert) {
                    }
                    android.util.Log.d("AutoUploadManager", "Cotizacion editada sincronizada via upsert: ${cotizacion.folio}")
                }

            } catch (e: Exception) {
                android.util.Log.e("AutoUploadManager", "Error sincronizando cotización editada: ${e.message}")
                throw e
            }
        }
    }
}