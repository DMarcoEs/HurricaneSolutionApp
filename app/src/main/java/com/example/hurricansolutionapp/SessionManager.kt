package com.example.hurricansolutionapp

import android.content.Context

private const val PREFS_SESSION = "session_prefs"
private const val KEY_USER_ID = "user_id"
private const val KEY_NOMBRE = "user_name"
private const val KEY_ROLE = "user_role"
private const val KEY_DARK_MODE = "dark_mode"

/**
 * Manager de sesión de usuario
 * ACTUALIZADO: Incluye helpers para rol INSTALADOR
 */
object SessionManager {

    // ═══════════════════════════════════════════════════════════════════════════════
    // FUNCIONES BÁSICAS DE SESIÓN
    // ═══════════════════════════════════════════════════════════════════════════════

    fun login(context: Context, userId: String, nombre: String, role: String) {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putString(KEY_NOMBRE, nombre)
            .putString(KEY_ROLE, role)
            .apply()
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        val darkMode = isDarkMode(context)
        prefs.edit().clear().apply()
        setDarkMode(context, darkMode)
    }

    fun getUserId(context: Context): String =
        context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
            .getString(KEY_USER_ID, "") ?: ""

    fun getNombre(context: Context): String =
        context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
            .getString(KEY_NOMBRE, "") ?: ""

    fun getRole(context: Context): String =
        context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
            .getString(KEY_ROLE, "") ?: ""

    /** Alias de getNombre para compatibilidad */
    fun getEspecialista(context: Context): String = getNombre(context)

    fun isLoggedIn(context: Context): Boolean = getUserId(context).isNotBlank()

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPERS DE ROL (NUEVO)
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Verifica si el usuario es administrador */
    fun isAdmin(context: Context): Boolean =
        getRole(context) == UserRoles.ADMIN

    /** Verifica si el usuario es especialista */
    fun isSpecialist(context: Context): Boolean =
        getRole(context) == UserRoles.SPECIALIST

    /** Verifica si el usuario es instalador */
    fun isInstaller(context: Context): Boolean =
        getRole(context) == UserRoles.INSTALLER

    /**
     * Obtiene la ruta inicial según el rol del usuario
     * @return Ruta de navegación correspondiente al rol
     */
    fun getInitialRoute(context: Context): String {
        return when (getRole(context)) {
            UserRoles.ADMIN -> Routes.ADMIN_HOME
            UserRoles.INSTALLER -> Routes.INSTALADOR_HOME
            else -> Routes.HOME
        }
    }

    /**
     * Verifica si el usuario puede crear cotizaciones
     * Solo ADMIN y SPECIALIST pueden crear cotizaciones
     */
    fun canCreateCotizaciones(context: Context): Boolean {
        val role = getRole(context)
        return role == UserRoles.ADMIN || role == UserRoles.SPECIALIST
    }

    /**
     * Verifica si el usuario puede ver/editar medidas de instalador
     * Solo ADMIN e INSTALLER pueden editar
     */
    fun canEditMedidasInstalador(context: Context): Boolean {
        val role = getRole(context)
        return role == UserRoles.ADMIN || role == UserRoles.INSTALLER
    }

    /**
     * Verifica si el usuario puede subir a carpeta de instaladores en Drive
     */
    fun canUploadToInstaladorDrive(context: Context): Boolean {
        val role = getRole(context)
        return role == UserRoles.ADMIN || role == UserRoles.INSTALLER
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // PERSISTENCIA DEL TEMA
    // ═══════════════════════════════════════════════════════════════════════════════

    fun setDarkMode(context: Context, isDark: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_DARK_MODE, isDark)
            .apply()
    }

    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DARK_MODE, true)
    }
}