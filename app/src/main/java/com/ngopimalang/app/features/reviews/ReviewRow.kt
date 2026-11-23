package com.ngopimalang.app.features.reviews

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("cafe_id") val cafeId: String,
    val rating: Int,
    val comment: String? = null,
    @SerialName("favorite_menu") val favoriteMenu: String? = null,
    @SerialName("created_at") val createdAt: String,
    val profiles: ProfileNameRow? = null,
    val cafes: CafeNameRow? = null
)

@Serializable
data class ProfileNameRow(
    @SerialName("display_name") val displayName: String? = null
)

@Serializable
data class CafeNameRow(
    val name: String? = null
)

@Serializable
data class ReviewUpsertPayload(
    @SerialName("user_id") val userId: String,
    @SerialName("cafe_id") val cafeId: String,
    val rating: Int,
    val comment: String,
    @SerialName("favorite_menu") val favoriteMenu: String
)

@Serializable
data class CafeRatingRow(
    val rating: Double? = null,
    @SerialName("review_count") val reviewCount: Int = 0
)

@Serializable
data class CafeOptionRow(
    val id: String,
    val name: String
)

@Serializable
data class DeletedReviewRow(
    val id: String
)

data class ReviewDraft(
    val rating: Int,
    val comment: String,
    val favoriteMenu: String
)