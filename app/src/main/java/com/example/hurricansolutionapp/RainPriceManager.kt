package com.example.hurricansolutionapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * RAIN PRICE MANAGER - GESTOR DE PRECIOS Y DESCUENTOS
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Calcula precios para Rain Protection basado en:
 * - Tela: $173.50/m²
 * - Kit Manual: $7,195.70 + Manivela $495.93
 * - Kit Eléctrico: $7,865.63 + Adaptador $1,120.95
 * - Perfil: $749.39 × ancho
 * - Contrapeso: $412.96 × ancho
 * - Inserto: $17.80 × ancho × 2
 * - Tensor: $16.65 × alto × 2
 * - Control adicional: $1,500.00 unidad
 *
 * Descuentos por zona:
 * - Continental: 23.50%
 * - Islas: 16.40%
 * - Foránea: 8.50%
 */
object RainPriceManager {

    // ═══════════════════════════════════════════════════════════════════════════
    // ESTADOS
    // ═══════════════════════════════════════════════════════════════════════════

    private val _precios = MutableStateFlow<Map<String, Double>>(getDefaultPrecios())
    val precios: StateFlow<Map<String, Double>> = _precios.asStateFlow()

    private val _descuentos = MutableStateFlow<Map<String, Double>>(getDefaultDescuentos())
    val descuentos: StateFlow<Map<String, Double>> = _descuentos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _preciosCargados = MutableStateFlow(false)
    val preciosCargados: StateFlow<Boolean> = _preciosCargados.asStateFlow()

    private var lastLoadTime: Long = 0
    private const val CACHE_DURATION = 60 * 1000 // 1 minuto

    // ═══════════════════════════════════════════════════════════════════════════
    // VALORES POR DEFECTO
    // ═══════════════════════════════════════════════════════════════════════════

    private fun getDefaultPrecios(): Map<String, Double> = mapOf(
        "tela" to 173.50,
        "kit_manual" to 7195.70,
        "kit_electrico" to 7865.63,
        "perfil" to 749.39,
        "contrapeso" to 412.96,
        "inserto" to 17.80,
        "tensor" to 16.65,
        "manivela" to 495.93,
        "kit_adaptador" to 1120.95,
        "control_adicional" to 600.00
    )

