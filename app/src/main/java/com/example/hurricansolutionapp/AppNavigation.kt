package com.example.hurricansolutionapp

import android.content.Context
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

    NavHost(
        navController = navController,
        startDestination = start
    ) {
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

        composable(Routes.HOME) {
            HomeScreen(
                userFirstName = SessionManager.getNombre(context),
                pendingCount = 3,
                isDarkMode = isDarkMode,
                onToggleDarkMode = { setDarkMode(!isDarkMode) },

                onNuevaCotizacion = { navController.navigate(Routes.CLIENTE) },

                // ✅ Botones del Home conectados:
                onVerCotizaciones = { navController.navigate(Routes.HISTORIAL) },
                onPendientes = { navController.navigate(Routes.PENDIENTES) },

                logoutEnabled = online,
                onCerrarSesion = {
                    scope.launch {
                        // Misma protección que ya tenías:
                        if (!isOnline(context)) return@launch

                        try {
                            AuthRepository.logout()
                        } catch (_: Exception) {
                            // si falla signOut, NO cierres sesión local
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

        composable(Routes.CLIENTE) {
            ClienteScreen(
                draft = cotizacionDraft,
                isDarkMode = isDarkMode,
                onBack = { navController.popBackStack() },
                onContinuar = { navController.navigate(Routes.MEDIDAS) }
            )
        }

        composable(Routes.MEDIDAS) {
            MedidasScreen(
                draft = cotizacionDraft,
                isDarkMode = isDarkMode, // ✅ Pasar isDarkMode
                onDraftChange = { /* Tu lógica de cambio */ },
                onBack = { navController.popBackStack() },
                onContinuarResumen = { cotizacion ->
                    cotizacionActual = cotizacion
                    navController.navigate(Routes.RESUMEN)
                }
            )
        }

        composable(Routes.RESUMEN) {
            val cot = cotizacionActual
            if (cot != null) {
                ResumenScreen(
                    cotizacion = cot,
                    desdeHistorial = false,
                    onVolverAInicio = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onVolverAEditar = { navController.popBackStack() },
                    onVolverAHistorial = { navController.navigate(Routes.HISTORIAL) }
                )
            } else {
                // Si por alguna razón entra sin datos, lo mandamos a Home
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) { inclusive = true }
                }
            }
        }

        // ✅ Pantalla real: Historial
        composable(Routes.HISTORIAL) {
            val listState = rememberLazyListState()

            HistorialScreen(
                listState = listState,
                onBack = { navController.popBackStack() },
                onVerDetalle = { cotizacion ->
                    // Si después creas una pantalla detalle, aquí navegas.
                    // Por ahora, NO hace nada (pero la pantalla sí abre).
                }
            )
        }

        // ✅ Pantalla real: Pendientes
        composable(Routes.PENDIENTES) {
            PendingUploadsScreen(
                onBack = { navController.popBackStack() },
                onRetryUpload = { pending ->
                    // Aquí va tu lógica real de reintento (subir PDF)
                },
                onRemove = { id ->
                    // Aquí va tu lógica real de borrar pendiente
                }
            )
        }
    }
}
