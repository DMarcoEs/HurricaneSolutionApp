package com.example.hurricansolutionapp

data class PendingUpload(
    val id: String,            // UUID
    val cotizacionId: String,   // tu id local
    val filePath: String,       // ruta local del PDF
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING | UPLOADING | DONE | ERROR
    val lastError: String? = null
)