    private fun getDefaultDescuentos(): Map<String, Double> = mapOf(
        "continental" to 23.50,
        "islas" to 16.40,
        "foranea" to 8.50
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // CARGA DESDE SUPABASE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Carga precios y descuentos desde Supabase
     */
    suspend fun loadPrecios(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()

        if (!forceRefresh && (now - lastLoadTime) < CACHE_DURATION && lastLoadTime > 0) {
            android.util.Log.d("RainPriceManager", "Usando precios en caché")
            return
        }

        _isLoading.value = true
        try {
            // Cargar precios de componentes
            val preciosRemoto = RainRepository.getPrecios()
            if (preciosRemoto.isNotEmpty()) {
                _precios.value = preciosRemoto.associate { it.componente to it.precio }
                android.util.Log.d("RainPriceManager", "Precios cargados: ${_precios.value.size}")
            }

            // Cargar descuentos por zona
            val descuentosRemoto = RainRepository.getDescuentos()
            if (descuentosRemoto.isNotEmpty()) {
                _descuentos.value = descuentosRemoto.associate { it.zona to it.descuento }
                android.util.Log.d("RainPriceManager", "Descuentos cargados: ${_descuentos.value.size}")
            }

            _preciosCargados.value = true
            lastLoadTime = now

        } catch (e: Exception) {
            android.util.Log.e("RainPriceManager", "Error cargando precios: ${e.message}")
            // Usar valores por defecto si falla
            if (_precios.value.isEmpty()) {
                _precios.value = getDefaultPrecios()
            }
            if (_descuentos.value.isEmpty()) {
                _descuentos.value = getDefaultDescuentos()
            }
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Fuerza recarga de precios
     */
    suspend fun refreshPrecios() {
        loadPrecios(forceRefresh = true)
    }

    /**
     * Invalida el caché
     */
    fun invalidateCache() {
        lastLoadTime = 0
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // OBTENER PRECIOS
    // ═══════════════════════════════════════════════════════════════════════════

    fun getPrecio(componente: String): Double {
        return _precios.value[componente] ?: getDefaultPrecios()[componente] ?: 0.0
    }

    fun getDescuentoPorZona(zona: ZonaGeografica): Double {
        val zonaId = zona.id
        return _descuentos.value[zonaId] ?: getDefaultDescuentos()[zonaId] ?: 0.0
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CÁLCULO DE PRECIOS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calcula el subtotal de un área (sin descuento de zona)
     *
     * Fórmula por pieza:
     * - Tela: alto × ancho × precio_tela
     * - Perfil: ancho × precio_perfil
     * - Contrapeso: ancho × precio_contrapeso
     * - Inserto: ancho × 2 × precio_inserto
     * - Tensor: alto × 2 × precio_tensor
     * - Kit: según tipo (manual o eléctrico)
     *
     * Total = subtotal_por_pieza × piezas
     * Cada pieza lleva su propio mecanismo (kit)
     */
    fun calcularSubtotalArea(alto: Double, ancho: Double, tipoMecanismo: TipoMecanismo, piezas: Int = 1): Double {
        val m2 = alto * ancho

        // Tela
        val costoTela = m2 * getPrecio("tela")

        // Perfil (ancho)
        val costoPerfil = ancho * getPrecio("perfil")

        // Contrapeso (ancho)
        val costoContrapeso = ancho * getPrecio("contrapeso")

        // Inserto (ancho × 2)
        val costoInserto = ancho * 2 * getPrecio("inserto")

        // Tensor (alto × 2)
        val costoTensor = alto * 2 * getPrecio("tensor")

        // Kit según mecanismo
        val costoKit = when (tipoMecanismo) {
            TipoMecanismo.MANUAL -> getPrecio("kit_manual") + getPrecio("manivela")
            TipoMecanismo.ELECTRICO -> getPrecio("kit_electrico") + getPrecio("kit_adaptador")
        }

        val subtotalPorPieza = costoTela + costoPerfil + costoContrapeso + costoInserto + costoTensor + costoKit
        return subtotalPorPieza * piezas.coerceAtLeast(1)
    }

    /**
     * Calcula el total con descuento de zona
     */
    fun calcularTotalConDescuento(subtotal: Double, zona: ZonaGeografica): Double {
        val descuentoPorcentaje = getDescuentoPorZona(zona)
        val descuentoMonto = subtotal * (descuentoPorcentaje / 100)
        return subtotal - descuentoMonto
    }

    /**
     * Calcula el costo de accesorios adicionales (SIN descuento de zona)
     * - Controles adicionales: cantidad × precio_control_adicional
     * - Manivelas adicionales: cantidad × precio_manivela
     */
    fun calcularCostoAccesorios(cantidadControles: Int, cantidadManivelas: Int): Double {
        val costoControles = cantidadControles * getPrecio("control_adicional")
        val costoManivelas = cantidadManivelas * getPrecio("manivela")
        return costoControles + costoManivelas
    }

    /**
     * Obtiene el desglose de costos para un área (solo para debug/admin, no mostrar al especialista)
     */
    fun getDesgloseArea(alto: Double, ancho: Double, tipoMecanismo: TipoMecanismo): Map<String, Double> {
        val m2 = alto * ancho

        return mapOf(
            "Tela (${String.format("%.2f", m2)} m²)" to (m2 * getPrecio("tela")),
            "Perfil (${String.format("%.2f", ancho)} m)" to (ancho * getPrecio("perfil")),
            "Contrapeso (${String.format("%.2f", ancho)} m)" to (ancho * getPrecio("contrapeso")),
            "Inserto (${String.format("%.2f", ancho)} m × 2)" to (ancho * 2 * getPrecio("inserto")),
            "Tensor (${String.format("%.2f", alto)} m × 2)" to (alto * 2 * getPrecio("tensor")),
            if (tipoMecanismo == TipoMecanismo.MANUAL) "Kit Manual" to getPrecio("kit_manual")
            else "Kit Eléctrico" to getPrecio("kit_electrico"),
            if (tipoMecanismo == TipoMecanismo.MANUAL) "Manivela" to getPrecio("manivela")
            else "Kit Adaptador" to getPrecio("kit_adaptador")
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Verifica si hay precios configurados
     */
    fun hayPreciosConfigurados(): Boolean = _precios.value.isNotEmpty()

    /**
     * Formatea precio como moneda USD
     */
    fun formatPrice(amount: Double): String {
        val format = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)}"
    }

    /**
     * Actualiza precios localmente (después de que admin los guarde)
     */
    fun updatePreciosLocal(nuevosPrecios: Map<String, Double>) {
        _precios.value = nuevosPrecios
        lastLoadTime = System.currentTimeMillis()
    }

    /**
     * Actualiza descuentos localmente (después de que admin los guarde)
     */
    fun updateDescuentosLocal(nuevosDescuentos: Map<String, Double>) {
        _descuentos.value = nuevosDescuentos
        lastLoadTime = System.currentTimeMillis()
    }
}