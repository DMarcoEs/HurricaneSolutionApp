package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CotizacionesFormScreen(
    draft: CotizacionDraft,
    onDraftChange: (CotizacionDraft) -> Unit,
    onContinuarResumen: (Cotizacion) -> Unit,
    onBack: () -> Unit,
    isDarkMode: Boolean,
    currentStep: Int = 2,
    totalSteps: Int = 3
) {
    val context = LocalContext.current
    var indexActual by rememberSaveable { mutableIntStateOf(0) }

    val tieneMedidasPrevias = remember(draft.ventanasForm) {
        draft.ventanasForm.any { it.descripcion.isNotBlank() && it.alto.isNotBlank() && it.ancho.isNotBlank() }
    }
    var primeraConfirmada by rememberSaveable { mutableStateOf(tieneMedidasPrevias) }

    val ventanas = remember {
        mutableStateListOf<VentanaFormState>().apply {
            addAll(draft.ventanasForm.ifEmpty { listOf(VentanaFormState()) })
        }
    }

    LaunchedEffect(Unit) {
        if (tieneMedidasPrevias) {
            val lastValidIndex =
                ventanas.indexOfLast { it.descripcion.isNotBlank() && it.alto.isNotBlank() && it.ancho.isNotBlank() }
            if (lastValidIndex >= 0) indexActual = lastValidIndex
        }
    }

    fun syncDraft() {
        draft.ventanasForm = ventanas.toMutableList()
        onDraftChange(draft)
    }

    val actual = ventanas.getOrNull(indexActual) ?: VentanaFormState()

    fun sanitizeDecimalInput(input: String): String {
        val normalized = input.replace(',', '.').replace("m", "").replace(" ", "")
        val filtered = normalized.filter { it.isDigit() || it == '.' }
        val parts = filtered.split('.', limit = 3)
        val intPart = parts.getOrNull(0).orEmpty()
        val decPart = parts.getOrNull(1).orEmpty().take(2)
        return if (filtered.contains('.')) "$intPart.$decPart" else intPart
    }

    fun formatMeasurement(value: String): String {
        val cleanValue = value.replace(",", ".").replace("m", "").replace(" ", "").trim()
        if (cleanValue.isBlank()) return ""
        val number = cleanValue.toDoubleOrNull() ?: return cleanValue
        return String.format("%.2f m", number)
    }

    fun getNumericValue(formatted: String): String =
        formatted.replace(",", ".").replace("m", "").replace(" ", "").trim()

    // COLORES BLACK MODE
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF2F4F6)
    val surface = if (isDarkMode) Color(0xFF0A0A0A) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val textLabel = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val inputBg = if (isDarkMode) Color(0xFF18181B) else Color.White
    val inputBorder = if (isDarkMode) Color(0xFF3F3F46) else Color(0xFFE5E7EB)
    val carouselBg = if (isDarkMode) Color(0xFF18181B) else Color.White
    val carouselBorder = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val pillBg = if (isDarkMode) Color.White else Color.Black
    val pillText = if (isDarkMode) Color.Black else Color.White
    val adecuacionesBg = if (isDarkMode) Color(0xFF09090B) else Color(0xFFF9FAFB)

    fun validarMedidaActual(): Boolean {
        val desc = actual.descripcion.trim()
        val altoNum = getNumericValue(actual.alto).toDoubleOrNull()
        val anchoNum = getNumericValue(actual.ancho).toDoubleOrNull()
        if (desc.isBlank()) {
            Toast.makeText(context, "Falta descripción", Toast.LENGTH_SHORT).show(); return false
        }
        if (altoNum == null || altoNum <= 0.0) {
            Toast.makeText(context, "Ingresa un alto válido", Toast.LENGTH_SHORT)
                .show(); return false
        }
        if (anchoNum == null || anchoNum <= 0.0) {
            Toast.makeText(context, "Ingresa un ancho válido", Toast.LENGTH_SHORT)
                .show(); return false
        }
        return true
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Captura de Medidas",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = surface,
                shadowElevation = 8.dp
            ) {
                if (!primeraConfirmada) {
                    // CORREGIDO: El botón ahora guarda la medida actual Y agrega una nueva vacía
                    Button(
                        onClick = {
                            if (validarMedidaActual()) {
                                syncDraft()
                                primeraConfirmada = true
                                // Agregar nueva medida vacía y mover al siguiente índice
                                ventanas.add(VentanaFormState())
                                indexActual = ventanas.lastIndex
                                syncDraft()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp)
                            .shadow(if (isDarkMode) 0.dp else 8.dp, RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color.White else Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            null,
                            tint = if (isDarkMode) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Agregar Medida",
                            color = if (isDarkMode) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (validarMedidaActual()) {
                                    ventanas.add(VentanaFormState()); indexActual =
                                        ventanas.lastIndex; syncDraft()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.5.dp,
                                if (isDarkMode) Color.White else Color.Black
                            )
                        ) {
                            Icon(
                                Icons.Default.Add,
                                null,
                                tint = textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Agregar",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Button(
                            onClick = {
                                syncDraft()
                                val ventanasValidas = ventanas.mapIndexedNotNull { idx, v ->
                                    val desc = v.descripcion.trim()
                                    val alto = getNumericValue(v.alto).toDoubleOrNull()
                                    val ancho = getNumericValue(v.ancho).toDoubleOrNull()
                                    if (desc.isBlank() && (alto == null || alto <= 0.0) && (ancho == null || ancho <= 0.0)) return@mapIndexedNotNull null
                                    if (desc.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "Falta descripción en Medida #${idx + 1}",
                                            Toast.LENGTH_SHORT
                                        ).show(); return@Button
                                    }
                                    if (alto == null || alto <= 0.0) {
                                        Toast.makeText(
                                            context,
                                            "Alto inválido en Medida #${idx + 1}",
                                            Toast.LENGTH_SHORT
                                        ).show(); return@Button
                                    }
                                    if (ancho == null || ancho <= 0.0) {
                                        Toast.makeText(
                                            context,
                                            "Ancho inválido en Medida #${idx + 1}",
                                            Toast.LENGTH_SHORT
                                        ).show(); return@Button
                                    }
                                    Ventana(
                                        zona = v.zona.trim(),  // ✅ NUEVO
                                        descripcion = desc,
                                        alto = alto,
                                        ancho = ancho,
                                        precioM2 = HS875_DEFAULT_PRICE,
                                        adecuacion = if (v.adecuacion == "Sí") v.adecuacionDetalle.ifBlank { "Sí" } else "No",
                                        tipoMontaje = v.tipoMontaje
                                    )
                                }
                                if (ventanasValidas.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "Agrega al menos 1 medida válida",
                                        Toast.LENGTH_SHORT
                                    ).show(); return@Button
                                }

                                val especialista =
                                    SessionManager.getNombre(context).ifBlank { "Especialista" }
                                val fecha = draft.fecha.ifBlank {
                                    LocalDate.now()
                                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                }

                                val cotizacion = Cotizacion(
                                    folio = "",
                                    clienteNombre = draft.nombre.trim(),
                                    clienteTelefono = draft.telefono.trim(),
                                    ubicacion = listOf(
                                        draft.ciudad,
                                        draft.colonia,
                                        draft.direccionDetalle
                                    ).filter { it.isNotBlank() }.joinToString(", "),
                                    ciudad = draft.ciudad.trim(),
                                    especialista = especialista,
                                    fecha = fecha,
                                    producto = TipoProducto.HS875,
                                    productos = listOf(TipoProducto.HS875),
                                    tipoMontaje = draft.tipoMontaje,
                                    ventanas = ventanasValidas
                                )
                                onContinuarResumen(cotizacion)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .shadow(if (isDarkMode) 0.dp else 4.dp, RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color.White else Color.Black),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Siguiente",
                                color = if (isDarkMode) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                null,
                                tint = if (isDarkMode) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            StitchMedidasCarousel(
                ventanas.size,
                indexActual,
                { indexActual = it.coerceIn(0, (ventanas.size - 1).coerceAtLeast(0)) },
                isDarkMode,
                pillBg,
                pillText,
                carouselBg,
                carouselBorder,
                textMuted
            )
            Spacer(Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, border.copy(0.5f)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "APERTURA SELECCIONADA",
                                color = textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "APERTURA #${String.format("%02d", indexActual + 1)}",
                                color = textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        IconButton(onClick = {
                            if (ventanas.size > 1) {
                                ventanas.removeAt(indexActual); indexActual =
                                    (indexActual - 1).coerceAtLeast(0); syncDraft()
                                if (!ventanas.any { it.descripcion.isNotBlank() && it.alto.isNotBlank() && it.ancho.isNotBlank() }) primeraConfirmada =
                                    false
                            }
                        }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Outlined.Delete,
                                "Eliminar",
                                tint = textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    HorizontalDivider(thickness = 1.dp, color = border.copy(0.3f))
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // ✅ NUEVO: Campo ZONA antes de descripción
                        StitchInputFieldMaterial(
                            "ZONA",
                            actual.zona,
                            "Ej. Terraza, Sala, Recámara",
                            {
                                if (indexActual in ventanas.indices) {
                                    ventanas[indexActual] =
                                        ventanas[indexActual].copy(zona = it); syncDraft()
                                }
                            },
                            Icons.Default.Place,
                            isDarkMode,
                            textPrimary,
                            textLabel,
                            inputBg,
                            inputBorder
                        )
                        StitchInputFieldMaterial(
                            "ÁREA A PROTEGER",
                            actual.descripcion,
                            "Ej. Ventana, Puerta corrediza",
                            {
                                if (indexActual in ventanas.indices) {
                                    ventanas[indexActual] =
                                        ventanas[indexActual].copy(descripcion = it); syncDraft()
                                }
                            },
                            Icons.Default.Edit,
                            isDarkMode,
                            textPrimary,
                            textLabel,
                            inputBg,
                            inputBorder
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(Modifier.weight(1f)) {
                                MeasurementInputField(
                                    "ALTO (M)",
                                    actual.alto,
                                    "0.00",
                                    {
                                        if (indexActual in ventanas.indices) {
                                            ventanas[indexActual] = ventanas[indexActual].copy(
                                                alto = sanitizeDecimalInput(it)
                                            ); syncDraft()
                                        }
                                    },
                                    {
                                        if (indexActual in ventanas.indices && actual.alto.isNotBlank()) {
                                            ventanas[indexActual] = ventanas[indexActual].copy(
                                                alto = formatMeasurement(actual.alto)
                                            ); syncDraft()
                                        }
                                    },
                                    Icons.Default.Height,
                                    isDarkMode,
                                    textPrimary,
                                    textLabel,
                                    inputBg,
                                    inputBorder
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                MeasurementInputField(
                                    "ANCHO (M)",
                                    actual.ancho,
                                    "0.00",
                                    {
                                        if (indexActual in ventanas.indices) {
                                            ventanas[indexActual] = ventanas[indexActual].copy(
                                                ancho = sanitizeDecimalInput(it)
                                            ); syncDraft()
                                        }
                                    },
                                    {
                                        if (indexActual in ventanas.indices && actual.ancho.isNotBlank()) {
                                            ventanas[indexActual] = ventanas[indexActual].copy(
                                                ancho = formatMeasurement(actual.ancho)
                                            ); syncDraft()
                                        }
                                    },
                                    Icons.Default.SwapHoriz,
                                    isDarkMode,
                                    textPrimary,
                                    textLabel,
                                    inputBg,
                                    inputBorder
                                )
                            }
                        }
                        Text(
                            "TIPO DE MONTAJE",
                            color = textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StitchMontajeCardMaterial(
                                "FLUSH MOUNT",
                                Icons.Default.ViewStream,
                                actual.tipoMontaje == "Flush Mount",
                                Modifier.weight(1f),
                                isDarkMode,
                                textMuted,
                                border
                            ) {
                                if (indexActual in ventanas.indices) {
                                    ventanas[indexActual] =
                                        ventanas[indexActual].copy(tipoMontaje = "Flush Mount"); syncDraft()
                                }
                            }
                            StitchMontajeCardMaterial(
                                "TRAPEZOIDAL",
                                Icons.Default.ChangeHistory,
                                actual.tipoMontaje == "Trapezoidal",
                                Modifier.weight(1f),
                                isDarkMode,
                                textMuted,
                                border
                            ) {
                                if (indexActual in ventanas.indices) {
                                    ventanas[indexActual] =
                                        ventanas[indexActual].copy(tipoMontaje = "Trapezoidal"); syncDraft()
                                }
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = adecuacionesBg,
                        border = BorderStroke(1.dp, border.copy(0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "¿REQUIERE ADECUACIONES?",
                                color = textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            StitchToggleButtonMaterial(
                                if (actual.adecuacion == "Sí") "Sí" else "No",
                                {
                                    if (indexActual in ventanas.indices) {
                                        ventanas[indexActual] = ventanas[indexActual].copy(
                                            adecuacion = it,
                                            adecuacionDetalle = if (it == "No") "" else actual.adecuacionDetalle
                                        ); syncDraft()
                                    }
                                },
                                isDarkMode,
                                textMuted
                            )
                            AnimatedVisibility(
                                actual.adecuacion == "Sí",
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "ESPECIFIQUE ADECUACIONES",
                                        color = textMuted,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        actual.adecuacionDetalle,
                                        {
                                            if (indexActual in ventanas.indices) {
                                                ventanas[indexActual] =
                                                    ventanas[indexActual].copy(adecuacionDetalle = it); syncDraft()
                                            }
                                        },
                                        placeholder = {
                                            Text(
                                                "Ej. Tabla roca...",
                                                color = textMuted.copy(0.6f),
                                                fontSize = 14.sp
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = inputBg,
                                            unfocusedContainerColor = inputBg,
                                            focusedBorderColor = if (isDarkMode) Color.White else Color.Black,
                                            unfocusedBorderColor = inputBorder,
                                            focusedTextColor = textPrimary,
                                            unfocusedTextColor = textPrimary,
                                            cursorColor = textPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(120.dp))
        }
    }
}

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// COMPONENTES PRIVADOS
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
private fun MeasurementInputField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    leadingIcon: ImageVector,
    isDarkMode: Boolean,
    textPrimary: Color,
    textLabel: Color,
    inputBg: Color,
    inputBorder: Color
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            color = textLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value,
            { if (isFocused) onValueChange(it.replace("m", "").replace(" ", "").trim()) },
            placeholder = { Text(placeholder, color = textLabel.copy(0.5f), fontSize = 15.sp) },
            leadingIcon = {
                Icon(
                    leadingIcon,
                    null,
                    tint = textLabel,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .onFocusChanged {
                    if (isFocused && !it.isFocused) onFocusLost(); isFocused = it.isFocused
                },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = if (isDarkMode) Color.White else Color.Black,
                unfocusedBorderColor = inputBorder,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                cursorColor = textPrimary
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun StitchMedidasCarousel(
    total: Int,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    isDarkMode: Boolean,
    pillBg: Color,
    pillText: Color,
    carouselBg: Color,
    carouselBorder: Color,
    textMuted: Color
) {
    if (total <= 0) return
    val window = 5;
    val start = (selectedIndex - window / 2).coerceAtLeast(0)
        .coerceAtMost((total - window).coerceAtLeast(0));
    val end = (start + window).coerceAtMost(total)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = carouselBg,
            border = BorderStroke(1.dp, carouselBorder),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(enabled = selectedIndex > 0) {
                            if (selectedIndex > 0) onSelect(selectedIndex - 1)
                        }, contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        "Anterior",
                        tint = if (selectedIndex > 0) textMuted else textMuted.copy(0.25f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                for (i in start until end) {
                    val selected = i == selectedIndex
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 36.dp else 32.dp)
                            .clip(CircleShape)
                            .background(if (selected) pillBg else Color.Transparent)
                            .clickable { onSelect(i) }, contentAlignment = Alignment.Center
                    ) {
                        Text(
                            String.format("%02d", i + 1),
                            color = when {
                                selected -> pillText; kotlin.math.abs(i - selectedIndex) == 1 -> textMuted; else -> textMuted.copy(
                                    0.4f
                                )
                            },
                            fontSize = if (selected) 14.sp else 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(enabled = selectedIndex < total - 1) {
                            if (selectedIndex < total - 1) onSelect(selectedIndex + 1)
                        }, contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        "Siguiente",
                        tint = if (selectedIndex < total - 1) textMuted else textMuted.copy(0.25f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StitchInputFieldMaterial(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    leadingIcon: ImageVector,
    isDarkMode: Boolean,
    textPrimary: Color,
    textLabel: Color,
    inputBg: Color,
    inputBorder: Color,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            color = textLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value,
            onValueChange,
            placeholder = { Text(placeholder, color = textLabel.copy(0.5f), fontSize = 15.sp) },
            leadingIcon = {
                Icon(
                    leadingIcon,
                    null,
                    tint = textLabel,
                    modifier = Modifier.size(20.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = if (isDarkMode) Color.White else Color.Black,
                unfocusedBorderColor = inputBorder,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                cursorColor = textPrimary,
                focusedLeadingIconColor = if (isDarkMode) Color.White else Color.Black,
                unfocusedLeadingIconColor = textLabel
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun StitchMontajeCardMaterial(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier,
    isDarkMode: Boolean,
    textMuted: Color,
    border: Color,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        selected && isDarkMode -> Color.White; selected -> Color.Black; isDarkMode -> Color(
            0xFF18181B
        ); else -> Color.White
    }
    val contentColor = when {
        selected && isDarkMode -> Color.Black; selected -> Color.White; else -> textMuted
    }
    Surface(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (!selected) BorderStroke(1.dp, border) else null,
        shadowElevation = if (selected) 4.dp else 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(28.dp)); Spacer(
                Modifier.height(8.dp)
            ); Text(
                label,
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            }
            if (selected) Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color.Black else Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    "Seleccionado",
                    tint = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun StitchToggleButtonMaterial(
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    isDarkMode: Boolean,
    textMuted: Color
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB))
            .padding(4.dp)
    ) {
        val noSelected = selectedOption == "No"
        Surface(
            onClick = { onOptionSelected("No") },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            color = if (noSelected) (if (isDarkMode) Color.White else Color.Black) else Color.Transparent,
            shadowElevation = if (noSelected) 2.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (noSelected) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = if (isDarkMode) Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        ); Spacer(Modifier.width(6.dp))
                    }; Text(
                    "No",
                    color = if (noSelected) (if (isDarkMode) Color.Black else Color.White) else textMuted.copy(
                        0.6f
                    ),
                    fontWeight = if (noSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
                }
            }
        }
        val siSelected = selectedOption == "Sí"
        Surface(
            onClick = { onOptionSelected("Sí") },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            color = if (siSelected) (if (isDarkMode) Color.White else Color.Black) else Color.Transparent,
            shadowElevation = if (siSelected) 2.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (siSelected) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = if (isDarkMode) Color.Black else Color.White,
                            modifier = Modifier.size(16.dp)
                        ); Spacer(Modifier.width(6.dp))
                    }; Text(
                    "Sí",
                    color = when {
                        siSelected && isDarkMode -> Color.Black; siSelected -> Color.White; else -> textMuted.copy(
                            0.6f
                        )
                    },
                    fontWeight = if (siSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp
                )
                }
            }
        }
    }
}

// COMPONENTES LEGACY
@Composable
fun MontajeItem(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) (if (isDarkMode) Color.White else Color.Black) else (if (isDarkMode) Color(
            0xFF27272A
        ) else Color.White),
        border = if (!selected) BorderStroke(1.dp, Color.LightGray.copy(0.2f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) (if (isDarkMode) Color.Black else Color.White) else Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OptionAdecuacion(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val contentColor =
        if (selected) (if (isDarkMode) Color.Black else Color.White) else (if (isDarkMode) Color(
            0xFF71717A
        ) else Color(0xFF9CA3AF)); Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) (if (isDarkMode) Color.White else Color.Black) else Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}