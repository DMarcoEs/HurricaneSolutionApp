package com.example.hurricansolutionapp

object Routes {
    // ═══════════════════════════════════════════════════════════════════════════════
    // RUTAS COMUNES
    // ═══════════════════════════════════════════════════════════════════════════════
    const val LOGIN = "login"

    // ═══════════════════════════════════════════════════════════════════════════════
    // RUTAS ESPECIALISTA
    // ═══════════════════════════════════════════════════════════════════════════════
    const val HOME = "home"
    const val CLIENTE = "cliente"
    const val MEDIDAS = "medidas"
    const val RESUMEN = "resumen"
    const val HISTORIAL = "historial"
    const val PENDIENTES = "pendientes"
    const val PENDIENTES_DRIVE = "pendientes_drive"

    // ═══════════════════════════════════════════════════════════════════════════════
    // RUTAS ADMINISTRADOR
    // ═══════════════════════════════════════════════════════════════════════════════
    const val ADMIN_HOME = "admin_home"
    const val ADMIN_PRECIOS = "admin_precios"
    const val ADMIN_COTIZACIONES = "admin_cotizaciones"
    const val ADMIN_COTIZACION_DETALLE = "admin_cotizacion_detalle/{folio}"
    const val ADMIN_EMPLEADOS = "admin_empleados"
    const val ADMIN_LEADS = "admin_leads"
    const val ADMIN_METROS = "admin_metros"  // Nueva ruta para metros cuadrados

    const val SELECCION_CLIENTE = "seleccion_cliente"

    // Helper para navegar al detalle con folio
    fun adminCotizacionDetalle(folio: String) = "admin_cotizacion_detalle/$folio"
}