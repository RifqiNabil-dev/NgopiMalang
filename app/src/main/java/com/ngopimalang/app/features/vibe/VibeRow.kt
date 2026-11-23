package com.ngopimalang.app.features.vibe

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VibeRow(
    val id: String = "",
    val name: String,
    val description: String = "",
    @SerialName("icon_key") val iconKey: String = "",
    @SerialName("cafe_count") val cafeCount: Int = 0
)

data class VibeDraft(
    val name: String,
    val description: String,
    val iconKey: String,
    val cafeIds: List<String>
)

/** Baris cafes_with_rating / hasil embed cafes. */
@Serializable
data class CafeRow(
    val id: String,
    val name: String,
    val address: String? = null,
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("price_min") val priceMin: Int? = null,
    @SerialName("price_max") val priceMax: Int? = null,
    val rating: Double? = null,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class CafeVibeEmbed(val cafes: CafeRow)

@Serializable
data class CafeVibeRow(@SerialName("cafe_id") val cafeId: String)

@Serializable
data class CafeVibeWrite(
    @SerialName("cafe_id") val cafeId: String,
    @SerialName("vibe_id") val vibeId: String
)