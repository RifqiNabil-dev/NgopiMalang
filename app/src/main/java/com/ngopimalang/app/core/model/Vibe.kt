package com.ngopimalang.app.core.model

/**
 * Model kategori vibe.
 */
data class Vibe(
    val id: String,
    val name: String,
    val description: String = "",
    val iconKey: String = "",
    val cafeCount: Int = 0
)