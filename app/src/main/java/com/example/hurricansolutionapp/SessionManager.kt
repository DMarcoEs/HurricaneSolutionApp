package com.example.hurricansolutionapp

import android.content.Context

private const val PREFS_SESSION = "session_prefs"
private const val KEY_USER_ID = "user_id"
private const val KEY_NOMBRE = "user_name"
private const val KEY_ROLE = "user_role"
private const val KEY_DARK_MODE = "dark_mode"

object SessionManager {

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
        // Guardamos el tema antes de limpiar
        val darkMode = isDarkMode(context)
        prefs.edit().clear().apply()
        // Restauramos el tema después de logout
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

    // Para no romper tu app: mantenemos este nombre de función
    fun getEspecialista(context: Context): String = getNombre(context)

    fun isLoggedIn(context: Context): Boolean = getUserId(context).isNotBlank()

    // ═══════════════════════════════════════════════════════════════════════════
    // PERSISTENCIA DEL TEMA (Dark Mode / Light Mode)
    // ═══════════════════════════════════════════════════════════════════════════

    fun setDarkMode(context: Context, isDark: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_DARK_MODE, isDark)
            .apply()
    }

    fun isDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        // Por defecto true (dark mode)
        return prefs.getBoolean(KEY_DARK_MODE, true)
    }
}