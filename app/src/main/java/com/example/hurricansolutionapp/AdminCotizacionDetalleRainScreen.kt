package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * ═══════════════════════════════════════════════════════════════════════════════
 * ADMIN COTIZACIÓN DETALLE RAIN SCREEN
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Pantalla de detalle de cotización Rain para Admin.
 * Estilo similar a AdminCotizacionDetalleScreen pero para Rain Protection.
 *
 * MODO SOLO LECTURA:
 * - El admin puede ver todos los detalles
 * - Puede descargar el PDF
 * - NO puede editar (eso es solo para especialistas)
 */

// Color Rain
private val RainBlue = Color(0xFF2346AF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCotizacionDetalleRainScreen(
    cotizacion: CotizacionRainRemota,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

    // Colores
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val headerBg = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF9FAFB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF374151) else Color(0xFFE5E7EB)
    val accentBorder = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val primary = RainBlue

    fun formatMoney(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)} USD"
    }

    // Parsear medidas del JSON
    val medidas = remember(cotizacion.medidas) {
        try {
            if (cotizacion.medidas.isNullOrBlank()) {
                emptyList()
            } else {
                Json.decodeFromString<List<MedidaRainJson>>(cotizacion.medidas).map { it.toMedidaRain() }
            }
        } catch (e: Exception) {
            android.util.Log.e("AdminDetalleRain", "Error parseando medidas: ${e.message}")
            emptyList()
        }
    }

    fun descargarYVerPdf() {
        if (cotizacion.pdfPath.isNullOrBlank()) {
            Toast.makeText(context, "Esta cotización no tiene PDF asociado", Toast.LENGTH_SHORT).show()
            return
        }
        isDownloading = true
        scope.launch {
            try {
                val bucket = SupabaseClientProvider.client.storage.from("cotizaciones-pdf")
                val bytes = withContext(Dispatchers.IO) {
                    bucket.downloadAuthenticated(cotizacion.pdfPath!!)
                }
                val fileName = cotizacion.pdfPath!!.substringAfterLast("/")
                val file = java.io.File(context.cacheDir, fileName)
                file.writeBytes(bytes)
                verPdf(context, file)
            } catch (e: Exception) {
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
                            "DETALLE RAIN PROTECTION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.size(40.dp))
                    }
                }

                // Indicador visual azul
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
                            .background(primary)
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
                    // Totales
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("SUBTOTAL", color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(formatMoney(cotizacion.subtotal), color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        if (cotizacion.descuentoMonto > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("DESCUENTO (${cotizacion.descuentoPorcentaje.toInt()}%)", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text("-${formatMoney(cotizacion.descuentoMonto)}", color = Color(0xFF10B981), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = border)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("TOTAL", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(formatMoney(cotizacion.total), color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Botón Ver PDF
                    Button(
                        onClick = { descargarYVerPdf() },
                        enabled = !isDownloading && !cotizacion.pdfPath.isNullOrBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isDownloading) "Descargando..." else "VER PDF",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                    }

                    if (cotizacion.pdfPath.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Esta cotización aún no tiene PDF",
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
            // HEADER: Folio + Empleado + Fecha
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = surface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Folio
                                Box(
                                    modifier = Modifier
                                        .background(primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "FOLIO: ${cotizacion.folio}",
                                        color = primary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Badge Rain
                                Box(
                                    modifier = Modifier
                                        .background(primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .border(1.dp, primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "RAIN",
                                        color = primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
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
                            Icon(Icons.Default.CalendarToday, null, tint = textMuted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(cotizacion.fecha, color = textMuted, fontSize = 14.sp)
                        }
                    }
                }
            }

            // DATOS DEL CLIENTE
            item {
                RainAdminStitchCard(
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
                        RainAdminDataRow("Nombre", cotizacion.clienteNombre, textMuted, textPrimary, border)
                        if (!cotizacion.clienteTelefono.isNullOrBlank()) {
                            RainAdminDataRow("Teléfono", cotizacion.clienteTelefono, textMuted, textPrimary, border)
                        }
                        if (!cotizacion.ciudad.isNullOrBlank()) {
                            RainAdminDataRow("Ciudad", cotizacion.ciudad, textMuted, textPrimary, border)
                        }
                        if (!cotizacion.ubicacion.isNullOrBlank()) {
                            RainAdminDataRow("Ubicación", cotizacion.ubicacion, textMuted, textPrimary, border)
                        }
                        if (!cotizacion.tipoPropiedad.isNullOrBlank()) {
                            RainAdminDataRow("Tipo Propiedad", cotizacion.tipoPropiedad, textMuted, textPrimary, border, showDivider = false)
                        }
                    }
                }
            }

            // RESUMEN DE ÁREAS
            item {
                RainAdminStitchCard(
                    title = "RESUMEN DE ÁREAS",
                    icon = Icons.Default.Window,
                    isDarkMode = isDarkMode,
                    surface = surface,
                    headerBg = headerBg,
                    border = border,
                    accentBorder = accentBorder,
                    textPrimary = textPrimary,
                    textMuted = textMuted
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Total de áreas destacado
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("TOTAL DE ÁREAS", color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${cotizacion.totalAreas}", color = primary, fontSize = 28.sp, fontWeight = FontWeight.Black)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    // Eléctricas
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("${cotizacion.areasElectricas} Eléctrica${if (cotizacion.areasElectricas != 1) "s" else ""}", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    // Manuales
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF3B82F6))
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("${cotizacion.areasManuales} Manual${if (cotizacion.areasManuales != 1) "es" else ""}", color = Color(0xFF3B82F6), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }

                        // Accesorios adicionales
                        if (cotizacion.controlesAdicionales > 0 || cotizacion.manivelasAdicionales > 0) {
                            Spacer(Modifier.height(16.dp))
                            Text("ACCESORIOS ADICIONALES", color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))

                            if (cotizacion.controlesAdicionales > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Controles adicionales", color = textMuted, fontSize = 13.sp)
                                    Text("${cotizacion.controlesAdicionales}", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (cotizacion.manivelasAdicionales > 0) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Manivelas adicionales", color = textMuted, fontSize = 13.sp)
                                    Text("${cotizacion.manivelasAdicionales}", color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (cotizacion.costoAccesorios > 0) {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = border.copy(alpha = 0.5f))
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Costo accesorios", color = textMuted, fontSize = 13.sp)
                                    Text(formatMoney(cotizacion.costoAccesorios), color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // DETALLE DE MEDIDAS
            item {
                RainAdminStitchCard(
                    title = "DETALLE DE MEDIDAS",
                    icon = Icons.Default.Straighten,
                    isDarkMode = isDarkMode,
                    surface = surface,
                    headerBg = headerBg,
                    border = border,
                    accentBorder = accentBorder,
                    textPrimary = textPrimary,
                    textMuted = textMuted
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (medidas.isEmpty()) {
                            Text("No hay medidas registradas", color = textMuted, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                        } else {
                            medidas.forEachIndexed { index, medida ->
                                RainMedidaDetalleItem(
                                    index = index + 1,
                                    medida = medida,
                                    isDarkMode = isDarkMode,
                                    textPrimary = textPrimary,
                                    textMuted = textMuted,
                                    border = border,
                                    formatMoney = ::formatMoney,
                                    showDivider = index < medidas.lastIndex
                                )
                            }
                        }
                    }
                }
            }

            // OBSERVACIONES
            if (!cotizacion.observaciones.isNullOrBlank()) {
                item {
                    RainAdminStitchCard(
                        title = "OBSERVACIONES",
                        icon = Icons.Default.Notes,
                        isDarkMode = isDarkMode,
                        surface = surface,
                        headerBg = headerBg,
                        border = border,
                        accentBorder = accentBorder,
                        textPrimary = textPrimary,
                        textMuted = textMuted
                    ) {
                        Text(
                            cotizacion.observaciones,
                            color = textPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
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

@Composable
private fun RainAdminStitchCard(
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
    val primary = RainBlue

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column {
            // Header con borde lateral azul
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            color = primary.copy(alpha = 0.5f),
                            topLeft = androidx.compose.ui.geometry.Offset.Zero,
                            size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height)
                        )
                    }
                    .background(headerBg)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = primary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(title, color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            HorizontalDivider(color = border)

            content()
        }
    }
}

@Composable
private fun RainAdminDataRow(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    borderColor: Color,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = labelColor, fontSize = 13.sp)
            Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        if (showDivider) {
            HorizontalDivider(color = borderColor.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun RainMedidaDetalleItem(
    index: Int,
    medida: MedidaRain,
    isDarkMode: Boolean,
    textPrimary: Color,
    textMuted: Color,
    border: Color,
    formatMoney: (Double) -> String,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Número y descripción
            Row(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$index", color = textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(medida.descripcion, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${medida.ancho}m × ${medida.alto}m", color = textMuted, fontSize = 12.sp)
                        if (medida.piezas > 1) {
                            Text("× ${medida.piezas} pzas", color = textMuted, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Tipos y subtotales
            Column(horizontalAlignment = Alignment.End) {
                // Badges de tipo (puede haber uno o ambos)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (medida.incluyeManual) {
                        Surface(
                            color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Manual",
                                color = Color(0xFF3B82F6),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (medida.incluyeElectrico) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "Eléctrico",
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                // Mostrar subtotales separados si tiene ambos
                if (medida.incluyeManual && medida.incluyeElectrico) {
                    Text("M: ${formatMoney(medida.subtotalManual)}", color = textMuted, fontSize = 11.sp)
                    Text("E: ${formatMoney(medida.subtotalElectrico)}", color = textMuted, fontSize = 11.sp)
                } else {
                    Text(formatMoney(medida.subtotal), color = textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showDivider) {
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = border.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))
        }
    }
}