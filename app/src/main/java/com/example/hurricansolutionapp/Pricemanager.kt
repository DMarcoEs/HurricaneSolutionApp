package com.example.hurricansolutionapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager singleton para precios dinámicos.
 * Carga los precios desde Supabase y los mantiene en memoria.
 * Todas las pantallas deben usar este manager para obtener precios.
 */
object PriceManager {

    // Estado de los precios actuales
    private val _currentConfig = MutableStateFlow(AppConfig())
    val currentConfig: StateFlow<AppConfig> = _currentConfig.asStateFlow()

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Indica si los precios cambiaron desde la última carga
    private val _preciosCambiaron = MutableStateFlow(false)
    val preciosCambiaron: StateFlow<Boolean> = _preciosCambiaron.asStateFlow()

    // Última vez que se cargaron los precios
    private var lastLoadTime: Long = 0

    // Caché reducido a 30 segundos para que los cambios se vean rápido
    private const val CACHE_DURATION = 30 * 1000 // 30 segundos

    /**
     * Carga los precios desde Supabase.
     * Siempre carga desde el servidor para tener datos actualizados.
     */
    suspend fun loadPrices(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()

        // Si no ha pasado el tiempo de caché y no es refresh forzado, no recargar
        if (!forceRefresh && (now - lastLoadTime) < CACHE_DURATION && lastLoadTime > 0) {
            android.util.Log.d("PriceManager", "Usando precios en caché (${(now - lastLoadTime) / 1000}s)")
            return
        }

        _isLoading.value = true
        try {
            val config = AdminRepository.getAppConfig()

            // Verificar si los precios cambiaron
            val preciosAnteriores = _currentConfig.value
            val cambio = lastLoadTime > 0 && (
                    config.hs875PrecioVenta != preciosAnteriores.hs875PrecioVenta ||
                            config.hs1250PrecioVenta != preciosAnteriores.hs1250PrecioVenta ||
                            config.hs1500PrecioVenta != preciosAnteriores.hs1500PrecioVenta ||
                            config.hs875PrecioBase != preciosAnteriores.hs875PrecioBase ||
                            config.hs1250PrecioBase != preciosAnteriores.hs1250PrecioBase ||
                            config.hs1500PrecioBase != preciosAnteriores.hs1500PrecioBase
                    )

            if (cambio) {
                android.util.Log.d("PriceManager", "¡PRECIOS ACTUALIZADOS DESDE SERVIDOR!")
                _preciosCambiaron.value = true
            }

            _currentConfig.value = config
            lastLoadTime = now

            android.util.Log.d("PriceManager", "Precios cargados: HS875=$${config.hs875PrecioVenta}, HS1250=$${config.hs1250PrecioVenta}, HS1500=$${config.hs1500PrecioVenta}")
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("PriceManager", "Error cargando precios: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Fuerza la recarga de precios
     */
    suspend fun refreshPrices() {
        android.util.Log.d("PriceManager", "Forzando recarga de precios...")
        loadPrices(forceRefresh = true)
    }

    /**
     * Actualiza los precios localmente (después de guardar en Supabase)
     */
    fun updateLocalConfig(config: AppConfig) {
        _currentConfig.value = config
        lastLoadTime = System.currentTimeMillis()
        android.util.Log.d("PriceManager", "Precios actualizados localmente: HS875=$${config.hs875PrecioVenta}")
    }

    /**
     * Invalida el caché para forzar recarga en la próxima llamada
     */
    fun invalidateCache() {
        lastLoadTime = 0
        android.util.Log.d("PriceManager", "Caché de precios invalidado")
    }

    /**
     * Marca que el usuario ya vio la notificación de cambio de precios
     */
    fun clearPreciosCambiaron() {
        _preciosCambiaron.value = false
    }

    /**
     * Verifica si hay precios nuevos comparando con el servidor.
     * Retorna true si los precios cambiaron.
     */
    suspend fun checkForUpdates(): Boolean {
        return try {
            val serverConfig = AdminRepository.getAppConfig()
            val currentConfig = _currentConfig.value

            val changed = serverConfig.hs875PrecioVenta != currentConfig.hs875PrecioVenta ||
                    serverConfig.hs1250PrecioVenta != currentConfig.hs1250PrecioVenta ||
                    serverConfig.hs1500PrecioVenta != currentConfig.hs1500PrecioVenta

            if (changed) {
                _currentConfig.value = serverConfig
                lastLoadTime = System.currentTimeMillis()
                _preciosCambiaron.value = true
                android.util.Log.d("PriceManager", "Precios actualizados desde servidor")
            }

            changed
        } catch (e: Exception) {
            android.util.Log.e("PriceManager", "Error verificando actualizaciones: ${e.message}")
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // FUNCIONES DE ACCESO A PRECIOS
    // ═══════════════════════════════════════════════════════════════════════════════

    fun getPrecioVenta(producto: TipoProducto): Double {
        val config = _currentConfig.value
        return when (producto) {
            TipoProducto.HS875 -> config.hs875PrecioVenta
            TipoProducto.HS1250 -> config.hs1250PrecioVenta
            TipoProducto.HS1500 -> config.hs1500PrecioVenta
            TipoProducto.PERSONALIZADO -> config.hs875PrecioVenta
        }
    }

    fun getPrecioBase(producto: TipoProducto): Double {
        val config = _currentConfig.value
        return when (producto) {
            TipoProducto.HS875 -> config.hs875PrecioBase
            TipoProducto.HS1250 -> config.hs1250PrecioBase
            TipoProducto.HS1500 -> config.hs1500PrecioBase
            TipoProducto.PERSONALIZADO -> config.hs875PrecioBase
        }
    }

    fun getMaxDescuento(producto: TipoProducto): Double {
        return getPrecioVenta(producto) - getPrecioBase(producto)
    }

    fun calcularSubtotal(areaM2: Double, producto: TipoProducto): Double {
        return areaM2 * getPrecioVenta(producto)
    }

    fun calcularSubtotalConDescuento(
        areaM2: Double,
        producto: TipoProducto,
        descuentoPorM2: Double
    ): Double {
        val precioVenta = getPrecioVenta(producto)
        val precioBase = getPrecioBase(producto)
        val descuentoAplicado = descuentoPorM2.coerceAtMost(precioVenta - precioBase)
        val precioFinal = (precioVenta - descuentoAplicado).coerceAtLeast(precioBase)
        return areaM2 * precioFinal
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // FUNCIONES DE FORMATO
    // ═══════════════════════════════════════════════════════════════════════════════

    fun formatPrice(amount: Double): String {
        val format = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)}"
    }

    fun getPricesSummary(): String {
        val config = _currentConfig.value
        return buildString {
            appendLine("HS-875: ${formatPrice(config.hs875PrecioVenta)} / m²")
            appendLine("HS-1250: ${formatPrice(config.hs1250PrecioVenta)} / m²")
            appendLine("HS-1500: ${formatPrice(config.hs1500PrecioVenta)} / m²")
        }
    }
}