package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.derivedStateOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenScreen(
    cotizacion: Cotizacion,
    desdeHistorial: Boolean,
    huboEdicionMedidas: Boolean = false,
    isDarkMode: Boolean = false,
    onVolverAInicio: () -> Unit,
    onVolverAEditar: () -> Unit,
    onVolverAHistorial: () -> Unit,
    onCotizacionActualizada: (Cotizacion) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var guardado by rememberSaveable { mutableStateOf(desdeHistorial) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var folioGenerado by rememberSaveable { mutableStateOf(cotizacion.folio) }
    var subiendoPdf by remember { mutableStateOf(false) }
    var mensajeSubida by remember { mutableStateOf<String?>(null) }

    // Estados para detectar cambios desde historial
    var pdfRegenerado by rememberSaveable { mutableStateOf(false) }
    var subiendoADrive by remember { mutableStateOf(false) }

    // CRITICO: Resetear estado stale cuando llega una cotizacion nueva
    // rememberSaveable persiste entre navegaciones, asi que si el usuario
    // salio de ResumenScreen (guardado=true) y vuelve con nueva cotizacion,
    // guardado seguiria en true sin este reset
    LaunchedEffect(cotizacion.folio, desdeHistorial) {
        if (!desdeHistorial && cotizacion.folio.isBlank()) {
            // Cotizacion completamente nueva: resetear todo
            guardado = false
            pdfRegenerado = false
            folioGenerado = ""
            pdfFile = null
        }
    }

    // Persistencia del estado "pendiente de actualizar en Drive" por folio
    val drivePrefs = remember { context.getSharedPreferences("drive_pending_prefs", android.content.Context.MODE_PRIVATE) }

    fun hasPendingDriveUpdate(folio: String): Boolean {
        return drivePrefs.getBoolean("pending_drive_$folio", false)
    }
    fun markPendingDriveUpdate(folio: String) {
        drivePrefs.edit().putBoolean("pending_drive_$folio", true).apply()
    }
    fun clearPendingDriveUpdate(folio: String) {
        drivePrefs.edit().remove("pending_drive_$folio").apply()
    }

    // Si viene del historial y este folio tiene una actualizacion de Drive pendiente, mostrar boton
    LaunchedEffect(cotizacion.folio, desdeHistorial) {
        if (desdeHistorial && cotizacion.folio.isNotBlank() && hasPendingDriveUpdate(cotizacion.folio)) {
            pdfRegenerado = true
        }
    }

    var ventanasFueronEditadas by rememberSaveable { mutableStateOf(huboEdicionMedidas) }

    // Si viene con huboEdicionMedidas = true, invalidar PDF (AMBOS flujos)
    LaunchedEffect(huboEdicionMedidas) {
        if (huboEdicionMedidas) {
            ventanasFueronEditadas = true
            pdfFile = null
            pdfRegenerado = false
        }
    }

    var hs875Selected by rememberSaveable {
        mutableStateOf(cotizacion.productos.contains(TipoProducto.HS875))
    }
    var hs1250Selected by rememberSaveable {
        mutableStateOf(cotizacion.productos.contains(TipoProducto.HS1250))
    }
    var hs1500Selected by rememberSaveable {
        mutableStateOf(cotizacion.productos.contains(TipoProducto.HS1500))
    }

    // Guardar valores originales para detectar cambios - usar key para actualizar
    val productosOriginales = remember(cotizacion.id, cotizacion.updatedAt) { cotizacion.productos.toSet() }
    val descuentoOriginal875 = remember(cotizacion.id, cotizacion.updatedAt) { cotizacion.descuentoHS875 }
    val descuentoOriginal1250 = remember(cotizacion.id, cotizacion.updatedAt) { cotizacion.descuentoHS1250 }
    val descuentoOriginal1500 = remember(cotizacion.id, cotizacion.updatedAt) { cotizacion.descuentoHS1500 }

    var aplicaDescuento by rememberSaveable {
        mutableStateOf(
            if (desdeHistorial) (cotizacion.descuentoHS875 > 0 || cotizacion.descuentoHS1250 > 0 || cotizacion.descuentoHS1500 > 0)
            else false
        )
    }

    var precioFinalHS875 by rememberSaveable {
        mutableStateOf(
            if (desdeHistorial && cotizacion.descuentoHS875 > 0) {
                val precioVenta = TipoProducto.HS875.getPrecioVenta()
                String.format(Locale.US, "%.2f", precioVenta - cotizacion.descuentoHS875)
            } else ""
        )
    }

    var precioFinalHS1250 by rememberSaveable {
        mutableStateOf(
            if (desdeHistorial && cotizacion.descuentoHS1250 > 0) {
                val precioVenta = TipoProducto.HS1250.getPrecioVenta()
                String.format(Locale.US, "%.2f", precioVenta - cotizacion.descuentoHS1250)
            } else ""
        )
    }

    var precioFinalHS1500 by rememberSaveable {
        mutableStateOf(
            if (desdeHistorial && cotizacion.descuentoHS1500 > 0) {
                val precioVenta = TipoProducto.HS1500.getPrecioVenta()
                String.format(Locale.US, "%.2f", precioVenta - cotizacion.descuentoHS1500)
            } else ""
        )
    }

    LaunchedEffect(hs875Selected, hs1250Selected, hs1500Selected, aplicaDescuento, precioFinalHS875, precioFinalHS1250, precioFinalHS1500) {
        pdfFile = null
        pdfRegenerado = false // Resetear estado de PDF regenerado
    }

    // Detectar cambios (productos, descuentos, ventanas) - AMBOS flujos
    val hayCambiosSinGuardar by remember(ventanasFueronEditadas) {
        derivedStateOf {
            if (!guardado) return@derivedStateOf false
            if (ventanasFueronEditadas) return@derivedStateOf true

            val productosActuales = mutableSetOf<TipoProducto>().apply {
                if (hs875Selected) add(TipoProducto.HS875)
                if (hs1250Selected) add(TipoProducto.HS1250)
                if (hs1500Selected) add(TipoProducto.HS1500)
            }

            // Comparar productos
            val productosChanged = productosActuales != productosOriginales

            // Comparar descuentos (si aplica descuento)
            val descuentosChanged = if (aplicaDescuento) {
                val desc875Actual = precioFinalHS875.toDoubleOrNull()?.let {
                    TipoProducto.HS875.getPrecioVenta() - it
                } ?: 0.0
                val desc1250Actual = precioFinalHS1250.toDoubleOrNull()?.let {
                    TipoProducto.HS1250.getPrecioVenta() - it
                } ?: 0.0
                val desc1500Actual = precioFinalHS1500.toDoubleOrNull()?.let {
                    TipoProducto.HS1500.getPrecioVenta() - it
                } ?: 0.0

                kotlin.math.abs(desc875Actual - descuentoOriginal875) > 0.01 ||
                        kotlin.math.abs(desc1250Actual - descuentoOriginal1250) > 0.01 ||
                        kotlin.math.abs(desc1500Actual - descuentoOriginal1500) > 0.01
            } else {
                descuentoOriginal875 > 0 || descuentoOriginal1250 > 0 || descuentoOriginal1500 > 0
            }

            // Incluir si las ventanas fueron editadas
            productosChanged || descuentosChanged || ventanasFueronEditadas
        }
    }

    var mostrarAdvertenciaDescuento by remember { mutableStateOf(false) }
    var productoAdvertencia by remember { mutableStateOf("") }

    // Tab seleccionado para el card unificado (0 = Tipo de Sistema, 1 = Descuentos)
    var selectedConfigTab by rememberSaveable { mutableIntStateOf(0) }

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF374151) else Color(0xFFE5E7EB)
    val headerBg = if (isDarkMode) Color(0xFF1F1F1F) else Color(0xFFF9FAFB)
    val accentBorder = if (isDarkMode) Color(0xFF6B7280) else Color.Black

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    val titleSize = when {
        screenWidth < 360 -> 14.sp
        screenWidth < 400 -> 16.sp
        else -> 18.sp
    }

    val bodySize = when {
        screenWidth < 360 -> 12.sp
        screenWidth < 400 -> 13.sp
        else -> 14.sp
    }

    val smallSize = when {
        screenWidth < 360 -> 10.sp
        screenWidth < 400 -> 11.sp
        else -> 12.sp
    }

    val tinySize = when {
        screenWidth < 360 -> 8.sp
        screenWidth < 400 -> 9.sp
        else -> 10.sp
    }

    val productosSeleccionados by remember {
        derivedStateOf {
            mutableListOf<TipoProducto>().apply {
                if (hs875Selected) add(TipoProducto.HS875)
                if (hs1250Selected) add(TipoProducto.HS1250)
                if (hs1500Selected) add(TipoProducto.HS1500)
            }.sortedBy { it.getPrecioVenta() }
        }
    }

    fun formatMoney(amount: Double): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)}"
    }

    fun formatArea(area: Double): String {
        return if (area >= 1000) {
            val format = NumberFormat.getNumberInstance(Locale.US).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
            }
            "${format.format(area)} m²"
        } else {
            String.format("%.2f m²", area)
        }
    }

    fun getDescuentoDesdePrecioFinal(producto: TipoProducto, textoPrecioFinal: String): Double {
        val precioFinalDeseado = textoPrecioFinal.replace(",", ".").toDoubleOrNull() ?: 0.0
        if (precioFinalDeseado <= 0.0) return 0.0

        val precioVenta = producto.getPrecioVenta()
        val precioBase = producto.getPrecioBase()

        val precioFinalValidado = precioFinalDeseado.coerceIn(precioBase, precioVenta)
        val descuentoCalculado = precioVenta - precioFinalValidado

        val maxDescuento = producto.getMaxDescuento()
        return descuentoCalculado.coerceIn(0.0, maxDescuento)
    }

    fun filtrarInput(input: String): String {
        var filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
            .replace(",", ".")

        val firstDotIndex = filtered.indexOf('.')
        if (firstDotIndex != -1) {
            val beforeDot = filtered.substring(0, firstDotIndex + 1)
            val afterDot = filtered.substring(firstDotIndex + 1).replace(".", "")
            filtered = beforeDot + afterDot
        }

        // Limitar a 2 decimales
        if (filtered.contains(".")) {
            val parts = filtered.split(".")
            if (parts.size == 2 && parts[1].length > 2) {
                filtered = parts[0] + "." + parts[1].take(2)
            }
        }

        return filtered
    }

    // Valida y corrige el valor cuando pierde el foco
    fun validarAlPerderFoco(valor: String, producto: TipoProducto): String {
        if (valor.isEmpty() || valor == ".") {
            mostrarAdvertenciaDescuento = false
            return ""
        }

        val numero = valor.toDoubleOrNull()
        if (numero == null) {
            mostrarAdvertenciaDescuento = false
            return ""
        }

        val precioVenta = producto.getPrecioVenta()
        val precioBase = producto.getPrecioBase()

        return when {
            numero > precioVenta -> {
                mostrarAdvertenciaDescuento = false
                String.format(Locale.US, "%.2f", precioVenta)
            }
            numero < precioBase -> {
                mostrarAdvertenciaDescuento = true
                productoAdvertencia = producto.etiquetaCorta
                String.format(Locale.US, "%.2f", precioBase)
            }
            else -> {
                mostrarAdvertenciaDescuento = false
                valor
            }
        }
    }

    fun calcularTotal(producto: TipoProducto): Double {
        val precioFinalTexto = when (producto) {
            TipoProducto.HS875 -> precioFinalHS875
            TipoProducto.HS1250 -> precioFinalHS1250
            TipoProducto.HS1500 -> precioFinalHS1500
            else -> ""
        }

        val descuento = if (aplicaDescuento) getDescuentoDesdePrecioFinal(producto, precioFinalTexto) else 0.0

        val precioFinal = if (aplicaDescuento && precioFinalTexto.isNotBlank()) {
            (producto.getPrecioVenta() - descuento).coerceAtLeast(producto.getPrecioBase())
        } else {
            producto.getPrecioVenta()
        }

        return cotizacion.ventanas.sumOf { it.areaM2 * precioFinal }
    }

    fun obtenerOGenerarPdf(skipEnqueue: Boolean = false): File? {
        if (pdfFile != null && pdfFile!!.exists()) {
            return pdfFile
        }

        // SIEMPRE usar los productos seleccionados actuales
        val cotizacionParaPdf = cotizacion.copy(
            productos = productosSeleccionados.ifEmpty { cotizacion.productos },
            producto = productosSeleccionados.firstOrNull() ?: cotizacion.producto,
            descuentoHS875 = if (aplicaDescuento) getDescuentoDesdePrecioFinal(
                TipoProducto.HS875,
                precioFinalHS875
            ) else cotizacion.descuentoHS875,

            descuentoHS1250 = if (aplicaDescuento) getDescuentoDesdePrecioFinal(
                TipoProducto.HS1250,
                precioFinalHS1250
            ) else cotizacion.descuentoHS1250,

            descuentoHS1500 = if (aplicaDescuento) getDescuentoDesdePrecioFinal(
                TipoProducto.HS1500,
                precioFinalHS1500
            ) else cotizacion.descuentoHS1500
        )

        val pdf = generarPdfCotizacion(context, cotizacionParaPdf, skipEnqueue)
        if (pdf != null) {
            pdfFile = pdf
        }
        return pdf
    }

    // Dialog de confirmacion para salir (solo modo en vivo)
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        when {
            desdeHistorial -> onVolverAHistorial()
            else -> showExitDialog = true
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
                    when {
                        desdeHistorial -> onVolverAHistorial()
                        else -> showExitDialog = true
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (productosSeleccionados.isNotEmpty()) {
                        productosSeleccionados.forEach { producto ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "TOTAL ${producto.etiquetaCorta}",
                                    color = textMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    formatMoney(calcularTotal(producto)),
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (!guardado) {
                        Button(
                            onClick = {
                                if (productosSeleccionados.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "Selecciona al menos un sistema",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }

                                val especialista =
                                    cotizacion.especialista.ifBlank { "Especialista" }
                                val folioFinal = if (folioGenerado.isBlank()) {
                                    FolioManager.nextFolioForEspecialista(context, especialista)
                                } else {
                                    folioGenerado
                                }
                                folioGenerado = folioFinal

                                val cotizacionFinal = cotizacion.copy(
                                    folio = folioFinal,
                                    productos = productosSeleccionados,
                                    producto = productosSeleccionados.first(),
                                    descuentoHS875 = if (aplicaDescuento) getDescuentoDesdePrecioFinal(
                                        TipoProducto.HS875,
                                        precioFinalHS875
                                    ) else cotizacion.descuentoHS875,

                                    descuentoHS1250 = if (aplicaDescuento) getDescuentoDesdePrecioFinal(
                                        TipoProducto.HS1250,
                                        precioFinalHS1250
                                    ) else cotizacion.descuentoHS1250,

                                    descuentoHS1500 = if (aplicaDescuento) getDescuentoDesdePrecioFinal(
                                        TipoProducto.HS1500,
                                        precioFinalHS1500
                                    ) else cotizacion.descuentoHS1500
                                )

                                guardarCotizacionLocal(context, cotizacionFinal)
                                // Propagar cotizacion con folio a AppNavigation
                                onCotizacionActualizada(cotizacionFinal)

                                subiendoPdf = true
                                val pdf = AutoUploadManager.generarYSubirPdf(
                                    context = context,
                                    cotizacion = cotizacionFinal,
                                    scope = scope,
                                    onPdfGenerated = { file ->
                                        pdfFile = file
                                        guardado = true
                                    },
                                    onUploadComplete = { success, error ->
                                        subiendoPdf = false
                                        mensajeSubida = if (success) {
                                            "PDF subido correctamente"
                                        } else if (error?.contains("Sin conexión") == true) {
                                            "Guardado localmente - Se subira cuando haya internet"
                                        } else {
                                            "Guardado - Error al subir: quedara pendiente"
                                        }
                                    }
                                )

                                scope.launch {
                                    try {
                                        val sistemaSeleccionado =
                                            cotizacionFinal.productos.firstOrNull()?.name ?: "HS875"
                                        val userId = SessionManager.getUserId(context)
                                        val userName = SessionManager.getNombre(context)

                                        android.util.Log.d(
                                            "ResumenScreen",
                                            "════════════════════════════════════════"
                                        )
                                        android.util.Log.d(
                                            "ResumenScreen",
                                            "📝 Creando registro de instalador..."
                                        )
                                        android.util.Log.d(
                                            "ResumenScreen",
                                            "📝 Folio: ${cotizacionFinal.folio}"
                                        )
                                        android.util.Log.d(
                                            "ResumenScreen",
                                            "📝 Sistema: $sistemaSeleccionado"
                                        )
                                        android.util.Log.d(
                                            "ResumenScreen",
                                            "📝 Ventanas: ${cotizacionFinal.ventanas.size}"
                                        )
                                        android.util.Log.d(
                                            "ResumenScreen",
                                            "════════════════════════════════════════"

                                        )

                                        val result =
                                            InstaladorRepository.crearRegistroDesdeCotizacionCompleto(
                                                cotizacion = cotizacionFinal,
                                                sistemaSeleccionado = sistemaSeleccionado,
                                                especialistaId = userId,
                                                especialistaNombre = userName
                                            )

                                        if (result.isSuccess) {
                                            android.util.Log.d(
                                                "ResumenScreen",
                                                "✅ Registro de instalador creado exitosamente"
                                            )
                                        } else {
                                            android.util.Log.e(
                                                "ResumenScreen",
                                                "❌ Error: ${result.exceptionOrNull()?.message}"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e(
                                            "ResumenScreen",
                                            "❌ Error creando registro instalador: ${e.message}",
                                            e
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color.White else Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !subiendoPdf
                        ) {
                            if (subiendoPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = if (isDarkMode) Color.Black else Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Guardando...",
                                    color = if (isDarkMode) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = if (isDarkMode) Color.Black else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "GUARDAR Y GENERAR PDF",
                                    color = if (isDarkMode) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    } else {

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Regenerar PDF: AMBOS flujos cuando hay cambios
                            if (hayCambiosSinGuardar && !pdfRegenerado) {
                                Button(
                                    onClick = {
                                        // skipEnqueue = true para NO crear PendingUpload
                                        val pdf = obtenerOGenerarPdf(skipEnqueue = true)
                                        if (pdf != null) {
                                            pdfFile = pdf
                                            pdfRegenerado = true
                                            ventanasFueronEditadas = false

                                            val cotizacionActualizada = cotizacion.copy(
                                                productos = productosSeleccionados,
                                                producto = productosSeleccionados.firstOrNull() ?: cotizacion.producto,
                                                descuentoHS875 = if (aplicaDescuento) getDescuentoDesdePrecioFinal(TipoProducto.HS875, precioFinalHS875) else 0.0,
                                                descuentoHS1250 = if (aplicaDescuento) getDescuentoDesdePrecioFinal(TipoProducto.HS1250, precioFinalHS1250) else 0.0,
                                                descuentoHS1500 = if (aplicaDescuento) getDescuentoDesdePrecioFinal(TipoProducto.HS1500, precioFinalHS1500) else 0.0,
                                                updatedAt = System.currentTimeMillis()
                                            )
                                            guardarCotizacionLocal(context, cotizacionActualizada, esActualizacion = true)
                                            onCotizacionActualizada(cotizacionActualizada)

                                            // INSERT nueva version en Supabase
                                            scope.launch {
                                                try {
                                                    AutoUploadManager.sincronizarCotizacionEditada(
                                                        context = context,
                                                        cotizacion = cotizacionActualizada,
                                                        pdfFile = pdf
                                                    )
                                                    android.util.Log.d("ResumenScreen", "Version sincronizada a Supabase")

                                                    // Drive: auto en vivo, manual en historial
                                                    if (!desdeHistorial) {
                                                        try {
                                                            val userName = SessionManager.getNombre(context)
                                                            val userRole = SessionManager.getRole(context)
                                                            if (userName.isNotBlank() && DriveAuthManager.isAuthenticated(context)) {
                                                                DriveUploadManager.uploadPdfToDriveAuto(
                                                                    context = context,
                                                                    pdfFile = pdf,
                                                                    userName = userName,
                                                                    userRole = userRole,
                                                                    folio = cotizacionActualizada.folio
                                                                )
                                                                android.util.Log.d("ResumenScreen", "PDF actualizado en Drive")
                                                            }
                                                        } catch (driveErr: Exception) {
                                                            android.util.Log.e("ResumenScreen", "Error Drive: ${driveErr.message}")
                                                        }
                                                    } else {
                                                        if (cotizacionActualizada.folio.isNotBlank()) {
                                                            markPendingDriveUpdate(cotizacionActualizada.folio)
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("ResumenScreen", "Error sincronizando: ${e.message}")
                                                }
                                            }

                                            Toast.makeText(context, "Nueva versión guardada", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Error al regenerar PDF",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDarkMode) Color.White else Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        null,
                                        tint = if (isDarkMode) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Regenerar PDF",
                                        color = if (isDarkMode) Color.Black else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                // Primera fila: Enviar, PDF, Editar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val pdf = obtenerOGenerarPdf(skipEnqueue = desdeHistorial)
                                            if (pdf != null) compartirPdf(context, pdf)
                                            else Toast.makeText(
                                                context,
                                                "Error al generar PDF",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (isDarkMode) Color.White else Color.Black
                                        )
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
                                            val pdf = obtenerOGenerarPdf(skipEnqueue = desdeHistorial)
                                            if (pdf != null) verPdf(context, pdf)
                                            else Toast.makeText(
                                                context,
                                                "Error al generar PDF",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color.White else Color.Black),
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
                                        onClick = onVolverAEditar,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (isDarkMode) Color.White else Color.Black
                                        )
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

                                if (desdeHistorial && pdfRegenerado) {
                                    Spacer(Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            // Si el PDF no existe en memoria, regenerarlo
                                            if (pdfFile == null || !pdfFile!!.exists()) {
                                                val regenerated = obtenerOGenerarPdf(skipEnqueue = true)
                                                if (regenerated == null) {
                                                    Toast.makeText(
                                                        context,
                                                        "Error al generar PDF",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
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
                                                        folio = cotizacion.folio
                                                    )

                                                    val uploadResult = result.getOrNull()
                                                    if (uploadResult?.success == true) {
                                                        Toast.makeText(
                                                            context,
                                                            "PDF actualizado en Drive",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        pdfRegenerado = false
                                                        // Limpiar la marca persistente de Drive pendiente
                                                        if (cotizacion.folio.isNotBlank()) {
                                                            clearPendingDriveUpdate(cotizacion.folio)
                                                        }
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Error: ${uploadResult?.error ?: "Desconocido"}",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(
                                                        context,
                                                        "Error al subir: ${e.message}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
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
                                            containerColor = if (isDarkMode) Color.White else Color.Black
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (subiendoADrive) {
                                            CircularProgressIndicator(
                                                color = if (isDarkMode) Color.Black else Color.White,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Subiendo...",
                                                color = if (isDarkMode) Color.Black else Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.CloudUpload,
                                                null,
                                                tint = Color(0xFF22C55E), // Verde para el icono de Drive
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Actualizar en Drive",
                                                color = if (isDarkMode) Color.Black else Color.White,
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
            item {
                StitchCard(
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
                        ClienteDataRow(
                            "Nombre",
                            cotizacion.clienteNombre,
                            textMuted,
                            textPrimary,
                            border
                        )
                        ClienteDataRow(
                            "Telefono",
                            cotizacion.clienteTelefono,
                            textMuted,
                            textPrimary,
                            border
                        )
                        ClienteDataRow(
                            "Ciudad",
                            cotizacion.ciudad.ifBlank {
                                cotizacion.ubicacion.split(",").firstOrNull() ?: ""
                            },
                            textMuted,
                            textPrimary,
                            border,
                            showDivider = false
                        )
                    }
                }
            }

            item {
                StitchCard(
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDarkMode) Color(0xFF1F1F1F) else Color.Black)
                                .padding(vertical = 24.dp), contentAlignment = Alignment.Center
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

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(headerBg)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "APERTURAS (${cotizacion.ventanas.size})",
                                color = textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                cotizacion.ventanas.forEachIndexed { index, ventana ->
                                    AperturaItemSimple(
                                        index + 1,
                                        ventana,
                                        isDarkMode,
                                        textPrimary,
                                        textMuted,
                                        border
                                    )
                                    if (index < cotizacion.ventanas.lastIndex) HorizontalDivider(
                                        color = border.copy(0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ========== CARD UNIFICADO CON TABS: TIPO DE SISTEMA + DESCUENTOS ==========
            item {
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
                            // TABS: TIPO DE SISTEMA | DESCUENTOS
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(headerBg)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Tab: TIPO DE SISTEMA
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selectedConfigTab == 0) {
                                                if (isDarkMode) Color.White else Color.Black
                                            } else {
                                                if (isDarkMode) Color(0xFF27272A) else Color(
                                                    0xFFF3F4F6
                                                )
                                            }
                                        )
                                        .clickable { selectedConfigTab = 0 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "TIPO DE SISTEMA",
                                        color = if (selectedConfigTab == 0) {
                                            if (isDarkMode) Color.Black else Color.White
                                        } else {
                                            textMuted
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedConfigTab == 0) FontWeight.Bold else FontWeight.Medium,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                // Tab: DESCUENTOS
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selectedConfigTab == 1) {
                                                if (isDarkMode) Color.White else Color.Black
                                            } else {
                                                if (isDarkMode) Color(0xFF27272A) else Color(
                                                    0xFFF3F4F6
                                                )
                                            }
                                        )
                                        .clickable { selectedConfigTab = 1 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "PRECIO DE VENTA",
                                        color = if (selectedConfigTab == 1) {
                                            if (isDarkMode) Color.Black else Color.White
                                        } else {
                                            textMuted
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedConfigTab == 1) FontWeight.Bold else FontWeight.Medium,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            HorizontalDivider(color = border.copy(0.5f))

                            // CONTENIDO SEGN TAB SELECCIONADO
                            AnimatedContent(
                                targetState = selectedConfigTab,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "config_tabs"
                            ) { tabIndex ->
                                when (tabIndex) {
                                    0 -> {
                                        // TAB: TIPO DE SISTEMA
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                SystemCard(
                                                    "HS-875",
                                                    hs875Selected,
                                                    { hs875Selected = !hs875Selected },
                                                    isDarkMode,
                                                    Modifier.weight(1f)
                                                )
                                                SystemCard(
                                                    "HS-1250",
                                                    hs1250Selected,
                                                    { hs1250Selected = !hs1250Selected },
                                                    isDarkMode,
                                                    Modifier.weight(1f)
                                                )
                                                SystemCard(
                                                    "HS-1500",
                                                    hs1500Selected,
                                                    { hs1500Selected = !hs1500Selected },
                                                    isDarkMode,
                                                    Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    1 -> {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Aplicar precio personalizado",
                                                    color = textPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                DescuentoToggleButton(
                                                    aplicaDescuento,
                                                    { aplicaDescuento = !aplicaDescuento },
                                                    isDarkMode,
                                                    textMuted,
                                                    border
                                                )
                                            }

                                            Spacer(Modifier.height(16.dp))

                                            AnimatedVisibility(
                                                visible = aplicaDescuento,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically()
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                    AnimatedVisibility(
                                                        visible = mostrarAdvertenciaDescuento,
                                                        enter = fadeIn() + expandVertically(),
                                                        exit = fadeOut() + shrinkVertically()
                                                    ) {
                                                        Surface(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            color = Color(0xFFFEF2F2),
                                                            shape = RoundedCornerShape(8.dp),
                                                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(12.dp),
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Warning,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFFDC2626),
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                                Text(
                                                                    "No se puede aplicar un descuento mayor al permitido para $productoAdvertencia",
                                                                    color = Color(0xFFDC2626),
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            }
                                                        }
                                                    }

                                                    if (hs875Selected) DiscountInputField(
                                                        label = "HS-875 (Precio final/m²)",
                                                        value = precioFinalHS875,
                                                        onValueChange = { precioFinalHS875 = filtrarInput(it) },
                                                        onFocusLost = { precioFinalHS875 = validarAlPerderFoco(precioFinalHS875, TipoProducto.HS875) },
                                                        precioBase = TipoProducto.HS875.getPrecioBase(),
                                                        precioVenta = TipoProducto.HS875.getPrecioVenta(),
                                                        isDarkMode = isDarkMode,
                                                        textPrimary = textPrimary,
                                                        border = border
                                                    )
                                                    if (hs1250Selected) DiscountInputField(
                                                        label = "HS-1250 (Precio final/m²)",
                                                        value = precioFinalHS1250,
                                                        onValueChange = { precioFinalHS1250 = filtrarInput(it) },
                                                        onFocusLost = { precioFinalHS1250 = validarAlPerderFoco(precioFinalHS1250, TipoProducto.HS1250) },
                                                        precioBase = TipoProducto.HS1250.getPrecioBase(),
                                                        precioVenta = TipoProducto.HS1250.getPrecioVenta(),
                                                        isDarkMode = isDarkMode,
                                                        textPrimary = textPrimary,
                                                        border = border
                                                    )
                                                    if (hs1500Selected) DiscountInputField(
                                                        label = "HS-1500 (Precio final/m²)",
                                                        value = precioFinalHS1500,
                                                        onValueChange = { precioFinalHS1500 = filtrarInput(it) },
                                                        onFocusLost = { precioFinalHS1500 = validarAlPerderFoco(precioFinalHS1500, TipoProducto.HS1500) },
                                                        precioBase = TipoProducto.HS1500.getPrecioBase(),
                                                        precioVenta = TipoProducto.HS1500.getPrecioVenta(),
                                                        isDarkMode = isDarkMode,
                                                        textPrimary = textPrimary,
                                                        border = border
                                                    )
                                                }
                                            }

                                            if (!aplicaDescuento) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "Usando precio de venta estándar",
                                                        color = textMuted,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

// Toggle de Descuentos mejorado
@Composable
private fun DescuentoToggleButton(
    aplicaDescuento: Boolean,
    onToggle: () -> Unit,
    isDarkMode: Boolean,
    textMuted: Color,
    border: Color
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
                    .background(if (!aplicaDescuento) (if (isDarkMode) Color.White else Color.Black) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "No",
                    color = if (!aplicaDescuento) (if (isDarkMode) Color.Black else Color.White) else textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (aplicaDescuento) (if (isDarkMode) Color.White else Color.Black) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    "Si",
                    color = if (aplicaDescuento) (if (isDarkMode) Color.Black else Color.White) else textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// AperturaItem SIMPLIFICADO
@Composable
private fun AperturaItemSimple(
    index: Int,
    ventana: Ventana,
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
                    ventana.descripcion,
                    color = textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${String.format("%.2f", ventana.alto)}m x ${
                        String.format(
                            "%.2f",
                            ventana.ancho
                        )
                    }m", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .border(1.dp, border, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        ventana.tipoMontaje,
                        color = textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text("|", color = textMuted.copy(0.5f), fontSize = 10.sp)
                Text(String.format("%.2f m²", ventana.areaM2), color = textMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ADECUACIONES:",
                    color = textMuted.copy(0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                val tieneAdecuacion = ventana.adecuacion != "No" && ventana.adecuacion.isNotBlank()
                Box(
                    modifier = Modifier
                        .background(
                            if (tieneAdecuacion) (if (isDarkMode) Color.White else Color.Black) else Color.Transparent,
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp,
                            if (tieneAdecuacion) Color.Transparent else border.copy(0.3f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (tieneAdecuacion) "Si, ${ventana.adecuacion}" else "No",
                        color = if (tieneAdecuacion) (if (isDarkMode) Color.Black else Color.White) else textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StitchCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDarkMode: Boolean,
    surface: Color,
    headerBg: Color,
    border: Color,
    accentBorder: Color,
    textPrimary: Color,
    textMuted: Color,
    badge: String? = null,
    headerContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surface,
        shape = RoundedCornerShape(0.dp),
        shadowElevation = 2.dp
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accentBorder))
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        badge?.let {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.Black,
                                        RoundedCornerShape(4.dp)
                                    )
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
                        headerContent?.invoke()
                    }
                }
                HorizontalDivider(color = border.copy(0.5f))
                content()
            }
        }
    }
}

@Composable
private fun ClienteDataRow(
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

@Composable
private fun SystemCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        selected && isDarkMode -> Color.White; selected -> Color.Black; isDarkMode -> Color(
            0xFF18181B
        ); else -> Color.White
    }
    val contentColor = when {
        selected && isDarkMode -> Color.Black; selected -> Color.White; isDarkMode -> Color(
            0xFF71717A
        ); else -> Color(0xFF6B7280)
    }
    val borderColor = when {
        selected -> Color.Transparent; isDarkMode -> Color(0xFF374151); else -> Color(0xFFE5E7EB)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (!selected) BorderStroke(1.dp, borderColor) else null,
        shadowElevation = if (selected) 4.dp else 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color.Black else Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isDarkMode) Color.White else Color.Black,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}



@Composable
private fun DiscountInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    precioBase: Double,
    precioVenta: Double,
    isDarkMode: Boolean,
    textPrimary: Color,
    border: Color
) {
    var hasFocus by remember { mutableStateOf(false) }

    Column {
        Text(
            label,
            color = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        // Mostrar rango permitido
        Text(
            "Rango: $${String.format("%.0f", precioBase)} - $${String.format("%.0f", precioVenta)}",
            color = if (isDarkMode) Color(0xFF6B7280) else Color(0xFF9CA3AF),
            fontSize = 9.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    String.format("%.0f", precioVenta),
                    color = if (isDarkMode) Color(0xFF6B7280) else Color(0xFF9CA3AF)
                )
            },
            leadingIcon = {
                Text(
                    "$",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (hasFocus && !focusState.isFocused) {
                        onFocusLost()
                    }
                    hasFocus = focusState.isFocused
                },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDarkMode) Color(0xFF1F1F1F) else Color.White,
                unfocusedContainerColor = if (isDarkMode) Color(0xFF1F1F1F) else Color.White,
                focusedBorderColor = if (isDarkMode) Color.White else Color.Black,
                unfocusedBorderColor = border,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
    }
}