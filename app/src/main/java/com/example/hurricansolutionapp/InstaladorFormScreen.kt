package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class MedidaEditable(
    val id: String = "",
    var zona: String = "",
    var descripcion: String = "",
    var alto: String = "",
    var ancho: String = "",
    var tipoMontaje: String = "Flush Mount",
    var requiereAdecuacion: Boolean = false,
    var adecuacionDetalle: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstaladorFormScreen(
    folio: String,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onNavigateToResumen: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var instaladorDatos by remember { mutableStateOf<InstaladorDatos?>(null) }
    var medidas by remember { mutableStateOf<List<MedidaInstalador>>(emptyList()) }

    var tipoPropiedad by remember { mutableStateOf("") }
    var nivel by remember { mutableStateOf("") }
    var requiereAndamios by remember { mutableStateOf(false) }
    var fechaSolicitada by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }
    var medidasEditables by remember { mutableStateOf<List<MedidaEditable>>(emptyList()) }
    var medidasOriginales by remember { mutableStateOf<List<MedidaInstalador>>(emptyList()) }  // âœ… NUEVO: Para comparar
    var indexActual by remember { mutableIntStateOf(0) }

    // âœ… NUEVO: Control para ediciÃ³n de fecha solicitada
    var showFechaEditDialog by remember { mutableStateOf(false) }
    var fechaEditable by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }  // âœ… NUEVO: Para guardar antes de navegar

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val inputBg = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFF9FAFB)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)

    LaunchedEffect(folio) {
        scope.launch {
            try {
                isLoading = true
                val result = InstaladorRepository.getDatosCompletosByFolio(folio)
                if (result.isSuccess) {
                    val data = result.getOrNull()
                    if (data != null) {
                        val (datos, medidasList) = data
                        instaladorDatos = datos
                        medidas = medidasList
                        // âœ… COPIA PROFUNDA: Guardar valores originales (no referencia)
                        medidasOriginales = medidasList.map { m ->
                            MedidaInstalador(
                                id = m.id,
                                instaladorDatosId = m.instaladorDatosId,
                                zona = m.zona,
                                descripcion = m.descripcion,
                                cantidad = m.cantidad,
                                alto = m.alto,
                                ancho = m.ancho,
                                tipoMontaje = m.tipoMontaje,
                                requiereAdecuacion = m.requiereAdecuacion,
                                adecuacionDetalle = m.adecuacionDetalle,
                                orden = m.orden,
                                createdAt = m.createdAt
                            )
                        }
                        android.util.Log.d("InstaladorForm", "ðŸ“ Datos cargados: ${datos.folio}")
                        android.util.Log.d(
                            "InstaladorForm",
                            "ðŸ“ Medidas encontradas: ${medidasList.size}"
                        )
                        android.util.Log.d(
                            "InstaladorForm",
                            "ðŸ“ Medidas ORIGINALES guardadas: ${medidasOriginales.size}"
                        )
                        medidasList.forEach { m ->
                            android.util.Log.d(
                                "InstaladorForm",
                                "  - ${m.descripcion}: ${m.alto}x${m.ancho}"
                            )
                        }
                        tipoPropiedad = datos.getTipoPropiedadSegura()
                        nivel = datos.getNivelSeguro()
                        requiereAndamios = datos.requiereAndamios
                        fechaSolicitada = datos.getFechaSolicitadaSegura()
                        observaciones = datos.getObservacionesSeguras()
                        medidasEditables = medidasList.map { m ->
                            MedidaEditable(
                                m.id,
                                m.getZonaSegura(),
                                m.descripcion,
                                m.alto.toString(),
                                m.ancho.toString(),
                                m.getTipoMontajeSeguro(),
                                m.requiereAdecuacion,
                                m.getAdecuacionDetalleSeguro()
                            )
                        }
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

    // âœ… NUEVO: DiÃ¡logo de confirmaciÃ³n para editar fecha solicitada
    if (showFechaEditDialog) {
        AlertDialog(
            onDismissRequest = { showFechaEditDialog = false },
            containerColor = cardBg,
            title = {
                Text(
                    "Editar Fecha Solicitada",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "EstÃ¡ por editar la fecha solicitada por el especialista. Â¿EstÃ¡ seguro de querer hacerlo?",
                    color = textMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        fechaEditable = true
                        showFechaEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Text("SÃ­, editar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFechaEditDialog = false }) {
                    Text("No", color = textMuted)
                }
            }
        )
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBarWithDivider(
                title = "Rectificar Â· $folio",
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
                    // Cliente info
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
                            Row {
                                Text("Nombre: ", color = textMuted, fontSize = 13.sp); Text(
                                instaladorDatos!!.nombreCliente,
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            }
                            Row {
                                Text("Sistema: ", color = textMuted, fontSize = 13.sp); Text(
                                instaladorDatos!!.sistemaSeleccionado.getSistemaDisplayName(),
                                color = textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            }
                        }
                    }
                    Spacer(Modifier.height(1.dp))

                    // Datos instalaciÃ³n
                    Surface(color = cardBg) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "DATOS DE INSTALACIÃ“N",
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )

                            var expandedTipo by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedTipo,
                                onExpandedChange = { expandedTipo = it }) {
                                OutlinedTextField(
                                    value = tipoPropiedad.ifBlank { "Seleccionar..." },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tipo de Propiedad", fontSize = 12.sp) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBg,
                                        unfocusedContainerColor = inputBg,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedTipo,
                                    onDismissRequest = { expandedTipo = false }) {
                                    TiposPropiedadInstalador.opciones.forEach {
                                        DropdownMenuItem(
                                            text = { Text(it) },
                                            onClick = { tipoPropiedad = it; expandedTipo = false })
                                    }
                                }
                            }

                            var expandedNivel by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedNivel,
                                onExpandedChange = { expandedNivel = it }) {
                                OutlinedTextField(
                                    value = nivel.ifBlank { "Seleccionar..." },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Nivel", fontSize = 12.sp) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNivel)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBg,
                                        unfocusedContainerColor = inputBg,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedNivel,
                                    onDismissRequest = { expandedNivel = false }) {
                                    NivelesInstalador.opciones.forEach {
                                        DropdownMenuItem(text = {
                                            Text(
                                                it
                                            )
                                        }, onClick = { nivel = it; expandedNivel = false })
                                    }
                                }
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Â¿Requiere Andamios?", color = textPrimary, fontSize = 14.sp)
                                Switch(
                                    checked = requiereAndamios,
                                    onCheckedChange = { requiereAndamios = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF10B981)
                                    )
                                )
                            }

                            // âœ… MODIFICADO: Fecha Solicitada con confirmaciÃ³n para editar
                            OutlinedTextField(
                                value = fechaSolicitada,
                                onValueChange = {
                                    if (fechaEditable) {
                                        fechaSolicitada = it
                                    }
                                },
                                label = { Text("Fecha Solicitada", fontSize = 12.sp) },
                                placeholder = {
                                    Text(
                                        "Ej: Primera semana de febrero",
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !fechaEditable) {
                                        // Mostrar diÃ¡logo de confirmaciÃ³n al tocar
                                        showFechaEditDialog = true
                                    },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = if (fechaEditable) inputBg else inputBg.copy(
                                        alpha = 0.7f
                                    ),
                                    unfocusedContainerColor = if (fechaEditable) inputBg else inputBg.copy(
                                        alpha = 0.7f
                                    ),
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = if (fechaEditable) textPrimary else textMuted
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                enabled = fechaEditable,
                                trailingIcon = {
                                    if (!fechaEditable) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Bloqueado",
                                            tint = textMuted.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clickable { showFechaEditDialog = true }
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Editando",
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(1.dp))

                    // Medidas
                    Surface(color = cardBg) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "MEDIDAS (${medidasEditables.size})",
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (indexActual > 0) indexActual-- },
                                        enabled = indexActual > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronLeft,
                                            null,
                                            tint = if (indexActual > 0) textPrimary else textMuted.copy(
                                                alpha = 0.3f
                                            )
                                        )
                                    }
                                    Text(
                                        "${indexActual + 1}/${medidasEditables.size}",
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { if (indexActual < medidasEditables.size - 1) indexActual++ },
                                        enabled = indexActual < medidasEditables.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            null,
                                            tint = if (indexActual < medidasEditables.size - 1) textPrimary else textMuted.copy(
                                                alpha = 0.3f
                                            )
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = border.copy(alpha = 0.5f))

                            if (medidasEditables.isNotEmpty() && indexActual in medidasEditables.indices) {
                                val m = medidasEditables[indexActual]

                                // âœ… Campo ZONA - Solo lectura (lo llena el especialista)
                                OutlinedTextField(
                                    value = m.zona.ifBlank { "No especificada" },
                                    onValueChange = { },  // No permite ediciÃ³n
                                    label = {
                                        Text(
                                            "Zona (definida por especialista)",
                                            fontSize = 12.sp
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBg.copy(alpha = 0.5f),
                                        unfocusedContainerColor = inputBg.copy(alpha = 0.5f),
                                        focusedTextColor = textMuted,
                                        unfocusedTextColor = textMuted,
                                        disabledTextColor = textMuted,
                                        disabledContainerColor = inputBg.copy(alpha = 0.5f),
                                        disabledBorderColor = border.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    enabled = false,  // Deshabilitado
                                    readOnly = true
                                )
                                OutlinedTextField(
                                    value = m.descripcion,
                                    onValueChange = { v ->
                                        medidasEditables = medidasEditables.toMutableList().also {
                                            it[indexActual] = it[indexActual].copy(descripcion = v)
                                        }
                                    },
                                    label = { Text("DescripciÃ³n", fontSize = 12.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = inputBg,
                                        unfocusedContainerColor = inputBg,
                                        focusedTextColor = textPrimary,
                                        unfocusedTextColor = textPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = m.alto,
                                        onValueChange = { v ->
                                            medidasEditables = medidasEditables.toMutableList()
                                                .also {
                                                    it[indexActual] =
                                                        it[indexActual].copy(alto = v.filter { it.isDigit() || it == '.' })
                                                }
                                        },
                                        label = { Text("Alto (m)", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = inputBg,
                                            unfocusedContainerColor = inputBg,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = m.ancho,
                                        onValueChange = { v ->
                                            medidasEditables = medidasEditables.toMutableList()
                                                .also {
                                                    it[indexActual] =
                                                        it[indexActual].copy(ancho = v.filter { it.isDigit() || it == '.' })
                                                }
                                        },
                                        label = { Text("Ancho (m)", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = inputBg,
                                            unfocusedContainerColor = inputBg,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }
                                val area =
                                    (m.alto.toDoubleOrNull() ?: 0.0) * (m.ancho.toDoubleOrNull()
                                        ?: 0.0)
                                Text(
                                    "Ãrea: ${String.format("%.2f", area)} mÂ²",
                                    color = textMuted,
                                    fontSize = 13.sp
                                )
                                Text("Tipo de Montaje", color = textMuted, fontSize = 12.sp)
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FilterChip(
                                        selected = m.tipoMontaje == "Flush Mount",
                                        onClick = {
                                            medidasEditables = medidasEditables.toMutableList()
                                                .also {
                                                    it[indexActual] =
                                                        it[indexActual].copy(tipoMontaje = "Flush Mount")
                                                }
                                        },
                                        label = { Text("Flush Mount", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = m.tipoMontaje == "Trapezoidal",
                                        onClick = {
                                            medidasEditables = medidasEditables.toMutableList()
                                                .also {
                                                    it[indexActual] =
                                                        it[indexActual].copy(tipoMontaje = "Trapezoidal")
                                                }
                                        },
                                        label = { Text("Trapezoidal", fontSize = 12.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Â¿Requiere Adecuaciones?",
                                        color = textPrimary,
                                        fontSize = 14.sp
                                    )
                                    Switch(
                                        checked = m.requiereAdecuacion,
                                        onCheckedChange = { c ->
                                            medidasEditables = medidasEditables.toMutableList()
                                                .also {
                                                    it[indexActual] = it[indexActual].copy(
                                                        requiereAdecuacion = c,
                                                        adecuacionDetalle = if (!c) "" else it[indexActual].adecuacionDetalle
                                                    )
                                                }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF10B981)
                                        )
                                    )
                                }
                                AnimatedVisibility(visible = m.requiereAdecuacion) {
                                    OutlinedTextField(
                                        value = m.adecuacionDetalle,
                                        onValueChange = { v ->
                                            medidasEditables = medidasEditables.toMutableList()
                                                .also {
                                                    it[indexActual] =
                                                        it[indexActual].copy(adecuacionDetalle = v)
                                                }
                                        },
                                        label = {
                                            Text(
                                                "Detalle de Adecuaciones",
                                                fontSize = 12.sp
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = inputBg,
                                            unfocusedContainerColor = inputBg,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        minLines = 2
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(1.dp))

                    // Observaciones
                    Surface(color = cardBg) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "OBSERVACIONES",
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            OutlinedTextField(
                                value = observaciones,
                                onValueChange = { observaciones = it },
                                placeholder = {
                                    Text(
                                        "Notas adicionales...",
                                        fontSize = 13.sp,
                                        color = textMuted
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = inputBg,
                                    unfocusedContainerColor = inputBg,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                minLines = 3
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val invalidas = medidasEditables.filter {
                                it.descripcion.isBlank() || (it.alto.toDoubleOrNull()
                                    ?: 0.0) <= 0 || (it.ancho.toDoubleOrNull() ?: 0.0) <= 0
                            }
                            if (invalidas.isNotEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Hay ${invalidas.size} medida(s) incompletas",
                                    Toast.LENGTH_SHORT
                                ).show(); return@Button
                            }

                            // âœ… NUEVO: Guardar cambios antes de navegar
                            isSaving = true
                            scope.launch {
                                try {
                                    // Detectar si hay cambios en las medidas
                                    android.util.Log.d(
                                        "InstaladorForm",
                                        "ðŸ“ Comparando medidas..."
                                    )
                                    val hayRectificaciones = medidasEditables.any { editable ->
                                        val original =
                                            medidasOriginales.find { it.id == editable.id }
                                        if (original == null) {
                                            android.util.Log.d(
                                                "InstaladorForm",
                                                "  âš ï¸ Medida nueva (sin original): ${editable.descripcion}"
                                            )
                                            true
                                        } else {
                                            val altoEditado = editable.alto.toDoubleOrNull() ?: 0.0
                                            val anchoEditado =
                                                editable.ancho.toDoubleOrNull() ?: 0.0
                                            val diffAlto =
                                                kotlin.math.abs(original.alto - altoEditado)
                                            val diffAncho =
                                                kotlin.math.abs(original.ancho - anchoEditado)
                                            val cambio = diffAlto > 0.001 || diffAncho > 0.001
                                            android.util.Log.d(
                                                "InstaladorForm",
                                                "  ðŸ“Š ${editable.descripcion}:"
                                            )
                                            android.util.Log.d(
                                                "InstaladorForm",
                                                "     Original: ${original.alto}x${original.ancho}"
                                            )
                                            android.util.Log.d(
                                                "InstaladorForm",
                                                "     Editado: ${altoEditado}x${anchoEditado}"
                                            )
                                            android.util.Log.d(
                                                "InstaladorForm",
                                                "     Diff: alto=$diffAlto, ancho=$diffAncho"
                                            )
                                            android.util.Log.d(
                                                "InstaladorForm",
                                                "     Â¿CambiÃ³?: $cambio"
                                            )
                                            cambio
                                        }
                                    }
                                    android.util.Log.d(
                                        "InstaladorForm",
                                        "ðŸ“ Â¿Hay rectificaciones?: $hayRectificaciones"
                                    )

                                    // Actualizar instalador_datos
                                    android.util.Log.d("InstaladorForm", "ðŸ“ Guardando datos:")
                                    android.util.Log.d(
                                        "InstaladorForm",
                                        "   tipoPropiedad: '$tipoPropiedad'"
                                    )
                                    android.util.Log.d("InstaladorForm", "   nivel: '$nivel'")
                                    android.util.Log.d(
                                        "InstaladorForm",
                                        "   requiereAndamios: $requiereAndamios"
                                    )
                                    android.util.Log.d(
                                        "InstaladorForm",
                                        "   fechaSolicitada: '$fechaSolicitada'"
                                    )
                                    android.util.Log.d(
                                        "InstaladorForm",
                                        "   observaciones: '$observaciones'"
                                    )

                                    instaladorDatos?.let { datos ->
                                        val updateResult = InstaladorRepository.updateDatos(
                                            id = datos.id,
                                            update = InstaladorDatosUpdate(
                                                tipoPropiedad = tipoPropiedad,  // Enviar tal cual, incluso si es ""
                                                nivel = nivel,  // Enviar tal cual
                                                requiereAndamios = requiereAndamios,
                                                fechaSolicitada = fechaSolicitada.ifBlank { null },
                                                observaciones = observaciones.ifBlank { null },
                                                rectificadas = hayRectificaciones  // âœ… AutomÃ¡tico
                                            )
                                        )
                                        android.util.Log.d(
                                            "InstaladorForm",
                                            "ðŸ“ Update instalador_datos: ${updateResult.isSuccess}"
                                        )
                                        if (updateResult.isFailure) {
                                            android.util.Log.e(
                                                "InstaladorForm",
                                                "âŒ Error update: ${updateResult.exceptionOrNull()?.message}"
                                            )
                                        }
                                    }

                                    // Actualizar medidas
                                    instaladorDatos?.let { datos ->
                                        val medidasInsert = medidasEditables.map { m ->
                                            MedidaInstaladorInsert(
                                                instaladorDatosId = datos.id,
                                                zona = m.zona.ifBlank { null },
                                                descripcion = m.descripcion,
                                                cantidad = 1,
                                                alto = m.alto.toDoubleOrNull() ?: 0.0,
                                                ancho = m.ancho.toDoubleOrNull() ?: 0.0,
                                                tipoMontaje = m.tipoMontaje,
                                                requiereAdecuacion = m.requiereAdecuacion,
                                                adecuacionDetalle = m.adecuacionDetalle.ifBlank { null },
                                                orden = medidasEditables.indexOf(m)
                                            )
                                        }
                                        val medidasResult = InstaladorRepository.replaceMedidas(
                                            datos.id,
                                            medidasInsert
                                        )
                                        android.util.Log.d(
                                            "InstaladorForm",
                                            "ðŸ“ Update medidas: ${medidasResult.isSuccess}"
                                        )
                                        if (medidasResult.isFailure) {
                                            android.util.Log.e(
                                                "InstaladorForm",
                                                "âŒ Error medidas: ${medidasResult.exceptionOrNull()?.message}"
                                            )
                                        }
                                    }

                                    // PequeÃ±a pausa para asegurar que BD se sincronize
                                    kotlinx.coroutines.delay(300)

                                    onNavigateToResumen(folio)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color.White else Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = if (isDarkMode) Color.Black else Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Guardando...",
                                color = if (isDarkMode) Color.Black else Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                "Continuar al Resumen",
                                color = if (isDarkMode) Color.Black else Color.White,
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