package com.ngopimalang.app.core.model

/**
 * Model kategori vibe. Pemilik fitur: Evia (features.vibe).
 * cafeCount berasal dari view vibes_with_count, bukan dihitung aplikasi.
 */
data class Vibe(
    val id: String,
    val name: String,
    val description: String = "",
    val iconKey: String = "",
    val cafeCount: Int = 0
)