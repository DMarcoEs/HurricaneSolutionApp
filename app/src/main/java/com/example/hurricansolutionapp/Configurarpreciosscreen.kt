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
    val surface = if (isDarkMode) Color(0xFF111111) else Color.White
    val card = if (isDarkMode) Color(0xFF18181B) else Color.White
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val inputBg = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)

    fun validateAndSave() {
        // Validar que todos los campos tengan valores
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

        // Validar que precio venta >= precio base
        if (v875 < b875 || v1250 < b1250 || v1500 < b1500) {
            Toast.makeText(context, "El precio de venta debe ser mayor o igual al precio base", Toast.LENGTH_LONG).show()
            return
        }

        scope.launch {
            isSaving = true

            val configUpdate = AppConfigUpdate(
                hs875PrecioVenta = v875,
                hs875PrecioBase = b875,
                hs1250PrecioVenta = v1250,
                hs1250PrecioBase = b1250,
                hs1500PrecioVenta = v1500,
                hs1500PrecioBase = b1500,
                updatedBy = SessionManager.getUserId(context)
            )

            val result = AdminRepository.updateAppConfig(context, configUpdate)

            result.onSuccess {
                // Actualizar el PriceManager local
                PriceManager.updateLocalConfig(
                    AppConfig(
                        hs875PrecioVenta = v875,
                        hs875PrecioBase = b875,
                        hs1250PrecioVenta = v1250,
                        hs1250PrecioBase = b1250,
                        hs1500PrecioVenta = v1500,
                        hs1500PrecioBase = b1500
                    )
                )
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
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = textPrimary
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "CONFIGURAR PRECIOS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.size(40.dp))
                }
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
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("GUARDANDO...", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Text("GUARDAR CAMBIOS", color = Color.White, fontWeight = FontWeight.Bold)
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Nota informativa
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF3B82F6).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
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

                // ═══════════════════════════════════════════════════════════════════
                // HS-875
                // ═══════════════════════════════════════════════════════════════════
                PriceSection(
                    title = "HS-875",
                    subtitle = "Polipropileno",
                    color = Color(0xFF10B981),
                    precioVenta = hs875Venta,
                    precioBase = hs875Base,
                    onVentaChange = { hs875Venta = it; hasChanges = true },
                    onBaseChange = { hs875Base = it; hasChanges = true },
                    isDarkMode = isDarkMode,
                    card = card,
                    border = border,
                    textPrimary = textPrimary,
                    textMuted = textMuted,
                    inputBg = inputBg
                )

                // ═══════════════════════════════════════════════════════════════════
                // HS-1250
                // ═══════════════════════════════════════════════════════════════════
                PriceSection(
                    title = "HS-1250",
                    subtitle = "Poliéster y Aramida",
                    color = Color(0xFF3B82F6),
                    precioVenta = hs1250Venta,
                    precioBase = hs1250Base,
                    onVentaChange = { hs1250Venta = it; hasChanges = true },
                    onBaseChange = { hs1250Base = it; hasChanges = true },
                    isDarkMode = isDarkMode,
                    card = card,
                    border = border,
                    textPrimary = textPrimary,
                    textMuted = textMuted,
                    inputBg = inputBg
                )

                // ═══════════════════════════════════════════════════════════════════
                // HS-1500
                // ═══════════════════════════════════════════════════════════════════
                PriceSection(
                    title = "HS-1500",
                    subtitle = "Nylon Balístico",
                    color = Color(0xFF8B5CF6),
                    precioVenta = hs1500Venta,
                    precioBase = hs1500Base,
                    onVentaChange = { hs1500Venta = it; hasChanges = true },
                    onBaseChange = { hs1500Base = it; hasChanges = true },
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
private fun PriceSection(
    title: String,
    subtitle: String,
    color: Color,
    precioVenta: String,
    precioBase: String,
    onVentaChange: (String) -> Unit,
    onBaseChange: (String) -> Unit,
    isDarkMode: Boolean,
    card: Color,
    border: Color,
    textPrimary: Color,
    textMuted: Color,
    inputBg: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = card,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column {
            // Header con color
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.1f))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            title,
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            subtitle,
                            color = textMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Campos de precio
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Precio de Venta
                PriceInputField(
                    label = "Precio de Venta (USD/m²)",
                    value = precioVenta,
                    onValueChange = { onVentaChange(it.filter { c -> c.isDigit() }) },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    textMuted = textMuted,
                    inputBg = inputBg,
                    border = border
                )

                // Precio Base
                PriceInputField(
                    label = "Precio Base / Costo (USD/m²)",
                    value = precioBase,
                    onValueChange = { onBaseChange(it.filter { c -> c.isDigit() }) },
                    isDarkMode = isDarkMode,
                    textPrimary = textPrimary,
                    textMuted = textMuted,
                    inputBg = inputBg,
                    border = border
                )

                // Margen calculado
                val venta = precioVenta.toDoubleOrNull() ?: 0.0
                val base = precioBase.toDoubleOrNull() ?: 0.0
                val margen = if (venta > 0) ((venta - base) / venta * 100) else 0.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (margen >= 0) Color(0xFF10B981).copy(alpha = 0.1f)
                            else Color(0xFFEF4444).copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Margen de ganancia:",
                        color = textMuted,
                        fontSize = 13.sp
                    )
                    Text(
                        "${String.format("%.1f", margen)}%",
                        color = if (margen >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isDarkMode: Boolean,
    textPrimary: Color,
    textMuted: Color,
    inputBg: Color,
    border: Color
) {
    Column {
        Text(
            label,
            color = textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            leadingIcon = {
                Text(
                    "$",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputBg,
                unfocusedContainerColor = inputBg,
                focusedBorderColor = if (isDarkMode) Color.White else Color.Black,
                unfocusedBorderColor = border,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}