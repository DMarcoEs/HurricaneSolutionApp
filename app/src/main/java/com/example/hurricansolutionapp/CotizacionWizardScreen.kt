package com.example.hurricansolutionapp

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ===============================================================================
// WIZARD DE COTIZACIÓN - 4 PASOS CON ANIMACIÓN SLIDE
// ===============================================================================
// Paso 1: Selección de Cliente (Leads del CRM o Cliente Nuevo)
// Paso 2: Datos del Cliente (ClienteScreen)
// Paso 3: Captura de Medidas (CotizacionesFormScreen)
// Paso 4: Resumen (ResumenScreen)
// ===============================================================================

private const val TOTAL_STEPS = 3 // Visualmente mostramos 3 pasos (el step 1 es selección)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CotizacionWizardScreen(
    context: Context,
    userId: String,
    userRole: String,
    draft: CotizacionDraft,
    isDarkMode: Boolean,
    onExit: () -> Unit,
    onFinalizar: (Cotizacion) -> Unit
) {
    val scope = rememberCoroutineScope()

    // Estado del wizard
    var currentStep by rememberSaveable { mutableIntStateOf(0) } // 0=Selección, 1=Cliente, 2=Medidas, 3=Resumen
    var isGoingForward by remember { mutableStateOf(true) }
    var showExitDialog by remember { mutableStateOf(false) }
    var cotizacionFinal by remember { mutableStateOf<Cotizacion?>(null) }
    var clienteTipo by rememberSaveable { mutableStateOf<String?>(null) } // "nuevo" o "crm"
    var leadSeleccionado by remember { mutableStateOf<Lead?>(null) }

    // Colores
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF6F7F8)
    val surface = if (isDarkMode) Color(0xFF0A0A0A) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    // Verificar si hay cambios para mostrar diálogo de salida
    val hayCambios = draft.nombre.isNotBlank() ||
            draft.telefono.isNotBlank() ||
            draft.ventanasForm.any { it.descripcion.isNotBlank() }

    // BackHandler
    BackHandler(enabled = true) {
        when {
            currentStep > 0 -> {
                isGoingForward = false
                currentStep--
            }
            hayCambios -> showExitDialog = true
            else -> onExit()
        }
    }

    // Función para navegar al siguiente paso
    fun goToNextStep() {
        isGoingForward = true
        currentStep++
    }

    // Función para navegar al paso anterior
    fun goToPreviousStep() {
        isGoingForward = false
        currentStep--
    }

    Scaffold(
        containerColor = bg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Animación de transición entre pasos
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (isGoingForward) {
                        // Avanzar: entra de derecha a izquierda
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(350, easing = EaseInOutCubic)
                        ) + fadeIn(
                            animationSpec = tween(350)
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(350, easing = EaseInOutCubic)
                        ) + fadeOut(
                            animationSpec = tween(350)
                        )
                    } else {
                        // Retroceder: entra de izquierda a derecha
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(350, easing = EaseInOutCubic)
                        ) + fadeIn(
                            animationSpec = tween(350)
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(350, easing = EaseInOutCubic)
                        ) + fadeOut(
                            animationSpec = tween(350)
                        )
                    }
                },
                label = "wizard_transition"
            ) { step ->
                when (step) {
                    // ===================================================================
                    // PASO 0: Selección de Cliente (Leads del CRM o Cliente Nuevo)
                    // ===================================================================
                    0 -> {
                        WizardSeleccionClienteContent(
                            context = context,
                            userId = userId,
                            userRole = userRole,
                            isDarkMode = isDarkMode,
                            onBack = {
                                if (hayCambios) showExitDialog = true else onExit()
                            },
                            onClienteNuevo = {
                                clienteTipo = "nuevo"
                                draft.clear()
                                draft.esClienteActual = false
                                draft.leadId = null
                                goToNextStep()
                            },
                            onClienteActualSeleccionado = { lead ->
                                clienteTipo = "crm"
                                leadSeleccionado = lead
                                draft.clear()
                                draft.nombre = lead.nombreCompleto
                                draft.telefono = lead.telefono
                                draft.ciudad = lead.ciudad ?: ""
                                draft.colonia = lead.colonia ?: ""
                                draft.direccionDetalle = "${lead.calle ?: ""} ${lead.numero ?: ""}".trim()
                                draft.esClienteActual = true
                                draft.leadId = lead.id
                                goToNextStep()
                            }
                        )
                    }

                    // ===================================================================
                    // PASO 1: Datos del Cliente
                    // ===================================================================
                    1 -> {
                        WizardClienteContent(
                            draft = draft,
                            isDarkMode = isDarkMode,
                            currentStep = 1,
                            totalSteps = TOTAL_STEPS,
                            onBack = { goToPreviousStep() },
                            onContinuar = { goToNextStep() }
                        )
                    }

                    // ===================================================================
                    // PASO 2: Captura de Medidas
                    // ===================================================================
                    2 -> {
                        WizardMedidasContent(
                            draft = draft,
                            isDarkMode = isDarkMode,
                            currentStep = 2,
                            totalSteps = TOTAL_STEPS,
                            onBack = { goToPreviousStep() },
                            onContinuarResumen = { cotizacion ->
                                cotizacionFinal = cotizacion
                                goToNextStep()
                            }
                        )
                    }

                    // ===================================================================
                    // PASO 3: Resumen
                    // ===================================================================
                    3 -> {
                        val cot = cotizacionFinal
                        if (cot != null) {
                            WizardResumenContent(
                                cotizacion = cot,
                                isDarkMode = isDarkMode,
                                currentStep = 3,
                                totalSteps = TOTAL_STEPS,
                                onBack = { goToPreviousStep() },
                                onVolverAInicio = {
                                    draft.clear()
                                    onExit()
                                },
                                onFinalizar = { finalCotizacion ->
                                    onFinalizar(finalCotizacion)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog de salida
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = if (isDarkMode) Color(0xFF0A0A0A) else Color.White,
            titleContentColor = if (isDarkMode) Color.White else Color(0xFF111418),
            textContentColor = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF4B5563),
            title = { Text("Salir de la cotización") },
            text = { Text("¿Qué quieres hacer con el borrador actual?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        draft.clear()
                        showExitDialog = false
                        onExit()
                    }
                ) { Text("Borrar y salir", color = Color(0xFFE7180B)) }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showExitDialog = false
                            onExit()
                        }
                    ) {
                        Text(
                            "Salir sin borrar",
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Cancelar", color = if (isDarkMode) Color.White else Color.Black)
                    }
                }
            }
        )
    }
}

