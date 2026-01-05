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
 */
object DriveAuthManager {

    private const val TAG = "DriveAuth"
    private var driveService: Drive? = null

    /**
     * Obtiene el cliente de Google Sign-In configurado
     * ✅ CORREGIDO: Removido requestServerAuthCode que causaba error 10
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            // ✅ REMOVIDO: .requestServerAuthCode() - no es necesario para Android
            .build()

        return GoogleSignIn.getClient(context, signInOptions)
    }

    /**
     * Verifica si el usuario ya está autenticado
     */
    fun isAuthenticated(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && hasRequiredScopes(account)
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
        return client.signInIntent
    }

    /**
     * Procesa el resultado de la autenticación
     */
    suspend fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.await()

            android.util.Log.d(TAG, "✅ Autenticación exitosa: ${account.email}")
            Result.success(account)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error en autenticación: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene el servicio de Google Drive configurado
     */
    fun getDriveService(context: Context): Drive? {
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
     * Revoca el acceso de la aplicación
     */
    suspend fun revokeAccess(context: Context): Result<Unit> {
        return try {
            val client = getGoogleSignInClient(context)
            client.revokeAccess().await()
            driveService = null

            android.util.Log.d(TAG, "✅ Acceso revocado")
            Result.success(Unit)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error revocando acceso: ${e.message}", e)
            Result.failure(e)
        }
    }
}