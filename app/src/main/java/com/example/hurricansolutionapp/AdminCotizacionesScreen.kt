package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Visibility
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
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

enum class CotizacionSortOrder {
    RECIENTES_PRIMERO, ANTIGUOS_PRIMERO, NOMBRE_AZ, NOMBRE_ZA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCotizacionesScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onVerDetalle: (CotizacionRemota) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var cotizaciones by remember { mutableStateOf<List<CotizacionRemota>>(emptyList()) }
    var empleados by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedEmpleado by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(CotizacionSortOrder.RECIENTES_PRIMERO) }

    LaunchedEffect(Unit) {
        isLoading = true
        cotizaciones = AdminRepository.getAllCotizaciones()
        empleados = AdminRepository.getEspecialistas()
        isLoading = false
    }

    val cotizacionesFiltradas = remember(cotizaciones, selectedEmpleado, searchQuery, sortOrder) {
        var result = cotizaciones
            .filter { cot ->
                (selectedEmpleado == null || cot.userId == selectedEmpleado) &&
                        (searchQuery.isBlank() ||
                                cot.clienteNombre.contains(searchQuery, ignoreCase = true) ||
                                cot.folio.contains(searchQuery, ignoreCase = true) ||
                                cot.especialistaNombre.contains(searchQuery, ignoreCase = true))
            }

        when (sortOrder) {
            CotizacionSortOrder.RECIENTES_PRIMERO -> result.sortedByDescending { it.createdAt }
            CotizacionSortOrder.ANTIGUOS_PRIMERO -> result.sortedBy { it.createdAt }
            CotizacionSortOrder.NOMBRE_AZ -> result.sortedBy { it.clienteNombre.lowercase() }
            CotizacionSortOrder.NOMBRE_ZA -> result.sortedByDescending { it.clienteNombre.lowercase() }
        }
    }

    val bg = StitchColors.background(isDarkMode)
    val surface = StitchColors.surface(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val border = StitchColors.border(isDarkMode)
    val primary = StitchColors.primary(isDarkMode)
    val onPrimary = StitchColors.onPrimary(isDarkMode)
    val greenColor = StitchColors.greenStandard

    fun formatMoney(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)}"
    }

    fun descargarPdf(cotizacion: CotizacionRemota) {
        if (cotizacion.pdfPath.isNullOrBlank()) {
            Toast.makeText(context, "Esta cotizacion no tiene PDF asociado", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            try {
                Toast.makeText(context, "Descargando PDF...", Toast.LENGTH_SHORT).show()
                val client = SupabaseClientProvider.client
                val bytes = withContext(Dispatchers.IO) {
                    client.storage.from("cotizaciones").downloadAuthenticated(cotizacion.pdfPath!!)
                }
                val fileName = "Cotizacion_${cotizacion.folio}.pdf"
                val file = java.io.File(context.getExternalFilesDir(null), fileName)
                file.writeBytes(bytes)
                verPdf(context, file)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al descargar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Cotizaciones",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Barra de busqueda + Boton Filtrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = surface,
                    border = BorderStroke(1.dp, border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null, tint = textSecondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text("Buscar por cliente, folio o empleado...", color = textSecondary, fontSize = 14.sp)
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = androidx.compose.ui.text.TextStyle(color = textPrimary, fontSize = 14.sp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, "Limpiar", tint = textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Boton Filtrar (con texto)
                Button(
                    onClick = { showFilterSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Icon(Icons.Default.FilterList, null, tint = onPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Filtrar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onPrimary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Chips de ordenamiento
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    StitchSortChip(
                        label = "Recientes",
                        selected = sortOrder == CotizacionSortOrder.RECIENTES_PRIMERO,
                        onClick = { sortOrder = CotizacionSortOrder.RECIENTES_PRIMERO },
                        isDarkMode = isDarkMode
                    )
                }
                item {
                    StitchSortChip(
                        label = "Antiguos",
                        selected = sortOrder == CotizacionSortOrder.ANTIGUOS_PRIMERO,
                        onClick = { sortOrder = CotizacionSortOrder.ANTIGUOS_PRIMERO },
                        isDarkMode = isDarkMode
                    )
                }
                item {
                    StitchSortChip(
                        label = "Nombre A-Z",
                        selected = sortOrder == CotizacionSortOrder.NOMBRE_AZ,
                        onClick = { sortOrder = CotizacionSortOrder.NOMBRE_AZ },
                        isDarkMode = isDarkMode
                    )
                }
                item {
                    StitchSortChip(
                        label = "Nombre Z-A",
                        selected = sortOrder == CotizacionSortOrder.NOMBRE_ZA,
                        onClick = { sortOrder = CotizacionSortOrder.NOMBRE_ZA },
                        isDarkMode = isDarkMode
                    )
                }
            }

            // Chip de filtro por empleado activo
            if (selectedEmpleado != null) {
                val empleadoNombre = empleados.find { it.id == selectedEmpleado }?.name ?: "Empleado"
                Spacer(Modifier.height(12.dp))
                AssistChip(
                    onClick = { selectedEmpleado = null },
                    label = { Text(empleadoNombre, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = { Icon(Icons.Default.Close, "Quitar filtro", modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = primary.copy(alpha = 0.1f), labelColor = textPrimary)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Contador
            Text("${cotizacionesFiltradas.size} cotizacion(es) encontrada(s)", color = textSecondary, fontSize = 13.sp)

            Spacer(Modifier.height(8.dp))

            // Titulo de seccion con barra lateral
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(primary)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "LISTA DE COTIZACIONES",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            // Lista de cotizaciones
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primary)
                }
            } else if (cotizacionesFiltradas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FolderOff, null, tint = textSecondary, modifier = Modifier.size(64.dp))
                        Text(
                            if (searchQuery.isNotBlank() || selectedEmpleado != null) "No se encontraron cotizaciones"
                            else "No hay cotizaciones registradas",
                            color = textSecondary, fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items = cotizacionesFiltradas, key = { it.folio }) { cotizacion ->
                        StitchCotizacionCard(
                            cotizacion = cotizacion,
                            isDarkMode = isDarkMode,
                            formatMoney = ::formatMoney,
                            onVerDetalle = { onVerDetalle(cotizacion) },
                            onDescargarPdf = { descargarPdf(cotizacion) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Bottom Sheet para filtrar por empleado
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }, containerColor = surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                Text("Filtrar por Empleado", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                Surface(
                    onClick = { selectedEmpleado = null; showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (selectedEmpleado == null) primary.copy(alpha = 0.1f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, null, tint = textPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Todos los empleados", color = textPrimary, fontWeight = FontWeight.Medium)
                        if (selectedEmpleado == null) {
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.Check, null, tint = primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = border)
                Spacer(Modifier.height(8.dp))

                empleados.forEach { emp ->
                    Surface(
                        onClick = { selectedEmpleado = emp.id; showFilterSheet = false },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (selectedEmpleado == emp.id) primary.copy(alpha = 0.1f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(StitchColors.surfaceVariant(isDarkMode)), contentAlignment = Alignment.Center) {
                                Text(emp.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase()?.toString() }.joinToString(""), color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(emp.name, color = textPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            if (selectedEmpleado == emp.id) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Check, null, tint = primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StitchSortChip(label: String, selected: Boolean, onClick: () -> Unit, isDarkMode: Boolean) {
    val primary = StitchColors.primary(isDarkMode)
    val onPrimary = StitchColors.onPrimary(isDarkMode)
    val surface = StitchColors.surface(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val border = StitchColors.border(isDarkMode)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) primary else surface,
        border = if (selected) null else BorderStroke(1.dp, border)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) onPrimary else textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun StitchCotizacionCard(
    cotizacion: CotizacionRemota,
    isDarkMode: Boolean,
    formatMoney: (Double) -> String,
    onVerDetalle: () -> Unit,
    onDescargarPdf: () -> Unit
) {
    val surface = StitchColors.surface(isDarkMode)
    val border = StitchColors.border(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val primary = StitchColors.primary(isDarkMode)
    val onPrimary = StitchColors.onPrimary(isDarkMode)
    val greenColor = StitchColors.greenStandard

    Surface(modifier = Modifier.fillMaxWidth(), color = surface, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, border)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Surface(color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6), shape = RoundedCornerShape(4.dp)) {
                    Text("#${cotizacion.folio}", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Surface(color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF9FAFB), shape = CircleShape, border = BorderStroke(1.dp, border)) {
                    Text(cotizacion.especialistaNombre.split(" ").take(2).joinToString(" ").uppercase(), color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(cotizacion.clienteNombre, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, tint = textSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(cotizacion.fecha, color = textSecondary, fontSize = 12.sp)
                }
                if (!cotizacion.ciudad.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = textSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(cotizacion.ciudad, color = textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("METRAJE", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("${String.format("%.2f", cotizacion.areaTotal)} m²", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                val maxTotal = maxOf(cotizacion.totalHs875 ?: 0.0, cotizacion.totalHs1250 ?: 0.0, cotizacion.totalHs1500 ?: 0.0)
                if (maxTotal > 0) {
                    Text(formatMoney(maxTotal), color = greenColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onVerDetalle, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, border)) {
                    Icon(Icons.Outlined.Visibility, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("DETALLE", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                }
                Button(onClick = onDescargarPdf, modifier = Modifier.weight(1f).height(44.dp), colors = ButtonDefaults.buttonColors(containerColor = primary), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.PictureAsPdf, null, tint = onPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PDF", color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}