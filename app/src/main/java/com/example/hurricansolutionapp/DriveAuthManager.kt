package com.example.hurricansolutionapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.tasks.await

/**
 * Manager para autenticación OAuth 2.0 con Google Drive
 *
 * ACTUALIZADO: Manejo de múltiples cuentas para evitar error 10 y archivos en limbo
 */
object DriveAuthManager {

    private const val TAG = "DriveAuth"
    private const val PREFS_NAME = "drive_auth_prefs"
    private const val KEY_AUTHORIZED_EMAIL = "authorized_account_email"

    private var driveService: Drive? = null

    /**
     * Obtiene el cliente de Google Sign-In configurado
     * [OK] CORREGIDO: Removido requestServerAuthCode que causaba error 10
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            // [OK] REMOVIDO: .requestServerAuthCode() - no es necesario para Android
            .build()

        return GoogleSignIn.getClient(context, signInOptions)
    }

    /**
     * Verifica si el usuario ya está autenticado CON LA CUENTA CORRECTA
     *
     * IMPORTANTE: Ahora verifica que sea la cuenta autorizada para evitar problemas
     * con múltiples cuentas que causan error 10 y archivos en limbo
     */
    fun isAuthenticated(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account == null || !hasRequiredScopes(account)) {
            return false
        }

        // Verificar que sea la cuenta autorizada
        return isCurrentAccountAuthorized(context)
    }

    /**
     * Verifica si la cuenta tiene los scopes necesarios
     */
    private fun hasRequiredScopes(account: GoogleSignInAccount): Boolean {
        val requiredScope = Scope(DriveScopes.DRIVE_FILE)
        return account.grantedScopes.contains(requiredScope)
    }

    /**
     * Obtiene la cuenta de Google autenticada
     */
    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    /**
     * Inicia el flujo de autenticación
     * Retorna el Intent que debe lanzarse con startActivityForResult
     */
    fun getSignInIntent(context: Context): Intent {
        val client = getGoogleSignInClient(context)

        // Log si hay cuenta autorizada previa
        val authorizedEmail = getLastAuthorizedAccountEmail(context)
        if (authorizedEmail != null) {
            android.util.Log.d(TAG, "📧 Se requiere iniciar sesión con: $authorizedEmail")
            android.util.Log.d(
                TAG,
                "⚠️ Si hay múltiples cuentas, selecciona la correcta para evitar errores"
            )
        }

        return client.signInIntent
    }

    /**
     * Procesa el resultado de la autenticación
     *
     * ACTUALIZADO: Guarda el email de la cuenta autorizada para validaciones futuras
     */
    suspend fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.await()

            android.util.Log.d(TAG, "✅ Autenticación exitosa: ${account.email}")

            // Guardar como cuenta autorizada
            saveAuthorizedAccountEmail(
                context = data?.extras?.get("context") as? Context,
                account.email ?: ""
            )

            Result.success(account)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error en autenticación: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Guarda el email de la cuenta autorizada para uso futuro
     *
     * Esto permite validar que siempre se use la misma cuenta y evitar
     * el problema de múltiples cuentas que causa error 10 y archivos en limbo
     */
    fun saveAuthorizedAccountEmail(context: Context?, email: String) {
        if (context == null || email.isBlank()) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AUTHORIZED_EMAIL, email).apply()
        android.util.Log.d(TAG, "💾 Cuenta autorizada guardada: $email")
    }

    /**
     * Obtiene el email de la última cuenta autorizada
     */
    fun getLastAuthorizedAccountEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_AUTHORIZED_EMAIL, null)
    }

    /**
     * Verifica si la cuenta actual es la cuenta autorizada
     *
     * CRÍTICO: Esto previene el problema de múltiples cuentas que causa:
     * - Error 10 de autorización
     * - Archivos que quedan en limbo
     * - Subidas que no aparecen en "Pendientes Drive"
     */
    fun isCurrentAccountAuthorized(context: Context): Boolean {
        val currentAccount = getSignedInAccount(context)
        val authorizedEmail = getLastAuthorizedAccountEmail(context)

        if (currentAccount == null) {
            android.util.Log.w(TAG, "⚠️ No hay cuenta activa")
            return false
        }

        if (authorizedEmail == null) {
            // Si no hay cuenta autorizada guardada, guardar la actual como autorizada
            android.util.Log.d(
                TAG,
                "📝 Primera vez: Guardando ${currentAccount.email} como cuenta autorizada"
            )
            saveAuthorizedAccountEmail(context, currentAccount.email ?: "")
            return true
        }

        val isAuthorized = currentAccount.email == authorizedEmail

        if (!isAuthorized) {
            android.util.Log.w(TAG, "❌ PROBLEMA DE MÚLTIPLES CUENTAS DETECTADO")
            android.util.Log.w(TAG, "   Cuenta actual: ${currentAccount.email}")
            android.util.Log.w(TAG, "   Cuenta autorizada: $authorizedEmail")
            android.util.Log.w(TAG, "   Esto causará error 10 y archivos en limbo")
        } else {
            android.util.Log.d(TAG, "✅ Cuenta correcta: ${currentAccount.email}")
        }

        return isAuthorized
    }

    /**
     * Cierra sesión de la cuenta actual si no es la autorizada
     *
     * Útil para forzar el inicio de sesión con la cuenta correcta
     */
    suspend fun signOutIfWrongAccount(context: Context): Boolean {
        if (!isCurrentAccountAuthorized(context)) {
            android.util.Log.d(TAG, "🔄 Cerrando sesión de cuenta incorrecta")
            signOut(context)
            return true
        }
        return false
    }

    /**
     * Obtiene el servicio de Google Drive configurado
     *
     * ACTUALIZADO: Valida que sea la cuenta autorizada antes de crear el servicio
     */
    fun getDriveService(context: Context): Drive? {
        // Validar que sea la cuenta autorizada
        if (!isCurrentAccountAuthorized(context)) {
            android.util.Log.e(TAG, "❌ No se puede crear servicio: cuenta no autorizada")
            return null
        }

        if (driveService != null) {
            return driveService
        }

        val account = getSignedInAccount(context) ?: return null

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName(ApiConfig.DRIVE_APPLICATION_NAME)
            .build()

        android.util.Log.d(TAG, "✅ Servicio Drive creado con cuenta: ${account.email}")
        return driveService
    }

    /**
     * Cierra sesión de Google Drive
     */
    suspend fun signOut(context: Context): Result<Unit> {
        return try {
            val client = getGoogleSignInClient(context)
            client.signOut().await()
            driveService = null

            android.util.Log.d(TAG, "✅ Sesión cerrada")
            Result.success(Unit)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error cerrando sesión: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Revoca el acceso de la aplicación Y limpia la cuenta autorizada
     */
    suspend fun revokeAccess(context: Context): Result<Unit> {
        return try {
            val client = getGoogleSignInClient(context)
            client.revokeAccess().await()
            driveService = null

            // Limpiar cuenta autorizada
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_AUTHORIZED_EMAIL).apply()

            android.util.Log.d(TAG, "✅ Acceso revocado y cuenta autorizada limpiada")
            Result.success(Unit)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error revocando acceso: ${e.message}", e)
            Result.failure(e)
        }
    }
}