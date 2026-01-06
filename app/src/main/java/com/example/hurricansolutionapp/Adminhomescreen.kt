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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

    // Cargar estadísticas
    LaunchedEffect(Unit) {
        isLoadingStats = true
        stats = AdminRepository.getDashboardStats()
        isLoadingStats = false
    }

    // Colores
    val bg = if (isDarkMode) Zinc950 else Color.White
    val card = if (isDarkMode) Zinc900 else Color(0xFFF9FAFB)
    val border = if (isDarkMode) Zinc800 else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Zinc400 else Color(0xFF6B7280)
    val adminAccent = Color(0xFF3B82F6)

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

        // TOP BAR
        AdminTopBar(isDarkMode, onToggleDarkMode, card, border, textPrimary, adminAccent)

        Spacer(Modifier.height(24.dp))

        // BIENVENIDA
        Text(
            text = getSpanishDate(),
            color = textMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        // Título con nombre en MÁXIMO 2 LÍNEAS
        AdminWelcomeText(
            userName = adminName,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = border, thickness = 1.dp)
        Spacer(Modifier.height(18.dp))

        // ESTADÍSTICAS RÁPIDAS - Fila 1
        Text(
            text = "Resumen General",
            color = textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminStatCardClickable(
                title = "Cotizaciones",
                value = if (isLoadingStats) "..." else stats.totalCotizaciones.toString(),
                icon = Icons.Default.Description,
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                card = card,
                border = border,
                textPrimary = textPrimary,
                textMuted = textMuted,
                onClick = onVerTodasCotizaciones
            )
            AdminStatCardClickable(
                title = "Empleados",
                value = if (isLoadingStats) "..." else stats.empleadosActivos.toString(),
                icon = Icons.Default.People,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                card = card,
                border = border,
                textPrimary = textPrimary,
                textMuted = textMuted,
                onClick = onVerEmpleados
            )
            AdminStatCardClickable(
                title = "m² Total",
                value = if (isLoadingStats) "..." else String.format("%.0f", stats.totalMetrosCuadrados),
                icon = Icons.Default.SquareFoot,
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                card = card,
                border = border,
                textPrimary = textPrimary,
                textMuted = textMuted,
                onClick = onVerMetros
            )
        }

        Spacer(Modifier.height(12.dp))

        // HERRAMIENTAS ADMIN - Fila 2 (Solo Precios y Leads)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminToolCard(
                title = "Precios",
                icon = Icons.Default.AttachMoney,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                card = card,
                border = border,
                textPrimary = textPrimary,
                onClick = onConfigurePrecios
            )
            AdminToolCard(
                title = "Leads",
                icon = Icons.Default.PersonAdd,
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                card = card,
                border = border,
                textPrimary = textPrimary,
                onClick = onGestionarLeads
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = border, thickness = 1.dp)
        Spacer(Modifier.height(18.dp))

        // ACCIONES RÁPIDAS
        Text(
            text = "Acciones Rápidas",
            color = textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // Nueva Cotización
        AdminBigActionCard(onNuevaCotizacion)

        Spacer(Modifier.height(16.dp))

        // Historial de Cotizaciones
        AdminSmallActionCard(
            title = "Historial De Cotizaciones Generadas",
            subtitle = "Ver mi historial de cotizaciones",
            iconRes = R.drawable.ic_history_lucide,
            animationType = AdminAnimationType.ROTATION,
            onClick = onVerMisCotizaciones,
            isDarkMode = isDarkMode,
            card = card,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(16.dp))

        // Sincronizaciones Pendientes
        AdminSmallActionCard(
            title = "Sincronizaciones Pendientes",
            subtitle = "Subir Cotizaciones A La Nube",
            badgeCount = pendingCount,
            iconRes = R.drawable.ic_upload_lucide,
            animationType = AdminAnimationType.BOUNCE,
            onClick = onPendientes,
            isDarkMode = isDarkMode,
            card = card,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(16.dp))

        // Archivar PDF
        AdminSmallActionCard(
            title = "Archivar PDF",
            subtitle = "PDFs Sin Subir A La Base De Datos",
            badgeCount = pendingDriveCount,
            iconRes = R.drawable.ic_google_drive,
            animationType = AdminAnimationType.BOUNCE,
            onClick = onPendientesDrive,
            isDarkMode = isDarkMode,
            card = card,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(16.dp))

        // CERRAR SESIÓN
        AdminCerrarSesionButton(
            onClick = { if (logoutEnabled) showLogoutDialog = true },
            isDarkMode = isDarkMode,
            enabled = logoutEnabled
        )

        Spacer(Modifier.height(32.dp))
    }

    // Dialog de logout
    if (showLogoutDialog) {
        val dialogBg = if (isDarkMode) Zinc900 else Color.White
        val dialogTitle = if (isDarkMode) Color.White else Color(0xFF111418)
        val dialogText = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF374151)
        val cancelColor = if (isDarkMode) Color.White else Color(0xFF111418)
        val dangerColor = Color(0xFFE53935)

        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = dialogBg,
            tonalElevation = 6.dp,
            title = {
                Text(
                    text = "Cerrar sesión",
                    color = dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    text = "¿Deseas cerrar sesión de administrador?\nTendrás que volver a iniciar sesión.",
                    color = dialogText,
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
                    Text(text = "Sí, salir", color = dangerColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = "Cancelar", color = cancelColor, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

/**
 * Texto de bienvenida con nombre en MÁXIMO 2 LÍNEAS.
 */
@Composable
private fun AdminWelcomeText(
    userName: String,
    textPrimary: Color,
    textMuted: Color
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = textPrimary, fontWeight = FontWeight.Black)) {
                append("Bienvenido, ")
            }
            withStyle(SpanStyle(color = textMuted, fontWeight = FontWeight.Black)) {
                append(userName)
            }
        },
        fontSize = 32.sp,
        lineHeight = 38.sp,
        maxLines = 2
    )
}

