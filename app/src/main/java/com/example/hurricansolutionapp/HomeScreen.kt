package com.example.hurricansolutionapp

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.alpha




// COLORES ZINC (Extraídos de tu archivo de diseño Stich)
val Zinc950 = Color(0xFF09090B)
val Zinc900 = Color(0xFF18181B)
val Zinc800 = Color(0xFF27272A)
val Zinc400 = Color(0xFF71717A)


@Composable
fun HomeScreen(
    userFirstName: String,
    pendingCount: Int,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onNuevaCotizacion: () -> Unit,
    onVerCotizaciones: () -> Unit,
    onPendientes: () -> Unit,
    logoutEnabled: Boolean,
    onCerrarSesion: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val bg = if (isDarkMode) Zinc950 else Color.White
    val card = if (isDarkMode) Zinc900 else Color(0xFFF9FAFB)
    val border = if (isDarkMode) Zinc800 else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Zinc400 else Color(0xFF6B7280)

    LaunchedEffect(Unit) {
        verificarPreciosActualizados()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .statusBarsPadding()

    ) {
        Spacer(Modifier.height(20.dp))

        // 1. TOP BAR CON LOGO DINÁMICO Y BOTÓN CON PROFUNDIDAD REAL
        TopBar(isDarkMode, onToggleDarkMode, card, border, textPrimary)

        PreciosActualizadosBanner(
            isDarkMode = isDarkMode
        )

        Spacer(Modifier.height(32.dp))

        var titleSize by remember { mutableStateOf(34.sp) } // pon aquí tu tamaño “base”
        val minTitle = 22.sp

        // 2. SECCIÓN DE BIENVENIDA (DISEÑO LIMPIO)
        Text(
            text = "Jueves, 24 de Octubre",
            color = textMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        color = textPrimary,      // ✅ antes: Color.White
                        fontSize = titleSize,
                        fontWeight = FontWeight.Black
                    )
                ) { append("Bienvenido, ") }

                withStyle(
                    SpanStyle(
                        color = textMuted,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Black
                    )
                ) {
                    append(userFirstName)
                }
            },
            maxLines = 2,
            overflow = TextOverflow.Clip,
            onTextLayout = { result ->
                if (result.hasVisualOverflow && titleSize > minTitle) {
                    titleSize = (titleSize.value - 1).sp
                }
            }
        )


        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = border, thickness = 1.dp)
        Spacer(Modifier.height(18.dp))

        Text(
            text = "Acciones Rápidas",
            color = textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // 3. TARJETA PRINCIPAL (NUEVA COTIZACIÓN)
        BigActionCard(onNuevaCotizacion)

        Spacer(Modifier.height(16.dp))

        // 4. ACCIONES SECUNDARIAS
        SmallActionCard(
            title = "Ver Cotizaciones",
            subtitle = "Consultar historial reciente",
            iconRes = R.drawable.ic_history_lucide,
            onClick = onVerCotizaciones,
            isDarkMode = isDarkMode,
            card = card, border = border, textPrimary = textPrimary, textMuted = textMuted,
            showArrow = true
        )

        Spacer(Modifier.height(16.dp))

        SmallActionCard(
            title = "Pendientes por subir",
            subtitle = "Sincronizar datos locales",
            badgeCount = pendingCount,
            iconRes = R.drawable.ic_upload_lucide,
            onClick = onPendientes,
            isDarkMode = isDarkMode,
            card = card, border = border, textPrimary = textPrimary, textMuted = textMuted
        )

        Spacer(Modifier.height(16.dp))
        CerrarSesionButton(
            onClick = { if (logoutEnabled) showLogoutDialog = true },
            isDarkMode = isDarkMode,
            enabled = logoutEnabled
        )

        val dialogBg = if (isDarkMode) Zinc900 else Color.White
        val dialogTitle = if (isDarkMode) Color.White else Color(0xFF111418)
        val dialogText = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF374151)
        val cancelColor = if (isDarkMode) Color.White else Color(0xFF111418)
        val dangerColor = Color(0xFFE53935)

        Spacer(Modifier.height(24.dp))
        if (showLogoutDialog) {
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
                        text = "¿Deseas cerrar sesión?\nTendrás que volver a iniciar sesión.",
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
                        Text(
                            text = "Sí, salir",
                            color = dangerColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },

                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text(
                            text = "Cancelar",
                            color = cancelColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }
    }
}
@Composable
private fun TopBar(isDarkMode: Boolean, onToggleDarkMode: () -> Unit, card: Color, border: Color, textPrimary: Color) {
    // 1. Definimos la animación de rotación (Gira 180 grados)
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
        // TU LÓGICA DE LOGOS SE MANTIENE EXACTAMENTE IGUAL
        val logoRes = if (isDarkMode) R.drawable.hurricane_solution_blanco else R.drawable.logo_header_new
        CroppedLogo(
            resId = logoRes,
            height = 48.dp,
            modifier = Modifier
        )

        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(elevation = if(isDarkMode) 0.dp else 6.dp, CircleShape)
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
                    // 2. APLICAMOS LA ROTACIÓN SOLO AL ICONO
                    .graphicsLayer(rotationZ = rotation)
            )
        }
    }
}

