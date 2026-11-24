package com.ngopimalang.app.features.bookmark

import kotlinx.datetime.Instant

object BookmarkMapper {

    fun toModel(row: BookmarkRow): BookmarkItem {

        val cafe = row.cafes

        return BookmarkItem(
            id = row.id,
            cafeId = row.cafeId,
            note = row.note.orEmpty(),
            savedAt = Instant.parse(row.createdAt),
            cafeName = cafe?.name ?: "Cafe tidak tersedia",
            address = cafe?.address.orEmpty(),
            photoPath = cafe?.photoPath,
            priceMin = cafe?.priceMin,
            priceMax = cafe?.priceMax,
            rating = cafe?.rating,
            reviewCount = cafe?.reviewCount ?: 0,
            isActive = cafe?.isActive ?: false,
            hasWifi = cafe?.hasWifi ?: false,
            plugCount = cafe?.plugCount ?: 0
        )
    }
}
