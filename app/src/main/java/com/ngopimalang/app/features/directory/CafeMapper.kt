package com.ngopimalang.app.features.directory

import com.ngopimalang.app.core.model.Cafe
import java.text.NumberFormat
import java.util.Locale

object CafeMapper {
    fun toModel(row: CafeRow, names: List<String> = emptyList(), photoUrl: String? = null) = Cafe(
        id = row.id, name = row.name, address = row.address.orEmpty(),
        priceRange = priceLabel(row.priceMin, row.priceMax), hasWifi = row.hasWifi,
        plugCount = row.plugCount, rating = row.rating, vibes = names,
        description = row.description, openHours = row.openHours, priceMin = row.priceMin,
        priceMax = row.priceMax, reviewCount = row.reviewCount, photoPath = row.photoPath,
        photoUrl = photoUrl, isActive = row.isActive
    )
    // Cloud requires these text columns to be non-null; empty text is valid for optional form input.
    fun toWrite(d: CafeDraft) = CafeWrite(d.name.trim(), d.address.trim(),
        d.description.trim(), d.openHours.trim(),
        d.priceMin, d.priceMax, d.hasWifi, d.plugCount)
}
fun priceLabel(min: Int?, max: Int?): String {
    val currency = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    return when {
        min != null && max != null -> "${currency.format(min)} – ${currency.format(max)}"
        min != null -> "Mulai ${currency.format(min)}"
        max != null -> "Hingga ${currency.format(max)}"
        else -> "Harga belum tersedia"
    }
}
