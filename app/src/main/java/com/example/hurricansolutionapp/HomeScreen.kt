package com.example.hurricansolutionapp

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.graphics.BitmapFactory
import androidx.compose.ui.unit.Dp
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// CONSTANTES DE COLOR - Usadas por MainActivity y otros archivos
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
val Zinc950 = Color(0xFF09090B)
val Zinc900 = Color(0xFF18181B)
val Zinc800 = Color(0xFF27272A)
val Zinc400 = Color(0xFF71717A)

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// HOME SCREEN - Diseño Stitch (igual que AdminHomeScreen)
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
fun HomeScreen(
    userFirstName: String,
    pendingCount: Int,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onNuevaCotizacion: () -> Unit,
    onVerCotizaciones: () -> Unit,
    onPendientes: () -> Unit,
    onPendientesDrive: () -> Unit,
    onEnviosInstalacion: () -> Unit,
    logoutEnabled: Boolean,
    onCerrarSesion: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Verificar actualizacion de precios
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    try { PriceManager.checkForUpdates() } catch (_: Exception) { }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Colores Stitch (igual que AdminHomeScreen)
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

        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        // TOP BAR - Logo + Boton tema (sin badge ADMIN)
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        HomeTopBar(
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode,
            surface = surface,
            border = border,
            textPrimary = textPrimary
        )

        // Banner de precios actualizados
        PreciosActualizadosBanner(isDarkMode = isDarkMode)

        Spacer(Modifier.height(24.dp))

        // Fecha dinamica
        Text(
            text = getHomeSpanishDate(),
            color = textMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        // Bienvenida (mismo estilo que Admin)
        HomeWelcomeText(
            userName = userFirstName,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(24.dp))

        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        // MENU - con barra lateral (SIN SECCION ESTADISTICAS)
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        HomeSectionTitle(title = "MENU", isDarkMode = isDarkMode, textPrimary = textPrimary)

        Spacer(Modifier.height(16.dp))

        // Nueva Cotizacion (tarjeta grande negra)
        HomeBigActionCard(isDarkMode = isDarkMode, onClick = onNuevaCotizacion)

        Spacer(Modifier.height(12.dp))

        // Historial de Cotizaciones
        HomeMenuCard(
            title = "Historial De Cotizaciones Generadas",
            subtitle = "Ver mi historial de cotizaciones",
            iconRes = R.drawable.ic_history_lucide,
            animationType = 1,
            onClick = onVerCotizaciones,
            isDarkMode = isDarkMode,
            surface = surface,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(12.dp))

        // Sincronizaciones Pendientes
        HomeMenuCard(
            title = "Sincronizaciones Pendientes",
            subtitle = "Subir Cotizaciones A La Nube",
            badgeCount = pendingCount,
            iconRes = R.drawable.ic_upload_lucide,
            animationType = 2,
            onClick = onPendientes,
            isDarkMode = isDarkMode,
            surface = surface,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(12.dp))

        // Pendientes Google Drive
        HomeMenuCard(
            title = "Pendientes Google Drive",
            subtitle = "PDFs sin subir a Drive",
            iconRes = R.drawable.ic_google_drive,
            animationType = 0,
            onClick = onPendientesDrive,
            isDarkMode = isDarkMode,
            surface = surface,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(12.dp))

        // Envios a Instalacion
        HomeMenuCard(
            title = "Envíos a Instalación",
            subtitle = "Enviar cotizaciones al instalador",
            iconRes = R.drawable.ic_upload_lucide,
            animationType = 0,
            onClick = onEnviosInstalacion,
            isDarkMode = isDarkMode,
            surface = surface,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(16.dp))

        // Cerrar Sesion
        HomeLogoutButton(
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
                    text = "¿Deseas cerrar sesión?\nTendrás que volver a iniciar sesión.",
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
                    Text("Sí, salir", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
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

// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
// COMPONENTES PRIVADOS
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
private fun HomeTopBar(
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val logoRes = if (isDarkMode) R.drawable.hurricane_solution_blanco else R.drawable.logo_header_new
        HomeCroppedLogo(resId = logoRes, height = 48.dp)

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
                contentDescription = null,
                tint = textPrimary,
                modifier = Modifier.size(20.dp).graphicsLayer(rotationZ = rotation)
            )
        }
    }
}

@Composable
private fun HomeWelcomeText(userName: String, textPrimary: Color, textMuted: Color) {
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

@Composable
private fun HomeSectionTitle(title: String, isDarkMode: Boolean, textPrimary: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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

@Composable
private fun HomeBigActionCard(isDarkMode: Boolean, onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val bgColor = if (isDarkMode) Color(0xFF111111) else Color.Black
    val contentColor = Color.White

    Surface(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp,
        onClick = {
            isPressed = true
            scope.launch { kotlinx.coroutines.delay(100); isPressed = false; onClick() }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
                }
                Text("NUEVA COTIZACIÓN", color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = contentColor.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun HomeMenuCard(
    title: String,
    subtitle: String,
    @DrawableRes iconRes: Int,
    animationType: Int = 0,
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
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "rotation"
    )
    val bounceAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -6f,
        animationSpec = infiniteRepeatable(animation = tween(500, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "bounce"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Surface(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
        color = surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border),
        onClick = {
            isPressed = true
            scope.launch { kotlinx.coroutines.delay(100); isPressed = false; onClick() }
        }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6))
                    .graphicsLayer {
                        when (animationType) {
                            1 -> rotationZ = rotationAnim
                            2 -> translationY = bounceAnim
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(iconRes), contentDescription = null, tint = textMuted, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = textMuted, fontSize = 12.sp)
            }
            if (badgeCount != null && badgeCount > 0) {
                Text("¢ $badgeCount", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HomeLogoutButton(onClick: () -> Unit, isDarkMode: Boolean, enabled: Boolean) {
    val scope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "logout_anim")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = if (enabled) 5f else 0f,
        animationSpec = infiniteRepeatable(animation = tween(850, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
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
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDarkMode) Color(0xFF18181B) else Color(0xFFFEF2F2))
            .border(1.dp, if (isDarkMode) Color(0xFF27272A) else Color(0xFFFECACA), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) {
                isPressed = true
                scope.launch { kotlinx.coroutines.delay(100); isPressed = false; onClick() }
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
            Icon(painter = painterResource(R.drawable.ic_logout_lucide), contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text("Cerrar Sesión", color = contentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(if (enabled) "Salir de la cuenta" else "Acción no disponible", color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun HomeCroppedLogo(@DrawableRes resId: Int, height: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val croppedBitmap = remember(resId) {
        val bmp = BitmapFactory.decodeResource(context.resources, resId).copy(Bitmap.Config.ARGB_8888, false)
        trimTransparentHome(bmp)
    }
    Image(bitmap = croppedBitmap.asImageBitmap(), contentDescription = "Logo", modifier = modifier.height(height), contentScale = ContentScale.Fit)
}

private fun trimTransparentHome(src: Bitmap): Bitmap {
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

private fun getHomeSpanishDate(): String {
    val dias = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
    val meses = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    val cal = java.util.Calendar.getInstance()
    return "${dias[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]}, ${cal.get(java.util.Calendar.DAY_OF_MONTH)} de ${meses[cal.get(java.util.Calendar.MONTH)]}"
}