package com.ngopimalang.app.features.bookmark

import kotlinx.datetime.Instant

data class BookmarkItem(
    val id: String,
    val cafeId: String,
    val note: String = "",
    val savedAt: Instant,
    val cafeName: String,
    val address: String = "",
    val photoPath: String? = null,
    val photoUrl: String? = null,
    val priceMin: Int? = null,
    val priceMax: Int? = null,
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val isActive: Boolean = true,
    val hasWifi: Boolean = false,
    val plugCount: Int = 0,
    val vibes: List<String> = emptyList()
)
