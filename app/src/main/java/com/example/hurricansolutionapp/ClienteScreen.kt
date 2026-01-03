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
import androidx.compose.material.icons.filled.Info

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteScreen(
    draft: CotizacionDraft,
    isDarkMode: Boolean,
    currentStep: Int = 1,   // ✅ Nuevo parámetro
    totalSteps: Int = 3,    // ✅ Nuevo parámetro
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

    val hayCambios = nombre.isNotBlank()
            || telefono.isNotBlank()
            || ciudad.isNotBlank()
            || colonia.isNotBlank()
            || direccionDetalle.isNotBlank()

    BackHandler(enabled = true) {
        if (hayCambios) showExitDialog = true else onBack()
    }

    val isFormValid = nombre.isNotBlank() && telefono.isNotBlank() && ciudad.isNotBlank()

    // ✅ Colores BLACK MODE (negro puro, no azul)
    val surface = if (isDarkMode) Color(0xFF0A0A0A) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
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

                // ✅ STEPPER DINÁMICO
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSteps) { step ->
                        val isCurrentStep = step + 1 == currentStep
                        val isPastStep = step + 1 < currentStep

                        Box(
                            modifier = if (isCurrentStep) {
                                Modifier
                                    .width(24.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isDarkMode) Color.White else Color.Black)
                            } else {
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isPastStep) {
                                            if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Black.copy(
                                                alpha = 0.6f
                                            )
                                        } else {
                                            textMuted.copy(alpha = 0.4f)
                                        }
                                    )
                            }
                        )

                        if (step < totalSteps - 1) {
                            Spacer(Modifier.width(6.dp))
                        }
                    }
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
        )

        {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "INFORMACIÓN PERSONAL",
                    color = textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                // ✅ NUEVO: Banner informativo si es cliente del CRM
                if (draft.esClienteActual) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) Color(0xFF1E3A8A).copy(alpha = 0.3f) else Color(0xFFDEEBFF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF3B82F6) else Color(0xFF2563EB))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,  // ✅ Usa Material Icons
                                contentDescription = null,
                                tint = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Cliente desde CRM: Nombre y teléfono no editables",
                                fontSize = 13.sp,
                                color = if (isDarkMode) Color(0xFFBFDBFE) else Color(0xFF1E40AF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                StitchField(
                    label = "Nombre Completo",
                    value = nombre,
                    hint = "Ej. Juan Pérez",
                    onValueChange = { nombre = it },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    surface = surface,
                    border = border,
                    icon = R.drawable.ic_user_lucide,
                    readOnly = draft.esClienteActual
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
                    keyboardType = KeyboardType.Number,
                    readOnly = draft.esClienteActual  // ✅ NUEVO: readonly si es cliente CRM
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

    // Dialog de salida
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = if (isDarkMode) Color(0xFF0A0A0A) else Color.White,
            titleContentColor = if (isDarkMode) Color.White else Color(0xFF111418),
            textContentColor = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF4B5563),
            title = { Text("Salir de la cotización") },
            text = { Text("¿Qué quieres hacer con el borrador actual?") },
            confirmButton = {
                TextButton(
                    onClick = {
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
                            draft.nombre = nombre
                            draft.telefono = telefono
                            draft.ciudad = ciudad
                            draft.colonia = colonia
                            draft.direccionDetalle = direccionDetalle
                            showExitDialog = false
                            onBack()
                        }
                    ) {
                        Text(
                            "Salir sin borrar",
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }

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
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "scale"
    )
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "float"
    )
    val phoneShake by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(150, easing = LinearEasing), RepeatMode.Reverse),
        label = "shake"
    )

    val inputBg = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF1F3F5)
    val inputBorder = if (isDarkMode) Color(0xFF3F3F46) else border.copy(alpha = 0.5f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            color = textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value, onValueChange = onValueChange, readOnly = readOnly,
            placeholder = { Text(hint, color = Color.Gray.copy(alpha = 0.6f)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (isTextArea) 110.dp else 54.dp)
                .onFocusChanged { isFocused = it.isFocused },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = surface,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = if (isDarkMode) Color.White else Color.Black,
                unfocusedBorderColor = inputBorder,
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
                                    when {
                                        isLocate -> {
                                            scaleX = scaleAnim; scaleY = scaleAnim
                                        }; isPhone -> rotationZ = phoneShake; else -> translationY =
                                        floatAnim
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