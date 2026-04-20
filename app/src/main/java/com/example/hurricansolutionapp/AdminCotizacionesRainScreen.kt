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

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ADMIN COTIZACIONES RAIN SCREEN
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Pantalla para que el ADMIN vea todas las cotizaciones de Rain Protection.
 * Similar a AdminCotizacionesScreen pero para Rain.
 *
 * - Lista cotizaciones Rain de todos los especialistas
 * - Filtrar por especialista
 * - Buscar por cliente/folio
 * - Ver detalle (solo lectura)
 * - Descargar PDF
 * - NO permite editar (eso es solo para especialistas)
 */

// Color Rain
private val RainBlue = Color(0xFF2346AF)

enum class CotizacionRainSortOrder {
    RECIENTES_PRIMERO, ANTIGUOS_PRIMERO, NOMBRE_AZ, NOMBRE_ZA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCotizacionesRainScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onVerDetalle: (CotizacionRainRemota) -> Unit,
    filtroUsuarioInicial: String? = null
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var cotizaciones by remember { mutableStateOf<List<CotizacionRainRemota>>(emptyList()) }
    var empleados by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedEmpleado by remember { mutableStateOf<String?>(filtroUsuarioInicial) }
    var searchQuery by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(CotizacionRainSortOrder.RECIENTES_PRIMERO) }
    var showHideDialog by remember { mutableStateOf(false) }
    var cotizacionAOcultar by remember { mutableStateOf<CotizacionRainRemota?>(null) }

    // IDs ocultos visualmente (solo local del admin)
    val hiddenPrefs = remember { context.getSharedPreferences("admin_hidden_cotizaciones_rain", android.content.Context.MODE_PRIVATE) }
    var hiddenIds by remember {
        mutableStateOf(hiddenPrefs.getStringSet("hidden_ids", emptySet()) ?: emptySet())
    }

    LaunchedEffect(Unit) {
        isLoading = true
        cotizaciones = RainRepository.getAllCotizaciones()
        empleados = AdminRepository.getEspecialistas()
        isLoading = false
    }

    val cotizacionesFiltradas = remember(cotizaciones, selectedEmpleado, searchQuery, sortOrder, hiddenIds) {
        var result = cotizaciones
            .filter { cot ->
                val idStr = cot.id?.toString() ?: ""
                !hiddenIds.contains(idStr) &&
                        (selectedEmpleado == null || cot.userId == selectedEmpleado) &&
                        (searchQuery.isBlank() ||
                                cot.clienteNombre.contains(searchQuery, ignoreCase = true) ||
                                cot.folio.contains(searchQuery, ignoreCase = true) ||
                                cot.especialistaNombre.contains(searchQuery, ignoreCase = true))
            }

        when (sortOrder) {
            CotizacionRainSortOrder.RECIENTES_PRIMERO -> result.sortedByDescending { it.createdAt }
            CotizacionRainSortOrder.ANTIGUOS_PRIMERO -> result.sortedBy { it.createdAt }
            CotizacionRainSortOrder.NOMBRE_AZ -> result.sortedBy { it.clienteNombre.lowercase() }
            CotizacionRainSortOrder.NOMBRE_ZA -> result.sortedByDescending { it.clienteNombre.lowercase() }
        }
    }

    // Colores
    val bg = StitchColors.background(isDarkMode)
    val surface = StitchColors.surface(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val border = StitchColors.border(isDarkMode)
    val primary = RainBlue  // Usar azul Rain en lugar del primary normal
    val onPrimary = Color.White

    fun formatMoney(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)}"
    }

    fun descargarPdf(cotizacion: CotizacionRainRemota) {
        if (cotizacion.pdfPath.isNullOrBlank()) {
            Toast.makeText(context, "Esta cotización no tiene PDF", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            try {
                val bucket = SupabaseClientProvider.client.storage.from("cotizaciones-pdf")
                val bytes = withContext(Dispatchers.IO) {
                    bucket.downloadAuthenticated(cotizacion.pdfPath!!)
                }
                val fileName = cotizacion.pdfPath!!.substringAfterLast("/")
                val file = java.io.File(context.cacheDir, fileName)
                file.writeBytes(bytes)
                verPdf(context, file)
            } catch (e: Exception) {
                Toast.makeText(context, "Error al descargar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun ocultarCotizacion(cotizacion: CotizacionRainRemota) {
        val idStr = cotizacion.id?.toString() ?: return
        val newHidden = hiddenIds + idStr
        hiddenIds = newHidden
        hiddenPrefs.edit().putStringSet("hidden_ids", newHidden).apply()
        Toast.makeText(context, "Cotización ocultada", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Cotizaciones Rain",
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
            Spacer(Modifier.height(8.dp))

            // Barra de búsqueda y filtro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar...", color = textSecondary, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = textSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, null, tint = textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = border,
                        focusedContainerColor = surface,
                        unfocusedContainerColor = surface
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Button(
                    onClick = { showFilterSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.FilterList, null, tint = onPrimary, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Chips de filtro rápido
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    RainSortChip("Recientes", sortOrder == CotizacionRainSortOrder.RECIENTES_PRIMERO, { sortOrder = CotizacionRainSortOrder.RECIENTES_PRIMERO }, isDarkMode)
                }
                item {
                    RainSortChip("Antiguos", sortOrder == CotizacionRainSortOrder.ANTIGUOS_PRIMERO, { sortOrder = CotizacionRainSortOrder.ANTIGUOS_PRIMERO }, isDarkMode)
                }
                item {
                    RainSortChip("A-Z", sortOrder == CotizacionRainSortOrder.NOMBRE_AZ, { sortOrder = CotizacionRainSortOrder.NOMBRE_AZ }, isDarkMode)
                }
                item {
                    RainSortChip("Z-A", sortOrder == CotizacionRainSortOrder.NOMBRE_ZA, { sortOrder = CotizacionRainSortOrder.NOMBRE_ZA }, isDarkMode)
                }
            }

            // Filtro de empleado activo
            if (selectedEmpleado != null) {
                Spacer(Modifier.height(8.dp))
                val empName = empleados.find { it.id == selectedEmpleado }?.name ?: "Empleado"
                Surface(
                    onClick = { selectedEmpleado = null },
                    color = primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Person, null, tint = primary, modifier = Modifier.size(16.dp))
                        Text(empName, color = primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Icon(Icons.Default.Close, null, tint = primary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Contador
            Text(
                "${cotizacionesFiltradas.size} cotizaciones",
                color = textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(12.dp))

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
                            else "No hay cotizaciones Rain registradas",
                            color = textSecondary, fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items = cotizacionesFiltradas, key = { it.id ?: 0L }) { cotizacion ->
                        // Calcular etiqueta de versión
                        val versionesDelFolio = cotizacionesFiltradas
                            .filter { it.folio == cotizacion.folio }
                            .sortedBy { it.createdAt ?: "" }
                        val versionIndex = versionesDelFolio.indexOf(cotizacion)
                        val versionLabel = if (versionesDelFolio.size > 1) {
                            if (versionIndex == 0) "Cotización Original"
                            else "Edición $versionIndex"
                        } else null

                        RainCotizacionAdminCard(
                            cotizacion = cotizacion,
                            isDarkMode = isDarkMode,
                            formatMoney = ::formatMoney,
                            versionLabel = versionLabel,
                            onVerDetalle = { onVerDetalle(cotizacion) },
                            onDescargarPdf = { descargarPdf(cotizacion) },
                            onOcultar = {
                                cotizacionAOcultar = cotizacion
                                showHideDialog = true
                            }
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
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()) {
                Text("Filtrar por Especialista", color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))

                Surface(
                    onClick = { selectedEmpleado = null; showFilterSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (selectedEmpleado == null) primary.copy(alpha = 0.1f) else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, null, tint = textPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Todos los especialistas", color = textPrimary, fontWeight = FontWeight.Medium)
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
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    emp.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase()?.toString() }.joinToString(""),
                                    color = textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(emp.name, color = textPrimary, fontWeight = FontWeight.Medium)
                            if (selectedEmpleado == emp.id) {
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.Check, null, tint = primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Dialog para confirmar ocultar
    if (showHideDialog && cotizacionAOcultar != null) {
        AlertDialog(
            onDismissRequest = { showHideDialog = false; cotizacionAOcultar = null },
            containerColor = surface,
            title = { Text("Ocultar cotización", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas ocultar la cotización de ${cotizacionAOcultar?.clienteNombre}?\n\nEsto solo la oculta de tu vista, no la elimina.", color = textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        cotizacionAOcultar?.let { ocultarCotizacion(it) }
                        showHideDialog = false
                        cotizacionAOcultar = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("Ocultar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showHideDialog = false; cotizacionAOcultar = null }) {
                    Text("Cancelar", color = textPrimary, fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
private fun RainSortChip(label: String, selected: Boolean, onClick: () -> Unit, isDarkMode: Boolean) {
    val primary = RainBlue
    val surface = StitchColors.surface(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val border = StitchColors.border(isDarkMode)

    Surface(
        onClick = onClick,
        color = if (selected) primary else surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (selected) primary else border)
    ) {
        Text(
            label,
            color = if (selected) Color.White else textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * Card de cotización Rain para Admin (solo lectura)
 */
@Composable
private fun RainCotizacionAdminCard(
    cotizacion: CotizacionRainRemota,
    isDarkMode: Boolean,
    formatMoney: (Double) -> String,
    versionLabel: String? = null,
    onVerDetalle: () -> Unit,
    onDescargarPdf: () -> Unit,
    onOcultar: () -> Unit = {}
) {
    val surface = StitchColors.surface(isDarkMode)
    val border = StitchColors.border(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val primary = RainBlue
    val onPrimary = Color.White

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {

            // Header: Folio + Badge Rain + Especialista
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Folio
                    Surface(color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "#${cotizacion.folio}",
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Badge Rain Protection
                    Surface(
                        color = primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            "RAIN",
                            color = primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // Especialista
                Surface(
                    color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF9FAFB),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, border)
                ) {
                    Text(
                        formatearNombreCortoRain(cotizacion.especialistaNombre).uppercase(),
                        color = textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Nombre del cliente
            Text(
                cotizacion.clienteNombre,
                color = textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Fecha y ciudad
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
                        Text(
                            cotizacion.ciudad.substringBefore(","),
                            color = textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Etiqueta de versión
            if (versionLabel != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val isOriginal = versionLabel.contains("Original")
                    val labelColor = if (isOriginal) Color(0xFF3B82F6) else Color(0xFFF59E0B)
                    Icon(if (isOriginal) Icons.Default.Description else Icons.Default.Edit, null, tint = labelColor, modifier = Modifier.size(12.dp))
                    Text(versionLabel, color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Info de áreas
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Window, null, tint = textSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))

                // Áreas eléctricas
                if (cotizacion.areasElectricas > 0) {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "${cotizacion.areasElectricas} Eléctrica${if (cotizacion.areasElectricas > 1) "s" else ""}",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Áreas manuales
                if (cotizacion.areasManuales > 0) {
                    Surface(
                        color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "${cotizacion.areasManuales} Manual${if (cotizacion.areasManuales > 1) "es" else ""}",
                            color = Color(0xFF3B82F6),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text("TOTAL", color = textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(formatMoney(cotizacion.total), color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Botones: DETALLE, PDF, OCULTAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onVerDetalle,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, border)
                ) {
                    Icon(Icons.Outlined.Visibility, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("DETALLE", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                }

                Button(
                    onClick = onDescargarPdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, tint = onPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PDF", color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                }

                IconButton(onClick = onOcultar, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Default.VisibilityOff, null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/**
 * Formatea nombre a iniciales o nombre corto (versión Rain)
 */
private fun formatearNombreCortoRain(nombre: String): String {
    val partes = nombre.split(" ").filter { it.isNotBlank() }
    return when {
        partes.size >= 2 -> "${partes[0].first()}${partes[1].first()}"
        partes.isNotEmpty() -> partes[0].take(2)
        else -> "??"
    }
}