package com.example.hurricansolutionapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de Estadísticas de Metros Cuadrados
 *
 * Datos desde SUPABASE (sincronizados)
 * Filtros: Semana, Mes, Año, Todos + Por Especialista
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
    val primary = if (isDarkMode) Color.White else Color.Black
    val onPrimary = if (isDarkMode) Color.Black else Color.White

    // Colores para sistemas
    val colorHS875 = Color(0xFF10B981)
    val colorHS1250 = Color(0xFF3B82F6)
    val colorHS1500 = Color(0xFF8B5CF6)

    // Colores para zonas
    val colorContinental = Color(0xFF06B6D4)
    val colorPeninsular = Color(0xFFF59E0B)
    val colorIsla = Color(0xFFEF4444)

    // Estados
    var isLoading by remember { mutableStateOf(true) }
    var cotizaciones by remember { mutableStateOf<List<CotizacionRemota>>(emptyList()) }
    var especialistas by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    // Filtros
    var periodoSeleccionado by remember { mutableStateOf("Mes") }
    var especialistaSeleccionado by remember { mutableStateOf<String?>(null) }
    var showPeriodoPicker by remember { mutableStateOf(false) }
    var showEspecialistaPicker by remember { mutableStateOf(false) }

    val periodos = listOf("Semana", "Mes", "Año", "Todos")

    // Cargar datos de Supabase
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                cotizaciones = AdminRepository.getAllCotizaciones()
                especialistas = AdminRepository.getAllUsers().filter {
                    it.role.equals("SPECIALIST", ignoreCase = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminMetros", "Error: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Filtrar cotizaciones
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
                    val fecha = parseFechaSupabase(cot.fecha) ?: parseFechaSupabase(cot.createdAt)
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
                    val fecha = parseFechaSupabase(cot.fecha) ?: parseFechaSupabase(cot.createdAt)
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
                    val fecha = parseFechaSupabase(cot.fecha) ?: parseFechaSupabase(cot.createdAt)
                    fecha != null && fecha.time >= startOfYear.timeInMillis
                }
            }
            else -> filtered
        }

        if (especialistaSeleccionado != null) {
            filtered = filtered.filter { it.userId == especialistaSeleccionado }
        }

        filtered
    }

    val estadisticas = remember(cotizacionesFiltradas) {
        calcularEstadisticasSupabase(cotizacionesFiltradas, especialistas)
    }

    val nombreEspecialistaSeleccionado = remember(especialistaSeleccionado, especialistas) {
        if (especialistaSeleccionado == null) "Todos"
        else especialistas.find { it.id == especialistaSeleccionado }?.name ?: "Desconocido"
    }

    // Diálogo selector de período
    if (showPeriodoPicker) {
        AlertDialog(
            onDismissRequest = { showPeriodoPicker = false },
            containerColor = cardBg,
            title = { Text("Seleccionar período", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    periodos.forEach { periodo ->
                        val isSelected = periodo == periodoSeleccionado
                        Surface(
                            onClick = {
                                periodoSeleccionado = periodo
                                showPeriodoPicker = false
                            },
                            color = if (isSelected) primary.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    when (periodo) {
                                        "Semana" -> "Esta semana"
                                        "Mes" -> "Este mes"
                                        "Año" -> "Este año"
                                        else -> "Todo el tiempo"
                                    },
                                    color = if (isSelected) textPrimary else textMuted,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, null, tint = primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showPeriodoPicker = false }) { Text("Cerrar", color = textMuted) } }
        )
    }

    // Diálogo selector de especialista
    if (showEspecialistaPicker) {
        AlertDialog(
            onDismissRequest = { showEspecialistaPicker = false },
            containerColor = cardBg,
            title = { Text("Filtrar por especialista", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item {
                        val isSelected = especialistaSeleccionado == null
                        Surface(
                            onClick = {
                                especialistaSeleccionado = null
                                showEspecialistaPicker = false
                            },
                            color = if (isSelected) primary.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Todos", color = if (isSelected) textPrimary else textMuted, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                if (isSelected) Icon(Icons.Default.Check, null, tint = primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    items(especialistas) { esp ->
                        val isSelected = esp.id == especialistaSeleccionado
                        Surface(
                            onClick = {
                                especialistaSeleccionado = esp.id
                                showEspecialistaPicker = false
                            },
                            color = if (isSelected) primary.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(esp.name, color = if (isSelected) textPrimary else textMuted, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                if (isSelected) Icon(Icons.Default.Check, null, tint = primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showEspecialistaPicker = false }) { Text("Cerrar", color = textMuted) } }
        )
    }

    Scaffold(
        containerColor = bg,
        topBar = { StitchTopBar(title = "Metros Cuadrados", onBack = onBack, isDarkMode = isDarkMode) }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = textPrimary, strokeWidth = 2.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // FILTROS
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            onClick = { showPeriodoPicker = true },
                            modifier = Modifier.weight(1f),
                            color = cardBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, border)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.CalendarMonth, null, tint = textMuted, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("Período", color = textMuted, fontSize = 10.sp)
                                        Text(
                                            when (periodoSeleccionado) { "Semana" -> "Semana"; "Mes" -> "Mes"; "Año" -> "Año"; else -> "Todos" },
                                            color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = textMuted, modifier = Modifier.size(18.dp))
                            }
                        }

                        Surface(
                            onClick = { showEspecialistaPicker = true },
                            modifier = Modifier.weight(1f),
                            color = cardBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, border)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Person, null, tint = textMuted, modifier = Modifier.size(18.dp))
                                    Column {
                                        Text("Especialista", color = textMuted, fontSize = 10.sp)
                                        Text(nombreEspecialistaSeleccionado, color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = textMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // TOTAL
                item {
                    Surface(color = primary, shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL COTIZADO", color = onPrimary.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("${String.format("%,.2f", estadisticas.totalMetros)} m²", color = onPrimary, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("${estadisticas.totalCotizaciones} cotización${if (estadisticas.totalCotizaciones != 1) "es" else ""}", color = onPrimary.copy(alpha = 0.7f), fontSize = 14.sp)
                        }
                    }
                }

                // POR SISTEMA
                item { Text("POR TIPO DE SISTEMA", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                item {
                    Surface(color = cardBg, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, border)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SistemaStatRow("HS-875", "Polipropileno", estadisticas.metrosHS875, estadisticas.getPorcentajeSistema("HS875"), colorHS875, textPrimary, textMuted)
                            HorizontalDivider(color = border.copy(alpha = 0.5f))
                            SistemaStatRow("HS-1250", "Poliéster y Aramida", estadisticas.metrosHS1250, estadisticas.getPorcentajeSistema("HS1250"), colorHS1250, textPrimary, textMuted)
                            HorizontalDivider(color = border.copy(alpha = 0.5f))
                            SistemaStatRow("HS-1500", "Nylon Balístico", estadisticas.metrosHS1500, estadisticas.getPorcentajeSistema("HS1500"), colorHS1500, textPrimary, textMuted)
                        }
                    }
                }

                // POR ZONA
                item { Text("POR ZONA GEOGRÁFICA", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                item {
                    Surface(color = cardBg, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, border)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ZonaStatRow("Continental", estadisticas.metrosContinental, estadisticas.cotsContinental, estadisticas.getPorcentajeZona("continental"), colorContinental, textPrimary, textMuted)
                            HorizontalDivider(color = border.copy(alpha = 0.5f))
                            ZonaStatRow("Peninsular", estadisticas.metrosPeninsular, estadisticas.cotsPeninsular, estadisticas.getPorcentajeZona("peninsular"), colorPeninsular, textPrimary, textMuted)
                            HorizontalDivider(color = border.copy(alpha = 0.5f))
                            ZonaStatRow("Isla", estadisticas.metrosIsla, estadisticas.cotsIsla, estadisticas.getPorcentajeZona("isla"), colorIsla, textPrimary, textMuted)
                        }
                    }
                }

                // RANKING (solo si no hay filtro de especialista)
                if (especialistaSeleccionado == null && estadisticas.rankingEspecialistas.isNotEmpty()) {
                    item { Text("RANKING DE ESPECIALISTAS", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                    items(estadisticas.rankingEspecialistas) { esp ->
                        EspecialistaCard(esp, isDarkMode, cardBg, textPrimary, textMuted, border, colorHS875, colorHS1250, colorHS1500)
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SistemaStatRow(nombre: String, subtitulo: String, metros: Double, porcentaje: Double, color: Color, textPrimary: Color, textMuted: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
            Column {
                Text(nombre, color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitulo, color = textMuted, fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${String.format("%,.2f", metros)} m²", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${String.format("%.1f", porcentaje)}%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ZonaStatRow(nombre: String, metros: Double, cotizaciones: Int, porcentaje: Double, color: Color, textPrimary: Color, textMuted: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
            Column {
                Text(nombre, color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("$cotizaciones cotización${if (cotizaciones != 1) "es" else ""}", color = textMuted, fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${String.format("%,.2f", metros)} m²", color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("${String.format("%.1f", porcentaje)}%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EspecialistaCard(especialista: EspecialistaStatsSupabase, isDarkMode: Boolean, cardBg: Color, textPrimary: Color, textMuted: Color, border: Color, colorHS875: Color, colorHS1250: Color, colorHS1500: Color) {
    var expanded by remember { mutableStateOf(false) }
    Surface(onClick = { expanded = !expanded }, color = cardBg, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, border)) {
        Column(modifier = Modifier.fillMaxWidth().animateContentSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = when (especialista.posicion) { 1 -> Color(0xFFFFD700); 2 -> Color(0xFFC0C0C0); 3 -> Color(0xFFCD7F32); else -> if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB) },
                        shape = CircleShape
                    ) {
                        Text("#${especialista.posicion}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (especialista.posicion <= 3) Color.Black else textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(especialista.nombre, color = textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${especialista.totalCotizaciones} cotización${if (especialista.totalCotizaciones != 1) "es" else ""}", color = textMuted, fontSize = 12.sp)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${String.format("%,.2f", especialista.totalMetros)} m²", color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, tint = textMuted, modifier = Modifier.size(20.dp))
                }
            }
            if (expanded) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = border.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))
                Text("Desglose por sistema:", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${String.format("%.2f", especialista.metrosHS875)} m²", color = colorHS875, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("HS-875", color = textMuted, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${String.format("%.2f", especialista.metrosHS1250)} m²", color = colorHS1250, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("HS-1250", color = textMuted, fontSize = 11.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${String.format("%.2f", especialista.metrosHS1500)} m²", color = colorHS1500, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("HS-1500", color = textMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private data class EstadisticasSupabase(
    val totalMetros: Double, val totalCotizaciones: Int,
    val metrosHS875: Double, val metrosHS1250: Double, val metrosHS1500: Double,
    val metrosContinental: Double, val metrosPeninsular: Double, val metrosIsla: Double,
    val cotsContinental: Int, val cotsPeninsular: Int, val cotsIsla: Int,
    val rankingEspecialistas: List<EspecialistaStatsSupabase>
) {
    fun getPorcentajeSistema(sistema: String): Double {
        if (totalMetros == 0.0) return 0.0
        return (when (sistema) { "HS875" -> metrosHS875; "HS1250" -> metrosHS1250; "HS1500" -> metrosHS1500; else -> 0.0 } / totalMetros) * 100
    }
    fun getPorcentajeZona(zona: String): Double {
        if (totalMetros == 0.0) return 0.0
        return (when (zona) { "continental" -> metrosContinental; "peninsular" -> metrosPeninsular; "isla" -> metrosIsla; else -> 0.0 } / totalMetros) * 100
    }
}

private data class EspecialistaStatsSupabase(val id: String, val nombre: String, val posicion: Int, val totalMetros: Double, val totalCotizaciones: Int, val metrosHS875: Double, val metrosHS1250: Double, val metrosHS1500: Double)

private fun parseFechaSupabase(fecha: String?): Date? {
    if (fecha.isNullOrBlank()) return null
    return try { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(fecha.take(19)) }
    catch (e: Exception) { try { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(fecha) } catch (e2: Exception) { null } }
}

private fun calcularEstadisticasSupabase(cotizaciones: List<CotizacionRemota>, especialistas: List<UserProfile>): EstadisticasSupabase {
    var totalMetros = 0.0; var metrosHS875 = 0.0; var metrosHS1250 = 0.0; var metrosHS1500 = 0.0
    var metrosContinental = 0.0; var metrosPeninsular = 0.0; var metrosIsla = 0.0
    var cotsContinental = 0; var cotsPeninsular = 0; var cotsIsla = 0
    val especialistasMap = mutableMapOf<String, MutableList<CotizacionRemota>>()

    cotizaciones.forEach { cot ->
        val area = cot.areaTotal
        totalMetros += area
        cot.productos.forEach { prod ->
            when (prod.uppercase()) { "HS875" -> metrosHS875 += area; "HS1250" -> metrosHS1250 += area; "HS1500" -> metrosHS1500 += area }
        }
        when (cot.zonaGeografica?.lowercase()) {
            "continental" -> { metrosContinental += area; cotsContinental++ }
            "foranea", "peninsular" -> { metrosPeninsular += area; cotsPeninsular++ }
            "islas", "isla" -> { metrosIsla += area; cotsIsla++ }
            else -> { metrosContinental += area; cotsContinental++ }
        }
        especialistasMap.getOrPut(cot.userId) { mutableListOf() }.add(cot)
    }

    val ranking = especialistasMap.map { (userId, cots) ->
        val nombre = especialistas.find { it.id == userId }?.name ?: cots.firstOrNull()?.especialistaNombre ?: "Desconocido"
        var hs875 = 0.0; var hs1250 = 0.0; var hs1500 = 0.0
        cots.forEach { cot -> val area = cot.areaTotal; cot.productos.forEach { prod -> when (prod.uppercase()) { "HS875" -> hs875 += area; "HS1250" -> hs1250 += area; "HS1500" -> hs1500 += area } } }
        EspecialistaStatsSupabase(userId, nombre, 0, cots.sumOf { it.areaTotal }, cots.size, hs875, hs1250, hs1500)
    }.sortedByDescending { it.totalMetros }.mapIndexed { i, s -> s.copy(posicion = i + 1) }

    return EstadisticasSupabase(totalMetros, cotizaciones.size, metrosHS875, metrosHS1250, metrosHS1500, metrosContinental, metrosPeninsular, metrosIsla, cotsContinental, cotsPeninsular, cotsIsla, ranking)
}