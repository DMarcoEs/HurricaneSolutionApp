package com.example.hurricansolutionapp

import kotlinx.serialization.json.*

/**
 * Funciones de extension para convertir entre modelos de cotizacion
 *
 * Estas funciones permiten:
 * - Convertir CotizacionRemota (de Supabase) a Cotizacion (modelo local)
 * - Permitir que los especialistas vean sus cotizaciones desde cualquier dispositivo
 */

/**
 * Convierte una CotizacionRemota (de Supabase) a Cotizacion (modelo local)
 *
 * Esta conversion es necesaria para:
 * - Mostrar cotizaciones de Supabase en HistorialScreen
 * - Mantener compatibilidad con el codigo existente que usa Cotizacion
 * - Permitir generar PDFs de cotizaciones sincronizadas
 */
fun CotizacionRemota.toCotizacionLocal(): Cotizacion {
    // Convertir productos de List<String> a List<TipoProducto>
    val productosLocal = this.productos.mapNotNull { productoStr ->
        when (productoStr) {
            "HS875" -> TipoProducto.HS875
            "HS1250" -> TipoProducto.HS1250
            "HS1500" -> TipoProducto.HS1500
            "PERSONALIZADO" -> TipoProducto.PERSONALIZADO
            else -> null
        }
    }

    // Tomar el primer producto como principal (para compatibilidad con el modelo viejo)
    val productoPrincipal = productosLocal.firstOrNull() ?: TipoProducto.HS875

    // Convertir ventanas de JsonElement a List<Ventana>
    val ventanasLocal = parseVentanasFromJson(this.ventanas)

    // Crear la cotizacion local
    return Cotizacion(
        id = this.id ?: 0L,
        folio = this.folio,
        clienteNombre = this.clienteNombre,
        clienteTelefono = this.clienteTelefono ?: "",
        ubicacion = this.ubicacion ?: "",
        ciudad = this.ciudad ?: "",
        especialista = this.especialistaNombre,
        fecha = this.fecha,
        producto = productoPrincipal,
        productos = productosLocal,
        descuentoHS875 = this.descuentoHs875 ?: 0.0,
        descuentoHS1250 = this.descuentoHs1250 ?: 0.0,
        descuentoHS1500 = this.descuentoHs1500 ?: 0.0,
        descuentoDolaresPorM2 = 0.0, // Campo legacy
        tipoMontaje = this.tipoMontaje ?: "Flush Mount",
        ventanas = ventanasLocal,
        updatedAt = parseTimestamp(this.updatedAt)
    )
}

/**
 * Parsea ventanas desde JsonElement a List<Ventana>
 */
private fun parseVentanasFromJson(ventanasJson: JsonElement?): List<Ventana> {
    if (ventanasJson == null || ventanasJson is JsonNull) {
        return emptyList()
    }

    return try {
        when (ventanasJson) {
            is JsonArray -> {
                ventanasJson.mapNotNull { element ->
                    parseVentanaFromJsonObject(element)
                }
            }
            is JsonObject -> {
                // Si es un objeto unico, convertirlo a lista de uno
                val ventana = parseVentanaFromJsonObject(ventanasJson)
                if (ventana != null) listOf(ventana) else emptyList()
            }
            else -> emptyList()
        }
    } catch (e: Exception) {
        android.util.Log.e("CotizacionConversion", "Error parseando ventanas: ${e.message}", e)
        emptyList()
    }
}

/**
 * Parsea una ventana individual desde JsonObject
 */
private fun parseVentanaFromJsonObject(jsonElement: JsonElement): Ventana? {
    if (jsonElement !is JsonObject) return null

    return try {
        Ventana(
            zona = jsonElement["zona"]?.jsonPrimitive?.contentOrNull ?: "",
            descripcion = jsonElement["descripcion"]?.jsonPrimitive?.contentOrNull ?: "",
            alto = jsonElement["alto"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            ancho = jsonElement["ancho"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            precioM2 = jsonElement["precio_m2"]?.jsonPrimitive?.doubleOrNull
                ?: jsonElement["precioM2"]?.jsonPrimitive?.doubleOrNull
                ?: 0.0,
            adecuacion = jsonElement["adecuacion"]?.jsonPrimitive?.contentOrNull ?: "",
            tipoMontaje = jsonElement["tipo_montaje"]?.jsonPrimitive?.contentOrNull
                ?: jsonElement["tipoMontaje"]?.jsonPrimitive?.contentOrNull
                ?: "Flush Mount"
        )
    } catch (e: Exception) {
        android.util.Log.e("CotizacionConversion", "Error parseando ventana individual: ${e.message}", e)
        null
    }
}

/**
 * Parsea timestamp ISO 8601 a timestamp Long para updatedAt
 * Si falla, retorna 0 (indica que no ha sido actualizada)
 */
private fun parseTimestamp(isoTimestamp: String?): Long {
    if (isoTimestamp == null) return 0L

    return try {
        // Formato ISO 8601: "2024-01-26T18:30:00Z"
        val instant = java.time.Instant.parse(isoTimestamp)
        instant.toEpochMilli()
    } catch (e: Exception) {
        android.util.Log.w("CotizacionConversion", "No se pudo parsear timestamp: $isoTimestamp")
        0L
    }
}

/**
 * Convierte una Cotizacion local a CotizacionInsert (para guardar en Supabase)
 *
 * NOTA: Esta funcion ya existe en AutoUploadManager, pero se incluye aqui
 * como referencia para futuras conversiones bidireccionales
 */
fun Cotizacion.toRemoteInsert(userId: String, zonaGeografica: String = "continental"): CotizacionInsert {
    // Convertir ventanas al modelo de inserción
    val ventanasInsert = this.ventanas.map { v ->
        VentanaInsert(
            zona = v.zona,
            descripcion = v.descripcion,
            alto = v.alto,
            ancho = v.ancho,
            precioM2 = v.precioM2,
            adecuacion = v.adecuacion,
            tipoMontaje = v.tipoMontaje
        )
    }

    // Calcular totales
    val totales = mutableMapOf<String, Double>()
    var totalHs875 = 0.0
    var totalHs1250 = 0.0
    var totalHs1500 = 0.0

    this.productos.forEach { producto ->
        val total = this.totalConDescuento(producto)
        totales[producto.name] = total

        when (producto) {
            TipoProducto.HS875 -> totalHs875 = total
            TipoProducto.HS1250 -> totalHs1250 = total
            TipoProducto.HS1500 -> totalHs1500 = total
            else -> { /* PERSONALIZADO u otros */ }
        }
    }

    return CotizacionInsert(
        folio = this.folio,
        userId = userId,
        especialistaNombre = this.especialista,
        clienteNombre = this.clienteNombre,
        clienteTelefono = this.clienteTelefono.ifBlank { null },
        ciudad = this.ciudad.ifBlank { null },
        ubicacion = this.ubicacion.ifBlank { null },
        fecha = this.fecha,
        productos = this.productos.map { it.name },
        tipoMontaje = this.tipoMontaje,
        areaTotal = this.areaTotal,
        descuentoHs875 = this.descuentoHS875,
        descuentoHs1250 = this.descuentoHS1250,
        descuentoHs1500 = this.descuentoHS1500,
        totalHs875 = totalHs875,
        totalHs1250 = totalHs1250,
        totalHs1500 = totalHs1500,
        totales = totales,
        ventanas = ventanasInsert,
        pdfPath = null,
        zonaGeografica = zonaGeografica
    )
}