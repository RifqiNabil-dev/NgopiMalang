package com.ngopimalang.app.features.eventpromo

import com.ngopimalang.app.core.model.EventCategory
import com.ngopimalang.app.core.model.EventPromo
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

object EventPromoMapper {

    /** `null` bila kolom waktu kosong; timestamp database selalu UTC dan berakhiran Z. */
    fun toModel(row: EventPromoRow): EventPromo {
        val start = runCatching { Instant.parse(row.startsAt) }.getOrNull()
        val end = runCatching { Instant.parse(row.endsAt) }.getOrNull()
        val safeStart = start ?: Clock.System.now()
        val safeEnd = end ?: safeStart
        return EventPromo(
            id = row.id,
            title = row.title,
            cafeName = row.cafes?.name?.trim().orEmpty(),
            category = EventCategory.fromDb(row.category),
            startsAt = safeStart,
            endsAt = safeEnd,
            description = row.description.orEmpty(),
            cafeId = row.cafeId,
            priceMin = row.priceMin,
            priceMax = row.priceMax,
            isPublished = row.isPublished
        )
    }

    fun toWrite(draft: EventPromoDraft): EventPromoWrite = EventPromoWrite(
        cafeId = draft.cafeId,
        title = draft.title.trim(),
        description = draft.description.trim(),
        category = draft.category,
        startsAt = draft.startsAt,
        endsAt = draft.endsAt,
        priceMin = draft.priceMin,
        priceMax = draft.priceMax,
        isPublished = draft.isPublished
    )
}