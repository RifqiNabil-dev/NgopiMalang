package com.ngopimalang.app.features.reviews

import com.ngopimalang.app.core.model.Review
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

object ReviewMapper {

    fun toModel(row: ReviewRow, currentUserId: String?): Review {
        val rawDisplayName = row.profiles?.displayName?.trim().orEmpty()
        val safeUserName = if (rawDisplayName.isEmpty() || rawDisplayName.contains("@")) {
            "Pengguna"
        } else {
            rawDisplayName
        }

        val parsedInstant = runCatching {
            Instant.parse(row.createdAt)
        }.getOrElse {
            Clock.System.now()
        }

        return Review(
            id = row.id,
            userId = row.userId,
            cafeId = row.cafeId,
            userName = safeUserName,
            cafeName = row.cafes?.name?.trim().orEmpty(),
            rating = row.rating,
            comment = row.comment.orEmpty(),
            favoriteMenu = row.favoriteMenu.orEmpty(),
            createdAt = parsedInstant,
            isMine = currentUserId != null && row.userId == currentUserId
        )
    }
}