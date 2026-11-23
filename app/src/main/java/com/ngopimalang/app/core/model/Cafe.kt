package com.ngopimalang.app.core.model

import androidx.annotation.DrawableRes

/**
 * Model cafe. Pada Progress 1 data diisi dari [com.ngopimalang.app.core.data.DummyData].
 * Pemilik fitur: Ossa (features.directory).
 */
data class Cafe(
    val id: String,
    val name: String,
    val address: String,
    val priceRange: String,
    val hasWifi: Boolean,
    val plugCount: Int,
    val rating: Double,
    val vibes: List<String>,
    @DrawableRes val imageResId: Int? = null
)
