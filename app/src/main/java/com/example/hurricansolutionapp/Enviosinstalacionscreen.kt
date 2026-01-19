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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla "Envíos a Instalación" para Especialistas y Admin
 * - Muestra TODAS las cotizaciones (no solo las de 1 sistema)
 * - Si hay múltiples sistemas, permite seleccionar cuál enviar
 * - Si ya existe en instalador_datos, actualiza en lugar de duplicar
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
    var fechaSolicitada by remember { mutableStateOf("") }
    var sistemaSeleccionado by remember { mutableStateOf<String?>(null) }
    var yaExisteEnInstalacion by remember { mutableStateOf(false) }

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val inputBg = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val primary = if (isDarkMode) Color.White else Color.Black
    val onPrimary = if (isDarkMode) Color.Black else Color.White

    val userId = remember { SessionManager.getUserId(context) }
    val userRole = remember { SessionManager.getRole(context) }
    val isAdmin = userRole.equals("ADMIN", ignoreCase = true)

    LaunchedEffect(refreshKey) {
        scope.launch {
            isLoading = true
            try {
                val result = EnviosInstalacionRepository.getCotizacionesPendientesEnvioTodas(
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

    val filteredList = remember(cotizaciones, searchQuery) {
        if (searchQuery.isBlank()) cotizaciones
        else cotizaciones.filter { cot ->
            cot.clienteNombre.contains(searchQuery, ignoreCase = true) ||
                    cot.folio.contains(searchQuery, ignoreCase = true)
        }
    }

    fun enviarAInstalacion(cotizacion: Cotizacion, sistema: String, fecha: String, esActualizacion: Boolean) {
        scope.launch {
            sendingFolio = cotizacion.folio
            try {
                val userName = SessionManager.getNombre(context)

                val result = if (esActualizacion) {
                    EnviosInstalacionRepository.actualizarRegistroInstalacion(
                        cotizacion = cotizacion,
                        sistemaSeleccionado = sistema,
                        especialistaId = userId,
                        especialistaNombre = userName,
                        fechaSolicitada = fecha.ifBlank { null }
                    )
                } else {
                    InstaladorRepository.crearRegistroDesdeCotizacionCompleto(
                        cotizacion = cotizacion,
                        sistemaSeleccionado = sistema,
                        especialistaId = userId,
                        especialistaNombre = userName,
                        fechaSolicitada = fecha.ifBlank { null }
                    )
                }

                if (result.isSuccess) {
                    EnviosInstalacionRepository.marcarComoEnviada(context, cotizacion.folio)
                    val mensaje = if (esActualizacion) "Instalación actualizada" else "Enviado a instalación"
                    Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
                    refreshKey++
                } else {
                    Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                sendingFolio = null
            }
        }
    }

    fun prepararEnvio(cotizacion: Cotizacion) {
        scope.launch {
            val existeResult = InstaladorRepository.getDatosByFolio(cotizacion.folio)
            yaExisteEnInstalacion = existeResult.isSuccess && existeResult.getOrNull() != null

            if (cotizacion.productos.size > 1) {
                sistemaSeleccionado = null
            } else {
                sistemaSeleccionado = cotizacion.productos.firstOrNull()?.name ?: "HS875"
            }
            showConfirmDialog = cotizacion
        }
    }

    if (showConfirmDialog != null) {
        val cot = showConfirmDialog!!
        val tieneMultiplesSistemas = cot.productos.size > 1

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = null
                fechaSolicitada = ""
                sistemaSeleccionado = null
            },
            containerColor = cardBg,
            title = {
                Text(
                    if (yaExisteEnInstalacion) "Actualizar Instalación" else "Enviar a Instalación",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (yaExisteEnInstalacion) {
                        Surface(
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                Text(
                                    "Esta cotización ya fue enviada. Se actualizará.",
                                    color = Color(0xFFF59E0B),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Text(
                        if (yaExisteEnInstalacion) "¿Actualizar los datos?" else "¿Enviar al instalador?",
                        color = textPrimary
                    )

                    Surface(color = inputBg, shape = RoundedCornerShape(8.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(cot.folio, color = textPrimary, fontWeight = FontWeight.Bold)
                            Text(cot.clienteNombre, color = textMuted, fontSize = 13.sp)
                        }
                    }

                    if (tieneMultiplesSistemas) {
                        Spacer(Modifier.height(4.dp))
                        Text("SELECCIONA EL SISTEMA", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            cot.productos.forEach { producto ->
                                val isSelected = sistemaSeleccionado == producto.name
                                Surface(
                                    onClick = { sistemaSeleccionado = producto.name },
                                    color = if (isSelected) primary.copy(alpha = 0.1f) else inputBg,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelected) primary else border)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(producto.etiquetaCorta, color = textPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, null, tint = primary, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val sistema = cot.productos.firstOrNull()?.etiquetaCorta ?: "-"
                        Text("Sistema: $sistema", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Spacer(Modifier.height(4.dp))
                    Text("FECHA SOLICITADA", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = fechaSolicitada,
                        onValueChange = { fechaSolicitada = it },
                        placeholder = { Text("Ej. 15/01/2026", color = textMuted.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            focusedBorderColor = primary,
                            unfocusedBorderColor = border,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.CalendarToday, null, tint = textMuted, modifier = Modifier.size(20.dp)) }
                    )
                }
            },
            confirmButton = {
                val canSend = !tieneMultiplesSistemas || sistemaSeleccionado != null
                Button(
                    onClick = {
                        val sistema = sistemaSeleccionado ?: cot.productos.firstOrNull()?.name ?: "HS875"
                        val fecha = fechaSolicitada
                        val esActualizacion = yaExisteEnInstalacion
                        showConfirmDialog = null
                        fechaSolicitada = ""
                        sistemaSeleccionado = null
                        enviarAInstalacion(cot, sistema, fecha, esActualizacion)
                    },
                    enabled = canSend,
                    colors = ButtonDefaults.buttonColors(containerColor = primary)
                ) {
                    Icon(if (yaExisteEnInstalacion) Icons.Default.Update else Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (yaExisteEnInstalacion) "Actualizar" else "Enviar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = null
                    fechaSolicitada = ""
                    sistemaSeleccionado = null
                }) { Text("Cancelar", color = textMuted) }
            }
        )
    }

    Scaffold(
        containerColor = bg,
        topBar = { StitchTopBar(title = "Envíos a Instalación", onBack = onBack, isDarkMode = isDarkMode) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Buscar...", color = textMuted, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = textMuted, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, "Limpiar", tint = textMuted) }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBg, unfocusedContainerColor = inputBg,
                        focusedBorderColor = border, unfocusedBorderColor = border,
                        focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Surface(
                    onClick = { refreshKey++ },
                    color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, border)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                        Text("Actualizar", color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = primary.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp)) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = textPrimary, modifier = Modifier.size(20.dp))
                    Text("Cotizaciones listas para enviar al instalador", color = textPrimary, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = textPrimary, strokeWidth = 2.dp) }
                    filteredList.isEmpty() -> Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Outlined.Inventory2, null, tint = textMuted.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(if (searchQuery.isNotBlank()) "No se encontraron resultados" else "No hay envíos pendientes", color = textMuted, fontSize = 16.sp, textAlign = TextAlign.Center)
                    }
                    else -> LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { Text("${filteredList.size} cotización${if (filteredList.size != 1) "es" else ""}", color = textMuted, fontSize = 12.sp) }
                        items(filteredList, key = { it.folio }) { cotizacion ->
                            EnvioInstalacionCard(cotizacion, isDarkMode, cardBg, textPrimary, textMuted, border, sendingFolio == cotizacion.folio) { prepararEnvio(cotizacion) }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnvioInstalacionCard(cotizacion: Cotizacion, isDarkMode: Boolean, cardBg: Color, textPrimary: Color, textMuted: Color, border: Color, isSending: Boolean, onEnviar: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val fecha = try { cotizacion.fecha?.let { dateFormat.format(Date(it)) } ?: "-" } catch (e: Exception) { "-" }
    val numSistemas = cotizacion.productos.size
    val sistemasText = if (numSistemas == 1) cotizacion.productos.firstOrNull()?.etiquetaCorta ?: "-" else "$numSistemas sistemas"
    val areaTotal = cotizacion.ventanas.sumOf { it.alto * it.ancho }
    val numVentanas = cotizacion.ventanas.size
    val primary = if (isDarkMode) Color.White else Color.Black
    val onPrimary = if (isDarkMode) Color.Black else Color.White

    Surface(modifier = Modifier.fillMaxWidth(), color = cardBg, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, border.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(cotizacion.folio, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Surface(color = primary, shape = RoundedCornerShape(6.dp)) {
                    Text(sistemasText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(cotizacion.clienteNombre, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = textMuted, modifier = Modifier.size(14.dp))
                    Text(fecha, color = textMuted, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Window, null, tint = textMuted, modifier = Modifier.size(14.dp))
                    Text("$numVentanas apertura${if (numVentanas != 1) "s" else ""}", color = textMuted, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.SquareFoot, null, tint = textMuted, modifier = Modifier.size(14.dp))
                    Text("${String.format("%.1f", areaTotal)} m²", color = textMuted, fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = border.copy(alpha = 0.3f))
            Button(onClick = onEnviar, enabled = !isSending, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = primary, disabledContainerColor = textMuted.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Enviando...", color = onPrimary, fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.Send, null, tint = onPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar a Instalación", color = onPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}