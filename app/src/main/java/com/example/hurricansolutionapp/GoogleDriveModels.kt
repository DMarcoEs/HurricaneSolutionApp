package com.example.hurricansolutionapp

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Resultado de subida a Google Drive
 */
data class DriveUploadResult(
    val success: Boolean,
    val fileId: String? = null,
    val fileName: String,
    val webViewLink: String? = null,
    val folderPath: String,
    val error: String? = null
)

/**
 * Información de archivo pendiente de subir a Drive
 */
@Serializable
data class DrivePendingUpload(
    val id: String,

    @SerialName("pdf_filename")
    val pdfFilename: String,

    @SerialName("supabase_url")
    val supabaseUrl: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("user_name")
    val userName: String,

    @SerialName("user_role")
    val userRole: String,

    @SerialName("folio")
    val folio: String,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("retry_count")
    val retryCount: Int = 0,

    @SerialName("last_error")
    val lastError: String? = null,

    @SerialName("target_folder_path")
    val targetFolderPath: String? = null
)

/**
 * Request para insertar pendiente de Drive
 */
@Serializable
data class DrivePendingUploadInsert(
    @SerialName("pdf_filename")
    val pdfFilename: String,

    @SerialName("supabase_url")
    val supabaseUrl: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("user_name")
    val userName: String,

    @SerialName("user_role")
    val userRole: String,

    @SerialName("folio")
    val folio: String,

    @SerialName("target_folder_path")
    val targetFolderPath: String
)

/**
 * Información de carpeta en Drive
 */
data class DriveFolderInfo(
    val folderId: String,
    val folderName: String,
    val folderPath: String
)