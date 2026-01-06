package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

enum class LeadFilter {
    TODOS, SIN_ASIGNAR, ASIGNADOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLeadsScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estado
    var isLoading by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var leads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var filteredLeads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var especialistas by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf(LeadFilter.TODOS) }
    var searchQuery by remember { mutableStateOf("") }

    // Dialogs
    var showAssignDialog by remember { mutableStateOf(false) }
    var selectedLead by remember { mutableStateOf<Lead?>(null) }
    var selectedEspecialista by remember { mutableStateOf<UserProfile?>(null) }

    // Cargar datos iniciales
    LaunchedEffect(Unit) {
        isLoading = true
        leads = LeadsRepository.getAllLeads()
        especialistas = AdminRepository.getEspecialistas().filter { it.isActive }
        isLoading = false
    }

    // Aplicar filtros
    LaunchedEffect(leads, selectedFilter, searchQuery) {
        var result = leads

        result = when (selectedFilter) {
            LeadFilter.TODOS -> result
            LeadFilter.SIN_ASIGNAR -> result.filter { it.assignedToUserId == null }
            LeadFilter.ASIGNADOS -> result.filter { it.assignedToUserId != null }
        }

        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            result = result.filter { lead ->
                lead.nombreCompleto.lowercase().contains(query) ||
                        lead.telefono.contains(query) ||
                        lead.email?.lowercase()?.contains(query) == true
            }
        }

        filteredLeads = result
    }

    fun syncLeads() {
        scope.launch {
            isSyncing = true
            val result = LeadsRepository.syncLeadsFromCRM(context)
            if (result.isSuccess) {
                leads = LeadsRepository.getAllLeads()
                Toast.makeText(context, "Leads sincronizados", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
            isSyncing = false
        }
    }

    fun assignLead() {
        val lead = selectedLead ?: return
        val especialista = selectedEspecialista ?: return

        scope.launch {
            val result = LeadsRepository.assignLeadToUser(
                leadId = lead.id,
                userId = especialista.id,
                userName = especialista.name
            )

            if (result.isSuccess) {
                leads = LeadsRepository.getAllLeads()
                Toast.makeText(context, "Lead asignado a ${especialista.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Error asignando: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }

            showAssignDialog = false
            selectedLead = null
            selectedEspecialista = null
        }
    }

    // Colores
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF000000) else Color.White
    val card = if (isDarkMode) Color(0xFF18181B) else Color.White
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val accentColor = Color(0xFFE63946)

    Scaffold(
        containerColor = bg,
        topBar = {
            // StitchTopBar con botón de sync
            Column(
                modifier = Modifier
                    .background(surface)
                    .statusBarsPadding()
            ) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = surface
                    ),
                    title = {
                        Text(
                            "GESTIONAR LEADS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary,
                            letterSpacing = 0.5.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_left),
                                contentDescription = "Volver",
                                tint = textPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { syncLeads() },
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = textPrimary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    "Sincronizar",
                                    tint = textPrimary
                                )
                            }
                        }
                    }
                )
                HorizontalDivider(color = border, thickness = 1.dp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por nombre, teléfono o email...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, "Limpiar")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = surface,
                    unfocusedContainerColor = surface,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary,
                    focusedBorderColor = if (isDarkMode) Color.White else Color.Black,
                    unfocusedBorderColor = border
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Filtros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == LeadFilter.TODOS,
                    onClick = { selectedFilter = LeadFilter.TODOS },
                    label = { Text("Todos (${leads.size})") }
                )
                FilterChip(
                    selected = selectedFilter == LeadFilter.SIN_ASIGNAR,
                    onClick = { selectedFilter = LeadFilter.SIN_ASIGNAR },
                    label = { Text("Sin asignar (${leads.count { it.assignedToUserId == null }})") }
                )
                FilterChip(
                    selected = selectedFilter == LeadFilter.ASIGNADOS,
                    onClick = { selectedFilter = LeadFilter.ASIGNADOS },
                    label = { Text("Asignados (${leads.count { it.assignedToUserId != null }})") }
                )
            }

            // Lista de leads
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (filteredLeads.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No se encontraron leads", color = textMuted)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLeads) { lead ->
                        AdminLeadCard(
                            lead = lead,
                            isDarkMode = isDarkMode,
                            card = card,
                            border = border,
                            textPrimary = textPrimary,
                            textMuted = textMuted,
                            accentColor = accentColor,
                            onAssign = {
                                selectedLead = lead
                                showAssignDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog para asignar
    if (showAssignDialog && selectedLead != null) {
        AlertDialog(
            onDismissRequest = {
                showAssignDialog = false
                selectedLead = null
                selectedEspecialista = null
            },
            containerColor = if (isDarkMode) Color(0xFF18181B) else Color.White,
            title = {
                Text(
                    "Asignar Lead",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Lead: ${selectedLead?.nombreCompleto}",
                        color = textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Selecciona un especialista:", color = textMuted)
                    Spacer(Modifier.height(8.dp))

                    especialistas.forEach { esp ->
                        Surface(
                            onClick = { selectedEspecialista = esp },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (selectedEspecialista == esp)
                                accentColor.copy(alpha = 0.1f)
                            else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        esp.name.take(2).uppercase(),
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(esp.name, color = textPrimary)
                                if (selectedEspecialista == esp) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = accentColor
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { assignLead() },
                    enabled = selectedEspecialista != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    )
                ) {
                    Text("Asignar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAssignDialog = false
                    selectedLead = null
                    selectedEspecialista = null
                }) {
                    Text("Cancelar", color = textMuted)
                }
            }
        )
    }
}

@Composable
private fun AdminLeadCard(
    lead: Lead,
    isDarkMode: Boolean,
    card: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color,
    accentColor: Color,
    onAssign: () -> Unit
) {
    val isAssigned = lead.assignedToUserId != null
    val avatarColor = if (isAssigned) Color(0xFF10B981) else Color(0xFFFCA5A5)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = card,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        lead.getInitials(),
                        color = avatarColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        lead.nombreCompleto,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        lead.telefono,
                        color = textMuted,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Estado de asignación
            if (isAssigned) {
                Surface(
                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Asignado a: ${lead.assignedToName ?: ""}",
                            color = Color(0xFF10B981),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Botón asignar
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Sin asignar",
                                color = Color(0xFFEF4444),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onAssign,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Asignar a...")
                }
            }
        }
    }
}