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
    val isInstaller = userRole == "INSTALLER"

    // ═══════════════════════════════════════════════════════════════════════════
    // CAMBIO: Admin y Specialist ahora van al selector de producto
    // Solo Installer va directo a su home
    // ═══════════════════════════════════════════════════════════════════════════
    val start = when {
        !SessionManager.isLoggedIn(context) -> Routes.LOGIN
        isInstaller -> Routes.INSTALADOR_HOME
        else -> Routes.PRODUCT_SELECTOR  // Admin y Specialist van al selector
    }

    // Estados Hurricane
    var cotizacionActual by remember { mutableStateOf<Cotizacion?>(null) }
    var desdeHistorial by remember { mutableStateOf(false) }
    var editandoDesdeHistorial by remember { mutableStateOf(false) }
    var huboEdicionMedidas by remember { mutableStateOf(false) }
    var cotizacionRemotaSeleccionada by remember { mutableStateOf<CotizacionRemota?>(null) }
    var cotizacionRainRemotaSeleccionada by remember { mutableStateOf<CotizacionRainRemota?>(null) }  // 👈 NUEVO

    // ═══════════════════════════════════════════════════════════════════════════
    // RAIN PROTECTION - DRAFT Y ESTADOS
    // ═══════════════════════════════════════════════════════════════════════════
    var rainDraft by remember { mutableStateOf(CotizacionRainDraft()) }
    var cotizacionRainActual by remember { mutableStateOf<CotizacionRain?>(null) }
    var desdeHistorialRain by remember { mutableStateOf(false) }
    var editandoDesdeHistorialRain by remember { mutableStateOf(false) }

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
                    // ═══════════════════════════════════════════════════════════════
                    // CAMBIO: Admin y Specialist van al selector de producto
                    // ═══════════════════════════════════════════════════════════════
                    val destination = when (role) {
                        "INSTALLER" -> Routes.INSTALADOR_HOME
                        else -> Routes.PRODUCT_SELECTOR  // Admin y Specialist
                    }
                    navController.navigate(destination) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // NUEVO: SELECTOR DE PRODUCTO (Primera pantalla después del login)
        // ═══════════════════════════════════════════════════════════════════════════

        composable(Routes.PRODUCT_SELECTOR) {
            val currentRole = SessionManager.getRole(context)
            val isAdmin = currentRole == "ADMIN"

            ProductSelectorHomeScreen(
                userName = SessionManager.getNombre(context),
                isDarkMode = isDarkMode,
                onToggleDarkMode = { setDarkMode(!isDarkMode) },
                onSelectHurricane = {
                    // Limpiar drafts
                    cotizacionDraft.clear()
                    rainDraft = CotizacionRainDraft()
                    // Navegar al Home de Hurricane según el rol
                    val destination = if (isAdmin) Routes.ADMIN_HOME else Routes.HOME
                    navController.navigate(destination) {
                        popUpTo(Routes.PRODUCT_SELECTOR) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onSelectRain = {
                    // Limpiar drafts
                    cotizacionDraft.clear()
                    rainDraft = CotizacionRainDraft()
                    // Navegar al Home de Rain según el rol
                    val destination = if (isAdmin) Routes.ADMIN_HOME_RAIN else Routes.HOME_RAIN
                    navController.navigate(destination) {
                        popUpTo(Routes.PRODUCT_SELECTOR) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onLogout = {
                    scope.launch {
                        if (!isOnline(context)) return@launch
                        try {
                            AuthRepository.logout()
                        } catch (_: Exception) {
                            return@launch
                        }
                        SessionManager.logout(context)
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.PRODUCT_SELECTOR) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                logoutEnabled = online
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // SELECCIÓN DE PRODUCTO (para cotización dentro del flujo - sin cambios)
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
                    navController.navigate(Routes.SELECCION_CLIENTE) {
                        launchSingleTop = true
                    }
                },
                onSelectRain = {
                    rainDraft = CotizacionRainDraft()
                    navController.navigate(Routes.RAIN_CLIENTE) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // HOME (ESPECIALISTA) - Ahora es el Home de Hurricane para especialistas
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
                    desdeHistorialRain = false
                    editandoDesdeHistorialRain = false
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
                    // CAMBIO: Volver al selector de producto
                    navController.navigate(Routes.PRODUCT_SELECTOR) {
                        popUpTo(Routes.HOME) { inclusive = true }
                        launchSingleTop = true
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
        // HOME RAIN (ESPECIALISTA) - Rain Protection
        // ═══════════════════════════════════════════════════════════════════════════

        composable(Routes.HOME_RAIN) {
            HomeRainScreen(
                userFirstName = SessionManager.getNombre(context),
                pendingCount = 0, // TODO: Contador de pendientes Rain
                isDarkMode = isDarkMode,
                onToggleDarkMode = { setDarkMode(!isDarkMode) },
                onNuevaCotizacion = {
                    desdeHistorialRain = false
                    editandoDesdeHistorialRain = false
                    rainDraft = CotizacionRainDraft()
                    navController.navigate(Routes.RAIN_CLIENTE) {
                        popUpTo(Routes.HOME_RAIN) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onVerCotizaciones = {
                    navController.navigate(Routes.HISTORIAL) { launchSingleTop = true }
                },
                onPendientes = { navController.navigate(Routes.PENDIENTES_DRIVE) },
                onPendientesDrive = { navController.navigate(Routes.PENDIENTES_DRIVE) },
                onEnviosInstalacion = { /* TODO: Envíos Rain */ },
                logoutEnabled = online,
                onCambiarProducto = {
                    // Volver al selector de producto
                    navController.navigate(Routes.PRODUCT_SELECTOR) {
                        popUpTo(Routes.HOME_RAIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ADMIN HOME - HURRICANE PROTECTION
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
                    desdeHistorialRain = false
                    editandoDesdeHistorialRain = false
                    cotizacionDraft.clear()
                    rainDraft = CotizacionRainDraft()
                    // Ir directo al flujo Hurricane
                    navController.navigate(Routes.SELECCION_CLIENTE) {
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
                    // CAMBIO: Volver al selector de producto en lugar de logout
                    navController.navigate(Routes.PRODUCT_SELECTOR) {
                        popUpTo(Routes.ADMIN_HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ADMIN HOME - RAIN PROTECTION (Usando AdminHomeRainScreen)
        // ═══════════════════════════════════════════════════════════════════════════

        composable(Routes.ADMIN_HOME_RAIN) {
            AdminHomeRainScreen(
                adminName = SessionManager.getNombre(context),
                pendingCount = 0, // TODO: Contador de pendientes Rain
                isDarkMode = isDarkMode,
                onToggleDarkMode = { setDarkMode(!isDarkMode) },
                onNuevaCotizacion = {
                    // Ir directo al flujo Rain
                    desdeHistorialRain = false
                    editandoDesdeHistorialRain = false
                    rainDraft = CotizacionRainDraft()
                    navController.navigate(Routes.RAIN_CLIENTE) {
                        launchSingleTop = true
                    }
                },
                onVerMisCotizaciones = {
                    navController.navigate(Routes.HISTORIAL) { launchSingleTop = true }
                },
                onPendientes = { navController.navigate(Routes.PENDIENTES_DRIVE) },
                onPendientesDrive = { navController.navigate(Routes.PENDIENTES_DRIVE) },
                onEnviosInstalacion = { /* TODO: Envíos Rain */ },
                onConfigurePrecios = {
                    navController.navigate(Routes.ADMIN_RAIN_PRECIOS)
                },
                onVerTodasCotizaciones = {
                    navController.navigate(Routes.ADMIN_RAIN_COTIZACIONES)  // 👈 Ahora va a pantalla específica para Admin
                },
                onVerEmpleados = { navController.navigate(Routes.ADMIN_RAIN_EMPLEADOS) },
                onGestionarLeads = { navController.navigate(Routes.ADMIN_LEADS) },
                logoutEnabled = online,
                onCambiarProducto = {
                    // Volver al selector de producto
                    navController.navigate(Routes.PRODUCT_SELECTOR) {
                        popUpTo(Routes.ADMIN_HOME_RAIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ADMIN - CONFIGURAR PRECIOS HURRICANE
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
        // ADMIN - CONFIGURAR PRECIOS RAIN
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.ADMIN_RAIN_PRECIOS,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            AdminRainPreciosScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ADMIN RAIN - GESTIONAR EMPLEADOS (sin M² Cotizados)
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.ADMIN_RAIN_EMPLEADOS,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            AdminEmpleadosScreen(
                isRainMode = true,  // 👈 Rain Mode: oculta M² Cotizados
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onVerCotizacionesEmpleado = { userId ->
                    navController.navigate("admin_rain_cotizaciones_filtrado/$userId")
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // ADMIN RAIN - VER COTIZACIONES (NUEVO)
        // ═══════════════════════════════════════════════════════════════════════════

        composable(
            route = Routes.ADMIN_RAIN_COTIZACIONES,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            AdminCotizacionesRainScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onVerDetalle = { cotizacion ->
                    cotizacionRainRemotaSeleccionada = cotizacion
                    navController.navigate(Routes.adminRainCotizacionDetalle(cotizacion.folio))
                }
            )
        }

        // Cotizaciones Rain filtradas por empleado
        composable(
            route = "admin_rain_cotizaciones_filtrado/{userId}",
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            AdminCotizacionesRainScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onVerDetalle = { cotizacion ->
                    cotizacionRainRemotaSeleccionada = cotizacion
                    navController.navigate(Routes.adminRainCotizacionDetalle(cotizacion.folio))
                },
                filtroUsuarioInicial = userId
            )
        }

        // Detalle de cotización Rain (solo lectura para Admin)
        composable(
            route = Routes.ADMIN_RAIN_COTIZACION_DETALLE,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            cotizacionRainRemotaSeleccionada?.let { cotizacion ->
                AdminCotizacionDetalleRainScreen(
                    cotizacion = cotizacion,
                    isDarkMode = isDarkMode,
                    onBack = { navController.popBackStack() }
                )
            }
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
            val odayUserId = backStackEntry.arguments?.getString("userId")
            AdminCotizacionesScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onVerDetalle = { cotizacion ->
                    cotizacionRemotaSeleccionada = cotizacion
                    navController.navigate(Routes.adminCotizacionDetalle(cotizacion.folio))
                },
                filtroUsuarioInicial = odayUserId
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
        // HISTORIAL - HURRICANE + RAIN
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
                        // Limpiar estados Hurricane
                        cotizacionActual = null
                        desdeHistorial = false
                        editandoDesdeHistorial = false
                        huboEdicionMedidas = false
                        // Limpiar estados Rain
                        cotizacionRainActual = null
                        desdeHistorialRain = false
                        editandoDesdeHistorialRain = false
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
                },
                onVerDetalleRain = { cotizacionRain ->
                    val currentRoute = navController.currentDestination?.route
                    if (currentRoute == Routes.HISTORIAL) {
                        cotizacionRainActual = cotizacionRain
                        desdeHistorialRain = true
                        // Cargar al draft para edición
                        rainDraft = CotizacionRainDraft()
                        rainDraft.cargarDesdeCotizacionRain(cotizacionRain)
                        navController.navigate(Routes.RAIN_RESUMEN) { launchSingleTop = true }
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

        // RAIN - SELECCIÓN DE CLIENTE
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
                    rainDraft = CotizacionRainDraft()
                    rainDraft.esClienteActual = false
                    rainDraft.leadId = null
                    navController.navigate(Routes.RAIN_DATOS)
                },
                onClienteActualSeleccionado = { lead ->
                    rainDraft = CotizacionRainDraft()
                    rainDraft.nombre = lead.nombreCompleto
                    rainDraft.telefono = lead.telefono
                    rainDraft.ciudad = lead.ciudad ?: ""
                    rainDraft.colonia = lead.colonia ?: ""
                    rainDraft.direccionDetalle = "${lead.calle ?: ""} ${lead.numero ?: ""}".trim()
                    rainDraft.esClienteActual = true
                    rainDraft.leadId = lead.id
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
                onBack = {
                    if (editandoDesdeHistorialRain) {
                        editandoDesdeHistorialRain = false
                    }
                    navController.popBackStack()
                },
                onContinue = {
                    if (editandoDesdeHistorialRain) {
                        editandoDesdeHistorialRain = false
                        navController.popBackStack()
                    } else {
                        navController.navigate(Routes.RAIN_RESUMEN)
                    }
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
            val currentUserRole = SessionManager.getRole(context)
            // CAMBIO: Rain vuelve a ADMIN_HOME_RAIN o HOME_RAIN según el rol
            val homeDestination = if (currentUserRole == "ADMIN") Routes.ADMIN_HOME_RAIN else Routes.HOME_RAIN

            RainResumenScreen(
                rainDraft = rainDraft,
                isDarkMode = isDarkMode,
                desdeHistorial = desdeHistorialRain,
                cotizacionRainExistente = cotizacionRainActual,
                onBack = { navController.popBackStack() },
                onVolverAInicio = {
                    rainDraft = CotizacionRainDraft()
                    cotizacionRainActual = null
                    desdeHistorialRain = false
                    editandoDesdeHistorialRain = false
                    navController.navigate(homeDestination) {
                        popUpTo(homeDestination) { inclusive = true }
                    }
                },
                onVolverAEditar = {
                    if (desdeHistorialRain) {
                        editandoDesdeHistorialRain = true
                        navController.navigate(Routes.RAIN_MEDIDAS)
                    } else {
                        navController.popBackStack()
                    }
                },
                onVolverAHistorial = {
                    cotizacionRainActual = null
                    desdeHistorialRain = false
                    editandoDesdeHistorialRain = false
                    navController.navigate(Routes.HISTORIAL) {
                        popUpTo(homeDestination) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onCotizarOtroProducto = { tipo ->
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