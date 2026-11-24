package com.ngopimalang.app.features.eventpromo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Bentuk baris tabel `event_promos.
 */
@Serializable
data class EventPromoRow(
    val id: String = "",
    @SerialName("cafe_id") val cafeId: String? = null,
    val title: String,
    val description: String? = null,
    val category: String = "event",
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    @SerialName("price_min") val priceMin: Int? = null,
    @SerialName("price_max") val priceMax: Int? = null,
    @SerialName("is_published") val isPublished: Boolean = true,
    val cafes: EventCafeRow? = null
)

/** Join minimal untuk nama cafe di kartu. */
@Serializable
data class EventCafeRow(
    val name: String? = null
)


@Serializable
data class EventPromoWrite(
    @SerialName("cafe_id") val cafeId: String?,
    val title: String,
    val description: String,
    val category: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String,
    @SerialName("price_min") val priceMin: Int?,
    @SerialName("price_max") val priceMax: Int?,
    @SerialName("is_published") val isPublished: Boolean
)

@Serializable
data class EventCafeOptionRow(
    val id: String,
    val name: String
)

@Serializable
data class DeletedEventRow(val id: String)

/** Input form, bebas dari tipe database. */
data class EventPromoDraft(
    val title: String,
    val description: String,
    val category: String,
    val startsAt: String,
    val endsAt: String,
    val cafeId: String?,
    val priceMin: Int?,
    val priceMax: Int?,
    val isPublished: Boolean
)
