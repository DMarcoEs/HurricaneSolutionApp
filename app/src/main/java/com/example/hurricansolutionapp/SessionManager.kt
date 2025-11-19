package com.example.hurricansolutionapp

import android.content.Context

private const val PREFS_SESSION = "session_prefs"
private const val KEY_ESPECIALISTA = "especialista_nombre"

object SessionManager {

    fun login(context: Context, nombreEspecialista: String) {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ESPECIALISTA, nombreEspecialista)
            .apply()
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        prefs.edit()
            .clear()
            .apply()
    }

    fun getEspecialista(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ESPECIALISTA, "") ?: ""
    }

    fun isLoggedIn(context: Context): Boolean {
        return getEspecialista(context).isNotBlank()
    }
}