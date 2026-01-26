package com.example.hurricansolutionapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// COLORES GLOBALES STITCH - Usados en toda la app para consistencia
object StitchColors {
    // Fondos
    fun background(isDark: Boolean) = if (isDark) Color(0xFF000000) else Color(0xFFF3F4F6)
    fun surface(isDark: Boolean) = if (isDark) Color(0xFF111111) else Color.White
    fun surfaceVariant(isDark: Boolean) = if (isDark) Color(0xFF18181B) else Color(0xFFF9FAFB)

    // Textos
    fun textPrimary(isDark: Boolean) = if (isDark) Color.White else Color(0xFF111418)
    fun textSecondary(isDark: Boolean) = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    fun textMuted(isDark: Boolean) = if (isDark) Color(0xFF71717A) else Color(0xFF9CA3AF)

    // Bordes
    fun border(isDark: Boolean) = if (isDark) Color(0xFF27272A) else Color(0xFFE5E7EB)
    fun borderLight(isDark: Boolean) = if (isDark) Color(0xFF27272A) else Color(0xFFF3F4F6)

    // COLORES ESTANDARIZADOS - Verde #2AA63E y Rojo #C11007

    // Verde estandarizado: #2AA63E
    val greenStandard = Color(0xFF2AA63E)
    fun statusGreenBg(isDark: Boolean) = if (isDark) Color(0xFF2AA63E).copy(alpha = 0.15f) else Color(0xFF2AA63E).copy(alpha = 0.1f)
    fun statusGreenText(isDark: Boolean) = if (isDark) Color(0xFF2AA63E) else Color(0xFF2AA63E)
    fun statusGreenBorder(isDark: Boolean) = if (isDark) Color(0xFF2AA63E).copy(alpha = 0.3f) else Color(0xFF2AA63E).copy(alpha = 0.2f)

    // Rojo estandarizado: #C11007
    val redStandard = Color(0xFFC11007)
    fun statusRedBg(isDark: Boolean) = if (isDark) Color(0xFFC11007).copy(alpha = 0.15f) else Color(0xFFC11007).copy(alpha = 0.1f)
    fun statusRedText(isDark: Boolean) = if (isDark) Color(0xFFC11007) else Color(0xFFC11007)
    fun statusRedBorder(isDark: Boolean) = if (isDark) Color(0xFFC11007).copy(alpha = 0.3f) else Color(0xFFC11007).copy(alpha = 0.2f)

    // Primario (Negro/Blanco invertido)
    fun primary(isDark: Boolean) = if (isDark) Color.White else Color.Black
    fun onPrimary(isDark: Boolean) = if (isDark) Color.Black else Color.White
}

/**
 * TopBar estandar de la app - Diseno Stitch
 * SIN separador - se integra visualmente con el contenido
 *
 * @param title Titulo de la pantalla
 * @param onBack Accion al presionar el boton de volver
 * @param isDarkMode Si esta en modo oscuro
 * @param actions Composable opcional para acciones en el lado derecho
 */
@Composable
fun StitchTopBar(
    title: String,
    onBack: () -> Unit,
    isDarkMode: Boolean,
    actions: @Composable RowScope.() -> Unit = {}
) {
    // Usa el mismo color de fondo que la pantalla para integrarse visualmente
    val bgColor = StitchColors.background(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Boton de volver - Estilo circular con hover
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = textPrimary.copy(alpha = 0.1f)),
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "Volver",
                    tint = textPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = 3.dp)
                )
            }

            // Titulo centrado
            Text(
                text = title.uppercase(),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textPrimary,
                letterSpacing = 0.5.sp
            )

            // Acciones o spacer para balance
            Row(
                modifier = Modifier.width(40.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

/**
 * TopBar con linea separadora inferior - Diseno Stitch
 * Usa el color surface (blanco/oscuro) con una linea de separacion
 */
@Composable
fun StitchTopBarWithDivider(
    title: String,
    onBack: () -> Unit,
    isDarkMode: Boolean,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val border = StitchColors.border(isDarkMode)
    val surface = StitchColors.surface(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Boton de volver
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = textPrimary.copy(alpha = 0.1f)),
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                        contentDescription = "Volver",
                        tint = textPrimary,
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = 3.dp)
                    )
                }

                // Titulo centrado
                Text(
                    text = title.uppercase(),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textPrimary,
                    letterSpacing = 0.5.sp
                )

                // Acciones o spacer
                Row(
                    modifier = Modifier.width(40.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
        HorizontalDivider(color = border, thickness = 1.dp)
    }
}

/**
 * Boton de accion para el TopBar (sincronizar, refrescar, etc.)
 * Estilo: cuadrado redondeado negro/blanco segun tema
 */
@Composable
fun StitchTopBarActionButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val primary = StitchColors.primary(isDarkMode)
    val onPrimary = StitchColors.onPrimary(isDarkMode)

    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = primary,
        onClick = { if (enabled && !isLoading) onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = onPrimary
                )
            } else {
                icon()
            }
        }
    }
}