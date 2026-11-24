package com.ngopimalang.app.features.directory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CafeRow(
    val id: String, val name: String,
    val address: String? = null, val description: String? = null,
    @SerialName("open_hours") val openHours: String? = null,
    @SerialName("price_min") val priceMin: Int? = null,
    @SerialName("price_max") val priceMax: Int? = null,
    @SerialName("has_wifi") val hasWifi: Boolean = false,
    @SerialName("plug_count") val plugCount: Int = 0,
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    val rating: Double? = null,
    @SerialName("review_count") val reviewCount: Int = 0
)

// Separate write DTO: generated IDs, ownership, timestamps and aggregates are never sent.
@Serializable
data class CafeWrite(
    val name: String, val address: String, val description: String,
    @SerialName("open_hours") val openHours: String,
    @SerialName("price_min") val priceMin: Int?,
    @SerialName("price_max") val priceMax: Int?,
    @SerialName("has_wifi") val hasWifi: Boolean,
    @SerialName("plug_count") val plugCount: Int
)
@Serializable data class VibeOption(val id: String, val name: String)
@Serializable data class CafeVibeLink(
    @SerialName("cafe_id") val cafeId: String,
    @SerialName("vibe_id") val vibeId: String
)
@Serializable data class PhotoWrite(@SerialName("photo_path") val path: String)
@Serializable data class ActiveWrite(@SerialName("is_active") val active: Boolean)
data class CafeDraft(
    val name: String, val address: String = "", val description: String = "",
    val openHours: String = "", val priceMin: Int? = null, val priceMax: Int? = null,
    val hasWifi: Boolean = false, val plugCount: Int = 0, val vibeIds: Set<String> = emptySet()
)
data class CafePhoto(val bytes: ByteArray, val mime: String, val extension: String)
