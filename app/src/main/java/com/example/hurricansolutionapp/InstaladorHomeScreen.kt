package com.example.hurricansolutionapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Home Screen del Instalador - Diseño Stitch
 * Logo GRANDE igual que Admin con función de recorte
 */
@Composable
fun InstaladorHomeScreen(
    instaladorName: String,
    pendingDriveCount: Int = 0,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onVerMedidas: () -> Unit,
    onPendientesDrive: () -> Unit,
    logoutEnabled: Boolean = true,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    var pendingCount by remember { mutableIntStateOf(pendingDriveCount) }

    LaunchedEffect(Unit) {
        scope.launch {
            try { pendingCount = InstaladorRepository.countAllPending() } catch (_: Exception) {}
        }
    }

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

        // TopBar con logo GRANDE igual que Admin
        InstaladorTopBar(isDarkMode, onToggleDarkMode, surface, border, textPrimary)

        Spacer(Modifier.height(24.dp))

        Text(getInstaladorSpanishDate(), color = textMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)

        InstaladorWelcomeText(instaladorName, textPrimary, textMuted)

        Spacer(Modifier.height(24.dp))

        InstaladorSectionTitle("MENÚ", isDarkMode, textPrimary)

        Spacer(Modifier.height(16.dp))

        InstaladorBigActionCard(isDarkMode, onVerMedidas)

        Spacer(Modifier.height(12.dp))

        InstaladorMenuCard(
            title = "Pendientes Google Drive",
            subtitle = "PDFs sin subir a Drive",
            iconRes = R.drawable.ic_google_drive,
            animationType = 2,
            badgeCount = if (pendingCount > 0) pendingCount else null,
            onClick = onPendientesDrive,
            isDarkMode = isDarkMode,
            surface = surface,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted
        )

        Spacer(Modifier.height(16.dp))

        InstaladorLogoutButton({ if (logoutEnabled) showLogoutDialog = true }, isDarkMode, logoutEnabled)

        Spacer(Modifier.height(32.dp))
    }

    // Diálogo de confirmación de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = if (isDarkMode) Color(0xFF18181B) else Color.White,
            tonalElevation = 6.dp,
            title = { Text("Cerrar sesión", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = { Text("¿Deseas cerrar sesión?\nTendrás que volver a iniciar sesión.", color = textMuted, fontSize = 15.sp) },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; onCerrarSesion() }) {
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

/**
 * TopBar con logo GRANDE usando función de recorte (igual que Admin)
 */
@Composable
private fun InstaladorTopBar(isDarkMode: Boolean, onToggleDarkMode: () -> Unit, surface: Color, border: Color, textPrimary: Color) {
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
        // Logo + Badge INSTALADOR juntos (igual que Admin)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Logo GRANDE con función de recorte (igual que Admin)
            val logoRes = if (isDarkMode) R.drawable.hurricane_solution_blanco else R.drawable.logo_header_new
            InstaladorCroppedLogo(resId = logoRes, height = 48.dp)

            // Badge INSTALADOR pegado al logo
            Surface(
                color = if (isDarkMode) Color.White else Color.Black,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "INSTALADOR",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = if (isDarkMode) Color.Black else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        // Botón tema circular
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

/**
 * Logo recortado - IGUAL que AdminCroppedLogo
 * Recorta los espacios transparentes para que el logo se vea más grande
 */
@Composable
private fun InstaladorCroppedLogo(@DrawableRes resId: Int, height: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val croppedBitmap = remember(resId) {
        val bmp = BitmapFactory.decodeResource(context.resources, resId)
            .copy(Bitmap.Config.ARGB_8888, false)
        trimTransparentInstalador(bmp)
    }

    Image(
        bitmap = croppedBitmap.asImageBitmap(),
        contentDescription = "Logo",
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit
    )
}

/**
 * Función para recortar espacios transparentes del logo
 * IGUAL que trimTransparentAdmin
 */
private fun trimTransparentInstalador(src: Bitmap): Bitmap {
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

@Composable
private fun InstaladorWelcomeText(userName: String, textPrimary: Color, textMuted: Color) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = textPrimary, fontWeight = FontWeight.Black)) { append("Bienvenido, ") }
            withStyle(SpanStyle(color = textMuted, fontWeight = FontWeight.Black)) { append(userName) }
        },
        fontSize = 32.sp,
        lineHeight = 38.sp,
        maxLines = 3
    )
}

