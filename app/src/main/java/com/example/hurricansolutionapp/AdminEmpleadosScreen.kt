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
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.border
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

    // Colores Stitch
    val bg = StitchColors.background(isDarkMode)
    val surface = StitchColors.surface(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val border = StitchColors.border(isDarkMode)
    val primary = StitchColors.primary(isDarkMode)
    val onPrimary = StitchColors.onPrimary(isDarkMode)

    // Colores estandarizados
    val greenColor = StitchColors.greenStandard
    val redColor = StitchColors.redStandard

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

    // Contadores
    val totalEmpleados = empleados.size
    val activos = empleados.count { it.isActive }
    val inactivos = empleados.count { !it.isActive }

    Scaffold(
        containerColor = bg,
        topBar = {
            // StitchTopBar sin separador
            StitchTopBar(
                title = "Gestionar Empleados",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { innerPadding ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primary)
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
                        tint = textSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No hay empleados registrados", color = textSecondary)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // ═══════════════════════════════════════════════════════════════════
                // STATS CARDS - 3 columnas
                // ═══════════════════════════════════════════════════════════════════
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total
                    StitchStatCard(
                        icon = Icons.Default.People,
                        value = totalEmpleados.toString(),
                        label = "TOTAL",
                        iconColor = textPrimary,
                        valueColor = textPrimary,
                        labelColor = textSecondary,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )

                    // Activos
                    StitchStatCard(
                        icon = Icons.Outlined.CheckCircle,
                        value = activos.toString(),
                        label = "ACTIVOS",
                        iconColor = greenColor,
                        valueColor = greenColor,
                        labelColor = greenColor,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )

                    // Inactivos
                    StitchStatCard(
                        icon = Icons.Outlined.Block,
                        value = inactivos.toString(),
                        label = "INACTIVOS",
                        iconColor = redColor,
                        valueColor = redColor,
                        labelColor = redColor,
                        isDarkMode = isDarkMode,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // ═══════════════════════════════════════════════════════════════════
                // TITULO DE SECCION - Estilo Stitch con barra lateral
                // ═══════════════════════════════════════════════════════════════════
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    // Barra vertical negra/blanca
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(primary)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "LISTA DE USUARIOS",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                // ═══════════════════════════════════════════════════════════════════
                // LISTA DE EMPLEADOS
                // ═══════════════════════════════════════════════════════════════════
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(empleados) { empleado ->
                        StitchEmpleadoCard(
                            empleado = empleado,
                            cotizaciones = cotizacionesPorEmpleado[empleado.id] ?: 0,
                            isDarkMode = isDarkMode,
                            onToggleActive = {
                                empleadoSeleccionado = empleado
                                showConfirmDialog = true
                            }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // DIALOG DE CONFIRMACION
    // ═══════════════════════════════════════════════════════════════════
    if (showConfirmDialog && empleadoSeleccionado != null) {
        val emp = empleadoSeleccionado!!
        val action = if (emp.isActive) "desactivar" else "activar"
        val actionColor = if (emp.isActive) redColor else greenColor

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                empleadoSeleccionado = null
            },
            containerColor = surface,
            shape = RoundedCornerShape(16.dp),
            icon = {
                Icon(
                    if (emp.isActive) Icons.Outlined.Block else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = actionColor,
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
                    "Estas seguro de que deseas $action a ${emp.name}?",
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { toggleUserActive(emp) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = actionColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        action.replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    empleadoSeleccionado = null
                }) {
                    Text("Cancelar", color = textSecondary)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTES AUXILIARES
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Tarjeta de estadistica estilo Stitch
 */
@Composable
private fun StitchStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    iconColor: Color,
    valueColor: Color,
    labelColor: Color,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val surface = StitchColors.surface(isDarkMode)
    val border = StitchColors.border(isDarkMode)

    Surface(
        modifier = modifier,
        color = surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                color = valueColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color = labelColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Tarjeta de empleado estilo Stitch
 */
@Composable
private fun StitchEmpleadoCard(
    empleado: UserProfile,
    cotizaciones: Int,
    isDarkMode: Boolean,
    onToggleActive: () -> Unit
) {
    val surface = StitchColors.surface(isDarkMode)
    val border = StitchColors.border(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val primary = StitchColors.primary(isDarkMode)
    val onPrimary = StitchColors.onPrimary(isDarkMode)

    // Colores estandarizados
    val greenColor = StitchColors.greenStandard
    val redColor = StitchColors.redStandard

    // Color del avatar segun rol/estado
    val isAdmin = empleado.role == "ADMIN"
    val avatarBg = if (isAdmin) {
        primary
    } else {
        if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)
    }
    val avatarTextColor = if (isAdmin) onPrimary else textPrimary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (empleado.isActive) 1f else 0.6f),
        color = surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, border)
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(avatarBg)
                    .then(
                        if (!isAdmin) {
                            Modifier.border(1.dp, border, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    empleado.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase()?.toString() }.joinToString(""),
                    color = avatarTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            // Info del empleado
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    empleado.name,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Estado (punto + texto)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (empleado.isActive) greenColor else redColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (empleado.isActive) "ACTIVO" else "INACTIVO",
                            color = if (empleado.isActive) greenColor else redColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Separador
                    Text(
                        "•",
                        color = textSecondary.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )

                    // Cotizaciones
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "$cotizaciones cotizaciones",
                            color = textSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Badge ADMIN o boton toggle
            if (isAdmin) {
                Surface(
                    color = primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "ADMIN",
                        color = onPrimary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                // Icono de bloquear/activar
                IconButton(
                    onClick = onToggleActive,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (empleado.isActive) Icons.Outlined.Block else Icons.Outlined.CheckCircle,
                        contentDescription = if (empleado.isActive) "Desactivar" else "Activar",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}