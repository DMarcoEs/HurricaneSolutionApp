package com.example.hurricansolutionapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenScreen(
    cotizacion: Cotizacion,
    desdeHistorial: Boolean,
    onVolverAInicio: () -> Unit,
    onVolverAEditar: () -> Unit,
    onVolverAHistorial: () -> Unit
) {
    val context = LocalContext.current

    var guardado by remember { mutableStateOf(desdeHistorial) }
    var pdfFile by remember { mutableStateOf<File?>(null) }

    BackHandler {
        when {
            desdeHistorial -> onVolverAHistorial()
            guardado -> onVolverAInicio()
            else -> onVolverAEditar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumen de cotización") }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ───────── ESPECIALISTA ─────────
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Person, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Especialista", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(cotizacion.especialista, fontWeight = FontWeight.Bold)
                            Text("Fecha: ${cotizacion.fecha}")
                        }
                    }
                }

                // ───────── CLIENTE ─────────
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Person, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Cliente", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(cotizacion.clienteNombre, fontWeight = FontWeight.Bold)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Phone, null)
                                Spacer(Modifier.width(6.dp))
                                Text(cotizacion.clienteTelefono)
                            }

                            if (cotizacion.ubicacion.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Place, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(cotizacion.ubicacion)
                                }
                            }
                        }
                    }
                }

                // ───────── PRODUCTOS ─────────
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "Productos y medidas",
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("Tipo de montaje: ${cotizacion.tipoMontaje}")

                            cotizacion.productos.forEach {
                                Text("• ${it.etiqueta}")
                            }
                        }
                    }
                }

                // ───────── TABLA ─────────
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(12.dp)) {

                            if (cotizacion.ventanas.isEmpty()) {
                                Text(
                                    "Sin medidas capturadas.",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                cotizacion.ventanas.forEach { ventana ->
                                    Column {
                                        Text(ventana.descripcion, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Área: %.2f m²".format(ventana.areaM2)
                                        )
                                        Text(
                                            "Subtotal: $${"%.2f".format(
                                                ventana.subtotalPorProducto(cotizacion.producto)
                                            )}"
                                        )
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onVolverAEditar,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("EDITAR COTIZACIÓN")
            }

            Spacer(Modifier.height(8.dp))

            if (!guardado) {
                Button(
                    onClick = {
                        guardarCotizacionLocal(context, cotizacion)
                        Toast.makeText(
                            context,
                            "Cotización guardada",
                            Toast.LENGTH_SHORT
                        ).show()
                        guardado = true
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("GUARDAR COTIZACIÓN")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val file = pdfFile ?: generarPdfCotizacion(context, cotizacion)
                            if (file != null) {
                                pdfFile = file
                                verPdf(context, file)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("VER PDF")
                    }

                    OutlinedButton(
                        onClick = {
                            val file = pdfFile ?: generarPdfCotizacion(context, cotizacion)
                            if (file != null) {
                                pdfFile = file
                                compartirPdf(context, file)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("COMPARTIR")
                    }
                }
            }
        }
    }
}