// Tipos de animación
private enum class AdminAnimationType {
    NONE, ROTATION, BOUNCE
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTES PRIVADOS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AdminTopBar(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    card: Color,
    border: Color,
    textPrimary: Color,
    adminAccent: Color
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

            Box(
                modifier = Modifier
                    .background(adminAccent, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "ADMIN",
                    color = Color.White,
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
                .background(card)
                .border(1.5.dp, border, CircleShape)
                .clickable { onToggleDarkMode() },
            contentAlignment = Alignment.Center
        ) {
            val themeIcon = if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon
            Icon(
                painter = painterResource(id = themeIcon),
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
private fun AdminBigActionCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val scope = rememberCoroutineScope()
    var isRotated by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (isRotated) 90f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .shadow(20.dp, shape, spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(shape)
            .border(width = 1.dp, color = Color.Black.copy(alpha = 0.08f), shape = shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF000000), Color(0xFF0B0B0D), Color(0xFF1A1A1D)),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 0f)
                )
            )
            .clickable {
                isRotated = true
                scope.launch {
                    kotlinx.coroutines.delay(350)
                    onClick()
                    isRotated = false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .align(Alignment.CenterStart)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.15f))
                    .graphicsLayer(rotationZ = rotation),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 26.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Nueva Cotización",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun AdminSmallActionCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    isDarkMode: Boolean,
    card: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color,
    badgeCount: Int? = null,
    animationType: AdminAnimationType = AdminAnimationType.NONE,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "icon_animations")

    val rotationOscilation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oscilacion"
    )

    val bounceAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "escala"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(card)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable {
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
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDarkMode) Zinc800 else Color(0xFFE5E7EB))
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    when (animationType) {
                        AdminAnimationType.ROTATION -> {
                            this.rotationZ = rotationOscilation
                            this.transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                        }
                        AdminAnimationType.BOUNCE -> {
                            this.translationY = bounceAnim
                        }
                        AdminAnimationType.NONE -> { }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = textMuted, fontSize = 13.sp)
        }
        if (badgeCount != null && badgeCount > 0) {
            Text("• $badgeCount", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun AdminCerrarSesionButton(onClick: () -> Unit, isDarkMode: Boolean, enabled: Boolean) {
    val scope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "logout_anim")

    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (enabled) 5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logout_move"
    )

    val redBg = if (isDarkMode) {
        if (enabled) Color(0xFF451A1A) else Zinc900.copy(alpha = 0.5f)
    } else {
        if (enabled) Color(0xFFFEF2F2) else Color(0xFFF3F4F6)
    }

    val redBorder = if (isDarkMode) {
        if (enabled) Color(0xFF7F1D1D) else Zinc800
    } else {
        if (enabled) Color(0xFFFEE2E2) else Color(0xFFE5E7EB)
    }

    val contentColor = if (enabled) Color(0xFFEF4444) else Zinc400

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(redBg)
            .border(1.dp, redBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { scope.launch { onClick() } }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (enabled) (if (isDarkMode) Color(0xFF7F1D1D).copy(alpha = 0.4f) else Color(0xFFFEE2E2))
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_logout_lucide),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { this.translationX = offsetX }
            )
        }

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                text = "Cerrar Sesión",
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = if (enabled) "Salir de la cuenta" else "Acción no disponible",
                color = contentColor.copy(alpha = if (isDarkMode) 0.6f else 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun AdminStatCardClickable(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    card: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        color = card,
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
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(title, color = textMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AdminToolCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    card: Color,
    border: Color,
    textPrimary: Color,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        color = card,
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
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                color = textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

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
        contentDescription = "Logo Hurricane Solution",
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit
    )
}

private fun trimTransparentAdmin(src: Bitmap): Bitmap {
    val w = src.width
    val h = src.height
    val pixels = IntArray(w * h)
    src.getPixels(pixels, 0, w, 0, 0, w, h)

    var left = w
    var top = h
    var right = 0
    var bottom = 0
    var found = false

    for (y in 0 until h) {
        val row = y * w
        for (x in 0 until w) {
            val alpha = (pixels[row + x] ushr 24) and 0xFF
            if (alpha > 10) {
                found = true
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }

    if (!found) return src

    val newW = (right - left + 1).coerceAtLeast(1)
    val newH = (bottom - top + 1).coerceAtLeast(1)
    return Bitmap.createBitmap(src, left, top, newW, newH)
}