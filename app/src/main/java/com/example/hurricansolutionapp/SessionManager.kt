package com.example.hurricansolutionapp

import android.content.Context

private const val PREFS_SESSION = "session_prefs"
private const val KEY_USER_ID = "user_id"
private const val KEY_NOMBRE = "user_name"
private const val KEY_ROLE = "user_role"

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
        prefs.edit().clear().apply()
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
}
