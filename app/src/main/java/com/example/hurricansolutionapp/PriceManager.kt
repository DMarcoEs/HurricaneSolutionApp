package com.example.hurricansolutionapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * PRICE MANAGER - GESTOR DE PRECIOS POR ZONA
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Manager singleton para precios dinámicos por zona geográfica.
 * Carga los precios desde Supabase y los mantiene en memoria.
 * Soporta 3 zonas: Continental, Islas, Foránea
 */
object PriceManager {

    // ═══════════════════════════════════════════════════════════════════════════
    // ESTADOS
    // ═══════════════════════════════════════════════════════════════════════════

    // Precios de todas las zonas
    private val _preciosZonas = MutableStateFlow(PreciosTodasZonas())
    val preciosZonas: StateFlow<PreciosTodasZonas> = _preciosZonas.asStateFlow()

    // Zona actualmente seleccionada para cotizar
    private val _zonaActual = MutableStateFlow(ZonaGeografica.CONTINENTAL)
    val zonaActual: StateFlow<ZonaGeografica> = _zonaActual.asStateFlow()

    // Legacy: Config actual (para compatibilidad) - Usa la zona actualmente seleccionada
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

    // ═══════════════════════════════════════════════════════════════════════════
    // FUNCIONES DE CARGA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Carga los precios de TODAS las zonas desde Supabase
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
            // Cargar precios de todas las zonas
            val precios = AdminRepository.getPreciosTodasZonas()
            
            // Verificar si los precios cambiaron
            val preciosAnteriores = _preciosZonas.value
            val cambio = lastLoadTime > 0 && preciosCambiaron(preciosAnteriores, precios)

            if (cambio) {
                android.util.Log.d("PriceManager", "¡PRECIOS ACTUALIZADOS DESDE SERVIDOR!")
                _preciosCambiaron.value = true
            }

            _preciosZonas.value = precios
            lastLoadTime = now

            // Actualizar config legacy con la zona actual
            actualizarConfigLegacy()

            android.util.Log.d("PriceManager", "Precios cargados para 3 zonas")
            logPreciosZona(precios.continental, "Continental")
            logPreciosZona(precios.islas, "Islas")
            logPreciosZona(precios.foranea, "Foránea")
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("PriceManager", "Error cargando precios por zona: ${e.message}")
            
