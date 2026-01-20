package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Visibility
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
    onNavigateToResumen: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var datosList by remember { mutableStateOf<List<InstaladorDatos>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var generatingPdfForFolio by remember { mutableStateOf<String?>(null) }

    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF3F4F6)
    val cardBg = if (isDarkMode) Color(0xFF111111) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF9CA3AF) else Color(0xFF6B7280)
    val inputBg = if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFF0F0F0)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)

    val userId = remember { SessionManager.getUserId(context) }
    val userRole = remember { SessionManager.getRole(context) }

    // Función para cargar datos con logs detallados
    fun cargarDatos() {
        scope.launch {
            try {
                if (!isLoading) isRefreshing = true
                error = null

                android.util.Log.d("InstaladorMedidas", "========================================")
                android.util.Log.d("InstaladorMedidas", "🔍 Cargando datos para instalador")
                android.util.Log.d("InstaladorMedidas", "📋 User ID: $userId")
                android.util.Log.d("InstaladorMedidas", "📋 User Role: $userRole")

                val result = InstaladorRepository.getDatosForInstalador(userId)

                if (result.isSuccess) {
                    datosList = result.getOrNull() ?: emptyList()
                    android.util.Log.d("InstaladorMedidas", "✅ Registros encontrados: ${datosList.size}")
                    datosList.forEach { datos ->
                        android.util.Log.d("InstaladorMedidas", "  → Folio: ${datos.folio}, Cliente: ${datos.nombreCliente}, InstaladorID: ${datos.instaladorId}")
                    }
                } else {
                    error = "Error al cargar: ${result.exceptionOrNull()?.message}"
                    android.util.Log.e("InstaladorMedidas", "❌ Error: ${result.exceptionOrNull()?.message}")
                }
                android.util.Log.d("InstaladorMedidas", "========================================")
            } catch (e: Exception) {
                error = "Error: ${e.message}"
                android.util.Log.e("InstaladorMedidas", "❌ Excepción: ${e.message}", e)
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    // Función para generar y mostrar PDF usando verPdf() de PdfUtils
    fun generarYMostrarPdf(datos: InstaladorDatos) {
        scope.launch {
            generatingPdfForFolio = datos.folio
            try {
                android.util.Log.d("InstaladorMedidas", "========================================")
                android.util.Log.d("InstaladorMedidas", "📄 Generando PDF para: ${datos.folio}")
                android.util.Log.d("InstaladorMedidas", "📄 Datos ID: ${datos.id}")

                // Obtener medidas
                val medidasResult = InstaladorRepository.getMedidasByDatosId(datos.id)
                val medidas = if (medidasResult.isSuccess) {
                    medidasResult.getOrNull() ?: emptyList()
                } else {
                    android.util.Log.e("InstaladorMedidas", "❌ Error obteniendo medidas: ${medidasResult.exceptionOrNull()?.message}")
                    emptyList()
                }

                android.util.Log.d("InstaladorMedidas", "📋 Medidas encontradas: ${medidas.size}")
                medidas.forEach { m ->
                    android.util.Log.d("InstaladorMedidas", "  → ${m.descripcion}: ${m.alto}x${m.ancho}")
                }

                // Crear Cotizacion dummy para el generador de PDF
                val cotizacionDummy = Cotizacion(
                    id = datos.cotizacionId ?: 0,
                    folio = datos.folio,
                    clienteNombre = datos.nombreCliente,
                    clienteTelefono = datos.telefonoCliente ?: "",
                    ubicacion = datos.getDireccionSegura(),
                    ciudad = datos.getCiudadSegura(),
                    especialista = datos.getEspecialistaNombreSeguro(),
                    fecha = "",
                    producto = TipoProducto.HS875,
                    ventanas = medidas.map { m ->
                        Ventana(
                            zona = m.getZonaSegura(),
                            descripcion = m.descripcion,
                            alto = m.alto,
                            ancho = m.ancho,
                            precioM2 = 0.0,
                            adecuacion = if (m.requiereAdecuacion) m.getAdecuacionDetalleSeguro() else "No",
                            tipoMontaje = m.getTipoMontajeSeguro()
                        )
                    }
                )

                // Generar PDF
                val pdfFile = PdfInstaladorGenerator.generarPdfOrdenInstalacion(
                    context = context,
                    cotizacion = cotizacionDummy,
                    sistemaSeleccionado = datos.sistemaSeleccionado,
                    instaladorDatos = datos,
                    medidasRectificadas = medidas
                )

                android.util.Log.d("InstaladorMedidas", "PDF generado: ${pdfFile?.absolutePath}, existe: ${pdfFile?.exists()}")

                if (pdfFile != null && pdfFile.exists()) {
                    // Usar verPdf() de PdfUtils que ya tiene el FileProvider correcto
                    verPdf(context, pdfFile)
                    Toast.makeText(context, "✓ PDF generado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error generando PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("InstaladorMedidas", "Error general: ${e.message}", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                generatingPdfForFolio = null
            }
        }
    }

    // Cargar al inicio
    LaunchedEffect(Unit) {
        cargarDatos()
    }

    val filteredList = remember(datosList, searchQuery) {
        if (searchQuery.isBlank()) datosList
        else datosList.filter {
            it.nombreCliente.contains(searchQuery, ignoreCase = true) ||
                    it.folio.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            StitchTopBarWithDivider(
                title = "Medidas Asignadas",
                onBack = onBack,
                isDarkMode = isDarkMode,
                actions = {
                    // Botón de refresh
                    IconButton(
                        onClick = { cargarDatos() },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = textPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Recargar",
                                tint = textPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Barra de búsqueda
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
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Limpiar", tint = textMuted)
                        }
                    }
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
                    ) {
                        CircularProgressIndicator(color = textPrimary, strokeWidth = 2.dp)
                    }

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
                            if (searchQuery.isNotBlank()) "No se encontraron resultados"
                            else "No hay medidas asignadas",
                            color = textMuted,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Las medidas aparecerán aquí cuando te sean asignadas",
                                color = textMuted.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                "${filteredList.size} instalación${if (filteredList.size != 1) "es" else ""}",
                                color = textMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(filteredList, key = { it.id }) { datos ->
                            InstaladorMedidaCardWithButtons(
                                datos = datos,
                                isDarkMode = isDarkMode,
                                cardBg = cardBg,
                                textPrimary = textPrimary,
                                textMuted = textMuted,
                                border = border,
                                isGeneratingPdf = generatingPdfForFolio == datos.folio,
                                onVerResumen = { onNavigateToResumen(datos.folio) },
                                onVerPdf = { generarYMostrarPdf(datos) }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstaladorMedidaCardWithButtons(
    datos: InstaladorDatos,
    isDarkMode: Boolean,
    cardBg: Color,
    textPrimary: Color,
    textMuted: Color,
    border: Color,
    isGeneratingPdf: Boolean,
    onVerResumen: () -> Unit,
    onVerPdf: () -> Unit
) {
    // Estado: Verde si rectificadas, Gris oscuro si pendiente
    val statusColor = if (datos.rectificadas) Color(0xFF10B981) else Color(0xFF6B7280)
    val statusText = if (datos.rectificadas) "RECTIFICADAS" else "PENDIENTE"
    val statusBgColor = if (datos.rectificadas) {
        Color(0xFF10B981).copy(alpha = 0.15f)
    } else {
        if (isDarkMode) Color(0xFF374151) else Color(0xFFE5E7EB)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Folio + Badge Sistema
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    datos.folio,
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                // Badge del sistema (negro)
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        datos.sistemaSeleccionado.getSistemaDisplayName(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Nombre del cliente
            Text(
                datos.nombreCliente,
                color = textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Ubicación
            val ubicacion = buildString {
                if (datos.getCiudadSegura().isNotBlank()) append(datos.getCiudadSegura())
                if (datos.getColoniaSegura().isNotBlank()) {
                    if (isNotBlank()) append(", ")
                    append(datos.getColoniaSegura())
                }
                if (datos.getDireccionSegura().isNotBlank()) {
                    if (isNotBlank()) append(", ")
                    append(datos.getDireccionSegura())
                }
            }
            if (ubicacion.isNotBlank()) {
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
                        ubicacion,
                        color = textMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Badge de estado
            Surface(
                color = statusBgColor,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    statusText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            // Botones: Ver Resumen y PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón Ver Resumen (outline)
                OutlinedButton(
                    onClick = onVerResumen,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Ver Resumen",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Botón PDF (filled negro)
                Button(
                    onClick = onVerPdf,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    ),
                    enabled = !isGeneratingPdf
                ) {
                    if (isGeneratingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "PDF",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}