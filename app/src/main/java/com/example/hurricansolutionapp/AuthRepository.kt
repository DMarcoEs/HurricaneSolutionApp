package com.example.hurricansolutionapp

// Usuario que se loguea en la app
data class UsuarioApp(
    val correo: String,     // se usa para iniciar sesión
    val nombre: String,     // nombre que se mostrará en "Bienvenido, ..."
    val password: String
)

object AuthRepository {

    // 🔒 Aquí defines a tus especialistas
    // Cambia correos / nombres / contraseñas como tú quieras
    private val usuarios = listOf(
        UsuarioApp(
            correo = "Fernando",
            nombre = "Fernando Loria Fernandez",
            password = "1234"
        ),
        UsuarioApp(
            correo = "Derek@hurricanesolution.com",
            nombre = "Derek Idrahim Hernandez Rios",
            password = "Derek.13.0804"
        ),
        UsuarioApp(
            correo = "Marco@hurricanesolution.com",
            nombre = "Marco Alejandro Canche Kantun",
            password = "MarcoHS7111"
        )
    )

    fun login(correo: String, password: String): UsuarioApp? {
        val correoNormalizado = correo.trim().lowercase()
        return usuarios.firstOrNull { user ->
            user.correo.lowercase() == correoNormalizado &&
                    user.password == password
        }
    }
}
