package com.example.hurricansolutionapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla de selección de producto (Hurricane o Rain)
 * Diseño minimalista en blanco y negro
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionProductoScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onSelectHurricane: () -> Unit,
    onSelectRain: () -> Unit
) {
    BackHandler { onBack() }

    // Colores usando StitchColors
    val bg = StitchColors.background(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Nueva Cotización",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = "¿Qué desea cotizar?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Seleccione el tipo de protección",
                fontSize = 14.sp,
                color = textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // Card Hurricane Protection
            ProductoCardMinimal(
                titulo = "HURRICANE PROTECTION",
                subtitulo = "Protección contra huracanes",
                descripcion = "Sistemas HS-875 • HS-1250 • HS-1500",
                iconResId = R.drawable.ic_hurricane_logo, // Logo de Hurricane
                isDarkMode = isDarkMode,
                invertColors = false, // En dark mode: fondo negro, logo blanco
                onClick = onSelectHurricane
            )

            Spacer(Modifier.height(16.dp))

            // Card Rain Protection
            ProductoCardMinimal(
                titulo = "RAIN PROTECTION",
                subtitulo = "Toldo vertical enrollable",
                descripcion = "Protección contra lluvia y sol",
                iconResId = R.drawable.ic_hurricane_logo, // Logo de Rain (necesitas agregarlo)
                isDarkMode = isDarkMode,
                invertColors = true, // En dark mode: fondo blanco, logo negro
                onClick = onSelectRain
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "Puede cotizar ambos productos para el mismo cliente",
                fontSize = 12.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun ProductoCardMinimal(
    titulo: String,
    subtitulo: String,
    descripcion: String,
    iconResId: Int,
    isDarkMode: Boolean,
    invertColors: Boolean, // Si true, invierte colores (para Rain)
    onClick: () -> Unit
) {
    // Colores según el modo y si se invierten
    val cardBg: Color
    val iconBg: Color
    val iconTint: Color
    val textPrimary: Color
    val textAccent: Color
    val border: Color

    if (invertColors) {
        // Rain Protection: fondo blanco/negro invertido
        if (isDarkMode) {
            cardBg = Color.White
            iconBg = Color.White
            iconTint = Color.Black
            textPrimary = Color.Black
            textAccent = Color(0xFF333333)
            border = Color(0xFFE5E7EB)
        } else {
            cardBg = Color.Black
            iconBg = Color.Black
            iconTint = Color.White
            textPrimary = Color.White
            textAccent = Color(0xFFCCCCCC)
            border = Color(0xFF333333)
        }
    } else {
        // Hurricane Protection: colores normales del tema
        if (isDarkMode) {
            cardBg = Color(0xFF18181B)
            iconBg = Color.Black
            iconTint = Color.White
            textPrimary = Color.White
            textAccent = Color(0xFF9CA3AF)
            border = Color(0xFF27272A)
        } else {
            cardBg = Color.White
            iconBg = Color.White
            iconTint = Color.Black
            textPrimary = Color(0xFF111418)
            textAccent = Color(0xFF6B7280)
            border = Color(0xFFE5E7EB)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkMode) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono/Logo
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Usa el logo si existe, si no usa un placeholder
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = subtitulo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textAccent
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = descripcion,
                    fontSize = 11.sp,
                    color = textAccent.copy(alpha = 0.7f)
                )
            }

            // Flecha
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = textAccent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}