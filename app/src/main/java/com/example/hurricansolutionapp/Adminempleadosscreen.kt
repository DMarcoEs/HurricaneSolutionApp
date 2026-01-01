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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEmpleadosScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var empleados by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var cotizacionesPorEmpleado by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var empleadoSeleccionado by remember { mutableStateOf<UserProfile?>(null) }

    // Cargar datos
    LaunchedEffect(Unit) {
        isLoading = true
        empleados = AdminRepository.getAllUsers()
        val cotizaciones = AdminRepository.getAllCotizaciones()
        cotizacionesPorEmpleado = cotizaciones.groupBy { it.userId }.mapValues { it.value.size }
        isLoading = false
    }

    // Colores
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val card = if (isDarkMode) Color(0xFF18181B) else Color.White
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    fun toggleUserActive(empleado: UserProfile) {
        scope.launch {
            val result = AdminRepository.setUserActive(empleado.id, !empleado.isActive)
            result.onSuccess {
                empleados = AdminRepository.getAllUsers()
                Toast.makeText(
                    context,
                    if (!empleado.isActive) "Usuario activado" else "Usuario desactivado",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            showConfirmDialog = false
            empleadoSeleccionado = null
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
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
                        "GESTIONAR EMPLEADOS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(40.dp))
                }
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
                CircularProgressIndicator(color = Color(0xFF8B5CF6))
            }
        } else if (empleados.isEmpty()) {
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
                        Icons.Default.PeopleOutline,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "No hay empleados registrados",
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
                // Resumen
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Total",
                            value = empleados.size.toString(),
                            icon = Icons.Default.People,
                            color = Color(0xFF3B82F6),
                            modifier = Modifier.weight(1f),
                            isDarkMode = isDarkMode,
                            card = card,
                            border = border,
                            textPrimary = textPrimary,
                            textMuted = textMuted
                        )
                        SummaryCard(
                            title = "Activos",
                            value = empleados.count { it.isActive }.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            isDarkMode = isDarkMode,
                            card = card,
                            border = border,
                            textPrimary = textPrimary,
                            textMuted = textMuted
                        )
                        SummaryCard(
                            title = "Inactivos",
                            value = empleados.count { !it.isActive }.toString(),
                            icon = Icons.Default.Block,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.weight(1f),
                            isDarkMode = isDarkMode,
                            card = card,
                            border = border,
                            textPrimary = textPrimary,
                            textMuted = textMuted
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Lista de Usuarios",
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(
                    items = empleados.sortedByDescending { it.isActive },
                    key = { it.id }
                ) { empleado ->
                    EmpleadoCard(
                        empleado = empleado,
                        cotizaciones = cotizacionesPorEmpleado[empleado.id] ?: 0,
                        isDarkMode = isDarkMode,
                        card = card,
                        border = border,
                        textPrimary = textPrimary,
                        textMuted = textMuted,
                        onToggleActive = {
                            empleadoSeleccionado = empleado
                            showConfirmDialog = true
                        }
                    )
                }
            }
        }
    }

    // Diálogo de confirmación
    if (showConfirmDialog && empleadoSeleccionado != null) {
        val empleado = empleadoSeleccionado!!
        val accion = if (empleado.isActive) "desactivar" else "activar"

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                empleadoSeleccionado = null
            },
            containerColor = if (isDarkMode) Color(0xFF18181B) else Color.White,
            icon = {
                Icon(
                    if (empleado.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (empleado.isActive) Color(0xFFEF4444) else Color(0xFF10B981),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "${accion.replaceFirstChar { it.uppercase() }} usuario",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "¿Estás seguro de que deseas $accion a ${empleado.name}?",
                        color = textMuted
                    )
                    if (empleado.isActive) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "El usuario no podrá iniciar sesión hasta que lo reactives.",
                            color = Color(0xFFEF4444),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { toggleUserActive(empleado) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (empleado.isActive) Color(0xFFEF4444) else Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        accion.replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showConfirmDialog = false
                        empleadoSeleccionado = null
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancelar", color = textPrimary)
                }
            }
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    card: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color
) {
    Surface(
        modifier = modifier,
        color = card,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                color = textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                color = textMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun EmpleadoCard(
    empleado: UserProfile,
    cotizaciones: Int,
    isDarkMode: Boolean,
    card: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color,
    onToggleActive: () -> Unit
) {
    val isAdmin = empleado.role == "ADMIN"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!empleado.isActive) Modifier.alpha(0.7f) else Modifier
            ),
        color = card,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            when {
                !empleado.isActive -> Color(0xFFEF4444).copy(alpha = 0.3f)
                isAdmin -> Color(0xFF3B82F6).copy(alpha = 0.3f)
                else -> border
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isAdmin -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                            empleado.isActive -> Color(0xFF10B981).copy(alpha = 0.2f)
                            else -> Color(0xFFEF4444).copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    empleado.name.split(" ")
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .joinToString(""),
                    color = when {
                        isAdmin -> Color(0xFF3B82F6)
                        empleado.isActive -> Color(0xFF10B981)
                        else -> Color(0xFFEF4444)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        empleado.name,
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (isAdmin) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF3B82F6), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "ADMIN",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Estado
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (empleado.isActive) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (empleado.isActive) "Activo" else "Inactivo",
                        color = textMuted,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.width(12.dp))

                    // Cotizaciones
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$cotizaciones cotización(es)",
                        color = textMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Botón toggle (no mostrar para admin)
            if (!isAdmin) {
                IconButton(
                    onClick = onToggleActive,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (empleado.isActive)
                                Color(0xFFEF4444).copy(alpha = 0.1f)
                            else
                                Color(0xFF10B981).copy(alpha = 0.1f)
                        )
                ) {
                    Icon(
                        if (empleado.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                        contentDescription = if (empleado.isActive) "Desactivar" else "Activar",
                        tint = if (empleado.isActive) Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}