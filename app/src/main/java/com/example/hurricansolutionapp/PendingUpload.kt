package com.example.hurricansolutionapp

data class PendingUpload(
    val id: String,                     // UUID
    val cotizacionId: String,           // Folio de la cotización
    val clienteNombre: String? = null,
    val createdByNombre: String? = null,
    val filePath: String,               // Ruta local del PDF
    val createdAt: Long = System.currentTimeMillis(),
    // Estado Supabase
    val status: String = "PENDING",     // PENDING | UPLOADING | DONE | ERROR
    val lastError: String? = null,
    // Estado Drive (nuevo)
    val driveStatus: String = "PENDING", // PENDING | UPLOADING | DONE | ERROR
    val driveError: String? = null
)