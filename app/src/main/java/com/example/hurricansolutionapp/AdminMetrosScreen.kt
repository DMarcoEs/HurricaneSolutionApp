package com.example.hurricansolutionapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de Estadísticas de Metros Cuadrados - SIMPLIFICADA
 * Solo muestra: Total y Por Tipo de Sistema
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMetrosScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Colores
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)

    // Colores para sistemas
    val colorHS875 = Color(0xFF10B981)   // Verde
    val colorHS1250 = Color(0xFF3B82F6)  // Azul
    val colorHS1500 = Color(0xFF8B5CF6)  // Morado

    // Estados
    var isLoading by remember { mutableStateOf(true) }
    var cotizaciones by remember { mutableStateOf<List<CotizacionRemota>>(emptyList()) }

    // Filtros
    var periodoSeleccionado by remember { mutableStateOf("Mes") }
    var especialistaSeleccionado by remember { mutableStateOf<String?>(null) }
    var especialistas by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var showPeriodoPicker by remember { mutableStateOf(false) }
    var showEspecialistaPicker by remember { mutableStateOf(false) }

    val periodos = listOf("Semana", "Mes", "Año", "Todos")

    // Cargar datos de Supabase
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                cotizaciones = AdminRepository.getAllCotizaciones()
                especialistas = AdminRepository.getAllUsers()
            } catch (e: Exception) {
                android.util.Log.e("AdminMetros", "Error: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Filtrar cotizaciones por período
    val cotizacionesFiltradas = remember(cotizaciones, periodoSeleccionado, especialistaSeleccionado) {
        var filtered = cotizaciones

        val now = Calendar.getInstance()
        filtered = when (periodoSeleccionado) {
            "Semana" -> {
                val startOfWeek = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                filtered.filter { cot ->
                    val fecha = parseFecha(cot.fecha) ?: parseFecha(cot.createdAt)
                    fecha != null && fecha.time >= startOfWeek.timeInMillis
                }
            }
            "Mes" -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                filtered.filter { cot ->
                    val fecha = parseFecha(cot.fecha) ?: parseFecha(cot.createdAt)
                    fecha != null && fecha.time >= startOfMonth.timeInMillis
                }
            }
            "Año" -> {
                val startOfYear = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                filtered.filter { cot ->
                    val fecha = parseFecha(cot.fecha) ?: parseFecha(cot.createdAt)
                    fecha != null && fecha.time >= startOfYear.timeInMillis
                }
            }
            else -> filtered // "Todos"
        }

        // Filtrar por especialista si está seleccionado
        if (especialistaSeleccionado != null) {
            filtered = filtered.filter { it.userId == especialistaSeleccionado }
        }

        filtered
    }

    // Calcular estadísticas
    val stats = remember(cotizacionesFiltradas) {
        calcularEstadisticas(cotizacionesFiltradas)
    }

    val nombreEspecialista = remember(especialistaSeleccionado, especialistas) {
        if (especialistaSeleccionado == null) "Todos"
        else especialistas.find { it.id == especialistaSeleccionado }?.name ?: "Desconocido"
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "METROS CUADRADOS",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = textPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // FILTROS
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Filtro de período
                        Box(modifier = Modifier.weight(1f)) {
                            FilterChipButton(
                                label = "Período",
                                value = periodoSeleccionado,
                                onClick = { showPeriodoPicker = true },
                                isDarkMode = isDarkMode,
                                cardBg = cardBg,
                                textPrimary = textPrimary,
                                textMuted = textMuted,
                                border = border
                            )
                            DropdownMenu(
                                expanded = showPeriodoPicker,
                                onDismissRequest = { showPeriodoPicker = false },
                                modifier = Modifier.background(cardBg)
                            ) {
                                periodos.forEach { periodo ->
                                    DropdownMenuItem(
                                        text = { Text(periodo, color = textPrimary) },
                                        onClick = {
                                            periodoSeleccionado = periodo
                                            showPeriodoPicker = false
                                        }
                                    )
                                }
                            }
                        }

                        // Filtro de especialista
                        Box(modifier = Modifier.weight(1f)) {
                            FilterChipButton(
                                label = "Especialista",
                                value = nombreEspecialista,
                                onClick = { showEspecialistaPicker = true },
                                isDarkMode = isDarkMode,
                                cardBg = cardBg,
                                textPrimary = textPrimary,
                                textMuted = textMuted,
                                border = border
                            )
                            DropdownMenu(
                                expanded = showEspecialistaPicker,
                                onDismissRequest = { showEspecialistaPicker = false },
                                modifier = Modifier.background(cardBg)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todos", color = textPrimary) },
                                    onClick = {
                                        especialistaSeleccionado = null
                                        showEspecialistaPicker = false
                                    }
                                )
                                HorizontalDivider(color = border)
                                especialistas.forEach { esp ->
                                    DropdownMenuItem(
                                        text = { Text(esp.name, color = textPrimary) },
                                        onClick = {
                                            especialistaSeleccionado = esp.id
                                            showEspecialistaPicker = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // CARD: TOTAL COTIZADO
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isDarkMode) Color(0xFF18181B) else Color(0xFF111418),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "TOTAL COTIZADO",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${String.format("%,.2f", stats.totalMetros)} m²",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${stats.totalCotizaciones} cotización${if (stats.totalCotizaciones != 1) "es" else ""}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // CARD: POR TIPO DE SISTEMA
                item {
                    Text(
                        "POR TIPO DE SISTEMA",
                        color = textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = cardBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // HS-875
                            SistemaStatRow(
                                nombre = "HS-875",
                                subtitulo = "Polipropileno",
                                metros = stats.metrosHS875,
                                porcentaje = stats.getPorcentajeSistema("HS875"),
                                color = colorHS875,
                                textPrimary = textPrimary,
                                textMuted = textMuted
                            )

                            HorizontalDivider(color = border.copy(alpha = 0.5f))

                            // HS-1250
                            SistemaStatRow(
                                nombre = "HS-1250",
                                subtitulo = "Poliéster y Aramida",
                                metros = stats.metrosHS1250,
                                porcentaje = stats.getPorcentajeSistema("HS1250"),
                                color = colorHS1250,
                                textPrimary = textPrimary,
                                textMuted = textMuted
                            )

                            HorizontalDivider(color = border.copy(alpha = 0.5f))

                            // HS-1500
                            SistemaStatRow(
                                nombre = "HS-1500",
                                subtitulo = "Nylon Balístico",
                                metros = stats.metrosHS1500,
                                porcentaje = stats.getPorcentajeSistema("HS1500"),
                                color = colorHS1500,
                                textPrimary = textPrimary,
                                textMuted = textMuted
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textMuted: Color,
    border: Color
) {
    Surface(
        onClick = onClick,
        color = cardBg,
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
            Column {
                Text(label, color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text(value, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.KeyboardArrowDown, null, tint = textMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SistemaStatRow(
    nombre: String,
    subtitulo: String,
    metros: Double,
    porcentaje: Double,
    color: Color,
    textPrimary: Color,
    textMuted: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Column {
                Text(nombre, color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitulo, color = textMuted, fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${String.format("%,.2f", metros)} m²",
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${String.format("%.1f", porcentaje)}%",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Data class para estadísticas
private data class MetrosStats(
    val totalMetros: Double,
    val totalCotizaciones: Int,
    val metrosHS875: Double,
    val metrosHS1250: Double,
    val metrosHS1500: Double
) {
    fun getPorcentajeSistema(sistema: String): Double {
        if (totalMetros == 0.0) return 0.0
        val metros = when (sistema) {
            "HS875" -> metrosHS875
            "HS1250" -> metrosHS1250
            "HS1500" -> metrosHS1500
            else -> 0.0
        }
        return (metros / totalMetros) * 100
    }
}

// Función para parsear fechas
private fun parseFecha(fecha: String?): Date? {
    if (fecha.isNullOrBlank()) return null
    return try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(fecha.take(19))
    } catch (e: Exception) {
        try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(fecha)
        } catch (e2: Exception) {
            null
        }
    }
}

// Función para calcular estadísticas
private fun calcularEstadisticas(cotizaciones: List<CotizacionRemota>): MetrosStats {
    var totalMetros = 0.0
    var metrosHS875 = 0.0
    var metrosHS1250 = 0.0
    var metrosHS1500 = 0.0

    cotizaciones.forEach { cot ->
        val area = cot.areaTotal
        totalMetros += area

        // Sumar metros por sistema seleccionado en la cotización
        cot.productos.forEach { prod ->
            when (prod.uppercase()) {
                "HS875" -> metrosHS875 += area
                "HS1250" -> metrosHS1250 += area
                "HS1500" -> metrosHS1500 += area
            }
        }
    }

    return MetrosStats(
        totalMetros = totalMetros,
        totalCotizaciones = cotizaciones.size,
        metrosHS875 = metrosHS875,
        metrosHS1250 = metrosHS1250,
        metrosHS1500 = metrosHS1500
    )
}