package com.example.hurricansolutionapp

data class SpecialistUser(
    val id: Int,
    val correo: String,
    val password: String
)

object AuthRepository {

    // 👉 Aquí defines a tus especialistas reales
    private val specialists = listOf(
        SpecialistUser(
            id = 1,
            correo = "Marco@Hurricanesolution.com",
            password = "MarcoHS7111"
        ),
        SpecialistUser(
            id = 2,
            correo = "Derek@Hurricanesolution.com",
            password = "Derek.130804"
        )
    )

    fun login(nombre: String, password: String): SpecialistUser? {
        return specialists.firstOrNull { user ->
            user.correo.equals(nombre.trim(), ignoreCase = true) &&
                    user.password == password
        }
    }
}
