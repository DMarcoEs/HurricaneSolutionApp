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
import androidx.compose.ui.res.painterResource
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
    val surface = if (isDarkMode) Color(0xFF000000) else Color.White
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
            // StitchTopBar
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
                            "GESTIONAR EMPLEADOS",
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
                    }
                )
                HorizontalDivider(color = border, thickness = 1.dp)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PersonOff,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No hay empleados registrados", color = textMuted)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.People,
                        value = empleados.size.toString(),
                        label = "Total",
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f),
                        isDarkMode = isDarkMode,
                        card = card,
                        border = border,
                        textPrimary = textPrimary,
                        textMuted = textMuted
                    )
                    StatCard(
                        icon = Icons.Default.CheckCircle,
                        value = empleados.count { it.isActive }.toString(),
                        label = "Activos",
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f),
                        isDarkMode = isDarkMode,
                        card = card,
                        border = border,
                        textPrimary = textPrimary,
                        textMuted = textMuted
                    )
                    StatCard(
                        icon = Icons.Default.Block,
                        value = empleados.count { !it.isActive }.toString(),
                        label = "Inactivos",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f),
                        isDarkMode = isDarkMode,
                        card = card,
                        border = border,
                        textPrimary = textPrimary,
                        textMuted = textMuted
                    )
                }

                // Título
                Text(
                    "Lista de Usuarios",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                // Lista
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(empleados) { empleado ->
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
    }

    // Dialog de confirmación
    if (showConfirmDialog && empleadoSeleccionado != null) {
        val emp = empleadoSeleccionado!!
        val action = if (emp.isActive) "desactivar" else "activar"

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                empleadoSeleccionado = null
            },
            containerColor = if (isDarkMode) Color(0xFF18181B) else Color.White,
            icon = {
                Icon(
                    if (emp.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (emp.isActive) Color(0xFFEF4444) else Color(0xFF10B981),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "${action.replaceFirstChar { it.uppercase() }} usuario",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas $action a ${emp.name}?",
                    color = textMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = { toggleUserActive(emp) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (emp.isActive) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                ) {
                    Text(action.replaceFirstChar { it.uppercase() })
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    empleadoSeleccionado = null
                }) {
                    Text("Cancelar", color = textMuted)
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
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
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = textMuted, fontSize = 11.sp)
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
    val avatarColor = when {
        empleado.role == "ADMIN" -> Color(0xFF3B82F6)
        empleado.isActive -> Color(0xFF10B981)
        else -> Color(0xFFEF4444)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (empleado.isActive) 1f else 0.6f),
        color = card,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border)
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    empleado.name.split(" ").take(2).map { it.firstOrNull()?.uppercase() ?: "" }.joinToString(""),
                    color = avatarColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    empleado.name,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Estado
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (empleado.isActive) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (empleado.isActive) "Activo" else "Inactivo",
                            color = textMuted,
                            fontSize = 12.sp
                        )
                    }

                    // Cotizaciones
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
            }

            // Badge ADMIN o botón toggle
            if (empleado.role == "ADMIN") {
                Surface(
                    color = Color(0xFF3B82F6),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "ADMIN",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                IconButton(onClick = onToggleActive) {
                    Icon(
                        if (empleado.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                        contentDescription = if (empleado.isActive) "Desactivar" else "Activar",
                        tint = if (empleado.isActive) Color(0xFFFCA5A5) else Color(0xFF10B981)
                    )
                }
            }
        }
    }
}