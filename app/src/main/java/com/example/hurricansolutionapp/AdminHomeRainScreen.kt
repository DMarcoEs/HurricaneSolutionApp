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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.offset
import androidx.annotation.DrawableRes
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ADMIN HOME RAIN SCREEN
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Dashboard de administrador para RAIN PROTECTION.
 * Mismo diseño que AdminHomeScreen pero:
 * - Color azul en lugar de negro
 * - Sin card de M² / Áreas
 * - Cards: COTIZACIONES, ESPECIALISTAS, PRECIOS, LEADS
 */

// Color de acento para Rain Protection
private val RainBlue = Color(0xFF2346AF)

@Composable
fun AdminHomeRainScreen(
    adminName: String,
    pendingCount: Int,
    pendingDriveCount: Int = 0,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onNuevaCotizacion: () -> Unit,
    onVerMisCotizaciones: () -> Unit,
    onPendientes: () -> Unit,
    onPendientesDrive: () -> Unit,
    onEnviosInstalacion: () -> Unit,
    onConfigurePrecios: () -> Unit,
    onVerTodasCotizaciones: () -> Unit,
    onVerEmpleados: () -> Unit,
    onGestionarLeads: () -> Unit,
    logoutEnabled: Boolean,
    onCambiarProducto: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var showChangeProductDialog by remember { mutableStateOf(false) }

    // Stats para Rain
    var totalCotizacionesRain by remember { mutableStateOf(0) }
    var empleadosActivos by remember { mutableStateOf(0) }
    var isLoadingStats by remember { mutableStateOf(true) }

    // ESTADO DE GOOGLE DRIVE AUTH
    var isGoogleAuthenticated by remember {
        mutableStateOf(
            com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context) != null
        )
    }
    var googleUserEmail by remember {
        mutableStateOf(
            com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)?.email ?: ""
        )
    }

    // Launcher para Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            try {
                val signInResult = DriveAuthManager.handleSignInResult(context, result.data)
                if (signInResult.isSuccess) {
                    val account = signInResult.getOrNull()
                    isGoogleAuthenticated = true
                    googleUserEmail = account?.email ?: ""
                    android.widget.Toast.makeText(
                        context,
                        "Conectado: ${account?.email}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "Error al iniciar sesión",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("AdminHomeRainScreen", "Error en Google Sign-In: ${e.message}")
            }
        }
    }

    // Verificar estado al volver a la pantalla
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
                isGoogleAuthenticated = account != null
                googleUserEmail = account?.email ?: ""

                scope.launch {
                    isLoadingStats = true
                    try {
                        val cotizacionesRain = RainRepository.getAllCotizaciones()
                        totalCotizacionesRain = cotizacionesRain.size

                        val empleados = AdminRepository.getAllUsers()
                        empleadosActivos = empleados.filter { it.role != "INSTALLER" }.size
                    } catch (e: Exception) {
                        android.util.Log.e("AdminHomeRain", "Error cargando stats: ${e.message}")
                    } finally {
                        isLoadingStats = false
                    }
                }
            }
        })
    }

    // Carga inicial
    LaunchedEffect(Unit) {
        isLoadingStats = true
        try {
            val cotizacionesRain = RainRepository.getAllCotizaciones()
            totalCotizacionesRain = cotizacionesRain.size

            val empleados = AdminRepository.getAllUsers()
            empleadosActivos = empleados.filter { it.role != "INSTALLER" }.size
        } catch (e: Exception) {
            android.util.Log.e("AdminHomeRain", "Error cargando stats inicial: ${e.message}")
        } finally {
            isLoadingStats = false
        }
    }

    // Colores
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

        RainAdminTopBar(
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode,
            isGoogleAuthenticated = isGoogleAuthenticated,
            googleUserEmail = googleUserEmail,
            onGoogleSignIn = {
                val signInIntent = DriveAuthManager.getSignInIntent(context)
                googleSignInLauncher.launch(signInIntent)
            },
            onGoogleSignOut = {
                scope.launch {
                    DriveAuthManager.signOut(context)
                    isGoogleAuthenticated = false
                    googleUserEmail = ""
                    android.widget.Toast.makeText(
                        context,
                        "Sesión de Drive cerrada",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            surface = surface,
            border = border,
            textPrimary = textPrimary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = getRainSpanishDate(),
            color = textMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        RainAdminWelcomeText(userName = adminName, textPrimary = textPrimary, textMuted = textMuted)

        Spacer(Modifier.height(24.dp))

        RainSectionTitle(
            title = "HERRAMIENTAS DE ADMINISTRACIÓN",
            isDarkMode = isDarkMode,
            textPrimary = textPrimary
        )

        Spacer(Modifier.height(16.dp))

        // Primera fila de cards - COTIZACIONES, ESPECIALISTAS, LEADS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RainAdminCard(
                title = "COTIZACIONES",
                value = if (isLoadingStats) "..." else totalCotizacionesRain.toString(),
                icon = Icons.Default.Description,
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                onClick = onVerTodasCotizaciones
            )
            RainAdminCard(
                title = "ESPECIALISTAS",
                value = if (isLoadingStats) "..." else empleadosActivos.toString(),
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                onClick = onVerEmpleados
            )
            RainAdminCard(
                title = "LEADS",
                value = null,
                icon = Icons.Default.PersonAdd,
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                onClick = onGestionarLeads
            )
        }

        Spacer(Modifier.height(12.dp))

        // Segunda fila de cards - Solo PRECIOS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RainAdminCard(
                title = "PRECIOS",
                value = null,
                icon = Icons.Default.AttachMoney,
                modifier = Modifier.weight(1f),
                isDarkMode = isDarkMode,
                onClick = onConfigurePrecios
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        RainSectionTitle(title = "MENU", isDarkMode = isDarkMode, textPrimary = textPrimary)

        Spacer(Modifier.height(16.dp))

        // Botón AZUL "NUEVO PROYECTO"
        RainBigActionCard(isDarkMode = isDarkMode, onClick = onNuevaCotizacion)

        Spacer(Modifier.height(12.dp))

        RainMenuCard(
            title = "Historial De Proyectos",
            subtitle = "Ver mi historial de cotizaciones Rain",
            iconRes = R.drawable.ic_history_lucide,
            animationType = RainAnimationType.ROTATION,
            onClick = onVerMisCotizaciones,
            isDarkMode = isDarkMode,
            surface = surface,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(12.dp))

        RainMenuCard(
            title = "Proyectos Por Registrar",
            subtitle = "Subir Cotizaciones Rain A La Nube",
            badgeCount = pendingCount,
            iconRes = R.drawable.ic_upload_lucide,
            animationType = RainAnimationType.BOUNCE,
            onClick = onPendientes,
            isDarkMode = isDarkMode,
            surface = surface,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(12.dp))

        RainMenuCard(
            title = "Proyectos A Instalar",
            subtitle = "Generar cotizaciones para instalador",
            iconRes = R.drawable.ic_upload_lucide,
            animationType = RainAnimationType.NONE,
            onClick = onEnviosInstalacion,
            isDarkMode = isDarkMode,
            surface = surface,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(16.dp))

        // Botón para cambiar de producto
        RainChangeProductButton(
            onClick = { if (logoutEnabled) showChangeProductDialog = true },
            isDarkMode = isDarkMode,
            enabled = logoutEnabled
        )

        Spacer(Modifier.height(32.dp))
    }

    // Dialog para cambiar de producto
    if (showChangeProductDialog) {
        AlertDialog(
            onDismissRequest = { showChangeProductDialog = false },
            containerColor = if (isDarkMode) Color(0xFF18181B) else Color.White,
            tonalElevation = 6.dp,
            title = {
                Text(
                    "Cambiar Producto",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    "¿Deseas volver a la selección de productos?\nPodrás elegir Hurricane Protection o continuar en Rain Protection.",
                    color = textMuted,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showChangeProductDialog = false
                    onCambiarProducto()
                }) {
                    Text(
                        "Cambiar",
                        color = RainBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeProductDialog = false }) {
                    Text("Cancelar", color = textMuted)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTES - Mismo estilo que AdminHomeScreen pero AZUL
// ═══════════════════════════════════════════════════════════════════════════════

private enum class RainAnimationType { NONE, ROTATION, BOUNCE }

@Composable
private fun RainAdminCard(
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
    // ═══════════════════════════════════════════════════════════════════════════
    // BORDE LATERAL AZUL para Rain Protection
    // ═══════════════════════════════════════════════════════════════════════════
    val leftBorderColor = RainBlue.copy(alpha = if (isDarkMode) 0.7f else 0.5f)
    val borderColor = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val iconColor = RainBlue.copy(alpha = 0.7f)  // Iconos azules
    val valueColor = if (isDarkMode) Color.White else Color(0xFF111418)
    val titleColor = Color(0xFF71717A)

    Surface(
        modifier = modifier
            .height(120.dp)  // 👈 Altura fija para TODAS las cards
            .graphicsLayer { scaleX = scale; scaleY = scale },
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
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(leftBorderColor)
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val availableWidth = maxWidth
                val titleFontSize = when {
                    availableWidth < 80.dp -> 7.sp
                    availableWidth < 100.dp -> 8.sp
                    else -> 9.sp
                }
                val valueFontSize = when {
                    availableWidth < 80.dp -> 18.sp
                    availableWidth < 100.dp -> 20.sp
                    else -> 24.sp
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    if (value != null) {
                        Text(
                            text = value,
                            color = valueColor,
                            fontSize = valueFontSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = title,
                        color = titleColor,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RainBigActionCard(isDarkMode: Boolean, onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var isNavigating by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    // ═══════════════════════════════════════════════════════════════════════════
    // CAMBIO PRINCIPAL: AZUL en lugar de negro
    // ═══════════════════════════════════════════════════════════════════════════
    val bgColor = RainBlue

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp,
        onClick = {
            if (!isNavigating) {
                isNavigating = true
                isPressed = true
                scope.launch {
                    kotlinx.coroutines.delay(100)
                    isPressed = false
                    onClick()
                    kotlinx.coroutines.delay(500)
                    isNavigating = false
                }
            }
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            val availableWidth = maxWidth
            val fontSize = when {
                availableWidth < 250.dp -> 14.sp
                availableWidth < 300.dp -> 16.sp
                else -> 18.sp
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        "NUEVO PROYECTO",
                        color = Color.White,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun RainMenuCard(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    animationType: RainAnimationType = RainAnimationType.NONE,
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
    var isNavigating by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "rain_menu_anim")
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
            .graphicsLayer { scaleX = scale; scaleY = scale },
        color = surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border),
        onClick = {
            if (!isNavigating) {
                isNavigating = true
                isPressed = true
                scope.launch {
                    kotlinx.coroutines.delay(100)
                    isPressed = false
                    onClick()
                    kotlinx.coroutines.delay(500)
                    isNavigating = false
                }
            }
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val availableWidth = maxWidth
            val titleFontSize = when {
                availableWidth < 200.dp -> 12.sp
                availableWidth < 280.dp -> 13.sp
                else -> 14.sp
            }
            val subtitleFontSize = when {
                availableWidth < 200.dp -> 10.sp
                availableWidth < 280.dp -> 11.sp
                else -> 12.sp
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6))
                        .graphicsLayer {
                            when (animationType) {
                                RainAnimationType.ROTATION -> rotationZ = rotationAnim
                                RainAnimationType.BOUNCE -> translationY = bounceAnim
                                RainAnimationType.NONE -> {}
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = RainBlue, // Iconos AZUL para Rain
                        modifier = Modifier.size(24.dp)
                    )
                    if (badgeCount != null && badgeCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = textPrimary,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = textMuted,
                        fontSize = subtitleFontSize
                    )
                }

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    tint = textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RainChangeProductButton(
    onClick: () -> Unit,
    isDarkMode: Boolean,
    enabled: Boolean
) {
    val scope = rememberCoroutineScope()
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
            .graphicsLayer { scaleX = scale; scaleY = scale }
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
                .background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFFEE2E2)),
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
                "Cambiar Producto",
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                if (enabled) "Volver a selección de productos" else "Acción no disponible",
                color = contentColor.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RainAdminTopBar(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    isGoogleAuthenticated: Boolean,
    googleUserEmail: String,
    onGoogleSignIn: () -> Unit,
    onGoogleSignOut: () -> Unit,
    surface: Color,
    border: Color,
    textPrimary: Color
) {
    val rotation by animateFloatAsState(
        targetValue = if (isDarkMode) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "rotation"
    )
    var showGoogleMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val logoRes = if (isDarkMode) R.drawable.hurricane_solution_blanco else R.drawable.logo_header_new
        RainCroppedLogo(resId = logoRes, height = 48.dp)

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de Google Drive
            Box {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(elevation = if (isDarkMode) 0.dp else 6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(surface)
                        .border(
                            1.5.dp,
                            if (isGoogleAuthenticated) Color(0xFF34A853) else border,
                            CircleShape
                        )
                        .clickable { showGoogleMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google_drive),
                        contentDescription = "Google Drive",
                        tint = if (isGoogleAuthenticated) Color(0xFF34A853) else textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (isGoogleAuthenticated) Color(0xFF34A853) else Color(0xFFEF4444))
                            .border(2.dp, surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGoogleAuthenticated) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = showGoogleMenu,
                    onDismissRequest = { showGoogleMenu = false },
                    modifier = Modifier.background(if (isDarkMode) Color(0xFF27272A) else Color.White)
                ) {
                    if (isGoogleAuthenticated) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Conectado", color = Color(0xFF34A853), fontSize = 12.sp)
                                    Text(
                                        googleUserEmail,
                                        color = textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            },
                            onClick = { },
                            enabled = false
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión de Drive", color = Color(0xFFEF4444)) },
                            onClick = {
                                showGoogleMenu = false
                                onGoogleSignOut()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Logout, null, tint = Color(0xFFEF4444))
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Conectar Google Drive", color = textPrimary) },
                            onClick = {
                                showGoogleMenu = false
                                onGoogleSignIn()
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_google_drive),
                                    contentDescription = null,
                                    tint = textPrimary
                                )
                            }
                        )
                    }
                }
            }

            // Botón de tema
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
                    painter = painterResource(id = if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon),
                    contentDescription = "Toggle theme",
                    tint = textPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}

@Composable
private fun RainAdminWelcomeText(userName: String, textPrimary: Color, textMuted: Color) {
    val annotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 26.sp)) {
            append("Bienvenido, ")
        }
        withStyle(SpanStyle(color = RainBlue, fontWeight = FontWeight.Bold, fontSize = 26.sp)) {
            append(userName)
        }
    }
    Text(annotatedString)
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Panel de Administración - Rain Protection",
        color = textMuted,
        fontSize = 14.sp
    )
}

@Composable
private fun RainSectionTitle(title: String, isDarkMode: Boolean, textPrimary: Color) {
    Text(
        text = title,
        color = textPrimary.copy(alpha = 0.7f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun RainCroppedLogo(@DrawableRes resId: Int, height: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val croppedBitmap = remember(resId) {
        val bmp = BitmapFactory.decodeResource(context.resources, resId)
            .copy(Bitmap.Config.ARGB_8888, false)
        trimTransparentRain(bmp)
    }
    Image(
        bitmap = croppedBitmap.asImageBitmap(),
        contentDescription = "Logo",
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit
    )
}

private fun trimTransparentRain(src: Bitmap): Bitmap {
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

private fun getRainSpanishDate(): String {
    val dias = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
    val meses = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    val cal = java.util.Calendar.getInstance()
    return "${dias[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]}, ${cal.get(java.util.Calendar.DAY_OF_MONTH)} de ${meses[cal.get(java.util.Calendar.MONTH)]}"
}