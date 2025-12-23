package com.example.hurricansolutionapp

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import com.example.hurricansolutionapp.HomeScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import java.util.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.net.ConnectException
import java.net.SocketTimeoutException
import androidx.activity.compose.BackHandler
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.ComponentActivity
import java.io.File
import androidx.compose.material.icons.filled.DateRange
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import org.json.JSONObject
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.hurricansolutionapp.ui.theme.HurricanSolutionAppTheme

sealed class AppScreen {
    object Login : AppScreen()
    object Home : AppScreen()
    object Historial : AppScreen()
    object PendingUploads : AppScreen()

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
    val scope = rememberCoroutineScope()
    val historialListState = rememberLazyListState()

    var currentScreen by remember {
        mutableStateOf<AppScreen>(
            if (SessionManager.isLoggedIn(context)) AppScreen.Home else AppScreen.Login
        )
    }

    BackHandler(enabled = currentScreen != AppScreen.Home && currentScreen != AppScreen.Login) {
        currentScreen = when (val s = currentScreen) {
            is AppScreen.PendingUploads -> AppScreen.Home
            is AppScreen.Historial -> AppScreen.Home
            is AppScreen.Form -> AppScreen.Home

            is AppScreen.Resumen -> {
                if (s.desdeHistorial) AppScreen.Historial
                else AppScreen.Form(s.cotizacion)
            }

            else -> AppScreen.Home
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {

            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(450)) togetherWith
                            fadeOut(animationSpec = tween(450))
                },
                label = "screen-transition"
            ) { screen ->

                when (screen) {

                    is AppScreen.Login -> {
                        LoginScreen(
                            onLoginSuccess = { currentScreen = AppScreen.Home }
                        )
                    }

                    is AppScreen.Home -> {

                        // 1) Estado del modo oscuro (solo para Home por ahora)
                        var isDarkMode by rememberSaveable { mutableStateOf(true) }

                        // 2) Datos del usuario
                        val userFirstName = remember {
                            SessionManager.getEspecialista(context)
                                .trim()
                                .split("\\s+".toRegex())
                                .firstOrNull()
                                ?: "Usuario"
                        }

                        // 3) Contador pendientes
                        val pendingCount = remember {
                            UploadQueueStorage.getAll(context)
                                .count { it.status == "PENDING" || it.status == "ERROR" }
                        }

                        // 4) HomeScreen NUEVO (el del diseño)
                        HomeScreen(
                            userFirstName = userFirstName,
                            pendingCount = pendingCount,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { isDarkMode = !isDarkMode },

                            onNuevaCotizacion = { currentScreen = AppScreen.Form(cotizacionInicial = null) },
                            onVerCotizaciones = { currentScreen = AppScreen.Historial },
                            onPendientes = { currentScreen = AppScreen.PendingUploads },

                            onCerrarSesion = {
                                SessionManager.logout(context)
                                currentScreen = AppScreen.Login
                            }
                        )
                    }

                    is AppScreen.PendingUploads -> PendingUploadsScreen(
                        onBack = { currentScreen = AppScreen.Home },
                        onRemove = { id -> UploadQueueStorage.remove(context, id) },
                        onRetryUpload = { item ->
                            if (!isOnline(context)) return@PendingUploadsScreen
                            UploadRepository.uploadOne(context, item)
                        }
                    )

                    is AppScreen.Form -> {
                        BackHandler { currentScreen = AppScreen.Home }

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
                        HistorialScreen(
                            listState = historialListState,
                            onBack = { currentScreen = AppScreen.Home },
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
}
// -----------------------------------------------------
// Historial
// -----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    listState: LazyListState,
    onBack: () -> Unit,
    onVerDetalle: (Cotizacion) -> Unit
) {
    BackHandler { onBack() }  // ✅ AQUÍ, al inicio
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
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                itemsIndexed(
                    items = cotizaciones,
                    key = { _, item -> item.id }
                ) { index, c ->

                    // Etiqueta "Cotización #X"
                    Text(
                        text = "Cotización #${index + 1}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onVerDetalle(c)
                                }
                                .padding(16.dp)
                        ) {

                            // ───── Cabecera: Cliente + Folio ─────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Cliente",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = c.clienteNombre,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (c.folio.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .background(
                                                color = Color(0xFFEDEDED),
                                                shape = RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = c.folio,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // ───── Datos del cliente ─────
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Fecha",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = c.fecha)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Teléfono",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = c.clienteTelefono)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Ubicación",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = c.ubicacion,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Número de medidas capturadas
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Medidas",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "${c.ventanas.size} medidas")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // ───── Total + acciones ─────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "$${"%,.2f".format(c.subtotal)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Ver PDF
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

                                    // Compartir PDF
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


@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // ✅ Fondo más negro, sin “azul”
            .background(Color(0xFF050505))
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(id = R.drawable.hurricane_solution_blanco),
                contentDescription = "Logo Hurricane Solution",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .height(120.dp)
                    .padding(bottom = 18.dp)
            )


            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = Color(0xB10F1116),
                tonalElevation = 0.dp,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // ✅ Menos separación entre título y subtítulo
                    Text(
                        text = "Bienvenido de nuevo",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "Inicia sesión para continuar",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = (0).dp)
                    )

                    Divider(
                        color = Color.White.copy(alpha = 0.12f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.85f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedLabelColor = Color.White.copy(alpha = 0.85f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        )
                    )

                    // ✅ separación más pareja entre campos
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = (0).dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White.copy(alpha = 0.85f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            focusedLabelColor = Color.White.copy(alpha = 0.85f),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White
                        )
                    )

                    Button(
                        onClick = {
                            errorMsg = null

                            if (!SessionManager.isLoggedIn(context) && !isOnline(context)) {
                                errorMsg = "No hay conexión a internet. Conéctate para iniciar sesión."
                                return@Button
                            }

                            if (correo.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Ingresa correo y contraseña.", Toast.LENGTH_LONG).show()
                                return@Button
                            }

                            scope.launch {
                                try {
                                    loading = true

                                    val user = AuthRepository.login(correo, password)

                                    SessionManager.login(
                                        context = context,
                                        userId = user.userId,
                                        nombre = user.nombre,
                                        role = user.role
                                    )

                                    Toast.makeText(context, "Bienvenido ${user.nombre}", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()

                                } catch (e: Exception) {

                                    val msg = (e.message ?: "").lowercase()

                                    val userFriendly = when {
                                        msg.contains("invalid login credentials") ||
                                                msg.contains("invalid_credentials") ||
                                                msg.contains("invalid_grant") -> "Usuario o contraseña incorrectos."

                                        msg.contains("usuario inactivo") -> "Tu usuario está inactivo. Contacta al administrador."

                                        e is UnknownHostException ||
                                                e is ConnectException ||
                                                e is SocketTimeoutException ||
                                                msg.contains("unable to resolve host") ||
                                                msg.contains("failed to connect") ||
                                                msg.contains("timeout") ||
                                                msg.contains("http request") -> "No hay conexión a internet. Verifica tu red e intenta de nuevo."

                                        else -> "Ocurrió un error al iniciar sesión. Intenta de nuevo."
                                    }

                                    errorMsg = userFriendly

                                } finally {
                                    loading = false
                                }
                            }

                        },
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(top = 2.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE6E6E6),
                            contentColor = Color(0xFF0C0F18),
                            disabledContainerColor = Color(0xFFBDBDBD),
                            disabledContentColor = Color(0xFF0C0F18)
                        )
                    ) {
                        Text(
                            text = if (loading) "ENTRANDO..." else "INICIAR SESIÓN",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // ✅ Texto final centrado como el de tu compañero
                    Text(
                        text = "¿Problemas para iniciar sesión?\nContacta al administrador.",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                }
            }
        }
    }
}


@Composable
private fun StatCard(
    title: String,
    value: String,
    isDark: Boolean,
    cardColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardColor.copy(alpha = if (isDark) 0.95f else 1f),
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isDark) 8.dp else 3.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(title, color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(value, color = if (isDark) Color.White else Color(0xFF111318), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BigActionCard(
    title: String,
    subtitle: String,
    leading: String,
    isDark: Boolean,
    cardColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isDark) 12.dp else 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = if (isDark) 0.35f else 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Text(leading, color = if (isDark) Color.White else Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    title,
                    color = if (isDark) Color.White else Color(0xFF111318),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    color = (if (isDark) Color.White else Color(0xFF111318)).copy(alpha = 0.60f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SmallActionCard(
    title: String,
    subtitle: String,
    leading: String,
    trailing: String,
    isDark: Boolean,
    cardColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = if (isDark) 10.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = if (isDark) 0.30f else 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Text(leading)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (isDark) Color.White else Color(0xFF111318),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = (if (isDark) Color.White else Color(0xFF111318)).copy(alpha = 0.60f),
                    fontSize = 12.sp
                )
            }

            if (trailing.isNotBlank()) {
                Text(
                    trailing,
                    color = (if (isDark) Color.White else Color(0xFF111318)).copy(alpha = 0.70f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// Fecha simple en español
private fun getSpanishDate(): String {
    val locale = Locale("es", "MX")
    val sdf = java.text.SimpleDateFormat("EEEE, d 'de' MMMM", locale)
    val text = sdf.format(java.util.Date())
    return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

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

// Campos de dirección desglosados
    var ciudad by remember { mutableStateOf("") }           // Ciudad / Localidad / Municipio
    var colonia by remember { mutableStateOf("") }          // Colonia / Fracc.
    var direccionDetalle by remember { mutableStateOf("") } // Calle, avenida, número (opcional)

    var fecha by remember { mutableStateOf(fechaHoy) }

    var precioM2Texto by remember { mutableStateOf("") }
    var tipoProducto by remember { mutableStateOf(TipoProducto.HS875) }

    // 💥 NUEVO: control del descuento
    var aplicaDescuento by remember { mutableStateOf(false) }
    var descuentoTexto by remember { mutableStateOf("") }  // dólares por m²

    var hs875Check by remember { mutableStateOf(true) }
    var hs1250Check by remember { mutableStateOf(false) }
    var hs1500Check by remember { mutableStateOf(false) }

    val ventanasForm = remember { mutableStateListOf<VentanaFormState>() }

    var descripcionActual by remember { mutableStateOf("") }
    var altoActual by remember { mutableStateOf("") }
    var anchoActual by remember { mutableStateOf("") }
    var adecuacionTipoActual by remember { mutableStateOf(AdecuacionTipo.NINGUNA) }
    var adecuacionDetalleActual by remember { mutableStateOf("") }

    var indexEditando by remember { mutableStateOf<Int?>(null) }

    val scrollState = rememberScrollState()
    var tipoMontaje by remember { mutableStateOf("Flush Mount") }


    LaunchedEffect(cotizacionInicial?.id) {
        ventanasForm.clear()

        if (cotizacionInicial != null) {
            // 🧾 Editando una cotización existente

// Datos del cliente
            nombre = cotizacionInicial.clienteNombre
            telefono = cotizacionInicial.clienteTelefono
            fecha = cotizacionInicial.fecha
            tipoProducto = cotizacionInicial.producto

// Desglosamos la ubicación guardada (separada por comas)
            val ubicacionInicial = cotizacionInicial.ubicacion
            if (ubicacionInicial.isNotBlank()) {
                val partes = ubicacionInicial
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                ciudad = partes.getOrNull(0) ?: ""
                colonia = partes.getOrNull(1) ?: ""
                direccionDetalle = partes.drop(2).joinToString(", ")
            } else {
                ciudad = ""
                colonia = ""
                direccionDetalle = ""
            }

            // Productos seleccionados
            val productosIniciales = cotizacionInicial.productos
            hs875Check = productosIniciales.contains(TipoProducto.HS875)
            hs1250Check = productosIniciales.contains(TipoProducto.HS1250)
            hs1500Check = productosIniciales.contains(TipoProducto.HS1500)

            // Descuento guardado (si existía)
            if (cotizacionInicial.descuentoDolaresPorM2 > 0.0) {
                aplicaDescuento = true
                descuentoTexto = cotizacionInicial.descuentoDolaresPorM2.toString()
            } else {
                aplicaDescuento = false
                descuentoTexto = ""
            }

            // Medidas
            cotizacionInicial.ventanas.forEach { v ->
                val (tipoAdecuacion, detalleAdecuacion) = when (v.adecuacion) {
                    "Ninguna" -> AdecuacionTipo.NINGUNA to ""
                    "Por revisar", "" -> AdecuacionTipo.POR_REVISAR to ""
                    else -> AdecuacionTipo.POR_REVISAR to v.adecuacion
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

            descripcionActual = ""
            altoActual = ""
            anchoActual = ""
            adecuacionTipoActual = AdecuacionTipo.NINGUNA
            adecuacionDetalleActual = ""
            indexEditando = null

            val v = cotizacionInicial.ventanas.firstOrNull()
            precioM2Texto = v?.precioM2?.toString() ?: ""
        } else {
// 🆕 Nueva cotización
            nombre = ""
            telefono = ""
            ciudad = ""
            colonia = ""
            direccionDetalle = ""
            fecha = fechaHoy

            descripcionActual = ""
            altoActual = ""
            anchoActual = ""
            adecuacionTipoActual = AdecuacionTipo.NINGUNA
            adecuacionDetalleActual = ""
            indexEditando = null

            precioM2Texto = HS875_DEFAULT_PRICE.toString()

            tipoProducto = TipoProducto.HS875
            hs875Check = true
            hs1250Check = false
            hs1500Check = false

            // Descuento por defecto
            aplicaDescuento = false
            descuentoTexto = ""
        }
    }


    // actualizar precio automático por producto
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

    // =================== LAYOUT PRINCIPAL ===================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Nueva cotización",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // ---------- CARD CLIENTE ----------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Cliente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del cliente") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Teléfono + Fecha en la misma fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { tel -> telefono = filtrarSoloDigitos(tel) },
                        label = { Text("Teléfono") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = fecha,
                        onValueChange = { },
                        label = { Text("Fecha") },
                        enabled = false,
                        modifier = Modifier.weight(1f)
                    )
                }

// Bloque de ubicación / dirección
                Text(
                    text = "Ubicación / Dirección",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = ciudad,
                    onValueChange = { ciudad = it },
                    label = { Text("Ciudad / Localidad / Municipio") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = colonia,
                    onValueChange = { colonia = it },
                    label = { Text("Colonia / Fraccionamiento") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = direccionDetalle,
                    onValueChange = { direccionDetalle = it },
                    label = { Text("Dirección (calle, avenida, número) - opcional") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ---------- CARD ÁREAS Y MEDIDAS (CARRUSEL + CAMPOS) ----------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Título
                Text(
                    text = "Áreas y medidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                val totalMedidas = ventanasForm.size
                val idxActual = indexEditando ?: totalMedidas

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Texto central: Medida X de Y / Medida nueva
                    val textoCentro = if (idxActual < totalMedidas) {
                        "Medida ${idxActual + 1} de $totalMedidas"
                    } else {
                        "Medida nueva (${totalMedidas + 1})"
                    }

                    Text(
                        text = textoCentro,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )

                    // Puntos del carrusel
                    if (totalMedidas > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(totalMedidas) { i ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(if (i == idxActual) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (i == idxActual)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botones largos: ANTERIOR / SIGUIENTE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (idxActual > 0) {
                                    val nuevo = idxActual - 1
                                    if (nuevo in ventanasForm.indices) {
                                        indexEditando = nuevo
                                        val v = ventanasForm[nuevo]
                                        descripcionActual = v.descripcion
                                        altoActual = v.alto
                                        anchoActual = v.ancho
                                        adecuacionTipoActual = v.adecuacionTipo
                                        adecuacionDetalleActual = v.adecuacionDetalle
                                    }
                                }
                            },
                            enabled = idxActual > 0,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Medida anterior"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Anterior")
                            }
                        }

                        Button(
                            onClick = {
                                if (idxActual < totalMedidas - 1) {
                                    val nuevo = idxActual + 1
                                    if (nuevo in ventanasForm.indices) {
                                        indexEditando = nuevo
                                        val v = ventanasForm[nuevo]
                                        descripcionActual = v.descripcion
                                        altoActual = v.alto
                                        anchoActual = v.ancho
                                        adecuacionTipoActual = v.adecuacionTipo
                                        adecuacionDetalleActual = v.adecuacionDetalle
                                    }
                                }
                            },
                            enabled = idxActual < totalMedidas - 1,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Siguiente")
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = "Siguiente medida"
                                )
                            }
                        }
                    }
                }

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

                        val numeroActualLocal =
                            indexEditando?.let { it + 1 } ?: (ventanasForm.size + 1)

                        val desc = if (descripcionActual.isBlank()) {
                            "Apertura $numeroActualLocal"
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
                        .height(48.dp)
                ) {
                    Text(if (esEdicion) "GUARDAR CAMBIOS" else "AGREGAR MEDIDA")
                }
            }
        }

        // ---------- CARD ADECUACIONES (BOTONES LARGOS) ----------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Adecuaciones",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (adecuacionTipoActual == AdecuacionTipo.NINGUNA)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                adecuacionTipoActual = AdecuacionTipo.NINGUNA
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No",
                            color =
                                if (adecuacionTipoActual == AdecuacionTipo.NINGUNA)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (adecuacionTipoActual == AdecuacionTipo.POR_REVISAR)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                adecuacionTipoActual = AdecuacionTipo.POR_REVISAR
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sí",
                            textAlign = TextAlign.Center,
                            color =
                                if (adecuacionTipoActual == AdecuacionTipo.POR_REVISAR)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (adecuacionTipoActual == AdecuacionTipo.POR_REVISAR) {
                    OutlinedTextField(
                        value = adecuacionDetalleActual,
                        onValueChange = { adecuacionDetalleActual = it },
                        label = { Text("Detalle de adecuación (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tipo de montaje",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    // FLUSH MOUNT
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (tipoMontaje == "Flush Mount")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { tipoMontaje = "Flush Mount" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Flush Mount",
                            color =
                                if (tipoMontaje == "Flush Mount")
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // TRAPEZOIDAL
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (tipoMontaje == "Trapezoidal")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { tipoMontaje = "Trapezoidal" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Trapezoidal",
                            color =
                                if (tipoMontaje == "Trapezoidal")
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }


// ---------- CARD TIPO DE PRODUCTO ----------
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tipo de producto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // HS-875
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (hs875Check)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            val nuevo = !hs875Check
                            hs875Check = nuevo
                            if (nuevo) tipoProducto = TipoProducto.HS875
                        }
                        .padding(vertical = 12.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = TipoProducto.HS875.etiqueta,
                        color = if (hs875Check)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                // HS-1250
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (hs1250Check)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            val nuevo = !hs1250Check
                            hs1250Check = nuevo
                            if (nuevo) tipoProducto = TipoProducto.HS1250
                        }
                        .padding(vertical = 12.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = TipoProducto.HS1250.etiqueta,
                        color = if (hs1250Check)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                // HS-1500
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (hs1500Check)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable {
                            val nuevo = !hs1500Check
                            hs1500Check = nuevo
                            if (nuevo) tipoProducto = TipoProducto.HS1500
                        }
                        .padding(vertical = 12.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = TipoProducto.HS1500.etiqueta,
                        color = if (hs1500Check)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        // -----------------------------------------
        //  DESCUENTO POR m²
        // -----------------------------------------

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "Descuento por m²",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("¿Aplicar descuento?", style = MaterialTheme.typography.bodyMedium)

                    Switch(
                        checked = aplicaDescuento,
                        onCheckedChange = { aplicaDescuento = it }
                    )
                }

                if (aplicaDescuento) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = descuentoTexto,
                        onValueChange = {
                            descuentoTexto = filtrarNumeroDecimal(it)
                        },
                        label = { Text("Descuento en dólares por m²") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    Text(
                        text = "Ejemplo: 5 = 5 dólares menos por m²",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "Sin descuento (0 dólares por m²).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ---------- BOTÓN FINAL ----------
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

                val productosSeleccionados = mutableListOf<TipoProducto>()
                if (hs875Check) productosSeleccionados.add(TipoProducto.HS875)
                if (hs1250Check) productosSeleccionados.add(TipoProducto.HS1250)
                if (hs1500Check) productosSeleccionados.add(TipoProducto.HS1500)

                if (productosSeleccionados.isEmpty()) {
                    Toast.makeText(
                        context,
                        "Selecciona al menos un tipo de producto.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }

                val productoPrincipal = productosSeleccionados.first()
                tipoProducto = productoPrincipal

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

                val especialistaSesion = SessionManager.getEspecialista(context)

                val folioFinal = if (cotizacionInicial != null && cotizacionInicial.folio.isNotBlank()) {
                    cotizacionInicial.folio
                } else {
                    val prefijo = especialistaSesion
                        .trim()
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() }

                    val consecutivo = FolioManager.nextFolioForPrefix(context, prefijo)

                    "$prefijo-${String.format("%04d", consecutivo)}"
                }

                // 💥 Obtenemos el descuento en dólares por m² (si aplica)
                val descuentoDolaresPorM2 = if (aplicaDescuento) {
                    descuentoTexto.toDoubleOrNull() ?: 0.0
                } else {
                    0.0
                }

                // Construimos la dirección final a partir de los 3 campos.
// Solo se usan las partes que no estén vacías.
                val partesUbicacion = listOf(ciudad, colonia, direccionDetalle)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                val ubicacionFinal = partesUbicacion.joinToString(separator = ", ")


                val cotizacion = Cotizacion(
                    id = cotizacionInicial?.id ?: 0L,
                    folio = folioFinal,
                    clienteNombre = nombre,
                    clienteTelefono = telefono,
                    ubicacion = ubicacionFinal,
                    especialista = especialistaSesion,
                    fecha = fecha,
                    producto = tipoProducto,
                    productos = productosSeleccionados,
                    tipoMontaje = tipoMontaje,

                    // 💥 NUEVO CAMPO
                    descuentoDolaresPorM2 = descuentoDolaresPorM2,

                    ventanas = listaVentanas
                )

                onCotizacionGenerada(cotizacion)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("CONTINUAR A RESUMEN")
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
        if (desdeHistorial) {
            onVolverAHistorial()
        } else if (guardado) {
            onVolverAInicio()
        } else {
            onVolverAEditar()
        }
    }

    // --------- Productos seleccionados ---------
    val productosSeleccionados: List<TipoProducto> = remember(cotizacion) {
        val lista = try {
            cotizacion.productos
        } catch (e: Exception) {
            emptyList<TipoProducto>()
        }
        if (lista.isNotEmpty()) lista.distinct() else listOf(cotizacion.producto)
    }

    fun precioM2Para(producto: TipoProducto, precioBase: Double): Double =
        when (producto) {
            TipoProducto.HS875 -> HS875_DEFAULT_PRICE
            TipoProducto.HS1250 -> HS1250_DEFAULT_PRICE
            TipoProducto.HS1500 -> HS1500_DEFAULT_PRICE
            TipoProducto.PERSONALIZADO -> precioBase
        }

    fun subtotalVentana(ventana: Ventana, producto: TipoProducto): Double {
        val area = ventana.alto * ventana.ancho
        val precioM2 = precioM2Para(producto, ventana.precioM2)
        return area * precioM2
    }

    val totalesPorProducto: Map<TipoProducto, Double> = remember(cotizacion) {
        productosSeleccionados.associateWith { p ->
            cotizacion.ventanas.sumOf { v -> subtotalVentana(v, p) }
        }
    }

    val productoPrincipal = productosSeleccionados.first()
    val totalPrincipal = totalesPorProducto[productoPrincipal] ?: 0.0

    fun formatoMoneda(valor: Double): String =
        "$" + "%,.2f".format(valor)

    // ================== LAYOUT ==================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // -------- PARTE SUPERIOR (contenido) --------
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Título
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resumen de cotización",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // -------- Tarjeta ESPECIALISTA --------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Especialista"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Especialista",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = cotizacion.especialista,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Fecha: ${cotizacion.fecha}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (cotizacion.folio.isNotBlank()) {
                        Text(
                            text = "Folio: ${cotizacion.folio}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // -------- Tarjeta CLIENTE --------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Cliente"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cliente",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = cotizacion.clienteNombre,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = "Teléfono"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cotizacion.clienteTelefono,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (cotizacion.ubicacion.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Place,
                                contentDescription = "Ubicación"
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cotizacion.ubicacion,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // -------- Tarjeta PRODUCTOS --------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.escuadra),
                            contentDescription = "Productos y medidas",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Productos y medidas",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val productosTexto = productosSeleccionados.joinToString("\n") {
                        "• ${it.etiqueta}"
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tipo de montaje: ${cotizacion.tipoMontaje}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = productosTexto,
                        style = MaterialTheme.typography.bodySmall

                    )
                }
            }

            // -------- Tarjeta TABLA --------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 320.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {

                    val columnasProducto = productosSeleccionados.size.coerceAtLeast(1)

                    val (weightDescripcion, weightArea) = when (columnasProducto) {
                        // 1 producto: hacemos más pequeñas Descripción y Área
                        // para que la columna HS-XXX se acerque al centro
                        1 -> 0.30f to 0.20f
                        // 2 productos: lo dejamos como estaba (ya se veía bien)
                        2 -> 0.36f to 0.18f
                        // 3 productos: compactas para que quepan las 3
                        else -> 0.33f to 0.17f
                    }

                    val weightProductosTotal = 1f - weightDescripcion - weightArea
                    val pesoPorProducto = weightProductosTotal / columnasProducto

                    // ENCABEZADO
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Descripción",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(weightDescripcion)
                        )
                        Text(
                            text = "Área\n(m²)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(weightArea)
                        )

                        productosSeleccionados.forEach { producto ->

                            val etiquetaHeader: String =
                                if (columnasProducto >= 3) {
                                    // 👉 Con 3 productos: "HS" arriba y número abajo
                                    when (producto) {
                                        TipoProducto.HS875 -> "HS\n875"
                                        TipoProducto.HS1250 -> "HS\n1250"
                                        TipoProducto.HS1500 -> "HS\n1500"
                                        TipoProducto.PERSONALIZADO -> "Otro"
                                    }
                                } else {
                                    // 👉 Con 1 ó 2 productos: texto normal en una sola línea
                                    when (producto) {
                                        TipoProducto.HS875 -> "HS-875"
                                        TipoProducto.HS1250 -> "HS-1250"
                                        TipoProducto.HS1500 -> "HS-1500"
                                        TipoProducto.PERSONALIZADO -> "Otro"
                                    }
                                }

                            Text(
                                text = etiquetaHeader,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (columnasProducto >= 3) 10.sp else 11.sp,
                                maxLines = if (columnasProducto >= 3) 2 else 1,
                                overflow = TextOverflow.Clip,
                                textAlign = when {
                                    columnasProducto >= 3 -> TextAlign.Center      // 3 productos: HS / 875 centrado
                                    columnasProducto == 1 -> TextAlign.Center      // 1 producto: centrado con el precio
                                    else -> TextAlign.End                          // 2 productos: lo dejamos como estaba
                                },
                                modifier = Modifier.weight(pesoPorProducto)
                            )
                        }
                    }

                    Divider()

                    // CUERPO DE LA TABLA
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (cotizacion.ventanas.isEmpty()) {
                            Text(
                                text = "Sin medidas capturadas.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn {
                                items(cotizacion.ventanas.size) { index ->
                                    val ventana = cotizacion.ventanas[index]
                                    val area = ventana.alto * ventana.ancho

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ventana.descripcion,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(weightDescripcion),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "%.2f".format(area),
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.weight(weightArea)
                                        )

                                        productosSeleccionados.forEach { producto ->
                                            val subtotal = subtotalVentana(ventana, producto)
                                            Text(
                                                text = formatoMoneda(subtotal),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = if (columnasProducto >= 3) 9.sp else 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip,
                                                textAlign = when {
                                                    columnasProducto == 1 -> TextAlign.Center
                                                    else -> TextAlign.End
                                                },
                                                modifier = Modifier.weight(pesoPorProducto)
                                            )
                                        }
                                    }
                                    Divider()
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------- PARTE INFERIOR (botones) --------
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { onVolverAEditar() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("EDITAR COTIZACIÓN")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!guardado) {
            Button(
                onClick = {
                    guardarCotizacionLocal(context, cotizacion)
                    val totalGuardadas = obtenerCotizacionesLocal(context).size
                    Toast.makeText(
                        context,
                        "Cotización guardada.\nTotal guardadas: $totalGuardadas",
                        Toast.LENGTH_LONG
                    ).show()
                    guardado = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("GUARDAR COTIZACIÓN")
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(50)
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
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("COMPARTIR")
                }
            }
        }
    }
}
private fun isOnline(context: android.content.Context): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
