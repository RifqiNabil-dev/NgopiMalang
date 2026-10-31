package com.ngopimalang.app.core.model

enum class EventCategory(val label: String) {
    EVENT("Event"),
    PROMO("Promo")
}

/**
 * Model event & promo. Pemilik fitur: Rifqi (features.eventpromo).
 */
data class EventPromo(
    val id: String,
    val title: String,
    val cafeName: String,
    val category: EventCategory,
    val schedule: String,
    val description: String
)
