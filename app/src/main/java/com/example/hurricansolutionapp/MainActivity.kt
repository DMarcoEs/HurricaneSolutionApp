package com.example.hurricansolutionapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.io.File
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.hurricansolutionapp.ui.theme.HurricanSolutionAppTheme

// -----------------------------------------------------
// Navegación
// -----------------------------------------------------

sealed class AppScreen {
    object Login : AppScreen()
    object Home : AppScreen()
    object Historial : AppScreen()
    data class Form(val cotizacionInicial: Cotizacion? = null) : AppScreen()
    data class Resumen(
        val cotizacion: Cotizacion,
        val desdeHistorial: Boolean
    ) : AppScreen()
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
                            currentScreen = AppScreen.Resumen(
                                cotizacion = nueva,
                                desdeHistorial = false
                            )
                        }
                    )
                }

                is AppScreen.Resumen -> {
                    ResumenScreen(
                        cotizacion = screen.cotizacion,
                        desdeHistorial = screen.desdeHistorial,
                        onVolverAInicio = { currentScreen = AppScreen.Home },
                        onVolverAEditar = { currentScreen = AppScreen.Form(screen.cotizacion) },
                        onVolverAHistorial = { currentScreen = AppScreen.Historial }
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

// -----------------------------------------------------
// Historial
// -----------------------------------------------------

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
                                .clickable { onVerDetalle(c) }
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
                                // 🔹 Mostrar subtotal (sin IVA)
                                Text("Total: \$${"%,.2f".format(c.subtotal)}")

                                Row {
                                    // Ver PDF directo desde historial
                                    TextButton(
                                        onClick = {
                                            val pdf = generarPdfCotizacion(context, c)
                                            if (pdf != null) {
                                                verPdf(context, pdf)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Error al generar PDF",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Text("PDF")
                                    }

                                    // Compartir PDF directo desde historial
                                    IconButton(
                                        onClick = {
                                            val pdf = generarPdfCotizacion(context, c)
                                            if (pdf != null) {
                                                compartirPdf(context, pdf)
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Error al generar PDF",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Compartir PDF"
                                        )
                                    }

                                    // Borrar cotización
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
}

// -----------------------------------------------------
// Login & Home
// -----------------------------------------------------

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    var nombre by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Inicia sesión para continuar",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del especialista") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

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

        Button(
            onClick = {
                if (nombre.isBlank() || password.isBlank()) {
                    Toast.makeText(
                        context,
                        "Ingresa nombre y contraseña.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                val user = AuthRepository.login(nombre, password)

                if (user == null) {
                    Toast.makeText(
                        context,
                        "Nombre o contraseña incorrectos.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                SessionManager.login(
                    context = context,
                    nombreEspecialista = user.correo
                )

                Toast.makeText(
                    context,
                    "Bienvenido ${user.correo}",
                    Toast.LENGTH_SHORT
                ).show()

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

// -----------------------------------------------------
// Helpers numéricos
// -----------------------------------------------------

private fun filtrarNumeroDecimal(input: String): String =
    input.filter { it.isDigit() || it == '.' }

private fun filtrarSoloDigitos(input: String): String =
    input.filter { it.isDigit() }

// -----------------------------------------------------
// Formulario de cotización (con Adecuaciones)
// -----------------------------------------------------

enum class AdecuacionTipo { NINGUNA, POR_REVISAR }

data class VentanaFormState(
    val descripcion: String = "",
    val alto: String = "",
    val ancho: String = "",
    val adecuacionTipo: AdecuacionTipo = AdecuacionTipo.NINGUNA,
    val adecuacionDetalle: String = ""
)

@Composable
fun CotizacionFormScreen(
    cotizacionInicial: Cotizacion? = null,
    onCotizacionGenerada: (Cotizacion) -> Unit
) {
    val context = LocalContext.current

    val fechaHoy = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(fechaHoy) }

    var precioM2Texto by remember { mutableStateOf("") }
    var tipoProducto by remember { mutableStateOf(TipoProducto.HS875) }

    val ventanasForm = remember { mutableStateListOf<VentanaFormState>() }

    var descripcionActual by remember { mutableStateOf("") }
    var altoActual by remember { mutableStateOf("") }
    var anchoActual by remember { mutableStateOf("") }
    var adecuacionTipoActual by remember { mutableStateOf(AdecuacionTipo.NINGUNA) }
    var adecuacionDetalleActual by remember { mutableStateOf("") }

    var indexEditando by remember { mutableStateOf<Int?>(null) }

    val scrollState = rememberScrollState()

    // Rellenar cuando vienes desde "Volver y editar"
    LaunchedEffect(cotizacionInicial?.id) {
        ventanasForm.clear()

        if (cotizacionInicial != null) {
            nombre = cotizacionInicial.clienteNombre
            telefono = cotizacionInicial.clienteTelefono
            ubicacion = cotizacionInicial.ubicacion
            fecha = cotizacionInicial.fecha
            tipoProducto = cotizacionInicial.producto

            cotizacionInicial.ventanas.forEach { v ->
                val tipoAdecuacion = if (v.adecuacion == "Ninguna") {
                    AdecuacionTipo.NINGUNA
                } else {
                    AdecuacionTipo.POR_REVISAR
                }

                val detalleAdecuacion = when (v.adecuacion) {
                    "Ninguna", "Por revisar" -> ""
                    else -> v.adecuacion
                }

                ventanasForm.add(
                    VentanaFormState(
                        descripcion = v.descripcion,
                        alto = v.alto.toString(),
                        ancho = v.ancho.toString(),
                        adecuacionTipo = tipoAdecuacion,
                        adecuacionDetalle = detalleAdecuacion
                    )
                )
            }

            val v = cotizacionInicial.ventanas.firstOrNull()
            precioM2Texto = v?.precioM2?.toString() ?: ""
        } else {
            nombre = ""
            telefono = ""
            ubicacion = ""
            fecha = fechaHoy
            descripcionActual = ""
            altoActual = ""
            anchoActual = ""
            adecuacionTipoActual = AdecuacionTipo.NINGUNA
            adecuacionDetalleActual = ""
            indexEditando = null
            precioM2Texto = HS875_DEFAULT_PRICE.toString()
        }
    }

    // Precio automático según producto (sólo nuevas)
    LaunchedEffect(tipoProducto, cotizacionInicial?.id) {
        if (cotizacionInicial == null) {
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

        // Datos cliente
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
            onValueChange = { },
            label = { Text("Fecha (automática)") },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Aperturas
        Text(
            text = "Datos de aperturas / áreas",
            style = MaterialTheme.typography.titleMedium
        )

        val numeroActual = indexEditando?.let { it + 1 } ?: (ventanasForm.size + 1)

        Text(
            text = "Medida actual: $numeroActual",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )

        OutlinedTextField(
            value = descripcionActual,
            onValueChange = { descripcionActual = it },
            label = { Text("Descripción (ej. Ventana sala)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = altoActual,
            onValueChange = { altoActual = filtrarNumeroDecimal(it) },
            label = { Text("Alto (m)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = anchoActual,
            onValueChange = { anchoActual = filtrarNumeroDecimal(it) },
            label = { Text("Ancho (m)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Adecuaciones
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Adecuaciones",
            style = MaterialTheme.typography.titleSmall
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = adecuacionTipoActual == AdecuacionTipo.NINGUNA,
                onClick = { adecuacionTipoActual = AdecuacionTipo.NINGUNA }
            )
            Text("No (Ninguna)")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = adecuacionTipoActual == AdecuacionTipo.POR_REVISAR,
                onClick = { adecuacionTipoActual = AdecuacionTipo.POR_REVISAR }
            )
            Text("Sí (hay adecuaciones)")
        }

        if (adecuacionTipoActual == AdecuacionTipo.POR_REVISAR) {
            OutlinedTextField(
                value = adecuacionDetalleActual,
                onValueChange = { adecuacionDetalleActual = it },
                label = { Text("Detalle de adecuación (opcional)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        val esEdicion = indexEditando != null

        Button(
            onClick = {
                val alto = altoActual.toDoubleOrNull()
                val ancho = anchoActual.toDoubleOrNull()

                if (alto == null || ancho == null) {
                    Toast.makeText(
                        context,
                        "Ingresa alto y ancho válidos.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                val desc = if (descripcionActual.isBlank()) {
                    "Apertura $numeroActual"
                } else {
                    descripcionActual
                }

                val nuevaVentana = VentanaFormState(
                    descripcion = desc,
                    alto = altoActual,
                    ancho = anchoActual,
                    adecuacionTipo = adecuacionTipoActual,
                    adecuacionDetalle = adecuacionDetalleActual
                )

                if (esEdicion) {
                    val idx = indexEditando!!
                    if (idx in ventanasForm.indices) {
                        ventanasForm[idx] = nuevaVentana
                        Toast.makeText(
                            context,
                            "Medida ${idx + 1} actualizada.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    ventanasForm.add(nuevaVentana)
                    Toast.makeText(
                        context,
                        "Medida ${ventanasForm.size} agregada.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                descripcionActual = ""
                altoActual = ""
                anchoActual = ""
                adecuacionTipoActual = AdecuacionTipo.NINGUNA
                adecuacionDetalleActual = ""
                indexEditando = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(if (esEdicion) "GUARDAR CAMBIOS" else "AGREGAR MEDIDA")
        }

        // Lista de medidas
        if (ventanasForm.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Medidas agregadas:",
                style = MaterialTheme.typography.titleSmall
            )

            ventanasForm.forEachIndexed { index, v ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Medida ${index + 1}: ${v.descripcion}")
                            Text(
                                "Alto: ${v.alto} m   Ancho: ${v.ancho} m",
                                style = MaterialTheme.typography.bodySmall
                            )
                            val txtAdec = when (v.adecuacionTipo) {
                                AdecuacionTipo.NINGUNA -> "Adecuación: Ninguna"
                                AdecuacionTipo.POR_REVISAR ->
                                    if (v.adecuacionDetalle.isBlank())
                                        "Adecuación: Por revisar"
                                    else
                                        "Adecuación: ${v.adecuacionDetalle}"
                            }
                            Text(
                                txtAdec,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            TextButton(
                                onClick = {
                                    indexEditando = index
                                    descripcionActual = v.descripcion
                                    altoActual = v.alto
                                    anchoActual = v.ancho
                                    adecuacionTipoActual = v.adecuacionTipo
                                    adecuacionDetalleActual = v.adecuacionDetalle
                                }
                            ) {
                                Text("Editar")
                            }

                            TextButton(
                                onClick = {
                                    ventanasForm.removeAt(index)
                                    if (indexEditando == index) {
                                        indexEditando = null
                                        descripcionActual = ""
                                        altoActual = ""
                                        anchoActual = ""
                                        adecuacionTipoActual = AdecuacionTipo.NINGUNA
                                        adecuacionDetalleActual = ""
                                    }
                                }
                            ) {
                                Text("Eliminar")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tipo de producto
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
            // 👇 Ya NO mostramos "Otro precio"
            // (TipoProducto.PERSONALIZADO se queda sólo por compatibilidad)
        }

        OutlinedTextField(
            value = precioM2Texto,
            onValueChange = { /* precio automático, no editable */ },
            label = { Text("Precio por m²") },
            enabled = false,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botón CONTINUAR
        Button(
            onClick = {
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
                        "Fecha inválida.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                val precioM2 = when (tipoProducto) {
                    TipoProducto.HS875 -> HS875_DEFAULT_PRICE
                    TipoProducto.HS1250 -> HS1250_DEFAULT_PRICE
                    TipoProducto.HS1500 -> HS1500_DEFAULT_PRICE
                    TipoProducto.PERSONALIZADO ->
                        precioM2Texto.toDoubleOrNull() ?: HS875_DEFAULT_PRICE
                }

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

                    val textoAdecuacion = when (v.adecuacionTipo) {
                        AdecuacionTipo.NINGUNA -> "Ninguna"
                        AdecuacionTipo.POR_REVISAR ->
                            if (v.adecuacionDetalle.isBlank()) "Por revisar"
                            else v.adecuacionDetalle
                    }

                    listaVentanas.add(
                        Ventana(
                            descripcion = desc,
                            alto = alto,
                            ancho = ancho,
                            precioM2 = precioM2,
                            adecuacion = textoAdecuacion
                        )
                    )
                }

                // 👉 Hasta aquí sólo llenamos listaVentanas
                //    Ahora sí armamos el objeto Cotizacion

                val especialistaSesion = SessionManager.getEspecialista(context)

                // Folio:
                // - Si vienes de "Volver y editar", conservamos el mismo folio
                // - Si es una cotización nueva, generamos uno nuevo para ese especialista
                val folioStr = if (cotizacionInicial != null && cotizacionInicial.folio.isNotBlank()) {
                    cotizacionInicial.folio
                } else {
                    FolioManager.nextFolioForEspecialista(
                        context = context,
                        nombreCompleto = especialistaSesion
                    )
                }

                val cotizacion = Cotizacion(
                    id = cotizacionInicial?.id ?: 0L,   // si editas, conserva el id
                    folio = folioStr,                   // 👈 MUY IMPORTANTE
                    clienteNombre = nombre,
                    clienteTelefono = telefono,
                    ubicacion = ubicacion,
                    especialista = especialistaSesion,
                    fecha = fecha,
                    producto = tipoProducto,
                    ventanas = listaVentanas,
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

// -----------------------------------------------------
// Resumen + PDF
// -----------------------------------------------------

@Composable
fun ResumenScreen(
    cotizacion: Cotizacion,
    desdeHistorial: Boolean,
    onVolverAInicio: () -> Unit,
    onVolverAEditar: () -> Unit,
    onVolverAHistorial: () -> Unit
) {
    val context = LocalContext.current

    // 👉 Si viene del historial, ya está guardada
    var guardado by remember { mutableStateOf(desdeHistorial) }

    // 👉 Para no generar el PDF muchas veces
    var pdfFile by remember { mutableStateOf<File?>(null) }

    // 👉 Comportamiento del botón físico "atrás"
    BackHandler {
        if (desdeHistorial) {
            // Vino desde Historial → regresar a Historial
            onVolverAHistorial()
        } else if (guardado) {
            // Nueva cotización pero YA guardada → ir al Home
            onVolverAInicio()
        } else {
            // Nueva cotización y NO guardada → volver a editar
            onVolverAEditar()
        }
    }

    // Total sin IVA
    val totalSinIva = cotizacion.subtotal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {

        // Título
        Text(
            text = "Resumen de cotización",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Botón volver (EDITAR o HISTORIAL, según origen)
        OutlinedButton(
            onClick = {
                if (desdeHistorial) {
                    onVolverAHistorial()
                } else {
                    onVolverAEditar()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                if (desdeHistorial) "Volver al historial" else "Volver y editar"
            )
        }

        // ---- Datos del cliente / encabezado ----
        Text(
            text = "Cliente: ${cotizacion.clienteNombre}\n" +
                    "Tel: ${cotizacion.clienteTelefono}",
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

        // -------- TABLA TIPO EXCEL --------
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Encabezados
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Descripción",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.4f)
                    )
                    Text(
                        text = "Área total (m²)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.2f)
                    )
                    Text(
                        text = "Tipo de montaje",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.2f)
                    )
                    Text(
                        text = "Costo",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.2f)
                    )
                }

                Divider()

                // Filas: una por cada apertura
                cotizacion.ventanas.forEach { ventana ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ventana.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.4f)
                        )

                        Text(
                            text = "%.2f".format(ventana.areaM2),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.2f)
                        )

                        Text(
                            text = cotizacion.producto.etiqueta,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.2f)
                        )

                        Text(
                            text = "$" + "%,.2f".format(ventana.subtotal),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.2f)
                        )
                    }
                }

                Divider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

                // Fila de TOTAL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Total:  ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$" + "%,.2f".format(totalSinIva),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ---- ZONA DE BOTONES SEGÚN ESTADO ----
        if (!guardado) {
            // 🔹 PRIMERA VEZ: solo botón GUARDAR
            Button(
                onClick = {
                    guardarCotizacionLocal(context, cotizacion)
                    val totalGuardadas = obtenerCotizacionesLocal(context).size

                    Toast.makeText(
                        context,
                        "Cotización guardada.\nTotal guardadas: $totalGuardadas",
                        Toast.LENGTH_LONG
                    ).show()

                    guardado = true      // 👉 Ocultamos GUARDAR y mostramos PDF/Compartir
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "GUARDAR")
            }
        } else {
            // 🔹 YA GUARDADA: botones PDF y COMPARTIR
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val archivo = pdfFile ?: generarPdfCotizacion(context, cotizacion)
                        if (archivo != null) {
                            pdfFile = archivo
                            verPdf(context, archivo)
                        } else {
                            Toast.makeText(
                                context,
                                "Error al generar el PDF.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("VER PDF")
                }

                OutlinedButton(
                    onClick = {
                        val archivo = pdfFile ?: generarPdfCotizacion(context, cotizacion)
                        if (archivo != null) {
                            pdfFile = archivo
                            compartirPdf(context, archivo)
                        } else {
                            Toast.makeText(
                                context,
                                "Error al generar el PDF.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("COMPARTIR")
                }
            }
        }
    }
}
// -----------------------------------------------------
// Previews
// -----------------------------------------------------

@Preview(showBackground = true)
@Composable
fun CotizacionFormPreview() {
    HurricanSolutionAppTheme {
        CotizacionFormScreen(onCotizacionGenerada = {})
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true)
@Composable
fun ResumenPreview() {
    val demo = Cotizacion(
        folio = "DEMO-0001",
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
            onVolverAInicio = {},
            onVolverAEditar = {},
            onVolverAHistorial = {}
        )
    }
}