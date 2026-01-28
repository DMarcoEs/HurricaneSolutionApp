package com.example.hurricansolutionapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import kotlinx.coroutines.launch

@Composable
fun AdminHomeScreen(
    adminName: String,
    pendingCount: Int,
    pendingDriveCount: Int = 0,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    // Funciones de ESPECIALISTA
    onNuevaCotizacion: () -> Unit,
    onVerMisCotizaciones: () -> Unit,
    onPendientes: () -> Unit,
    onPendientesDrive: () -> Unit,
    // Funciones de ADMIN
    onConfigurePrecios: () -> Unit,
    onVerTodasCotizaciones: () -> Unit,
    onVerEmpleados: () -> Unit,
    onGestionarLeads: () -> Unit,
    onVerMetros: () -> Unit,
    // Logout
    logoutEnabled: Boolean,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(AdminRepository.DashboardStats()) }
    var isLoadingStats by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoadingStats = true
        stats = AdminRepository.getDashboardStats()
        isLoadingStats = false
    }

    // Colores Stitch
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Spacer(Modifier.height(12.dp))

        AdminTopBarStitch(
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode,
            surface = surface,
            border = border,
            textPrimary = textPrimary
        )

        Spacer(Modifier.height(24.dp))

        // Fecha
        Text(
            text = getSpanishDateAdmin(),
            color = textMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        AdminWelcomeText(
            userName = adminName,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(24.dp))

        StitchSectionTitle(
            title = "HERRAMIENTAS DE ADMINISTRACIÓN",
            isDarkMode = isDarkMode,
            textPrimary = textPrimary
        )

        Spacer(Modifier.height(16.dp))

        // Grid de 3x2 con tarjetas estilo Stitch (borde izquierdo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cotizaciones
            StitchAdminCard(
                title = "COTIZACIONES",
                value = if (isLoadingStats) "..." else stats.totalCotizaciones.toString(),
                icon = Icons.Default.Description,
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                onClick = onVerTodasCotizaciones
            )
            // Empleados
            StitchAdminCard(
                title = "EMPLEADOS",
                value = if (isLoadingStats) "..." else stats.empleadosActivos.toString(),
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                onClick = onVerEmpleados
            )
            // mÂ² Total
            StitchAdminCard(
                title = "M² TOTAL",
                value = if (isLoadingStats) "..." else String.format("%.0f", stats.totalMetrosCuadrados),
                icon = Icons.Default.SquareFoot,
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                onClick = onVerMetros
            )
        }

        Spacer(Modifier.height(12.dp))

        // Fila 2: Precios, Leads
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Precios
            StitchAdminCard(
                title = "PRECIOS",
                value = null,
                icon = Icons.Default.AttachMoney,
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                onClick = onConfigurePrecios
            )
            // Leads
            StitchAdminCard(
                title = "LEADS",
                value = null,
                icon = Icons.Default.PersonAdd,
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                onClick = onGestionarLeads
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        // MENU - Estilo Stitch con barra lateral
        StitchSectionTitle(
            title = "MENU",
            isDarkMode = isDarkMode,
            textPrimary = textPrimary
        )

        Spacer(Modifier.height(16.dp))

        StitchBigActionCard(
            isDarkMode = isDarkMode,
            onClick = onNuevaCotizacion
        )

        Spacer(Modifier.height(12.dp))

        // Historial de Cotizaciones
        StitchMenuCard(
            title = "Historial De Cotizaciones Generadas",
            subtitle = "Ver mi historial de cotizaciones",
            iconRes = R.drawable.ic_history_lucide,
            animationType = StitchAnimationType.ROTATION,
            onClick = onVerMisCotizaciones,
            isDarkMode = isDarkMode,
            surface = surface,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(12.dp))

        // Sincronizaciones Pendientes
        StitchMenuCard(
            title = "Sincronizaciones Pendientes",
            subtitle = "Subir Cotizaciones A La Nube",
            badgeCount = pendingCount,
            iconRes = R.drawable.ic_upload_lucide,
            animationType = StitchAnimationType.BOUNCE,
            onClick = onPendientes,
            isDarkMode = isDarkMode,
            surface = surface,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(16.dp))

        StitchLogoutButton(
            onClick = { if (logoutEnabled) showLogoutDialog = true },
            isDarkMode = isDarkMode,
            enabled = logoutEnabled
        )

        Spacer(Modifier.height(32.dp))
    }

    // Dialog de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = if (isDarkMode) Color(0xFF18181B) else Color.White,
            tonalElevation = 6.dp,
            title = {
                Text(
                    text = "Cerrar sesión",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "Deseas cerrar sesión de administrador?\nTendrás que volver a iniciar sesión.",
                    color = textMuted,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onCerrarSesion()
                    }
                ) {
                    Text("Si, salir", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = textPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

// COMPONENTES STITCH

private enum class StitchAnimationType {
    NONE, ROTATION, BOUNCE
}


@Composable
private fun AdminWelcomeText(
    userName: String,
    textPrimary: Color,
    textMuted: Color
) {
    // Formatear nombre: Primer Nombre + Primer Apellido
    val nombreCorto = formatearNombreCorto(userName)

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = textPrimary, fontWeight = FontWeight.Black)) {
                append("Bienvenido, ")
            }
            withStyle(SpanStyle(color = textMuted, fontWeight = FontWeight.Black)) {
                append(nombreCorto)
            }
        },
        fontSize = 24.sp,  // Reducido de 32sp a 24sp
        lineHeight = 30.sp, // Reducido de 38sp a 30sp
        maxLines = 2
    )
}


@Composable
private fun StitchSectionTitle(
    title: String,
    isDarkMode: Boolean,
    textPrimary: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Barra lateral
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDarkMode) Color.White else Color.Black)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

