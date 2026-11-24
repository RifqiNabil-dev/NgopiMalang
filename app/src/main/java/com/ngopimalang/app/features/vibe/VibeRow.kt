package com.ngopimalang.app.features.vibe

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Bentuk baris `vibes` / `vibes_with_count`.
 *
 * Saat membaca tabel `vibes` biasa (insert/update), `cafe_count` tidak ada sehingga
 * tetap 0; angka yang ditampilkan layar selalu berasal dari view.
 * Kolom milik database (id, created_by, created_at, updated_at) tidak pernah dikirim.
 */
@Serializable
data class VibeRow(
    val id: String = "",
    val name: String,
    val description: String? = "",
    @SerialName("icon_key") val iconKey: String = "work",
    @SerialName("cafe_count") val cafeCount: Int = 0
)

data class VibeDraft(
    val name: String,
    val description: String,
    val iconKey: String,
    val cafeIds: List<String>
)
