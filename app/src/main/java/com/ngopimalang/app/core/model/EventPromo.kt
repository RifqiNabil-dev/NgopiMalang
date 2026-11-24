package com.ngopimalang.app.core.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class EventCategory(val label: String) {
    EVENT("Event"),
    PROMO("Promo");

    /** Nilai kolom `event_promos.category` di database. */
    val dbValue: String get() = name.lowercase()

    companion object {
        fun fromDb(value: String?): EventCategory =
            entries.firstOrNull { it.dbValue.equals(value?.trim(), ignoreCase = true) } ?: EVENT
    }
}

/**
 * Status dihitung dari waktu.
 */
enum class EventStatus(val label: String) {
    UPCOMING("Akan datang"),
    ONGOING("Berlangsung"),
    FINISHED("Selesai");

    companion object {
        fun of(startsAt: Instant, endsAt: Instant, now: Instant = Clock.System.now()): EventStatus = when {
            now < startsAt -> UPCOMING
            now > endsAt -> FINISHED
            else -> ONGOING
        }
    }
}

/**
 * Model event & promo.
 */
data class EventPromo(
    val id: String,
    val title: String,
    val cafeName: String,
    val category: EventCategory,
    val startsAt: Instant,
    val endsAt: Instant,
    val description: String = "",
    val cafeId: String? = null,
    val priceMin: Int? = null,
    val priceMax: Int? = null,
    val isPublished: Boolean = true
) {
    val status: EventStatus get() = EventStatus.of(startsAt, endsAt)

    val schedule: String get() = formatSchedule(startsAt, endsAt)

    /** Format ringkas untuk preview card — satu hari: "21 Des • 19.00", multi-hari: "20 - 27 Des" */
    val scheduleShort: String get() = formatScheduleShort(startsAt, endsAt)
}

private val MONTHS_SHORT = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
private val DAYS_SHORT = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")

/**
 * Format tanggal-waktu lokal perangkat, contoh:
 * `Sab, 21 Des 2025 • 19.00 WIB`
 * Rentang satu hari digabung: `Sab, 21 Des 2025 • 19.00 - 22.00 WIB`.
 */
fun formatSchedule(startsAt: Instant, endsAt: Instant, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val start = startsAt.toLocalDateTime(zone)
    val end = endsAt.toLocalDateTime(zone)
    val startDate = "${DAYS_SHORT.getOrElse(start.dayOfWeek.ordinal) { "" }}, ${start.dayOfMonth} " +
        "${MONTHS_SHORT.getOrElse(start.monthNumber - 1) { "" }} ${start.year}"
    val startTime = "%02d.%02d".format(start.hour, start.minute)
    val endTime = "%02d.%02d".format(end.hour, end.minute)

    return if (start.date == end.date) {
        "$startDate • $startTime - $endTime WIB"
    } else {
        val endDate = "${end.dayOfMonth} ${MONTHS_SHORT.getOrElse(end.monthNumber - 1) { "" }} ${end.year}"
        "$startDate $startTime - $endDate $endTime WIB"
    }
}

fun formatScheduleShort(startsAt: Instant, endsAt: Instant, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val start = startsAt.toLocalDateTime(zone)
    val end   = endsAt.toLocalDateTime(zone)
    val startMonthStr = MONTHS_SHORT.getOrElse(start.monthNumber - 1) { "" }
    val endMonthStr   = MONTHS_SHORT.getOrElse(end.monthNumber - 1) { "" }

    return if (start.date == end.date) {
        // Satu hari — tampilkan tanggal + jam mulai
        "${start.dayOfMonth} $startMonthStr • %02d.%02d".format(start.hour, start.minute)
    } else if (start.month == end.month && start.year == end.year) {
        // Multi-hari, bulan sama
        "${start.dayOfMonth} - ${end.dayOfMonth} $startMonthStr ${start.year}"
    } else {
        // Multi-hari, bulan atau tahun beda
        "${start.dayOfMonth} $startMonthStr - ${end.dayOfMonth} $endMonthStr"
    }
}
