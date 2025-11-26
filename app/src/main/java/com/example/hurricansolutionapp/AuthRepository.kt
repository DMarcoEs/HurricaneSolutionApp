package com.example.hurricansolutionapp

data class SpecialistUser(
    val id: Int,
    val nombre: String,
    val password: String
)

object AuthRepository {

    // 👉 Aquí defines a tus especialistas reales
    private val specialists = listOf(
        SpecialistUser(
            id = 1,
            nombre = "Marco Alejandro Canche Kantun",
            password = "MarcoHS7111"
        ),
        SpecialistUser(
            id = 2,
            nombre = "Derek Idrahim Hernandez Rios",
            password = "Derek.130804"
        )
    )

    fun login(nombre: String, password: String): SpecialistUser? {
        return specialists.firstOrNull { user ->
            user.nombre.equals(nombre.trim(), ignoreCase = true) &&
                    user.password == password
        }
    }
}
