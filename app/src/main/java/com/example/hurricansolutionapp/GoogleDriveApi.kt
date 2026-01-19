package com.example.hurricansolutionapp

import android.content.Context
import android.util.Log
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * API para interactuar con Google Drive
 */
object GoogleDriveApi {

    private const val TAG = ApiConfig.LOG_TAG_DRIVE

    /**
     * Crea una carpeta en Google Drive
     *
     * @param drive Servicio de Drive autenticado
     * @param folderName Nombre de la carpeta
     * @param parentFolderId ID de la carpeta padre (null para raíz)
     * @return ID de la carpeta creada o existente
     */
    suspend fun createFolder(
        drive: Drive,
        folderName: String,
        parentFolderId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Primero verificar si ya existe
            val existingFolder = findFolderByName(drive, folderName, parentFolderId)
            if (existingFolder != null) {
                Log.d(TAG, "ðŸ“ Carpeta ya existe: $folderName (${existingFolder.id})")
                return@withContext Result.success(existingFolder.id)
            }

            // Crear nueva carpeta
            val folderMetadata = File().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
                parents = parentFolderId?.let { listOf(it) }
            }

            val folder = drive.files().create(folderMetadata)
                .setFields("id, name")
                .execute()

            Log.d(TAG, "[OK] Carpeta creada: $folderName (${folder.id})")
            Result.success(folder.id)

        } catch (e: Exception) {
            Log.e(TAG, "âŒ Error creando carpeta '$folderName': ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Busca una carpeta por nombre
     *
     * @param drive Servicio de Drive
     * @param folderName Nombre de la carpeta
     * @param parentFolderId ID del padre (null para buscar en raíz)
     * @return File de Drive o null si no existe
     */
    private suspend fun findFolderByName(
        drive: Drive,
        folderName: String,
        parentFolderId: String? = null
    ): File? = withContext(Dispatchers.IO) {
        try {
            val query = buildString {
                append("mimeType='application/vnd.google-apps.folder'")
                append(" and name='$folderName'")
                append(" and trashed=false")

                if (parentFolderId != null) {
                    append(" and '$parentFolderId' in parents")
                } else {
                    append(" and 'root' in parents")
                }
            }

            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            result.files.firstOrNull()

        } catch (e: Exception) {
            Log.e(TAG, "Error buscando carpeta: ${e.message}", e)
            null
        }
    }

    /**
     * Sube un archivo PDF a Google Drive
     *
     * @param context Contexto de Android
     * @param drive Servicio de Drive autenticado
     * @param pdfUrl URL del PDF en Supabase Storage
     * @param fileName Nombre del archivo
     * @param folderId ID de la carpeta destino
     * @return Resultado con información del archivo subido
     */
    /**
     * Sube un archivo PDF local a Google Drive
     * [OK] CORREGIDO: Usa archivo local directamente, NO descarga de Supabase
     *
     * @param drive Servicio de Drive autenticado
     * @param localFile Archivo PDF local (ya existe en el dispositivo)
     * @param folderId ID de la carpeta destino
     * @return Resultado con información del archivo subido
     */
    suspend fun uploadPdfFromLocalFile(
        drive: Drive,
        localFile: java.io.File,
        folderId: String
    ): Result<DriveUploadResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "ðŸ“¤ Iniciando subida: ${localFile.name}")

            // Verificar que el archivo existe
            if (!localFile.exists()) {
                return@withContext Result.success(
                    DriveUploadResult(
                        success = false,
                        fileName = localFile.name,
                        folderPath = "",
                        error = "Archivo local no existe"
                    )
                )
            }

            Log.d(TAG, "ðŸ“„ Archivo local: ${localFile.length()} bytes")

            // Crear metadata del archivo en Drive
            val fileMetadata = File().apply {
                name = localFile.name
                parents = listOf(folderId)
                mimeType = "application/pdf"
            }

            // Subir a Drive directamente desde archivo local
            val mediaContent = FileContent("application/pdf", localFile)

            val file = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink")
                .execute()

            Log.d(TAG, "[OK] PDF subido a Drive: ${file.name} (${file.id})")

            Result.success(
                DriveUploadResult(
                    success = true,
                    fileId = file.id,
                    fileName = file.name,
                    webViewLink = file.webViewLink,
                    folderPath = "Google Drive"
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "âŒ Error subiendo PDF: ${e.message}", e)
            Result.success(
                DriveUploadResult(
                    success = false,
                    fileName = localFile.name,
                    folderPath = "",
                    error = e.message ?: "Error desconocido"
                )
            )
        }
    }

    /**
     * Lista archivos en una carpeta
     *
     * @param drive Servicio de Drive
     * @param folderId ID de la carpeta
     * @return Lista de archivos
     */
    suspend fun listFilesInFolder(
        drive: Drive,
        folderId: String
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val result = drive.files().list()
                .setQ("'$folderId' in parents and trashed=false")
                .setSpaces("drive")
                .setFields("files(id, name, mimeType, webViewLink, createdTime)")
                .execute()

            Log.d(TAG, "[OK] Archivos listados: ${result.files.size}")
            Result.success(result.files)

        } catch (e: Exception) {
            Log.e(TAG, "âŒ Error listando archivos: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Elimina un archivo de Drive
     *
     * @param drive Servicio de Drive
     * @param fileId ID del archivo
     */
    suspend fun deleteFile(
        drive: Drive,
        fileId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            drive.files().delete(fileId).execute()
            Log.d(TAG, "[OK] Archivo eliminado: $fileId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "âŒ Error eliminando archivo: ${e.message}", e)
            Result.failure(e)
        }
    }
}