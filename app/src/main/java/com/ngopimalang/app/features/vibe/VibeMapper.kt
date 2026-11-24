package com.ngopimalang.app.features.vibe

import com.ngopimalang.app.core.model.Vibe

object VibeMapper {
    fun toModel(row: VibeRow): Vibe = Vibe(
        id = row.id,
        name = row.name,
        description = row.description.orEmpty(),
        iconKey = row.iconKey.ifBlank { "work" },
        cafeCount = row.cafeCount
    )

    fun draftToRow(draft: VibeDraft): VibeRow = VibeRow(
        name = draft.name.trim(),
        description = draft.description.trim(),
        iconKey = draft.iconKey.trim().lowercase().take(24).ifBlank { "work" }
    )
}
