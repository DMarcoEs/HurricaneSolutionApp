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
        // Principales de Riviera Maya
        "Cancún, Quintana Roo",
        "Playa del Carmen, Quintana Roo",
        "Tulum, Quintana Roo",
        "Puerto Morelos, Quintana Roo",
        "Puerto Aventuras, Quintana Roo",
        "Akumal, Quintana Roo",
        "Xpu-Ha, Quintana Roo",
        "Chemuyil, Quintana Roo",
        
        // Zonas y colonias de Cancún
        "Zona Hotelera, Cancún",
        "Puerto Cancún, Cancún",
        "Puerto Juárez, Cancún",
        "Alfredo V. Bonfil, Cancún",
        "Supermanzana (SM), Cancún",
        "El Table, Cancún",
        "Leona Vicario, Quintana Roo",
        
        // Zonas de Playa del Carmen
        "Playacar, Playa del Carmen",
        "Mayakoba, Playa del Carmen",
        "Colosio, Playa del Carmen",
        "Ejido, Playa del Carmen",
        "Luis Donaldo Colosio, Playa del Carmen",
        "Gonzalo Guerrero, Playa del Carmen",
        
        // Comunidades de la Riviera Maya
        "Xcaret, Quintana Roo",
        "Xel-Há, Quintana Roo",
        "Tankah, Quintana Roo",
        "Soliman Bay, Quintana Roo",
        "Bahía Príncipe, Quintana Roo",
        "Grand Sirenis, Quintana Roo",
        "Barceló Maya, Quintana Roo",
        
        // Sur de Quintana Roo (menos frecuente pero incluido)
        "Bacalar, Quintana Roo",
        "Felipe Carrillo Puerto, Quintana Roo",
        "Chetumal, Quintana Roo",
        "Mahahual, Quintana Roo",
        "Xcalak, Quintana Roo",
        "Laguna Bacalar, Quintana Roo",
        "Limones, Quintana Roo",
        "José María Morelos, Quintana Roo",
        
        // Norte de Quintana Roo
        "Kantunil, Quintana Roo",
        "Nuevo Xcan, Quintana Roo",
        "Central Vallarta, Quintana Roo",
        "Tres Reyes, Quintana Roo"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // ZONA ISLAS
    // ═══════════════════════════════════════════════════════════════════════════
    
    val ciudadesIslas = listOf(
        "Cozumel, Quintana Roo",
        "San Miguel de Cozumel, Quintana Roo",
        "Isla Mujeres, Quintana Roo",
        "Holbox, Quintana Roo",
        "Isla Holbox, Quintana Roo",
        "Isla Contoy, Quintana Roo",
        "Isla Blanca, Quintana Roo"
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
        // Yucatán (cercano pero foráneo)
        "Mérida, Yucatán",
        "Valladolid, Yucatán",
        "Progreso, Yucatán",
        "Izamal, Yucatán",
        "Tizimín, Yucatán",
        "Motul, Yucatán",
        "Ticul, Yucatán",
        
        // Campeche
        "Campeche, Campeche",
        "Ciudad del Carmen, Campeche",
        "Champotón, Campeche",
        "Escárcega, Campeche",
        "Calakmul, Campeche",
        
        // Ciudad de México y área metropolitana
        "Ciudad de México, CDMX",
        "Polanco, CDMX",
        "Santa Fe, CDMX",
        "Coyoacán, CDMX",
        "Condesa, CDMX",
        "Roma Norte, CDMX",
        "Roma Sur, CDMX",
        "Del Valle, CDMX",
        "Nápoles, CDMX",
        "Pedregal, CDMX",
        "Interlomas, Estado de México",
        "Huixquilucan, Estado de México",
        "Naucalpan, Estado de México",
        "Tlalnepantla, Estado de México",
        "Ecatepec, Estado de México",
        "Toluca, Estado de México",
        "Metepec, Estado de México",
        
        // Nuevo León
        "Monterrey, Nuevo León",
        "San Pedro Garza García, Nuevo León",
        "Santa Catarina, Nuevo León",
        "Guadalupe, Nuevo León",
        "Apodaca, Nuevo León",
        "Escobedo, Nuevo León",
        "San Nicolás de los Garza, Nuevo León",
        
        // Jalisco
        "Guadalajara, Jalisco",
        "Zapopan, Jalisco",
        "Tlaquepaque, Jalisco",
        "Tonalá, Jalisco",
        "Puerto Vallarta, Jalisco",
        "Tlajomulco, Jalisco",
        
        // Querétaro
        "Querétaro, Querétaro",
        "San Juan del Río, Querétaro",
        "Juriquilla, Querétaro",
        "El Marqués, Querétaro",
        
        // Guanajuato
        "León, Guanajuato",
        "Guanajuato, Guanajuato",
        "Irapuato, Guanajuato",
        "Celaya, Guanajuato",
        "San Miguel de Allende, Guanajuato",
        "Salamanca, Guanajuato",
        
        // Puebla
        "Puebla, Puebla",
        "Cholula, Puebla",
        "Atlixco, Puebla",
        "Tehuacán, Puebla",
        
        // Veracruz
        "Veracruz, Veracruz",
        "Xalapa, Veracruz",
        "Boca del Río, Veracruz",
        "Coatzacoalcos, Veracruz",
        "Córdoba, Veracruz",
        "Orizaba, Veracruz",
        "Poza Rica, Veracruz",
        
        // Guerrero
        "Acapulco, Guerrero",
        "Ixtapa, Guerrero",
        "Zihuatanejo, Guerrero",
        "Taxco, Guerrero",
        
        // Oaxaca
        "Oaxaca, Oaxaca",
        "Puerto Escondido, Oaxaca",
        "Huatulco, Oaxaca",
        "Salina Cruz, Oaxaca",
        
        // Chiapas
        "Tuxtla Gutiérrez, Chiapas",
        "San Cristóbal de las Casas, Chiapas",
        "Tapachula, Chiapas",
        "Palenque, Chiapas",
        "Comitán, Chiapas",
        
        // Tabasco
        "Villahermosa, Tabasco",
        "Cárdenas, Tabasco",
        "Comalcalco, Tabasco",
        
        // Norte
        "Tijuana, Baja California",
        "Mexicali, Baja California",
        "Ensenada, Baja California",
        "La Paz, Baja California Sur",
        "Los Cabos, Baja California Sur",
        "San José del Cabo, Baja California Sur",
        "Cabo San Lucas, Baja California Sur",
        "Hermosillo, Sonora",
        "Ciudad Obregón, Sonora",
        "Nogales, Sonora",
        "Chihuahua, Chihuahua",
        "Ciudad Juárez, Chihuahua",
        "Torreón, Coahuila",
        "Saltillo, Coahuila",
        "Durango, Durango",
        "Culiacán, Sinaloa",
        "Mazatlán, Sinaloa",
        "Los Mochis, Sinaloa",
        "Tampico, Tamaulipas",
        "Reynosa, Tamaulipas",
        "Matamoros, Tamaulipas",
        "Nuevo Laredo, Tamaulipas",
        "Ciudad Victoria, Tamaulipas",
        
        // Centro-Occidente
        "Aguascalientes, Aguascalientes",
        "San Luis Potosí, San Luis Potosí",
        "Zacatecas, Zacatecas",
        "Morelia, Michoacán",
        "Uruapan, Michoacán",
        "Pátzcuaro, Michoacán",
        "Colima, Colima",
        "Manzanillo, Colima",
        "Tepic, Nayarit",
        "Nuevo Vallarta, Nayarit",
        "Sayulita, Nayarit",
        "Cuernavaca, Morelos",
        "Jiutepec, Morelos",
        "Cuautla, Morelos",
        "Pachuca, Hidalgo",
        "Tlaxcala, Tlaxcala"
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
        
        // Primero verificar si es una isla
        val esIsla = ciudadesIslas.any { it.lowercase().contains(ciudadLower) || ciudadLower.contains(it.lowercase().substringBefore(",")) }
        if (esIsla) return ZonaGeografica.ISLAS
        
        // Verificar palabras clave de islas
        val palabrasIslas = listOf("cozumel", "isla mujeres", "holbox", "contoy", "isla blanca")
        if (palabrasIslas.any { ciudadLower.contains(it) }) {
            return ZonaGeografica.ISLAS
        }
        
        // Verificar si es zona continental (Quintana Roo continental)
        val esContinental = ciudadesContinental.any { 
            it.lowercase().contains(ciudadLower) || ciudadLower.contains(it.lowercase().substringBefore(","))
        }
        if (esContinental) return ZonaGeografica.CONTINENTAL
        
        // Verificar palabras clave de Quintana Roo continental
        val palabrasContinental = listOf(
            "cancún", "cancun", "playa del carmen", "tulum", "puerto morelos",
            "akumal", "puerto aventuras", "chemuyil", "xpu-ha", "xpuha",
            "bacalar", "chetumal", "felipe carrillo", "mahahual",
            "zona hotelera", "playacar", "mayakoba", "riviera maya",
            "quintana roo"
        )
        if (palabrasContinental.any { ciudadLower.contains(it) }) {
            return ZonaGeografica.CONTINENTAL
        }
        
        // Todo lo demás es foráneo
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
