package com.example.hurricansolutionapp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import java.util.Locale

/**
 * Manager para subida de PDFs de Instalación a Google Drive
 *
 * Estructura de carpetas:
 * [Carpeta Compartida]/Instalaciones/[Usuario]/[Año]/[Mes]/archivo.pdf
 */
object InstaladorUploadManager {

    private const val TAG = "InstaladorUploadManager"

    /**
     * Formatea el nombre para que sea profesional
     */
    private fun formatName(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

        return normalized
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .joinToString("_") { word ->
                word.lowercase(Locale.getDefault())
                    .replaceFirstChar { it.uppercase() }
            }
            .replace("[^A-Za-z0-9_]+".toRegex(), "")
            .take(50)
    }

    /**
     * Genera el nombre del archivo PDF de instalación
     */
    fun generateFileName(clienteNombre: String, folio: String, sistema: String): String {
        val clienteFormateado = formatName(clienteNombre)
        val sistemaCorto = sistema.replace("HS", "").replace("-", "").replace(" ", "")
        return "Instalacion_${clienteFormateado}_${folio}_HS${sistemaCorto}.pdf"
    }

    /**
     * Sube un PDF de instalador a Google Drive
     *
     * @param context Contexto de la aplicación
     * @param pdfFile Archivo PDF generado
     * @param cotizacion Datos de la cotización
     * @param sistemaSeleccionado Sistema HS seleccionado
     * @return Result con el éxito o error de la operación
     */
    suspend fun uploadToDrive(
        context: Context,
        pdfFile: File,
        cotizacion: Cotizacion,
        sistemaSeleccionado: String
    ): Result<DriveUploadResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando subida de PDF de instalación: ${pdfFile.name}")

            // Verificar que el archivo existe
            if (!pdfFile.exists()) {
                Log.e(TAG, "Archivo no existe: ${pdfFile.absolutePath}")
                return@withContext Result.success(
                    DriveUploadResult(
                        success = false,
                        fileName = pdfFile.name,
                        folderPath = "",
                        error = "Archivo no existe"
                    )
                )
            }

            // Verificar autenticación con Drive
            if (!DriveAuthManager.isAuthenticated(context)) {
                Log.w(TAG, "No autenticado con Google Drive")
                return@withContext Result.success(
                    DriveUploadResult(
                        success = false,
                        fileName = pdfFile.name,
                        folderPath = "",
                        error = "No autenticado con Google Drive. Inicia sesión en Drive primero."
                    )
                )
            }

            // Obtener nombre del usuario
            val userName = SessionManager.getNombre(context)
            if (userName.isBlank()) {
                return@withContext Result.success(
                    DriveUploadResult(
                        success = false,
                        fileName = pdfFile.name,
                        folderPath = "",
                        error = "No se pudo obtener el nombre del usuario"
                    )
                )
            }

            // Subir a Drive usando la función específica para Instalaciones
            val result = GoogleDriveRepository.uploadInstalacionPdf(
                context = context,
                localPdfFile = pdfFile,
                userName = userName
            )

            if (result.isSuccess) {
                val uploadResult = result.getOrNull()
                if (uploadResult?.success == true) {
                    Log.d(TAG, "[OK] PDF subido exitosamente a: ${uploadResult.folderPath}")
                } else {
                    Log.w(TAG, "Error en subida: ${uploadResult?.error}")
                }
                return@withContext result
            } else {
                Log.e(TAG, "Error en subida: ${result.exceptionOrNull()?.message}")
                return@withContext Result.success(
                    DriveUploadResult(
                        success = false,
                        fileName = pdfFile.name,
                        folderPath = "",
                        error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    )
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado: ${e.message}", e)
            Result.success(
                DriveUploadResult(
                    success = false,
                    fileName = pdfFile.name,
                    folderPath = "",
                    error = e.message ?: "Error desconocido"
                )
            )
        }
    }

    /**
     * Versión simplificada que solo necesita el contexto, archivo y nombre de usuario
     */
    suspend fun uploadToDriveSimple(
        context: Context,
        pdfFile: File
    ): Result<DriveUploadResult> = withContext(Dispatchers.IO) {
        try {
            if (!pdfFile.exists()) {
                return@withContext Result.success(
                    DriveUploadResult(
                        success = false,
                        fileName = pdfFile.name,
                        folderPath = "",
                        error = "Archivo no existe"
                    )
                )
            }

            if (!DriveAuthManager.isAuthenticated(context)) {
                return@withContext Result.success(
                    DriveUploadResult(
                        success = false,
                        fileName = pdfFile.name,
                        folderPath = "",
                        error = "No autenticado con Google Drive"
                    )
                )
            }

            val userName = SessionManager.getNombre(context)

            GoogleDriveRepository.uploadInstalacionPdf(
                context = context,
                localPdfFile = pdfFile,
                userName = userName
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            Result.success(
                DriveUploadResult(
                    success = false,
                    fileName = pdfFile.name,
                    folderPath = "",
                    error = e.message ?: "Error desconocido"
                )
            )
        }
    }
}