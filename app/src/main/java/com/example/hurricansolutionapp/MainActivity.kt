package com.example.hurricansolutionapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.io.File
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    var password by remember { mutableStateOf("") }

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
                if (nombre.isBlank() || password.isBlank()) {
                    Toast.makeText(
                        context,
                        "Ingresa nombre y contraseña.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                // ✅ Validar contra la lista fija de especialistas
                val user = AuthRepository.login(nombre, password)

                if (user == null) {
                    // ❌ Credenciales incorrectas
                    Toast.makeText(
                        context,
                        "Nombre o contraseña incorrectos.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                // ✅ Login correcto → guardamos en SessionManager
                SessionManager.login(
                    context = context,
                    nombreEspecialista = user.nombre
                )

                Toast.makeText(
                    context,
                    "Bienvenido ${user.nombre}",
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

// 🔧 Helper para dejar solo dígitos (y opcionalmente punto)
private fun filtrarNumeroDecimal(input: String): String =
    input.filter { it.isDigit() || it == '.' }

private fun filtrarSoloDigitos(input: String): String =
    input.filter { it.isDigit() }

/**
 * 📝 Formulario de cotización
 */

// Estado de cada apertura/ventana en el formulario
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

    // ---- fecha automática de hoy ----
    val fechaHoy = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    // ---- States de los campos generales ----
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(fechaHoy) }

    var precioM2Texto by remember { mutableStateOf("") }
    var tipoProducto by remember { mutableStateOf(TipoProducto.HS875) }

    // 🔹 Lista de TODAS las aperturas guardadas
    val ventanasForm = remember { mutableStateListOf<VentanaFormState>() }

    // 🔹 Campos de la APERTURA ACTUAL (una sola caja)
    var descripcionActual by remember { mutableStateOf("") }
    var altoActual by remember { mutableStateOf("") }
    var anchoActual by remember { mutableStateOf("") }

    // 🔹 Índice que estoy editando (null = creando nueva)
    var indexEditando by remember { mutableStateOf<Int?>(null) }

    val scrollState = rememberScrollState()

    // ---- Rellenar cuando vienes de "Volver y editar" ----
    LaunchedEffect(cotizacionInicial?.id) {
        ventanasForm.clear()

        if (cotizacionInicial != null) {
            nombre = cotizacionInicial.clienteNombre
            telefono = cotizacionInicial.clienteTelefono
            ubicacion = cotizacionInicial.ubicacion
            fecha = cotizacionInicial.fecha
            tipoProducto = cotizacionInicial.producto

            cotizacionInicial.ventanas.forEach { v ->
                ventanasForm.add(
                    VentanaFormState(
                        descripcion = v.descripcion,
                        alto = v.alto.toString(),
                        ancho = v.ancho.toString()
                    )
                )
            }

            val v = cotizacionInicial.ventanas.firstOrNull()
            precioM2Texto = v?.precioM2?.toString() ?: ""
        } else {
            // Nueva cotización
            nombre = ""
            telefono = ""
            ubicacion = ""
            fecha = fechaHoy
            descripcionActual = ""
            altoActual = ""
            anchoActual = ""
            indexEditando = null
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

        // 🔹 Fecha solo de lectura (automática)
        OutlinedTextField(
            value = fecha,
            onValueChange = { /* no editable */ },
            label = { Text("Fecha (automática)") },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ------ Aperturas / Ventanas en UNA sola caja ------
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
                    ancho = anchoActual
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

                // Limpiar campos y salir de modo edición
                descripcionActual = ""
                altoActual = ""
                anchoActual = ""
                indexEditando = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(if (esEdicion) "GUARDAR CAMBIOS" else "AGREGAR MEDIDA")
        }

        // 🔹 Lista compacta de medidas agregadas (para ver, editar, borrar)
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
                        }

                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            TextButton(
                                onClick = {
                                    // Cargar esta medida en la caja para editar
                                    indexEditando = index
                                    descripcionActual = v.descripcion
                                    altoActual = v.alto
                                    anchoActual = v.ancho
                                }
                            ) {
                                Text("Editar")
                            }

                            TextButton(
                                onClick = {
                                    ventanasForm.removeAt(index)
                                    // Si estaba editando y se borra, salir de edición
                                    if (indexEditando == index) {
                                        indexEditando = null
                                        descripcionActual = ""
                                        altoActual = ""
                                        anchoActual = ""
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

                // Precio por m²
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

                // Construir la lista de Ventana a partir de TODAS las aperturas guardadas
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

    // 👉 NUEVOS ESTADOS PARA EL PDF
    var ultimoPdfGenerado by remember { mutableStateOf<File?>(null) }
    var mostrarDialogoPdf by remember { mutableStateOf(false) }

    // Total sin mostrar IVA por separado (usamos el subtotal)
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

        // Botón volver (historial o editar)
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

        // ---- Botón GUARDAR ----
        Button(
            onClick = {
                guardarCotizacionLocal(context, cotizacion)
                val pdfFile = generarPdfCotizacion(context, cotizacion)
                val totalGuardadas = obtenerCotizacionesLocal(context).size

                if (pdfFile != null) {
                    ultimoPdfGenerado = pdfFile
                    mostrarDialogoPdf = true
                    Toast.makeText(
                        context,
                        "Cotización guardada y PDF creado.\nTotal guardadas: $totalGuardadas",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Cotización guardada (error al crear PDF).\nTotal guardadas: $totalGuardadas",
                        Toast.LENGTH_LONG
                    ).show()
                }
                // 👀 Ya NO llamo a onFinalizar() aquí, para que puedas ver/compartir primero.
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "GUARDAR")
        }
    }

    // 👉 ESTE BLOQUE VA **FUERA** DEL COLUMN, PERO DENTRO DE ResumenScreen
    if (mostrarDialogoPdf && ultimoPdfGenerado != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPdf = false },
            title = { Text("PDF generado") },
            text = { Text("¿Qué deseas hacer con la cotización en PDF?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoPdf = false
                        verPdf(context, ultimoPdfGenerado!!)
                    }
                ) {
                    Text("Ver PDF")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoPdf = false
                        compartirPdf(context, ultimoPdfGenerado!!)
                    }
                ) {
                    Text("Compartir")
                }
            }
        )
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