package com.example.hurricansolutionapp

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
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
// ANIMACIONES DE TRANSICIÓN - MÁS SUAVES (tipo fade, menos "cambio de pantalla")
// ═══════════════════════════════════════════════════════════════════════════════

private const val ANIMATION_DURATION = 350

// Animación de entrada: fade + slide sutil
private fun enterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(ANIMATION_DURATION)
    ) + slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth / 4 },
        animationSpec = tween(ANIMATION_DURATION)
    )
}

// Animación de salida: fade out sutil
private fun exitTransition(): ExitTransition {
    return fadeOut(
        animationSpec = tween(ANIMATION_DURATION / 2)
    )
}

// Animación de entrada al volver: fade
private fun popEnterTransition(): EnterTransition {
    return fadeIn(
        animationSpec = tween(ANIMATION_DURATION)
    )
}

// Animación de salida al volver: fade + slide sutil
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
    val start = if (SessionManager.isLoggedIn(context)) Routes.HOME else Routes.LOGIN
    var cotizacionActual by remember { mutableStateOf<Cotizacion?>(null) }
    var desdeHistorial by remember { mutableStateOf(false) }

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
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // HOME
        // ═══════════════════════════════════════════════════════════════════
        composable(Routes.HOME) {
            HomeScreen(
                userFirstName = SessionManager.getNombre(context),
                pendingCount = UploadQueueStorage.getAll(context)
                    .filter { it.status != "DONE" }.size,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { setDarkMode(!isDarkMode) },

                onNuevaCotizacion = {
                    cotizacionDraft.clear()
                    desdeHistorial = false
                    navController.navigate(Routes.CLIENTE)
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
                            // Si viene del historial, volver al historial
                            navController.popBackStack()
                        } else {
                            // Si es nueva cotización, volver a medidas
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
                    // ✅ Cuando haces click en una cotización, se abre el ResumenScreen
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
    }
}