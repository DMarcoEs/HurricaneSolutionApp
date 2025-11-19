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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Card
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
    data class Resumen(
        val cotizacion: Cotizacion,
        val desdeHistorial: Boolean   // 👈 solo este flag nuevo
    ) : AppScreen()
}

enum class OrigenResumen {
    NUEVA,
    HISTORIAL
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

@OptIn(ExperimentalMaterial3Api::class)
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
                    LoginScreen(
                        onLoginSuccess = {
                            currentScreen = AppScreen.Home
                        }
                    )
                }

                is AppScreen.Home -> {
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
                    BackHandler {
                        currentScreen = AppScreen.Home
                    }

                    CotizacionFormScreen(
                        cotizacionInicial = screen.cotizacionInicial,
                        onCotizacionGenerada = { nueva ->
                            // 👇 venimos del formulario, NO del historial
                            currentScreen = AppScreen.Resumen(
                                cotizacion = nueva,
                                desdeHistorial = false
                            )
                        }
                    )
                }

                is AppScreen.Resumen -> {
                    // si vienes del historial, el back debe regresar al historial
                    BackHandler {
                        currentScreen = if (screen.desdeHistorial) {
                            AppScreen.Historial
                        } else {
                            AppScreen.Form(screen.cotizacion)
                        }
                    }

                    ResumenScreen(
                        cotizacion = screen.cotizacion,
                        desdeHistorial = screen.desdeHistorial,
                        onVolver = {
                            currentScreen = if (screen.desdeHistorial) {
                                AppScreen.Historial
                            } else {
                                AppScreen.Form(screen.cotizacion)
                            }
                        },
                        onFinalizar = {
                            // Después de GUARDAR siempre te mando al Home
                            currentScreen = AppScreen.Home
                        }
                    )
                }

                is AppScreen.Historial -> {
                    BackHandler {
                        currentScreen = AppScreen.Home
                    }

                    HistorialScreen(
                        onBack = {
                            currentScreen = AppScreen.Home
                        },
                        onVerDetalle = { cotizacionSeleccionada ->
                            // 👇 ahora indicamos que el resumen viene del historial
                            currentScreen = AppScreen.Resumen(
                                cotizacion = cotizacionSeleccionada,
                                desdeHistorial = true
                            )
                        }
                    )
                }
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    onBack: () -> Unit,
    onVerDetalle: (Cotizacion) -> Unit
) {
    val context = LocalContext.current
    var cotizaciones by remember { mutableStateOf(obtenerCotizacionesLocal(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cotizaciones guardadas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        if (cotizaciones.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay cotizaciones guardadas.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = cotizaciones,
                    key = { it.id }
                ) { c ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVerDetalle(c) } // 👈 Tap en la tarjeta → ver detalle
                                .padding(12.dp)
                        ) {
                            Text("Cliente: ${c.clienteNombre}")
                            Text("Fecha: ${c.fecha}")
                            Text("Teléfono: ${c.clienteTelefono}")

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total: \$${"%,.2f".format(c.total)}")

                                // 🔴 Botón para borrar SOLO esta cotización
                                IconButton(
                                    onClick = {
                                        borrarCotizacionLocal(context, c.id)
                                        cotizaciones = obtenerCotizacionesLocal(context)

                                        Toast.makeText(
                                            context,
                                            "Cotización eliminada",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Borrar"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    var nombre by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // De momento solo visual

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Título
        Text(
            text = "Hurricane Solution",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Subtítulo
        Text(
            text = "Inicia sesión para continuar",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Nombre del especialista
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del especialista") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Contraseña (oculta)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón de iniciar sesión
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

                // Guardamos la sesión con el nombre del especialista
                SessionManager.login(
                    context = context,
                    nombreEspecialista = nombre
                )

                onLoginSuccess()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "INICIAR SESIÓN")
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

// Estado de cada apertura/ventana en el formulario
data class VentanaFormState(
    val descripcion: String = "",
    val alto: String = "",
    val ancho: String = ""
)

@Composable
fun CotizacionFormScreen(
    cotizacionInicial: Cotizacion? = null,
    onCotizacionGenerada: (Cotizacion) -> Unit
) {
    val context = LocalContext.current

    // ---- States de los campos generales ----
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf("") }

    var precioM2Texto by remember { mutableStateOf("") }
    var tipoProducto by remember { mutableStateOf(TipoProducto.HS875) }

    // 🔹 Lista dinámica de aperturas en el formulario
    val ventanasForm = remember { mutableStateListOf<VentanaFormState>() }

    val scrollState = rememberScrollState()

    // ---- Rellenar cuando vienes de "Volver y editar" ----
    LaunchedEffect(cotizacionInicial?.id) {
        // Limpia la lista primero
        ventanasForm.clear()

        cotizacionInicial?.let { cot ->
            nombre = cot.clienteNombre
            telefono = cot.clienteTelefono
            ubicacion = cot.ubicacion
            fecha = cot.fecha
            tipoProducto = cot.producto

            // Convertir cada ventana en un VentanaFormState
            cot.ventanas.forEach { v ->
                ventanasForm.add(
                    VentanaFormState(
                        descripcion = v.descripcion,
                        alto = v.alto.toString(),
                        ancho = v.ancho.toString()
                    )
                )
            }

            // Precio según la cotización original
            val v = cot.ventanas.firstOrNull()
            precioM2Texto = v?.precioM2?.toString() ?: ""
        }

        // Si es una cotización nueva
        if (cotizacionInicial == null) {
            // 1 apertura vacía por defecto
            ventanasForm.add(VentanaFormState())
            precioM2Texto = HS875_DEFAULT_PRICE.toString()
        }
    }

    // Cuando cambia el producto (y NO vienes de editar), ajustar precio por m²
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

        // ------ Datos generales del cliente ------
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

        // ------ Aperturas / Ventanas dinámicas ------
        Text(
            text = "Datos de aperturas / áreas",
            style = MaterialTheme.typography.titleMedium
        )

        // Lista de aperturas
        ventanasForm.forEachIndexed { index, ventanaState ->

            Text(
                text = "Apertura ${index + 1}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            OutlinedTextField(
                value = ventanaState.descripcion,
                onValueChange = { value ->
                    ventanasForm[index] = ventanaState.copy(descripcion = value)
                },
                label = { Text("Descripción (ej. Ventana sala)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ventanaState.alto,
                onValueChange = { value ->
                    ventanasForm[index] = ventanaState.copy(alto = filtrarNumeroDecimal(value))
                },
                label = { Text("Alto (m)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = ventanaState.ancho,
                onValueChange = { value ->
                    ventanasForm[index] = ventanaState.copy(ancho = filtrarNumeroDecimal(value))
                },
                label = { Text("Ancho (m)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Botón para eliminar SOLO esta apertura (mientras haya al menos 1)
            if (ventanasForm.size > 1) {
                TextButton(
                    onClick = {
                        ventanasForm.removeAt(index)
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Eliminar apertura")
                }
            }
        }

        // Botón para agregar una nueva apertura vacía
        OutlinedButton(
            onClick = {
                ventanasForm.add(VentanaFormState())
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Agregar otra apertura")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ------ Tipo de producto y precio ------
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

        // ------ Botón CONTINUAR ------
        Button(
            onClick = {
                // Validar datos generales
                if (nombre.isBlank()) {
                    Toast.makeText(
                        context,
                        "Ingresa el nombre del cliente.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                if (ventanasForm.isEmpty()) {
                    Toast.makeText(
                        context,
                        "Agrega al menos una apertura.",
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

                // Precio por m² según tipo de producto
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

                // Construir la lista de Ventana a partir de todas las aperturas del formulario
                val listaVentanas = mutableListOf<Ventana>()

                ventanasForm.forEachIndexed { index, v ->
                    val alto = v.alto.toDoubleOrNull()
                    val ancho = v.ancho.toDoubleOrNull()

                    if (alto == null || ancho == null) {
                        Toast.makeText(
                            context,
                            "Revisa alto/ancho de la apertura ${index + 1}.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Button
                    }

                    val desc = if (v.descripcion.isBlank()) {
                        "Apertura ${index + 1}"
                    } else {
                        v.descripcion
                    }

                    listaVentanas.add(
                        Ventana(
                            descripcion = desc,
                            alto = alto,
                            ancho = ancho,
                            precioM2 = precioM2
                        )
                    )
                }

                val especialistaSesion = SessionManager.getEspecialista(context)

                val cotizacion = Cotizacion(
                    clienteNombre = nombre,
                    clienteTelefono = telefono,
                    ubicacion = ubicacion,
                    especialista = especialistaSesion,
                    fecha = fecha,
                    producto = tipoProducto,
                    ventanas = listaVentanas
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
    desdeHistorial: Boolean,
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
            .verticalScroll(rememberScrollState())  // 👈 ahora sí baja hasta IVA/Total
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
            Text(
                if (desdeHistorial) "Volver al historial" else "Volver y editar"
            )
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

        Spacer(modifier = Modifier.height(16.dp))

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
            desdeHistorial = false,
            onVolver = {},
            onFinalizar = {}
        )
    }
}