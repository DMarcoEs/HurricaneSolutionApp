package com.example.hurricansolutionapp

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch


private const val ANIMATION_DURATION = 350

private fun enterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(ANIMATION_DURATION)
    ) + slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth / 4 },
        animationSpec = tween(ANIMATION_DURATION)
    )
}

private fun exitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(ANIMATION_DURATION / 2)
    )
}

private fun popEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(ANIMATION_DURATION)
    )
}

private fun popExitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(ANIMATION_DURATION / 2)
    ) + slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth / 4 },
        animationSpec = tween(ANIMATION_DURATION)
    )
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    context: Context,
    scope: CoroutineScope,
    cotizacionDraft: CotizacionDraft,
    isDarkMode: Boolean,
    setDarkMode: (Boolean) -> Unit,
    online: Boolean
) {
    // Determinar pantalla inicial basada en rol
    val userRole = SessionManager.getRole(context)
    val isAdmin = userRole == "ADMIN"
    val isInstaller = userRole == "INSTALLER"

    val start = when {
        !SessionManager.isLoggedIn(context) -> Routes.LOGIN
        isInstaller -> Routes.INSTALADOR_HOME
        isAdmin -> Routes.ADMIN_HOME
        else -> Routes.HOME
    }

    var cotizacionActual by remember { mutableStateOf<Cotizacion?>(null) }
    var desdeHistorial by remember { mutableStateOf(false) }
    var editandoDesdeHistorial by remember { mutableStateOf(false) }
    var huboEdicionMedidas by remember { mutableStateOf(false) }
    var cotizacionRemotaSeleccionada by remember { mutableStateOf<CotizacionRemota?>(null) }

    // ═══════════════════════════════════════════════════════════════════════════
    // RAIN PROTECTION - DRAFT
    // ═══════════════════════════════════════════════════════════════════════════
    var rainDraft by remember { mutableStateOf(CotizacionRainDraft()) }
    var cotizacionRainActual by remember { mutableStateOf<CotizacionRain?>(null) }

    // Mover historialListState aquí (fuera del NavHost)
    val historialListState = rememberLazyListState()

    // Cargar precios al iniciar (para todos los usuarios)
    LaunchedEffect(Unit) {
        if (SessionManager.isLoggedIn(context)) {
            PriceManager.loadPrices()
            RainPriceManager.loadPrecios()
        }
    }

    NavHost(
        navController = navController,
        startDestination = start
    ) {

        // ═══════════════════════════════════════════════════════════════════════════
        // LOGIN
        // ═══════════════════════════════════════════════════════════════════════════

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    scope.launch {
                        PriceManager.loadPrices()
                        RainPriceManager.loadPrecios()
                    }
                    val role = SessionManager.getRole(context)
                    val destination = when (role) {
                        "ADMIN" -> Routes.ADMIN_HOME
                        "INSTALLER" -> Routes.INSTALADOR_HOME
                        else -> Routes.HOME
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // SELECCIÓN DE PRODUCTO
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.SELECCION_PRODUCTO,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            SeleccionProductoScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onSelectHurricane = {
                    // Flujo Hurricane: va a selección de cliente
                    navController.navigate(Routes.SELECCION_CLIENTE) {
                        launchSingleTop = true
                    }
                },
                onSelectRain = {
                    // Flujo Rain: va a selección de cliente Rain
                    rainDraft = CotizacionRainDraft()
                    navController.navigate(Routes.RAIN_CLIENTE) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // HOME (ESPECIALISTA)
        // ═══════════════════════════════════════════════════════════════════════════

        composable(Routes.HOME) {
            HomeScreen(
                userFirstName = SessionManager.getNombre(context),
                pendingCount = UploadQueueStorage.getAll(context)
                    .filter { it.status != "DONE" }.size,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { setDarkMode(!isDarkMode) },
                onNuevaCotizacion = {
                    desdeHistorial = false
                    editandoDesdeHistorial = false
                    huboEdicionMedidas = false
                    cotizacionDraft.clear()
                    rainDraft = CotizacionRainDraft()
                    navController.navigate(Routes.SELECCION_PRODUCTO) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onVerCotizaciones = {
                    navController.navigate(Routes.HISTORIAL) { launchSingleTop = true }
                },
                onPendientes = { navController.navigate(Routes.PENDIENTES) },
                onPendientesDrive = { navController.navigate(Routes.PENDIENTES_DRIVE) },
                onEnviosInstalacion = { navController.navigate(Routes.ENVIOS_INSTALACION) },
                logoutEnabled = online,
                onCerrarSesion = {
                    scope.launch {
                        if (!isOnline(context)) return@launch
                        try {
                            AuthRepository.logout()
                        } catch (_: Exception) {
                            return@launch
                        }
                        SessionManager.logout(context)
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = Routes.ENVIOS_INSTALACION,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            EnviosInstalacionScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ADMIN HOME
        // ═══════════════════════════════════════════════════════════════════════════

        composable(Routes.ADMIN_HOME) {
            AdminHomeScreen(
                adminName = SessionManager.getNombre(context),
                pendingCount = UploadQueueStorage.getAll(context)
                    .filter { it.status != "DONE" }.size,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { setDarkMode(!isDarkMode) },
                onNuevaCotizacion = {
                    desdeHistorial = false
                    editandoDesdeHistorial = false
                    huboEdicionMedidas = false
                    cotizacionDraft.clear()
                    rainDraft = CotizacionRainDraft()
                    navController.navigate(Routes.SELECCION_PRODUCTO) {
                        popUpTo(Routes.ADMIN_HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onVerMisCotizaciones = {
                    navController.navigate(Routes.HISTORIAL) { launchSingleTop = true }
                },
                onPendientes = { navController.navigate(Routes.PENDIENTES) },
                onPendientesDrive = { navController.navigate(Routes.PENDIENTES_DRIVE) },
                onEnviosInstalacion = { navController.navigate(Routes.ENVIOS_INSTALACION) },
                onVerMetros = { navController.navigate(Routes.ADMIN_METROS) },
                onConfigurePrecios = { navController.navigate(Routes.ADMIN_PRECIOS) },
                onVerTodasCotizaciones = { navController.navigate(Routes.ADMIN_COTIZACIONES) },
                onVerEmpleados = { navController.navigate(Routes.ADMIN_EMPLEADOS) },
                onGestionarLeads = { navController.navigate(Routes.ADMIN_LEADS) },
                logoutEnabled = online,
                onCerrarSesion = {
                    scope.launch {
                        if (!isOnline(context)) return@launch
                        try {
                            AuthRepository.logout()
                        } catch (_: Exception) {
                            return@launch
                        }
                        SessionManager.logout(context)
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.ADMIN_HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ADMIN - CONFIGURAR PRECIOS
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.ADMIN_PRECIOS,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            ConfigurarPreciosScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ADMIN - VER COTIZACIONES
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.ADMIN_COTIZACIONES,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            AdminCotizacionesScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onVerDetalle = { cotizacion ->
                    cotizacionRemotaSeleccionada = cotizacion
                    navController.navigate(Routes.adminCotizacionDetalle(cotizacion.folio))
                }
            )
        }

        composable(
            route = Routes.ADMIN_COTIZACION_DETALLE,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            cotizacionRemotaSeleccionada?.let { cotizacion ->
                AdminCotizacionDetalleScreen(
                    cotizacion = cotizacion,
                    isDarkMode = isDarkMode,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ADMIN - GESTIONAR EMPLEADOS
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.ADMIN_EMPLEADOS,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            AdminEmpleadosScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onVerCotizacionesEmpleado = { userId ->
                    navController.navigate("admin_cotizaciones_filtrado/$userId")
                }
            )
        }

        composable(
            route = "admin_cotizaciones_filtrado/{userId}",
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            AdminCotizacionesScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onVerDetalle = { cotizacion ->
                    cotizacionRemotaSeleccionada = cotizacion
                    navController.navigate(Routes.adminCotizacionDetalle(cotizacion.folio))
                },
                filtroUsuarioInicial = userId
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ADMIN: GESTIONAR LEADS
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.ADMIN_LEADS,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            AdminLeadsScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // FLUJO HURRICANE - SELECCIÓN DE CLIENTE
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.SELECCION_CLIENTE,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            SeleccionClienteScreen(
                context = context,
                userId = SessionManager.getUserId(context),
                userRole = SessionManager.getRole(context),
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onClienteNuevo = {
                    cotizacionDraft.clear()
                    cotizacionDraft.esClienteActual = false
                    cotizacionDraft.leadId = null
                    navController.navigate(Routes.CLIENTE)
                },
                onClienteActualSeleccionado = { lead ->
                    cotizacionDraft.clear()
                    cotizacionDraft.nombre = lead.nombreCompleto
                    cotizacionDraft.telefono = lead.telefono
                    cotizacionDraft.ciudad = lead.ciudad ?: ""
                    cotizacionDraft.colonia = lead.colonia ?: ""
                    cotizacionDraft.direccionDetalle =
                        "${lead.calle ?: ""} ${lead.numero ?: ""}".trim()
                    cotizacionDraft.esClienteActual = true
                    cotizacionDraft.leadId = lead.id
                    navController.navigate(Routes.CLIENTE)
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // FLUJO HURRICANE - CLIENTE
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.CLIENTE,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            ClienteScreen(
                draft = cotizacionDraft,
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onContinuar = { navController.navigate(Routes.TIPO_PROPIEDAD) }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // FLUJO HURRICANE - TIPO DE PROPIEDAD
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.TIPO_PROPIEDAD,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            TipoPropiedadScreen(
                draft = cotizacionDraft,
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onTipoPropiedadSelected = { tipo ->
                    cotizacionDraft.tipoPropiedad = tipo.label
                    navController.navigate(Routes.MEDIDAS)
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // FLUJO HURRICANE - MEDIDAS
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.MEDIDAS,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            MedidasScreen(
                draft = cotizacionDraft,
                isDarkMode = isDarkMode,
                onDraftChange = { },
                onBack = {
                    if (editandoDesdeHistorial) {
                        editandoDesdeHistorial = false
                    }
                    navController.popBackStack()
                },
                onContinuarResumen = { cotizacion ->
                    cotizacionActual = cotizacion

                    if (editandoDesdeHistorial) {
                        guardarCotizacionLocal(context, cotizacion, esActualizacion = true)
                        huboEdicionMedidas = true
                        desdeHistorial = true
                        editandoDesdeHistorial = false
                        navController.popBackStack()
                    } else {
                        huboEdicionMedidas = cotizacion.folio.isNotBlank()
                        desdeHistorial = false
                        navController.navigate(Routes.RESUMEN) { launchSingleTop = true }
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // FLUJO HURRICANE - RESUMEN
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.RESUMEN,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            val cot = cotizacionActual
            val currentUserRole = SessionManager.getRole(context)
            val homeDestination = if (currentUserRole == "ADMIN") Routes.ADMIN_HOME else Routes.HOME

            if (cot != null) {
                ResumenScreen(
                    cotizacion = cot,
                    desdeHistorial = desdeHistorial,
                    huboEdicionMedidas = huboEdicionMedidas,
                    isDarkMode = isDarkMode,
                    onVolverAInicio = {
                        cotizacionDraft.clear()
                        cotizacionActual = null
                        desdeHistorial = false
                        editandoDesdeHistorial = false
                        huboEdicionMedidas = false
                        navController.navigate(homeDestination) {
                            popUpTo(homeDestination) { inclusive = true }
                        }
                    },
                    onVolverAEditar = {
                        if (desdeHistorial) {
                            cotizacionDraft.cargarDesdeCotizacion(cot)
                            editandoDesdeHistorial = true
                            navController.navigate(Routes.MEDIDAS)
                        } else {
                            cotizacionDraft.cargarDesdeCotizacion(cot)
                            navController.popBackStack()
                        }
                    },
                    onVolverAHistorial = {
                        cotizacionActual = null
                        desdeHistorial = false
                        editandoDesdeHistorial = false
                        huboEdicionMedidas = false
                        navController.navigate(Routes.HISTORIAL) {
                            popUpTo(homeDestination) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onCotizacionActualizada = { cotizacionNueva ->
                        cotizacionActual = cotizacionNueva
                    }
                )
            } else {
                val currentUserRole2 = SessionManager.getRole(context)
                val safeHome = if (currentUserRole2 == "ADMIN") Routes.ADMIN_HOME else Routes.HOME
                BackHandler {
                    navController.navigate(safeHome) {
                        popUpTo(safeHome) { inclusive = true }
                    }
                }
                Box(modifier = Modifier.fillMaxSize())
            }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // HISTORIAL
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.HISTORIAL,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            HistorialScreen(
                listState = historialListState,
                isDarkMode = isDarkMode,
                onBack = {
                    if (navController.currentDestination?.route == Routes.HISTORIAL) {
                        cotizacionActual = null
                        desdeHistorial = false
                        editandoDesdeHistorial = false
                        huboEdicionMedidas = false
                        navController.popBackStack()
                    }
                },
                onVerDetalle = { cotizacion ->
                    val currentRoute = navController.currentDestination?.route
                    if (currentRoute == Routes.HISTORIAL) {
                        cotizacionActual = cotizacion
                        desdeHistorial = true
                        huboEdicionMedidas = false
                        navController.navigate(Routes.RESUMEN) { launchSingleTop = true }
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // PENDIENTES
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.PENDIENTES,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            PendingUploadsScreen(
                isDarkMode = isDarkMode,
                isOnline = online,
                onBack = { navController.popBackStack() },
                onRetryUpload = { pending ->
                    scope.launch {
                        UploadRepository.uploadOne(context, pending)
                    }
                },
                onRemove = { id ->
                    UploadQueueStorage.remove(context, id)
                }
            )
        }

        composable(
            route = Routes.PENDIENTES_DRIVE,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            PendingDriveUploadsScreen(
                isDarkMode = isDarkMode,
                isOnline = online,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // RAIN PROTECTION - FLUJO COMPLETO
        // ═══════════════════════════════════════════════════════════════════════════

        // RAIN - SELECCIÓN DE CLIENTE (leads CRM o nuevo)
        composable(
            route = Routes.RAIN_CLIENTE,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            SeleccionClienteScreen(
                context = context,
                userId = SessionManager.getUserId(context),
                userRole = SessionManager.getRole(context),
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onClienteNuevo = {
                    // Cliente nuevo: limpiar y ir a captura de datos
                    rainDraft = CotizacionRainDraft()
                    rainDraft.esClienteActual = false
                    rainDraft.leadId = null
                    navController.navigate(Routes.RAIN_DATOS)
                },
                onClienteActualSeleccionado = { lead ->
                    // Cliente del CRM: pre-llenar datos y ir a captura de datos
                    rainDraft = CotizacionRainDraft()
                    rainDraft.nombre = lead.nombreCompleto
                    rainDraft.telefono = lead.telefono
                    rainDraft.ciudad = lead.ciudad ?: ""
                    rainDraft.colonia = lead.colonia ?: ""
                    rainDraft.direccionDetalle = "${lead.calle ?: ""} ${lead.numero ?: ""}".trim()
                    rainDraft.esClienteActual = true
                    rainDraft.leadId = lead.id
                    // Detectar zona geográfica
                    rainDraft.zonaGeografica = ZonasData.detectarZona(rainDraft.ciudad)
                    navController.navigate(Routes.RAIN_DATOS)
                }
            )
        }

        // RAIN - CAPTURA DE DATOS DEL CLIENTE
        composable(
            route = Routes.RAIN_DATOS,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            RainClienteScreen(
                rainDraft = rainDraft,
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onContinuar = {
                    navController.navigate(Routes.RAIN_TIPO_PROPIEDAD)
                }
            )
        }

        // RAIN - TIPO DE PROPIEDAD
        composable(
            route = Routes.RAIN_TIPO_PROPIEDAD,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            RainTipoPropiedadScreen(
                rainDraft = rainDraft,
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onTipoPropiedadSelected = { tipo ->
                    rainDraft.tipoPropiedad = tipo.name
                    navController.navigate(Routes.RAIN_MEDIDAS)
                }
            )
        }

        // RAIN - CAPTURA DE MEDIDAS
        composable(
            route = Routes.RAIN_MEDIDAS,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            RainMedidasScreen(
                rainDraft = rainDraft,
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(Routes.RAIN_RESUMEN)
                }
            )
        }

        // RAIN - RESUMEN
        composable(
            route = Routes.RAIN_RESUMEN,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            RainResumenScreen(
                rainDraft = rainDraft,
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onGuardarYGenerarPdf = {
                    scope.launch {
                        try {
                            // Generar folio
                            val especialista = SessionManager.getNombre(context)
                            val folio = RainFolioManager.nextFolioForEspecialista(context, especialista)
                            rainDraft.folio = folio

                            // Fecha actual
                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                            rainDraft.fecha = sdf.format(java.util.Date())

                            // TODO: Guardar en Supabase cuando el PDF esté listo

                            android.widget.Toast.makeText(
                                context,
                                "Cotización ${folio} generada (PDF pendiente)",
                                android.widget.Toast.LENGTH_LONG
                            ).show()

                            // Volver al inicio
                            rainDraft = CotizacionRainDraft()
                            if (isAdmin) {
                                navController.navigate(Routes.ADMIN_HOME) {
                                    popUpTo(Routes.ADMIN_HOME) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.HOME) { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("RainResumen", "Error: ${e.message}")
                            android.widget.Toast.makeText(
                                context,
                                "Error al guardar: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onCotizarOtroProducto = { tipo ->
                    // Copiar datos del cliente a cotizacionDraft de huracanes
                    cotizacionDraft.clear()
                    cotizacionDraft.nombre = rainDraft.nombre
                    cotizacionDraft.telefono = rainDraft.telefono
                    cotizacionDraft.ciudad = rainDraft.ciudad
                    cotizacionDraft.colonia = rainDraft.colonia
                    cotizacionDraft.direccionDetalle = rainDraft.direccionDetalle
                    cotizacionDraft.leadId = rainDraft.leadId
                    cotizacionDraft.esClienteActual = rainDraft.esClienteActual
                    cotizacionDraft.zonaGeografica = rainDraft.zonaGeografica
                    cotizacionDraft.tipoPropiedad = rainDraft.tipoPropiedad

                    navController.navigate(Routes.TIPO_PROPIEDAD) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // INSTALADOR
        // ═══════════════════════════════════════════════════════════════════════════

        composable(Routes.INSTALADOR_HOME) {
            InstaladorHomeScreen(
                instaladorName = SessionManager.getNombre(context),
                isDarkMode = isDarkMode,
                onToggleDarkMode = { setDarkMode(!isDarkMode) },
                onVerMedidas = { navController.navigate(Routes.INSTALADOR_MEDIDAS_LIST) },
                onPendientesDrive = { navController.navigate(Routes.INSTALADOR_DRIVE) },
                logoutEnabled = online,
                onCerrarSesion = {
                    scope.launch {
                        if (!isOnline(context)) return@launch
                        try {
                            AuthRepository.logout()
                        } catch (_: Exception) {
                            return@launch
                        }
                        SessionManager.logout(context)
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.INSTALADOR_HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }

        composable(
            route = Routes.INSTALADOR_MEDIDAS_LIST,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            InstaladorMedidasListScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onNavigateToResumen = { folio ->
                    navController.navigate(Routes.instaladorResumen(folio))
                }
            )
        }

        composable(
            route = Routes.INSTALADOR_FORM,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) { backStackEntry ->
            val folio = backStackEntry.arguments?.getString("folio") ?: ""
            InstaladorFormScreen(
                folio = folio,
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onNavigateToResumen = { f ->
                    navController.navigate(Routes.instaladorResumen(f))
                }
            )
        }

        composable(
            route = Routes.INSTALADOR_RESUMEN,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) { backStackEntry ->
            val folio = backStackEntry.arguments?.getString("folio") ?: ""
            InstaladorResumenScreen(
                folio = folio,
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Routes.INSTALADOR_HOME) {
                        popUpTo(Routes.INSTALADOR_HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.INSTALADOR_DRIVE,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            InstaladorDriveScreen(
                isDarkMode = isDarkMode,
                isOnline = online,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ADMIN - METROS CUADRADOS
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.ADMIN_METROS,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            AdminMetrosScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() }
            )
        }
    }
}