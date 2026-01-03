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

// ═══════════════════════════════════════════════════════════════════════════════
// ANIMACIONES DE TRANSICIÓN
// ═══════════════════════════════════════════════════════════════════════════════

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

    val start = when {
        !SessionManager.isLoggedIn(context) -> Routes.LOGIN
        isAdmin -> Routes.ADMIN_HOME
        else -> Routes.HOME
    }

    var cotizacionActual by remember { mutableStateOf<Cotizacion?>(null) }
    var desdeHistorial by remember { mutableStateOf(false) }
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
        // ═══════════════════════════════════════════════════════════════════
        // LOGIN
        // ═══════════════════════════════════════════════════════════════════
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    // Cargar precios después del login
                    scope.launch {
                        PriceManager.loadPrices()
                    }

                    // Navegar según el rol
                    val role = SessionManager.getRole(context)
                    val destination = if (role == "ADMIN") Routes.ADMIN_HOME else Routes.HOME

                    navController.navigate(destination) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // MODIFICAR: HOME (ESPECIALISTA)
        // ═══════════════════════════════════════════════════════════════════
        composable(Routes.HOME) {
            HomeScreen(
                userFirstName = SessionManager.getNombre(context),
                pendingCount = UploadQueueStorage.getAll(context).filter { it.status != "DONE" }.size,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { setDarkMode(!isDarkMode) },

                onNuevaCotizacion = {
                    // ✅ CAMBIO: Ya NO navega directo a CLIENTE, sino a SELECCION_CLIENTE
                    navController.navigate(Routes.SELECCION_CLIENTE)
                },

                onVerCotizaciones = { navController.navigate(Routes.HISTORIAL) },
                onPendientes = { navController.navigate(Routes.PENDIENTES) },

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

        // ═══════════════════════════════════════════════════════════════════
        // MODIFICAR: ADMIN HOME
        // ═══════════════════════════════════════════════════════════════════
        composable(Routes.ADMIN_HOME) {
            AdminHomeScreen(
                adminName = SessionManager.getNombre(context),
                pendingCount = UploadQueueStorage.getAll(context).filter { it.status != "DONE" }.size,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { setDarkMode(!isDarkMode) },

                // ✅ CAMBIO: Admin también usa selección de cliente
                onNuevaCotizacion = {
                    navController.navigate(Routes.SELECCION_CLIENTE)
                },

                onVerMisCotizaciones = { navController.navigate(Routes.HISTORIAL) },
                onPendientes = { navController.navigate(Routes.PENDIENTES) },
                onConfigurePrecios = { navController.navigate(Routes.ADMIN_PRECIOS) },
                onVerTodasCotizaciones = { navController.navigate(Routes.ADMIN_COTIZACIONES) },
                onVerEmpleados = { navController.navigate(Routes.ADMIN_EMPLEADOS) },

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

        // ═══════════════════════════════════════════════════════════════════
        // ADMIN - CONFIGURAR PRECIOS
        // ═══════════════════════════════════════════════════════════════════
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

        // ═══════════════════════════════════════════════════════════════════
        // ADMIN - VER COTIZACIONES
        // ═══════════════════════════════════════════════════════════════════
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

        // ═══════════════════════════════════════════════════════════════════
        // ADMIN - DETALLE DE COTIZACIÓN
        // ═══════════════════════════════════════════════════════════════════
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

        // ═══════════════════════════════════════════════════════════════════
        // ADMIN - GESTIONAR EMPLEADOS
        // ═══════════════════════════════════════════════════════════════════
        composable(
            route = Routes.ADMIN_EMPLEADOS,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            AdminEmpleadosScreen(
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // CLIENTE (Paso 1 del flujo de cotización)
        // ═══════════════════════════════════════════════════════════════════
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
                onContinuar = { navController.navigate(Routes.MEDIDAS) }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // MEDIDAS (Paso 2 del flujo de cotización)
        // ═══════════════════════════════════════════════════════════════════
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
                currentStep = 2,
                totalSteps = 3,
                onDraftChange = { /* Tu lógica de cambio */ },
                onBack = { navController.popBackStack() },
                onContinuarResumen = { cotizacion ->
                    cotizacionActual = cotizacion
                    desdeHistorial = false
                    navController.navigate(Routes.RESUMEN)
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // RESUMEN (Paso 3 del flujo de cotización)
        // ═══════════════════════════════════════════════════════════════════
        composable(
            route = Routes.RESUMEN,
            enterTransition = { enterTransition() },
            exitTransition = { exitTransition() },
            popEnterTransition = { popEnterTransition() },
            popExitTransition = { popExitTransition() }
        ) {
            val cot = cotizacionActual
            if (cot != null) {
                ResumenScreen(
                    cotizacion = cot,
                    desdeHistorial = desdeHistorial,
                    isDarkMode = isDarkMode,
                    onVolverAInicio = {
                        cotizacionDraft.clear()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onVolverAEditar = {
                        if (desdeHistorial) {
                            navController.popBackStack()
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onVolverAHistorial = {
                        navController.navigate(Routes.HISTORIAL) {
                            popUpTo(Routes.HISTORIAL) { inclusive = true }
                        }
                    }
                )
            } else {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // HISTORIAL
        // ═══════════════════════════════════════════════════════════════════
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
                    navController.navigate(Routes.RESUMEN)
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // PENDIENTES
        // ═══════════════════════════════════════════════════════════════════
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
        // ═══════════════════════════════════════════════════════════════════
        // ✅ NUEVA RUTA: SELECCIÓN DE CLIENTE
        // ═══════════════════════════════════════════════════════════════════
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
                    // Cliente nuevo: limpiar draft y permitir edición completa
                    cotizacionDraft.clear()
                    cotizacionDraft.esClienteActual = false
                    cotizacionDraft.leadId = null
                    navController.navigate(Routes.CLIENTE)
                },
                onClienteActualSeleccionado = { lead ->
                    // Cliente actual: pre-llenar datos y marcar como no editable
                    cotizacionDraft.clear()
                    cotizacionDraft.nombre = lead.nombreCompleto
                    cotizacionDraft.telefono = lead.telefono
                    cotizacionDraft.ciudad = lead.ciudad ?: ""
                    cotizacionDraft.colonia = lead.colonia ?: ""
                    cotizacionDraft.direccionDetalle = "${lead.calle ?: ""} ${lead.numero ?: ""}".trim()
                    cotizacionDraft.esClienteActual = true  // ✅ Esto activa el modo readonly
                    cotizacionDraft.leadId = lead.id
                    navController.navigate(Routes.CLIENTE)
                }
            )
        }
    }
}