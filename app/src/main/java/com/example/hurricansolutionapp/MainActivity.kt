package com.example.hurricansolutionapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Leer tema inicial
        val initialDarkMode = SessionManager.isDarkMode(this)

        // Configurar barras del sistema con colores apropiados
        enableEdgeToEdge(
            statusBarStyle = if (initialDarkMode) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            },
            navigationBarStyle = if (initialDarkMode) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
        )

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            // ✅ Leer el tema guardado desde SharedPreferences
            var isDarkMode by remember { mutableStateOf(SessionManager.isDarkMode(context)) }

            val navController = rememberNavController()
            val cotizacionDraft = remember { CotizacionDraft() }

            var online by remember { mutableStateOf(isOnline(context)) }

            // ✅ NUEVO: Validar cuenta de Drive al iniciar
            LaunchedEffect(Unit) {
                validarCuentaDriveAlInicio(context)
            }

            // Actualizar colores de barra cuando cambia el tema
            LaunchedEffect(isDarkMode) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkMode) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDarkMode) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            DisposableEffect(Unit) {
                val cm =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        scope.launch { online = true }
                    }

                    override fun onLost(network: Network) {
                        scope.launch { online = false }
                    }
                }

                cm.registerDefaultNetworkCallback(callback)
                onDispose { cm.unregisterNetworkCallback(callback) }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isDarkMode) Zinc950 else Color.White
            ) {
                AppNavigation(
                    navController = navController,
                    context = context,
                    scope = scope,
                    cotizacionDraft = cotizacionDraft,
                    isDarkMode = isDarkMode,
                    setDarkMode = { newValue ->
                        isDarkMode = newValue
                        // ✅ Guardar el tema en SharedPreferences
                        SessionManager.setDarkMode(context, newValue)
                    },
                    online = online
                )
            }
        }
    }

    /**
     * Valida que la cuenta de Drive actual sea la cuenta autorizada
     *
     * Esto previene el problema de múltiples cuentas que causa:
     * - Error 10 de autorización
     * - Archivos que quedan en limbo
     * - Subidas que no aparecen en "Pendientes Drive"
     */
    private suspend fun validarCuentaDriveAlInicio(context: Context) {
        try {
            // Si hay una sesión de Drive activa
            val account = DriveAuthManager.getSignedInAccount(context)
            if (account != null) {
                // Verificar si es la cuenta autorizada
                val isAuthorized = DriveAuthManager.isCurrentAccountAuthorized(context)

                if (!isAuthorized) {
                    android.util.Log.w(
                        "MainActivity",
                        "⚠️ ADVERTENCIA: Cuenta de Drive incorrecta detectada"
                    )
                    android.util.Log.w("MainActivity", "   Cuenta actual: ${account.email}")
                    android.util.Log.w(
                        "MainActivity",
                        "   Cuenta esperada: ${
                            DriveAuthManager.getLastAuthorizedAccountEmail(context)
                        }"
                    )
                    android.util.Log.w(
                        "MainActivity",
                        "   Se cerrará la sesión para evitar problemas"
                    )

                    // Cerrar sesión de la cuenta incorrecta
                    DriveAuthManager.signOut(context)

                    android.util.Log.d(
                        "MainActivity",
                        "✅ Sesión cerrada. Usuario deberá iniciar sesión con la cuenta correcta"
                    )
                } else {
                    android.util.Log.d(
                        "MainActivity",
                        "✅ Cuenta de Drive correcta: ${account.email}"
                    )
                }
            } else {
                val authorizedEmail = DriveAuthManager.getLastAuthorizedAccountEmail(context)
                if (authorizedEmail != null) {
                    android.util.Log.d(
                        "MainActivity",
                        "📧 Cuenta autorizada registrada: $authorizedEmail"
                    )
                    android.util.Log.d(
                        "MainActivity",
                        "   Usuario deberá iniciar sesión con esta cuenta"
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error validando cuenta Drive: ${e.message}", e)
        }
    }
}