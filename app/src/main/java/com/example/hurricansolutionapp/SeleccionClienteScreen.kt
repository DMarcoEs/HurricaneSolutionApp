package com.example.hurricansolutionapp

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionClienteScreen(
    context: android.content.Context,  // ✅ NUEVO
    userId: String,
    userRole: String,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onClienteNuevo: () -> Unit,
    onClienteActualSeleccionado: (Lead) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Estado
    var allLeads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var filteredLeads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    // Colores según tema
    val backgroundColor = if (isDarkMode) Zinc950 else Color.White
    val cardColor = if (isDarkMode) Zinc900 else Color(0xFFF5F5F5)
    val textColor = if (isDarkMode) Color.White else Zinc950
    val textSecondary = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF666666)
    val accentColor = Color(0xFFE63946)

    // Cargar leads al iniciar
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val leads = LeadsRepository.getAllLeads()
            allLeads = leads

            // Filtrar según rol
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
            TopAppBar(
                title = { Text("Seleccionar Cliente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = textColor)
                    }
                },
                actions = {
                    // Botón de sincronizar
                    IconButton(
                        onClick = {
                            scope.launch {
                                isSyncing = true
                                try {
                                    val result = LeadsRepository.syncLeadsFromCRM(context)
                                    if (result.isSuccess) {
                                        // Recargar leads
                                        val leads = LeadsRepository.getAllLeads()
                                        allLeads = leads
                                        showError = result.getOrNull()
                                    } else {
                                        showError = "Error sincronizando leads"
                                    }
                                } catch (e: Exception) {
                                    showError = "Error: ${e.message}"
                                } finally {
                                    isSyncing = false
                                }
                            }
                        },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, "Sincronizar", tint = textColor)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = textColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            // Snackbar de error
            showError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
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
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            fontSize = 14.sp,
                            color = Color(0xFFD32F2F)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { showError = null }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón "Cliente Nuevo"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClienteNuevo() },
                colors = CardDefaults.cardColors(
                    containerColor = accentColor
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "CLIENTE NUEVO",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Título "Cliente Actual"
            Text(
                text = "Cliente Actual",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre, teléfono o email") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = if (isDarkMode) Color(0xFF444444) else Color(0xFFCCCCCC)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Contador de leads
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredLeads.size} lead${if (filteredLeads.size != 1) "s" else ""} encontrado${if (filteredLeads.size != 1) "s" else ""}",
                    fontSize = 14.sp,
                    color = textSecondary
                )

                if (userRole != "ADMIN") {
                    Text(
                        text = "(Solo tus leads asignados)",
                        fontSize = 12.sp,
                        color = textSecondary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
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
                    CircularProgressIndicator(color = accentColor)
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
                            tint = textSecondary,
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
                            color = textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLeads) { lead ->
                        LeadCard(
                            lead = lead,
                            currentUserId = userId,
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

@Composable
private fun LeadCard(
    lead: Lead,
    currentUserId: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val cardColor = if (isDarkMode) Zinc900 else Color.White
    val textColor = if (isDarkMode) Color.White else Zinc950
    val textSecondary = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF666666)
    val accentColor = Color(0xFFE63946)

    val isAssignedToMe = lead.assignedToUserId == currentUserId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
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
                    .background(if (isAssignedToMe) accentColor else Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lead.getInitials(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Información del lead
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = lead.nombreCompleto,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = lead.telefono,
                        fontSize = 14.sp,
                        color = textSecondary
                    )
                }

                if (!lead.ciudad.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = lead.ciudad,
                            fontSize = 13.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Pipeline stage
                if (!lead.pipelineStage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = accentColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = lead.pipelineStage,
                            fontSize = 11.sp,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Indicador visual si está asignado
            if (isAssignedToMe) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Asignado a mí",
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Seleccionar",
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}