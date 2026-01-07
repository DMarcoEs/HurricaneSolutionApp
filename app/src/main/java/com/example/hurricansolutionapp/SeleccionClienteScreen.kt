package com.example.hurricansolutionapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionClienteScreen(
    context: android.content.Context,
    userId: String,
    userRole: String,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onClienteNuevo: () -> Unit,
    onClienteActualSeleccionado: (Lead) -> Unit
) {
    BackHandler { onBack() }

    val scope = rememberCoroutineScope()

    // Estado
    var allLeads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var filteredLeads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    // Colores Stitch
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF000000) else Color.White
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val inputBg = if (isDarkMode) Color(0xFF111111) else Color.White

    // Cargar leads al iniciar
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val leads = LeadsRepository.getAllLeads()
            allLeads = leads
            filteredLeads = if (userRole == "ADMIN") {
                leads
            } else {
                leads.filter { it.assignedToUserId == userId }
            }
        } catch (e: Exception) {
            showError = "Error cargando leads: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Filtrar por búsqueda
    LaunchedEffect(searchQuery, allLeads) {
        filteredLeads = if (searchQuery.isBlank()) {
            if (userRole == "ADMIN") allLeads else allLeads.filter { it.assignedToUserId == userId }
        } else {
            val query = searchQuery.lowercase()
            val filtered = allLeads.filter { lead ->
                lead.nombreCompleto.lowercase().contains(query) ||
                        lead.telefono.contains(query) ||
                        lead.email?.lowercase()?.contains(query) == true
            }
            if (userRole == "ADMIN") filtered else filtered.filter { it.assignedToUserId == userId }
        }
    }

    Scaffold(
        topBar = {
            StitchTopBarWithDivider(
                title = "Leads Nuevos",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        },
        containerColor = bg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Snackbar de error/info
            showError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (error.contains("Error")) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = if (error.contains("Error")) Color(0xFFD32F2F) else Color(0xFF388E3C),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            fontSize = 14.sp,
                            color = if (error.contains("Error")) Color(0xFFD32F2F) else Color(0xFF388E3C),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showError = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = if (error.contains("Error")) Color(0xFFD32F2F) else Color(0xFF388E3C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Botón "CLIENTE NUEVO"
            Button(
                onClick = onClienteNuevo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = if (isDarkMode) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CLIENTE NUEVO",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.Black else Color.White,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Título "LEADS DEL CRM" centrado
            Text(
                text = "LEADS DEL CRM",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de búsqueda + Botón Actualizar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Campo de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Buscar...",
                            color = textMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = textMuted
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpiar",
                                    tint = textMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedBorderColor = border,
                        unfocusedBorderColor = border,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )

                // Botón Actualizar
                Button(
                    onClick = {
                        scope.launch {
                            isSyncing = true
                            try {
                                val result = LeadsRepository.syncLeadsFromCRM(context)
                                if (result.isSuccess) {
                                    val leads = LeadsRepository.getAllLeads()
                                    allLeads = leads
                                    showError = "Leads actualizados"
                                } else {
                                    showError = "Error: ${result.exceptionOrNull()?.message}"
                                }
                            } catch (e: Exception) {
                                showError = "Error: ${e.message}"
                            } finally {
                                isSyncing = false
                            }
                        }
                    },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkMode) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = if (isDarkMode) Color.Black else Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Actualizar",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.Black else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contador de leads
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredLeads.size} lead${if (filteredLeads.size != 1) "s" else ""} encontrado${if (filteredLeads.size != 1) "s" else ""}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMuted
                )

                if (userRole != "ADMIN") {
                    Text(
                        text = "(Solo tus leads asignados)",
                        fontSize = 12.sp,
                        color = textMuted,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista de leads
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CircularProgressIndicator(
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                }
            } else if (filteredLeads.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isBlank()) {
                                "No hay leads asignados"
                            } else {
                                "No se encontraron resultados"
                            },
                            fontSize = 16.sp,
                            color = textMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLeads) { lead ->
                        StitchLeadCard(
                            lead = lead,
                            isDarkMode = isDarkMode,
                            onClick = { onClienteActualSeleccionado(lead) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

/**
 * Tarjeta de lead estilo Stitch
 * Borde izquierdo negro/gris, SIN borde verde completo
 */
@Composable
private fun StitchLeadCard(
    lead: Lead,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val leftBorder = if (isDarkMode) Color(0xFFE5E7EB) else Color.Black

    // Tarjeta con borde izquierdo (estilo Stitch)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = cardBg,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Borde izquierdo negro/gris
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(leftBorder)
            )

            // Contenido de la tarjeta
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar con iniciales
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color.White else Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lead.getInitials(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.Black else Color.White,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Información del lead
                Column(modifier = Modifier.weight(1f)) {
                    // Nombre
                    Text(
                        text = lead.nombreCompleto,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Teléfono
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_phone_lucide),
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = lead.telefono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textMuted
                        )
                    }

                    // Pipeline stage badge
                    if (!lead.pipelineStage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(2.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDarkMode) Color(0xFF3F3F46) else Color(0xFFE5E7EB)
                            )
                        ) {
                            Text(
                                text = lead.pipelineStage.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}