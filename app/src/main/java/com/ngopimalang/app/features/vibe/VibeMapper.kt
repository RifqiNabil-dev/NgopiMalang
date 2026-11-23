package com.ngopimalang.app.features.vibe

import com.ngopimalang.app.core.model.Cafe
import com.ngopimalang.app.core.model.Vibe

object VibeMapper {

    fun toModel(row: VibeRow) = Vibe(
        id = row.id,
        name = row.name,
        description = row.description,
        iconKey = row.iconKey,
        cafeCount = row.cafeCount
    )

    /** icon_key dirapikan: trim, huruf kecil, maks 24 karakter, kosong = work. */
    fun draftToRow(draft: VibeDraft) = VibeRow(
        name = draft.name.trim(),
        description = draft.description.trim(),
        iconKey = draft.iconKey.trim().lowercase().take(24).ifBlank { "work" }
    )

    /** Sementara, menyesuaikan model Cafe milik Ossa yang sekarang. */
    fun toCafe(row: CafeRow) = Cafe(
        id = row.id,
        name = row.name,
        address = row.address.orEmpty(),
        priceRange = when {
            row.priceMin != null && row.priceMax != null -> "Rp${row.priceMin} - Rp${row.priceMax}"
            row.priceMin != null -> "Mulai Rp${row.priceMin}"
            row.priceMax != null -> "Hingga Rp${row.priceMax}"
            else -> "-"
        },
        hasWifi = false,
        plugCount = 0,
        rating = row.rating ?: 0.0,
        vibes = emptyList(),
        imageResId = null
    )
}