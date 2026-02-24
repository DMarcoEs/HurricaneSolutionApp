package com.example.hurricansolutionapp

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    // Cargar precios al iniciar (para todos los usuarios)
    LaunchedEffect(Unit) {
        if (SessionManager.isLoggedIn(context)) {
            PriceManager.loadPrices()
        }
    }

    NavHost(
        navController = navController,
        startDestination = start
    ) {

        // LOGIN

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    scope.launch {
                        PriceManager.loadPrices()
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


        // HOME (ESPECIALISTA)

        composable(Routes.HOME) {
            HomeScreen(
                userFirstName = SessionManager.getNombre(context),
                pendingCount = UploadQueueStorage.getAll(context)
                    .filter { it.status != "DONE" }.size,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { setDarkMode(!isDarkMode) },
                onNuevaCotizacion = {
                    cotizacionActual = null
                    desdeHistorial = false
                    editandoDesdeHistorial = false
                    huboEdicionMedidas = false
                    cotizacionDraft.clear()
                    navController.navigate(Routes.SELECCION_CLIENTE)
                },
                onVerCotizaciones = { navController.navigate(Routes.HISTORIAL) },
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

        // ADMIN HOME
        composable(Routes.ADMIN_HOME) {
            AdminHomeScreen(
                adminName = SessionManager.getNombre(context),
                pendingCount = UploadQueueStorage.getAll(context)
                    .filter { it.status != "DONE" }.size,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { setDarkMode(!isDarkMode) },
                onNuevaCotizacion = {
                    cotizacionActual = null
                    desdeHistorial = false
                    editandoDesdeHistorial = false
                    huboEdicionMedidas = false
                    cotizacionDraft.clear()
                    navController.navigate(Routes.SELECCION_CLIENTE)
                },
                onVerMisCotizaciones = { navController.navigate(Routes.HISTORIAL) },
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


        // ADMIN - CONFIGURAR PRECIOS
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


        // ADMIN - VER COTIZACIONES

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


        // ADMIN - GESTIONAR EMPLEADOS
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

        // ADMIN - VER COTIZACIONES FILTRADAS POR EMPLEADO
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


        // ADMIN: GESTIONAR LEADS

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
                currentStep = 1,
                totalSteps = 3,
                onBack = { navController.popBackStack() },
                onContinuar = { navController.navigate(Routes.TIPO_PROPIEDAD) }
            )
        }

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
                    navController.navigate(Routes.MEDIDAS)
                }
            )
        }


        // MEDIDAS (Captura de medidas)

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
                        // Guardar cambios en almacenamiento local
                        guardarCotizacionLocal(context, cotizacion, esActualizacion = true)
                        huboEdicionMedidas = true
                        // Mantener desdeHistorial = true
                        desdeHistorial = true
                        editandoDesdeHistorial = false
                        navController.navigate(Routes.RESUMEN) {
                            popUpTo(Routes.HISTORIAL) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        // Modo vivo: si ya tiene folio, es re-edicion
                        huboEdicionMedidas = cotizacion.folio.isNotBlank()
                        desdeHistorial = false
                        navController.navigate(Routes.RESUMEN) { launchSingleTop = true }
                    }
                }
            )
        }


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
                            // Modo vivo: cargar cotizacion CON folio al draft
                            cotizacionDraft.cargarDesdeCotizacion(cot)
                            navController.popBackStack()
                        }
                    },
                    onVolverAHistorial = {
                        huboEdicionMedidas = false
                        navController.navigate(Routes.HISTORIAL) {
                            popUpTo(Routes.HISTORIAL) { inclusive = true }
                        }
                    },
                    onCotizacionActualizada = { cotizacionNueva ->
                        // Actualizar cotizacionActual para que ediciones
                        // subsecuentes tengan los datos correctos
                        cotizacionActual = cotizacionNueva
                    }
                )
            } else {
                navController.navigate(homeDestination) {
                    popUpTo(homeDestination) { inclusive = true }
                }
            }
        }


        // HISTORIAL

        composable(
            route = Routes.HISTORIAL,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            val listState = rememberLazyListState()
            HistorialScreen(
                listState = listState,
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onVerDetalle = { cotizacion ->
                    cotizacionActual = cotizacion
                    desdeHistorial = true
                    huboEdicionMedidas = false
                    navController.navigate(Routes.RESUMEN) { launchSingleTop = true }
                }
            )
        }


        // PENDIENTES
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

        // PENDIENTES GOOGLE DRIVE
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


        // SELECCIONA DE CLIENTE
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


        // INSTALADOR - LISTA DE MEDIDAS
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


        // ADMIN - METROS CUADRADOS

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