package com.ngopimalang.app.core.model

import kotlinx.datetime.Instant

data class Review(
    val id: String,
    val userId: String = "",
    val cafeId: String = "",
    val userName: String,
    val cafeName: String,
    val rating: Int,
    val comment: String,
    val favoriteMenu: String,
    val createdAt: Instant,
    val isMine: Boolean = false
)