/**
 * TopBar con badge ADMIN NEGRO en light / BLANCO en dark (NO azul)
 */
@Composable
private fun AdminTopBarStitch(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    surface: Color,
    border: Color,
    textPrimary: Color
) {
    val rotation by animateFloatAsState(
        targetValue = if (isDarkMode) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val logoRes = if (isDarkMode) R.drawable.hurricane_solution_blanco else R.drawable.logo_header_new
            AdminCroppedLogo(resId = logoRes, height = 48.dp)

            // Badge ADMIN - NEGRO en light, BLANCO en dark (SIN azul)
            Box(
                modifier = Modifier
                    .background(
                        color = if (isDarkMode) Color.White else Color.Black,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "ADMIN",
                    color = if (isDarkMode) Color.Black else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(elevation = if (isDarkMode) 0.dp else 6.dp, CircleShape)
                .clip(CircleShape)
                .background(surface)
                .border(1.5.dp, border, CircleShape)
                .clickable { onToggleDarkMode() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon
                ),
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer(rotationZ = rotation)
            )
        }
    }
}


@Composable
private fun StitchAdminCard(
    title: String,
    value: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val leftBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.15f)
    val borderColor = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val iconColor = if (isDarkMode) Color(0xFF71717A) else Color(0xFF71717A)
    val valueColor = if (isDarkMode) Color.White else Color(0xFF111418)
    val titleColor = if (isDarkMode) Color(0xFF71717A) else Color(0xFF71717A)

    Surface(
        modifier = modifier
            .height(if (value != null) 120.dp else 100.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        onClick = {
            isPressed = true
            scope.launch {
                kotlinx.coroutines.delay(100)
                isPressed = false
                onClick()
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(cardBg)
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
        ) {
            // Borde izquierdo grueso (4dp) - Estilo Stitch
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(leftBorderColor)
            )

            // Contenido centrado
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icono gris
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )

                if (value != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        value,
                        color = valueColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    title,
                    color = titleColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}


@Composable
private fun StitchBigActionCard(
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val bgColor = if (isDarkMode) Color(0xFF111111) else Color.Black
    val contentColor = Color.White
    val iconBgColor = Color.White.copy(alpha = 0.2f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp,
        onClick = {
            isPressed = true
            scope.launch {
                kotlinx.coroutines.delay(100)
                isPressed = false
                onClick()
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    "NUEVA COTIZACIÓN",
                    color = contentColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
private fun StitchMenuCard(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    animationType: StitchAnimationType = StitchAnimationType.NONE,
    badgeCount: Int? = null,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    surface: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "menu_anim")
    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    val bounceAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        color = surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border),
        onClick = {
            isPressed = true
            scope.launch {
                kotlinx.coroutines.delay(100)
                isPressed = false
                onClick()
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6))
                    .graphicsLayer {
                        when (animationType) {
                            StitchAnimationType.ROTATION -> rotationZ = rotationAnim
                            StitchAnimationType.BOUNCE -> translationY = bounceAnim
                            StitchAnimationType.NONE -> {}
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = textMuted, fontSize = 12.sp)
            }
            if (badgeCount != null && badgeCount > 0) {
                Text("* $badgeCount", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}


@Composable
private fun StitchLogoutButton(
    onClick: () -> Unit,
    isDarkMode: Boolean,
    enabled: Boolean
) {
    val scope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "logout_anim")

    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (enabled) 5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val contentColor = if (isDarkMode) {
        if (enabled) Color(0xFFFCA5A5) else Color(0xFF6B7280)
    } else {
        if (enabled) Color(0xFFDC2626) else Color(0xFF9CA3AF)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkMode) Color(0xFF18181B) else Color(0xFFFEF2F2))
            .border(
                1.dp,
                if (isDarkMode) Color(0xFF27272A) else Color(0xFFFECACA),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled) {
                isPressed = true
                scope.launch {
                    kotlinx.coroutines.delay(100)
                    isPressed = false
                    onClick()
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFFEE2E2))
                .graphicsLayer { translationX = if (enabled) offsetX else 0f },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_logout_lucide),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                "Cerrar Sesión",
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                if (enabled) "Salir de la cuenta" else "Acción no disponible",
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Logo recortado
 */
@Composable
private fun AdminCroppedLogo(@DrawableRes resId: Int, height: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val croppedBitmap = remember(resId) {
        val bmp = BitmapFactory.decodeResource(context.resources, resId)
            .copy(Bitmap.Config.ARGB_8888, false)
        trimTransparentAdmin(bmp)
    }

    Image(
        bitmap = croppedBitmap.asImageBitmap(),
        contentDescription = "Logo",
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit
    )
}

private fun trimTransparentAdmin(src: Bitmap): Bitmap {
    val w = src.width
    val h = src.height
    val pixels = IntArray(w * h)
    src.getPixels(pixels, 0, w, 0, 0, w, h)

    var left = w; var top = h; var right = 0; var bottom = 0
    var found = false

    for (y in 0 until h) {
        for (x in 0 until w) {
            if (((pixels[y * w + x] ushr 24) and 0xFF) > 10) {
                found = true
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }

    if (!found) return src
    return Bitmap.createBitmap(src, left, top, (right - left + 1).coerceAtLeast(1), (bottom - top + 1).coerceAtLeast(1))
}


private fun getSpanishDateAdmin(): String {
    val dias = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
    val meses = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    val cal = java.util.Calendar.getInstance()
    return "${dias[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]}, ${cal.get(java.util.Calendar.DAY_OF_MONTH)} de ${meses[cal.get(java.util.Calendar.MONTH)]}"
}