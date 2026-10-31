package com.ngopimalang.app.core.model

/**
 * Model ulasan & rating. Pemilik fitur: Shelfina (features.reviews).
 */
data class Review(
    val id: String,
    val userName: String,
    val cafeName: String,
    val rating: Int,
    val comment: String,
    val favoriteMenu: String,
    val dateLabel: String
)
