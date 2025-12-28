package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CotizacionesFormScreen(
    draft: CotizacionDraft,
    onDraftChange: (CotizacionDraft) -> Unit,
    onContinuarResumen: (Cotizacion) -> Unit,
    onBack: () -> Unit,
    isDarkMode: Boolean
) {
    val context = LocalContext.current

    var indexActual by rememberSaveable { mutableIntStateOf(0) }

    var primeraConfirmada by remember { mutableStateOf(false) }

    // ✅ Estado real de UI: lista stateful para que el TextField NO crashee y sí re-renderice
    val ventanas = remember {
        mutableStateListOf<VentanaFormState>().apply {
            addAll(draft.ventanasForm.ifEmpty { listOf(VentanaFormState()) })
        }
    }

    // Mantén el draft sincronizado (para PDF/Resumen)
    fun syncDraft() {
        draft.ventanasForm = ventanas.toMutableList()
        onDraftChange(draft)
    }

    val actual = ventanas.getOrNull(indexActual) ?: VentanaFormState()

    fun sanitizeDecimalInput(input: String): String {
        // Convierte coma a punto
        val normalized = input.replace(',', '.')

        // Deja solo dígitos y puntos
        val filtered = normalized.filter { it.isDigit() || it == '.' }

        // Permite solo un punto
        val parts = filtered.split('.', limit = 3)
        val intPart = parts.getOrNull(0).orEmpty()
        val decPart = parts.getOrNull(1).orEmpty()

        // Limita decimales (cambia 3 por 2 si quieres)
        val decLimited = decPart.take(3)

        return if (filtered.contains('.')) {
            "$intPart.$decLimited"
        } else {
            intPart
        }
    }

    fun isVentanaCompleta(v: VentanaFormState): Boolean {
        val descOk = v.descripcion.trim().isNotBlank()
        val altoOk = v.alto.replace(',', '.').toDoubleOrNull()?.let { it > 0.0 } == true
        val anchoOk = v.ancho.replace(',', '.').toDoubleOrNull()?.let { it > 0.0 } == true
        return descOk && altoOk && anchoOk
    }

    val medidasCompletasCount = ventanas.count { isVentanaCompleta(it) }
    val totalParaCarrusel = maxOf(1, medidasCompletasCount)  // mínimo 1
    val indexCarrusel = indexActual.coerceIn(0, (totalParaCarrusel - 1).coerceAtLeast(0))


    // Colores
    val bg = if (isDarkMode) Color(0xFF000000) else Color(0xFFF2F4F6)
    val surface = if (isDarkMode) Color(0xFF09090B) else Color.White
    val textPrimary = if (isDarkMode) Color.White else Color(0xFF111418)
    val textMuted = if (isDarkMode) Color(0xFF71717A) else Color(0xFF9CA3AF)
    val border = if (isDarkMode) Color(0xFF27272A) else Color(0xFFE5E7EB)

    Scaffold(
        containerColor = bg,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = surface),
                title = {
                    Text(
                        "Captura de Medidas",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(R.drawable.ic_chevron_left),
                            null,
                            tint = textPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(modifier = Modifier.navigationBarsPadding(), color = surface) {

                val textoBtn = if (!primeraConfirmada) "Agregar medida" else "Siguiente"

                Button(
                    onClick = {
                        syncDraft()

                        // ✅ 1) Si aún no hay ninguna medida completa, este botón actúa como "Agregar medida"
                        if (!primeraConfirmada) {
                            val desc = actual.descripcion.trim()
                            val alto = actual.alto.replace(',', '.').toDoubleOrNull()
                            val ancho = actual.ancho.replace(',', '.').toDoubleOrNull()

                            if (desc.isBlank()) {
                                Toast.makeText(context, "Falta descripción en Apertura #01", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (alto == null || alto <= 0.0) {
                                Toast.makeText(context, "Falta alto válido en Apertura #01", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (ancho == null || ancho <= 0.0) {
                                Toast.makeText(context, "Falta ancho válido en Apertura #01", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            primeraConfirmada = true

                            // ✅ crea una siguiente apertura vacía (no obligatoria)
                            ventanas.add(VentanaFormState())
                            indexActual = ventanas.lastIndex
                            syncDraft()
                            return@Button
                        }

                        // ✅ 2) Ya hay al menos 1 medida completa -> CONTINÚA (tu lógica original)
                        val productos = draft.productosSeleccionados
                        val productoPrincipal = productos.firstOrNull() ?: TipoProducto.HS875

                        val ventanasValidas = ventanas.mapIndexedNotNull { idx, v ->
                            val desc = v.descripcion.trim()
                            val alto = v.alto.replace(',', '.').toDoubleOrNull()
                            val ancho = v.ancho.replace(',', '.').toDoubleOrNull()

                            // Solo se toman las COMPLETAS; las vacías se ignoran
                            if (desc.isBlank() && (alto == null || alto <= 0.0) && (ancho == null || ancho <= 0.0)) {
                                return@mapIndexedNotNull null
                            }

                            if (desc.isBlank()) {
                                val num = (idx + 1).toString().padStart(2, '0')
                                Toast.makeText(context, "Falta descripción en Apertura #$num", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (alto == null || alto <= 0.0) {
                                Toast.makeText(context, "Falta alto válido en Apertura #${String.format("%02d", idx + 1)}", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (ancho == null || ancho <= 0.0) {
                                Toast.makeText(context, "Falta ancho válido en Apertura #${String.format("%02d", idx + 1)}", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            Ventana(
                                descripcion = desc,
                                alto = alto,
                                ancho = ancho,
                                precioM2 = HS875_DEFAULT_PRICE,
                                adecuacion = if (v.adecuacion == "Sí") v.adecuacionDetalle else "No"
                            )
                        }

                        if (ventanasValidas.isEmpty()) {
                            Toast.makeText(context, "Captura al menos 1 medida válida (alto y ancho)", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val ubicacion = listOf(
                            draft.ciudad.trim(),
                            draft.colonia.trim(),
                            draft.direccionDetalle.trim()
                        ).filter { it.isNotBlank() }.joinToString(", ")

                        val especialista = SessionManager.getNombre(context).ifBlank { "Especialista" }

                        val fecha = draft.fecha.ifBlank {
                            LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        }

                        val descuento = draft.descuentoTexto.replace(',', '.').toDoubleOrNull() ?: 0.0

                        val cotizacion = Cotizacion(
                            folio = FolioManager.nextFolioForEspecialista(context, especialista),
                            clienteNombre = draft.nombre.trim(),
                            clienteTelefono = draft.telefono.trim(),
                            ubicacion = ubicacion,
                            especialista = especialista,
                            fecha = fecha,
                            producto = productoPrincipal,
                            productos = if (productos.isEmpty()) listOf(productoPrincipal) else productos.toList(),
                            tipoMontaje = draft.tipoMontaje,
                            descuentoDolaresPorM2 = descuento,
                            ventanas = ventanasValidas
                        )

                        onContinuarResumen(cotizacion)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkMode) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        textoBtn,
                        color = if (isDarkMode) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        tint = if (isDarkMode) Color.Black else Color.White
                    )
                }
            }
        },

        ) {
            inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ✅ Carrusel ahora va aquí (debajo del topbar)
            MedidasCarousel(
                total = ventanas.size,
                selectedIndex = indexActual,
                onSelect = { newIndex ->
                    val maxIndex = (ventanas.size - 1).coerceAtLeast(0)
                    indexActual = newIndex.coerceIn(0, maxIndex)
                },
                isDarkMode = isDarkMode,
                textPrimary = textPrimary,
                textMuted = textMuted,
                surface = surface,
                border = border
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, border.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "APERTURA SELECCIONADA",
                                color = textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Apertura #${String.format("%02d", indexActual + 1)}",
                                color = textPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        IconButton(onClick = {
                            if (ventanas.size > 1) {
                                ventanas.removeAt(indexActual)
                                indexActual = 0
                                syncDraft()
                            }
                        }) {
                            Icon(
                                painterResource(R.drawable.ic_delete),
                                null,
                                tint = textPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = border.copy(alpha = 0.3f))

                    HorizontalDivider(thickness = 1.dp, color = border.copy(alpha = 0.3f))

                    StitchField(
                        "Descripción / Ubicación",
                        actual.descripcion,
                        "Ej. Ventana, Sala",
                        { nuevaDesc ->
                            if (indexActual in ventanas.indices) {
                                ventanas[indexActual] = ventanas[indexActual].copy(
                                    descripcion = nuevaDesc,          // ✅ ESTO ES LO QUE FALTABA
                                    adecuacion = "No",
                                    adecuacionDetalle = ""
                                )
                                syncDraft()
                            }
                        },
                        isDarkMode, textPrimary, surface, border, R.drawable.ic_notebook_pen
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.weight(1f)) {
                            StitchField(
                                "Alto (m)",
                                actual.alto,
                                "0.00",
                                { v ->
                                    val limpio = sanitizeDecimalInput(v)
                                    if (indexActual in ventanas.indices) {
                                        ventanas[indexActual] = ventanas[indexActual].copy(alto = limpio)
                                        syncDraft()
                                    }
                                },
                                isDarkMode,
                                textPrimary,
                                surface,
                                border,
                                R.drawable.ic_move_vertical,
                                keyboardType = KeyboardType.Decimal
                            )
                        }

                        Box(Modifier.weight(1f)) {
                            StitchField(
                                "Ancho (m)",
                                actual.ancho,
                                "0.00",
                                { v ->
                                    val limpio = sanitizeDecimalInput(v)
                                    if (indexActual in ventanas.indices) {
                                        ventanas[indexActual] = ventanas[indexActual].copy(ancho = limpio)
                                        syncDraft()
                                    }
                                },
                                isDarkMode,
                                textPrimary,
                                surface,
                                border,
                                R.drawable.ic_move_horizontal,
                                keyboardType = KeyboardType.Decimal
                            )
                        }
                    }

                    Text(
                        "TIPO DE MONTAJE",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                        MontajeItem(
                            label = "FLUSH MOUNT",
                            selected = (actual.tipoMontaje == "Flush Mount"),
                            modifier = Modifier.weight(1f),
                            isDarkMode = isDarkMode
                        ) {
                            if (indexActual in ventanas.indices) {
                                ventanas[indexActual] =
                                    ventanas[indexActual].copy(tipoMontaje = "Flush Mount")
                                syncDraft()
                            }
                        }

                        MontajeItem(
                            label = "TRAPEZOIDAL",
                            selected = (actual.tipoMontaje == "Trapezoidal"),
                            modifier = Modifier.weight(1f),
                            isDarkMode = isDarkMode
                        ) {
                            if (indexActual in ventanas.indices) {
                                ventanas[indexActual] =
                                    ventanas[indexActual].copy(tipoMontaje = "Trapezoidal")
                                syncDraft()
                            }
                        }
                    }

                    Text(
                        "¿REQUIERE ADECUACIONES?",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val requiere = actual.adecuacion == "Sí"
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(border.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {

                        OptionAdecuacion("No", !requiere, Modifier.weight(1f), isDarkMode) {
                            if (indexActual in ventanas.indices) {
                                ventanas[indexActual] = ventanas[indexActual].copy(
                                    adecuacion = "No",
                                    adecuacionDetalle = ""
                                )
                                syncDraft()
                            }
                        }

                        OptionAdecuacion("Sí", requiere, Modifier.weight(1f), isDarkMode) {
                            if (indexActual in ventanas.indices) {
                                ventanas[indexActual] =
                                    ventanas[indexActual].copy(adecuacion = "Sí")
                                syncDraft()
                            }
                        }
                    }

                    if (requiere) {
                        StitchField(
                            "Especifique adecuaciones",
                            actual.adecuacionDetalle,
                            "Ej. Tabla roca, madera, refuerzo estructural…",
                            { txt ->
                                if (indexActual in ventanas.indices) {
                                    ventanas[indexActual] =
                                        ventanas[indexActual].copy(adecuacionDetalle = txt)
                                    syncDraft()
                                }
                            },
                            isDarkMode, textPrimary, surface, border, R.drawable.ic_notebook_pen
                        )
                    }
                }

                if (primeraConfirmada) {
                    OutlinedButton(
                        onClick = {
                            // (Opcional recomendado) Validar que la medida actual esté completa antes de crear otra
                            val desc = actual.descripcion.trim()
                            val alto = actual.alto.replace(',', '.').toDoubleOrNull()
                            val ancho = actual.ancho.replace(',', '.').toDoubleOrNull()

                            if (desc.isBlank() || alto == null || alto <= 0.0 || ancho == null || ancho <= 0.0) {
                                Toast.makeText(context, "Completa la medida actual antes de agregar otra", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }

                            ventanas.add(VentanaFormState())
                            indexActual = ventanas.lastIndex   // ✅ SALTA a la nueva (03, 04, etc.)
                            syncDraft()
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, textPrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Add, null, tint = textPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("AGREGAR MEDIDA", color = textPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MontajeItem(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) (if (isDarkMode) Color.White else Color.Black) else (if (isDarkMode) Color(
            0xFF27272A
        ) else Color.White),
        border = if (!selected) BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) (if (isDarkMode) Color.Black else Color.White) else Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OptionAdecuacion(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) {
        if (isDarkMode) Color.Black else Color.White
    } else {
        if (isDarkMode) Color(0xFF71717A) else Color(0xFF9CA3AF)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) (if (isDarkMode) Color.White else Color.Black) else Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun MedidasCarousel(
    total: Int,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    isDarkMode: Boolean,
    textPrimary: Color,
    textMuted: Color,
    surface: Color,
    border: Color
) {
    if (total <= 0) return

    val window = 5
    val start = (selectedIndex - window / 2)
        .coerceAtLeast(0)
        .coerceAtMost((total - window).coerceAtLeast(0))
    val end = (start + window).coerceAtMost(total)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flecha izquierda
        IconButton(
            onClick = { if (selectedIndex > 0) onSelect(selectedIndex - 1) },
            enabled = selectedIndex > 0
        ) {
            Icon(
                painterResource(R.drawable.ic_chevron_left),
                null,
                tint = if (selectedIndex > 0) textMuted else textMuted.copy(alpha = 0.25f)
            )
        }

        // Pastilla
        Row(
            modifier = Modifier
                .background(
                    color = if (isDarkMode) border.copy(alpha = 0.15f) else border.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in start until end) {
                val selected = i == selectedIndex
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(if (selected) 36.dp else 28.dp)
                        .clip(CircleShape)
                        .background(if (selected) textPrimary else Color.Transparent)
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", i + 1),
                        color = if (selected) surface else textMuted,
                        fontSize = if (selected) 13.sp else 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Flecha derecha (NO tenemos ic_chevron_right, entonces reusamos left rotado)
        IconButton(
            onClick = { if (selectedIndex < total - 1) onSelect(selectedIndex + 1) },
            enabled = selectedIndex < total - 1
        ) {
            Icon(
                painterResource(R.drawable.ic_chevron_left),
                null,
                tint = if (selectedIndex < total - 1) textMuted else textMuted.copy(alpha = 0.25f),
                modifier = Modifier.rotate(180f)
            )
        }
    }
}
