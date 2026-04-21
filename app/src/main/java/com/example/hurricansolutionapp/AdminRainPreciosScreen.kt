package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val RainBlue = Color(0xFF2346AF)

private data class ComponenteInfo(val nombre: String, val unidad: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRainPreciosScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val bg = StitchColors.background(isDarkMode)
    val surface = StitchColors.surface(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textMuted = StitchColors.textMuted(isDarkMode)
    val border = StitchColors.border(isDarkMode)

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabNames = listOf("MANUAL", "ELÉCTRICO", "DESCUENTOS")

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var editedPrecios by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var editedDescuentos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val componentesManual = listOf(
        "tela" to ComponenteInfo("Tela", "M²", Icons.Default.Layers),
        "kit_manual" to ComponenteInfo("Componentes Toldo /Incluye Manivela", "pza", Icons.Default.Build),
        "perfil" to ComponenteInfo("Tubo para toldo 70mm", "metro lineal", Icons.Default.Straighten),
        "contrapeso" to ComponenteInfo("Contrapeso", "metro lineal", Icons.Default.FitnessCenter),
        "inserto" to ComponenteInfo("Inserto", "metro lineal", Icons.Default.ViewColumn),
        "tensor" to ComponenteInfo("Tensor", "metro lineal", Icons.Default.SwapVert),
        "manivela" to ComponenteInfo("Manivela", "pza", Icons.Default.SettingsBackupRestore)
    )

    val componentesElectrico = listOf(
        "tela" to ComponenteInfo("Tela", "M²", Icons.Default.Layers),
        "kit_adaptador" to ComponenteInfo("Kit de Toldo c/Adaptador", "pza", Icons.Default.Extension),
        "componentes_toldo_electrico" to ComponenteInfo("Componentes Toldo", "pza", Icons.Default.Build),
        "perfil" to ComponenteInfo("Tubo para toldo 70mm", "metro lineal", Icons.Default.Straighten),
        "intermedio_conector" to ComponenteInfo("Intermedio conector Bmighty 45mm", "pza", Icons.Default.Cable),
        "contrapeso" to ComponenteInfo("Contrapeso", "metro lineal", Icons.Default.FitnessCenter),
        "inserto_plastico" to ComponenteInfo("Inserto Plástico", "pza", Icons.Default.ViewColumn),
        "kit_electrico" to ComponenteInfo("Kit motor bidireccional 45mm", "pza", Icons.Default.ElectricalServices),
        "control_multicanal" to ComponenteInfo("Control Bmighty multicanal 15", "pza", Icons.Default.SettingsRemote),
        "control_monocanal" to ComponenteInfo("Control Bmighty Monocanal", "pza", Icons.Default.SettingsRemote)
    )

    val zonaNames = mapOf(
        "continental" to "Zona Continental",
        "islas" to "Zona Islas",
        "foranea" to "Zona Foránea"
    )

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            RainPriceManager.loadPrecios(forceRefresh = true)
            editedPrecios = RainPriceManager.precios.value.mapValues { String.format("%.2f", it.value) }
            editedDescuentos = RainPriceManager.descuentos.value.mapValues { String.format("%.2f", it.value) }
        } finally {
            isLoading = false
        }
    }

    fun guardarPrecios() {
        scope.launch {
            isSaving = true
            try {
                val userId = SessionManager.getUserId(context)
                var success = true
                for ((comp, valorStr) in editedPrecios) {
                    val valor = valorStr.toDoubleOrNull() ?: continue
                    if (!RainRepository.updatePrecio(comp, valor, userId)) success = false
                }
                for ((zona, valorStr) in editedDescuentos) {
                    val valor = valorStr.toDoubleOrNull() ?: continue
                    if (!RainRepository.updateDescuento(zona, valor, userId)) success = false
                }
                if (success) {
                    RainPriceManager.updatePreciosLocal(editedPrecios.mapValues { it.value.toDoubleOrNull() ?: 0.0 })
                    RainPriceManager.updateDescuentosLocal(editedDescuentos.mapValues { it.value.toDoubleOrNull() ?: 0.0 })
                    Toast.makeText(context, "Precios actualizados correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error al guardar algunos precios", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = { StitchTopBar(title = "Configurar Precios", onBack = onBack, isDarkMode = isDarkMode) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = surface,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { guardarPrecios() },
                    enabled = !isSaving && !isLoading,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RainBlue, disabledContainerColor = RainBlue.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("GUARDAR ${tabNames[selectedTabIndex]}", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Banner informativo
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkMode) RainBlue.copy(alpha = 0.2f) else Color(0xFFDBEAFE)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isDarkMode) RainBlue.copy(alpha = 0.3f) else Color(0xFF93C5FD))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = if (isDarkMode) Color(0xFF60A5FA) else RainBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Los cambios se aplicarán a todas las cotizaciones nuevas.", fontSize = 13.sp, color = if (isDarkMode) Color(0xFFBFDBFE) else Color(0xFF1E40AF), lineHeight = 18.sp)
                }
            }

            // Pestañas MANUAL / ELÉCTRICO
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabNames.forEachIndexed { index, nombre ->
                    val isSelected = selectedTabIndex == index
                    val buttonBg = if (isSelected) RainBlue else { if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6) }
                    val buttonText = if (isSelected) Color.White else { if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280) }
                    Box(
                        modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(buttonBg).clickable { selectedTabIndex = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(nombre, color = buttonText, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.3.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = border)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = RainBlue) }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Título con barra azul
                    val tituloSeccion = when (selectedTabIndex) {
                        0 -> "PRECIOS MECANISMO MANUAL"
                        1 -> "PRECIOS MECANISMO ELÉCTRICO"
                        else -> "DESCUENTOS POR ZONA"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        Box(modifier = Modifier.width(4.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(RainBlue))
                        Spacer(Modifier.width(12.dp))
                        Text(tituloSeccion, color = textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                    }

                    when (selectedTabIndex) {
                        // Tab MANUAL
                        0 -> {
                            componentesManual.forEach { (key, info) ->
                                RainPrecioCard(
                                    info = info, valor = editedPrecios[key] ?: "",
                                    onValueChange = { editedPrecios = editedPrecios + (key to it.filter { c -> c.isDigit() || c == '.' }) },
                                    isDarkMode = isDarkMode, surface = surface, textPrimary = textPrimary, textMuted = textMuted, border = border
                                )
                            }
                        }
                        // Tab ELÉCTRICO
                        1 -> {
                            componentesElectrico.forEach { (key, info) ->
                                RainPrecioCard(
                                    info = info, valor = editedPrecios[key] ?: "",
                                    onValueChange = { editedPrecios = editedPrecios + (key to it.filter { c -> c.isDigit() || c == '.' }) },
                                    isDarkMode = isDarkMode, surface = surface, textPrimary = textPrimary, textMuted = textMuted, border = border
                                )
                            }
                        }
                        // Tab DESCUENTOS
                        2 -> {
                            // Nota informativa
                            Text(
                                "Estos descuentos se aplican tanto a Manual como a Eléctrico según la zona geográfica del cliente.",
                                fontSize = 13.sp,
                                color = textMuted,
                                lineHeight = 18.sp
                            )

                            editedDescuentos.forEach { (zona, valor) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = surface),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, border)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)) {
                                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Discount, null, tint = textPrimary, modifier = Modifier.size(24.dp)) }
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(zonaNames[zona] ?: zona, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)
                                                Text("Aplica a Manual y Eléctrico", fontSize = 13.sp, color = textMuted)
                                            }
                                        }
                                        Spacer(Modifier.height(16.dp))
                                        Text("PORCENTAJE DE DESCUENTO", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = valor,
                                            onValueChange = { editedDescuentos = editedDescuentos + (zona to it.filter { c -> c.isDigit() || c == '.' }) },
                                            modifier = Modifier.fillMaxWidth(),
                                            suffix = { Text("%", color = textMuted, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF9FAFB),
                                                unfocusedContainerColor = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF9FAFB),
                                                focusedBorderColor = RainBlue, unfocusedBorderColor = border,
                                                focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
                                            ),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun RainPrecioCard(
    info: ComponenteInfo, valor: String, onValueChange: (String) -> Unit,
    isDarkMode: Boolean, surface: Color, textPrimary: Color, textMuted: Color, border: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = if (isDarkMode) Color(0xFF27272A) else Color(0xFFF3F4F6)) {
                    Box(contentAlignment = Alignment.Center) { Icon(info.icon, null, tint = textPrimary, modifier = Modifier.size(24.dp)) }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(info.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimary)
                    Text(info.unidad, fontSize = 13.sp, color = textMuted)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("PRECIO X UNIDAD DE MEDIDA VENTA", color = textMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = valor, onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Text("$", color = textMuted, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF9FAFB),
                    unfocusedContainerColor = if (isDarkMode) Color(0xFF18181B) else Color(0xFFF9FAFB),
                    focusedBorderColor = RainBlue, unfocusedBorderColor = border,
                    focusedTextColor = textPrimary, unfocusedTextColor = textPrimary
                ),
                singleLine = true
            )
        }
    }
}