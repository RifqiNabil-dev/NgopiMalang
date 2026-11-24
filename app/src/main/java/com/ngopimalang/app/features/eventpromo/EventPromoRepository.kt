package com.ngopimalang.app.features.eventpromo

import com.ngopimalang.app.core.data.Supabase
import com.ngopimalang.app.core.model.EventCategory
import com.ngopimalang.app.core.model.EventPromo
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import java.util.UUID

/**
 * Satu-satunya tempat fitur Event & Promo memanggil Supabase (dok 10 aturan A3).
 * Tabel: `event_promos`. Menulis: admin. Membaca: event terbit untuk semua, plus
 * milik admin yang belum terbit karena policy mengizinkan `is_published or is_admin()`.
 */
class EventPromoRepository(private val client: SupabaseClient = Supabase.client) {

    /**
     * Daftar event milik satu kategori. [category] null berarti semua.
     * Pencarian teks dan penyaringan status dilakukan di ViewModel (dok 10 bagian 4),
     * supaya hanya ada satu sumber daftar dan tidak ada list yang disimpan ganda.
     */
    suspend fun observeEvents(category: EventCategory?, isAdmin: Boolean, limit: Int = 50): List<EventPromo> {
        val rows = client.postgrest["event_promos"]
            .select(
                Columns.raw(
                    "id, cafe_id, title, description, category, starts_at, ends_at, " +
                        "price_min, price_max, is_published, cafes(name)"
                )
            ) {
                filter {
                    if (category != null) eq("category", category.dbValue)
                    if (!isAdmin) eq("is_published", true)
                }
                order("starts_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<EventPromoRow>()
        return rows.map(EventPromoMapper::toModel)
    }

    /** Detail dibuka dengan id, bukan objek, agar tetap benar saat daftar belum termuat. */
    suspend fun loadEvent(id: String): EventPromo? {
        UUID.fromString(id)
        return client.postgrest["event_promos"]
            .select(
                Columns.raw(
                    "id, cafe_id, title, description, category, starts_at, ends_at, " +
                        "price_min, price_max, is_published, cafes(name)"
                )
            ) {
                filter { eq("id", id) }
                limit(1)
            }
            .decodeList<EventPromoRow>()
            .firstOrNull()
            ?.let(EventPromoMapper::toModel)
    }

    /** Daftar cafe untuk dipilih di form. Cafe arsip tidak ditawarkan. */
    suspend fun cafesForPicker(limit: Int = 100): List<EventCafeOptionRow> =
        client.postgrest["cafes_with_rating"]
            .select(Columns.raw("id, name")) {
                filter { eq("is_active", true) }
                order("name", Order.ASCENDING)
                limit(limit.toLong())
            }
            .decodeList<EventCafeOptionRow>()

    suspend fun create(draft: EventPromoDraft, cafeName: String): EventPromo {
        val rows = client.postgrest["event_promos"]
            .insert(EventPromoMapper.toWrite(draft)) { select() }
            .decodeList<EventPromoRow>()
        val created = rows.firstOrNull() ?: error("Event tidak dapat disimpan")
        // Baris hasil insert tidak membawa join cafes; nama diambil dari pilihan form.
        return EventPromoMapper.toModel(created).copy(cafeName = cafeName)
    }

    suspend fun update(id: String, draft: EventPromoDraft): EventPromo {
        val updated = client.postgrest["event_promos"]
            .update(EventPromoMapper.toWrite(draft)) {
                filter { eq("id", id) }
                select()
            }
            .decodeList<EventPromoRow>()
            .firstOrNull() ?: error("Event tidak ditemukan atau tidak dapat diubah")
        // Baca ulang supaya join cafes dan data tersimpan ikut terlihat.
        return loadEvent(id) ?: EventPromoMapper.toModel(updated)
    }

    /**
     * Mengembalikan jumlah baris yang benar-benar terhapus.
     * Policy menulis berbasis peran mengembalikan 0 baris, bukan exception
     * (dok 09 bagian 12.3), jadi jumlahnya wajib diperiksa pemanggil.
     */
    suspend fun delete(id: String): Int =
        client.postgrest["event_promos"]
            .delete {
                filter { eq("id", id) }
                select()
            }
            .decodeList<DeletedEventRow>()
            .size
}