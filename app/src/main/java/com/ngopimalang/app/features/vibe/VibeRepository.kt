package com.ngopimalang.app.features.vibe

import com.ngopimalang.app.core.data.Supabase
import com.ngopimalang.app.core.model.Cafe
import com.ngopimalang.app.core.model.Vibe
import com.ngopimalang.app.features.directory.priceLabel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.Duration.Companion.hours

/**
 * Kolom cafes_with_rating yang dipakai fitur Vibe. `rating` null berarti belum ada ulasan;
 * nilai null tidak diubah menjadi 0 di sini (lihat 09-skema-database.md bagian 8).
 */
@Serializable
private data class VibeCafeRow(
    val id: String,
    val name: String,
    val address: String? = null,
    @SerialName("has_wifi") val hasWifi: Boolean = false,
    @SerialName("plug_count") val plugCount: Int = 0,
    @SerialName("price_min") val priceMin: Int? = null,
    @SerialName("price_max") val priceMax: Int? = null,
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    val rating: Double? = null,
    @SerialName("review_count") val reviewCount: Int = 0
)

@Serializable
private data class VibeCafeJoinRow(
    @SerialName("cafes") val cafe: VibeCafeRow
)

@Serializable
private data class VibeRelationRow(
    @SerialName("cafe_id") val cafeId: String,
    @SerialName("vibe_id") val vibeId: String
)

@Serializable
private data class VibeCafeLinkWrite(
    @SerialName("cafe_id") val cafeId: String,
    @SerialName("vibe_id") val vibeId: String
)

class VibeRepository(private val client: SupabaseClient = Supabase.client) {

    // Foto dibaca lewat signed URL karena bucket tidak diakses publik langsung.
    private suspend fun toModel(row: VibeCafeRow): Cafe = Cafe(
        id = row.id,
        name = row.name,
        address = row.address.orEmpty(),
        priceRange = priceLabel(row.priceMin, row.priceMax),
        hasWifi = row.hasWifi,
        plugCount = row.plugCount,
        rating = row.rating,
        vibes = emptyList(),
        priceMin = row.priceMin,
        priceMax = row.priceMax,
        reviewCount = row.reviewCount,
        photoPath = row.photoPath,
        photoUrl = row.photoPath?.let { path ->
            runCatching { client.storage.from(PHOTO_BUCKET).createSignedUrl(path, 1.hours) }.getOrNull()
        },
        isActive = row.isActive
    )

    /** Jumlah cafe diambil dari view, bukan dihitung dari daftar yang dibatasi limit. */
    suspend fun observeVibes(): List<Vibe> =
        client.postgrest["vibes_with_count"]
            .select { order("name", Order.ASCENDING) }
            .decodeList<VibeRow>()
            .map { VibeMapper.toModel(it) }

    suspend fun loadVibe(id: String): Vibe? {
        runCatching { UUID.fromString(id) }.getOrNull() ?: return null
        return client.postgrest["vibes_with_count"]
            .select { filter { eq("id", id) }; limit(1) }
            .decodeSingleOrNull<VibeRow>()
            ?.let { VibeMapper.toModel(it) }
    }

    suspend fun cafeIdsOfVibe(vibeId: String): Set<String> =
        client.postgrest["cafe_vibes"]
            .select(Columns.raw("cafe_id, vibe_id")) { filter { eq("vibe_id", vibeId) } }
            .decodeList<VibeRelationRow>()
            .map { it.cafeId }
            .toSet()

    /** Cafe milik vibe lewat relasi cafe_vibes; cafe arsip tidak ikut (09-skema bagian 11.1). */
    suspend fun cafesForVibe(vibeId: String, limit: Int = 50): List<Cafe> =
        client.postgrest["cafe_vibes"]
            .select(
                Columns.raw(
                    "cafes:cafes_with_rating!inner(id, name, address, has_wifi, plug_count, " +
                        "price_min, price_max, photo_path, is_active, rating, review_count)"
                )
            ) {
                filter {
                    eq("vibe_id", vibeId)
                    eq("cafes.is_active", true)
                }
                limit(limit.toLong())
            }
            .decodeList<VibeCafeJoinRow>()
            .map { toModel(it.cafe) }

    suspend fun create(draft: VibeDraft): Vibe {
        val inserted = client.postgrest["vibes"]
            .insert(VibeMapper.draftToRow(draft)) { select() }
            .decodeSingle<VibeRow>()
        setCafeRelations(inserted.id, draft.cafeIds)
        return loadVibe(inserted.id) ?: VibeMapper.toModel(inserted)
    }

    suspend fun update(id: String, draft: VibeDraft): Vibe {
        val updated = client.postgrest["vibes"]
            .update(VibeMapper.draftToRow(draft)) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle<VibeRow>()
        setCafeRelations(id, draft.cafeIds)
        return loadVibe(id) ?: VibeMapper.toModel(updated)
    }

    /** Relasi cafe_vibes ikut terhapus lewat FK on delete cascade; tidak ada error 23503 di sini. */
    suspend fun delete(id: String) {
        client.postgrest["vibes"].delete { filter { eq("id", id) } }
    }

    suspend fun setCafeRelations(vibeId: String, cafeIds: List<String>) {
        if (cafeIds.isEmpty()) {
            client.postgrest["cafe_vibes"].delete { filter { eq("vibe_id", vibeId) } }
            return
        }
        val existingIds = cafeIdsOfVibe(vibeId)
        val toDelete = existingIds - cafeIds.toSet()
        if (toDelete.isNotEmpty()) {
            client.postgrest["cafe_vibes"].delete {
                filter {
                    eq("vibe_id", vibeId)
                    isIn("cafe_id", toDelete.toList())
                }
            }
        }
        val relations = cafeIds.distinct().map { VibeCafeLinkWrite(cafeId = it, vibeId = vibeId) }
        client.postgrest["cafe_vibes"].upsert(relations) { onConflict = "cafe_id, vibe_id" }
    }

    /** Daftar cafe untuk form tambah/edit vibe. */
    suspend fun cafesForPicker(limit: Int = 100): List<Cafe> =
        client.postgrest["cafes_with_rating"]
            .select(
                Columns.raw(
                    "id, name, address, has_wifi, plug_count, price_min, price_max, " +
                        "photo_path, is_active, rating, review_count"
                )
            ) {
                filter { eq("is_active", true) }
                order("name", Order.ASCENDING)
                limit(limit.toLong())
            }
            .decodeList<VibeCafeRow>()
            .map { toModel(it) }

    private companion object {
        const val PHOTO_BUCKET = "cafe-photos"
    }
}
