package com.example.hurricansolutionapp

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Banner que muestra cuando los precios han sido actualizados.
 * Se muestra en la parte superior del HomeScreen.
 *
 * USO: Agregar esto al inicio del HomeScreen, después del TopBar:
 *
 * PreciosActualizadosBanner(
 *     isDarkMode = isDarkMode,
 *     onDismiss = { }
 * )
 */
@Composable
fun PreciosActualizadosBanner(
    isDarkMode: Boolean,
    onDismiss: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val preciosCambiaron by PriceManager.preciosCambiaron.collectAsState()

    AnimatedVisibility(
        visible = preciosCambiaron,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            color = Color(0xFF10B981).copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Precios actualizados",
                        color = Color(0xFF10B981),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Los precios han sido modificados por el administrador",
                        color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF047857),
                        fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = {
                        PriceManager.clearPreciosCambiaron()
                        onDismiss()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Función para verificar precios al abrir la app.
 * Llamar esto en el LaunchedEffect del HomeScreen o MainActivity.
 *
 * USO:
 * LaunchedEffect(Unit) {
 *     verificarPreciosActualizados()
 * }
 */
suspend fun verificarPreciosActualizados() {
    try {
        PriceManager.refreshPrices()
    } catch (e: Exception) {
        android.util.Log.e("PreciosBanner", "Error verificando precios: ${e.message}")
    }
}