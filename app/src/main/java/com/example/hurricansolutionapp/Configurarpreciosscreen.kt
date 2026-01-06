package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
    var hasChanges by remember { mutableStateOf(false) }

    // Precios HS-875
    var hs875Venta by rememberSaveable { mutableStateOf("") }
    var hs875Base by rememberSaveable { mutableStateOf("") }

    // Precios HS-1250
    var hs1250Venta by rememberSaveable { mutableStateOf("") }
    var hs1250Base by rememberSaveable { mutableStateOf("") }

    // Precios HS-1500
    var hs1500Venta by rememberSaveable { mutableStateOf("") }
    var hs1500Base by rememberSaveable { mutableStateOf("") }

    // Cargar precios actuales
    LaunchedEffect(Unit) {
        isLoading = true
        val config = AdminRepository.getAppConfig()
        hs875Venta = config.hs875PrecioVenta.toInt().toString()
        hs875Base = config.hs875PrecioBase.toInt().toString()
        hs1250Venta = config.hs1250PrecioVenta.toInt().toString()
        hs1250Base = config.hs1250PrecioBase.toInt().toString()
        hs1500Venta = config.hs1500PrecioVenta.toInt().toString()
        hs1500Base = config.hs1500PrecioBase.toInt().toString()
        isLoading = false
    }

    // Colores
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val surface = if (isDarkMode) Color(0xFF000000) else Color.White
    val card = if (isDarkMode) Color(0xFF18181B) else Color.White
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val inputBg = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)

    fun validateAndSave() {
        val v875 = hs875Venta.toDoubleOrNull()
        val b875 = hs875Base.toDoubleOrNull()
        val v1250 = hs1250Venta.toDoubleOrNull()
        val b1250 = hs1250Base.toDoubleOrNull()
        val v1500 = hs1500Venta.toDoubleOrNull()
        val b1500 = hs1500Base.toDoubleOrNull()

        if (v875 == null || b875 == null || v1250 == null ||
            b1250 == null || v1500 == null || b1500 == null) {
            Toast.makeText(context, "Todos los campos son requeridos", Toast.LENGTH_SHORT).show()
            return
        }

        if (v875 < b875 || v1250 < b1250 || v1500 < b1500) {
            Toast.makeText(context, "El precio de venta debe ser mayor o igual al precio base", Toast.LENGTH_LONG).show()
            return
        }

        scope.launch {
            isSaving = true

            // Obtener el userId del usuario actual
            val userId = SessionManager.getUserId(context)

            val configUpdate = AppConfigUpdate(
                hs875PrecioVenta = v875,
                hs875PrecioBase = b875,
                hs1250PrecioVenta = v1250,
                hs1250PrecioBase = b1250,
                hs1500PrecioVenta = v1500,
                hs1500PrecioBase = b1500,
                updatedBy = userId
            )

            AdminRepository.updateAppConfig(context, configUpdate).onSuccess {
                // Actualizar config local en PriceManager
                val newConfig = AppConfig(
                    hs875PrecioVenta = v875,
                    hs875PrecioBase = b875,
                    hs1250PrecioVenta = v1250,
                    hs1250PrecioBase = b1250,
                    hs1500PrecioVenta = v1500,
                    hs1500PrecioBase = b1500
                )
                PriceManager.updateLocalConfig(newConfig)

                Toast.makeText(context, "Precios actualizados correctamente", Toast.LENGTH_SHORT).show()
                hasChanges = false
                onBack()
            }.onFailure { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }

            isSaving = false
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            // StitchTopBar
            Column(
                modifier = Modifier
                    .background(surface)
                    .statusBarsPadding()
            ) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = surface
                    ),
                    title = {
                        Text(
                            "CONFIGURAR PRECIOS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary,
                            letterSpacing = 0.5.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chevron_left),
                                contentDescription = "Volver",
                                tint = textPrimary
                            )
                        }
                    }
                )
                HorizontalDivider(color = border, thickness = 1.dp)
            }
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
                    onClick = { validateAndSave() },
                    enabled = !isSaving && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "GUARDAR CAMBIOS",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF10B981))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF3B82F6).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Los cambios se aplicarán a todas las cotizaciones nuevas. Las cotizaciones existentes mantendrán sus precios originales.",
                            color = Color(0xFF3B82F6),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // HS-875
                PriceCard(
                    title = "HS-875",
                    subtitle = "Polipropileno",
                    iconColor = Color(0xFF10B981),
                    precioVenta = hs875Venta,
                    onPrecioVentaChange = { hs875Venta = it; hasChanges = true },
                    precioBase = hs875Base,
                    onPrecioBaseChange = { hs875Base = it; hasChanges = true },
                    isDarkMode = isDarkMode,
                    card = card,
                    border = border,
                    textPrimary = textPrimary,
                    textMuted = textMuted,
                    inputBg = inputBg
                )

                // HS-1250
                PriceCard(
                    title = "HS-1250",
                    subtitle = "Poliéster y Aramida",
                    iconColor = Color(0xFF3B82F6),
                    precioVenta = hs1250Venta,
                    onPrecioVentaChange = { hs1250Venta = it; hasChanges = true },
                    precioBase = hs1250Base,
                    onPrecioBaseChange = { hs1250Base = it; hasChanges = true },
                    isDarkMode = isDarkMode,
                    card = card,
                    border = border,
                    textPrimary = textPrimary,
                    textMuted = textMuted,
                    inputBg = inputBg
                )

                // HS-1500
                PriceCard(
                    title = "HS-1500",
                    subtitle = "Aramida Premium",
                    iconColor = Color(0xFF8B5CF6),
                    precioVenta = hs1500Venta,
                    onPrecioVentaChange = { hs1500Venta = it; hasChanges = true },
                    precioBase = hs1500Base,
                    onPrecioBaseChange = { hs1500Base = it; hasChanges = true },
                    isDarkMode = isDarkMode,
                    card = card,
                    border = border,
                    textPrimary = textPrimary,
                    textMuted = textMuted,
                    inputBg = inputBg
                )

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun PriceCard(
    title: String,
    subtitle: String,
    iconColor: Color,
    precioVenta: String,
    onPrecioVentaChange: (String) -> Unit,
    precioBase: String,
    onPrecioBaseChange: (String) -> Unit,
    isDarkMode: Boolean,
    card: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color,
    inputBg: Color
) {
    val venta = precioVenta.toDoubleOrNull() ?: 0.0
    val base = precioBase.toDoubleOrNull() ?: 0.0
    val margen = if (venta > 0 && base > 0) ((venta - base) / venta * 100) else 0.0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = card,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(subtitle, color = textMuted, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Precio de Venta
            Text("Precio de Venta (USD/m²)", color = textMuted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = precioVenta,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onPrecioVentaChange(it) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Text("$", color = textMuted, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = inputBg,
                    unfocusedContainerColor = inputBg,
                    focusedBorderColor = iconColor,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                )
            )

            Spacer(Modifier.height(12.dp))

            // Precio Base
            Text("Precio Base / Costo (USD/m²)", color = textMuted, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = precioBase,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onPrecioBaseChange(it) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Text("$", color = textMuted, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = inputBg,
                    unfocusedContainerColor = inputBg,
                    focusedBorderColor = iconColor,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                )
            )

            Spacer(Modifier.height(12.dp))

            // Margen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconColor.copy(alpha = 0.1f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Margen de ganancia:", color = textMuted, fontSize = 13.sp)
                Text(
                    "${String.format("%.1f", margen)}%",
                    color = iconColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}