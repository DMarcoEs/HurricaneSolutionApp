package com.example.hurricansolutionapp

import android.content.Context
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

    // Última vez que se cargaron los precios
    private var lastLoadTime: Long = 0
    private const val CACHE_DURATION = 5 * 60 * 1000 // 5 minutos

    /**
     * Carga los precios desde Supabase.
     * Usa caché de 5 minutos para evitar llamadas excesivas.
     */
    suspend fun loadPrices(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()

        // Si no ha pasado el tiempo de caché y no es refresh forzado, no recargar
        if (!forceRefresh && (now - lastLoadTime) < CACHE_DURATION && lastLoadTime > 0) {
            return
        }

        _isLoading.value = true
        try {
            val config = AdminRepository.getAppConfig()
            _currentConfig.value = config
            lastLoadTime = now
        } catch (e: Exception) {
            e.printStackTrace()
            // Mantener valores anteriores o por defecto
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Fuerza la recarga de precios (después de que admin actualice)
     */
    suspend fun refreshPrices() {
        loadPrices(forceRefresh = true)
    }

    /**
     * Actualiza los precios localmente (después de guardar en Supabase)
     */
    fun updateLocalConfig(config: AppConfig) {
        _currentConfig.value = config
        lastLoadTime = System.currentTimeMillis()
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // FUNCIONES DE ACCESO A PRECIOS
    // ═══════════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene el precio de venta para un producto
     */
    fun getPrecioVenta(producto: TipoProducto): Double {
        val config = _currentConfig.value
        return when (producto) {
            TipoProducto.HS875 -> config.hs875PrecioVenta
            TipoProducto.HS1250 -> config.hs1250PrecioVenta
            TipoProducto.HS1500 -> config.hs1500PrecioVenta
            TipoProducto.PERSONALIZADO -> config.hs875PrecioVenta
        }
    }

    /**
     * Obtiene el precio base (costo) para un producto
     */
    fun getPrecioBase(producto: TipoProducto): Double {
        val config = _currentConfig.value
        return when (producto) {
            TipoProducto.HS875 -> config.hs875PrecioBase
            TipoProducto.HS1250 -> config.hs1250PrecioBase
            TipoProducto.HS1500 -> config.hs1500PrecioBase
            TipoProducto.PERSONALIZADO -> config.hs875PrecioBase
        }
    }

    /**
     * Obtiene el descuento máximo permitido para un producto
     */
    fun getMaxDescuento(producto: TipoProducto): Double {
        return getPrecioVenta(producto) - getPrecioBase(producto)
    }

    /**
     * Calcula el subtotal para un área con un producto específico
     */
    fun calcularSubtotal(areaM2: Double, producto: TipoProducto): Double {
        return areaM2 * getPrecioVenta(producto)
    }

    /**
     * Calcula el subtotal con descuento aplicado
     */
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

    /**
     * Formatea un precio como moneda
     */
    fun formatPrice(amount: Double): String {
        val format = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)}"
    }

    /**
     * Obtiene un resumen de precios para mostrar
     */
    fun getPricesSummary(): String {
        val config = _currentConfig.value
        return buildString {
            appendLine("HS-875: ${formatPrice(config.hs875PrecioVenta)} / m²")
            appendLine("HS-1250: ${formatPrice(config.hs1250PrecioVenta)} / m²")
            appendLine("HS-1500: ${formatPrice(config.hs1500PrecioVenta)} / m²")
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXTENSIONES PARA TipoProducto (usando precios dinámicos)
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Extensión para obtener precio de venta dinámico
 */
fun TipoProducto.getPrecioVentaDinamico(): Double = PriceManager.getPrecioVenta(this)

/**
 * Extensión para obtener precio base dinámico
 */
fun TipoProducto.getPrecioBaseDinamico(): Double = PriceManager.getPrecioBase(this)

/**
 * Extensión para obtener descuento máximo dinámico
 */
fun TipoProducto.getMaxDescuentoDinamico(): Double = PriceManager.getMaxDescuento(this)