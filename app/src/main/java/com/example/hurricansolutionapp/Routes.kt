package com.example.hurricansolutionapp

/**
 * Rutas de navegación de la aplicación Hurricane Solution
 */
object Routes {
    // ═══════════════════════════════════════════════════════════════════════════════
    // AUTENTICACIÓN
    // ═══════════════════════════════════════════════════════════════════════════════
    const val LOGIN = "login"

    // ═══════════════════════════════════════════════════════════════════════════════
    // ESPECIALISTA (SPECIALIST)
    // ═══════════════════════════════════════════════════════════════════════════════
    const val HOME = "home"
    const val CLIENTE = "cliente"
    const val SELECCION_CLIENTE = "seleccion_cliente"
    const val TIPO_PROPIEDAD = "tipo_propiedad"
    const val MEDIDAS = "medidas"
    const val RESUMEN = "resumen"
    const val HISTORIAL = "historial"
    const val PENDIENTES = "pendientes"
    const val PENDIENTES_DRIVE = "pendientes_drive"

    // Envíos a Instalación (Especialista y Admin)
    const val ENVIOS_INSTALACION = "envios_instalacion"

    // ═══════════════════════════════════════════════════════════════════════════════
    // ADMINISTRADOR (ADMIN)
    // ═══════════════════════════════════════════════════════════════════════════════
    const val ADMIN_HOME = "admin_home"
    const val ADMIN_LEADS = "admin_leads"
    const val ADMIN_COTIZACIONES = "admin_cotizaciones"
    const val ADMIN_COTIZACION_DETALLE = "admin_cotizacion_detalle/{folio}"
    const val ADMIN_EMPLEADOS = "admin_empleados"
    const val ADMIN_PRECIOS = "admin_precios"
    const val ADMIN_METROS = "admin_metros"

    // ═══════════════════════════════════════════════════════════════════════════════
    // INSTALADOR (INSTALLER)
    // ═══════════════════════════════════════════════════════════════════════════════
    const val INSTALADOR_HOME = "instalador_home"
    const val INSTALADOR_MEDIDAS_LIST = "instalador_medidas_list"
    const val INSTALADOR_FORM = "instalador_form/{folio}"
    const val INSTALADOR_RESUMEN = "instalador_resumen/{folio}"
    const val INSTALADOR_DRIVE = "instalador_drive"

    // ═══════════════════════════════════════════════════════════════════════════════
    // SELECCIÓN DE PRODUCTO
    // ═══════════════════════════════════════════════════════════════════════════════
    const val SELECCION_PRODUCTO = "seleccion_producto"

    // ═══════════════════════════════════════════════════════════════════════════════
    // RAIN PROTECTION - FLUJO COMPLETO
    // ═══════════════════════════════════════════════════════════════════════════════
    const val RAIN_CLIENTE = "rain_cliente"              // Selección de lead o cliente nuevo
    const val RAIN_DATOS = "rain_datos"                  // Captura de datos del cliente
    const val RAIN_TIPO_PROPIEDAD = "rain_tipo_propiedad" // Selección tipo propiedad
    const val RAIN_MEDIDAS = "rain_medidas"              // Captura de medidas
    const val RAIN_RESUMEN = "rain_resumen"              // Resumen y generación PDF

    // Admin Rain
    const val ADMIN_RAIN_PRECIOS = "admin_rain_precios"

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPERS PARA RUTAS CON PARÁMETROS
    // ═══════════════════════════════════════════════════════════════════════════════
    fun adminCotizacionDetalle(folio: String): String = "admin_cotizacion_detalle/$folio"
    fun instaladorForm(folio: String): String = "instalador_form/$folio"
    fun instaladorResumen(folio: String): String = "instalador_resumen/$folio"
}

/**
 * Roles de usuario soportados
 */
object UserRoles {
    const val ADMIN = "ADMIN"
    const val SPECIALIST = "SPECIALIST"
    const val INSTALLER = "INSTALLER"
}