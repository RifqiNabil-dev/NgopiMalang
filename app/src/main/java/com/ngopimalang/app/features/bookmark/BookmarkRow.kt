package com.ngopimalang.app.features.bookmark

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookmarkRow(
    val id: String,
    val note: String? = null,

    @SerialName("cafe_id")
    val cafeId: String,

    @SerialName("created_at")
    val createdAt: String,

    val cafes: BookmarkCafeRow? = null
)

@Serializable
data class BookmarkCafeRow(
    val id: String,
    val name: String,
    val address: String? = null,

    @SerialName("photo_path")
    val photoPath: String? = null,

    @SerialName("price_min")
    val priceMin: Int? = null,

    @SerialName("price_max")
    val priceMax: Int? = null,

    val rating: Double? = null,

    @SerialName("review_count")
    val reviewCount: Int = 0,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("has_wifi")
    val hasWifi: Boolean = false,

    @SerialName("plug_count")
    val plugCount: Int = 0
)

@Serializable
data class BookmarkVibeLink(
    @SerialName("cafe_id")
    val cafeId: String,

    @SerialName("vibe_id")
    val vibeId: String
)

@Serializable
data class BookmarkVibeRow(
    val id: String,
    val name: String
)
