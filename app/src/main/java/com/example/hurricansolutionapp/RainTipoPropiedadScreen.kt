package com.example.hurricansolutionapp

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pantalla de selección de Tipo de Propiedad para Rain Protection
 * Copia exacta de TipoPropiedadScreen pero usando CotizacionRainDraft
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RainTipoPropiedadScreen(
    rainDraft: CotizacionRainDraft,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onTipoPropiedadSelected: (TipoPropiedad) -> Unit
) {
    BackHandler { onBack() }

    // Estado de selección
    var selectedTipo by remember { mutableStateOf<TipoPropiedad?>(null) }

    // Colores Stitch
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF9FAFB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val selectedBorder = if (isDarkMode) Color.White else Color.Black

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Tipo de Propiedad",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            // Título y subtítulo centrados
            Text(
                text = "Selecciona tu tipo de propiedad",
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Esto nos ayudará a personalizar mejor tu cotización",
                fontSize = 14.sp,
                color = textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(40.dp))

            // Cards de tipos de propiedad
            TipoPropiedad.entries.forEach { tipo ->
                val isSelected = selectedTipo == tipo

                RainTipoPropiedadCard(
                    tipo = tipo,
                    isSelected = isSelected,
                    isDarkMode = isDarkMode,
                    cardBg = cardBg,
                    border = border,
                    selectedBorder = selectedBorder,
                    textPrimary = textPrimary,
                    textMuted = textMuted,
                    onClick = {
                        selectedTipo = tipo
                        // Guardar en rainDraft y navegar inmediatamente
                        rainDraft.tipoPropiedad = tipo.name
                        onTipoPropiedadSelected(tipo)
                    }
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RainTipoPropiedadCard(
    tipo: TipoPropiedad,
    isSelected: Boolean,
    isDarkMode: Boolean,
    cardBg: Color,
    border: Color,
    selectedBorder: Color,
    textPrimary: Color,
    textMuted: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val borderColor = if (isSelected) selectedBorder else border
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = if (isSelected) 4.dp else 1.dp,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Contenido centrado
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icono según el tipo
                val iconVector = when (tipo) {
                    TipoPropiedad.CASA -> Icons.Default.Home
                    TipoPropiedad.DEPARTAMENTO -> Icons.Default.Apartment
                    TipoPropiedad.COMERCIAL -> Icons.Default.Store
                }

                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = if (isSelected) textPrimary else textMuted,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = tipo.label.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = if (isSelected) textPrimary else textMuted
                )
            }

            // Check icon cuando está seleccionado
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = if (isDarkMode) Color.White else Color.Black,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(24.dp)
                )
            }
        }
    }
}