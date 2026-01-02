package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCotizacionesScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit
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

    // Cargar datos
    LaunchedEffect(Unit) {
        isLoading = true
        cotizaciones = AdminRepository.getAllCotizaciones()
        empleados = AdminRepository.getEspecialistas()
        isLoading = false
    }

    // Filtrar cotizaciones
    val cotizacionesFiltradas = remember(cotizaciones, selectedEmpleado, searchQuery) {
        cotizaciones
            .filter { cot ->
                (selectedEmpleado == null || cot.userId == selectedEmpleado) &&
                        (searchQuery.isBlank() ||
                                cot.clienteNombre.contains(searchQuery, ignoreCase = true) ||
                                cot.folio.contains(searchQuery, ignoreCase = true) ||
                                cot.especialistaNombre.contains(searchQuery, ignoreCase = true))
            }
            .sortedByDescending { it.createdAt }
    }

    // Colores
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val card = if (isDarkMode) Color(0xFF18181B) else Color.White
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    fun formatMoney(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)}"
    }

    // Función para descargar PDF
    fun descargarPdf(cotizacion: CotizacionRemota) {
        if (cotizacion.pdfPath.isNullOrBlank()) {
            Toast.makeText(context, "Esta cotización no tiene PDF asociado", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                Toast.makeText(context, "Descargando PDF...", Toast.LENGTH_SHORT).show()

                val client = SupabaseClientProvider.client
                val bytes = withContext(Dispatchers.IO) {
                    client.storage.from("cotizaciones")
                        .downloadAuthenticated(cotizacion.pdfPath!!)
                }

                // Guardar archivo localmente
                val fileName = "Cotizacion_${cotizacion.folio}.pdf"
                val file = java.io.File(context.getExternalFilesDir(null), fileName)
                file.writeBytes(bytes)

                // Abrir PDF
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
            Column(modifier = Modifier.background(surface)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = surface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = textPrimary)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "COTIZACIONES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.weight(1f))

                        // Botón de filtro
                        IconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Badge(
                                containerColor = if (selectedEmpleado != null) Color(0xFF3B82F6) else Color.Transparent
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filtrar",
                                    tint = textPrimary
                                )
                            }
                        }
                    }
                }

                // Barra de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Buscar por cliente, folio o empleado...",
                            color = textMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = textMuted
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF3F4F6),
                        unfocusedContainerColor = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF3F4F6),
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    singleLine = true
                )

                // Chip de filtro activo
                if (selectedEmpleado != null) {
                    val empleadoNombre = empleados.find { it.id == selectedEmpleado }?.name ?: "Empleado"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        AssistChip(
                            onClick = { selectedEmpleado = null },
                            label = { Text(empleadoNombre, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Quitar filtro",
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF3B82F6).copy(alpha = 0.1f),
                                labelColor = Color(0xFF3B82F6)
                            )
                        )
                    }
                }

                HorizontalDivider(color = border.copy(0.5f))
            }
        }
    ) { innerPadding ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            }
        } else if (cotizacionesFiltradas.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOff,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        if (searchQuery.isNotBlank() || selectedEmpleado != null)
                            "No se encontraron cotizaciones"
                        else
                            "No hay cotizaciones registradas",
                        color = textMuted,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Header con contador
                item {
                    Text(
                        "${cotizacionesFiltradas.size} cotización(es) encontrada(s)",
                        color = textMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(
                    items = cotizacionesFiltradas,
                    key = { it.folio }
                ) { cotizacion ->
                    AdminCotizacionCard(
                        cotizacion = cotizacion,
                        isDarkMode = isDarkMode,
                        card = card,
                        border = border,
                        textPrimary = textPrimary,
                        textMuted = textMuted,
                        formatMoney = ::formatMoney,
                        onDescargarPdf = { descargarPdf(cotizacion) }
                    )
                }
            }
        }
    }

    // Bottom Sheet para filtrar por empleado
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "Filtrar por Empleado",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Opción "Todos"
                Surface(
                    onClick = {
                        selectedEmpleado = null
                        showFilterSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (selectedEmpleado == null) Color(0xFF3B82F6).copy(alpha = 0.1f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            tint = if (selectedEmpleado == null) Color(0xFF3B82F6) else textMuted
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Todos los empleados",
                            color = if (selectedEmpleado == null) Color(0xFF3B82F6) else textPrimary,
                            fontWeight = if (selectedEmpleado == null) FontWeight.Bold else FontWeight.Normal
                        )
                        if (selectedEmpleado == null) {
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = border)
                Spacer(Modifier.height(8.dp))

                // Lista de empleados
                empleados.forEach { empleado ->
                    val cotCount = cotizaciones.count { it.userId == empleado.id }

                    Surface(
                        onClick = {
                            selectedEmpleado = empleado.id
                            showFilterSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (selectedEmpleado == empleado.id) Color(0xFF3B82F6).copy(alpha = 0.1f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    empleado.name.take(2).uppercase(),
                                    color = Color(0xFF3B82F6),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    empleado.name,
                                    color = if (selectedEmpleado == empleado.id) Color(0xFF3B82F6) else textPrimary,
                                    fontWeight = if (selectedEmpleado == empleado.id) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    "$cotCount cotización(es)",
                                    color = textMuted,
                                    fontSize = 12.sp
                                )
                            }

                            if (selectedEmpleado == empleado.id) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF3B82F6)
                                )
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
private fun AdminCotizacionCard(
    cotizacion: CotizacionRemota,
    isDarkMode: Boolean,
    card: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color,
    formatMoney: (Double) -> String,
    onDescargarPdf: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = card,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, border.copy(0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Folio + Empleado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    // Folio
                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFF3B82F6).copy(alpha = 0.1f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "#${cotizacion.folio}",
                            color = Color(0xFF3B82F6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Nombre cliente
                    Text(
                        cotizacion.clienteNombre,
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badge empleado
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        cotizacion.especialistaNombre.split(" ").take(2).joinToString(" "),
                        color = Color(0xFF8B5CF6),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Info: Fecha, Ciudad, Área
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(cotizacion.fecha, color = textMuted, fontSize = 12.sp)
                }

                if (!cotizacion.ciudad.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            cotizacion.ciudad,
                            color = textMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Área y totales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${String.format("%.2f", cotizacion.areaTotal)} m²",
                    color = textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                // Mostrar el total más alto
                val maxTotal = maxOf(
                    cotizacion.totalHs875 ?: 0.0,
                    cotizacion.totalHs1250 ?: 0.0,
                    cotizacion.totalHs1500 ?: 0.0
                )
                if (maxTotal > 0) {
                    Text(
                        formatMoney(maxTotal),
                        color = Color(0xFF10B981),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = border.copy(0.5f))
            Spacer(Modifier.height(12.dp))

            // Botón descargar PDF
            Button(
                onClick = onDescargarPdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = if (isDarkMode) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ver PDF",
                    color = if (isDarkMode) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}