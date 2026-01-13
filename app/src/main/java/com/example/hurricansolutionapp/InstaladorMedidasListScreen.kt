package com.example.hurricansolutionapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstaladorMedidasListScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onNavigateToForm: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var datosList by remember { mutableStateOf<List<InstaladorDatos>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val inputBg = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)

    val userId = remember { SessionManager.getUserId(context) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                error = null
                val result = InstaladorRepository.getDatosForInstalador(userId)
                if (result.isSuccess) {
                    datosList = result.getOrNull() ?: emptyList()
                } else {
                    error = "Error al cargar: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                error = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    val filteredList = remember(datosList, searchQuery) {
        if (searchQuery.isBlank()) datosList
        else datosList.filter {
            it.nombreCliente.contains(
                searchQuery,
                ignoreCase = true
            ) || it.folio.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBarWithDivider(
                title = "Medidas Asignadas",
                onBack = onBack,
                isDarkMode = isDarkMode
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = {
                    Text(
                        "Buscar por nombre o folio...",
                        color = textMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) IconButton(onClick = {
                        searchQuery = ""
                    }) { Icon(Icons.Default.Clear, "Limpiar", tint = textMuted) }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = inputBg,
                    unfocusedContainerColor = inputBg,
                    focusedBorderColor = border,
                    unfocusedBorderColor = border,
                    focusedTextColor = textPrimary,
                    unfocusedTextColor = textPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = textPrimary, strokeWidth = 2.dp) }

                    error != null -> Column(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Outlined.ErrorOutline,
                            null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            error ?: "Error",
                            color = textMuted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    filteredList.isEmpty() -> Column(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Straighten,
                            null,
                            tint = textMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "No se encontraron resultados" else "No hay medidas asignadas",
                            color = textMuted,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(Modifier.height(8.dp)); Text(
                                "Las medidas aparecerán aquí cuando te sean asignadas",
                                color = textMuted.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> LazyColumn(
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ), verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "${filteredList.size} medida${if (filteredList.size != 1) "s" else ""}",
                                color = textMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(filteredList, key = { it.id }) { datos ->
                            InstaladorMedidaCard(
                                datos = datos,
                                isDarkMode = isDarkMode,
                                cardBg = cardBg,
                                textPrimary = textPrimary,
                                textMuted = textMuted,
                                border = border,
                                onClick = { onNavigateToForm(datos.folio) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstaladorMedidaCard(
    datos: InstaladorDatos,
    isDarkMode: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textMuted: Color,
    border: Color,
    onClick: () -> Unit
) {
    val statusColor = if (datos.rectificadas) Color(0xFF10B981) else Color(0xFFF59E0B)
    val statusText = if (datos.rectificadas) "RECTIFICADAS" else "PENDIENTE"
    val direccion = datos.getDireccionSegura()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    datos.folio,
                    color = textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Text(
                datos.nombreCliente,
                color = textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (direccion.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_map_pin_lucide),
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        direccion,
                        color = textMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        datos.sistemaSeleccionado.getSistemaDisplayName(),
                        color = textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}