            // Fallback: intentar cargar precios legacy
            try {
                val legacyConfig = AdminRepository.getAppConfig()
                _currentConfig.value = legacyConfig
                android.util.Log.d("PriceManager", "Usando precios legacy como fallback")
            } catch (e2: Exception) {
                android.util.Log.e("PriceManager", "Error en fallback: ${e2.message}")
            }
        } finally {
            _isLoading.value = false
        }
    }

    private fun logPreciosZona(precio: PrecioZona, nombre: String) {
        android.util.Log.d("PriceManager", "$nombre: HS875=$${precio.hs875PrecioVenta}, HS1250=$${precio.hs1250PrecioVenta}, HS1500=$${precio.hs1500PrecioVenta}")
    }

    private fun preciosCambiaron(anterior: PreciosTodasZonas, nuevo: PreciosTodasZonas): Boolean {
        return !preciosIguales(anterior.continental, nuevo.continental) ||
               !preciosIguales(anterior.islas, nuevo.islas) ||
               !preciosIguales(anterior.foranea, nuevo.foranea)
    }

    private fun preciosIguales(a: PrecioZona, b: PrecioZona): Boolean {
        return a.hs875PrecioVenta == b.hs875PrecioVenta &&
               a.hs1250PrecioVenta == b.hs1250PrecioVenta &&
               a.hs1500PrecioVenta == b.hs1500PrecioVenta &&
               a.hs875PrecioBase == b.hs875PrecioBase &&
               a.hs1250PrecioBase == b.hs1250PrecioBase &&
               a.hs1500PrecioBase == b.hs1500PrecioBase
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
        android.util.Log.d("PriceManager", "Precios actualizados localmente")
    }

    /**
     * Actualiza los precios de una zona específica localmente
     */
    fun updatePreciosZona(zona: ZonaGeografica, precios: PrecioZona) {
        val current = _preciosZonas.value
        _preciosZonas.value = when (zona) {
            ZonaGeografica.CONTINENTAL -> current.copy(continental = precios)
            ZonaGeografica.ISLAS -> current.copy(islas = precios)
            ZonaGeografica.FORANEA -> current.copy(foranea = precios)
        }
        lastLoadTime = System.currentTimeMillis()
        actualizarConfigLegacy()
        android.util.Log.d("PriceManager", "Precios de ${zona.nombreDisplay} actualizados localmente")
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

    // ═══════════════════════════════════════════════════════════════════════════
    // FUNCIONES DE ZONA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Establece la zona actual para cotizar
     */
    fun setZonaActual(zona: ZonaGeografica) {
        _zonaActual.value = zona
        actualizarConfigLegacy()
        android.util.Log.d("PriceManager", "Zona actual cambiada a: ${zona.nombreDisplay}")
    }

    /**
     * Establece la zona basándose en la ciudad seleccionada
     */
    fun setZonaFromCiudad(ciudad: String) {
        val zona = ZonasData.detectarZona(ciudad)
        setZonaActual(zona)
        android.util.Log.d("PriceManager", "Zona detectada para '$ciudad': ${zona.nombreDisplay}")
    }

    /**
     * Actualiza el config legacy con los precios de la zona actual
     */
    private fun actualizarConfigLegacy() {
        val precios = _preciosZonas.value.getPreciosZona(_zonaActual.value)
        _currentConfig.value = precios.toAppConfig()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FUNCIONES DE ACCESO A PRECIOS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Obtiene el precio de venta para la zona ACTUAL
     */
    fun getPrecioVenta(producto: TipoProducto): Double {
        return getPrecioVenta(producto, _zonaActual.value)
    }

    /**
     * Obtiene el precio de venta para una zona específica
     */
    fun getPrecioVenta(producto: TipoProducto, zona: ZonaGeografica): Double {
        val precios = _preciosZonas.value.getPreciosZona(zona)
        return when (producto) {
            TipoProducto.HS875 -> precios.hs875PrecioVenta
            TipoProducto.HS1250 -> precios.hs1250PrecioVenta
            TipoProducto.HS1500 -> precios.hs1500PrecioVenta
            TipoProducto.PERSONALIZADO -> precios.hs875PrecioVenta
        }
    }

    /**
     * Obtiene el precio base para la zona ACTUAL
     */
    fun getPrecioBase(producto: TipoProducto): Double {
        return getPrecioBase(producto, _zonaActual.value)
    }

    /**
     * Obtiene el precio base para una zona específica
     */
    fun getPrecioBase(producto: TipoProducto, zona: ZonaGeografica): Double {
        val precios = _preciosZonas.value.getPreciosZona(zona)
        return when (producto) {
            TipoProducto.HS875 -> precios.hs875PrecioBase
            TipoProducto.HS1250 -> precios.hs1250PrecioBase
            TipoProducto.HS1500 -> precios.hs1500PrecioBase
            TipoProducto.PERSONALIZADO -> precios.hs875PrecioBase
        }
    }

    /**
     * Obtiene el descuento máximo para la zona ACTUAL
     */
    fun getMaxDescuento(producto: TipoProducto): Double {
        return getPrecioVenta(producto) - getPrecioBase(producto)
    }

    /**
     * Obtiene el descuento máximo para una zona específica
     */
    fun getMaxDescuento(producto: TipoProducto, zona: ZonaGeografica): Double {
        return getPrecioVenta(producto, zona) - getPrecioBase(producto, zona)
    }

    /**
     * Calcula subtotal para la zona ACTUAL
     */
    fun calcularSubtotal(areaM2: Double, producto: TipoProducto): Double {
        return areaM2 * getPrecioVenta(producto)
    }

    /**
     * Calcula subtotal para una zona específica
     */
    fun calcularSubtotal(areaM2: Double, producto: TipoProducto, zona: ZonaGeografica): Double {
        return areaM2 * getPrecioVenta(producto, zona)
    }

    /**
     * Calcula subtotal con descuento para la zona ACTUAL
     */
    fun calcularSubtotalConDescuento(
        areaM2: Double,
        producto: TipoProducto,
        descuentoPorM2: Double
    ): Double {
        return calcularSubtotalConDescuento(areaM2, producto, descuentoPorM2, _zonaActual.value)
    }

    /**
     * Calcula subtotal con descuento para una zona específica
     */
    fun calcularSubtotalConDescuento(
        areaM2: Double,
        producto: TipoProducto,
        descuentoPorM2: Double,
        zona: ZonaGeografica
    ): Double {
        val precioVenta = getPrecioVenta(producto, zona)
        val precioBase = getPrecioBase(producto, zona)
        val descuentoAplicado = descuentoPorM2.coerceAtMost(precioVenta - precioBase)
        val precioFinal = (precioVenta - descuentoAplicado).coerceAtLeast(precioBase)
        return areaM2 * precioFinal
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FUNCIONES DE VERIFICACIÓN
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifica si hay precios nuevos comparando con el servidor
     */
    suspend fun checkForUpdates(): Boolean {
        return try {
            val serverPrecios = AdminRepository.getPreciosTodasZonas()
            val currentPrecios = _preciosZonas.value

            val changed = preciosCambiaron(currentPrecios, serverPrecios)

            if (changed) {
                _preciosZonas.value = serverPrecios
                lastLoadTime = System.currentTimeMillis()
                _preciosCambiaron.value = true
                actualizarConfigLegacy()
                android.util.Log.d("PriceManager", "Precios actualizados desde servidor")
            }

            changed
        } catch (e: Exception) {
            android.util.Log.e("PriceManager", "Error verificando actualizaciones: ${e.message}")
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FUNCIONES DE FORMATO
    // ═══════════════════════════════════════════════════════════════════════════

    fun formatPrice(amount: Double): String {
        val format = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)}"
    }

    fun getPricesSummary(): String {
        val zona = _zonaActual.value
        val precios = _preciosZonas.value.getPreciosZona(zona)
        return buildString {
            appendLine("${zona.nombreDisplay}")
            appendLine("HS-875: ${formatPrice(precios.hs875PrecioVenta)} / m²")
            appendLine("HS-1250: ${formatPrice(precios.hs1250PrecioVenta)} / m²")
            appendLine("HS-1500: ${formatPrice(precios.hs1500PrecioVenta)} / m²")
        }
    }

    fun getPricesSummaryForZona(zona: ZonaGeografica): String {
        val precios = _preciosZonas.value.getPreciosZona(zona)
        return buildString {
            appendLine("HS-875: ${formatPrice(precios.hs875PrecioVenta)} / m²")
            appendLine("HS-1250: ${formatPrice(precios.hs1250PrecioVenta)} / m²")
            appendLine("HS-1500: ${formatPrice(precios.hs1500PrecioVenta)} / m²")
        }
    }
}
