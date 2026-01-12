package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla "EnvÃ­os a InstalaciÃ³n" para Especialistas y Admin
 * Muestra cotizaciones con 1 solo sistema que aÃºn no se han enviado al instalador
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnviosInstalacionScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var cotizaciones by remember { mutableStateOf<List<Cotizacion>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var sendingFolio by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf<Cotizacion?>(null) }
    var fechaSolicitada by remember { mutableStateOf("") }  // âœ… NUEVO

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val inputBg = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val accentGreen = Color(0xFF10B981)

    val userId = remember { SessionManager.getUserId(context) }
    val userRole = remember { SessionManager.getRole(context) }
    val isAdmin = userRole.equals("ADMIN", ignoreCase = true)

    // Cargar cotizaciones pendientes de envÃ­o
    LaunchedEffect(refreshKey) {
        scope.launch {
            isLoading = true
            try {
                // Obtener cotizaciones con 1 solo sistema que no se han enviado
                val result = EnviosInstalacionRepository.getCotizacionesPendientesEnvio(
                    context = context,
                    userId = if (isAdmin) null else userId
                )
                if (result.isSuccess) {
                    cotizaciones = result.getOrNull() ?: emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("EnviosInstalacion", "Error cargando: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Filtrar por bÃºsqueda
    val filteredList = remember(cotizaciones, searchQuery) {
        if (searchQuery.isBlank()) cotizaciones
        else cotizaciones.filter { cot ->
            cot.clienteNombre.contains(searchQuery, ignoreCase = true) ||
                    cot.folio.contains(searchQuery, ignoreCase = true)
        }
    }

    // FunciÃ³n para enviar a instalaciÃ³n
    fun enviarAInstalacion(cotizacion: Cotizacion, fecha: String) {
        scope.launch {
            sendingFolio = cotizacion.folio
            try {
                val userName = SessionManager.getNombre(context)

                // 1. Crear registro en instalador_datos + medidas
                val sistemaSeleccionado = cotizacion.productos.firstOrNull()?.name ?: "HS875"

                val result = InstaladorRepository.crearRegistroDesdeCotizacionCompleto(
                    cotizacion = cotizacion,
                    sistemaSeleccionado = sistemaSeleccionado,
                    especialistaId = userId,
                    especialistaNombre = userName,
                    fechaSolicitada = fecha.ifBlank { null }  // âœ… NUEVO
                )

                if (result.isSuccess) {
                    // 2. Marcar cotizaciÃ³n como enviada (localmente)
                    EnviosInstalacionRepository.marcarComoEnviada(context, cotizacion.folio)

                    Toast.makeText(context, "âœ… Enviado a instalaciÃ³n", Toast.LENGTH_SHORT).show()
                    refreshKey++
                } else {
                    Toast.makeText(
                        context,
                        "Error: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                sendingFolio = null
            }
        }
    }

    // DiÃ¡logo de confirmaciÃ³n con campo de fecha
    if (showConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = null
                fechaSolicitada = ""  // Limpiar al cerrar
            },
            containerColor = cardBg,
            title = {
                Text("Enviar a InstalaciÃ³n", color = textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Â¿Enviar esta cotizaciÃ³n al instalador?", color = textPrimary)

                    Surface(color = inputBg, shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                showConfirmDialog!!.folio,
                                color = textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                showConfirmDialog!!.clienteNombre,
                                color = textMuted,
                                fontSize = 13.sp
                            )
                            Text(
                                "Sistema: ${showConfirmDialog!!.productos.firstOrNull()?.etiquetaCorta ?: "â€”"}",
                                color = accentGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // âœ… NUEVO: Campo de fecha solicitada
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "FECHA SOLICITADA DE INSTALACIÃ“N",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = fechaSolicitada,
                        onValueChange = { fechaSolicitada = it },
                        placeholder = {
                            Text(
                                "Ej. 15/01/2026",
                                color = textMuted.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = accentGreen,
                            unfocusedBorderColor = border,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cot = showConfirmDialog!!
                        val fecha = fechaSolicitada
                        showConfirmDialog = null
                        fechaSolicitada = ""  // Limpiar
                        enviarAInstalacion(cot, fecha)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
                ) {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = null
                    fechaSolicitada = ""  // Limpiar
                }) {
                    Text("Cancelar", color = textMuted)
                }
            }
        )
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Envíos a Instalación",
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
            // Barra de búsqueda + Botón Actualizar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Buscar por nombre o folio...",
                            color = textMuted,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            null,
                            tint = textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Limpiar", tint = textMuted)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedBorderColor = border,
                        unfocusedBorderColor = border,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Botón Actualizar
                Surface(
                    onClick = { refreshKey++ },
                    color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Actualizar",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Info banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = accentGreen.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        null,
                        tint = accentGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "Cotizaciones con 1 solo sistema listas para enviar al instalador",
                        color = accentGreen,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Lista
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = textPrimary, strokeWidth = 2.dp)
                        }
                    }

                    filteredList.isEmpty() -> {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.Inventory2,
                                null,
                                tint = textMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (searchQuery.isNotBlank()) "No se encontraron resultados"
                                else "No hay envÃ­os pendientes",
                                color = textMuted,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                            if (searchQuery.isBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Las cotizaciones con 1 sistema\naparecerÃ¡n aquÃ­",
                                    color = textMuted.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    "${filteredList.size} cotizaciÃ³n${if (filteredList.size != 1) "es" else ""} pendiente${if (filteredList.size != 1) "s" else ""}",
                                    color = textMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            items(filteredList, key = { it.folio }) { cotizacion ->
                                EnvioInstalacionCard(
                                    cotizacion = cotizacion,
                                    isDarkMode = isDarkMode,
                                    cardBg = cardBg,
                                    textPrimary = textPrimary,
                                    textMuted = textMuted,
                                    border = border,
                                    accentGreen = accentGreen,
                                    isSending = sendingFolio == cotizacion.folio,
                                    onEnviar = { showConfirmDialog = cotizacion }
                                )
                            }

                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnvioInstalacionCard(
    cotizacion: Cotizacion,
    isDarkMode: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textMuted: Color,
    border: Color,
    accentGreen: Color,
    isSending: Boolean,
    onEnviar: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val fecha = try {
        cotizacion.fecha?.let { dateFormat.format(Date(it)) } ?: "â€”"
    } catch (e: Exception) {
        "â€”"
    }

    val sistema = cotizacion.productos.firstOrNull()?.etiquetaCorta ?: "â€”"
    val areaTotal = cotizacion.ventanas.sumOf { it.alto * it.ancho }
    val numVentanas = cotizacion.ventanas.size

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Folio + Sistema
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    cotizacion.folio,
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = accentGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        sistema,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = accentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Cliente
            Text(
                cotizacion.clienteNombre,
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        null,
                        tint = textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(fecha, color = textMuted, fontSize = 12.sp)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Window,
                        null,
                        tint = textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "$numVentanas apertura${if (numVentanas != 1) "s" else ""}",
                        color = textMuted,
                        fontSize = 12.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.SquareFoot,
                        null,
                        tint = textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "${String.format("%.1f", areaTotal)} mÂ²",
                        color = textMuted,
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(color = border.copy(alpha = 0.3f))

            // BotÃ³n enviar
            Button(
                onClick = onEnviar,
                enabled = !isSending,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) Color.White else Color.Black,
                    disabledContainerColor = textMuted.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = if (isDarkMode) Color.Black else Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Enviando...",
                        color = if (isDarkMode) Color.Black else Color.White,
                        fontSize = 13.sp
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        null,
                        tint = if (isDarkMode) Color.Black else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Enviar a InstalaciÃ³n",
                        color = if (isDarkMode) Color.Black else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}