package com.example.hurricansolutionapp

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun getColoresDisponibles(sistema: String): List<String> {
    return when {
        sistema.contains("875", ignoreCase = true) -> listOf("Negro", "Café")
        sistema.contains("1250", ignoreCase = true) -> listOf("Blanco", "Beige")
        sistema.contains("1500", ignoreCase = true) -> listOf("Café")
        else -> emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnviosInstalacionScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estados de la lista
    var isLoading by remember { mutableStateOf(true) }
    var cotizaciones by remember { mutableStateOf<List<Cotizacion>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    var cotizacionSeleccionada by remember { mutableStateOf<Cotizacion?>(null) }
    var sistemaSeleccionado by remember { mutableStateOf<String?>(null) }
    var colorSeleccionado by remember { mutableStateOf<String?>(null) }  // NUEVO: Color de tela
    var fechaInstalacion by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var pdfGenerado by remember { mutableStateOf<File?>(null) }

    // Estado para rastrear PDFs generados por folio
    var pdfsGenerados by remember { mutableStateOf<Map<String, File>>(emptyMap()) }

    // Colores
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val inputBg = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val primary = if (isDarkMode) Color.White else Color.Black
    val onPrimary = if (isDarkMode) Color.Black else Color.White

    val userId = remember { SessionManager.getUserId(context) }
    val userRole = remember { SessionManager.getRole(context) }
    val userName = remember { SessionManager.getNombre(context) }
    val isAdmin = userRole.equals("ADMIN", ignoreCase = true)

    // Cargar cotizaciones al iniciar
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                val result = EnviosInstalacionRepository.getCotizacionesPendientesEnvioTodas(
                    context = context,
                    userId = userId,
                    especialistaNombre = userName
                )
                if (result.isSuccess) {
                    cotizaciones = result.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("OrdenesInstalacion", "Error cargando: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Filtrar lista
    val filteredList = remember(cotizaciones, searchQuery) {
        if (searchQuery.isBlank()) cotizaciones
        else cotizaciones.filter { cot ->
            cot.clienteNombre.contains(searchQuery, ignoreCase = true) ||
                    cot.folio.contains(searchQuery, ignoreCase = true)
        }
    }

    // Resetear color cuando cambia el sistema
    LaunchedEffect(sistemaSeleccionado) {
        if (sistemaSeleccionado != null) {
            val colores = getColoresDisponibles(sistemaSeleccionado!!)
            colorSeleccionado = colores.firstOrNull()
        } else {
            colorSeleccionado = null
        }
    }

    fun generarPdfInstalacion(cotizacion: Cotizacion, sistema: String, color: String?, fecha: String, obs: String) {
        scope.launch {
            isGeneratingPdf = true
            try {
                val pdfFile = PdfInstaladorGenerator.generarPdfOrdenInstalacion(
                    context = context,
                    cotizacion = cotizacion,
                    sistemaSeleccionado = sistema,
                    colorSeleccionado = color,
                    instaladorDatos = null,
                    medidasRectificadas = null,
                    fechaSolicitadaManual = fecha,
                    observacionesManuales = obs
                )

                if (pdfFile != null && pdfFile.exists()) {
                    pdfGenerado = pdfFile
                    pdfsGenerados = pdfsGenerados + (cotizacion.folio to pdfFile)

                    // Subir a Google Drive automáticamente
                    try {
                        val driveResult = InstaladorUploadManager.uploadToDrive(
                            context = context,
                            pdfFile = pdfFile,
                            cotizacion = cotizacion,
                            sistemaSeleccionado = sistema
                        )

                        val uploadResult = driveResult.getOrNull()
                        if (uploadResult?.success == true) {
                            Toast.makeText(
                                context,
                                "PDF generado y subido a Drive",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // PDF generado pero no subido - mostrar error específico
                            val errorMsg = uploadResult?.error ?: "Error desconocido"
                            Toast.makeText(
                                context,
                                "PDF generado. Error Drive: $errorMsg",
                                Toast.LENGTH_LONG
                            ).show()
                            android.util.Log.w("OrdenesInstalacion", "Error subiendo a Drive: $errorMsg")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("OrdenesInstalacion", "Error subiendo a Drive: ${e.message}")
                        Toast.makeText(
                            context,
                            "PDF generado. Error al subir a Drive.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(context, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("OrdenesInstalacion", "Error generando PDF: ${e.message}", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isGeneratingPdf = false
            }
        }
    }

    fun mostrarDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                fechaInstalacion = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    if (cotizacionSeleccionada != null) {
        val cot = cotizacionSeleccionada!!
        val tieneMultiplesSistemas = cot.productos.size > 1
        val areaTotal = cot.ventanas.sumOf { it.alto * it.ancho }

        AlertDialog(
            onDismissRequest = {
                if (!isGeneratingPdf) {
                    cotizacionSeleccionada = null
                    sistemaSeleccionado = null
                    colorSeleccionado = null
                    fechaInstalacion = ""
                    observaciones = ""
                    pdfGenerado = null
                }
            },
            containerColor = cardBg,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Orden de Instalación",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Surface(
                        color = primary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            cot.folio,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = onPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        "DATOS DEL CLIENTE",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        color = inputBg,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Person, null, tint = textMuted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    cot.clienteNombre,
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (cot.clienteTelefono.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Phone, null, tint = textMuted, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        cot.clienteTelefono,
                                        color = textPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.LocationOn, null, tint = textMuted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    cot.ubicacion.ifBlank { cot.ciudad },
                                    color = textPrimary,
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Text(
                        "MEDIDAS A INSTALAR",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        color = inputBg,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Window, null, tint = textMuted, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${cot.ventanas.size} apertura${if (cot.ventanas.size != 1) "s" else ""}",
                                        color = textPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.SquareFoot, null, tint = textMuted, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "${String.format("%.2f", areaTotal)} m²",
                                        color = textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            if (cot.ventanas.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = border.copy(alpha = 0.5f))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 100.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(top = 8.dp)
                                    ) {
                                        cot.ventanas.forEachIndexed { index, ventana ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "${index + 1}. ${ventana.descripcion.ifBlank { "Apertura" }}",
                                                    color = textPrimary,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    "${String.format("%.2f", ventana.alto)} x ${String.format("%.2f", ventana.ancho)}",
                                                    color = textMuted,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            if (index < cot.ventanas.lastIndex) {
                                                Spacer(Modifier.height(4.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        "SISTEMA DE PROTECCIÓN",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    if (tieneMultiplesSistemas) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            cot.productos.forEach { producto ->
                                val isSelected = sistemaSeleccionado == producto.name
                                Surface(
                                    onClick = { sistemaSeleccionado = producto.name },
                                    color = if (isSelected) primary.copy(alpha = 0.1f) else inputBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) primary else border
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            producto.etiquetaCorta,
                                            color = textPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                null,
                                                tint = primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val sistema = cot.productos.firstOrNull()
                        LaunchedEffect(Unit) {
                            sistemaSeleccionado = sistema?.name
                        }

                        Surface(
                            color = primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(2.dp, primary)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        sistema?.etiquetaCorta ?: "Sistema",
                                        color = textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        sistema?.etiqueta ?: "",
                                        color = textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (sistemaSeleccionado != null) {
                        val coloresDisponibles = getColoresDisponibles(sistemaSeleccionado!!)

                        if (coloresDisponibles.isNotEmpty()) {
                            Text(
                                "COLOR DE TELA",
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                coloresDisponibles.forEach { color ->
                                    val isSelected = colorSeleccionado == color
                                    Surface(
                                        onClick = { colorSeleccionado = color },
                                        color = if (isSelected) primary.copy(alpha = 0.1f) else inputBg,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) primary else border
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val colorVisual = when (color.lowercase()) {
                                                "negro" -> Color.Black
                                                "café", "cafe" -> Color(0xFF8B4513)
                                                "blanco" -> Color.White
                                                "beige" -> Color(0xFFF5F5DC)
                                                else -> Color.Gray
                                            }
                                            Surface(
                                                modifier = Modifier.size(16.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                color = colorVisual,
                                                border = BorderStroke(1.dp, Color.Gray)
                                            ) {}
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                color,
                                                color = textPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp
                                            )
                                            if (isSelected) {
                                                Spacer(Modifier.width(4.dp))
                                                Icon(
                                                    Icons.Default.Check,
                                                    null,
                                                    tint = primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        "FECHA DE INSTALACIÓ",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        onClick = { mostrarDatePicker() },
                        color = inputBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.CalendarToday,
                                    null,
                                    tint = if (fechaInstalacion.isNotBlank()) primary else textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    if (fechaInstalacion.isNotBlank()) fechaInstalacion else "Seleccionar fecha...",
                                    color = if (fechaInstalacion.isNotBlank()) textPrimary else textMuted,
                                    fontSize = 14.sp
                                )
                            }
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                null,
                                tint = textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        "OBSERVACIONES (OPCIONAL)",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    OutlinedTextField(
                        value = observaciones,
                        onValueChange = { observaciones = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        placeholder = {
                            Text(
                                "Notas para el instalador...",
                                color = textMuted.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = primary,
                            unfocusedBorderColor = border,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (pdfGenerado != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "PDF listo",
                                color = textPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { verPdf(context, pdfGenerado!!) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Visibility,
                                    null,
                                    tint = textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Ver PDF", color = textPrimary, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { compartirPdf(context, pdfGenerado!!) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    null,
                                    tint = onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Compartir", color = onPrimary, fontSize = 13.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { pdfGenerado = null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, border)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                null,
                                tint = textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Volver a generar", color = textMuted, fontSize = 13.sp)
                        }

                        TextButton(
                            onClick = {
                                cotizacionSeleccionada = null
                                sistemaSeleccionado = null
                                colorSeleccionado = null
                                fechaInstalacion = ""
                                observaciones = ""
                                pdfGenerado = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cerrar", color = textMuted)
                        }
                    } else {
                        val canGenerate = sistemaSeleccionado != null && fechaInstalacion.isNotBlank()

                        if (sistemaSeleccionado != null && fechaInstalacion.isBlank()) {
                            Text(
                                "Selecciona una fecha de instalación",
                                color = Color(0xFFF59E0B),
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = {
                                val sistema = sistemaSeleccionado ?: cot.productos.firstOrNull()?.name ?: "HS875"
                                generarPdfInstalacion(cot, sistema, colorSeleccionado, fechaInstalacion, observaciones)
                            },
                            enabled = canGenerate && !isGeneratingPdf,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primary,
                                disabledContainerColor = textMuted.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isGeneratingPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Generando...", color = onPrimary)
                            } else {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Generar PDF", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        TextButton(
                            onClick = {
                                cotizacionSeleccionada = null
                                sistemaSeleccionado = null
                                colorSeleccionado = null
                                fechaInstalacion = ""
                                observaciones = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar", color = textMuted)
                        }
                    }
                }
            },
            dismissButton = null
        )
    }

    // PANTALLA PRINCIPAL
    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Órdenes de InstalaciÓn",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Buscar por nombre o folio...", color = textMuted, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = textMuted, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, null, tint = textMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedBorderColor = primary,
                    unfocusedBorderColor = border,
                    focusedContainerColor = inputBg,
                    unfocusedContainerColor = inputBg
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primary)
                }
            } else if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Inbox,
                            null,
                            tint = textMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "Sin resultados" else "No hay cotizaciones pendientes",
                            color = textMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.folio }) { cotizacion ->
                        val pdfExistente = pdfsGenerados[cotizacion.folio]

                        CotizacionInstalacionCard(
                            cotizacion = cotizacion,
                            pdfGenerado = pdfExistente,
                            isDarkMode = isDarkMode,
                            onClick = {
                                cotizacionSeleccionada = cotizacion
                                sistemaSeleccionado = null
                                colorSeleccionado = null
                                fechaInstalacion = ""
                                observaciones = ""
                                pdfGenerado = null
                            },
                            onVerPdf = { pdfExistente?.let { verPdf(context, it) } },
                            onCompartirPdf = { pdfExistente?.let { compartirPdf(context, it) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CotizacionInstalacionCard(
    cotizacion: Cotizacion,
    pdfGenerado: File?,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    onVerPdf: () -> Unit,
    onCompartirPdf: () -> Unit
) {
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val primary = if (isDarkMode) Color.White else Color.Black
    val onPrimary = if (isDarkMode) Color.Black else Color.White

    val areaTotal = cotizacion.ventanas.sumOf { it.alto * it.ancho }

    Surface(
        onClick = onClick,
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        cotizacion.clienteNombre,
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        cotizacion.ubicacion.ifBlank { cotizacion.ciudad },
                        color = textMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = primary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        cotizacion.folio,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = border.copy(alpha = 0.5f))
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Window,
                        null,
                        tint = textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${cotizacion.ventanas.size} aperturas",
                        color = textMuted,
                        fontSize = 12.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.SquareFoot,
                        null,
                        tint = textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${String.format("%.2f", areaTotal)} m²",
                        color = textMuted,
                        fontSize = 12.sp
                    )
                }
                Text(
                    cotizacion.fecha,
                    color = textMuted,
                    fontSize = 12.sp
                )
            }

            if (pdfGenerado != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onVerPdf() },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, border),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            null,
                            tint = textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Ver", color = textMuted, fontSize = 12.sp)
                    }

                    Button(
                        onClick = { onCompartirPdf() },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            null,
                            tint = onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Compartir", color = onPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
