package com.ngopimalang.app.features.vibe

import com.ngopimalang.app.core.data.Supabase
import com.ngopimalang.app.core.model.Cafe
import com.ngopimalang.app.core.model.Vibe
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class CafeRatingRow(
    val id: String,
    val name: String,
    val address: String = "",
    @SerialName("price_min") val priceMin: Int? = null,
    @SerialName("price_max") val priceMax: Int? = null,
    val rating: Double? = null,
    @SerialName("is_active") val isActive: Boolean = true
) {
    fun toModel(): Cafe {
        val price = when {
            priceMin != null && priceMax != null -> "Rp  - Rp "
            priceMin != null -> "Mulai Rp "
            else -> "-"
        }
        return Cafe(
            id = id,
            name = name,
            address = address,
            priceRange = price,
            hasWifi = false,
            plugCount = 0,
            rating = rating ?: 0.0,
            vibes = emptyList()
        )
    }
}

@Serializable
private data class CafeVibeJoinRow(
    @SerialName("cafes") val cafe: CafeRatingRow
)

@Serializable
private data class CafeVibeRelation(
    @SerialName("cafe_id") val cafeId: String,
    @SerialName("vibe_id") val vibeId: String
)

class VibeRepository(private val client: SupabaseClient = Supabase.client) {

    suspend fun observeVibes(): List<Vibe> {
        return client.from("vibes_with_count")
            .select()
            .decodeList<VibeRow>()
            .map { VibeMapper.toModel(it) }
    }

    suspend fun loadVibe(id: String): Vibe? {
        return client.from("vibes_with_count")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<VibeRow>()
            ?.let { VibeMapper.toModel(it) }
    }

    suspend fun cafeIdsOfVibe(vibeId: String): Set<String> {
        return client.from("cafe_vibes")
            .select(Columns.raw("cafe_id, vibe_id")) {
                filter { eq("vibe_id", vibeId) }
            }
            .decodeList<CafeVibeRelation>()
            .map { it.cafeId }
            .toSet()
    }

    suspend fun cafesForVibe(vibeId: String, limit: Int = 50): List<Cafe> {
        return client.from("cafe_vibes")
            .select(Columns.raw("cafes:cafes_with_rating!inner(id, name, address, price_min, price_max, rating, is_active)")) {
                filter {
                    eq("vibe_id", vibeId)
                    eq("cafes.is_active", true)
                }
                limit(limit.toLong())
            }
            .decodeList<CafeVibeJoinRow>()
            .map { it.cafe.toModel() }
    }

    suspend fun create(draft: VibeDraft): Vibe {
        val inserted = client.from("vibes")
            .insert(VibeMapper.draftToRow(draft)) { select() }
            .decodeSingle<VibeRow>()
        setCafeRelations(inserted.id, draft.cafeIds)
        return loadVibe(inserted.id) ?: VibeMapper.toModel(inserted)
    }

    suspend fun update(id: String, draft: VibeDraft): Vibe {
        val updated = client.from("vibes")
            .update(VibeMapper.draftToRow(draft)) {
                filter { eq("id", id) }
                select()
            }
            .decodeSingle<VibeRow>()
        setCafeRelations(id, draft.cafeIds)
        return loadVibe(id) ?: VibeMapper.toModel(updated)
    }

    suspend fun delete(id: String) {
        client.from("vibes").delete { filter { eq("id", id) } }
    }

    suspend fun setCafeRelations(vibeId: String, cafeIds: List<String>) {
        if (cafeIds.isEmpty()) {
            client.from("cafe_vibes").delete { filter { eq("vibe_id", vibeId) } }
        } else {
            val existingIds = cafeIdsOfVibe(vibeId)
            val toDelete = existingIds - cafeIds.toSet()
            if (toDelete.isNotEmpty()) {
                client.from("cafe_vibes").delete {
                    filter {
                        eq("vibe_id", vibeId)
                        isIn("cafe_id", toDelete.toList())
                    }
                }
            }
            val relations = cafeIds.distinct().map { CafeVibeRelation(cafeId = it, vibeId = vibeId) }
            client.from("cafe_vibes").upsert(relations) { onConflict = "cafe_id, vibe_id" }
        }
    }

    suspend fun cafesForPicker(limit: Int = 100): List<Cafe> {
        return client.from("cafes_with_rating")
            .select(Columns.raw("id, name, address, price_min, price_max, rating, is_active")) {
                filter { eq("is_active", true) }
                limit(limit.toLong())
            }
            .decodeList<CafeRatingRow>()
            .map { it.toModel() }
    }
}
