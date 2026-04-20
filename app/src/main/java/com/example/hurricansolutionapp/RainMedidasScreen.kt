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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Pantalla de captura de medidas para Rain Protection
 * Copia exacta de CotizacionesFormScreen pero:
 * - Sin sección de "¿Requiere Adecuaciones?"
 * - Con "Tipo de Mecanismo" (Manual/Eléctrico) en lugar de "Tipo de Montaje"
 */

// Color Rain Protection
private val RainBlue = Color(0xFF2346AF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RainMedidasScreen(
    rainDraft: CotizacionRainDraft,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    var indexActual by rememberSaveable { mutableIntStateOf(0) }

    val tieneMedidasPrevias = remember(rainDraft.areas) {
        rainDraft.areas.any { it.descripcion.isNotBlank() && it.alto.isNotBlank() && it.ancho.isNotBlank() }
    }
    var primeraConfirmada by rememberSaveable { mutableStateOf(tieneMedidasPrevias) }

    val areas = remember {
        mutableStateListOf<RainAreaFormState>().apply {
            addAll(rainDraft.areas.ifEmpty { listOf(RainAreaFormState()) })
        }
    }

    LaunchedEffect(Unit) {
        if (tieneMedidasPrevias) {
            val lastValidIndex = areas.indexOfLast {
                it.descripcion.isNotBlank() && it.alto.isNotBlank() && it.ancho.isNotBlank()
            }
            if (lastValidIndex >= 0) indexActual = lastValidIndex
        }
    }

    fun syncDraft() {
        rainDraft.areas = areas.toMutableList()
    }

    val actual = areas.getOrNull(indexActual) ?: RainAreaFormState()

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
    val pillBg = if (isDarkMode) Color.White else RainBlue
    val pillText = Color.White

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    val titleSize = when {
        screenWidth < 360 -> 14.sp
        screenWidth < 400 -> 16.sp
        else -> 18.sp
    }

    val bodySize = when {
        screenWidth < 360 -> 12.sp
        screenWidth < 400 -> 13.sp
        else -> 14.sp
    }

    fun validarMedidaActual(): Boolean {
        val desc = actual.descripcion.trim()
        val altoNum = getNumericValue(actual.alto).toDoubleOrNull()
        val anchoNum = getNumericValue(actual.ancho).toDoubleOrNull()
        val piezasNum = actual.piezas.toIntOrNull()
        if (desc.isBlank()) {
            Toast.makeText(context, "Falta descripción", Toast.LENGTH_SHORT).show()
            return false
        }
        if (altoNum == null || altoNum <= 0.0) {
            Toast.makeText(context, "Ingresa un alto válido", Toast.LENGTH_SHORT).show()
            return false
        }
        if (anchoNum == null || anchoNum <= 0.0) {
            Toast.makeText(context, "Ingresa un ancho válido", Toast.LENGTH_SHORT).show()
            return false
        }
        if (anchoNum > 5.80) {
            Toast.makeText(context, "El ancho máximo por pieza es 5.80m", Toast.LENGTH_LONG).show()
            return false
        }
        if (piezasNum == null || piezasNum < 1) {
            Toast.makeText(context, "El número de piezas debe ser al menos 1", Toast.LENGTH_SHORT).show()
            return false
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
                    Button(
                        onClick = {
                            if (validarMedidaActual()) {
                                syncDraft()
                                primeraConfirmada = true
                                areas.add(RainAreaFormState())
                                indexActual = areas.lastIndex
                                syncDraft()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp)
                            .shadow(if (isDarkMode) 0.dp else 8.dp, RoundedCornerShape(12.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color.White else RainBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Agregar Medida",
                            color = Color.White,
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
                                    areas.add(RainAreaFormState())
                                    indexActual = areas.lastIndex
                                    syncDraft()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, if (isDarkMode) Color.White else RainBlue)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                null,
                                tint = textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Agregar otra",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Button(
                            onClick = {
                                syncDraft()
                                // Validar todas las áreas
                                val areasValidas = areas.mapIndexedNotNull { idx, a ->
                                    val desc = a.descripcion.trim()
                                    val alto = getNumericValue(a.alto).toDoubleOrNull()
                                    val ancho = getNumericValue(a.ancho).toDoubleOrNull()
                                    val piezasVal = a.piezas.toIntOrNull() ?: 1
                                    if (desc.isBlank() && (alto == null || alto <= 0.0) && (ancho == null || ancho <= 0.0)) {
                                        return@mapIndexedNotNull null
                                    }
                                    if (desc.isBlank()) {
                                        Toast.makeText(context, "Falta descripción en Área #${idx + 1}", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (alto == null || alto <= 0.0) {
                                        Toast.makeText(context, "Alto inválido en Área #${idx + 1}", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (ancho == null || ancho <= 0.0) {
                                        Toast.makeText(context, "Ancho inválido en Área #${idx + 1}", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (ancho > 5.80) {
                                        Toast.makeText(context, "Ancho máx. 5.80m en Área #${idx + 1}", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    if (piezasVal < 1) {
                                        Toast.makeText(context, "Piezas debe ser al menos 1 en Área #${idx + 1}", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    // Área válida - copiar datos limpios
                                    a.copy(
                                        descripcion = desc,
                                        alto = alto.toString(),
                                        ancho = ancho.toString(),
                                        piezas = piezasVal.toString()
                                    )
                                }

                                if (areasValidas.isEmpty()) {
                                    Toast.makeText(context, "Agrega al menos 1 área válida", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                // Guardar áreas validadas en el draft
                                rainDraft.areas = areasValidas.toMutableList()
                                onContinue()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .shadow(if (isDarkMode) 0.dp else 4.dp, RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color.White else RainBlue
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Terminar",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                null,
                                tint = Color.White,
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

            // Carrusel de medidas
            RainMedidasCarousel(
                areas.size,
                indexActual,
                { indexActual = it.coerceIn(0, (areas.size - 1).coerceAtLeast(0)) },
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
                    // Header con número de apertura y botón eliminar
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
                        IconButton(
                            onClick = {
                                if (areas.size > 1) {
                                    areas.removeAt(indexActual)
                                    indexActual = (indexActual - 1).coerceAtLeast(0)
                                    syncDraft()
                                    if (!areas.any { it.descripcion.isNotBlank() && it.alto.isNotBlank() && it.ancho.isNotBlank() }) {
                                        primeraConfirmada = false
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                "Eliminar",
                                tint = textMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = border.copy(0.3f))

                    // Campos del formulario
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Campo ZONA
                        RainInputField(
                            "ZONA",
                            actual.zona,
                            "Ej. Terraza, Sala, Recámara",
                            {
                                if (indexActual in areas.indices) {
                                    areas[indexActual] = areas[indexActual].copy(zona = it)
                                    syncDraft()
                                }
                            },
                            Icons.Default.Place,
                            isDarkMode,
                            textPrimary,
                            textLabel,
                            inputBg,
                            inputBorder
                        )

                        // Campo ÁREA A PROTEGER
                        RainInputField(
                            "ÁREA A PROTEGER",
                            actual.descripcion,
                            "Ej. Ventana, Puerta corrediza",
                            {
                                if (indexActual in areas.indices) {
                                    areas[indexActual] = areas[indexActual].copy(descripcion = it)
                                    syncDraft()
                                }
                            },
                            Icons.Default.Edit,
                            isDarkMode,
                            textPrimary,
                            textLabel,
                            inputBg,
                            inputBorder
                        )

                        // Campos ALTO y ANCHO
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(Modifier.weight(1f)) {
                                RainMeasurementField(
                                    "ALTO (M)",
                                    actual.alto,
                                    "0.00",
                                    {
                                        if (indexActual in areas.indices) {
                                            areas[indexActual] = areas[indexActual].copy(
                                                alto = sanitizeDecimalInput(it)
                                            )
                                            syncDraft()
                                        }
                                    },
                                    {
                                        if (indexActual in areas.indices && actual.alto.isNotBlank()) {
                                            areas[indexActual] = areas[indexActual].copy(
                                                alto = formatMeasurement(actual.alto)
                                            )
                                            syncDraft()
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
                                RainMeasurementField(
                                    "ANCHO (M)",
                                    actual.ancho,
                                    "0.00",
                                    {
                                        if (indexActual in areas.indices) {
                                            areas[indexActual] = areas[indexActual].copy(
                                                ancho = sanitizeDecimalInput(it)
                                            )
                                            syncDraft()
                                        }
                                    },
                                    {
                                        if (indexActual in areas.indices && actual.ancho.isNotBlank()) {
                                            areas[indexActual] = areas[indexActual].copy(
                                                ancho = formatMeasurement(actual.ancho)
                                            )
                                            syncDraft()
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

                        // ═══════════════════════════════════════════════════════════════
                        // NÚMERO DE PIEZAS
                        // ═══════════════════════════════════════════════════════════════
                        RainInputField(
                            "NÚMERO DE PIEZAS",
                            actual.piezas,
                            "1",
                            {
                                if (indexActual in areas.indices) {
                                    val filtered = it.filter { c -> c.isDigit() }.take(2)
                                    areas[indexActual] = areas[indexActual].copy(piezas = filtered)
                                    syncDraft()
                                }
                            },
                            Icons.Default.ContentCopy,
                            isDarkMode,
                            textPrimary,
                            textLabel,
                            inputBg,
                            inputBorder,
                            keyboardType = KeyboardType.Number
                        )

                        // ═══════════════════════════════════════════════════════════════
                        // TIPO DE MECANISMO - Selección múltiple (puede elegir ambos)
                        // ═══════════════════════════════════════════════════════════════
                        Text(
                            "TIPO DE MECANISMO",
                            color = textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            "Puedes seleccionar uno o ambos",
                            color = textMuted.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Opción MANUAL (checkbox style)
                            RainMecanismoCardMultiple(
                                "MANUAL",
                                Icons.Default.Settings,
                                actual.incluyeManual,
                                Modifier.weight(1f),
                                isDarkMode,
                                textMuted,
                                border
                            ) {
                                if (indexActual in areas.indices) {
                                    val currentManual = areas[indexActual].incluyeManual
                                    val currentElectrico = areas[indexActual].incluyeElectrico
                                    // No permitir deseleccionar ambos
                                    if (!(!currentManual && !currentElectrico) || currentElectrico) {
                                        areas[indexActual] = areas[indexActual].copy(
                                            incluyeManual = !currentManual
                                        )
                                        syncDraft()
                                    }
                                }
                            }
                            // Opción ELÉCTRICO (checkbox style)
                            RainMecanismoCardMultiple(
                                "ELÉCTRICO",
                                Icons.Default.FlashOn,
                                actual.incluyeElectrico,
                                Modifier.weight(1f),
                                isDarkMode,
                                textMuted,
                                border
                            ) {
                                if (indexActual in areas.indices) {
                                    val currentManual = areas[indexActual].incluyeManual
                                    val currentElectrico = areas[indexActual].incluyeElectrico
                                    // No permitir deseleccionar ambos
                                    if (!(!currentManual && !currentElectrico) || currentManual) {
                                        areas[indexActual] = areas[indexActual].copy(
                                            incluyeElectrico = !currentElectrico
                                        )
                                        syncDraft()
                                    }
                                }
                            }
                        }

                        // ═══════════════════════════════════════════════════════════════
                        // SIN SECCIÓN DE ADECUACIONES
                        // ═══════════════════════════════════════════════════════════════
                    }
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTES PRIVADOS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RainMeasurementField(
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
                Icon(leadingIcon, null, tint = textLabel, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .onFocusChanged {
                    if (isFocused && !it.isFocused) onFocusLost()
                    isFocused = it.isFocused
                },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = if (isDarkMode) Color.White else RainBlue,
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
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun RainMedidasCarousel(
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
    val window = 5
    val start = (selectedIndex - window / 2).coerceAtLeast(0)
        .coerceAtMost((total - window).coerceAtLeast(0))
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
                        },
                    contentAlignment = Alignment.Center
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
                            .clickable { onSelect(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            String.format("%02d", i + 1),
                            color = when {
                                selected -> pillText
                                kotlin.math.abs(i - selectedIndex) == 1 -> textMuted
                                else -> textMuted.copy(0.4f)
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
                        },
                    contentAlignment = Alignment.Center
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
private fun RainInputField(
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
                Icon(leadingIcon, null, tint = textLabel, modifier = Modifier.size(20.dp))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = if (isDarkMode) Color.White else RainBlue,
                unfocusedBorderColor = inputBorder,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                cursorColor = textPrimary,
                focusedLeadingIconColor = if (isDarkMode) Color.White else RainBlue,
                unfocusedLeadingIconColor = textLabel
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun RainMecanismoCard(
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
        selected && isDarkMode -> Color.White
        selected -> Color.Black
        isDarkMode -> Color(0xFF18181B)
        else -> Color.White
    }
    val contentColor = when {
        selected && isDarkMode -> Color.Black
        selected -> Color.White
        else -> textMuted
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
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    label,
                    color = contentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            if (selected) {
                Box(
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
}

/**
 * Card de mecanismo para selección MÚLTIPLE (checkbox style)
 * Usa RainBlue en lugar de negro cuando está seleccionado
 */
@Composable
private fun RainMecanismoCardMultiple(
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
        selected && isDarkMode -> Color.White
        selected -> RainBlue  // Usar azul Rain en lugar de negro
        isDarkMode -> Color(0xFF18181B)
        else -> Color.White
    }
    val contentColor = when {
        selected && isDarkMode -> RainBlue
        selected -> Color.White
        else -> textMuted
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (!selected) BorderStroke(1.dp, border) else BorderStroke(2.dp, RainBlue),
        shadowElevation = if (selected) 4.dp else 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    label,
                    color = contentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            // Checkbox indicator
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selected) (if (isDarkMode) RainBlue else Color.White) else border.copy(alpha = 0.3f))
                    .border(1.dp, if (selected) RainBlue else border, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        Icons.Default.Check,
                        "Seleccionado",
                        tint = if (isDarkMode) Color.White else RainBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}