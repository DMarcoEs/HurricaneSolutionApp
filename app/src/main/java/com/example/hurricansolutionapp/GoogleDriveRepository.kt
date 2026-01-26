package com.example.hurricansolutionapp

import android.content.Context
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Repositorio para gestión de Google Drive
 * Maneja la estructura de carpetas y lógica de subida
 */
object GoogleDriveRepository {

    private const val TAG = ApiConfig.LOG_TAG_DRIVE

    suspend fun uploadPdfToStructuredFolder(
        context: Context,
        localPdfFile: java.io.File,
        userName: String,
        userRole: String
    ): Result<DriveUploadResult> {
        return try {
            // 1. Verificar autenticación
            if (!DriveAuthManager.isAuthenticated(context)) {
                return Result.success(
                    DriveUploadResult(
                        success = false,
                        fileName = localPdfFile.name,
                        folderPath = "",
                        error = "No autenticado con Google Drive"
                    )
                )
            }

            // 2. Obtener servicio de Drive
            val drive = DriveAuthManager.getDriveService(context)
                ?: return Result.success(
                    DriveUploadResult(
                        success = false,
                        fileName = localPdfFile.name,
                        folderPath = "",
                        error = "No se pudo obtener servicio de Drive"
                    )
                )

            // 3. Crear/obtener estructura de carpetas
            val folderResult = createFolderStructure(drive, userName, userRole)

            if (folderResult.isFailure) {
                return Result.success(
                    DriveUploadResult(
                        success = false,
                        fileName = localPdfFile.name,
                        folderPath = "",
                        error = "Error creando carpetas: ${folderResult.exceptionOrNull()?.message}"
                    )
                )
            }

            val folderInfo = folderResult.getOrNull()!!

            // 4. Subir PDF
            val uploadResult = GoogleDriveApi.uploadPdfFromLocalFile(
                drive = drive,
                localFile = localPdfFile,
                folderId = folderInfo.folderId
            )

            if (uploadResult.isFailure) {
                return Result.success(
                    DriveUploadResult(
                        success = false,
                        fileName = localPdfFile.name,
                        folderPath = folderInfo.folderPath,
                        error = uploadResult.exceptionOrNull()?.message
                    )
                )
            }

            val result = uploadResult.getOrNull()!!

            Log.d(TAG, "[OK] PDF subido exitosamente: $localPdfFile.name ’ ${folderInfo.folderPath}")

            Result.success(result.copy(folderPath = folderInfo.folderPath))

        } catch (e: Exception) {
            Log.e(TAG, "Error en uploadPdfToStructuredFolder: ${e.message}", e)
            Result.success(
                DriveUploadResult(
                    success = false,
                    fileName = localPdfFile.name,
                    folderPath = "",
                    error = e.message ?: "Error desconocido"
                )
            )
        }
    }

    /**
     * Crea la estructura de carpetas necesaria dentro de la carpeta compartida
     *
     * [Carpeta Compartida]/[Rol]/[Usuario]/[Año]/[Mes]/
     *
     * @param drive Servicio de Drive
     * @param userName Nombre del usuario
     * @param userRole Rol del usuario
     * @return Información de la carpeta final
     */
    private suspend fun createFolderStructure(
        drive: com.google.api.services.drive.Drive,
        userName: String,
        userRole: String
    ): Result<DriveFolderInfo> {
        return try {
            // 1. Usar carpeta compartida como raíz (ya existe, no crear)
            val rootId = ApiConfig.DRIVE_SHARED_FOLDER_ID

            Log.d(TAG, "sando carpeta compartida: ${ApiConfig.DRIVE_ROOT_FOLDER}")

            // 2. Carpeta de rol (Admins / Especialistas)
            val roleFolderName = when (userRole) {
                "ADMIN" -> "Admins"
                "SPECIALIST" -> "Especialistas"
                "INSTALLER" -> "Instaladores"
                else -> "Otros"
            }

            val roleResult = GoogleDriveApi.createFolder(
                drive = drive,
                folderName = roleFolderName,
                parentFolderId = rootId
            )

            if (roleResult.isFailure) {
                return Result.failure(roleResult.exceptionOrNull()!!)
            }

            val roleId = roleResult.getOrNull()!!

            // 3. Carpeta del usuario
            val userResult = GoogleDriveApi.createFolder(
                drive = drive,
                folderName = userName,
                parentFolderId = roleId
            )

            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull()!!)
            }

            val userId = userResult.getOrNull()!!

            // 4. Carpeta del año (2026)
            val now = LocalDateTime.now()
            val year = now.year.toString()

            val yearResult = GoogleDriveApi.createFolder(
                drive = drive,
                folderName = year,
                parentFolderId = userId
            )

            if (yearResult.isFailure) {
                return Result.failure(yearResult.exceptionOrNull()!!)
            }

            val yearId = yearResult.getOrNull()!!

            // 5. Carpeta del mes (Enero, Febrero, etc.)
            val monthName = now.format(
                DateTimeFormatter.ofPattern("MMMM", Locale("es", "ES"))
            ).replaceFirstChar { it.uppercase() }

            val monthResult = GoogleDriveApi.createFolder(
                drive = drive,
                folderName = monthName,
                parentFolderId = yearId
            )

            if (monthResult.isFailure) {
                return Result.failure(monthResult.exceptionOrNull()!!)
            }

            val monthId = monthResult.getOrNull()!!

            // Path completo
            val folderPath = "${ApiConfig.DRIVE_ROOT_FOLDER}/$roleFolderName/$userName/$year/$monthName"

            Result.success(
                DriveFolderInfo(
                    folderId = monthId,
                    folderName = monthName,
                    folderPath = folderPath
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error creando estructura de carpetas: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verifica si un PDF ya fue subido a Drive
     *
     * @param context Contexto
     * @param fileName Nombre del archivo
     * @param userName Nombre del usuario
     * @param userRole Rol del usuario
     * @return true si ya existe
     */
    suspend fun isPdfAlreadyUploaded(
        context: Context,
        fileName: String,
        userName: String,
        userRole: String
    ): Boolean {
        return try {
            val drive = DriveAuthManager.getDriveService(context) ?: return false

            val folderResult = createFolderStructure(drive, userName, userRole)
            if (folderResult.isFailure) return false

            val folderInfo = folderResult.getOrNull() ?: return false

            val filesResult = GoogleDriveApi.listFilesInFolder(drive, folderInfo.folderId)
            if (filesResult.isFailure) return false

            val files = filesResult.getOrNull() ?: return false

            files.any { it.name == fileName }

        } catch (e: Exception) {
            Log.e(TAG, "Error verificando archivo: ${e.message}", e)
            false
        }
    }
}