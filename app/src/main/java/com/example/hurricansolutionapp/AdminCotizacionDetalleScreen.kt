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
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.text.NumberFormat
import java.util.Locale

/**
 * Pantalla de detalle de cotización para Admin.
 * Estilo similar a ResumenScreen pero en modo solo lectura.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCotizacionDetalleScreen(
    cotizacion: CotizacionRemota,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

    // Colores (igual que ResumenScreen)
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val headerBg = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF9FAFB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF374151) else Color(0xFFE5E7EB)
    val accentBorder = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)

    fun formatMoney(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)} USD"
    }

    fun formatArea(area: Double): String {
        return "${String.format("%.2f", area)} m²"
    }

    // Parsear ventanas del JSON
    val ventanas = remember(cotizacion.ventanas) {
        try {
            when (val v = cotizacion.ventanas) {
                is JsonArray -> v.mapNotNull { element ->
                    try {
                        val obj = element.jsonObject
                        VentanaDetalle(
                            descripcion = obj["descripcion"]?.jsonPrimitive?.content ?: "",
                            alto = obj["alto"]?.jsonPrimitive?.double ?: 0.0,
                            ancho = obj["ancho"]?.jsonPrimitive?.double ?: 0.0,
                            precioM2 = obj["precio_m2"]?.jsonPrimitive?.double ?: obj["precioM2"]?.jsonPrimitive?.double ?: 0.0,
                            adecuacion = obj["adecuacion"]?.jsonPrimitive?.content ?: "",
                            tipoMontaje = obj["tipo_montaje"]?.jsonPrimitive?.content ?: obj["tipoMontaje"]?.jsonPrimitive?.content ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun descargarYVerPdf() {
        if (cotizacion.pdfPath.isNullOrBlank()) {
            Toast.makeText(context, "Esta cotización no tiene PDF asociado", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                isDownloading = true
                Toast.makeText(context, "Descargando PDF...", Toast.LENGTH_SHORT).show()

                val client = SupabaseClientProvider.client
                val bytes = withContext(Dispatchers.IO) {
                    client.storage.from("cotizaciones")
                        .downloadAuthenticated(cotizacion.pdfPath!!)
                }

                val fileName = "Cotizacion_${cotizacion.folio}.pdf"
                val file = java.io.File(context.getExternalFilesDir(null), fileName)
                file.writeBytes(bytes)

                verPdf(context, file)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al descargar: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isDownloading = false
            }
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            Column(modifier = Modifier.background(surface)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = surface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = textPrimary)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "DETALLE DE COTIZACIÓN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.size(40.dp))
                    }
                }

                // Indicador visual
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surface)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .width(24.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isDarkMode) Color.White else Color.Black)
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    // Mostrar totales en el bottom bar
                    cotizacion.productos.forEach { producto ->
                        val total = when (producto) {
                            "HS875" -> cotizacion.totalHs875 ?: 0.0
                            "HS1250" -> cotizacion.totalHs1250 ?: 0.0
                            "HS1500" -> cotizacion.totalHs1500 ?: 0.0
                            else -> 0.0
                        }
                        if (total > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "TOTAL $producto",
                                    color = textMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    formatMoney(total),
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Botones
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Botón Ver PDF
                        Button(
                            onClick = { descargarYVerPdf() },
                            enabled = !isDownloading && !cotizacion.pdfPath.isNullOrBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color.White else Color.Black,
                                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    color = if (isDarkMode) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = if (isDarkMode) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isDownloading) "DESCARGANDO..." else "VER PDF",
                                color = if (isDarkMode) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Advertencia si no hay PDF
                    if (cotizacion.pdfPath.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⚠️ Esta cotización aún no tiene PDF",
                            color = Color(0xFFD97706),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
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
            // ═══════════════════════════════════════════════════════════════════
            // HEADER: Folio + Empleado + Fecha
            // ═══════════════════════════════════════════════════════════════════
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, border)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Folio
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0xFF3B82F6).copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "FOLIO: ${cotizacion.folio}",
                                    color = Color(0xFF3B82F6),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Badge empleado
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    cotizacion.especialistaNombre,
                                    color = Color(0xFF8B5CF6),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(cotizacion.fecha, color = textMuted, fontSize = 14.sp)
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // DATOS DEL CLIENTE (Estilo StitchCard)
            // ═══════════════════════════════════════════════════════════════════
            item {
                AdminStitchCard(
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
                        AdminDataRow("Nombre", cotizacion.clienteNombre, textMuted, textPrimary, border)
                        if (!cotizacion.clienteTelefono.isNullOrBlank()) {
                            AdminDataRow("Teléfono", cotizacion.clienteTelefono, textMuted, textPrimary, border)
                        }
                        if (!cotizacion.ciudad.isNullOrBlank()) {
                            AdminDataRow("Ciudad", cotizacion.ciudad, textMuted, textPrimary, border)
                        }
                        if (!cotizacion.ubicacion.isNullOrBlank()) {
                            AdminDataRow("Ubicación", cotizacion.ubicacion, textMuted, textPrimary, border, showDivider = false)
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // APERTURAS (Ventanas)
            // ═══════════════════════════════════════════════════════════════════
            item {
                AdminStitchCard(
                    title = "APERTURAS",
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
                        // Área total destacada
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDarkMode) Color(0xFF1F1F1F) else Color.Black)
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "ÁREA TOTAL",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatArea(cotizacion.areaTotal),
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Header de aperturas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(headerBg)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "APERTURAS (${ventanas.size})",
                                color = textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                cotizacion.tipoMontaje ?: "Flush Mount",
                                color = textMuted,
                                fontSize = 10.sp
                            )
                        }

                        // Lista de ventanas
                        if (ventanas.isNotEmpty()) {
                            Column {
                                ventanas.forEachIndexed { index, ventana ->
                                    AdminAperturaItem(
                                        numero = index + 1,
                                        ventana = ventana,
                                        isDarkMode = isDarkMode,
                                        textPrimary = textPrimary,
                                        textMuted = textMuted,
                                        border = border
                                    )
                                    if (index < ventanas.lastIndex) {
                                        HorizontalDivider(color = border.copy(0.5f))
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No hay información de aperturas",
                                    color = textMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // SISTEMAS COTIZADOS
            // ═══════════════════════════════════════════════════════════════════
            item {
                AdminStitchCard(
                    title = "SISTEMAS COTIZADOS",
                    icon = Icons.Default.Category,
                    isDarkMode = isDarkMode,
                    surface = surface,
                    headerBg = headerBg,
                    border = border,
                    accentBorder = accentBorder,
                    textPrimary = textPrimary,
                    textMuted = textMuted
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        cotizacion.productos.forEach { producto ->
                            val total = when (producto) {
                                "HS875" -> cotizacion.totalHs875 ?: 0.0
                                "HS1250" -> cotizacion.totalHs1250 ?: 0.0
                                "HS1500" -> cotizacion.totalHs1500 ?: 0.0
                                else -> 0.0
                            }
                            val descuento = when (producto) {
                                "HS875" -> cotizacion.descuentoHs875 ?: 0.0
                                "HS1250" -> cotizacion.descuentoHs1250 ?: 0.0
                                "HS1500" -> cotizacion.descuentoHs1500 ?: 0.0
                                else -> 0.0
                            }

                            AdminSystemCard(
                                nombre = producto.replace("HS", "HS-"),
                                total = total,
                                descuento = descuento,
                                formatMoney = ::formatMoney,
                                isDarkMode = isDarkMode,
                                textPrimary = textPrimary,
                                textMuted = textMuted
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTES AUXILIARES
// ═══════════════════════════════════════════════════════════════════════════════

private data class VentanaDetalle(
    val descripcion: String,
    val alto: Double,
    val ancho: Double,
    val precioM2: Double,
    val adecuacion: String,
    val tipoMontaje: String
)

@Composable
private fun AdminStitchCard(
    title: String,
    icon: ImageVector,
    isDarkMode: Boolean,
    surface: Color,
    headerBg: Color,
    border: Color,
    accentBorder: Color,
    textPrimary: Color,
    textMuted: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentBorder)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    color = textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            HorizontalDivider(color = border.copy(0.5f))

            // Content
            content()
        }
    }
}

@Composable
private fun AdminDataRow(
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
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = textMuted, fontSize = 14.sp)
            Text(
                value,
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        if (showDivider) {
            HorizontalDivider(color = border.copy(0.3f))
        }
    }
}

@Composable
private fun AdminAperturaItem(
    numero: Int,
    ventana: VentanaDetalle,
    isDarkMode: Boolean,
    textPrimary: Color,
    textMuted: Color,
    border: Color
) {
    val area = ventana.alto * ventana.ancho

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Número
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$numero",
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                ventana.descripcion.ifBlank { "Apertura $numero" },
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row {
                Text(
                    "${String.format("%.2f", ventana.alto)} × ${String.format("%.2f", ventana.ancho)} m",
                    color = textMuted,
                    fontSize = 12.sp
                )
                if (ventana.adecuacion.isNotBlank()) {
                    Text(" • ", color = textMuted, fontSize = 12.sp)
                    Text(
                        ventana.adecuacion,
                        color = Color(0xFF8B5CF6),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Área
        Text(
            "${String.format("%.2f", area)} m²",
            color = textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AdminSystemCard(
    nombre: String,
    total: Double,
    descuento: Double,
    formatMoney: (Double) -> String,
    isDarkMode: Boolean,
    textPrimary: Color,
    textMuted: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF10B981).copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    nombre,
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (descuento > 0) {
                    Text(
                        "Descuento: $${descuento.toInt()}/m²",
                        color = textMuted,
                        fontSize = 12.sp
                    )
                }
            }
            Text(
                formatMoney(total),
                color = Color(0xFF10B981),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}