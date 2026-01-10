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

    // Colores Stitch
    val bg = StitchColors.background(isDarkMode)
    val surface = StitchColors.surface(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val border = StitchColors.border(isDarkMode)
    val primary = StitchColors.primary(isDarkMode)
    val onPrimary = StitchColors.onPrimary(isDarkMode)

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
            StitchTopBar(
                title = "Configurar Precios",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        },
        bottomBar = {
            // Boton fijo abajo
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkMode) Color.Black.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f),
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { validateAndSave() },
                    enabled = !isSaving && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
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
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            tint = onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "GUARDAR CAMBIOS",
                            color = onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
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
                CircularProgressIndicator(color = primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Banner informativo
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDarkMode) Color(0xFF1E3A5F).copy(alpha = 0.3f) else Color(0xFFE0F2FE),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF1E3A5F) else Color(0xFFBAE6FD))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Los cambios se aplicaran a todas las cotizaciones nuevas. Las cotizaciones existentes mantendran sus precios originales.",
                            color = if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF0369A1),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Titulo de seccion con barra lateral
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(primary)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "PRECIOS ACTUALES",
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }

                // HS-875
                StitchPriceCard(
                    title = "HS-875",
                    subtitle = "Polipropileno",
                    precioVenta = hs875Venta,
                    onPrecioVentaChange = { hs875Venta = it; hasChanges = true },
                    precioBase = hs875Base,
                    onPrecioBaseChange = { hs875Base = it; hasChanges = true },
                    isDarkMode = isDarkMode
                )

                // HS-1250
                StitchPriceCard(
                    title = "HS-1250",
                    subtitle = "Poliester y Aramida",
                    precioVenta = hs1250Venta,
                    onPrecioVentaChange = { hs1250Venta = it; hasChanges = true },
                    precioBase = hs1250Base,
                    onPrecioBaseChange = { hs1250Base = it; hasChanges = true },
                    isDarkMode = isDarkMode
                )

                // HS-1500
                StitchPriceCard(
                    title = "HS-1500",
                    subtitle = "Nylon Balistico",
                    precioVenta = hs1500Venta,
                    onPrecioVentaChange = { hs1500Venta = it; hasChanges = true },
                    precioBase = hs1500Base,
                    onPrecioBaseChange = { hs1500Base = it; hasChanges = true },
                    isDarkMode = isDarkMode
                )

                // Espacio para el boton inferior
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun StitchPriceCard(
    title: String,
    subtitle: String,
    precioVenta: String,
    onPrecioVentaChange: (String) -> Unit,
    precioBase: String,
    onPrecioBaseChange: (String) -> Unit,
    isDarkMode: Boolean
) {
    val surface = StitchColors.surface(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val border = StitchColors.border(isDarkMode)
    val primary = StitchColors.primary(isDarkMode)
    val onPrimary = StitchColors.onPrimary(isDarkMode)
    val inputBg = StitchColors.surfaceVariant(isDarkMode)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column {
            // Header con icono
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkMode) Color(0xFF18181B) else Color(0xFFF9FAFB))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono de escudo
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        title,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        subtitle,
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(color = border, thickness = 1.dp)

            // Campos de precio
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Precio de Venta
                Column {
                    Text(
                        "PRECIO DE VENTA (USD/m²)",
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = precioVenta,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onPrecioVentaChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Text(
                                "$",
                                color = textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedBorderColor = primary,
                            unfocusedBorderColor = border,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    )
                }

                // Precio Base
                Column {
                    Text(
                        "PRECIO BASE / COSTO (USD/m²)",
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = precioBase,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onPrecioBaseChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Text(
                                "$",
                                color = textSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg,
                            focusedBorderColor = primary,
                            unfocusedBorderColor = border,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    )
                }
                // SIN margen de ganancia
            }
        }
    }
}