// ===============================================================================
// CONTENIDO DEL PASO 0: SELECCIÓN DE CLIENTE
// ===============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WizardSeleccionClienteContent(
    context: Context,
    userId: String,
    userRole: String,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onClienteNuevo: () -> Unit,
    onClienteActualSeleccionado: (Lead) -> Unit
) {
    // Reutilizamos la lógica de SeleccionClienteScreen pero sin Scaffold propio
    val scope = rememberCoroutineScope()

    var allLeads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var filteredLeads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }
    var isSyncing by remember { mutableStateOf(false) }

    // Colores
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // TopBar
        WizardTopBar(
            title = "Leads Nuevos",
            onBack = onBack,
            isDarkMode = isDarkMode
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Error/Info message
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
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
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

            // Título "LEADS DEL CRM"
            Text(
                text = "LEADS DEL CRM",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de búsqueda + Botón Actualizar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar...", color = textMuted) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedBorderColor = if (isDarkMode) Color.White else Color.Black,
                        unfocusedBorderColor = border,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    ),
                    singleLine = true
                )

                Button(
                    onClick = {
                        scope.launch {
                            isSyncing = true
                            try {
                                val result = LeadsRepository.syncLeadsFromCRM(context)
                                if (result.isSuccess) {
                                    val leads = LeadsRepository.getAllLeads()
                                    allLeads = leads
                                    filteredLeads = if (userRole == "ADMIN") leads
                                    else leads.filter { it.assignedToUserId == userId }
                                    showError = "Sincronización completada"
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
                    modifier = Modifier.height(52.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = if (isDarkMode) Color.Black else Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Actualizar",
                            color = if (isDarkMode) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contador
            Text(
                text = "${filteredLeads.size} lead${if (filteredLeads.size != 1) "s" else ""} encontrado${if (filteredLeads.size != 1) "s" else ""}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textMuted
            )

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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = textMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No hay leads asignados"
                            else "No se encontraron resultados",
                            fontSize = 16.sp,
                            color = textMuted
                        )
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLeads.size) { index ->
                        val lead = filteredLeads[index]
                        WizardLeadCard(
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

// ===============================================================================
// CONTENIDO DEL PASO 1: DATOS DEL CLIENTE
// ===============================================================================

@Composable
private fun WizardClienteContent(
    draft: CotizacionDraft,
    isDarkMode: Boolean,
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onContinuar: () -> Unit
) {
    // Reutilizamos ClienteScreen pasando los callbacks correctos
    ClienteScreen(
        draft = draft,
        isDarkMode = isDarkMode,
        currentStep = currentStep,
        totalSteps = totalSteps,
        onBack = onBack,
        onContinuar = onContinuar
    )
}

// ===============================================================================
// CONTENIDO DEL PASO 2: CAPTURA DE MEDIDAS
// ===============================================================================

@Composable
private fun WizardMedidasContent(
    draft: CotizacionDraft,
    isDarkMode: Boolean,
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onContinuarResumen: (Cotizacion) -> Unit
) {
    CotizacionesFormScreen(
        draft = draft,
        onDraftChange = { /* No-op, el draft se modifica directamente */ },
        onContinuarResumen = onContinuarResumen,
        onBack = onBack,
        isDarkMode = isDarkMode,
        currentStep = currentStep,
        totalSteps = totalSteps
    )
}

// ===============================================================================
// CONTENIDO DEL PASO 3: RESUMEN
// ===============================================================================

@Composable
private fun WizardResumenContent(
    cotizacion: Cotizacion,
    isDarkMode: Boolean,
    currentStep: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    onVolverAInicio: () -> Unit,
    onFinalizar: (Cotizacion) -> Unit
) {
    ResumenScreen(
        cotizacion = cotizacion,
        desdeHistorial = false,
        isDarkMode = isDarkMode,
        onVolverAInicio = onVolverAInicio,
        onVolverAEditar = onBack,
        onVolverAHistorial = onVolverAInicio
    )
}

// ===============================================================================
// COMPONENTES AUXILIARES
// ===============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WizardTopBar(
    title: String,
    onBack: () -> Unit,
    isDarkMode: Boolean
) {
    val surface = if (isDarkMode) Color(0xFF000000) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)

    Column(modifier = Modifier.background(surface)) {
        CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = surface),
            title = {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chevron_left),
                        contentDescription = null,
                        tint = textPrimary
                    )
                }
            }
        )
        HorizontalDivider(color = border, thickness = 1.dp)
    }
}

@Composable
private fun WizardLeadCard(
    lead: Lead,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val leftBorder = if (isDarkMode) Color(0xFFE5E7EB) else Color.Black

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
            // Borde izquierdo
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(leftBorder)
            )

            // Contenido
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lead.nombreCompleto,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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

                    if (!lead.pipelineStage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(
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

// Easing personalizado
private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)