package com.ngopimalang.app.features.directory

import com.ngopimalang.app.core.data.Supabase
import com.ngopimalang.app.core.model.Cafe
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CancellationException
import java.util.UUID
import kotlin.time.Duration.Companion.hours

class CafeRepository(private val client: SupabaseClient = Supabase.client) {
    suspend fun list(query: String, wifi: Boolean, plugs: Boolean, admin: Boolean): List<Cafe> {
        // Fixed view and bounded result; literal search avoids interpolating PostgREST filter syntax.
        val rows = client.postgrest["cafes_with_rating"].select {
            filter {
                if (!admin) eq("is_active", true)
                if (wifi) eq("has_wifi", true)
                if (plugs) gt("plug_count", 0)
            }
            order("name", Order.ASCENDING)
            limit(50)
        }.decodeList<CafeRow>()
        val keyword = query.trim()
        return rows.filter { keyword.isEmpty() || it.name.contains(keyword, true) ||
            it.address.orEmpty().contains(keyword, true) }.map { model(it) }
    }
    suspend fun loadCafe(id: String): Cafe? {
        UUID.fromString(id)
        val row = client.postgrest["cafes_with_rating"].select {
            filter { eq("id", id) }; limit(1)
        }.decodeSingleOrNull<CafeRow>() ?: return null
        val ids = links(id).map { it.vibeId }.toSet()
        val names = if (ids.isEmpty()) emptyList() else vibes().filter { it.id in ids }.map { it.name }
        return model(row, names)
    }
    suspend fun vibes() = client.postgrest["vibes"].select { order("name", Order.ASCENDING) }.decodeList<VibeOption>()
    suspend fun links(id: String) = client.postgrest["cafe_vibes"].select {
        filter { eq("cafe_id", id) }
    }.decodeList<CafeVibeLink>()
    suspend fun save(id: String?, draft: CafeDraft): String {
        require(draft.name.isNotBlank()) { "Nama cafe wajib diisi" }
        val rows = if (id == null) client.postgrest["cafes"].insert(CafeMapper.toWrite(draft)) {
            select()
        }.decodeList<CafeRow>() else client.postgrest["cafes"].update(CafeMapper.toWrite(draft)) {
            filter { eq("id", id) }; select()
        }.decodeList<CafeRow>()
        check(rows.size == 1) { "Cafe tidak berubah. Periksa hak admin atau muat ulang data." }
        return rows.single().id
    }
    suspend fun saveVibes(id: String, selected: Set<String>) {
        val old = links(id).map { it.vibeId }.toSet()
        // Add first. Partial failure is retryable by recomputing this difference.
        val additions = selected - old
        if (additions.isNotEmpty()) client.postgrest["cafe_vibes"].insert(additions.map { CafeVibeLink(id, it) })
        for (removed in old - selected) client.postgrest["cafe_vibes"].delete {
            filter { eq("cafe_id", id); eq("vibe_id", removed) }
        }
    }
    suspend fun photoUrl(path: String?): String? = path?.let {
        // Signed URLs work with authenticated/private buckets too; never persist this temporary URL.
        client.storage.from("cafe-photos").createSignedUrl(it, 1.hours)
    }
    private suspend fun model(row: CafeRow, names: List<String> = emptyList()): Cafe {
        val url = try { photoUrl(row.photoPath) } catch (e: CancellationException) { throw e }
            catch (_: Exception) { null }
        return CafeMapper.toModel(row, names, url)
    }
    suspend fun attachPhoto(id: String, photo: CafePhoto, oldPath: String?) {
        require(photo.bytes.size <= 5 * 1024 * 1024) { "Foto maksimal 5 MB" }
        require(photo.mime in setOf("image/jpeg", "image/png", "image/webp")) { "Pilih foto JPEG, PNG, atau WebP" }
        val path = "$id/${UUID.randomUUID()}.${photo.extension}"
        val bucket = client.storage.from("cafe-photos")
        bucket.upload(path, photo.bytes) { contentType = ContentType.parse(photo.mime); upsert = false }
        try {
            val changed = client.postgrest["cafes"].update(PhotoWrite(path)) {
                filter { eq("id", id) }; select()
            }.decodeList<CafeRow>()
            check(changed.size == 1) { "Tidak dapat mengaitkan foto ke cafe" }
        } catch (e: Exception) {
            try { bucket.delete(listOf(path)) } catch (_: Exception) { /* retry cleanup later */ }
            throw e
        }
        if (oldPath != null) try { bucket.delete(listOf(oldPath)) } catch (_: Exception) { /* old file only */ }
    }
    suspend fun archive(id: String) {
        val rows = client.postgrest["cafes"].update(ActiveWrite(false)) {
            filter { eq("id", id) }; select()
        }.decodeList<CafeRow>()
        check(rows.size == 1) { "Cafe tidak berubah. Periksa hak admin." }
    }
    suspend fun delete(id: String, path: String?) {
        val rows = client.postgrest["cafes"].delete {
            filter { eq("id", id) }; select()
        }.decodeList<CafeRow>()
        check(rows.size == 1) { "Cafe tidak terhapus. Periksa hak admin." }
        if (path != null) try { client.storage.from("cafe-photos").delete(listOf(path)) }
            catch (_: Exception) { /* row already deleted; an orphan does not invalidate deletion */ }
    }
}
