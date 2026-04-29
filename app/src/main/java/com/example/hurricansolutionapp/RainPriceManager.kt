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
 * - Tela: $312.30/m²
 * - Kit Manual (Componentes Toldo /Incluye Manivela): $12,952.26
 * - Kit Eléctrico (Motor bidireccional): $14,158.13 + Adaptador $2,017.71
 * - Perfil (Tubo 70mm): $1,348.90 × ancho
 * - Contrapeso: $743.33 × ancho
 * - Inserto: $32.05 × ancho × 2
 * - Tensor: $29.97 × alto × 2
 * - Control adicional: $600.00 unidad
 *
 * Componentes de referencia (solo admin, no afectan cálculo):
 * - Manivela, Intermedio conector, Control Bmighty, etc.
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
        // Componentes compartidos
        "tela" to 312.30,
        "perfil" to 1348.90,
        "contrapeso" to 743.33,
        "inserto" to 32.05,
        "tensor" to 29.97,
        // Componentes Manual
        "kit_manual" to 12952.26,
        "manivela" to 892.67,
        // Componentes Eléctrico
        "kit_adaptador" to 2017.71,
        "componentes_toldo_electrico" to 12745.04,
        "intermedio_conector" to 567.45,
        "inserto_plastico" to 32.05,
        "kit_electrico" to 14158.13,
        "control_multicanal" to 2438.01,
        "control_monocanal" to 2010.42,
        // Accesorios adicionales
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
     * MANUAL:
     * - Tela: alto × ancho × precio × piezas
     * - Perfil: ancho × precio × piezas
     * - Contrapeso: ancho × precio × piezas
     * - Inserto: ancho × 2 × piezas × precio
     * - Tensor: alto × 2 × piezas × precio
     * - Kit Manual (incluye manivela): precio × piezas
     *
     * ELÉCTRICO:
     * - Tela: alto × ancho × precio × piezas
     * - Kit Adaptador: precio × piezas
     * - Componentes Toldo: precio × piezas
     * - Perfil: ancho × precio × piezas
     * - Intermedio conector: (piezas - 1) × precio (solo si piezas > 1)
     * - Contrapeso: ancho × precio × piezas
     * - Inserto Plástico: ancho × 2 × piezas × precio
     * - Kit Motor: precio × piezas
     * - Control Multicanal: 1 × precio (default, siempre incluido)
     * - NO tensor en eléctrico
     */
    fun calcularSubtotalArea(alto: Double, ancho: Double, tipoMecanismo: TipoMecanismo, piezas: Int = 1): Double {
        val p = piezas.coerceAtLeast(1)
        val m2 = alto * ancho

        return when (tipoMecanismo) {
            TipoMecanismo.MANUAL -> {
                val costoTela = m2 * getPrecio("tela") * p
                val costoPerfil = ancho * getPrecio("perfil") * p
                val costoContrapeso = ancho * getPrecio("contrapeso") * p
                val costoInserto = ancho * 2.0 * p * getPrecio("inserto")
                val costoTensor = alto * 2.0 * p * getPrecio("tensor")
                val costoKit = getPrecio("kit_manual") * p  // Ya incluye manivela

                costoTela + costoPerfil + costoContrapeso + costoInserto + costoTensor + costoKit
            }
            TipoMecanismo.ELECTRICO -> {
                val costoTela = m2 * getPrecio("tela") * p
                val costoAdaptador = getPrecio("kit_adaptador") * p
                val costoComponentes = getPrecio("componentes_toldo_electrico") * p
                val costoPerfil = ancho * getPrecio("perfil") * p
                val costoIntermedio = if (p > 1) (p - 1) * getPrecio("intermedio_conector") else 0.0
                val costoContrapeso = ancho * getPrecio("contrapeso") * p
                val costoInserto = ancho * 2.0 * p * getPrecio("inserto_plastico")
                val costoMotor = getPrecio("kit_electrico") * p
                val costoControl = getPrecio("control_multicanal")  // 1 por default siempre

                costoTela + costoAdaptador + costoComponentes + costoPerfil + costoIntermedio +
                        costoContrapeso + costoInserto + costoMotor + costoControl
            }
        }
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
     * - Manivelas adicionales: cantidad × precio_manivela (solo para Manual)
     * - Controles adicionales: depende del tipo seleccionado
     *   - Si cambiaron de Multicanal (default) a Monocanal: cantidad × precio_monocanal
     *     NOTA: El Multicanal default ya está incluido en calcularSubtotalArea
     *     Si eligen Monocanal, se REEMPLAZA el multicanal default por monocanales
     */
    fun calcularCostoAccesorios(cantidadManivelas: Int, tipoControl: String, cantidadControles: Int): Double {
        val costoManivelas = cantidadManivelas * getPrecio("manivela")

        // Si el tipo de control es monocanal, se reemplaza el multicanal default
        // Costo = (cantidad × monocanal) - (1 × multicanal que ya se cobró en el subtotal)
        val costoControles = if (tipoControl == "monocanal" && cantidadControles > 0) {
            val costoMonocanales = cantidadControles * getPrecio("control_monocanal")
            val creditoMulticanal = getPrecio("control_multicanal") // Devolver el default que ya se cobró
            costoMonocanales - creditoMulticanal
        } else {
            0.0 // Multicanal default ya incluido en el subtotal
        }

        return costoManivelas + costoControles
    }

    /**
     * Obtiene el desglose de costos para un área (solo para debug/admin, no mostrar al especialista)
     */
    fun getDesgloseArea(alto: Double, ancho: Double, tipoMecanismo: TipoMecanismo, piezas: Int = 1): Map<String, Double> {
        val m2 = alto * ancho
        val p = piezas.coerceAtLeast(1)

        return when (tipoMecanismo) {
            TipoMecanismo.MANUAL -> mutableMapOf(
                "Tela (${String.format("%.2f", m2)} m² × $p pzas)" to (m2 * getPrecio("tela") * p),
                "Perfil (${String.format("%.2f", ancho)} m × $p)" to (ancho * getPrecio("perfil") * p),
                "Contrapeso (${String.format("%.2f", ancho)} m × $p)" to (ancho * getPrecio("contrapeso") * p),
                "Inserto (${String.format("%.2f", ancho)} m × 2 × $p)" to (ancho * 2.0 * p * getPrecio("inserto")),
                "Tensor (${String.format("%.2f", alto)} m × 2 × $p)" to (alto * 2.0 * p * getPrecio("tensor")),
                "Kit Manual (incluye manivela) × $p" to (getPrecio("kit_manual") * p)
            )
            TipoMecanismo.ELECTRICO -> {
                val desglose = mutableMapOf(
                    "Tela (${String.format("%.2f", m2)} m² × $p pzas)" to (m2 * getPrecio("tela") * p),
                    "Kit Adaptador × $p" to (getPrecio("kit_adaptador") * p),
                    "Componentes Toldo × $p" to (getPrecio("componentes_toldo_electrico") * p),
                    "Perfil (${String.format("%.2f", ancho)} m × $p)" to (ancho * getPrecio("perfil") * p),
                    "Contrapeso (${String.format("%.2f", ancho)} m × $p)" to (ancho * getPrecio("contrapeso") * p),
                    "Inserto Plástico (${String.format("%.2f", ancho)} m × 2 × $p)" to (ancho * 2.0 * p * getPrecio("inserto_plastico")),
                    "Kit Motor × $p" to (getPrecio("kit_electrico") * p),
                    "Control Multicanal (default)" to getPrecio("control_multicanal")
                )
                if (p > 1) {
                    desglose["Intermedio conector (${p - 1} uds)"] = (p - 1) * getPrecio("intermedio_conector")
                }
                desglose
            }
        }
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