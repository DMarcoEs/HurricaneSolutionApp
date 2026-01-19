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
import androidx.compose.ui.res.painterResource
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.api.ApiException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenScreen(
    cotizacion: Cotizacion,
    desdeHistorial: Boolean,
    isDarkMode: Boolean = false,
    onVolverAInicio: () -> Unit,
    onVolverAEditar: () -> Unit,
    onVolverAHistorial: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var guardado by rememberSaveable { mutableStateOf(desdeHistorial) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var folioGenerado by rememberSaveable { mutableStateOf(cotizacion.folio) }
    var subiendoPdf by remember { mutableStateOf(false) }
    var mensajeSubida by remember { mutableStateOf<String?>(null) }
    var subiendoDrive by remember { mutableStateOf(false) }
    var driveUploadSuccess by remember { mutableStateOf<Boolean?>(null) }
    var driveErrorMessage by remember { mutableStateOf<String?>(null) }


    fun uploadToDrive() {
        if (pdfFile == null || folioGenerado.isBlank()) {
            Toast.makeText(context, "No hay PDF para subir", Toast.LENGTH_SHORT).show()
            return
        }

        val userName = SessionManager.getNombre(context)
        val userRole = SessionManager.getRole(context)

        if (userName.isBlank() || userRole.isBlank()) {
            Toast.makeText(context, "Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        subiendoDrive = true
        driveUploadSuccess = null
        driveErrorMessage = null

        scope.launch {
            try {
                val result = GoogleDriveRepository.uploadPdfToStructuredFolder(
                    context = context,
                    localPdfFile = pdfFile!!,
                    userName = userName,
                    userRole = userRole
                )

                if (result.isSuccess) {
                    val uploadResult = result.getOrNull()!!

                    if (uploadResult.success) {
                        driveUploadSuccess = true
                        Toast.makeText(
                            context,
                            "PDF subido a Drive: ${uploadResult.folderPath}",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        driveUploadSuccess = false
                        driveErrorMessage = uploadResult.error
                        Toast.makeText(
                            context,
                            "Error: ${uploadResult.error}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    driveUploadSuccess = false
                    driveErrorMessage = result.exceptionOrNull()?.message
                    Toast.makeText(
                        context,
                        "Error al subir: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                driveUploadSuccess = false
                driveErrorMessage = e.message
                Toast.makeText(
                    context,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                subiendoDrive = false
            }
        }
    }


    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            val resultData = result.data
            val signInResult = DriveAuthManager.handleSignInResult(resultData)

            if (signInResult.isSuccess) {
                Toast.makeText(context, "Autenticado con Google", Toast.LENGTH_SHORT).show()
                uploadToDrive()
            } else {
                Toast.makeText(
                    context,
                    "Error de autenticación: ${signInResult.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    var hs875Selected by rememberSaveable {
        mutableStateOf(if (desdeHistorial) cotizacion.productos.contains(TipoProducto.HS875) else true)
    }
    var hs1250Selected by rememberSaveable {
        mutableStateOf(if (desdeHistorial) cotizacion.productos.contains(TipoProducto.HS1250) else false)
    }
    var hs1500Selected by rememberSaveable {
        mutableStateOf(if (desdeHistorial) cotizacion.productos.contains(TipoProducto.HS1500) else false)
    }

    var aplicaDescuento by rememberSaveable {
        mutableStateOf(if (desdeHistorial) (cotizacion.descuentoHS875 > 0 || cotizacion.descuentoHS1250 > 0 || cotizacion.descuentoHS1500 > 0) else false)
    }
    var descuentoHS875 by rememberSaveable {
        mutableStateOf(if (desdeHistorial && cotizacion.descuentoHS875 > 0) cotizacion.descuentoHS875.toInt().toString() else "")
    }
    var descuentoHS1250 by rememberSaveable {
        mutableStateOf(if (desdeHistorial && cotizacion.descuentoHS1250 > 0) cotizacion.descuentoHS1250.toInt().toString() else "")
    }
    var descuentoHS1500 by rememberSaveable {
        mutableStateOf(if (desdeHistorial && cotizacion.descuentoHS1500 > 0) cotizacion.descuentoHS1500.toInt().toString() else "")
    }

    // Tab seleccionado para el card unificado (0 = Tipo de Sistema, 1 = Descuentos)
    var selectedConfigTab by rememberSaveable { mutableIntStateOf(0) }

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val border = if (isDarkMode) Color(0xFF374151) else Color(0xFFE5E7EB)
    val headerBg = if (isDarkMode) Color(0xFF1F1F1F) else Color(0xFFF9FAFB)
    val accentBorder = if (isDarkMode) Color(0xFF6B7280) else Color.Black

    val productosSeleccionados = remember(hs875Selected, hs1250Selected, hs1500Selected) {
        mutableListOf<TipoProducto>().apply {
            if (hs875Selected) add(TipoProducto.HS875)
            if (hs1250Selected) add(TipoProducto.HS1250)
            if (hs1500Selected) add(TipoProducto.HS1500)
        }.sortedBy { it.getPrecioVenta() }
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

    fun getDescuentoValidado(producto: TipoProducto, textoDescuento: String): Double {
        val descuento = textoDescuento.replace(",", ".").toDoubleOrNull() ?: 0.0
        val maxDescuento = producto.getMaxDescuento()
        return descuento.coerceIn(0.0, maxDescuento)
    }

    fun validarInputDescuento(input: String, maxDescuento: Double): String {
        val filtered = input.filter { it.isDigit() }
        val valor = filtered.toIntOrNull() ?: return filtered
        return if (valor > maxDescuento.toInt()) {
            maxDescuento.toInt().toString()
        } else {
            filtered
        }
    }

    fun calcularTotal(producto: TipoProducto): Double {
        val descuentoTexto = when (producto) {
            TipoProducto.HS875 -> descuentoHS875
            TipoProducto.HS1250 -> descuentoHS1250
            TipoProducto.HS1500 -> descuentoHS1500
            else -> "0"
        }
        val descuento = if (aplicaDescuento) getDescuentoValidado(producto, descuentoTexto) else 0.0
        val precioFinal = (producto.getPrecioVenta() - descuento).coerceAtLeast(producto.getPrecioBase())
        return cotizacion.ventanas.sumOf { it.areaM2 * precioFinal }
    }

    fun obtenerOGenerarPdf(): File? {
        if (pdfFile != null && pdfFile!!.exists()) {
            return pdfFile
        }

        val cotizacionParaPdf = if (desdeHistorial) {
            cotizacion.copy(
                productos = productosSeleccionados.ifEmpty { cotizacion.productos },
                descuentoHS875 = if (aplicaDescuento) getDescuentoValidado(TipoProducto.HS875, descuentoHS875) else cotizacion.descuentoHS875,
                descuentoHS1250 = if (aplicaDescuento) getDescuentoValidado(TipoProducto.HS1250, descuentoHS1250) else cotizacion.descuentoHS1250,
                descuentoHS1500 = if (aplicaDescuento) getDescuentoValidado(TipoProducto.HS1500, descuentoHS1500) else cotizacion.descuentoHS1500
            )
        } else {
            cotizacion
        }

        val pdf = generarPdfCotizacion(context, cotizacionParaPdf)
        if (pdf != null) {
            pdfFile = pdf
        }
        return pdf
    }



    fun handleDriveUpload() {
        if (!DriveAuthManager.isAuthenticated(context)) {
            val signInIntent = DriveAuthManager.getSignInIntent(context)
            signInLauncher.launch(signInIntent)
        } else {
            uploadToDrive()
        }
    }

    BackHandler {
        when {
            desdeHistorial -> onVolverAHistorial()
            guardado -> onVolverAInicio()
            else -> onVolverAEditar()
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Resumen de Cotización",
                onBack = {
                    when {
                        desdeHistorial -> onVolverAHistorial()
                        guardado -> onVolverAInicio()
                        else -> onVolverAEditar()
                    }
                },
                isDarkMode = isDarkMode
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding(), color = surface.copy(alpha = 0.95f), shadowElevation = 8.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (productosSeleccionados.isNotEmpty()) {
                        productosSeleccionados.forEach { producto ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("TOTAL ${producto.etiquetaCorta}", color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Text(formatMoney(calcularTotal(producto)), color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    if (!guardado) {
                        Button(
                            onClick = {
                                if (productosSeleccionados.isEmpty()) {
                                    Toast.makeText(context, "Selecciona al menos un sistema", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val especialista = cotizacion.especialista.ifBlank { "Especialista" }
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
                                    descuentoHS875 = if (aplicaDescuento) getDescuentoValidado(TipoProducto.HS875, descuentoHS875) else 0.0,
                                    descuentoHS1250 = if (aplicaDescuento) getDescuentoValidado(TipoProducto.HS1250, descuentoHS1250) else 0.0,
                                    descuentoHS1500 = if (aplicaDescuento) getDescuentoValidado(TipoProducto.HS1500, descuentoHS1500) else 0.0
                                )

                                guardarCotizacionLocal(context, cotizacionFinal)

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
                                            "Guardado localmente - Se subirá cuando haya internet"
                                        } else {
                                            "Guardado - Error al subir: quedará pendiente"
                                        }
                                    }
                                )

                                scope.launch {
                                    try {
                                        if (cotizacion.productos.size == 1) {
                                            val sistemaSeleccionado = cotizacion.productos.first().name
                                            val userId = SessionManager.getUserId(context)
                                            val userName = SessionManager.getNombre(context)

                                            android.util.Log.d(
                                                "ResumenScreen",
                                                "Creando registro de instalador para sistema: $sistemaSeleccionado"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("ResumenScreen", "Error creando registro instalador", e)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color.White else Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !subiendoPdf
                        ) {
                            if (subiendoPdf) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = if (isDarkMode) Color.Black else Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Guardando...", color = if (isDarkMode) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = if (isDarkMode) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("GUARDAR Y GENERAR PDF", color = if (isDarkMode) Color.Black else Color.White, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                        }
                    } else {
                        // Botones cuando ya está guardado
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {

                            // Primera fila: Enviar, PDF, Editar
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val pdf = obtenerOGenerarPdf()
                                        if (pdf != null) compartirPdf(context, pdf)
                                        else Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1.2f).height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.5.dp, if (isDarkMode) Color.White else Color.Black)
                                ) {
                                    Icon(Icons.Default.Share, null, tint = textPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Enviar", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        val pdf = obtenerOGenerarPdf()
                                        if (pdf != null) verPdf(context, pdf)
                                        else Toast.makeText(context, "Error al generar PDF", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isDarkMode) Color.White else Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, null, tint = if (isDarkMode) Color.Black else Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("PDF", color = if (isDarkMode) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = onVolverAEditar,
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.5.dp, if (isDarkMode) Color.White else Color.Black)
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Editar", color = textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Segunda fila: BOTN DE GOOGLE DRIVE
                            Button(
                                onClick = { handleDriveUpload() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                enabled = !subiendoDrive,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        driveUploadSuccess == true -> Color(0xFF10B981)
                                        driveUploadSuccess == false -> Color(0xFFEF4444)
                                        else -> Color(0xFF4285F4)
                                    }
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (subiendoDrive) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Subiendo a Drive...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_google_drive),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        when {
                                            driveUploadSuccess == true -> "✓ Subido a Google Drive"
                                            driveUploadSuccess == false -> "Error - Reintentar"
                                            else -> "Subir a Google Drive"
                                        },
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                StitchCard(title = "DATOS DEL CLIENTE", icon = Icons.Default.Person, isDarkMode = isDarkMode, surface = surface, headerBg = headerBg, border = border, accentBorder = accentBorder, textPrimary = textPrimary, textMuted = textMuted) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ClienteDataRow("Nombre", cotizacion.clienteNombre, textMuted, textPrimary, border)
                        ClienteDataRow("Teléfono", cotizacion.clienteTelefono, textMuted, textPrimary, border)
                        ClienteDataRow("Ciudad", cotizacion.ciudad.ifBlank { cotizacion.ubicacion.split(",").firstOrNull() ?: "" }, textMuted, textPrimary, border, showDivider = false)
                    }
                }
            }

            item {
                StitchCard(title = "APERTURAS", icon = Icons.Default.Straighten, isDarkMode = isDarkMode, surface = surface, headerBg = headerBg, border = border, accentBorder = accentBorder, textPrimary = textPrimary, textMuted = textMuted) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().background(if (isDarkMode) Color(0xFF1F1F1F) else Color.Black).padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ÁREA TOTAL", color = Color(0xFF9CA3AF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(formatArea(cotizacion.areaTotal), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().background(headerBg).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("APERTURAS (${cotizacion.ventanas.size})", color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }

                        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                cotizacion.ventanas.forEachIndexed { index, ventana ->
                                    AperturaItemSimple(index + 1, ventana, isDarkMode, textPrimary, textMuted, border)
                                    if (index < cotizacion.ventanas.lastIndex) HorizontalDivider(color = border.copy(0.5f))
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
                        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accentBorder))
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
                                                if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)
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
                                                if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)
                                            }
                                        )
                                        .clickable { selectedConfigTab = 1 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "DESCUENTOS",
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
                                                SystemCard("HS-875", hs875Selected, { hs875Selected = !hs875Selected }, isDarkMode, Modifier.weight(1f))
                                                SystemCard("HS-1250", hs1250Selected, { hs1250Selected = !hs1250Selected }, isDarkMode, Modifier.weight(1f))
                                                SystemCard("HS-1500", hs1500Selected, { hs1500Selected = !hs1500Selected }, isDarkMode, Modifier.weight(1f))
                                            }
                                        }
                                    }
                                    1 -> {
                                        // TAB: DESCUENTOS
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            // Toggle Sí/No
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Aplicar descuentos",
                                                    color = textPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                DescuentoToggleButton(aplicaDescuento, { aplicaDescuento = !aplicaDescuento }, isDarkMode, textMuted, border)
                                            }

                                            Spacer(Modifier.height(16.dp))

                                            AnimatedVisibility(
                                                visible = aplicaDescuento,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically()
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                    if (hs875Selected) DiscountInputField("HS-875", descuentoHS875, { descuentoHS875 = validarInputDescuento(it, TipoProducto.HS875.getMaxDescuento()) }, isDarkMode, textPrimary, border)
                                                    if (hs1250Selected) DiscountInputField("HS-1250", descuentoHS1250, { descuentoHS1250 = validarInputDescuento(it, TipoProducto.HS1250.getMaxDescuento()) }, isDarkMode, textPrimary, border)
                                                    if (hs1500Selected) DiscountInputField("HS-1500", descuentoHS1500, { descuentoHS1500 = validarInputDescuento(it, TipoProducto.HS1500.getMaxDescuento()) }, isDarkMode, textPrimary, border)
                                                }
                                            }

                                            if (!aplicaDescuento) {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Sin descuentos aplicados", color = textMuted, fontSize = 14.sp)
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
private fun DescuentoToggleButton(aplicaDescuento: Boolean, onToggle: () -> Unit, isDarkMode: Boolean, textMuted: Color, border: Color) {
    Surface(
        onClick = onToggle,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)),
        color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB),
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(if (!aplicaDescuento) (if (isDarkMode) Color.White else Color.Black) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("No", color = if (!aplicaDescuento) (if (isDarkMode) Color.Black else Color.White) else textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(if (aplicaDescuento) (if (isDarkMode) Color.White else Color.Black) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Sí", color = if (aplicaDescuento) (if (isDarkMode) Color.Black else Color.White) else textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// AperturaItem SIMPLIFICADO
@Composable
private fun AperturaItemSimple(index: Int, ventana: Ventana, isDarkMode: Boolean, textPrimary: Color, textMuted: Color, border: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(if (isDarkMode) Color(0xFF374151) else Color(0xFFF3F4F6)), contentAlignment = Alignment.Center) {
            Text(String.format("%02d", index), color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(ventana.descripcion, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${String.format("%.2f", ventana.alto)}m x ${String.format("%.2f", ventana.ancho)}m", color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.border(1.dp, border, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(ventana.tipoMontaje, color = textMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
                Text("|", color = textMuted.copy(0.5f), fontSize = 10.sp)
                Text(String.format("%.2f m²", ventana.areaM2), color = textMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("ADECUACIONES:", color = textMuted.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                val tieneAdecuacion = ventana.adecuacion != "No" && ventana.adecuacion.isNotBlank()
                Box(modifier = Modifier.background(if (tieneAdecuacion) (if (isDarkMode) Color.White else Color.Black) else Color.Transparent, RoundedCornerShape(4.dp)).border(1.dp, if (tieneAdecuacion) Color.Transparent else border.copy(0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(if (tieneAdecuacion) "Sí, ${ventana.adecuacion}" else "No", color = if (tieneAdecuacion) (if (isDarkMode) Color.Black else Color.White) else textMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun StitchCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isDarkMode: Boolean, surface: Color, headerBg: Color, border: Color, accentBorder: Color, textPrimary: Color, textMuted: Color, badge: String? = null, headerContent: @Composable (() -> Unit)? = null, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = surface, shape = RoundedCornerShape(0.dp), shadowElevation = 2.dp) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accentBorder))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth().background(headerBg).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(icon, contentDescription = null, tint = textPrimary, modifier = Modifier.size(20.dp))
                        Text(title, color = textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        badge?.let { Box(modifier = Modifier.background(Color.Black, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) { Text(it, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) } }
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
private fun ClienteDataRow(label: String, value: String, textMuted: Color, textPrimary: Color, border: Color, showDivider: Boolean = true) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = textMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(value, color = textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f, false))
        }
        if (showDivider) HorizontalDivider(color = border.copy(0.3f))
    }
}

@Composable
private fun SystemCard(label: String, selected: Boolean, onClick: () -> Unit, isDarkMode: Boolean, modifier: Modifier = Modifier) {
    val backgroundColor = when { selected && isDarkMode -> Color.White; selected -> Color.Black; isDarkMode -> Color(0xFF18181B); else -> Color.White }
    val contentColor = when { selected && isDarkMode -> Color.Black; selected -> Color.White; isDarkMode -> Color(0xFF71717A); else -> Color(0xFF6B7280) }
    val borderColor = when { selected -> Color.Transparent; isDarkMode -> Color(0xFF374151); else -> Color(0xFFE5E7EB) }

    Surface(onClick = onClick, modifier = modifier.height(70.dp), shape = RoundedCornerShape(12.dp), color = backgroundColor, border = if (!selected) BorderStroke(1.dp, borderColor) else null, shadowElevation = if (selected) 4.dp else 1.dp) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (selected) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(18.dp).clip(CircleShape).background(if (isDarkMode) Color.Black else Color.White), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = if (isDarkMode) Color.White else Color.Black, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun DiscountInputField(label: String, value: String, onValueChange: (String) -> Unit, isDarkMode: Boolean, textPrimary: Color, border: Color) {
    Column {
        Text(label, color = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = value, onValueChange = onValueChange, placeholder = { Text("0", color = if (isDarkMode) Color(0xFF6B7280) else Color(0xFF9CA3AF)) }, leadingIcon = { Text("$", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = if (isDarkMode) Color(0xFF1F1F1F) else Color.White, unfocusedContainerColor = if (isDarkMode) Color(0xFF1F1F1F) else Color.White, focusedBorderColor = if (isDarkMode) Color.White else Color.Black, unfocusedBorderColor = border, focusedTextColor = textPrimary, unfocusedTextColor = textPrimary), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
    }
}