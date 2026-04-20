package com.example.hurricansolutionapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * PRODUCT SELECTOR HOME SCREEN
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Primera pantalla después del login.
 * Permite al usuario seleccionar entre Hurricane Protection o Rain Protection.
 * Según la selección, navega al Home correspondiente.
 */
@Composable
fun ProductSelectorHomeScreen(
    userName: String,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onSelectHurricane: () -> Unit,
    onSelectRain: () -> Unit,
    onLogout: () -> Unit,
    logoutEnabled: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Colores
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    // Colores de producto
    val hurricaneGreen = Color(0xFF000000)
    val rainBlue = Color(0xFF2346AF)

    BackHandler {
        showLogoutDialog = true
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

        // ═══════════════════════════════════════════════════════════════════════
        // TOP BAR
        // ═══════════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            val logoRes = if (isDarkMode) R.drawable.hurricane_solution_blanco else R.drawable.logo_header_new
            SelectorCroppedLogo(resId = logoRes, height = 44.dp)

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
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // ═══════════════════════════════════════════════════════════════════════
        // SALUDO
        // ═══════════════════════════════════════════════════════════════════════
        Text(
            text = getSelectorSpanishDate(),
            color = textMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Bienvenido, $userName",
            color = textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(40.dp))

        // ═══════════════════════════════════════════════════════════════════════
        // TÍTULO DE SELECCIÓN
        // ═══════════════════════════════════════════════════════════════════════
        Text(
            text = "¿Qué producto deseas gestionar?",
            color = textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Selecciona una línea de productos",
            color = textMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        // ═══════════════════════════════════════════════════════════════════════
        // CARDS DE PRODUCTOS
        // ═══════════════════════════════════════════════════════════════════════

        // Hurricane Protection Card
        ProductSelectorCard(
            title = "HURRICANE PROTECTION",
            subtitle = "Mallas de protección contra huracanes",
            description = "Sistemas HS-875 • HS-1250 • HS-1500",
            iconResId = R.drawable.ic_hurricane_tornado,
            accentColor = hurricaneGreen,
            isDarkMode = isDarkMode,
            onClick = onSelectHurricane
        )

        Spacer(Modifier.height(16.dp))

        // Rain Protection Card
        ProductSelectorCard(
            title = "RAIN PROTECTION",
            subtitle = "Toldos verticales enrollables",
            description = "Protección contra lluvia y sol",
            iconResId = R.drawable.ic_rain_cloud,
            accentColor = rainBlue,
            isDarkMode = isDarkMode,
            onClick = onSelectRain
        )

        Spacer(Modifier.weight(1f))

        // ═══════════════════════════════════════════════════════════════════════
        // BOTÓN CERRAR SESIÓN
        // ═══════════════════════════════════════════════════════════════════════
        SelectorLogoutButton(
            onClick = { if (logoutEnabled) showLogoutDialog = true },
            isDarkMode = isDarkMode,
            enabled = logoutEnabled
        )

        Spacer(Modifier.height(32.dp))
    }

    // Dialog de confirmación de logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = if (isDarkMode) Color(0xFF18181B) else Color.White,
            tonalElevation = 6.dp,
            title = {
                Text(
                    "Cerrar sesión",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    "¿Deseas cerrar sesión?\nTendrás que volver a iniciar sesión.",
                    color = textMuted,
                    fontSize = 15.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text(
                        "Cerrar sesión",
                        color = if (isDarkMode) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = textMuted)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTES
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProductSelectorCard(
    title: String,
    subtitle: String,
    description: String,
    iconResId: Int,
    accentColor: Color,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val cardBg = if (isDarkMode) Color(0xFF18181B) else Color.White
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, border),
        shadowElevation = if (isDarkMode) 0.dp else 4.dp,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barra lateral de color
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            Spacer(Modifier.width(16.dp))

            // Icono
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMuted
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = textMuted.copy(alpha = 0.7f)
                )
            }

            // Flecha
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SelectorLogoutButton(
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

@Composable
private fun SelectorCroppedLogo(@DrawableRes resId: Int, height: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val croppedBitmap = remember(resId) {
        val bmp = BitmapFactory.decodeResource(context.resources, resId)
            .copy(Bitmap.Config.ARGB_8888, false)
        trimTransparentSelector(bmp)
    }
    Image(
        bitmap = croppedBitmap.asImageBitmap(),
        contentDescription = "Logo",
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit
    )
}

private fun trimTransparentSelector(src: Bitmap): Bitmap {
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
    return Bitmap.createBitmap(
        src,
        left,
        top,
        (right - left + 1).coerceAtLeast(1),
        (bottom - top + 1).coerceAtLeast(1)
    )
}

private fun getSelectorSpanishDate(): String {
    val dias = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
    val meses = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )
    val cal = java.util.Calendar.getInstance()
    return "${dias[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]}, ${cal.get(java.util.Calendar.DAY_OF_MONTH)} de ${meses[cal.get(java.util.Calendar.MONTH)]}"
}