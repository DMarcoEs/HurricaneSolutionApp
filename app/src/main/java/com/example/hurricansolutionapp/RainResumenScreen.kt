package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.util.Locale

/**
 * Pantalla de Resumen para Rain Protection
 * Diseño idéntico a ResumenScreen de Hurricane pero:
 * - Sin tabs de TIPO DE SISTEMA / PRECIO DE VENTA
 * - Sin botones HS-875, HS-1250, HS-1500
 * - Muestra TOTAL ÁREAS (número) en lugar de ÁREA TOTAL (m²)
 * - Cada apertura muestra Tipo de Mecanismo en lugar de Tipo Montaje + Adecuaciones
 * - Sección de totales con Subtotal, Descuento, Total
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RainResumenScreen(
    rainDraft: CotizacionRainDraft,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onGuardarYGenerarPdf: () -> Unit,
    onCotizarOtroProducto: (TipoCotizacion) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var guardado by rememberSaveable { mutableStateOf(false) }
    var subiendoPdf by remember { mutableStateOf(false) }

    // Colores Stitch
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF0A0A0A) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val headerBg = if (isDarkMode) Color(0xFF111111) else Color(0xFFF9FAFB)
    val accentBorder = if (isDarkMode) Color.White else Color.Black

    // Obtener medidas válidas
    val medidas = rainDraft.getMedidas()
    val totalAreas = medidas.size
    val subtotal = rainDraft.getSubtotal()
    val descuentoPorcentaje = rainDraft.getDescuentoPorcentaje()
    val descuentoMonto = rainDraft.getDescuentoMonto()
    val total = rainDraft.getTotal()

    fun formatMoney(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        return format.format(amount)
    }

    // Dialog de confirmación para salir
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (guardado) {
            onBack()
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = surface,
            title = {
                Text(
                    if (guardado) "Salir de la cotización" else "¿Salir sin guardar?",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (guardado)
                        "Tu cotización ya fue guardada. ¿Deseas volver al inicio?"
                    else
                        "Aún no has guardado esta cotización. Si sales, perderás los datos capturados.",
                    color = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (guardado) {
                            if (isDarkMode) Color.White else Color.Black
                        } else Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (guardado) "Ir al inicio" else "Salir sin guardar",
                        color = if (guardado) {
                            if (isDarkMode) Color.Black else Color.White
                        } else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Continuar cotizando", color = textPrimary)
                }
            }
        )
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Resumen de Cotización",
                onBack = {
                    if (guardado) onBack() else showExitDialog = true
                },
                isDarkMode = isDarkMode
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mostrar total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOTAL RAIN PROTECTION",
                            color = textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            formatMoney(total),
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    if (!guardado) {
                        // Botón principal: Guardar y Generar PDF
                        Button(
                            onClick = {
                                if (medidas.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "No hay áreas válidas para guardar",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }

                                subiendoPdf = true
                                onGuardarYGenerarPdf()
                            },
                            enabled = !subiendoPdf && medidas.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color.White else Color.Black,
                                disabledContainerColor = textMuted.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (subiendoPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = if (isDarkMode) Color.Black else Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Guardando...",
                                    color = if (isDarkMode) Color.Black else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    null,
                                    tint = if (isDarkMode) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "GUARDAR Y GENERAR PDF",
                                    color = if (isDarkMode) Color.Black else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    } else {
                        // Después de guardar: botones de acciones
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // TODO: Compartir PDF
                                    Toast.makeText(context, "Función próximamente", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, if (isDarkMode) Color.White else Color.Black)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    null,
                                    tint = textPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Enviar",
                                    color = textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    // TODO: Ver PDF
                                    Toast.makeText(context, "Función próximamente", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkMode) Color.White else Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    null,
                                    tint = if (isDarkMode) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "PDF",
                                    color = if (isDarkMode) Color.Black else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, if (isDarkMode) Color.White else Color.Black)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    null,
                                    tint = textPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Editar",
                                    color = textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // ═══════════════════════════════════════════════════════════════
            // CARD: DATOS DEL CLIENTE
            // ═══════════════════════════════════════════════════════════════
            item {
                RainStitchCard(
                    title = "DATOS DEL CLIENTE",
                    icon = Icons.Default.Person,
                    isDarkMode = isDarkMode,
                    surface = surface,
                    headerBg = headerBg,
                    border = border,
                    accentBorder = accentBorder,
                    textPrimary = textPrimary,
                    textMuted = textMuted
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RainClienteDataRow(
                            "Nombre",
                            rainDraft.nombre,
                            textMuted,
                            textPrimary,
                            border
                        )
                        RainClienteDataRow(
                            "Teléfono",
                            rainDraft.telefono,
                            textMuted,
                            textPrimary,
                            border
                        )
                        RainClienteDataRow(
                            "Ciudad",
                            rainDraft.ciudad,
                            textMuted,
                            textPrimary,
                            border,
                            showDivider = false
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // CARD: ÁREAS (APERTURAS)
            // ═══════════════════════════════════════════════════════════════
            item {
                RainStitchCard(
                    title = "ÁREAS",
                    icon = Icons.Default.Straighten,
                    isDarkMode = isDarkMode,
                    surface = surface,
                    headerBg = headerBg,
                    border = border,
                    accentBorder = accentBorder,
                    textPrimary = textPrimary,
                    textMuted = textMuted
                ) {
                    Column {
                        // Box negro con TOTAL ÁREAS (número)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDarkMode) Color(0xFF1F1F1F) else Color.Black)
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "TOTAL ÁREAS",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "$totalAreas",
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Header de lista
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(headerBg)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "APERTURAS ($totalAreas)",
                                color = textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        // Lista de aperturas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                        ) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                medidas.forEachIndexed { index, medida ->
                                    RainAperturaItem(
                                        index + 1,
                                        medida,
                                        isDarkMode,
                                        textPrimary,
                                        textMuted,
                                        border
                                    )
                                    if (index < medidas.lastIndex) {
                                        HorizontalDivider(color = border.copy(0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // CARD: TOTALES (Subtotal, Descuento, Total)
            // ═══════════════════════════════════════════════════════════════
            item {
                RainStitchCard(
                    title = "RESUMEN DE PRECIOS",
                    icon = Icons.Default.Receipt,
                    isDarkMode = isDarkMode,
                    surface = surface,
                    headerBg = headerBg,
                    border = border,
                    accentBorder = accentBorder,
                    textPrimary = textPrimary,
                    textMuted = textMuted
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Subtotal
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Subtotal:",
                                color = textMuted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                formatMoney(subtotal),
                                color = textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Descuento
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Descuento (${String.format("%.1f", descuentoPorcentaje)}%):",
                                color = Color(0xFF22C55E),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "-${formatMoney(descuentoMonto)}",
                                color = Color(0xFF22C55E),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = border
                        )

                        // Total
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "TOTAL RAIN PROTECTION",
                                color = textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                formatMoney(total),
                                color = textPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Espacio para el bottom bar
            item {
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTES PRIVADOS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RainAperturaItem(
    index: Int,
    medida: MedidaRain,
    isDarkMode: Boolean,
    textPrimary: Color,
    textMuted: Color,
    border: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Número de apertura
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isDarkMode) Color(0xFF374151) else Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                String.format("%02d", index),
                color = textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            // Primera fila: Descripción y dimensiones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    medida.descripcion,
                    color = textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${String.format("%.2f", medida.alto)}m x ${String.format("%.2f", medida.ancho)}m",
                    color = textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(6.dp))

            // Segunda fila: Tipo de mecanismo (badge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge del tipo de mecanismo
                val isElectrico = medida.tipoMecanismo == TipoMecanismo.ELECTRICO
                Box(
                    modifier = Modifier
                        .background(
                            if (isElectrico) {
                                if (isDarkMode) Color(0xFF3B82F6) else Color(0xFF2563EB)
                            } else {
                                if (isDarkMode) Color(0xFF374151) else Color(0xFFF3F4F6)
                            },
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        medida.tipoMecanismo.etiqueta,
                        color = if (isElectrico) Color.White else textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun RainStitchCard(
    title: String,
    icon: ImageVector,
    isDarkMode: Boolean,
    surface: Color,
    headerBg: Color,
    border: Color,
    accentBorder: Color,
    textPrimary: Color,
    textMuted: Color,
    badge: String? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surface,
        shape = RoundedCornerShape(0.dp),
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Borde lateral de acento
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentBorder)
            )
            Column(modifier = Modifier.weight(1f)) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            title,
                            color = textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    badge?.let {
                        Box(
                            modifier = Modifier
                                .background(Color.Black, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                it,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                HorizontalDivider(color = border.copy(0.5f))
                content()
            }
        }
    }
}

@Composable
private fun RainClienteDataRow(
    label: String,
    value: String,
    textMuted: Color,
    textPrimary: Color,
    border: Color,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                color = textMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                value,
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, false)
            )
        }
        if (showDivider) {
            HorizontalDivider(color = border.copy(0.3f))
        }
    }
}