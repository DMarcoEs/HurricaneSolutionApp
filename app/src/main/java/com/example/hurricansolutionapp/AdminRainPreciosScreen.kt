package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

/**
 * Pantalla de administración de precios Rain Protection
 */

// Color Rain Protection
private val RainBlue = Color(0xFF2346AF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminRainPreciosScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Colores
    val bg = StitchColors.background(isDarkMode)
    val surface = StitchColors.surface(isDarkMode)
    val textPrimary = StitchColors.textPrimary(isDarkMode)
    val textSecondary = StitchColors.textSecondary(isDarkMode)
    val border = StitchColors.border(isDarkMode)

    // Estados
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var precios by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var descuentos by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var editedPrecios by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var editedDescuentos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Cargar datos
    LaunchedEffect(Unit) {
        try {
            RainPriceManager.loadPrecios(forceRefresh = true)
            precios = RainPriceManager.precios.value
            descuentos = RainPriceManager.descuentos.value

            editedPrecios = precios.mapValues { String.format("%.2f", it.value) }
            editedDescuentos = descuentos.mapValues { String.format("%.2f", it.value) }
        } finally {
            isLoading = false
        }
    }

    // Nombres legibles para los componentes
    val componenteNames = mapOf(
        "tela" to "Tela (por m²)",
        "kit_manual" to "Kit Manual",
        "kit_electrico" to "Kit Eléctrico",
        "perfil" to "Perfil (por metro)",
        "contrapeso" to "Contrapeso (por metro)",
        "inserto" to "Inserto (por metro)",
        "tensor" to "Tensor (por metro)",
        "manivela" to "Manivela",
        "kit_adaptador" to "Kit Adaptador"
    )

    val zonaNames = mapOf(
        "continental" to "Zona Continental",
        "islas" to "Zona Islas",
        "foranea" to "Zona Foránea"
    )

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBar(
                title = "PRECIOS RAIN PROTECTION",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = if (isDarkMode) Color.White else RainBlue
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ═══════════════════════════════════════════════════════════════
                // SECCIÓN: PRECIOS DE COMPONENTES
                // ═══════════════════════════════════════════════════════════════
                item {
                    Text(
                        text = "PRECIOS DE COMPONENTES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(editedPrecios.keys.toList()) { componente ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = surface),
                        border = BorderStroke(1.dp, border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = componenteNames[componente] ?: componente,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary
                                )
                            }

                            OutlinedTextField(
                                value = editedPrecios[componente] ?: "",
                                onValueChange = { newValue ->
                                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                                    editedPrecios = editedPrecios + (componente to filtered)
                                },
                                modifier = Modifier.width(120.dp),
                                prefix = { Text("$") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = textPrimary,
                                    unfocusedBorderColor = border,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // SECCIÓN: DESCUENTOS POR ZONA
                // ═══════════════════════════════════════════════════════════════
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "DESCUENTOS POR ZONA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(editedDescuentos.keys.toList()) { zona ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = surface),
                        border = BorderStroke(1.dp, border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = zonaNames[zona] ?: zona,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textPrimary
                                )
                            }

                            OutlinedTextField(
                                value = editedDescuentos[zona] ?: "",
                                onValueChange = { newValue ->
                                    val filtered = newValue.filter { it.isDigit() || it == '.' }
                                    editedDescuentos = editedDescuentos + (zona to filtered)
                                },
                                modifier = Modifier.width(100.dp),
                                suffix = { Text("%") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = textPrimary,
                                    unfocusedBorderColor = border,
                                    focusedTextColor = textPrimary,
                                    unfocusedTextColor = textPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // BOTÓN GUARDAR
                // ═══════════════════════════════════════════════════════════════
                item {
                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                try {
                                    val userId = SessionManager.getUserId(context)
                                    var success = true

                                    // Guardar precios
                                    for ((componente, valorStr) in editedPrecios) {
                                        val valor = valorStr.toDoubleOrNull() ?: continue
                                        val result = RainRepository.updatePrecio(componente, valor, userId)
                                        if (!result) success = false
                                    }

                                    // Guardar descuentos
                                    for ((zona, valorStr) in editedDescuentos) {
                                        val valor = valorStr.toDoubleOrNull() ?: continue
                                        val result = RainRepository.updateDescuento(zona, valor, userId)
                                        if (!result) success = false
                                    }

                                    if (success) {
                                        // Actualizar caché local
                                        RainPriceManager.updatePreciosLocal(
                                            editedPrecios.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                                        )
                                        RainPriceManager.updateDescuentosLocal(
                                            editedDescuentos.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                                        )

                                        Toast.makeText(
                                            context,
                                            "Precios actualizados correctamente",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Error al guardar algunos precios",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) Color.White else RainBlue,
                            contentColor = Color.White
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}