package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Resumen de Instalación - Diseño Stitch
 * Similar al ResumenScreen del Especialista pero para el Instalador
 * Con funciones de rectificación y aprobación de medidas
 */
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

    // Estado de aprobación/rectificación por medida
    var medidasEstado by remember { mutableStateOf<Map<String, MedidaEstado>>(emptyMap()) }

    // Diálogo de edición de medida
    var showEditDialog by remember { mutableStateOf<MedidaInstalador?>(null) }
    var editAlto by remember { mutableStateOf("") }
    var editAncho by remember { mutableStateOf("") }

    // Colores Stitch
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val headerBg = if (isDarkMode) Color(0xFF0A0A0A) else Color(0xFFF9FAFB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val accentBorder = if (isDarkMode) Color.White else Color.Black

    val userId = remember { SessionManager.getUserId(context) }
    val userName = remember { SessionManager.getNombre(context) }
    val userRole = remember { SessionManager.getRole(context) }

    // Cargar datos
    LaunchedEffect(folio) {
        scope.launch {
            try {
                isLoading = true
                val result = InstaladorRepository.getDatosCompletosByFolio(folio)
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    if (data != null) {
                        instaladorDatos = data.first
                        medidas = data.second
                        // Inicializar estado de medidas
                        medidasEstado = medidas.associate { it.id to MedidaEstado.PENDIENTE }
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

    // Contadores de estado
    val rectificadas = medidasEstado.values.count { it == MedidaEstado.RECTIFICADO }
    val aprobadas = medidasEstado.values.count { it == MedidaEstado.APROBADO }
    val pendientes = medidasEstado.values.count { it == MedidaEstado.PENDIENTE }
    val todasValidadas = pendientes == 0 && medidas.isNotEmpty()

    // Función para guardar y generar PDF
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

    fun enviarValidacion() {
        if (instaladorDatos == null || !todasValidadas) return
        scope.launch {
            isSaving = true
            try {
                // 1. Marcar como rectificadas
                val updateResult = InstaladorRepository.updateDatos(
                    id = instaladorDatos!!.id,
                    update = InstaladorDatosUpdate(
                        instaladorId = userId,
                        instaladorNombre = userName,
                        rectificadas = true,
                        fechaRectificacion = java.time.OffsetDateTime.now().toString()
                    )
                )

                if (updateResult.isFailure) {
                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                    isSaving = false
                    return@launch
                }

                // 2. Recargar datos actualizados
                val finalDataResult = InstaladorRepository.getDatosCompletosByFolio(folio)
                if (finalDataResult.isSuccess) {
                    val finalData = finalDataResult.getOrNull()
                    if (finalData != null) {
                        instaladorDatos = finalData.first
                        medidas = finalData.second
                    }
                }

                // 3. Generar PDF de instalación
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
                            zona = m.getZonaSegura(),
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
                    if (isOnline(context) && DriveAuthManager.isAuthenticated(context)) {
                        val uploadResult = GoogleDriveRepository.uploadPdfToStructuredFolder(
                            context = context,
                            localPdfFile = pdfFile,
                            userName = userName,
                            userRole = userRole
                        )

                        if (uploadResult.isSuccess && uploadResult.getOrNull()?.success == true) {
                            Toast.makeText(context, "✓ Validación enviada y PDF subido", Toast.LENGTH_LONG).show()
                        } else {
                            enqueueForLater(pdfFile, instaladorDatos!!)
                            Toast.makeText(context, "✓ Validación enviada. PDF pendiente", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        enqueueForLater(pdfFile, instaladorDatos!!)
                        Toast.makeText(context, "✓ Validación enviada. PDF se subirá después", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "✓ Validación enviada", Toast.LENGTH_SHORT).show()
                }

                onNavigateToHome()

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
            }
        }
    }

    // Diálogo de edición de medida
    if (showEditDialog != null) {
        val medida = showEditDialog!!
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            containerColor = surface,
            title = { Text("Rectificar Medida", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(medida.descripcion, color = textPrimary, fontWeight = FontWeight.Medium)
                    Text("Medida original: ${String.format("%.2f", medida.alto)}m x ${String.format("%.2f", medida.ancho)}m", color = textMuted, fontSize = 12.sp)

                    OutlinedTextField(
                        value = editAlto,
                        onValueChange = { editAlto = it },
                        label = { Text("Alto (m)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = accentBorder,
                            unfocusedBorderColor = border
                        )
                    )

                    OutlinedTextField(
                        value = editAncho,
                        onValueChange = { editAncho = it },
                        label = { Text("Ancho (m)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = accentBorder,
                            unfocusedBorderColor = border
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nuevoAlto = editAlto.toDoubleOrNull() ?: medida.alto
                        val nuevoAncho = editAncho.toDoubleOrNull() ?: medida.ancho

                        scope.launch {
                            // Crear objeto MedidaInstaladorInsert con los valores actualizados
                            val medidaActualizada = MedidaInstaladorInsert(
                                instaladorDatosId = medida.instaladorDatosId,
                                zona = medida.zona,
                                descripcion = medida.descripcion,
                                cantidad = medida.cantidad,
                                alto = nuevoAlto,
                                ancho = nuevoAncho,
                                tipoMontaje = medida.tipoMontaje,
                                requiereAdecuacion = medida.requiereAdecuacion,
                                adecuacionDetalle = medida.adecuacionDetalle,
                                orden = medida.orden
                            )
                            InstaladorRepository.updateMedida(medida.id, medidaActualizada)
                            // Actualizar lista local
                            medidas = medidas.map {
                                if (it.id == medida.id) it.copy(alto = nuevoAlto, ancho = nuevoAncho)
                                else it
                            }
                            // Marcar como rectificada
                            medidasEstado = medidasEstado + (medida.id to MedidaEstado.RECTIFICADO)
                        }
                        showEditDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color.White else Color.Black)
                ) {
                    Text("Guardar", color = if (isDarkMode) Color.Black else Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("Cancelar", color = textMuted)
                }
            }
        )
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Resumen de Instalación",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        },
        bottomBar = {
            // Footer fijo
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = surface,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Estado actual
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ESTADO ACTUAL", color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(
                                when {
                                    todasValidadas -> "Todas las medidas validadas"
                                    else -> "$aprobadas Aprobadas, $rectificadas Rectificadas, $pendientes Pendientes"
                                },
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Botón enviar
                    Button(
                        onClick = { enviarValidacion() },
                        enabled = todasValidadas && !isSaving,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color.White else Color.Black,
                            disabledContainerColor = border
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = if (isDarkMode) Color.Black else Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Enviando...", color = if (isDarkMode) Color.Black else Color.White)
                        } else {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("ENVIAR VALIDACIÓN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = textPrimary, strokeWidth = 2.dp)
                    }
                }
                error != null -> {
                    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.ErrorOutline, null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(error ?: "Error", color = textMuted, textAlign = TextAlign.Center)
                    }
                }
                instaladorDatos != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card: Datos del Cliente
                        StitchCardInstalador(
                            title = "DATOS DEL CLIENTE",
                            icon = Icons.Default.Person,
                            isDarkMode = isDarkMode,
                            surface = surface,
                            headerBg = headerBg,
                            border = border,
                            accentBorder = accentBorder,
                            textPrimary = textPrimary,
                            textMuted = textMuted
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                DataRowInstalador("Nombre", instaladorDatos!!.nombreCliente, textMuted, textPrimary)
                                DataRowInstalador("Ciudad", instaladorDatos!!.getCiudadSegura().ifBlank { "-" }, textMuted, textPrimary)
                                DataRowInstalador("Tipo de Sistema", "Sistema ${instaladorDatos!!.sistemaSeleccionado.getSistemaDisplayName()}", textMuted, textPrimary)
                            }
                        }

                        // Card: Aperturas
                        StitchCardInstalador(
                            title = "APERTURAS",
                            icon = Icons.Default.Straighten,
                            isDarkMode = isDarkMode,
                            surface = surface,
                            headerBg = headerBg,
                            border = border,
                            accentBorder = accentBorder,
                            textPrimary = textPrimary,
                            textMuted = textMuted
                        ) {
                            Column {
                                // Área total
                                Box(
                                    modifier = Modifier.fillMaxWidth().background(if (isDarkMode) Color(0xFF1F1F1F) else Color.Black).padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("ÁREA TOTAL A VALIDAR", color = Color(0xFF9CA3AF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text("${String.format("%.2f", areaTotal)} m²", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                                    }
                                }

                                // Header lista
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(headerBg).padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("LISTADO (${medidas.size})", color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Text("Toque para validar", color = textMuted.copy(alpha = 0.7f), fontSize = 10.sp)
                                }

                                // Lista de medidas con scroll interno
                                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                        medidas.forEachIndexed { index, medida ->
                                            MedidaItemInstalador(
                                                index = index + 1,
                                                medida = medida,
                                                estado = medidasEstado[medida.id] ?: MedidaEstado.PENDIENTE,
                                                isDarkMode = isDarkMode,
                                                textPrimary = textPrimary,
                                                textMuted = textMuted,
                                                border = border,
                                                onRectificar = {
                                                    editAlto = String.format("%.2f", medida.alto)
                                                    editAncho = String.format("%.2f", medida.ancho)
                                                    showEditDialog = medida
                                                },
                                                onAprobar = {
                                                    medidasEstado = medidasEstado + (medida.id to MedidaEstado.APROBADO)
                                                }
                                            )
                                            if (index < medidas.lastIndex) {
                                                HorizontalDivider(color = border.copy(alpha = 0.5f))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Aviso
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFCD34D))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.Info, null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                Text(
                                    "Verifique todas las medidas en sitio antes de enviar. Una vez validada, la orden pasará a producción.",
                                    color = Color(0xFF92400E),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(100.dp)) // Espacio para el footer
                    }
                }
            }
        }
    }
}

// Estado de validación de medida
enum class MedidaEstado { PENDIENTE, APROBADO, RECTIFICADO }

@Composable
private fun StitchCardInstalador(
    title: String,
    icon: ImageVector,
    isDarkMode: Boolean,
    surface: Color,
    headerBg: Color,
    border: Color,
    accentBorder: Color,
    textPrimary: Color,
    textMuted: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surface,
        shape = RoundedCornerShape(0.dp),
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accentBorder))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(headerBg).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(icon, null, tint = textMuted, modifier = Modifier.size(18.dp))
                    Text(title, color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                content()
            }
        }
    }
}

@Composable
private fun DataRowInstalador(label: String, value: String, textMuted: Color, textPrimary: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = textMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(value, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun MedidaItemInstalador(
    index: Int,
    medida: MedidaInstalador,
    estado: MedidaEstado,
    isDarkMode: Boolean,
    textPrimary: Color,
    textMuted: Color,
    border: Color,
    onRectificar: () -> Unit,
    onAprobar: () -> Unit
) {
    val area = medida.alto * medida.ancho

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Info de la medida
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6), shape = RoundedCornerShape(4.dp)) {
                    Text(String.format("%02d", index), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(medida.descripcion, color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(6.dp))

            Text(
                "${String.format("%.2f", medida.alto)}m x ${String.format("%.2f", medida.ancho)}m",
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                "${medida.getTipoMontajeSeguro()} | ${String.format("%.2f", area)} m²",
                color = textMuted,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(8.dp))

            // Badge de estado
            when (estado) {
                MedidaEstado.APROBADO -> {
                    Surface(color = if (isDarkMode) Color.White else Color.Black, shape = RoundedCornerShape(20.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = if (isDarkMode) Color.Black else Color.White, modifier = Modifier.size(14.dp))
                            Text("APROBADO", color = if (isDarkMode) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                MedidaEstado.RECTIFICADO -> {
                    Surface(color = if (isDarkMode) Color.White else Color.Black, shape = RoundedCornerShape(20.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Edit, null, tint = if (isDarkMode) Color.Black else Color.White, modifier = Modifier.size(14.dp))
                            Text("RECTIFICADO", color = if (isDarkMode) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                MedidaEstado.PENDIENTE -> {
                    Surface(color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB), shape = RoundedCornerShape(20.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.RadioButtonUnchecked, null, tint = textMuted, modifier = Modifier.size(14.dp))
                            Text("SIN VALIDAR", color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Botones de acción
        if (estado == MedidaEstado.PENDIENTE) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Botón Rectificar
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        color = Color.Transparent,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, border),
                        onClick = onRectificar
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, null, tint = textMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("Rectificar", color = textMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // Botón Aprobar
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        color = if (isDarkMode) Color.White else Color.Black,
                        shape = CircleShape,
                        onClick = onAprobar
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, null, tint = if (isDarkMode) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text("OK", color = textPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}