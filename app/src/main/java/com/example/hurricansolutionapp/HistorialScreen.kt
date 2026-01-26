package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Historial de Cotizaciones
 *
 * ACTUALIZADO: Ahora carga cotizaciones desde Supabase para ESPECIALISTAS
 * - ESPECIALISTA: Ve solo sus propias cotizaciones (filtradas por user_id)
 * - ADMIN: Continúa viendo todas (sin cambios)
 *
 * Esto permite que el especialista vea sus cotizaciones desde cualquier dispositivo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    listState: LazyListState,
    isDarkMode: Boolean = false,
    onBack: () -> Unit,
    onVerDetalle: (Cotizacion) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ✅ NUEVO: Estados para carga desde Supabase
    var isLoading by remember { mutableStateOf(true) }
    var cotizaciones by remember { mutableStateOf<List<Cotizacion>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    // Estado para el diálogo de confirmación
    var showDeleteDialog by remember { mutableStateOf(false) }
    var cotizacionAEliminar by remember { mutableStateOf<Cotizacion?>(null) }

    // Obtener datos del usuario
    val userId = remember { SessionManager.getUserId(context) }
    val userRole = remember { SessionManager.getRole(context) }
    val isAdmin = userRole.equals("ADMIN", ignoreCase = true)

    // ✅ NUEVO: Cargar cotizaciones al iniciar
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                if (isAdmin) {
                    // Admin ve todas (sin cambios)
                    val localCotizaciones = obtenerCotizacionesLocal(context)
                    cotizaciones = localCotizaciones
                    android.util.Log.d(
                        "HistorialScreen",
                        "Admin: Cargadas ${cotizaciones.size} cotizaciones locales"
                    )
                } else {
                    // ✅ ESPECIALISTA: Cargar desde Supabase filtrado por user_id
                    val remotas = AdminRepository.getCotizacionesByUser(userId)

                    // Convertir de CotizacionRemota a Cotizacion
                    cotizaciones = remotas.map { it.toCotizacionLocal() }

                    android.util.Log.d(
                        "HistorialScreen",
                        "Especialista: Cargadas ${cotizaciones.size} cotizaciones desde Supabase"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "HistorialScreen",
                    "Error cargando cotizaciones: ${e.message}",
                    e
                )
                Toast.makeText(context, "Error al cargar cotizaciones", Toast.LENGTH_SHORT).show()
                cotizaciones = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    val cotizacionesFiltradas = remember(cotizaciones, searchQuery) {
        if (searchQuery.isBlank()) {
            cotizaciones
        } else {
            cotizaciones.filter {
                it.clienteNombre.contains(searchQuery, ignoreCase = true) ||
                        it.folio.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val cardBg = if (isDarkMode) Color(0xFF18181B) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val searchBg = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF3F4F6)

    fun formatMoney(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)}"
    }

    // Diálogo de confirmación para eliminar
    if (showDeleteDialog && cotizacionAEliminar != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                cotizacionAEliminar = null
            },
            containerColor = if (isDarkMode) Color(0xFF18181B) else Color.White,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Eliminar cotización",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "¿Estás seguro de que deseas eliminar esta cotización?",
                        color = textMuted,
                        fontSize = 14.sp
                    )
                    if (cotizacionAEliminar?.folio?.isNotBlank() == true) {
                        Text(
                            "Folio: #${cotizacionAEliminar?.folio}",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        "Cliente: ${cotizacionAEliminar?.clienteNombre}",
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Esta acción no se puede deshacer.",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        cotizacionAEliminar?.let { cot ->
                            // ⚠️ NOTA: Solo elimina local para Admin
                            // Para especialistas, las cotizaciones están en Supabase
                            if (isAdmin) {
                                borrarCotizacionLocal(context, cot.id)
                                cotizaciones = obtenerCotizacionesLocal(context)
                            } else {
                                // Para especialistas, solo recargar desde Supabase
                                scope.launch {
                                    try {
                                        val remotas = AdminRepository.getCotizacionesByUser(userId)
                                        cotizaciones = remotas.map { it.toCotizacionLocal() }
                                    } catch (e: Exception) {
                                        android.util.Log.e(
                                            "HistorialScreen",
                                            "Error recargando: ${e.message}"
                                        )
                                    }
                                }
                            }
                            Toast.makeText(context, "Cotización eliminada", Toast.LENGTH_SHORT)
                                .show()
                        }
                        showDeleteDialog = false
                        cotizacionAEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = false
                        cotizacionAEliminar = null
                    },
                    border = BorderStroke(1.dp, border),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar", color = textPrimary, fontWeight = FontWeight.Medium)
                }
            }
        )
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Cotizaciones Guardadas",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Barra de búsqueda con botón de actualizar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Buscar por cliente, #cotización...",
                            color = textMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = textMuted)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = textMuted
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = searchBg,
                        unfocusedContainerColor = searchBg,
                        focusedBorderColor = border,
                        unfocusedBorderColor = border,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // ✅ NUEVO: Botón de actualizar (solo para especialistas)
                if (!isAdmin) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                try {
                                    val remotas = AdminRepository.getCotizacionesByUser(userId)
                                    cotizaciones = remotas.map { it.toCotizacionLocal() }
                                    Toast.makeText(context, "Actualizado", Toast.LENGTH_SHORT)
                                        .show()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Error al actualizar",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(searchBg, RoundedCornerShape(12.dp))
                            .border(1.dp, border, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = textPrimary
                        )
                    }
                }
            }

            // ✅ NUEVO: Indicador de carga
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = textPrimary)
                        Text(
                            "Cargando cotizaciones...",
                            color = textMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else if (cotizacionesFiltradas.isEmpty()) {
                // Estado vacío
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "No se encontraron resultados"
                            else "No hay cotizaciones guardadas",
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "Intenta con otro término de búsqueda"
                            else if (isAdmin) "Las cotizaciones aparecerán aquí una vez creadas"
                            else "Tus cotizaciones se sincronizan automáticamente",
                            color = textMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(cotizacionesFiltradas) { index, c ->
                        CotizacionCard(
                            cotizacion = c,
                            numeroOrden = cotizacionesFiltradas.size - index,
                            isDarkMode = isDarkMode,
                            cardBg = cardBg,
                            textPrimary = textPrimary,
                            textMuted = textMuted,
                            border = border,
                            formatMoney = { formatMoney(it) },
                            onClick = { onVerDetalle(c) },
                            onPdf = {
                                val pdfFile = generarPdfCotizacion(context, c)
                                if (pdfFile != null) {
                                    verPdf(context, pdfFile)
                                    Toast.makeText(context, "PDF generado", Toast.LENGTH_SHORT)
                                        .show()
                                } else Toast.makeText(
                                    context,
                                    "Error al generar PDF",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onCompartir = {
                                val pdfFile = generarPdfCotizacion(context, c)
                                if (pdfFile != null) compartirPdf(context, pdfFile)
                                else Toast.makeText(
                                    context,
                                    "Error al generar PDF",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onEliminar = {
                                cotizacionAEliminar = c
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CotizacionCard(
    cotizacion: Cotizacion,
    numeroOrden: Int,
    isDarkMode: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textMuted: Color,
    border: Color,
    formatMoney: (Double) -> String,
    onClick: () -> Unit,
    onPdf: () -> Unit,
    onCompartir: () -> Unit,
    onEliminar: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = cardBg,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, border.copy(0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (cotizacion.folio.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isDarkMode) Color(0xFF3F3F46) else Color(0xFFE5E7EB),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "#${cotizacion.folio}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = textMuted
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Text(
                        text = cotizacion.clienteNombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(cotizacion.fecha, color = textMuted, fontSize = 12.sp)
                    }

                    // Mostrar badge de editado si aplica
                    if (cotizacion.fueEditada()) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Editado ${cotizacion.getUpdatedAtFormatted()}",
                                color = Color(0xFFF59E0B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "#$numeroOrden",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Straighten,
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Row {
                        Text("No. de Medidas: ", color = textMuted, fontSize = 13.sp)
                        Text(
                            "${cotizacion.ventanas.size}",
                            color = textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Total del área del proyecto: ${
                            String.format(
                                "%.2f",
                                cotizacion.areaTotal
                            )
                        } m²", color = textMuted, fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                cotizacion.productos.forEachIndexed { index, producto ->
                    val total = cotizacion.totalConDescuento(producto)
                    val isLast = index == cotizacion.productos.lastIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${producto.etiquetaCorta}:", color = textMuted, fontSize = 12.sp)
                        Text(
                            formatMoney(total),
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!isLast) HorizontalDivider(
                        color = border.copy(0.3f),
                        modifier = Modifier.padding(vertical = 0.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = border.copy(0.5f))
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPdf,
                    modifier = Modifier
                        .weight(2f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color.White else Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = if (isDarkMode) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "PDF",
                        color = if (isDarkMode) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onCompartir,
                    modifier = Modifier
                        .weight(2f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDarkMode) Color(
                            0xFF27272A
                        ) else Color(0xFFF3F4F6)
                    ),
                    border = BorderStroke(0.dp, Color.Transparent)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Enviar",
                        color = textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onEliminar,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDarkMode) Color(
                            0xFF450A0A
                        ).copy(0.3f) else Color(0xFFFEF2F2)
                    ),
                    border = BorderStroke(0.dp, Color.Transparent)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}