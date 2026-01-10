package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    var isLoading by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var leads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var filteredLeads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var especialistas by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf(LeadFilter.TODOS) }
    var searchQuery by remember { mutableStateOf("") }

    var showAssignDialog by remember { mutableStateOf(false) }
    var selectedLead by remember { mutableStateOf<Lead?>(null) }
    var selectedEspecialista by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        leads = LeadsRepository.getAllLeads()
        especialistas = AdminRepository.getEspecialistas().filter { it.isActive }
        isLoading = false
    }

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

    val bg = StitchColors.background(isDarkMode)
    val surface = StitchColors.surface(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val border = StitchColors.border(isDarkMode)
    val primary = StitchColors.primary(isDarkMode)
    val onPrimary = StitchColors.onPrimary(isDarkMode)

    val totalCount = leads.size
    val sinAsignarCount = leads.count { it.assignedToUserId == null }
    val asignadosCount = leads.count { it.assignedToUserId != null }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Gestionar Leads",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = surface,
                    shadowElevation = 1.dp
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
                                Text("Buscar...", color = textSecondary, fontSize = 14.sp)
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

                Button(
                    onClick = { syncLeads() },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Actualizar", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = onPrimary)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    StitchFilterChip("Todos ($totalCount)", selectedFilter == LeadFilter.TODOS, { selectedFilter = LeadFilter.TODOS }, isDarkMode)
                }
                item {
                    StitchFilterChip("Sin asignar ($sinAsignarCount)", selectedFilter == LeadFilter.SIN_ASIGNAR, { selectedFilter = LeadFilter.SIN_ASIGNAR }, isDarkMode)
                }
                item {
                    StitchFilterChip("Asignados ($asignadosCount)", selectedFilter == LeadFilter.ASIGNADOS, { selectedFilter = LeadFilter.ASIGNADOS }, isDarkMode)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Titulo de seccion con barra lateral
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
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
                    "LEADS DISPONIBLES",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primary)
                }
            } else if (filteredLeads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonSearch, null, tint = textSecondary, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No se encontraron leads", color = textSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(filteredLeads) { lead ->
                        StitchLeadCard(lead, isDarkMode) {
                            selectedLead = lead
                            showAssignDialog = true
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showAssignDialog && selectedLead != null) {
        AlertDialog(
            onDismissRequest = {
                showAssignDialog = false
                selectedLead = null
                selectedEspecialista = null
            },
            containerColor = surface,
            shape = RoundedCornerShape(16.dp),
            title = { Text("Asignar Lead", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column {
                    Surface(color = StitchColors.surfaceVariant(isDarkMode), shape = RoundedCornerShape(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(StitchColors.surfaceVariant(isDarkMode)).border(1.dp, border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(selectedLead!!.getInitials(), color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(selectedLead!!.nombreCompleto, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(selectedLead!!.telefono, color = textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Seleccionar especialista:", color = textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    if (especialistas.isEmpty()) {
                        Text("No hay especialistas activos", color = textSecondary, fontSize = 14.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            especialistas.forEach { esp ->
                                Surface(
                                    onClick = { selectedEspecialista = esp },
                                    color = if (selectedEspecialista?.id == esp.id) primary.copy(alpha = 0.1f) else StitchColors.surfaceVariant(isDarkMode),
                                    shape = RoundedCornerShape(8.dp),
                                    border = if (selectedEspecialista?.id == esp.id) BorderStroke(2.dp, primary) else BorderStroke(1.dp, border)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(primary), contentAlignment = Alignment.Center) {
                                            Text(esp.name.take(2).uppercase(), color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Text(esp.name, color = textPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                        Spacer(Modifier.weight(1f))
                                        if (selectedEspecialista?.id == esp.id) {
                                            Icon(Icons.Default.CheckCircle, null, tint = primary, modifier = Modifier.size(20.dp))
                                        }
                                    }
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
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ASIGNAR", color = onPrimary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssignDialog = false; selectedLead = null; selectedEspecialista = null }) {
                    Text("Cancelar", color = textSecondary)
                }
            }
        )
    }
}

@Composable
private fun StitchFilterChip(label: String, selected: Boolean, onClick: () -> Unit, isDarkMode: Boolean) {
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
private fun StitchLeadCard(lead: Lead, isDarkMode: Boolean, onAssign: () -> Unit) {
    val surface = StitchColors.surface(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val primary = StitchColors.primary(isDarkMode)
    val onPrimary = StitchColors.onPrimary(isDarkMode)
    val greenColor = StitchColors.greenStandard
    val redColor = StitchColors.redStandard
    val isAssigned = lead.assignedToUserId != null

    Surface(modifier = Modifier.fillMaxWidth(), color = surface, shape = RoundedCornerShape(12.dp), shadowElevation = 1.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(StitchColors.surfaceVariant(isDarkMode)).border(1.dp, StitchColors.borderLight(isDarkMode), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(lead.getInitials(), color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(lead.nombreCompleto.uppercase(), color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.3.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(2.dp))
                    Text(lead.telefono, color = textSecondary, fontSize = 12.sp)
                }
            }

            if (isAssigned) {
                Surface(color = StitchColors.statusGreenBg(isDarkMode), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, StitchColors.statusGreenBorder(isDarkMode))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = greenColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Asignado a: ${lead.assignedToName ?: ""}", color = greenColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                    }
                }
            } else {
                Surface(color = StitchColors.statusRedBg(isDarkMode), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, StitchColors.statusRedBorder(isDarkMode))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Warning, null, tint = redColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sin asignar", color = redColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                    }
                }
                Button(
                    onClick = onAssign,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Outlined.PersonAdd, null, tint = onPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ASIGNAR LEAD", color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}