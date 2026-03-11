package com.example.hurricansolutionapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties

/**
 * Pantalla de captura de datos del cliente para Rain Protection
 * Copia exacta de ClienteScreen pero usando CotizacionRainDraft
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RainClienteScreen(
    rainDraft: CotizacionRainDraft,
    isDarkMode: Boolean,
    currentStep: Int = 1,
    totalSteps: Int = 4,
    onBack: () -> Unit,
    onContinuar: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }

    var nombre by rememberSaveable { mutableStateOf(rainDraft.nombre) }
    var telefono by rememberSaveable { mutableStateOf(rainDraft.telefono) }
    val fechaActual = remember {
        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
    var ciudad by rememberSaveable { mutableStateOf(rainDraft.ciudad) }
    var colonia by rememberSaveable { mutableStateOf(rainDraft.colonia) }
    var direccionDetalle by rememberSaveable { mutableStateOf(rainDraft.direccionDetalle) }

    // Estado para la zona detectada (interno, no se muestra)
    var zonaDetectada by remember { mutableStateOf<ZonaGeografica?>(null) }

    // Estado para el autocompletado
    var showSugerencias by remember { mutableStateOf(false) }
    var sugerenciasCiudad by remember { mutableStateOf<List<String>>(emptyList()) }

    // Detectar zona cuando cambia la ciudad
    LaunchedEffect(ciudad) {
        if (ciudad.isNotEmpty()) {
            sugerenciasCiudad = ZonasData.getSugerencias(ciudad, 8)
            showSugerencias = sugerenciasCiudad.isNotEmpty() && !sugerenciasCiudad.any {
                it.equals(ciudad, ignoreCase = true)
            }
            zonaDetectada = ZonasData.detectarZona(ciudad)
        } else {
            sugerenciasCiudad = emptyList()
            showSugerencias = false
        }
    }

    val hayCambios = nombre.isNotBlank()
            || telefono.isNotBlank()
            || ciudad.isNotBlank()
            || colonia.isNotBlank()
            || direccionDetalle.isNotBlank()

    BackHandler(enabled = true) {
        if (hayCambios) showExitDialog = true else onBack()
    }

    // Validación: TODOS los campos son obligatorios
    val isFormValid = nombre.isNotBlank()
            && telefono.isNotBlank()
            && ciudad.isNotBlank()
            && colonia.isNotBlank()
            && direccionDetalle.isNotBlank()

    val surface = if (isDarkMode) Color(0xFF0A0A0A) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF6F7F8)

    Scaffold(
        topBar = {
            StitchTopBar(
                title = "Datos del Cliente",
                onBack = { if (hayCambios) showExitDialog = true else onBack() },
                isDarkMode = isDarkMode
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = surface,
                tonalElevation = 0.dp
            ) {
                Button(
                    onClick = {
                        rainDraft.nombre = nombre
                        rainDraft.telefono = telefono
                        rainDraft.ciudad = ciudad
                        rainDraft.colonia = colonia
                        rainDraft.direccionDetalle = direccionDetalle
                        rainDraft.zonaGeografica = zonaDetectada ?: ZonaGeografica.CONTINENTAL
                        onContinuar()
                    },
                    enabled = isFormValid,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkMode) Color.White else Color.Black,
                        disabledContainerColor = textMuted.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Siguiente",
                            color = if (isDarkMode) Color.Black else Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            null,
                            tint = if (isDarkMode) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        containerColor = bg
    ) { inner ->
        Column(
            modifier = Modifier.padding(inner).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "INFORMACIÓN PERSONAL",
                    color = textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (rainDraft.esClienteActual) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) Color(0xFF1E3A8A).copy(alpha = 0.3f)
                            else Color(0xFFDEEBFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF3B82F6) else Color(0xFF2563EB))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                null,
                                tint = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Cliente desde CRM: Nombre y teléfono no editables",
                                fontSize = 13.sp,
                                color = if (isDarkMode) Color(0xFFBFDBFE) else Color(0xFF1E40AF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                StitchField(
                    label = "Nombre Completo",
                    value = nombre,
                    hint = "Ej. Juan Perez",
                    onValueChange = { nombre = it },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    surface = surface,
                    border = border,
                    icon = R.drawable.ic_user_lucide,
                    readOnly = rainDraft.esClienteActual
                )
                StitchField(
                    label = "Número de Celular",
                    value = telefono,
                    hint = "9841234567",
                    onValueChange = { txt -> if (txt.all { it.isDigit() }) telefono = txt },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    surface = surface,
                    border = border,
                    icon = R.drawable.ic_phone_lucide,
                    isPhone = true,
                    keyboardType = KeyboardType.Number,
                    readOnly = rainDraft.esClienteActual
                )
                StitchField(
                    label = "Fecha de Cotización",
                    value = fechaActual,
                    hint = "",
                    onValueChange = { },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    surface = surface,
                    border = border,
                    icon = R.drawable.ic_calendar_lucide,
                    readOnly = true
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "UBICACIÓN DEL PROYECTO",
                    color = textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // Campo de Ciudad con Autocompletado
                RainCiudadAutocompleteField(
                    value = ciudad,
                    onValueChange = { ciudad = it },
                    sugerencias = sugerenciasCiudad,
                    showSugerencias = showSugerencias,
                    onSugerenciaSelected = { selectedCiudad ->
                        ciudad = selectedCiudad
                        zonaDetectada = ZonasData.detectarZona(selectedCiudad)
                        showSugerencias = false
                    },
                    onDismissSugerencias = { showSugerencias = false },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    surface = surface,
                    border = border
                )

                StitchField(
                    label = "Colonia / Fraccionamiento *",
                    value = colonia,
                    hint = "Ej. Centro",
                    onValueChange = { colonia = it },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    surface = surface,
                    border = border,
                    icon = R.drawable.ic_map_pin_lucide
                )
                StitchField(
                    label = "Calle y Número *",
                    value = direccionDetalle,
                    hint = "Ej. Av. Constitución #2000",
                    onValueChange = { direccionDetalle = it },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    surface = surface,
                    border = border,
                    icon = R.drawable.ic_map_pinned_lucide,
                    isTextArea = true,
                    isLocate = true
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = if (isDarkMode) Color(0xFF0A0A0A) else Color.White,
            title = {
                Text(
                    "Se borrarán los datos",
                    color = if (isDarkMode) Color.White else Color(0xFF111418),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "¿Seguro que deseas salir?\nLos datos ingresados se perderán.",
                    color = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF4B5563)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onBack()
                    }
                ) {
                    Text("Confirmar", color = Color(0xFFE7180B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancelar", color = if (isDarkMode) Color.White else Color.Black)
                }
            }
        )
    }
}

// CAMPO DE CIUDAD CON AUTOCOMPLETADO PARA RAIN
@Composable
private fun RainCiudadAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    sugerencias: List<String>,
    showSugerencias: Boolean,
    onSugerenciaSelected: (String) -> Unit,
    onDismissSugerencias: () -> Unit,
    isDarkMode: Boolean,
    textPrimary: Color,
    surface: Color,
    border: Color
) {
    var isFocused by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "icons")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "float"
    )

    val inputBg = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF1F3F5)
    val inputBorder = if (isDarkMode) Color(0xFF3F3F46) else border.copy(alpha = 0.5f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Ciudad / Municipio",
            color = textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text("Ej. Cancún, Playa del Carmen...", color = Color.Gray.copy(alpha = 0.6f))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .onFocusChanged {
                        isFocused = it.isFocused
                        if (!it.isFocused) onDismissSugerencias()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = surface,
                    unfocusedContainerColor = inputBg,
                    focusedBorderColor = if (isDarkMode) Color.White else Color.Black,
                    unfocusedBorderColor = inputBorder,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB))
                            .graphicsLayer { if (isFocused) translationY = floatAnim },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_building_lucide),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = textPrimary.copy(alpha = 0.8f)
                        )
                    }
                },
                singleLine = true
            )

            DropdownMenu(
                expanded = showSugerencias && sugerencias.isNotEmpty(),
                onDismissRequest = onDismissSugerencias,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 300.dp)
                    .background(if (isDarkMode) Color(0xFF18181B) else Color.White),
                properties = PopupProperties(focusable = false)
            ) {
                sugerencias.forEach { ciudadSugerida ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Place,
                                    null,
                                    tint = textPrimary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(ciudadSugerida, color = textPrimary, fontSize = 15.sp)
                            }
                        },
                        onClick = { onSugerenciaSelected(ciudadSugerida) }
                    )
                }
            }
        }
    }
}