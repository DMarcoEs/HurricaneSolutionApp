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

    suspend fun createFolder(
        drive: Drive,
        folderName: String,
        parentFolderId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Primero verificar si ya existe
            val existingFolder = findFolderByName(drive, folderName, parentFolderId)
            if (existingFolder != null) {
                Log.d(TAG, "Carpeta ya existe: $folderName (${existingFolder.id})")
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
            Log.e(TAG, "Error creando carpeta '$folderName': ${e.message}", e)
            Result.failure(e)
        }
    }

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

    suspend fun uploadPdfFromLocalFile(
        drive: Drive,
        localFile: java.io.File,
        folderId: String
    ): Result<DriveUploadResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando subida: ${localFile.name}")

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

            Log.d(TAG, "Archivo local: ${localFile.length()} bytes")

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
            Log.e(TAG, "Error subiendo PDF: ${e.message}", e)
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
            Log.e(TAG, "Error listando archivos: ${e.message}", e)
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
            Log.e(TAG, "Error eliminando archivo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Busca un archivo por nombre en una carpeta específica
     *
     * @param drive Servicio de Drive
     * @param fileName Nombre del archivo a buscar
     * @param folderId ID de la carpeta donde buscar
     * @return File de Drive o null si no existe
     */
    suspend fun findFileByName(
        drive: Drive,
        fileName: String,
        folderId: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val query = buildString {
                append("name='$fileName'")
                append(" and '$folderId' in parents")
                append(" and trashed=false")
                append(" and mimeType='application/pdf'")
            }

            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute()

            val file = result.files.firstOrNull()
            if (file != null) {
                Log.d(TAG, "Archivo encontrado: ${file.name} (${file.id})")
            }
            file

        } catch (e: Exception) {
            Log.e(TAG, "Error buscando archivo '$fileName': ${e.message}", e)
            null
        }
    }

    /**
     * Busca archivos que contengan un FOLIO específico en el nombre
     * Útil para encontrar versiones anteriores de una cotización
     *
     * @param drive Servicio de Drive
     * @param folio Folio de la cotización (ej: "FL007")
     * @param folderId ID de la carpeta donde buscar
     * @return Lista de archivos que contienen el folio
     */
    suspend fun findFilesByFolio(
        drive: Drive,
        folio: String,
        folderId: String
    ): List<File> = withContext(Dispatchers.IO) {
        try {
            val query = buildString {
                append("name contains '$folio'")
                append(" and '$folderId' in parents")
                append(" and trashed=false")
                append(" and mimeType='application/pdf'")
            }

            val result = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, createdTime)")
                .execute()

            val files = result.files ?: emptyList()
            Log.d(TAG, "Archivos encontrados con folio '$folio': ${files.size}")
            files

        } catch (e: Exception) {
            Log.e(TAG, "Error buscando archivos con folio '$folio': ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Sube un archivo PDF a Google Drive, reemplazando si ya existe uno con el mismo FOLIO
     * Elimina TODAS las versiones anteriores del mismo folio antes de subir
     *
     * @param drive Servicio de Drive autenticado
     * @param localFile Archivo PDF local
     * @param folderId ID de la carpeta destino
     * @param folio Folio de la cotización para buscar versiones anteriores
     * @return Resultado con información del archivo subido
     */
    suspend fun uploadPdfWithReplaceByFolio(
        drive: Drive,
        localFile: java.io.File,
        folderId: String,
        folio: String
    ): Result<DriveUploadResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando subida con reemplazo por folio: ${localFile.name} (Folio: $folio)")

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

            // Buscar y eliminar TODAS las versiones anteriores con el mismo folio
            if (folio.isNotBlank()) {
                val existingFiles = findFilesByFolio(drive, folio, folderId)
                if (existingFiles.isNotEmpty()) {
                    Log.d(TAG, "Eliminando ${existingFiles.size} versiones anteriores del folio $folio")
                    existingFiles.forEach { existingFile ->
                        try {
                            drive.files().delete(existingFile.id).execute()
                            Log.d(TAG, "Eliminado: ${existingFile.name}")
                        } catch (e: Exception) {
                            Log.w(TAG, "No se pudo eliminar ${existingFile.name}: ${e.message}")
                        }
                    }
                }
            }

            Log.d(TAG, "Subiendo archivo: ${localFile.length()} bytes")

            // Crear metadata del archivo en Drive
            val fileMetadata = File().apply {
                name = localFile.name
                parents = listOf(folderId)
                mimeType = "application/pdf"
            }

            // Subir a Drive
            val mediaContent = FileContent("application/pdf", localFile)

            val file = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink")
                .execute()

            Log.d(TAG, "[OK] PDF subido/reemplazado en Drive: ${file.name} (${file.id})")

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
            Log.e(TAG, "Error subiendo PDF: ${e.message}", e)
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
     * Sube un archivo PDF a Google Drive, reemplazando si ya existe uno con el mismo nombre exacto
     * (Versión legacy - usar uploadPdfWithReplaceByFolio para mejor manejo)
     */
    suspend fun uploadPdfWithReplace(
        drive: Drive,
        localFile: java.io.File,
        folderId: String
    ): Result<DriveUploadResult> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando subida con reemplazo por nombre: ${localFile.name}")

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

            // Buscar si ya existe un archivo con el mismo nombre
            val existingFile = findFileByName(drive, localFile.name, folderId)
            if (existingFile != null) {
                Log.d(TAG, "Archivo existente encontrado, eliminando: ${existingFile.id}")
                try {
                    drive.files().delete(existingFile.id).execute()
                    Log.d(TAG, "[OK] Archivo anterior eliminado")
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudo eliminar archivo anterior: ${e.message}")
                }
            }

            Log.d(TAG, "Subiendo archivo: ${localFile.length()} bytes")

            val fileMetadata = File().apply {
                name = localFile.name
                parents = listOf(folderId)
                mimeType = "application/pdf"
            }

            val mediaContent = FileContent("application/pdf", localFile)

            val file = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink")
                .execute()

            Log.d(TAG, "[OK] PDF subido/reemplazado en Drive: ${file.name} (${file.id})")

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
            Log.e(TAG, "Error subiendo PDF: ${e.message}", e)
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
}