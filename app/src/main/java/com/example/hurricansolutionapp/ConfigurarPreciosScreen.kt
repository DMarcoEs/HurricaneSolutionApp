package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurarPreciosScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Tab seleccionado (0 = Continental, 1 = Islas, 2 = Foránea)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val zonas = listOf(ZonaGeografica.CONTINENTAL, ZonaGeografica.ISLAS, ZonaGeografica.FORANEA)

    // Estados de edición para cada zona
    // Continental
    var continental875Venta by rememberSaveable { mutableStateOf("") }
    var continental875Base by rememberSaveable { mutableStateOf("") }
    var continental1250Venta by rememberSaveable { mutableStateOf("") }
    var continental1250Base by rememberSaveable { mutableStateOf("") }
    var continental1500Venta by rememberSaveable { mutableStateOf("") }
    var continental1500Base by rememberSaveable { mutableStateOf("") }

    // Islas
    var islas875Venta by rememberSaveable { mutableStateOf("") }
    var islas875Base by rememberSaveable { mutableStateOf("") }
    var islas1250Venta by rememberSaveable { mutableStateOf("") }
    var islas1250Base by rememberSaveable { mutableStateOf("") }
    var islas1500Venta by rememberSaveable { mutableStateOf("") }
    var islas1500Base by rememberSaveable { mutableStateOf("") }

    // Foránea
    var foranea875Venta by rememberSaveable { mutableStateOf("") }
    var foranea875Base by rememberSaveable { mutableStateOf("") }
    var foranea1250Venta by rememberSaveable { mutableStateOf("") }
    var foranea1250Base by rememberSaveable { mutableStateOf("") }
    var foranea1500Venta by rememberSaveable { mutableStateOf("") }
    var foranea1500Base by rememberSaveable { mutableStateOf("") }

    // Cargar precios actuales
    LaunchedEffect(Unit) {
        isLoading = true
        val precios = AdminRepository.getPreciosTodasZonas()

        // Continental
        continental875Venta = precios.continental.hs875PrecioVenta.toInt().toString()
        continental875Base = precios.continental.hs875PrecioBase.toInt().toString()
        continental1250Venta = precios.continental.hs1250PrecioVenta.toInt().toString()
        continental1250Base = precios.continental.hs1250PrecioBase.toInt().toString()
        continental1500Venta = precios.continental.hs1500PrecioVenta.toInt().toString()
        continental1500Base = precios.continental.hs1500PrecioBase.toInt().toString()

        // Islas
        islas875Venta = precios.islas.hs875PrecioVenta.toInt().toString()
        islas875Base = precios.islas.hs875PrecioBase.toInt().toString()
        islas1250Venta = precios.islas.hs1250PrecioVenta.toInt().toString()
        islas1250Base = precios.islas.hs1250PrecioBase.toInt().toString()
        islas1500Venta = precios.islas.hs1500PrecioVenta.toInt().toString()
        islas1500Base = precios.islas.hs1500PrecioBase.toInt().toString()

        // Foránea
        foranea875Venta = precios.foranea.hs875PrecioVenta.toInt().toString()
        foranea875Base = precios.foranea.hs875PrecioBase.toInt().toString()
        foranea1250Venta = precios.foranea.hs1250PrecioVenta.toInt().toString()
        foranea1250Base = precios.foranea.hs1250PrecioBase.toInt().toString()
        foranea1500Venta = precios.foranea.hs1500PrecioVenta.toInt().toString()
        foranea1500Base = precios.foranea.hs1500PrecioBase.toInt().toString()

        isLoading = false
    }

    // Colores Stitch
    val bg = StitchColors.background(isDarkMode)
    val surface = StitchColors.surface(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textMuted = StitchColors.textMuted(isDarkMode)
    val border = StitchColors.border(isDarkMode)
    val primary = StitchColors.primary(isDarkMode)
    val onPrimary = StitchColors.onPrimary(isDarkMode)

    fun validateAndSaveZona(zona: ZonaGeografica) {
        val v875: Double?
        val b875: Double?
        val v1250: Double?
        val b1250: Double?
        val v1500: Double?
        val b1500: Double?

        when (zona) {
            ZonaGeografica.CONTINENTAL -> {
                v875 = continental875Venta.toDoubleOrNull()
                b875 = continental875Base.toDoubleOrNull()
                v1250 = continental1250Venta.toDoubleOrNull()
                b1250 = continental1250Base.toDoubleOrNull()
                v1500 = continental1500Venta.toDoubleOrNull()
                b1500 = continental1500Base.toDoubleOrNull()
            }
            ZonaGeografica.ISLAS -> {
                v875 = islas875Venta.toDoubleOrNull()
                b875 = islas875Base.toDoubleOrNull()
                v1250 = islas1250Venta.toDoubleOrNull()
                b1250 = islas1250Base.toDoubleOrNull()
                v1500 = islas1500Venta.toDoubleOrNull()
                b1500 = islas1500Base.toDoubleOrNull()
            }
            ZonaGeografica.FORANEA -> {
                v875 = foranea875Venta.toDoubleOrNull()
                b875 = foranea875Base.toDoubleOrNull()
                v1250 = foranea1250Venta.toDoubleOrNull()
                b1250 = foranea1250Base.toDoubleOrNull()
                v1500 = foranea1500Venta.toDoubleOrNull()
                b1500 = foranea1500Base.toDoubleOrNull()
            }
        }

        if (v875 == null || b875 == null || v1250 == null || b1250 == null || v1500 == null || b1500 == null) {
            Toast.makeText(context, "Todos los campos son requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        if (v875 < b875 || v1250 < b1250 || v1500 < b1500) {
            Toast.makeText(context, "El precio de venta debe ser mayor o igual al precio base", Toast.LENGTH_LONG).show()
            return
        }

        scope.launch {
            isSaving = true

            val userId = SessionManager.getUserId(context)
            val update = PrecioZonaUpdate(
                hs875PrecioVenta = v875,
                hs875PrecioBase = b875,
                hs1250PrecioVenta = v1250,
                hs1250PrecioBase = b1250,
                hs1500PrecioVenta = v1500,
                hs1500PrecioBase = b1500,
                updatedBy = userId
            )

            AdminRepository.updatePreciosZona(context, zona.id, update).onSuccess {
                val nuevoPrecio = PrecioZona(
                    zona = zona.id,
                    zonaNombre = zona.nombreDisplay,
                    hs875PrecioVenta = v875,
                    hs875PrecioBase = b875,
                    hs1250PrecioVenta = v1250,
                    hs1250PrecioBase = b1250,
                    hs1500PrecioVenta = v1500,
                    hs1500PrecioBase = b1500
                )
                PriceManager.updatePreciosZona(zona, nuevoPrecio)

                Toast.makeText(context, "Precios de ${zona.nombreDisplay} actualizados", Toast.LENGTH_SHORT).show()
            }.onFailure { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }

            isSaving = false
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "Configurar Precios",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = surface,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { validateAndSaveZona(zonas[selectedTabIndex]) },
                    enabled = !isSaving && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        disabledContainerColor = primary.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "GUARDAR ${zonas[selectedTabIndex].nombreDisplay.uppercase()}",
                            fontWeight = FontWeight.Bold,
                            color = onPrimary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Banner informativo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E3A8A).copy(alpha = 0.2f) else Color(0xFFDBEAFE)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF3B82F6).copy(alpha = 0.3f) else Color(0xFF93C5FD))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Los cambios se aplicarán a todas las cotizaciones nuevas.",
                        fontSize = 13.sp,
                        color = if (isDarkMode) Color(0xFFBFDBFE) else Color(0xFF1E40AF),
                        lineHeight = 18.sp
                    )
                }
            }

            // Tabs de zonas
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = surface,
                contentColor = textPrimary
            ) {
                zonas.forEachIndexed { index, zona ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(ZonasData.getZonaEmoji(zona), fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    when (zona) {
                                        ZonaGeografica.CONTINENTAL -> "Continental"
                                        ZonaGeografica.ISLAS -> "Islas"
                                        ZonaGeografica.FORANEA -> "Foránea"
                                    },
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        Box(modifier = Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(primary))
                        Spacer(Modifier.width(12.dp))
                        Text("PRECIOS ${zonas[selectedTabIndex].nombreDisplay.uppercase()}", color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                    }

                    when (selectedTabIndex) {
                        0 -> {
                            PrecioProductoCard("HS-875", "Polipropileno", continental875Venta, { continental875Venta = it }, continental875Base, { continental875Base = it }, isDarkMode, surface, textPrimary, textMuted, border)
                            PrecioProductoCard("HS-1250", "Poliester y Aramida", continental1250Venta, { continental1250Venta = it }, continental1250Base, { continental1250Base = it }, isDarkMode, surface, textPrimary, textMuted, border)
                            PrecioProductoCard("HS-1500", "Nylon Balístico", continental1500Venta, { continental1500Venta = it }, continental1500Base, { continental1500Base = it }, isDarkMode, surface, textPrimary, textMuted, border)
                        }
                        1 -> {
                            PrecioProductoCard("HS-875", "Polipropileno", islas875Venta, { islas875Venta = it }, islas875Base, { islas875Base = it }, isDarkMode, surface, textPrimary, textMuted, border)
                            PrecioProductoCard("HS-1250", "Poliester y Aramida", islas1250Venta, { islas1250Venta = it }, islas1250Base, { islas1250Base = it }, isDarkMode, surface, textPrimary, textMuted, border)
                            PrecioProductoCard("HS-1500", "Nylon Balístico", islas1500Venta, { islas1500Venta = it }, islas1500Base, { islas1500Base = it }, isDarkMode, surface, textPrimary, textMuted, border)
                        }
                        2 -> {
                            PrecioProductoCard("HS-875", "Polipropileno", foranea875Venta, { foranea875Venta = it }, foranea875Base, { foranea875Base = it }, isDarkMode, surface, textPrimary, textMuted, border)
                            PrecioProductoCard("HS-1250", "Poliester y Aramida", foranea1250Venta, { foranea1250Venta = it }, foranea1250Base, { foranea1250Base = it }, isDarkMode, surface, textPrimary, textMuted, border)
                            PrecioProductoCard("HS-1500", "Nylon Balístico", foranea1500Venta, { foranea1500Venta = it }, foranea1500Base, { foranea1500Base = it }, isDarkMode, surface, textPrimary, textMuted, border)
                        }
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun PrecioProductoCard(
    nombreProducto: String,
    descripcion: String,
    precioVenta: String,
    onPrecioVentaChange: (String) -> Unit,
    precioBase: String,
    onPrecioBaseChange: (String) -> Unit,
    isDarkMode: Boolean,
    surface: Color,
    textPrimary: Color,
    textMuted: Color,
    border: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Shield, null, tint = textPrimary, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(nombreProducto, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textPrimary)
                    Text(descripcion, fontSize = 13.sp, color = textMuted)
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("PRECIO DE VENTA (USD/m²)", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = precioVenta,
                onValueChange = { if (it.all { c -> c.isDigit() }) onPrecioVentaChange(it) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Text("$", color = textMuted, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF9FAFB),
                    unfocusedContainerColor = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF9FAFB),
                    focusedBorderColor = if (isDarkMode) Color.White else Color.Black,
                    unfocusedBorderColor = border,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Text("PRECIO BASE / COSTO (USD/m²)", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = precioBase,
                onValueChange = { if (it.all { c -> c.isDigit() }) onPrecioBaseChange(it) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Text("$", color = textMuted, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF9FAFB),
                    unfocusedContainerColor = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF9FAFB),
                    focusedBorderColor = if (isDarkMode) Color.White else Color.Black,
                    unfocusedBorderColor = border,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                singleLine = true
            )
        }
    }
}