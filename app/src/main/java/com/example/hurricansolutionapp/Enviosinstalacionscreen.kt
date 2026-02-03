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
    var fechaInstalacion by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var pdfGenerado by remember { mutableStateOf<File?>(null) }

    // Estado para rastrear PDFs generados por folio (para mostrar botones en la card)
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
    // Cada usuario solo ve sus propias cotizaciones (filtradas por nombre de especialista)
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                val result = EnviosInstalacionRepository.getCotizacionesPendientesEnvioTodas(
                    context = context,
                    userId = userId,
                    especialistaNombre = userName  // Filtrar por nombre del usuario actual
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

    fun generarPdfInstalacion(cotizacion: Cotizacion, sistema: String, fecha: String, obs: String) {
        scope.launch {
            isGeneratingPdf = true
            try {
                val pdfFile = PdfInstaladorGenerator.generarPdfOrdenInstalacion(
                    context = context,
                    cotizacion = cotizacion,
                    sistemaSeleccionado = sistema,
                    instaladorDatos = null,
                    medidasRectificadas = null,
                    fechaSolicitadaManual = fecha,
                    observacionesManuales = obs
                )

                if (pdfFile != null && pdfFile.exists()) {
                    pdfGenerado = pdfFile
                    // Guardar en el mapa para mostrar botones en la card
                    pdfsGenerados = pdfsGenerados + (cotizacion.folio to pdfFile)
                    Toast.makeText(context, "PDF generado", Toast.LENGTH_SHORT).show()
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
                        // Solo un sistema
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

                    Text(
                        "FECHA DE INSTALACIÓN",
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

                        // Botones Ver PDF y Compartir (mismo estilo)
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
                            onClick = {
                                pdfGenerado = null // Resetear para volver a generar
                            },
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

                        // Mensaje si falta fecha
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
                                generarPdfInstalacion(cot, sistema, fechaInstalacion, observaciones)
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

    // PANTALLA PRINCIPAL - LISTA DE COTIZACIONES
    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Órdenes de Instalación",
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
                            Icon(Icons.Default.Clear, "Limpiar", tint = textMuted)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = inputBg,
                    unfocusedContainerColor = inputBg,
                    focusedBorderColor = border,
                    unfocusedBorderColor = border,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            // Banner informativo
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        null,
                        tint = textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            "Genera la Orden de Instalación",
                            color = textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Selecciona una cotización para crear el PDF",
                            color = textMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Lista de cotizaciones
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = textPrimary, strokeWidth = 2.dp)
                        }
                    }
                    filteredList.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.Inventory2,
                                null,
                                tint = textMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (searchQuery.isNotBlank()) "No se encontraron resultados"
                                else "No hay cotizaciones disponibles",
                                color = textMuted,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    "${filteredList.size} cotización${if (filteredList.size != 1) "es" else ""}",
                                    color = textMuted,
                                    fontSize = 12.sp
                                )
                            }

                            items(filteredList, key = { it.folio }) { cotizacion ->
                                val pdfExistente = pdfsGenerados[cotizacion.folio]
                                OrdenInstalacionCard(
                                    cotizacion = cotizacion,
                                    isDarkMode = isDarkMode,
                                    cardBg = cardBg,
                                    textPrimary = textPrimary,
                                    textMuted = textMuted,
                                    border = border,
                                    primary = primary,
                                    onPrimary = onPrimary,
                                    pdfGenerado = pdfExistente,
                                    onGenerarOrden = { cotizacionSeleccionada = cotizacion },
                                    onVerPdf = { pdfExistente?.let { verPdf(context, it) } },
                                    onCompartirPdf = { pdfExistente?.let { compartirPdf(context, it) } },
                                    onVolverAGenerar = {
                                        pdfsGenerados = pdfsGenerados - cotizacion.folio
                                        cotizacionSeleccionada = cotizacion
                                    }
                                )
                            }

                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdenInstalacionCard(
    cotizacion: Cotizacion,
    isDarkMode: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textMuted: Color,
    border: Color,
    primary: Color,
    onPrimary: Color,
    pdfGenerado: File?,
    onGenerarOrden: () -> Unit,
    onVerPdf: () -> Unit,
    onCompartirPdf: () -> Unit,
    onVolverAGenerar: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val fecha = try {
        cotizacion.fecha?.let { dateFormat.format(Date(it)) } ?: "-"
    } catch (e: Exception) { "-" }

    val numSistemas = cotizacion.productos.size
    val sistemasText = if (numSistemas == 1) {
        cotizacion.productos.firstOrNull()?.etiquetaCorta ?: "-"
    } else {
        "$numSistemas sistemas"
    }

    val areaTotal = cotizacion.ventanas.sumOf { it.alto * it.ancho }
    val numVentanas = cotizacion.ventanas.size

    val tienePdf = pdfGenerado != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (tienePdf) primary.copy(alpha = 0.5f) else border.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Folio + Sistema
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        cotizacion.folio,
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Indicador de PDF generado
                    if (tienePdf) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    "PDF",
                                    color = Color(0xFF10B981),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                Surface(
                    color = primary,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        sistemasText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Nombre del cliente
            Text(
                cotizacion.clienteNombre,
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (cotizacion.ubicacion.isNotBlank() || cotizacion.ciudad.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        null,
                        tint = textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        cotizacion.ubicacion.ifBlank { cotizacion.ciudad },
                        color = textMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = textMuted, modifier = Modifier.size(14.dp))
                    Text(fecha, color = textMuted, fontSize = 12.sp)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Window, null, tint = textMuted, modifier = Modifier.size(14.dp))
                    Text("$numVentanas apertura${if (numVentanas != 1) "s" else ""}", color = textMuted, fontSize = 12.sp)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.SquareFoot, null, tint = textMuted, modifier = Modifier.size(14.dp))
                    Text("${String.format("%.1f", areaTotal)} m²", color = textMuted, fontSize = 12.sp)
                }
            }

            HorizontalDivider(color = border.copy(alpha = 0.3f))

            if (tienePdf) {
                // PDF ya generado - mostrar 3 botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ver PDF
                    Button(
                        onClick = onVerPdf,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            null,
                            tint = textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Ver", color = textPrimary, fontSize = 12.sp)
                    }

                    // Compartir
                    Button(
                        onClick = onCompartirPdf,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
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

                    // Regenerar
                    OutlinedButton(
                        onClick = onVolverAGenerar,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, border),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            null,
                            tint = textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Nueva", color = textMuted, fontSize = 12.sp)
                    }
                }
            } else {
                Button(
                    onClick = onGenerarOrden,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        null,
                        tint = onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Generar Orden de Instalación",
                        color = onPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}