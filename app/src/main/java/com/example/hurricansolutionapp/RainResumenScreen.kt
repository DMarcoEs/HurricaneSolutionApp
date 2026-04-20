package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.jan.supabase.storage.storage
import java.io.File
import java.text.NumberFormat
import java.util.Locale

/**
 * Pantalla de Resumen para Rain Protection
 * Replica toda la funcionalidad de ResumenScreen de Hurricane:
 * - Generación de PDF con apertura automática
 * - Guardar en Supabase + Storage + Drive
 * - Botones Enviar, PDF, Editar post-guardado
 * - Soporte desdeHistorial con regeneración y actualización en Drive
 * - Sin tabs de TIPO DE SISTEMA / PRECIO DE VENTA
 */

// Color Rain Protection
private val RainBlue = Color(0xFF2346AF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RainResumenScreen(
    rainDraft: CotizacionRainDraft,
    isDarkMode: Boolean,
    desdeHistorial: Boolean = false,
    cotizacionRainExistente: CotizacionRain? = null,
    onBack: () -> Unit,
    onVolverAInicio: () -> Unit,
    onVolverAEditar: () -> Unit = {},
    onVolverAHistorial: () -> Unit = {},
    onCotizarOtroProducto: (TipoCotizacion) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var guardado by rememberSaveable { mutableStateOf(desdeHistorial) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var folioGenerado by rememberSaveable { mutableStateOf(rainDraft.folio) }
    var subiendoPdf by remember { mutableStateOf(false) }
    var mensajeSubida by remember { mutableStateOf<String?>(null) }

    // Estados para detectar cambios desde historial
    var pdfRegenerado by rememberSaveable { mutableStateOf(false) }
    var subiendoADrive by remember { mutableStateOf(false) }

    // ═══════════════════════════════════════════════════════════════════════════
    // ESTADOS DE ACCESORIOS
    // ═══════════════════════════════════════════════════════════════════════════
    var quiereControles by rememberSaveable { mutableStateOf(rainDraft.quiereControles) }
    var cantidadControles by rememberSaveable { mutableIntStateOf(rainDraft.cantidadControles.coerceAtLeast(1)) }
    var quiereManivelas by rememberSaveable { mutableStateOf(rainDraft.quiereManivelas) }
    var cantidadManivelas by rememberSaveable { mutableIntStateOf(rainDraft.cantidadManivelas.coerceAtLeast(1)) }

    // Resetear estado cuando llega una cotizacion nueva
    LaunchedEffect(rainDraft.folio, desdeHistorial) {
        if (!desdeHistorial && rainDraft.folio.isBlank()) {
            guardado = false
            pdfRegenerado = false
            folioGenerado = ""
            pdfFile = null
        }
    }

    // Drive pending state persistence
    val drivePrefs = remember {
        context.getSharedPreferences("drive_pending_prefs", android.content.Context.MODE_PRIVATE)
    }

    fun hasPendingDriveUpdate(folio: String): Boolean {
        return drivePrefs.getBoolean("pending_drive_rain_$folio", false)
    }
    fun markPendingDriveUpdate(folio: String) {
        drivePrefs.edit().putBoolean("pending_drive_rain_$folio", true).apply()
    }
    fun clearPendingDriveUpdate(folio: String) {
        drivePrefs.edit().remove("pending_drive_rain_$folio").apply()
    }

    LaunchedEffect(rainDraft.folio, desdeHistorial) {
        if (desdeHistorial && rainDraft.folio.isNotBlank() && hasPendingDriveUpdate(rainDraft.folio)) {
            pdfRegenerado = true
        }
    }

    // Colores Stitch
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF0A0A0A) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val headerBg = if (isDarkMode) Color(0xFF111111) else Color(0xFFF9FAFB)
    val accentBorder = RainBlue  // Azul Rain

    // Obtener medidas válidas
    val medidas = rainDraft.getMedidas()
    val totalAreas = medidas.size
    val subtotal = rainDraft.getSubtotal()
    val subtotalManual = rainDraft.getSubtotalManual()
    val subtotalElectrico = rainDraft.getSubtotalElectrico()
    val descuentoPorcentaje = rainDraft.getDescuentoPorcentaje()
    val descuentoMonto = rainDraft.getDescuentoMonto()
    val descuentoMontoManual = rainDraft.getDescuentoMontoManual()
    val descuentoMontoElectrico = rainDraft.getDescuentoMontoElectrico()

    // Costo de accesorios (reactivo a switches/cantidades)
    val costoAccesorios = remember(quiereControles, cantidadControles, quiereManivelas, cantidadManivelas) {
        val ctrlCount = if (quiereControles) cantidadControles else 0
        val manCount = if (quiereManivelas) cantidadManivelas else 0
        RainPriceManager.calcularCostoAccesorios(ctrlCount, manCount)
    }

    // Totales = (Subtotal - Descuento) + Accesorios
    val total = (subtotal - descuentoMonto) + costoAccesorios
    val totalManual = (subtotalManual - descuentoMontoManual) + costoAccesorios
    val totalElectrico = (subtotalElectrico - descuentoMontoElectrico) + costoAccesorios

    // Verificar si tiene cada tipo de mecanismo
    val tieneManual = rainDraft.tieneManual()
    val tieneElectrico = rainDraft.tieneElectrico()

    // Sincronizar con el draft
    LaunchedEffect(quiereControles, cantidadControles, quiereManivelas, cantidadManivelas) {
        rainDraft.quiereControles = quiereControles
        rainDraft.cantidadControles = if (quiereControles) cantidadControles else 0
        rainDraft.quiereManivelas = quiereManivelas
        rainDraft.cantidadManivelas = if (quiereManivelas) cantidadManivelas else 0
    }

    fun formatMoney(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        return format.format(amount)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GENERACIÓN Y CACHÉ DE PDF
    // ═══════════════════════════════════════════════════════════════════════════

    // Cotización Rain usada para el PDF (construida desde draft o existente)
    var cotizacionRainId by rememberSaveable { mutableStateOf(cotizacionRainExistente?.id ?: 0L) }

    fun buildCotizacionRain(): CotizacionRain {
        val ubicacionParts = listOfNotNull(
            rainDraft.ciudad.ifBlank { null },
            rainDraft.colonia.ifBlank { null },
            rainDraft.direccionDetalle.ifBlank { null }
        )
        return CotizacionRain(
            id = cotizacionRainId,
            folio = folioGenerado.ifBlank { rainDraft.folio },
            clienteNombre = rainDraft.nombre,
            clienteTelefono = rainDraft.telefono,
            ubicacion = ubicacionParts.joinToString(", "),
            ciudad = rainDraft.ciudad,
            especialista = SessionManager.getNombre(context),
            fecha = rainDraft.fecha.ifBlank {
                java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date())
            },
            medidas = medidas,
            zonaGeografica = rainDraft.zonaGeografica,
            tipoPropiedad = rainDraft.tipoPropiedad,
            subtotal = subtotal,
            subtotalManual = subtotalManual,
            subtotalElectrico = subtotalElectrico,
            descuentoPorcentaje = descuentoPorcentaje,
            descuentoMonto = descuentoMonto,
            total = total,
            totalManual = totalManual,
            totalElectrico = totalElectrico,
            totalAreas = totalAreas,
            areasElectricas = rainDraft.getAreasElectricas(),
            areasManuales = rainDraft.getAreasManuales(),
            observaciones = rainDraft.observaciones,
            controlesAdicionales = if (quiereControles) cantidadControles else 0,
            manivelasAdicionales = if (quiereManivelas) cantidadManivelas else 0,
            costoAccesorios = costoAccesorios
        )
    }

    fun obtenerOGenerarPdf(skipEnqueue: Boolean = false): File? {
        if (pdfFile != null && pdfFile!!.exists()) return pdfFile

        val cotizacionRain = buildCotizacionRain()
        val pdf = generarPdfRainCotizacion(context, cotizacionRain, skipEnqueue)
        if (pdf != null) pdfFile = pdf
        return pdf
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DIALOG DE CONFIRMACIÓN PARA SALIR
    // ═══════════════════════════════════════════════════════════════════════════

    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        when {
            desdeHistorial -> onVolverAHistorial()
            guardado -> showExitDialog = true
            else -> onBack() // Volver a MedidasScreen para corregir
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
                        onVolverAInicio()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (guardado) {
                            if (isDarkMode) Color.White else RainBlue
                        } else Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (guardado) "Ir al inicio" else "Salir sin guardar",
                        color = Color.White,
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

    // ═══════════════════════════════════════════════════════════════════════════
    // UI PRINCIPAL
    // ═══════════════════════════════════════════════════════════════════════════

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Resumen de Cotización",
                onBack = {
                    when {
                        desdeHistorial -> onVolverAHistorial()
                        guardado -> showExitDialog = true
                        else -> onBack() // Volver a MedidasScreen para corregir
                    }
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Desglose de precios minimalista
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = textMuted, fontSize = 11.sp)
                        Text(formatMoney(subtotal), color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Descuento (${String.format("%.1f", descuentoPorcentaje)}%)",
                            color = Color(0xFF22C55E),
                            fontSize = 11.sp
                        )
                        Text(
                            "-${formatMoney(descuentoMonto)}",
                            color = Color(0xFF22C55E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (costoAccesorios > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Accesorios", color = textMuted, fontSize = 11.sp)
                            Text(
                                "+${formatMoney(costoAccesorios)}",
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    HorizontalDivider(color = border.copy(0.3f), modifier = Modifier.padding(vertical = 4.dp))

                    // Total
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

                    // Mensaje de subida
                    mensajeSubida?.let { msg ->
                        Text(
                            msg,
                            color = textMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (!guardado) {
                        // ═══════════════════════════════════════════════════
                        // BOTÓN: GUARDAR Y GENERAR PDF
                        // ═══════════════════════════════════════════════════
                        Button(
                            onClick = {
                                if (medidas.isEmpty()) {
                                    Toast.makeText(context, "No hay áreas válidas para guardar", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                subiendoPdf = true

                                scope.launch {
                                    try {
                                        // 1. Generar folio
                                        val especialista = SessionManager.getNombre(context)
                                        val folio = RainFolioManager.nextFolioForEspecialista(context, especialista)
                                        rainDraft.folio = folio
                                        folioGenerado = folio

                                        // 2. Fecha actual
                                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        rainDraft.fecha = sdf.format(java.util.Date())

                                        // 3. Construir ubicación
                                        val ubicacionParts = listOfNotNull(
                                            rainDraft.ciudad.ifBlank { null },
                                            rainDraft.colonia.ifBlank { null },
                                            rainDraft.direccionDetalle.ifBlank { null }
                                        )
                                        val ubicacionCompleta = ubicacionParts.joinToString(", ")

                                        // 4. Guardar en Supabase
                                        val userId = SessionManager.getUserId(context)
                                        val insertData = CotizacionRainInsert(
                                            folio = folio,
                                            userId = userId,
                                            especialistaNombre = especialista,
                                            clienteNombre = rainDraft.nombre,
                                            clienteTelefono = rainDraft.telefono.ifBlank { null },
                                            ubicacion = ubicacionCompleta,
                                            ciudad = rainDraft.ciudad.ifBlank { null },
                                            colonia = rainDraft.colonia.ifBlank { null },
                                            calle = rainDraft.direccionDetalle.ifBlank { null },
                                            fecha = rainDraft.fecha,
                                            zonaGeografica = rainDraft.zonaGeografica.id,
                                            tipoPropiedad = rainDraft.tipoPropiedad.ifBlank { null },
                                            medidas = rainDraft.getMedidasJson(),
                                            subtotal = subtotal,
                                            descuentoPorcentaje = descuentoPorcentaje,
                                            descuentoMonto = descuentoMonto,
                                            total = total,
                                            totalAreas = totalAreas,
                                            areasElectricas = rainDraft.getAreasElectricas(),
                                            areasManuales = rainDraft.getAreasManuales(),
                                            leadId = rainDraft.leadId,
                                            observaciones = rainDraft.observaciones.ifBlank { null },
                                            controlesAdicionales = if (quiereControles) cantidadControles else 0,
                                            manivelasAdicionales = if (quiereManivelas) cantidadManivelas else 0,
                                            costoAccesorios = costoAccesorios
                                        )

                                        val saveResult = withContext(Dispatchers.IO) {
                                            RainRepository.saveCotizacion(insertData)
                                        }
                                        val savedId = saveResult.getOrThrow()
                                        cotizacionRainId = savedId

                                        // 5. Generar PDF
                                        val cotizacionRain = buildCotizacionRain()
                                        val pdf = withContext(Dispatchers.IO) {
                                            generarPdfRainCotizacion(context, cotizacionRain)
                                        }

                                        if (pdf != null) {
                                            pdfFile = pdf
                                            guardado = true

                                            // 6. Actualizar pdf_path en Supabase
                                            withContext(Dispatchers.IO) {
                                                RainRepository.updatePdfPath(savedId, pdf.absolutePath)
                                            }

                                            // 7. Subir PDF a Supabase Storage
                                            withContext(Dispatchers.IO) {
                                                try {
                                                    val clienteFormateado = rainDraft.nombre.trim()
                                                        .split("\\s+".toRegex())
                                                        .joinToString("_") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
                                                        .take(30)
                                                    val pdfRemotePath = "$userId/Rain_${clienteFormateado}_${folio}.pdf"
                                                    val supabase = SupabaseClientProvider.client
                                                    val bytes = pdf.readBytes()
                                                    supabase.storage
                                                        .from("cotizaciones")
                                                        .upload(
                                                            path = pdfRemotePath,
                                                            data = bytes,
                                                            upsert = true
                                                        )
                                                    android.util.Log.d("RainResumen", "PDF subido a Storage: $pdfRemotePath")
                                                } catch (e: Exception) {
                                                    android.util.Log.e("RainResumen", "Error subiendo a Storage: ${e.message}")
                                                }
                                            }

                                            // 8. Subir a Google Drive
                                            withContext(Dispatchers.IO) {
                                                try {
                                                    val userName = SessionManager.getNombre(context)
                                                    val userRole = SessionManager.getRole(context)
                                                    if (userName.isNotBlank() && DriveAuthManager.isAuthenticated(context)) {
                                                        val driveSuccess = DriveUploadManager.uploadPdfToDriveAuto(
                                                            context = context,
                                                            pdfFile = pdf,
                                                            userName = userName,
                                                            userRole = userRole,
                                                            folio = folio
                                                        )
                                                        if (driveSuccess) {
                                                            android.util.Log.d("RainResumen", "PDF subido a Drive")
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("RainResumen", "Error Drive: ${e.message}")
                                                }
                                            }

                                            subiendoPdf = false
                                            mensajeSubida = "PDF generado correctamente"

                                            // Abrir el PDF automáticamente
                                            verPdf(context, pdf)

                                        } else {
                                            subiendoPdf = false
                                            Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                                        }

                                    } catch (e: Exception) {
                                        subiendoPdf = false
                                        android.util.Log.e("RainResumen", "Error: ${e.message}")
                                        Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !subiendoPdf && medidas.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RainBlue,  // 👈 Azul Rain
                                disabledContainerColor = textMuted.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (subiendoPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Guardando...",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "GUARDAR Y GENERAR PDF",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    } else {
                        // ═══════════════════════════════════════════════════
                        // POST-GUARDADO: Botones de acciones
                        // ═══════════════════════════════════════════════════
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Primera fila: Enviar, PDF, Editar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // ENVIAR (Compartir)
                                OutlinedButton(
                                    onClick = {
                                        val pdf = obtenerOGenerarPdf(skipEnqueue = desdeHistorial)
                                        if (pdf != null) compartirPdf(context, pdf)
                                        else Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.5.dp, if (isDarkMode) Color.White else RainBlue)
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

                                // VER PDF
                                Button(
                                    onClick = {
                                        val pdf = obtenerOGenerarPdf(skipEnqueue = desdeHistorial)
                                        if (pdf != null) verPdf(context, pdf)
                                        else Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RainBlue  // 👈 Azul Rain
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "PDF",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // EDITAR
                                OutlinedButton(
                                    onClick = {
                                        if (desdeHistorial) onVolverAEditar()
                                        else onBack()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.5.dp, RainBlue)  // 👈 Borde azul
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

                            // Botón "Actualizar en Drive" (cuando hay PDF regenerado desde historial)
                            if (desdeHistorial && pdfRegenerado) {
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        if (pdfFile == null || !pdfFile!!.exists()) {
                                            val regenerated = obtenerOGenerarPdf(skipEnqueue = true)
                                            if (regenerated == null) {
                                                Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            pdfFile = regenerated
                                        }

                                        subiendoADrive = true
                                        scope.launch {
                                            try {
                                                val userName = SessionManager.getNombre(context)
                                                val userRole = SessionManager.getRole(context)

                                                val result = GoogleDriveRepository.uploadPdfToStructuredFolder(
                                                    context = context,
                                                    localPdfFile = pdfFile!!,
                                                    userName = userName,
                                                    userRole = userRole,
                                                    folio = folioGenerado.ifBlank { rainDraft.folio }
                                                )

                                                val uploadResult = result.getOrNull()
                                                if (uploadResult?.success == true) {
                                                    Toast.makeText(context, "PDF actualizado en Drive", Toast.LENGTH_SHORT).show()
                                                    pdfRegenerado = false
                                                    val folio = folioGenerado.ifBlank { rainDraft.folio }
                                                    if (folio.isNotBlank()) clearPendingDriveUpdate(folio)
                                                } else {
                                                    Toast.makeText(context, "Error: ${uploadResult?.error ?: "Desconocido"}", Toast.LENGTH_LONG).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error al subir: ${e.message}", Toast.LENGTH_LONG).show()
                                            } finally {
                                                subiendoADrive = false
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    enabled = !subiendoADrive,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDarkMode) Color.White else RainBlue
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (subiendoADrive) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Subiendo...",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.CloudUpload,
                                            null,
                                            tint = Color(0xFF22C55E),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "Actualizar en Drive",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
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
                        RainClienteDataRow("Nombre", rainDraft.nombre, textMuted, textPrimary, border)
                        RainClienteDataRow("Teléfono", rainDraft.telefono, textMuted, textPrimary, border)
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
                        // Box negro con TOTAL ÁREAS
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
            // CARD: ACCESORIOS ADICIONALES
            // ═══════════════════════════════════════════════════════════════
            item {
                RainStitchCard(
                    title = "ACCESORIOS",
                    icon = Icons.Default.Settings,
                    isDarkMode = isDarkMode,
                    surface = surface,
                    headerBg = headerBg,
                    border = border,
                    accentBorder = accentBorder,
                    textPrimary = textPrimary,
                    textMuted = textMuted
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // ── Controles adicionales ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Controles adicionales",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    formatMoney(RainPriceManager.getPrecio("control_adicional")) + " c/u",
                                    color = textMuted,
                                    fontSize = 12.sp
                                )
                            }
                            // Toggle No/Sí estilo Hurricane
                            RainToggleButton(
                                isActive = quiereControles,
                                onToggle = { quiereControles = !quiereControles },
                                isDarkMode = isDarkMode,
                                textMuted = textMuted
                            )
                        }

                        // Selector de cantidad controles
                        AnimatedVisibility(visible = quiereControles) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cantidad:", color = textMuted, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = if (cantidadControles > 0) cantidadControles.toString() else "",
                                    onValueChange = { input ->
                                        val num = input.filter { it.isDigit() }.take(2).toIntOrNull()
                                        cantidadControles = (num ?: 0).coerceIn(0, 99)
                                    },
                                    modifier = Modifier.width(72.dp).height(48.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(6.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isDarkMode) Color.White else RainBlue,
                                        unfocusedBorderColor = border,
                                        cursorColor = textPrimary
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = border.copy(0.3f))
                        Spacer(Modifier.height(12.dp))

                        // ── Manivelas adicionales ──
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Manivelas adicionales",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    formatMoney(RainPriceManager.getPrecio("manivela")) + " c/u",
                                    color = textMuted,
                                    fontSize = 12.sp
                                )
                            }
                            // Toggle No/Sí estilo Hurricane
                            RainToggleButton(
                                isActive = quiereManivelas,
                                onToggle = { quiereManivelas = !quiereManivelas },
                                isDarkMode = isDarkMode,
                                textMuted = textMuted
                            )
                        }

                        // Selector de cantidad manivelas
                        AnimatedVisibility(visible = quiereManivelas) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cantidad:", color = textMuted, fontSize = 13.sp)
                                OutlinedTextField(
                                    value = if (cantidadManivelas > 0) cantidadManivelas.toString() else "",
                                    onValueChange = { input ->
                                        val num = input.filter { it.isDigit() }.take(2).toIntOrNull()
                                        cantidadManivelas = (num ?: 0).coerceIn(0, 99)
                                    },
                                    modifier = Modifier.width(72.dp).height(48.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(6.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isDarkMode) Color.White else RainBlue,
                                        unfocusedBorderColor = border,
                                        cursorColor = textPrimary
                                    )
                                )
                            }
                        }

                        // Mostrar subtotal de accesorios si hay alguno seleccionado
                        if (quiereControles || quiereManivelas) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = border.copy(0.3f))
                            Spacer(Modifier.height(8.dp))

                            if (quiereControles) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Controles × $cantidadControles",
                                        color = textMuted,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        formatMoney(cantidadControles * RainPriceManager.getPrecio("control_adicional")),
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            if (quiereManivelas) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Manivelas × $cantidadManivelas",
                                        color = textMuted,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        formatMoney(cantidadManivelas * RainPriceManager.getPrecio("manivela")),
                                        color = textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Total accesorios:",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    formatMoney(costoAccesorios),
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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

// ═══════════════════════════════════════════════════════════════════════════
// COMPONENTES PRIVADOS
// ═══════════════════════════════════════════════════════════════════════════

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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge(s) de tipo de mecanismo - puede mostrar uno o ambos
                if (medida.incluyeManual) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDarkMode) Color(0xFF374151) else Color(0xFFF3F4F6),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "Manual",
                            color = textMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (medida.incluyeElectrico) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDarkMode) Color(0xFF3B82F6) else Color(0xFF2563EB),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "Eléctrico",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text("|", color = textMuted.copy(0.5f), fontSize = 10.sp)
                Text(
                    String.format("%.2f m²", medida.areaM2),
                    color = textMuted,
                    fontSize = 10.sp
                )
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
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentBorder)
            )
            Column(modifier = Modifier.weight(1f)) {
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
            Text(label, color = textMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                value,
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, false)
            )
        }
        if (showDivider) HorizontalDivider(color = border.copy(0.3f))
    }
}

/**
 * Toggle No/Sí estilo Hurricane (rectangular, blanco/negro)
 */
@Composable
private fun RainToggleButton(
    isActive: Boolean,
    onToggle: () -> Unit,
    isDarkMode: Boolean,
    textMuted: Color
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)),
        color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (!isActive) (if (isDarkMode) Color.White else RainBlue)
                        else Color.Transparent
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "No",
                    color = if (!isActive) Color.White else textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isActive) (if (isDarkMode) Color.White else RainBlue)
                        else Color.Transparent
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "Si",
                    color = if (isActive) Color.White else textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}