@Composable
private fun BigActionCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)

    // 1. Scope para manejar el tiempo de espera (delay) [cite: 175, 190]
    val scope = rememberCoroutineScope()
    var isRotated by remember { mutableStateOf(false) }

    // 2. Definición del giro suave (400ms) [cite: 175, 190]
    val rotation by animateFloatAsState(
        targetValue = if (isRotated) 90f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .shadow(20.dp, shape, spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(shape)
            .border(
                width = 1.dp,
                color = Color.Black.copy(alpha = 0.08f),
                shape = shape
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF000000), Color(0xFF0B0B0D), Color(0xFF1A1A1D)), // Tu gradiente original [cite: 192]
                    start = Offset(0f, 0f),
                    end = Offset(900f, 0f)
                )
            )
            .clickable {
                // 3. Activa la rotación y espera antes de navegar [cite: 193, 194]
                isRotated = true
                scope.launch {
                    kotlinx.coroutines.delay(350)
                    onClick()
                    isRotated = false
                }
            }
    ) {
        Column(modifier = Modifier.padding(20.dp).align(Alignment.CenterStart)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.15f))
                    // 4. Aplica el giro únicamente al círculo del icono
                    .graphicsLayer(rotationZ = rotation),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 26.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text("Nueva Cotización", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            // No agregamos textos extra para no mover tu diseño original
        }
    }
}
@Composable
private fun SmallActionCard(
    title: String, subtitle: String, iconRes: Int, isDarkMode: Boolean,
    card: Color, border: Color, textPrimary: Color, textMuted: Color,
    badgeCount: Int? = null, showArrow: Boolean = false, onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "icon_animations")

    // Rotación parcial de vaivén para el Historial (-15 a 15 grados)
    val rotationOscilation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "oscilacion"
    )

    // Salto sutil para la nube (Upload)
    val uploadAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ), label = "upload"
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
                .background(
                    if (isDarkMode) Zinc800
                    else Color(0xFFE5E7EB) // 👈 gris un poquito más marcado
                )
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale

                    if (title == "Ver Cotizaciones") {
                        this.rotationZ = rotationOscilation
                        // El eje se queda en el centro para que rote sobre su sitio
                        this.transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                    } else if (title == "Pendientes por subir") {
                        this.translationY = uploadAnim
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
        if (badgeCount != null) {
            Text("• $badgeCount", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}
@Composable
private fun CerrarSesionButton(onClick: () -> Unit, isDarkMode: Boolean, enabled: Boolean) {
    val scope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "logout_anim")

    // La animación se queda quieta (0f) si está deshabilitado
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (enabled) 5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "logout_move"
    )

    // Ajuste de colores: si no está habilitado, usamos tonos Zinc/Gris
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

    // El contenido (Icono y Texto) se vuelve gris Zinc400 si está apagado
    val contentColor = if (enabled) Color(0xFFEF4444) else Zinc400

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(redBg)
            .border(1.dp, redBorder, RoundedCornerShape(16.dp))
            .alpha(if (enabled) 1f else 0.6f) // Suaviza todo el botón si está apagado
            .clickable(enabled = enabled) { scope.launch { onClick() } }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled) (if (isDarkMode) Color(0xFF7F1D1D).copy(alpha = 0.4f) else Color(0xFFFEE2E2)) else Color.Transparent),
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
                color = if (isDarkMode) contentColor.copy(alpha = 0.6f) else contentColor.copy(0.7f),
                fontSize = 12.sp
            )
        }
    }
}
@Composable
private fun CroppedLogo(
    @DrawableRes resId: Int,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val croppedBitmap = remember(resId) {
        val bmp = BitmapFactory.decodeResource(context.resources, resId)
            .copy(Bitmap.Config.ARGB_8888, false)
        trimTransparent(bmp)
    }

    Image(
        bitmap = croppedBitmap.asImageBitmap(),
        contentDescription = "Logo Hurricane Solution",
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit
    )
}

private fun trimTransparent(src: Bitmap): Bitmap {
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