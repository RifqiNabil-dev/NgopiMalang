package com.ngopimalang.app.features.vibe

import com.ngopimalang.app.core.data.Supabase
import com.ngopimalang.app.core.model.Cafe
import com.ngopimalang.app.core.model.Vibe
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class VibeRepository(private val client: SupabaseClient = Supabase.client) {

    suspend fun observeVibes(): List<Vibe> =
        client.from("vibes_with_count")
            .select { order("name", Order.ASCENDING) }
            .decodeList<VibeRow>()
            .map(VibeMapper::toModel)

    suspend fun loadVibe(id: String): Vibe? =
        client.from("vibes_with_count")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<VibeRow>()
            ?.let(VibeMapper::toModel)

    /** Relasi cafe_vibes, bukan pencocokan nama. Cafe arsip difilter. */
    suspend fun cafesForVibe(vibeId: String, limit: Int = 50): List<Cafe> =
        client.from("cafe_vibes")
            .select(
                Columns.raw(
                    "cafes!inner(id, name, address, photo_path, price_min, price_max, rating, review_count, is_active)"
                )
            ) {
                filter {
                    eq("vibe_id", vibeId)
                    eq("cafes.is_active", true)
                }
                limit(limit.toLong())
            }
            .decodeList<CafeVibeEmbed>()
            .map { VibeMapper.toCafe(it.cafes) }

    suspend fun cafesForPicker(limit: Int = 100): List<Cafe> =
        client.from("cafes_with_rating")
            .select {
                filter { eq("is_active", true) }
                order("name", Order.ASCENDING)
                limit(limit.toLong())
            }
            .decodeList<CafeRow>()
            .map(VibeMapper::toCafe)

    /** Dipakai form edit (isi centang) dan setCafeRelations. */
    suspend fun cafeIdsOfVibe(vibeId: String): Set<String> =
        client.from("cafe_vibes")
            .select { filter { eq("vibe_id", vibeId) } }
            .decodeList<CafeVibeRow>()
            .map { it.cafeId }
            .toSet()

    /** Hanya baris vibes. Id diambil dari hasil insert. */
    suspend fun create(draft: VibeDraft): Vibe {
        val row = VibeMapper.draftToRow(draft)
        return client.from("vibes")
            .insert(payload(row)) { select() }
            .decodeSingle<VibeRow>()
            .let(VibeMapper::toModel)
    }

    suspend fun update(id: String, draft: VibeDraft): Vibe {
        val row = VibeMapper.draftToRow(draft)
        return client.from("vibes")
            .update(payload(row)) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingle<VibeRow>()
            .let(VibeMapper::toModel)
    }

    /** cafe_vibes ikut terhapus (cascade). Peringatan jumlah cafe urusan UI. */
    suspend fun delete(id: String) {
        client.from("vibes").delete { filter { eq("id", id) } }
    }

    /** Hapus yang dilepas, insert yang baru dengan onConflict. */
    suspend fun setCafeRelations(vibeId: String, cafeIds: List<String>) {
        val wanted = cafeIds.toSet()
        val current = cafeIdsOfVibe(vibeId)

        val removed = current - wanted
        if (removed.isNotEmpty()) {
            client.from("cafe_vibes").delete {
                filter {
                    eq("vibe_id", vibeId)
                    isIn("cafe_id", removed.toList())
                }
            }
        }

        val added = wanted - current
        if (added.isNotEmpty()) {
            client.from("cafe_vibes").upsert(
                added.map { CafeVibeWrite(cafeId = it, vibeId = vibeId) }
            ) {
                onConflict = "cafe_id,vibe_id"
                ignoreDuplicates = true
            }
        }
    }

    /** Hanya kolom yang boleh dikirim; id dan cafe_count tidak ikut. */
    private fun payload(row: VibeRow) = buildJsonObject {
        put("name", row.name)
        put("description", row.description)
        put("icon_key", row.iconKey)
    }
}