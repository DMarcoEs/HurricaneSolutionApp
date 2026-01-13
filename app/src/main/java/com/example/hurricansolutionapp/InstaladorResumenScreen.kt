package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstaladorResumenScreen(
    folio: String,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var instaladorDatos by remember { mutableStateOf<InstaladorDatos?>(null) }
    var medidas by remember { mutableStateOf<List<MedidaInstalador>>(emptyList()) }
    var medidasOriginales by remember { mutableStateOf<List<MedidaInstalador>>(emptyList()) }  // âœ… NUEVO

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val successColor = Color(0xFF10B981)

    val userId = remember { SessionManager.getUserId(context) }
    val userName = remember { SessionManager.getNombre(context) }
    val userRole = remember { SessionManager.getRole(context) }

    suspend fun enqueueForLater(pdfFile: java.io.File, datos: InstaladorDatos) {
        try {
            val pending = InstaladorPendingInsert(
                cotizacionId = datos.cotizacionId?.toString() ?: datos.folio,
                folio = datos.folio,
                filePath = pdfFile.absolutePath,
                fileName = pdfFile.name,
                clienteNombre = datos.nombreCliente,
                createdById = userId,
                createdByNombre = userName
            )
            InstaladorRepository.enqueuePending(pending)
        } catch (e: Exception) {
            android.util.Log.e("InstaladorResumen", "Error encolando PDF: ${e.message}")
        }
    }

    LaunchedEffect(folio) {
        scope.launch {
            try {
                isLoading = true
                val result = InstaladorRepository.getDatosCompletosByFolio(folio)
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    if (data != null) {
                        instaladorDatos = data.first; medidas = data.second
                    } else {
                        error = "No se encontraron datos"
                    }
                } else {
                    error = result.exceptionOrNull()?.message
                }
            } catch (e: Exception) {
                error = e.message
            } finally {
                isLoading = false
            }
        }
    }

    val areaTotal = remember(medidas) { medidas.sumOf { it.alto * it.ancho } }

    fun guardarRectificacionYGenerarPDF() {
        if (instaladorDatos == null) return
        scope.launch {
            isSaving = true
            try {
                // 0. RECARGAR datos frescos de la BD para asegurar que tenemos los valores actualizados
                android.util.Log.d("InstaladorResumen", "Recargando datos frescos de BD...")
                val freshDataResult = InstaladorRepository.getDatosCompletosByFolio(folio)
                if (freshDataResult.isSuccess) {
                    val freshData = freshDataResult.getOrNull()
                    if (freshData != null) {
                        instaladorDatos = freshData.first
                        medidas = freshData.second
                        android.util.Log.d(
                            "InstaladorResumen",
                            "Datos recargados: rectificadas=${instaladorDatos?.rectificadas}, tipoPropiedad=${instaladorDatos?.tipoPropiedad}"
                        )
                    }
                }

                // 1. Actualizar instalador_datos con info del instalador
                val updateResult = InstaladorRepository.updateDatos(
                    id = instaladorDatos!!.id,
                    update = InstaladorDatosUpdate(
                        instaladorId = userId,
                        instaladorNombre = userName,
                        fechaRectificacion = java.time.OffsetDateTime.now().toString()
                    )
                )

                if (updateResult.isFailure) {
                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                    isSaving = false
                    return@launch
                }

                // Recargar una vez mas para tener datos 100% actualizados
                val finalDataResult = InstaladorRepository.getDatosCompletosByFolio(folio)
                if (finalDataResult.isSuccess) {
                    val finalData = finalDataResult.getOrNull()
                    if (finalData != null) {
                        instaladorDatos = finalData.first
                        medidas = finalData.second
                    }
                }

                // 2. Generar PDF de instalacion
                android.util.Log.d("InstaladorResumen", "Generando PDF de instalacion...")

                // Crear cotizacion dummy para el generador
                val cotizacionDummy = Cotizacion(
                    id = instaladorDatos!!.cotizacionId ?: 0,
                    folio = instaladorDatos!!.folio,
                    clienteNombre = instaladorDatos!!.nombreCliente,
                    clienteTelefono = instaladorDatos!!.telefonoCliente ?: "",
                    ubicacion = instaladorDatos!!.getDireccionSegura(),
                    ciudad = instaladorDatos!!.getCiudadSegura(),
                    especialista = instaladorDatos!!.getEspecialistaNombreSeguro(),
                    fecha = "",
                    producto = TipoProducto.HS875,
                    ventanas = medidas.map { m ->
                        Ventana(
                            zona = m.getZonaSegura(),  // âœ… NUEVO
                            descripcion = m.descripcion,
                            alto = m.alto,
                            ancho = m.ancho,
                            precioM2 = 0.0,
                            adecuacion = if (m.requiereAdecuacion) m.getAdecuacionDetalleSeguro() else "No",
                            tipoMontaje = m.getTipoMontajeSeguro()
                        )
                    }
                )

                val pdfFile = PdfInstaladorGenerator.generarPdfOrdenInstalacion(
                    context = context,
                    cotizacion = cotizacionDummy,
                    sistemaSeleccionado = instaladorDatos!!.sistemaSeleccionado,
                    instaladorDatos = instaladorDatos,
                    medidasRectificadas = medidas
                )

                if (pdfFile != null) {
                    android.util.Log.d(
                        "InstaladorResumen",
                        "âœ“ PDF generado: ${pdfFile.absolutePath}"
                    )

                    // 3. Subir a Google Drive automaticamente si hay conexion
                    if (isOnline(context) && DriveAuthManager.isAuthenticated(context)) {
                        android.util.Log.d("InstaladorResumen", "Subiendo a Google Drive...")

                        val uploadResult = GoogleDriveRepository.uploadPdfToStructuredFolder(
                            context = context,
                            localPdfFile = pdfFile,
                            userName = userName,
                            userRole = userRole
                        )

                        if (uploadResult.isSuccess && uploadResult.getOrNull()?.success == true) {
                            Toast.makeText(
                                context,
                                "âœ“ Rectificacion guardada y PDF subido a Drive",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            // Encolar para despues
                            enqueueForLater(pdfFile, instaladorDatos!!)
                            Toast.makeText(
                                context,
                                "âœ“ Guardado. PDF pendiente de subir a Drive",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        // Encolar para despues
                        enqueueForLater(pdfFile, instaladorDatos!!)
                        Toast.makeText(
                            context,
                            "âœ“ Guardado. PDF se subira cuando haya conexion",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "âœ“ Guardado (error generando PDF)",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

                onNavigateToHome()

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("InstaladorResumen", "Error: ${e.message}", e)
            } finally {
                isSaving = false
            }
        }
    }


    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBarWithDivider(
                title = "Resumen - $folio",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = textPrimary, strokeWidth = 2.dp) }

                error != null -> Column(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline,
                        null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(error ?: "Error", color = textMuted, textAlign = TextAlign.Center)
                }

                instaladorDatos != null -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    val datos = instaladorDatos!!

                    // Cliente
                    Surface(color = cardBg) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "CLIENTE",
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                datos.nombreCliente,
                                color = textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val direccion = datos.getDireccionSegura()
                            if (direccion.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.LocationOn,
                                        null,
                                        tint = textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(direccion, color = textMuted, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(1.dp))

                    // Datos instalacion
                    Surface(color = cardBg) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "DATOS DE INSTALACION",
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Sistema",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    ); Text(
                                    datos.sistemaSeleccionado.getSistemaDisplayName(),
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Tipo Propiedad",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    ); Text(
                                    datos.getTipoPropiedadSegura().ifBlank { "-" },
                                    color = textPrimary,
                                    fontSize = 14.sp
                                )
                                }
                            }
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Nivel",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    ); Text(
                                    datos.getNivelSeguro().ifBlank { "-" },
                                    color = textPrimary,
                                    fontSize = 14.sp
                                )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Andamios",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    ); Text(
                                    if (datos.requiereAndamios) "Si" else "No",
                                    color = textPrimary,
                                    fontSize = 14.sp
                                )
                                }
                            }
                            val fechaSol = datos.getFechaSolicitadaSegura()
                            if (fechaSol.isNotBlank()) {
                                Column {
                                    Text(
                                        "Fecha Solicitada",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    ); Text(fechaSol, color = textPrimary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(1.dp))

                    // Medidas
                    Surface(color = cardBg) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "MEDIDAS (${medidas.size})",
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Surface(
                                    color = successColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "Área Total: ${String.format("%.2f", areaTotal)} m2",
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 6.dp
                                        ),
                                        color = successColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            HorizontalDivider(color = border.copy(alpha = 0.5f))
                            medidas.forEachIndexed { index, medida ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "#${index + 1}",
                                                color = textMuted,
                                                fontSize = 11.sp
                                            )
                                            val zona = medida.getZonaSegura()
                                            if (zona.isNotBlank()) Text(
                                                " â€¢ $zona",
                                                color = textMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            medida.descripcion,
                                            color = textPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "${
                                                String.format(
                                                    "%.2f",
                                                    medida.ancho
                                                )
                                            } x ${
                                                String.format(
                                                    "%.2f",
                                                    medida.alto
                                                )
                                            } m â€¢ ${medida.getTipoMontajeSeguro()}",
                                            color = textMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        "${String.format("%.2f", medida.alto * medida.ancho)} mÂ²",
                                        color = textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (index < medidas.size - 1) HorizontalDivider(
                                    color = border.copy(
                                        alpha = 0.3f
                                    )
                                )
                            }
                        }
                    }

                    // Observaciones
                    val obs = datos.getObservacionesSeguras()
                    if (obs.isNotBlank()) {
                        Spacer(Modifier.height(1.dp))
                        Surface(color = cardBg) {
                            Column(Modifier.padding(20.dp)) {
                                Text(
                                    "OBSERVACIONES",
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(obs, color = textPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    // Boton guardar
                    Button(
                        onClick = { guardarRectificacionYGenerarPDF() },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = successColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSaving) CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        else {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Guardar y Generar PDF",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}