@Composable
private fun InstaladorSectionTitle(title: String, isDarkMode: Boolean, textPrimary: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(if (isDarkMode) Color.White else Color.Black))
        Spacer(Modifier.width(12.dp))
        Text(title, color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun InstaladorBigActionCard(isDarkMode: Boolean, onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")
    val bgColor = if (isDarkMode) Color(0xFF111111) else Color.Black
    val contentColor = Color.White

    Surface(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp,
        onClick = { isPressed = true; scope.launch { delay(100); isPressed = false; onClick() } }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Straighten, contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
                }
                Text("MEDIDAS ASIGNADAS", color = contentColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = contentColor.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun InstaladorMenuCard(title: String, subtitle: String, iconRes: Int, animationType: Int = 0, badgeCount: Int? = null, onClick: () -> Unit, isDarkMode: Boolean, surface: Color, border: Color, textPrimary: Color, textMuted: Color) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "menu_anim")
    val rotationAnim by infiniteTransition.animateFloat(initialValue = -10f, targetValue = 10f, animationSpec = infiniteRepeatable(animation = tween(600, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse), label = "rotation")
    val bounceAnim by infiniteTransition.animateFloat(initialValue = 0f, targetValue = -6f, animationSpec = infiniteRepeatable(animation = tween(500, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse), label = "bounce")
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")

    Surface(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
        color = surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border),
        onClick = { isPressed = true; scope.launch { delay(100); isPressed = false; onClick() } }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)).graphicsLayer {
                    when (animationType) { 1 -> rotationZ = rotationAnim; 2 -> translationY = bounceAnim }
                },
                contentAlignment = Alignment.Center
            ) { Icon(painter = painterResource(iconRes), contentDescription = null, tint = textMuted, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = textMuted, fontSize = 12.sp)
            }
            if (badgeCount != null && badgeCount > 0) {
                Surface(color = Color(0xFFEF4444), shape = CircleShape) {
                    Text(if (badgeCount > 99) "99+" else badgeCount.toString(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InstaladorLogoutButton(onClick: () -> Unit, isDarkMode: Boolean, enabled: Boolean) {
    val scope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "logout_anim")
    val offsetX by infiniteTransition.animateFloat(initialValue = 0f, targetValue = if (enabled) 5f else 0f, animationSpec = infiniteRepeatable(animation = tween(850, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse), label = "offset")
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale")
    val contentColor = if (isDarkMode) { if (enabled) Color(0xFFFCA5A5) else Color(0xFF6B7280) } else { if (enabled) Color(0xFFDC2626) else Color(0xFF9CA3AF) }
    val bgColor = if (isDarkMode) { if (enabled) Color(0xFF7F1D1D).copy(alpha = 0.3f) else Color(0xFF27272A) } else { if (enabled) Color(0xFFFEE2E2) else Color(0xFFF3F4F6) }
    val borderColor = if (enabled) contentColor.copy(alpha = 0.3f) else Color.Transparent

    Surface(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        onClick = { if (enabled) { isPressed = true; scope.launch { delay(100); isPressed = false; onClick() } } }
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(contentColor.copy(alpha = 0.15f)).graphicsLayer { translationX = offsetX }, contentAlignment = Alignment.Center) {
                Icon(painter = painterResource(R.drawable.ic_logout_lucide), contentDescription = null, tint = contentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Cerrar Sesión", color = contentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text("Salir de la cuenta", color = contentColor.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

private fun getInstaladorSpanishDate(): String {
    val locale = Locale("es", "MX")
    val calendar = Calendar.getInstance()
    val dayOfWeek = SimpleDateFormat("EEEE", locale).format(calendar.time).replaceFirstChar { it.uppercase() }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = SimpleDateFormat("MMMM", locale).format(calendar.time)
    return "$dayOfWeek, $day de $month"
}