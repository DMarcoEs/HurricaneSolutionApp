package com.example.hurricansolutionapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de captura de MEDIDAS (la que antes estaba dentro del MainActivity).
 * Aquí ya NO hay TEMP.
 *
 * Entrada: draft con datos del cliente ya capturados.
 * Salida: Cotizacion completa lista para Resumen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CotizacionFormScreen(
    draft: CotizacionDraft,
    onDraftChange: (CotizacionDraft) -> Unit,
    onContinuarResumen: (Cotizacion) -> Unit,
    onBack: () -> Unit
) {
    // Asegura que exista al menos 1 medida en el form
    LaunchedEffect(draft.ventanasForm.size) {
        if (draft.ventanasForm.isEmpty()) {
            val copy = draft.copy(ventanasForm = (draft.ventanasForm + VentanaFormState()).toMutableList())
            onDraftChange(copy)
        }
    }

    var indexActual by remember { mutableIntStateOf(0) }
    val scroll = rememberScrollState()

    val ventanas = draft.ventanasForm
    if (indexActual !in ventanas.indices) indexActual = 0
    val actual = ventanas.getOrNull(indexActual) ?: VentanaFormState()

    // Producto principal (usa el primero seleccionado)
    val productoPrincipal = draft.productosSeleccionados.firstOrNull() ?: TipoProducto.HS875

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva cotización") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ====== ÁREAS Y MEDIDAS ======
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Áreas y medidas", style = MaterialTheme.typography.titleMedium)
                    Text("Medida nueva (${indexActual + 1})", style = MaterialTheme.typography.bodyMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            enabled = indexActual > 0,
                            onClick = { indexActual-- }
                        ) { Text("← Anterior") }

                        Button(
                            enabled = indexActual < ventanas.size - 1,
                            onClick = { indexActual++ }
                        ) { Text("Siguiente →") }
                    }

                    OutlinedTextField(
                        value = actual.descripcion,
                        onValueChange = {
                            actualizarVentana(draft, indexActual, actual.copy(descripcion = it), onDraftChange)
                        },
                        label = { Text("Descripción (ej. Ventana sala)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = actual.alto,
                        onValueChange = {
                            val limpio = filtrarNumeroDecimalLocal(it)
                            actualizarVentana(draft, indexActual, actual.copy(alto = limpio), onDraftChange)
                        },
                        label = { Text("Alto (m)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = actual.ancho,
                        onValueChange = {
                            val limpio = filtrarNumeroDecimalLocal(it)
                            actualizarVentana(draft, indexActual, actual.copy(ancho = limpio), onDraftChange)
                        },
                        label = { Text("Ancho (m)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            // agrega nueva medida vacía y te manda a esa
                            val nuevaLista = (draft.ventanasForm + VentanaFormState()).toMutableList()
                            onDraftChange(draft.copy(ventanasForm = nuevaLista))
                            indexActual = nuevaLista.size - 1
                        }
                    ) {
                        Text("AGREGAR MEDIDA")
                    }
                }
            }

            // ====== ADECUACIONES ======
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Adecuaciones", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val esNo = actual.adecuacion.equals("No", true)
                        val esSi = actual.adecuacion.equals("Sí", true) || actual.adecuacion.equals("Si", true)

                        FilterChip(
                            selected = esNo,
                            onClick = {
                                actualizarVentana(draft, indexActual, actual.copy(adecuacion = "No"), onDraftChange)
                            },
                            label = { Text("No") }
                        )
                        FilterChip(
                            selected = esSi,
                            onClick = {
                                actualizarVentana(draft, indexActual, actual.copy(adecuacion = "Sí"), onDraftChange)
                            },
                            label = { Text("Sí") }
                        )
                    }
                }
            }

            // ====== TIPO DE MONTAJE ======
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tipo de montaje", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val flush = draft.tipoMontaje == "Flush Mount"
                        val trap = draft.tipoMontaje == "Trapezoidal"

                        FilterChip(
                            selected = flush,
                            onClick = { onDraftChange(draft.copy(tipoMontaje = "Flush Mount")) },
                            label = { Text("Flush Mount") }
                        )
                        FilterChip(
                            selected = trap,
                            onClick = { onDraftChange(draft.copy(tipoMontaje = "Trapezoidal")) },
                            label = { Text("Trapezoidal") }
                        )
                    }
                }
            }

            // ====== TIPO DE PRODUCTO ======
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Tipo de producto", style = MaterialTheme.typography.titleMedium)

                    val opciones = listOf(TipoProducto.HS875, TipoProducto.HS1250, TipoProducto.HS1500)
                    opciones.forEach { prod ->
                        val selected = (draft.productosSeleccionados.firstOrNull() == prod)
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                onDraftChange(draft.copy(productosSeleccionados = mutableListOf(prod)))
                            },
                            colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                        ) { Text(prod.etiqueta) }

                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            // ====== DESCUENTO ======
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Descuento por m²", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("¿Aplicar descuento?")
                            Text(
                                if (draft.aplicaDescuento) "Con descuento" else "Sin descuento",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = draft.aplicaDescuento,
                            onCheckedChange = { onDraftChange(draft.copy(aplicaDescuento = it)) }
                        )
                    }

                    OutlinedTextField(
                        value = draft.descuentoTexto,
                        onValueChange = {
                            val limpio = filtrarNumeroDecimalLocal(it)
                            onDraftChange(draft.copy(descuentoTexto = limpio))
                        },
                        enabled = draft.aplicaDescuento,
                        label = { Text("Descuento ($/m²)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ====== CONTINUAR A RESUMEN ======
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val cotizacion = construirCotizacionDesdeDraft(draft)
                    onContinuarResumen(cotizacion)
                }
            ) {
                Text("CONTINUAR A RESUMEN")
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

/**
 * Alias por si en alguna parte llamas CotizacionesFormScreen(...) (plural).
 * Así evitas "Unresolved reference" por nombres.
 */
@Composable
fun CotizacionesFormScreen(
    draft: CotizacionDraft,
    onDraftChange: (CotizacionDraft) -> Unit,
    onContinuarResumen: (Cotizacion) -> Unit,
    onBack: () -> Unit
) = CotizacionFormScreen(draft, onDraftChange, onContinuarResumen, onBack)

// -------------------- Helpers --------------------

private fun actualizarVentana(
    draft: CotizacionDraft,
    index: Int,
    nueva: VentanaFormState,
    onDraftChange: (CotizacionDraft) -> Unit
) {
    val lista = draft.ventanasForm.toMutableList()
    if (index in lista.indices) lista[index] = nueva
    onDraftChange(draft.copy(ventanasForm = lista))
}

private fun construirCotizacionDesdeDraft(draft: CotizacionDraft): Cotizacion {
    val producto = draft.productosSeleccionados.firstOrNull() ?: TipoProducto.HS875

    val descuento = if (draft.aplicaDescuento) (draft.descuentoTexto.toDoubleOrNull() ?: 0.0) else 0.0
    val ubicacion = listOf(draft.ciudad, draft.colonia, draft.direccionDetalle)
        .filter { it.isNotBlank() }
        .joinToString(", ")

    val fecha = if (draft.fecha.isNotBlank()) draft.fecha else getSpanishDateLocal()

    val ventanas = draft.ventanasForm.mapNotNull { f ->
        val alto = f.alto.toDoubleOrNull()
        val ancho = f.ancho.toDoubleOrNull()
        if (alto == null || ancho == null || f.descripcion.isBlank()) return@mapNotNull null

        Ventana(
            descripcion = f.descripcion.trim(),
            alto = alto,
            ancho = ancho,
            precioM2 = when (producto) {
                TipoProducto.HS875 -> HS875_DEFAULT_PRICE
                TipoProducto.HS1250 -> HS1250_DEFAULT_PRICE
                TipoProducto.HS1500 -> HS1500_DEFAULT_PRICE
                TipoProducto.PERSONALIZADO -> HS875_DEFAULT_PRICE
            } - descuento,
            adecuacion = f.adecuacion.ifBlank { "Por revisar" }
        )
    }

    return Cotizacion(
        id = 0L,
        folio = "",
        clienteNombre = draft.nombre,
        clienteTelefono = draft.telefono,
        ubicacion = ubicacion,
        especialista = "Especialista",
        fecha = fecha,
        producto = producto,
        productos = listOf(producto),
        tipoMontaje = draft.tipoMontaje,
        descuentoDolaresPorM2 = descuento,
        ventanas = ventanas
    )
}

private fun filtrarNumeroDecimalLocal(input: String): String {
    // deja dígitos y un solo punto
    val filtered = input.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot == -1) {
        filtered
    } else {
        filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    }
}

private fun getSpanishDateLocal(): String {
    val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "MX"))
    val raw = sdf.format(Date())
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "MX")) else it.toString() }
}
