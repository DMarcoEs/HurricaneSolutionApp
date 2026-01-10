package com.example.hurricansolutionapp

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════════
// MODELOS PARA TABLA: instalador_datos
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class InstaladorDatos(
    val id: String = "",
    @SerialName("cotizacion_id") val cotizacionId: Long? = null,
    val folio: String = "",
    @SerialName("nombre_cliente") val nombreCliente: String = "",
    @SerialName("telefono_cliente") val telefonoCliente: String? = null,
    val direccion: String? = null,
    val ciudad: String? = null,
    val colonia: String? = null,
    @SerialName("sistema_seleccionado") val sistemaSeleccionado: String = "",
    @SerialName("tipo_propiedad") val tipoPropiedad: String? = null,
    val nivel: String? = null,
    @SerialName("requiere_andamios") val requiereAndamios: Boolean = false,
    @SerialName("fecha_solicitada") val fechaSolicitada: String? = null,
    val observaciones: String? = null,
    @SerialName("especialista_id") val especialistaId: String? = null,
    @SerialName("especialista_nombre") val especialistaNombre: String? = null,
    @SerialName("instalador_id") val instaladorId: String? = null,
    @SerialName("instalador_nombre") val instaladorNombre: String? = null,
    val rectificadas: Boolean = false,
    @SerialName("fecha_rectificacion") val fechaRectificacion: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun getDireccionSegura(): String = direccion ?: ""
    fun getCiudadSegura(): String = ciudad ?: ""
    fun getColoniaSegura(): String = colonia ?: ""
    fun getTipoPropiedadSegura(): String = tipoPropiedad ?: ""
    fun getNivelSeguro(): String = nivel ?: ""
    fun getFechaSolicitadaSegura(): String = fechaSolicitada ?: ""
    fun getObservacionesSeguras(): String = observaciones ?: ""
    fun getEspecialistaNombreSeguro(): String = especialistaNombre ?: ""
    fun getInstaladorNombreSeguro(): String = instaladorNombre ?: ""
}

@Serializable
data class InstaladorDatosInsert(
    @SerialName("cotizacion_id") val cotizacionId: Long? = null,
    val folio: String,
    @SerialName("nombre_cliente") val nombreCliente: String,
    @SerialName("telefono_cliente") val telefonoCliente: String? = null,
    val direccion: String? = null,
    val ciudad: String? = null,
    val colonia: String? = null,
    @SerialName("sistema_seleccionado") val sistemaSeleccionado: String,
    @SerialName("tipo_propiedad") val tipoPropiedad: String? = null,
    val nivel: String? = null,
    @SerialName("requiere_andamios") val requiereAndamios: Boolean = false,
    @SerialName("fecha_solicitada") val fechaSolicitada: String? = null,
    val observaciones: String? = null,
    @SerialName("especialista_id") val especialistaId: String,
    @SerialName("especialista_nombre") val especialistaNombre: String,
    @SerialName("instalador_id") val instaladorId: String? = null,
    @SerialName("instalador_nombre") val instaladorNombre: String? = null
)

@Serializable
data class InstaladorDatosUpdate(
    @SerialName("tipo_propiedad")
    val tipoPropiedad: String? = null,

    val nivel: String? = null,

    @SerialName("requiere_andamios")
    val requiereAndamios: Boolean? = null,

    @SerialName("fecha_solicitada")
    val fechaSolicitada: String? = null,

    val observaciones: String? = null,

    @SerialName("instalador_id")
    val instaladorId: String? = null,

    @SerialName("instalador_nombre")
    val instaladorNombre: String? = null,

    val rectificadas: Boolean? = null,

    @SerialName("fecha_rectificacion")
    val fechaRectificacion: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// MODELOS PARA TABLA: medidas_instalador
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class MedidaInstalador(
    val id: String = "",
    @SerialName("instalador_datos_id") val instaladorDatosId: String = "",
    val zona: String? = null,
    val descripcion: String = "",
    val cantidad: Int = 1,
    // Supabase devuelve NUMERIC como Double en JSON
    val alto: Double = 0.0,
    val ancho: Double = 0.0,
    @SerialName("tipo_montaje") val tipoMontaje: String? = null,
    @SerialName("requiere_adecuacion") val requiereAdecuacion: Boolean = false,
    @SerialName("adecuacion_detalle") val adecuacionDetalle: String? = null,
    val orden: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun getArea(): Double = alto * ancho * cantidad
    fun getZonaSegura(): String = zona ?: ""
    fun getTipoMontajeSeguro(): String = tipoMontaje ?: "Flush Mount"
    fun getAdecuacionDetalleSeguro(): String = adecuacionDetalle ?: ""
}

@Serializable
data class MedidaInstaladorInsert(
    @SerialName("instalador_datos_id") val instaladorDatosId: String,
    val zona: String? = null,
    val descripcion: String,
    val cantidad: Int = 1,
    // Insert sigue usando Double - Supabase lo acepta
    val alto: Double,
    val ancho: Double,
    @SerialName("tipo_montaje") val tipoMontaje: String? = "Flush Mount",
    @SerialName("requiere_adecuacion") val requiereAdecuacion: Boolean = false,
    @SerialName("adecuacion_detalle") val adecuacionDetalle: String? = null,
    val orden: Int = 0
)

// ═══════════════════════════════════════════════════════════════════════════════
// MODELOS PARA TABLA: instalador_pending_uploads
// ═══════════════════════════════════════════════════════════════════════════════

@Serializable
data class InstaladorPendingUpload(
    val id: String = "",
    @SerialName("cotizacion_id") val cotizacionId: String = "",
    val folio: String = "",
    @SerialName("file_path") val filePath: String = "",
    @SerialName("file_name") val fileName: String = "",
    @SerialName("cliente_nombre") val clienteNombre: String = "",
    val status: String = InstaladorUploadStatus.PENDING,
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("retry_count") val retryCount: Int = 0,
    @SerialName("drive_file_id") val driveFileId: String? = null,
    @SerialName("drive_folder_path") val driveFolderPath: String? = null,
    @SerialName("created_by_id") val createdById: String? = null,
    @SerialName("created_by_nombre") val createdByNombre: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String? = null
) {
    fun getErrorMessageSeguro(): String = errorMessage ?: ""
}

@Serializable
data class InstaladorPendingInsert(
    @SerialName("cotizacion_id") val cotizacionId: String,
    val folio: String,
    @SerialName("file_path") val filePath: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("cliente_nombre") val clienteNombre: String,
    @SerialName("created_by_id") val createdById: String,
    @SerialName("created_by_nombre") val createdByNombre: String
)

// ═══════════════════════════════════════════════════════════════════════════════
// CONSTANTES
// ═══════════════════════════════════════════════════════════════════════════════

object InstaladorUploadStatus {
    const val PENDING = "PENDING"
    const val UPLOADING = "UPLOADING"
    const val DONE = "DONE"
    const val ERROR = "ERROR"
}

object TiposPropiedadInstalador {
    val opciones = listOf(
        "Casa",
        "Departamento",
        "Oficina",
        "Local Comercial",
        "Bodega",
        "Edificio",
        "Condominio",
        "Otro"
    )
}

object NivelesInstalador {
    val opciones = listOf(
        "Planta Baja",
        "1er Piso",
        "2do Piso",
        "3er Piso",
        "4to Piso o más",
        "Sótano",
        "Mezanine"
    )
}

fun String.getSistemaDisplayName(): String = when (this.uppercase()) {
    "HS875" -> "HS-875"
    "HS1250" -> "HS-1250"
    "HS1500" -> "HS-1500"
    else -> this
}