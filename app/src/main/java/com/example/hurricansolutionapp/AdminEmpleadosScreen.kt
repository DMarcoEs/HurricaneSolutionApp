package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
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

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ADMIN EMPLEADOS SCREEN
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Pantalla de gestión de especialistas.
 *
 * CAMBIO: Agregado parámetro isRainMode para ocultar M² Cotizados en Rain Protection
 * - isRainMode = false (default): Muestra M² Cotizados, Monto USD, Cotizaciones
 * - isRainMode = true: Solo muestra Monto USD y Cotizaciones (sin M²)
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEmpleadosScreen(
    isRainMode: Boolean = false,  // 👈 NUEVO: Si es true, oculta M² Cotizados
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onVerCotizacionesEmpleado: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var empleados by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var cotizaciones by remember { mutableStateOf<List<CotizacionRemota>>(emptyList()) }
    var cotizacionesPorEmpleado by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var empleadoSeleccionado by remember { mutableStateOf<UserProfile?>(null) }

    // Estado para expandir tarjeta de empleado
    var expandedUserId by remember { mutableStateOf<String?>(null) }

    // Cargar datos
    LaunchedEffect(Unit) {
        isLoading = true
        empleados = AdminRepository.getAllUsers()
        cotizaciones = AdminRepository.getAllCotizaciones()
        val ultimasPorFolio = AdminRepository.getLatestPerFolio(cotizaciones)
        cotizacionesPorEmpleado = ultimasPorFolio.groupBy { it.userId }.mapValues { it.value.size }
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
            StitchTopBar(
                title = "Gestionar Especialistas",
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

                // STATS CARDS - 3 columnas
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

                // TITULO DE SECCION
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
                        "LISTA DE USUARIOS",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                // LISTA DE EMPLEADOS
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(empleados) { empleado ->
                        val userCotizaciones = AdminRepository.getLatestPerFolio(
                            cotizaciones.filter { it.userId == empleado.id }
                        )
                        val totalM2 = userCotizaciones.sumOf { it.areaTotal }
                        val montoTotal = userCotizaciones.sumOf { calcularMontoEstimado(it) }
                        val numCotizaciones = userCotizaciones.size

                        StitchEmpleadoCardExpandible(
                            empleado = empleado,
                            cotizaciones = numCotizaciones,
                            totalM2 = totalM2,
                            montoTotal = montoTotal,
                            isExpanded = expandedUserId == empleado.id,
                            onExpandToggle = {
                                expandedUserId = if (expandedUserId == empleado.id) null else empleado.id
                            },
                            isDarkMode = isDarkMode,
                            isRainMode = isRainMode,  // 👈 Pasar el parámetro
                            onToggleActive = {
                                empleadoSeleccionado = empleado
                                showConfirmDialog = true
                            },
                            onVerCotizaciones = {
                                onVerCotizacionesEmpleado(empleado.id)
                            }
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showConfirmDialog && empleadoSeleccionado != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                empleadoSeleccionado = null
            },
            containerColor = if (isDarkMode) Color(0xFF18181B) else Color.White,
            title = {
                Text(
                    if (empleadoSeleccionado!!.isActive) "Desactivar usuario" else "Activar usuario",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (empleadoSeleccionado!!.isActive)
                        "¿Deseas desactivar a ${empleadoSeleccionado!!.name}?\nNo podrá iniciar sesión."
                    else
                        "¿Deseas activar a ${empleadoSeleccionado!!.name}?\nPodrá volver a iniciar sesión.",
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { toggleUserActive(empleadoSeleccionado!!) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (empleadoSeleccionado!!.isActive) redColor else greenColor
                    )
                ) {
                    Text(
                        if (empleadoSeleccionado!!.isActive) "Desactivar" else "Activar",
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


private fun calcularMontoEstimado(cot: CotizacionRemota): Double {
    val precioPorSistema = mapOf(
        "HS875" to 250.0,
        "HS1250" to 300.0,
        "HS1500" to 350.0
    )

    // Usar el primer sistema de la lista o un promedio
    val precioUsado = cot.productos.firstOrNull()?.let {
        precioPorSistema[it.uppercase()]
    } ?: 250.0

    return cot.areaTotal * precioUsado
}
// COMPONENTES

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


@Composable
private fun StitchEmpleadoCardExpandible(
    empleado: UserProfile,
    cotizaciones: Int,
    totalM2: Double,
    montoTotal: Double,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    isDarkMode: Boolean,
    isRainMode: Boolean = false,  // 👈 NUEVO: Oculta M² si es true
    onToggleActive: () -> Unit,
    onVerCotizaciones: () -> Unit
) {
    val surface = StitchColors.surface(isDarkMode)
    val border = StitchColors.border(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val primary = StitchColors.primary(isDarkMode)
    val onPrimary = StitchColors.onPrimary(isDarkMode)

    val greenColor = StitchColors.greenStandard
    val redColor = StitchColors.redStandard

    val isAdmin = empleado.role == "ADMIN"
    val avatarBg = if (isAdmin) {
        primary
    } else {
        if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)
    }
    val avatarTextColor = if (isAdmin) onPrimary else textPrimary

    Surface(
        onClick = onExpandToggle,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (empleado.isActive) 1f else 0.6f),
        color = surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        // Estado
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

                        Text("*", color = textSecondary.copy(alpha = 0.5f), fontSize = 10.sp)

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

            if (isExpanded) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = border.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // ═══════════════════════════════════════════════════════════════════
                    // M² COTIZADOS - Solo mostrar si NO es Rain Mode
                    // ═══════════════════════════════════════════════════════════════════
                    if (!isRainMode) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${String.format("%.2f", totalM2)} m²",
                                color = Color(0xFF10B981), // Verde
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "M² Cotizados",
                                color = textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Monto USD - Siempre se muestra
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$${String.format("%,.2f", montoTotal)}",
                            color = Color(0xFF3B82F6), // Azul
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Monto USD",
                            color = textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Total Cotizaciones - Siempre se muestra
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$cotizaciones",
                            color = Color(0xFF8B5CF6), // Morado
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Cotizaciones",
                            color = textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Botón Ver Cotizaciones
                Button(
                    onClick = onVerCotizaciones,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Ver Cotizaciones",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Indicador de expandido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Contraer",
                        tint = textSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}