package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
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

        // Filtro por asignación
        result = when (selectedFilter) {
            LeadFilter.TODOS -> result
            LeadFilter.SIN_ASIGNAR -> result.filter { it.assignedToUserId == null }
            LeadFilter.ASIGNADOS -> result.filter { it.assignedToUserId != null }
        }

        // Filtro por búsqueda
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

    // Función de sincronización
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

    // Función de asignación
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
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val card = if (isDarkMode) Color(0xFF18181B) else Color.White
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val accentColor = Color(0xFFE63946)

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "GESTIONAR LEADS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
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
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, "Sincronizar")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surface,
                    titleContentColor = textPrimary,
                    navigationIconContentColor = textPrimary,
                    actionIconContentColor = textPrimary
                )
            )
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
                    unfocusedTextColor = textPrimary
                )
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

            Divider(color = border)

            // Lista de leads
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                filteredLeads.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = textMuted.copy(alpha = 0.5f)
                            )
                            Text(
                                "No hay leads",
                                color = textMuted,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredLeads) { lead ->
                            LeadCard(
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
                                },
                                onUnassign = {
                                    scope.launch {
                                        LeadsRepository.unassignLead(lead.id)
                                        leads = LeadsRepository.getAllLeads()
                                        Toast.makeText(context, "Lead desasignado", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog de asignación
    if (showAssignDialog && selectedLead != null) {
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            confirmButton = {
                TextButton(
                    onClick = { assignLead() },
                    enabled = selectedEspecialista != null
                ) {
                    Text("Asignar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = { Text("Asignar Lead") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lead: ${selectedLead!!.nombreCompleto}")
                    Text("Selecciona un especialista:", fontWeight = FontWeight.Bold)

                    especialistas.forEach { especialista ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedEspecialista?.id == especialista.id)
                                        accentColor.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedEspecialista?.id == especialista.id,
                                onClick = { selectedEspecialista = especialista }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(especialista.name)
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun LeadCard(
    lead: Lead,
    isDarkMode: Boolean,
    card: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color,
    accentColor: Color,
    onAssign: () -> Unit,
    onUnassign: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = card,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Nombre y avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lead.getInitials(),
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lead.nombreCompleto,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = lead.telefono,
                        fontSize = 14.sp,
                        color = textMuted
                    )
                }
            }

            // Email (si existe)
            lead.email?.let { email ->
                Text(
                    text = "📧 $email",
                    fontSize = 12.sp,
                    color = textMuted
                )
            }

            // Estado de asignación
            if (lead.assignedToUserId != null && lead.assignedToName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.1f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Asignado a: ${lead.assignedToName}",
                        fontSize = 12.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "⚠️ Sin asignar",
                        fontSize = 12.sp,
                        color = Color(0xFFA16207),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (lead.assignedToUserId == null) {
                    Button(
                        onClick = onAssign,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Asignar a...")
                    }
                } else {
                    OutlinedButton(
                        onClick = onAssign,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Reasignar")
                    }
                    OutlinedButton(
                        onClick = onUnassign,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Desasignar")
                    }
                }
            }
        }
    }
}