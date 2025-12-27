package com.example.hurricansolutionapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var isDarkMode by remember { mutableStateOf(true) }
            val navController = rememberNavController()
            val cotizacionDraft = remember { CotizacionDraft() }

            var online by remember { mutableStateOf(isOnline(context)) }

            DisposableEffect(Unit) {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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
                    setDarkMode = { isDarkMode = it },
                    online = online
                )
            }
        }
    }
}
