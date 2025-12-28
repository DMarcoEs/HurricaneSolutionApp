package com.example.hurricansolutionapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteScreen(
    draft: CotizacionDraft,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onContinuar: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }

    var nombre by rememberSaveable { mutableStateOf(draft.nombre) }
    var telefono by rememberSaveable { mutableStateOf(draft.telefono) }
    val fechaActual = remember {
        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
    }
    var ciudad by rememberSaveable { mutableStateOf(draft.ciudad) }
    var colonia by rememberSaveable { mutableStateOf(draft.colonia) }
    var direccionDetalle by rememberSaveable { mutableStateOf(draft.direccionDetalle) }

    // ✅ Detecta si el usuario ya escribió algo
    val hayCambios = nombre.isNotBlank()
            || telefono.isNotBlank()
            || ciudad.isNotBlank()
            || colonia.isNotBlank()
            || direccionDetalle.isNotBlank()

    // ✅ Back físico / gesto del celular
    BackHandler(enabled = true) {
        if (hayCambios) showExitDialog = true else onBack()
    }

    val isFormValid = nombre.isNotBlank() && telefono.isNotBlank() && ciudad.isNotBlank()

    val surface = if (isDarkMode) Color(0xFF09090B) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF71717A) else Color(0xFF9CA3AF)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF6F7F8)

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(surface)) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = surface),
                    title = {
                        Text(
                            "Datos del Cliente",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (hayCambios) showExitDialog = true else onBack()
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_left),
                                contentDescription = null,
                                tint = textPrimary
                            )
                        }
                    }
                )

                // Stepper
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .width(24.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isDarkMode) Color.White else Color.Black)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.size(6.dp)
                            .clip(CircleShape)
                            .background(textMuted.copy(alpha = 0.4f))
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.size(6.dp)
                            .clip(CircleShape)
                            .background(textMuted.copy(alpha = 0.4f))
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = surface,
                tonalElevation = 0.dp
            ) {
                Button(
                    onClick = {
                        draft.nombre = nombre
                        draft.telefono = telefono
                        draft.ciudad = ciudad
                        draft.colonia = colonia
                        draft.direccionDetalle = direccionDetalle
                        onContinuar()
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
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
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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

                StitchField(
                    label = "Nombre Completo",
                    value = nombre,
                    hint = "Ej. Juan Pérez",
                    onValueChange = { nombre = it },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    surface = surface,
                    border = border,
                    icon = R.drawable.ic_user_lucide
                )

                StitchField(
                    label = "Número de Celular",
                    value = telefono,
                    hint = "55 1234 5678",
                    onValueChange = { txt -> if (txt.all { ch -> ch.isDigit() }) telefono = txt },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    surface = surface,
                    border = border,
                    icon = R.drawable.ic_phone_lucide,
                    isPhone = true,
                    keyboardType = KeyboardType.Number
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

                StitchField(
                    label = "Ciudad / Municipio",
                    value = ciudad,
                    hint = "Ej. Monterrey",
                    onValueChange = { ciudad = it },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    surface = surface,
                    border = border,
                    icon = R.drawable.ic_building_lucide
                )

                StitchField(
                    label = "Colonia / Fraccionamiento",
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
                    label = "Calle y Número",
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

    // ✅ Dialog de salida (solo si hay cambios)
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = if (isDarkMode) Color(0xFF09090B) else Color.White,
            titleContentColor = if (isDarkMode) Color.White else Color(0xFF111418),
            textContentColor = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF4B5563),
            title = { Text("Salir de la cotización") },
            text = { Text("¿Qué quieres hacer con el borrador actual?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // ⚠️ Importante: draft.clear() debe existir
                        draft.clear()
                        showExitDialog = false
                        onBack()
                    }
                ) { Text("Borrar y salir", color = Color(0xFFE7180B)) }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            // ✅ Guardar borrador ANTES de salir
                            draft.nombre = nombre
                            draft.telefono = telefono
                            draft.ciudad = ciudad
                            draft.colonia = colonia
                            draft.direccionDetalle = direccionDetalle

                            showExitDialog = false
                            onBack()
                        }
                    ) { Text("Salir sin borrar", color = if (isDarkMode) Color.White else Color.Black) }

                    Spacer(Modifier.width(8.dp))

                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Cancelar", color = if (isDarkMode) Color.White else Color.Black)
                    }
                }
            }
        )
    }
}

@Composable
fun StitchField(
    label: String,
    value: String,
    hint: String = "",
    onValueChange: (String) -> Unit,
    isDarkMode: Boolean,
    textPrimary: Color,
    surface: Color,
    border: Color,
    icon: Int? = null,
    isTextArea: Boolean = false,
    readOnly: Boolean = false,
    isPhone: Boolean = false,
    isLocate: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    var isFocused by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "icons")

    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "scale"
    )

    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "float"
    )

    val phoneShake by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            tween(150, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "shake"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            color = textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            placeholder = { Text(hint, color = Color.Gray.copy(alpha = 0.6f)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (isTextArea) 110.dp else 54.dp)
                .onFocusChanged { isFocused = it.isFocused },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = surface,
                unfocusedContainerColor = if (isDarkMode) surface else Color(0xFFF1F3F5),
                focusedBorderColor = if (isDarkMode) Color.White else Color.Black,
                unfocusedBorderColor = border.copy(alpha = 0.5f),
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary
            ),
            trailingIcon = icon?.let { resId ->
                {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB))
                            .graphicsLayer {
                                if (isFocused && !readOnly) {
                                    if (isLocate) {
                                        scaleX = scaleAnim
                                        scaleY = scaleAnim
                                    } else if (isPhone) {
                                        rotationZ = phoneShake
                                    } else {
                                        translationY = floatAnim
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(resId),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = textPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        )
    }
}
