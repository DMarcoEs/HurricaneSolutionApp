package com.example.hurricansolutionapp

import android.content.Context


object EnviosInstalacionRepository {

    private const val TAG = "EnviosInstalacionRepo"
    private const val PREFS_NAME = "envios_instalacion_prefs"
    private const val KEY_ENVIADOS = "folios_enviados"

    /**
     * Obtiene TODAS las cotizaciones que NO se han enviado a instalacion
     * Filtra por nombre del especialista si se proporciona
     */
    fun getCotizacionesPendientesEnvioTodas(
        context: Context,
        userId: String? = null,
        especialistaNombre: String? = null
    ): Result<List<Cotizacion>> {
        return try {
            // Obtener todas las cotizaciones locales
            val todasCotizaciones = obtenerCotizacionesLocal(context)

            // Obtener folios ya enviados
            val foliosEnviados = getFoliosEnviados(context)

            // Filtrar por productos no vacios y no enviadas
            var pendientes = todasCotizaciones.filter { cot ->
                cot.productos.isNotEmpty() && !foliosEnviados.contains(cot.folio)
            }

            // Si se proporciona nombre de especialista, filtrar por el
            // Cada usuario solo ve sus propias cotizaciones
            if (!especialistaNombre.isNullOrBlank()) {
                pendientes = pendientes.filter { cot ->
                    cot.especialista.equals(especialistaNombre, ignoreCase = true)
                }
            }

            android.util.Log.d(TAG, "[OK] ${pendientes.size} cotizaciones pendientes de envio (filtradas por: $especialistaNombre)")
            Result.success(pendientes)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getCotizacionesPendientesEnvioTodas: ${e.message}", e)
            Result.failure(e)
        }
    }


    fun getCotizacionesPendientesEnvio(
        context: Context,
        userId: String? = null
    ): Result<List<Cotizacion>> {
        return try {
            val todasCotizaciones = obtenerCotizacionesLocal(context)
            val foliosEnviados = getFoliosEnviados(context)

            val pendientes = todasCotizaciones.filter { cot ->
                cot.productos.size == 1 && !foliosEnviados.contains(cot.folio)
            }

            android.util.Log.d(TAG, "[OK] ${pendientes.size} cotizaciones pendientes de envío (1 sistema)")
            Result.success(pendientes)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error getCotizacionesPendientesEnvio: ${e.message}", e)
            Result.failure(e)
        }
    }


    fun marcarComoEnviada(context: Context, folio: String): Result<Unit> {
        return try {
            val foliosEnviados = getFoliosEnviados(context).toMutableSet()
            foliosEnviados.add(folio)
            saveFoliosEnviados(context, foliosEnviados)

            android.util.Log.d(TAG, "[OK] Cotización $folio marcada como enviada")
            Result.success(Unit)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error marcarComoEnviada: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Verifica si una cotizaciÃ³n ya fue enviada
     */
    fun fueEnviada(context: Context, folio: String): Boolean {
        return getFoliosEnviados(context).contains(folio)
    }

    /**
     * Obtiene la lista de folios enviados
     */
    private fun getFoliosEnviados(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_ENVIADOS, emptySet()) ?: emptySet()
    }

    /**
     * Guarda la lista de folios enviados
     */
    private fun saveFoliosEnviados(context: Context, folios: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_ENVIADOS, folios).apply()
    }

    /**
     * Limpia un folio de la lista de enviados (para reenviar)
     */
    fun desmarcarEnviado(context: Context, folio: String): Result<Unit> {
        return try {
            val foliosEnviados = getFoliosEnviados(context).toMutableSet()
            foliosEnviados.remove(folio)
            saveFoliosEnviados(context, foliosEnviados)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza un registro existente en instalador_datos
     * Se usa cuando la cotizaciÃ³n ya fue enviada y se modificÃ³
     */
    suspend fun actualizarRegistroInstalacion(
        cotizacion: Cotizacion,
        sistemaSeleccionado: String,
        especialistaId: String,
        especialistaNombre: String,
        fechaSolicitada: String? = null
    ): Result<Any> {
        return try {
            android.util.Log.d(TAG, "Actualizando registro para: ${cotizacion.folio}")

            // 1. Obtener el registro existente
            val existenteResult = InstaladorRepository.getDatosByFolio(cotizacion.folio)
            if (existenteResult.isFailure || existenteResult.getOrNull() == null) {
                // Si no existe, crear uno nuevo
                return InstaladorRepository.crearRegistroDesdeCotizacionCompleto(
                    cotizacion = cotizacion,
                    sistemaSeleccionado = sistemaSeleccionado,
                    especialistaId = especialistaId,
                    especialistaNombre = especialistaNombre,
                    fechaSolicitada = fechaSolicitada
                )
            }

            val existente = existenteResult.getOrNull()!!

            // 2. Actualizar los datos principales
            val update = InstaladorDatosUpdate(
                tipoPropiedad = existente.tipoPropiedad,  // Mantener el tipo de propiedad existente
                fechaSolicitada = fechaSolicitada ?: existente.fechaSolicitada,
                observaciones = "Actualizado desde cotización modificada"
            )

            val updateResult = InstaladorRepository.updateDatos(existente.id, update)
            if (updateResult.isFailure) {
                return Result.failure(updateResult.exceptionOrNull() ?: Exception("Error actualizando datos"))
            }

            // 3. Actualizar las medidas (eliminar existentes y crear nuevas)
            val nuevasMedidas = cotizacion.ventanas.mapIndexed { index, ventana ->
                MedidaInstaladorInsert(
                    instaladorDatosId = existente.id,
                    zona = ventana.zona.ifBlank { null },
                    descripcion = ventana.descripcion,
                    cantidad = 1,
                    alto = ventana.alto,
                    ancho = ventana.ancho,
                    tipoMontaje = ventana.tipoMontaje,
                    requiereAdecuacion = ventana.adecuacion.isNotBlank() && ventana.adecuacion != "No",
                    adecuacionDetalle = if (ventana.adecuacion != "No") ventana.adecuacion else null,
                    orden = index
                )
            }

            val medidasResult = InstaladorRepository.replaceMedidas(existente.id, nuevasMedidas)
            if (medidasResult.isFailure) {
                android.util.Log.e(TAG, "Error actualizando medidas: ${medidasResult.exceptionOrNull()?.message}")
            }

            android.util.Log.d(TAG, "[OK] Registro actualizado: ${cotizacion.folio}")
            Result.success(existente)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error actualizarRegistroInstalacion: ${e.message}", e)
            Result.failure(e)
        }
    }
}