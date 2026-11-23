package com.ngopimalang.app.features.vibe

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
