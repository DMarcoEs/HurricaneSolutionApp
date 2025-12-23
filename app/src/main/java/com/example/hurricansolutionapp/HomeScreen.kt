package com.example.hurricansolutionapp

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    userFirstName: String,
    pendingCount: Int,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onNuevaCotizacion: () -> Unit,
    onVerCotizaciones: () -> Unit,
    onPendientes: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val bg = if (isDarkMode) Color(0xFF050505) else Color(0xFFF6F6F6)
    val card = if (isDarkMode) Color(0xFF101114) else Color.White
    val border = if (isDarkMode) Color(0xFF202126) else Color(0xFFE8E8E8)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111111)
    val textMuted = if (isDarkMode) Color(0xFF9A9A9A) else Color(0xFF777777)

    val date = remember {
        val localeEs = Locale("es", "ES")
        val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", localeEs)
        val raw = LocalDate.now().format(formatter)
        raw.replaceFirstChar { it.uppercase(localeEs) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 18.dp)
            .padding(top = 14.dp, bottom = 18.dp)
    ) {
        TopBar(
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode,
            bg = bg,
            textPrimary = textPrimary
        )

        Spacer(Modifier.height(18.dp))

        // Fecha
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_my_calendar),
                contentDescription = null,
                tint = textMuted,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(date, color = textMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(8.dp))

        // Título
        Text(
            text = "Bienvenido,",
            color = textPrimary,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = userFirstName,
            color = if (isDarkMode) Color(0xFF6F6F6F) else Color(0xFFB0B0B0),
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        )

        Spacer(Modifier.height(16.dp))

        // (OPCIONAL) Stats cards – si las quieres, descomenta este bloque:
        /*
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Esta semana", "124m²", isDarkMode, card, border, textPrimary, textMuted)
            StatCard("Completadas", "8", isDarkMode, card, border, textPrimary, textMuted)
        }
        Spacer(Modifier.height(18.dp))
        Divider(color = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFEDEDED))
        Spacer(Modifier.height(18.dp))
        */

        Text(
            text = "Acciones Rápidas",
            color = textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(12.dp))

        // Nueva cotización (card grande)
        BigActionCard(
            title = "Nueva Cotización",
            subtitle = "Calcular por metraje",
            isDarkMode = isDarkMode,
            onClick = onNuevaCotizacion
        )

        Spacer(Modifier.height(14.dp))

        SmallActionCard(
            title = "Ver Cotizaciones",
            subtitle = "Consultar historial reciente",
            endArrow = true,
            isDarkMode = isDarkMode,
            card = card,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted,
            iconRes = android.R.drawable.ic_menu_recent_history,
            onClick = onVerCotizaciones
        )

        Spacer(Modifier.height(12.dp))

        SmallActionCard(
            title = "Pendientes por subir",
            subtitle = "Sincronizar datos locales",
            badgeText = "$pendingCount PENDIENTES",
            isDarkMode = isDarkMode,
            card = card,
            border = border,
            textPrimary = textPrimary,
            textMuted = textMuted,
            iconRes = android.R.drawable.ic_menu_upload,
            onClick = onPendientes
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = "Cerrar sesión",
            color = textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .clickable { onCerrarSesion() },
        )
    }
}

@Composable
private fun TopBar(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    bg: Color,
    textPrimary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo + nombre (izquierda)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.hurricane_solution_blanco), // <-- pon aquí tu icono cuadrado tipo “shield”
                contentDescription = null,
                modifier = Modifier.size(34.dp)
            )
            Spacer(Modifier.width(10.dp))
        }

        Spacer(Modifier.weight(1f))

        // Botón modo oscuro (derecha)
// Botón modo oscuro (derecha)
        IconButton(onClick = onToggleDarkMode) {
            Icon(
                painter = painterResource(
                    id = if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon
                ),
                contentDescription = "Tema",
                tint = Color.Unspecified
            )
        }

        /*
        Image(
            painter = painterResource(id = android.R.drawable.sym_def_app_icon),
            contentDescription = null,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(50))
                .border(1.dp, if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFE0E0E0), RoundedCornerShape(50)),
            contentScale = ContentScale.Crop
        )

         */
    }
}

@Composable
private fun BigActionCard(
    title: String,
    subtitle: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val bg = if (isDarkMode) Color(0xFF0B0B0C) else Color(0xFF111111)
    val overlay = Brush.horizontalGradient(
        listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.35f)
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(18.dp, shape)
            .clip(shape)
            .background(bg)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(18.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(10.dp))
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFFB1B1B1), fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }

        // panel decorativo a la derecha (simula la imagen del diseño)
        Box(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .background(overlay)
        )
    }
}

@Composable
private fun SmallActionCard(
    title: String,
    subtitle: String,
    isDarkMode: Boolean,
    card: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color,
    iconRes: Int,
    endArrow: Boolean = false,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape)
            .clip(shape)
            .background(card)
            .border(1.dp, border, shape)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDarkMode) Color(0xFF15161A) else Color(0xFFF4F4F4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = textMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            if (endArrow) {
                Text("›", color = textMuted, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(50))
                    .background(if (isDarkMode) Color(0xFF15161A) else Color(0xFFF4F4F4))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isDarkMode) Color.White else Color.Black)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = badgeText,
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}