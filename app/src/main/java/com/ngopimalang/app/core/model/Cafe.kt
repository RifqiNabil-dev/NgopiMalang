package com.ngopimalang.app.core.model

import androidx.annotation.DrawableRes

/**
 * Model cafe.
 */
data class Cafe(
    val id: String,
    val name: String,
    val address: String,
    val priceRange: String = "Harga belum tersedia",
    val hasWifi: Boolean,
    val plugCount: Int,
    val rating: Double? = null,
    val vibes: List<String>,
    @DrawableRes val imageResId: Int? = null,
    val description: String? = null,
    val openHours: String? = null,
    val priceMin: Int? = null,
    val priceMax: Int? = null,
    val reviewCount: Int = 0,
    val photoPath: String? = null,
    val photoUrl: String? = null,
    val isActive: Boolean = true
) {
    /** Compatibility for other Progress 1 modules; database relations populate this list. */
    val vibeNames: List<String> get() = vibes
}
