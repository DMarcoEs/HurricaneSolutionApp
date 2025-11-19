package com.example.hurricansolutionapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hurricansolutionapp.ui.theme.HurricanSolutionAppTheme

sealed class AppScreen {
    object Login : AppScreen()
    object Home : AppScreen()
    object Historial : AppScreen()
    data class Form(val cotizacionInicial: Cotizacion? = null) : AppScreen()
    data class Resumen(val cotizacion: Cotizacion) : AppScreen()
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HurricanSolutionAppTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current

    var currentScreen by remember {
        mutableStateOf<AppScreen>(
            if (SessionManager.isLoggedIn(context)) AppScreen.Home else AppScreen.Login
        )
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {

            when (val screen = currentScreen) {

                is AppScreen.Login -> {
                    // Aquí dejamos que el back del sistema cierre la app
                    LoginScreen(
                        onLoginSuccess = {
                            currentScreen = AppScreen.Home
                        }
                    )
                }

                is AppScreen.Home -> {
                    // En Home también dejamos que el back cierre la app
                    HomeScreen(
                        onNuevaCotizacion = {
                            currentScreen = AppScreen.Form()
                        },
                        onVerHistorial = {
                            currentScreen = AppScreen.Historial
                        },
                        onLogout = {
                            SessionManager.logout(context)
                            currentScreen = AppScreen.Login
                        }
                    )
                }

                is AppScreen.Form -> {
                    // ⬅️ Back físico: regresar al Home
                    BackHandler {
                        currentScreen = AppScreen.Home
                    }

                    CotizacionFormScreen(
                        cotizacionInicial = screen.cotizacionInicial,
                        onCotizacionGenerada = { nueva ->
                            currentScreen = AppScreen.Resumen(nueva)
                        }
                    )
                }

                is AppScreen.Resumen -> {
                    // ⬅️ Back físico: volver a editar
                    BackHandler {
                        currentScreen = AppScreen.Form(screen.cotizacion)
                    }

                    ResumenScreen(
                        cotizacion = screen.cotizacion,
                        onVolver = {
                            currentScreen = AppScreen.Form(screen.cotizacion)
                        },
                        onFinalizar = {
                            // después de guardar volvemos al Home
                            currentScreen = AppScreen.Home
                        }
                    )
                }

                is AppScreen.Historial -> {
                    // ⬅️ Back físico: regresar al Home
                    BackHandler {
                        currentScreen = AppScreen.Home
                    }

                    HistorialScreen(
                        onBack = {
                            currentScreen = AppScreen.Home
                        },
                        onVerDetalle = { seleccionada ->
                            // Abrir el resumen de la cotización tocada
                            currentScreen = AppScreen.Resumen(seleccionada)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HistorialScreen(
    onBack: () -> Unit,
    onVerDetalle: (Cotizacion) -> Unit   // 👈 NUEVO
) {
    val context = LocalContext.current
    val cotizaciones = obtenerCotizacionesLocal(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Cotizaciones guardadas",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cotizaciones.isEmpty()) {
            Text(
                text = "No hay cotizaciones guardadas.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(cotizaciones) { c ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVerDetalle(c) }   // 👈 al tocar, abrimos detalle
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Cliente: ${c.clienteNombre}")
                            Text("Fecha: ${c.fecha}")
                            Text("Teléfono: ${c.clienteTelefono}")
                            Text("Total: \$${"%,.2f".format(c.total)}")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Volver")
        }
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    var nombre by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // de momento decorativo

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Hurricane Solution",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del especialista") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),   // 👈 aquí
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )


        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (nombre.isBlank()) {
                    Toast.makeText(
                        context,
                        "Ingresa tu nombre para iniciar sesión.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }
                SessionManager.login(context, nombre)
                onLoginSuccess()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("INICIAR SESIÓN")
        }
    }
}

@Composable
fun HomeScreen(
    onNuevaCotizacion: () -> Unit,
    onVerHistorial: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val nombreEspecialista = SessionManager.getEspecialista(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Bienvenido, $nombreEspecialista",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Selecciona una opción:",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = onNuevaCotizacion,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Nueva cotización")
        }

        OutlinedButton(
            onClick = onVerHistorial,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Ver cotizaciones guardadas")
        }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onLogout,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Cerrar sesión")
        }
    }
}

// 🔧 Helper para dejar solo dígitos (y opcionalmente punto)
private fun filtrarNumeroDecimal(input: String): String =
    input.filter { it.isDigit() || it == '.' }

private fun filtrarSoloDigitos(input: String): String =
    input.filter { it.isDigit() }

/**
 * 📝 Formulario de cotización
 */
@Composable
fun CotizacionFormScreen(
    cotizacionInicial: Cotizacion? = null,
    onCotizacionGenerada: (Cotizacion) -> Unit
) {
    val context = LocalContext.current

    // ---- States de los campos ----
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }

    var descVentana by remember { mutableStateOf("") }
    var altoTexto by remember { mutableStateOf("") }
    var anchoTexto by remember { mutableStateOf("") }
    var precioM2Texto by remember { mutableStateOf("") }

    var tipoProducto by remember { mutableStateOf(TipoProducto.HS875) }

    val scrollState = rememberScrollState()

    // ---- Rellenar cuando vienes de "Volver y editar" ----
    LaunchedEffect(cotizacionInicial?.id) {
        cotizacionInicial?.let { cot ->
            nombre = cot.clienteNombre
            telefono = cot.clienteTelefono
            ubicacion = cot.ubicacion
            fecha = cot.fecha

            tipoProducto = cot.producto

            val v = cot.ventanas.firstOrNull()
            if (v != null) {
                descVentana = v.descripcion
                altoTexto = v.alto.toString()
                anchoTexto = v.ancho.toString()
                precioM2Texto = v.precioM2.toString()
            } else {
                precioM2Texto = ""
            }
        }

        if (cotizacionInicial == null) {
            precioM2Texto = HS875_DEFAULT_PRICE.toString()
        }
    }

    LaunchedEffect(tipoProducto, cotizacionInicial?.id) {
        if (tipoProducto != TipoProducto.PERSONALIZADO && cotizacionInicial == null) {
            precioM2Texto = when (tipoProducto) {
                TipoProducto.HS875 -> HS875_DEFAULT_PRICE.toString()
                TipoProducto.HS1250 -> HS1250_DEFAULT_PRICE.toString()
                TipoProducto.HS1500 -> HS1500_DEFAULT_PRICE.toString()
                TipoProducto.PERSONALIZADO -> precioM2Texto
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text(
            text = "Nueva cotización",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del cliente") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = telefono,
            onValueChange = { tel -> telefono = filtrarSoloDigitos(tel) },
            label = { Text("Teléfono") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ubicacion,
            onValueChange = { ubicacion = it },
            label = { Text("Ubicación / Dirección") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = fecha,
            onValueChange = { texto ->
                val filtrado = texto.filter { it.isDigit() || it == '/' }.take(10)
                fecha = filtrado
            },
            label = { Text("Fecha (dd/MM/aaaa)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Datos del área",
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = descVentana,
            onValueChange = { descVentana = it },
            label = { Text("Descripción (ej. Ventana sala)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = altoTexto,
            onValueChange = { altoTexto = filtrarNumeroDecimal(it) },
            label = { Text("Alto (m)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = anchoTexto,
            onValueChange = { anchoTexto = filtrarNumeroDecimal(it) },
            label = { Text("Ancho (m)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Tipo de producto",
            style = MaterialTheme.typography.titleSmall
        )

        Column {
            ProductoRadioRow(
                label = TipoProducto.HS875.etiqueta,
                selected = tipoProducto == TipoProducto.HS875,
                onClick = { tipoProducto = TipoProducto.HS875 }
            )
            ProductoRadioRow(
                label = TipoProducto.HS1250.etiqueta,
                selected = tipoProducto == TipoProducto.HS1250,
                onClick = { tipoProducto = TipoProducto.HS1250 }
            )
            ProductoRadioRow(
                label = TipoProducto.HS1500.etiqueta,
                selected = tipoProducto == TipoProducto.HS1500,
                onClick = { tipoProducto = TipoProducto.HS1500 }
            )
            ProductoRadioRow(
                label = TipoProducto.PERSONALIZADO.etiqueta,
                selected = tipoProducto == TipoProducto.PERSONALIZADO,
                onClick = { tipoProducto = TipoProducto.PERSONALIZADO }
            )
        }

        OutlinedTextField(
            value = precioM2Texto,
            onValueChange = { precioM2Texto = filtrarNumeroDecimal(it) },
            label = { Text("Precio por m²") },
            enabled = tipoProducto == TipoProducto.PERSONALIZADO,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val alto = altoTexto.toDoubleOrNull()
                val ancho = anchoTexto.toDoubleOrNull()

                if (nombre.isBlank() || alto == null || ancho == null) {
                    Toast.makeText(
                        context,
                        "Revisa los datos: nombre, alto y ancho.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                val regexFecha = Regex("\\d{2}/\\d{2}/\\d{4}")
                if (!fecha.matches(regexFecha)) {
                    Toast.makeText(
                        context,
                        "Fecha inválida. Usa formato dd/MM/aaaa.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                val precioM2 = when (tipoProducto) {
                    TipoProducto.HS875 -> HS875_DEFAULT_PRICE
                    TipoProducto.HS1250 -> HS1250_DEFAULT_PRICE
                    TipoProducto.HS1500 -> HS1500_DEFAULT_PRICE
                    TipoProducto.PERSONALIZADO -> {
                        val p = precioM2Texto.toDoubleOrNull()
                        if (p == null) {
                            Toast.makeText(
                                context,
                                "Ingresa un precio por m² válido.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }
                        p
                    }
                }

                val ventana = Ventana(
                    descripcion = if (descVentana.isBlank()) "Apertura 1" else descVentana,
                    alto = alto,
                    ancho = ancho,
                    precioM2 = precioM2
                )

                val especialistaSesion = SessionManager.getEspecialista(context)

                val cotizacion = Cotizacion(
                    clienteNombre = nombre,
                    clienteTelefono = telefono,
                    ubicacion = ubicacion,
                    especialista = especialistaSesion,
                    fecha = fecha,
                    producto = tipoProducto,
                    ventanas = listOf(ventana)
                )

                onCotizacionGenerada(cotizacion)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("CONTINUAR")
        }
    }
}

@Composable
private fun ProductoRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(text = label)
    }
}

/**
 * ✅ Pantalla de resumen
 */
@Composable
fun ResumenScreen(
    cotizacion: Cotizacion,
    onVolver: () -> Unit,
    onFinalizar: () -> Unit
) {
    val context = LocalContext.current

    val detalleVentanas = buildString {
        cotizacion.ventanas.forEachIndexed { index, ventana ->
            append("Apertura ${index + 1}\n")
            append("  • Descripción: ${ventana.descripcion}\n")
            append("  • Medidas: ${ventana.alto} x ${ventana.ancho} m\n")
            append("  • Área: ${"%.2f".format(ventana.areaM2)} m²\n")
            append("  • Precio por m²: \$${"%.2f".format(ventana.precioM2)}\n")
            append("  • Subtotal: \$${"%,.2f".format(ventana.subtotal)}\n\n")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Resumen de cotización",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        OutlinedButton(
            onClick = onVolver,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text("Volver y editar")
        }

        Text(
            text = "Cliente: ${cotizacion.clienteNombre}\nTel: ${cotizacion.clienteTelefono}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Ubicación: ${cotizacion.ubicacion}\n" +
                    "Especialista: ${cotizacion.especialista}\n" +
                    "Fecha: ${cotizacion.fecha}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Producto: ${cotizacion.producto.etiqueta}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Descripción del Área:\n$detalleVentanas",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Subtotal: \$${"%,.2f".format(cotizacion.subtotal)}\n" +
                    "IVA: \$${"%,.2f".format(cotizacion.iva)}\n" +
                    "Total: \$${"%,.2f".format(cotizacion.total)}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                guardarCotizacionLocal(context, cotizacion)
                val pdfFile = generarPdfCotizacion(context, cotizacion)
                val totalGuardadas = obtenerCotizacionesLocal(context).size

                val mensaje = if (pdfFile != null) {
                    "Cotización guardada y PDF creado.\nTotal guardadas: $totalGuardadas"
                } else {
                    "Cotización guardada (error al crear PDF).\nTotal guardadas: $totalGuardadas"
                }

                Toast.makeText(
                    context,
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()

                onFinalizar()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "GUARDAR")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CotizacionFormPreview() {
    HurricanSolutionAppTheme {
        CotizacionFormScreen(onCotizacionGenerada = {})
    }
}

@Preview(showBackground = true)
@Composable
fun ResumenPreview() {
    val demo = Cotizacion(
        clienteNombre = "Esteban",
        clienteTelefono = "9840000000",
        ubicacion = "Puerto Morelos",
        especialista = "Fernando Loria",
        fecha = "30/09/2025",
        producto = TipoProducto.HS875,
        ventanas = listOf(
            Ventana(
                descripcion = "Apertura 1",
                alto = 2.5,
                ancho = 3.1,
                precioM2 = HS875_DEFAULT_PRICE
            )
        )
    )

    HurricanSolutionAppTheme {
        ResumenScreen(
            cotizacion = demo,
            onVolver = {},
            onFinalizar = {}
        )
    }
}