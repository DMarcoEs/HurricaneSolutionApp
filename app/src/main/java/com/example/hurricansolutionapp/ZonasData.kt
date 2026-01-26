package com.example.hurricansolutionapp

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * ZONAS GEOGRÁFICAS Y CIUDADES - HURRICANE SOLUTION
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * Este archivo contiene:
 * - Enum de zonas geográficas (Continental, Islas, Foránea)
 * - Lista completa de ciudades de la Riviera Maya
 * - Lista de islas
 * - Lista de estados de México para zona foránea
 * - Funciones de autocompletado y detección de zona
 */

// ═══════════════════════════════════════════════════════════════════════════════
// ENUM DE ZONAS
// ═══════════════════════════════════════════════════════════════════════════════

enum class ZonaGeografica(
    val id: String,
    val nombreDisplay: String,
    val descripcion: String
) {
    CONTINENTAL("continental", "Zona Continental", "Riviera Maya y alrededores"),
    ISLAS("islas", "Zona Islas", "Cozumel, Isla Mujeres, Holbox"),
    FORANEA("foranea", "Zona Foránea", "Otros estados de México");

    companion object {
        fun fromId(id: String): ZonaGeografica {
            return entries.find { it.id == id } ?: CONTINENTAL
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DATOS DE CIUDADES POR ZONA
// ═══════════════════════════════════════════════════════════════════════════════

object ZonasData {

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONA CONTINENTAL - Riviera Maya y Quintana Roo continental
    // ═══════════════════════════════════════════════════════════════════════════

    val ciudadesContinental = listOf(
        // ═══════════════════════════════════════════════════════════════════
        // ZONA CONTINENTAL - Solo ciudades/municipios principales
        // Sin colonias ni fraccionamientos
        // ═══════════════════════════════════════════════════════════════════

        // Principales de la Riviera Maya (Norte a Sur)
        "Cancún, Quintana Roo",
        "Puerto Morelos, Quintana Roo",
        "Playa del Carmen, Quintana Roo",
        "Puerto Aventuras, Quintana Roo",
        "Akumal, Quintana Roo",
        "Xpu-Ha, Quintana Roo",
        "Chemuyil, Quintana Roo",
        "Tulum, Quintana Roo",

        // Pueblos y comunidades de la Riviera Maya
        "Leona Vicario, Quintana Roo",
        "Central Vallarta, Quintana Roo",
        "Nuevo Xcan, Quintana Roo",
        "Tankah, Quintana Roo",
        "Soliman Bay, Quintana Roo",

        // Sur de Quintana Roo
        "Felipe Carrillo Puerto, Quintana Roo",
        "Bacalar, Quintana Roo",
        "Chetumal, Quintana Roo",
        "Mahahual, Quintana Roo",
        "Xcalak, Quintana Roo",
        "Limones, Quintana Roo",
        "José María Morelos, Quintana Roo"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONA ISLAS
    // ═══════════════════════════════════════════════════════════════════════════

    val ciudadesIslas = listOf(
        // ═══════════════════════════════════════════════════════════════════
        // ZONA ISLAS - Solo las islas principales
        // ═══════════════════════════════════════════════════════════════════
        "Cozumel, Quintana Roo",
        "Isla Mujeres, Quintana Roo",
        "Holbox, Quintana Roo"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONA FORÁNEA - Otros estados de México
    // ═══════════════════════════════════════════════════════════════════════════

    val estadosMexico = listOf(
        "Aguascalientes",
        "Baja California",
        "Baja California Sur",
        "Campeche",
        "Chiapas",
        "Chihuahua",
        "Ciudad de México",
        "Coahuila",
        "Colima",
        "Durango",
        "Estado de México",
        "Guanajuato",
        "Guerrero",
        "Hidalgo",
        "Jalisco",
        "Michoacán",
        "Morelos",
        "Nayarit",
        "Nuevo León",
        "Oaxaca",
        "Puebla",
        "Querétaro",
        "San Luis Potosí",
        "Sinaloa",
        "Sonora",
        "Tabasco",
        "Tamaulipas",
        "Tlaxcala",
        "Veracruz",
        "Yucatán",
        "Zacatecas"
    )

    // Ciudades principales de otros estados (para autocompletado)
    val ciudadesForaneas = listOf(
        // ═══════════════════════════════════════════════════════════════════
        // ZONA FORÁNEA - Solo ciudades principales sin colonias
        // ═══════════════════════════════════════════════════════════════════

        // Yucatán (cercano pero foráneo)
        "Mérida, Yucatán",
        "Valladolid, Yucatán",
        "Progreso, Yucatán",
        "Izamal, Yucatán",
        "Tizimín, Yucatán",

        // Campeche
        "Campeche, Campeche",
        "Ciudad del Carmen, Campeche",
        "Champotón, Campeche",

        // Ciudad de México
        "Ciudad de México, CDMX",

        // Estado de México
        "Toluca, Estado de México",
        "Naucalpan, Estado de México",
        "Tlalnepantla, Estado de México",
        "Ecatepec, Estado de México",
        "Huixquilucan, Estado de México",
        "Metepec, Estado de México",

        // Nuevo León
        "Monterrey, Nuevo León",
        "San Pedro Garza García, Nuevo León",
        "Guadalupe, Nuevo León",
        "Apodaca, Nuevo León",
        "San Nicolás de los Garza, Nuevo León",

        // Jalisco
        "Guadalajara, Jalisco",
        "Zapopan, Jalisco",
        "Puerto Vallarta, Jalisco",
        "Tlaquepaque, Jalisco",

        // Querétaro
        "Querétaro, Querétaro",
        "San Juan del Río, Querétaro",

        // Guanajuato
        "León, Guanajuato",
        "Guanajuato, Guanajuato",
        "Irapuato, Guanajuato",
        "Celaya, Guanajuato",
        "San Miguel de Allende, Guanajuato",

        // Puebla
        "Puebla, Puebla",
        "Cholula, Puebla",

        // Veracruz
        "Veracruz, Veracruz",
        "Xalapa, Veracruz",
        "Boca del Río, Veracruz",
        "Coatzacoalcos, Veracruz",

        // Guerrero
        "Acapulco, Guerrero",
        "Ixtapa-Zihuatanejo, Guerrero",

        // Oaxaca
        "Oaxaca, Oaxaca",
        "Puerto Escondido, Oaxaca",
        "Huatulco, Oaxaca",

        // Chiapas
        "Tuxtla Gutiérrez, Chiapas",
        "San Cristóbal de las Casas, Chiapas",
        "Palenque, Chiapas",

        // Tabasco
        "Villahermosa, Tabasco",

        // Baja California
        "Tijuana, Baja California",
        "Mexicali, Baja California",
        "Ensenada, Baja California",

        // Baja California Sur
        "La Paz, Baja California Sur",
        "Los Cabos, Baja California Sur",

        // Sonora
        "Hermosillo, Sonora",

        // Chihuahua
        "Chihuahua, Chihuahua",
        "Ciudad Juárez, Chihuahua",

        // Coahuila
        "Torreón, Coahuila",
        "Saltillo, Coahuila",

        // Sinaloa
        "Culiacán, Sinaloa",
        "Mazatlán, Sinaloa",

        // Tamaulipas
        "Tampico, Tamaulipas",
        "Reynosa, Tamaulipas",

        // Otros estados principales
        "Aguascalientes, Aguascalientes",
        "San Luis Potosí, San Luis Potosí",
        "Zacatecas, Zacatecas",
        "Morelia, Michoacán",
        "Cuernavaca, Morelos",
        "Pachuca, Hidalgo",
        "Durango, Durango",
        "Tepic, Nayarit",
        "Colima, Colima"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // TODAS LAS CIUDADES COMBINADAS
    // ═══════════════════════════════════════════════════════════════════════════

    val todasLasCiudades: List<String> by lazy {
        (ciudadesContinental + ciudadesIslas + ciudadesForaneas + estadosMexico).distinct().sorted()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FUNCIONES DE DETECCIÓN DE ZONA
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Detecta la zona geográfica basándose en el nombre de la ciudad
     */
    fun detectarZona(ciudad: String): ZonaGeografica {
        val ciudadLower = ciudad.lowercase().trim()

        // ═══════════════════════════════════════════════════════════════════
        // ISLAS - Verificar primero (tienen prioridad)
        // ═══════════════════════════════════════════════════════════════════
        val palabrasIslas = listOf("cozumel", "isla mujeres", "holbox")
        if (palabrasIslas.any { ciudadLower.contains(it) }) {
            return ZonaGeografica.ISLAS
        }

        // ═══════════════════════════════════════════════════════════════════
        // CONTINENTAL - Riviera Maya y Quintana Roo
        // ═══════════════════════════════════════════════════════════════════
        val palabrasContinental = listOf(
            // Ciudades principales
            "cancún", "cancun",
            "playa del carmen",
            "tulum",
            "puerto morelos",
            "akumal",
            "puerto aventuras",
            "chemuyil",
            "xpu-ha", "xpuha",
            // Sur de Quintana Roo
            "bacalar",
            "chetumal",
            "felipe carrillo puerto",
            "mahahual",
            "xcalak",
            // Otros
            "leona vicario",
            "nuevo xcan",
            "tankah",
            "soliman",
            // Genérico
            "quintana roo",
            "riviera maya"
        )
        if (palabrasContinental.any { ciudadLower.contains(it) }) {
            return ZonaGeografica.CONTINENTAL
        }

        // ═══════════════════════════════════════════════════════════════════
        // FORÁNEA - Todo lo demás
        // ═══════════════════════════════════════════════════════════════════
        return ZonaGeografica.FORANEA
    }

    /**
     * Obtiene sugerencias de autocompletado basadas en el texto ingresado
     */
    fun getSugerencias(query: String, limit: Int = 10): List<String> {
        if (query.isBlank() || query.length < 2) return emptyList()

        val queryLower = query.lowercase().trim()

        // Priorizar ciudades que empiezan con el query
        val empiezanCon = todasLasCiudades.filter {
            it.lowercase().startsWith(queryLower)
        }

        // Luego las que contienen el query
        val contienen = todasLasCiudades.filter {
            it.lowercase().contains(queryLower) && !it.lowercase().startsWith(queryLower)
        }

        return (empiezanCon + contienen).take(limit)
    }

    /**
     * Obtiene sugerencias priorizando la zona continental (más común)
     */
    fun getSugerenciasPriorizadas(query: String, limit: Int = 10): List<Pair<String, ZonaGeografica>> {
        if (query.isBlank() || query.length < 2) return emptyList()

        val queryLower = query.lowercase().trim()

        // Buscar en continental primero
        val continental = ciudadesContinental.filter {
            it.lowercase().contains(queryLower)
        }.map { it to ZonaGeografica.CONTINENTAL }

        // Luego islas
        val islas = ciudadesIslas.filter {
            it.lowercase().contains(queryLower)
        }.map { it to ZonaGeografica.ISLAS }

        // Finalmente foráneas
        val foraneas = ciudadesForaneas.filter {
            it.lowercase().contains(queryLower)
        }.map { it to ZonaGeografica.FORANEA }

        return (continental + islas + foraneas).take(limit)
    }

    /**
     * Verifica si una ciudad es válida (está en nuestra lista)
     */
    fun esCiudadValida(ciudad: String): Boolean {
        val ciudadLower = ciudad.lowercase().trim()
        return todasLasCiudades.any { it.lowercase() == ciudadLower }
    }

    /**
     * Obtiene el emoji correspondiente a cada zona
     */
    fun getZonaEmoji(zona: ZonaGeografica): String = when (zona) {
        ZonaGeografica.CONTINENTAL -> "🏖️"
        ZonaGeografica.ISLAS -> "🏝️"
        ZonaGeografica.FORANEA -> "🌎"
    }

    /**
     * Obtiene el color correspondiente a cada zona (para UI)
     */
    fun getZonaColorHex(zona: ZonaGeografica): Long = when (zona) {
        ZonaGeografica.CONTINENTAL -> 0xFF2AA63E // Verde
        ZonaGeografica.ISLAS -> 0xFF3B82F6 // Azul
        ZonaGeografica.FORANEA -> 0xFFF59E0B // Naranja/Amber
    }
}