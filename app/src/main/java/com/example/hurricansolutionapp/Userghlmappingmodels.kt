package com.example.hurricansolutionapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo para mapeo de nombres de GoHighLevel a usuarios de Supabase
 */
@Serializable
data class UserGHLMapping(
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("ghl_name")
    val ghlName: String,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Modelo para insertar un nuevo mapeo
 */
@Serializable
data class UserGHLMappingInsert(
    @SerialName("user_id")
    val userId: String,

    @SerialName("ghl_name")
    val ghlName: String
)