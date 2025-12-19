@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.hurricansolutionapp

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class UsuarioApp(
    val correo: String,
    val nombre: String,
    val role: String,
    val userId: String
)
@Serializable
data class ProfileRow(
    val id: String,
    val name: String,
    val role: String,
    @SerialName("is_active") val isActive: Boolean = true
)

object AuthRepository {

    suspend fun login(correo: String, password: String): UsuarioApp {
        val client = SupabaseClientProvider.client

        client.auth.signInWith(Email) {
            email = correo.trim()
            this.password = password
        }

        val userId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("No se pudo obtener el usuario actual.")

        val profile = client
            .from("profiles")
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingle<ProfileRow>()

        if (!profile.isActive) {
            throw Exception("Usuario desactivado. Contacta al administrador.")
        }

        return UsuarioApp(
            correo = correo.trim(),
            nombre = profile.name,
            role = profile.role,
            userId = userId
        )
    }

    suspend fun logout() {
        SupabaseClientProvider.client.auth.signOut()
    }
}
