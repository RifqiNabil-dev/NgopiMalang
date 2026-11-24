package com.ngopimalang.app.features.bookmark

import com.ngopimalang.app.core.data.Supabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.hours

class BookmarkRepository(
    private val client: SupabaseClient = Supabase.client
) {

    @Serializable
    private data class BookmarkInsert(
        @SerialName("user_id")
        val userId: String,

        @SerialName("cafe_id")
        val cafeId: String
    )

    @Serializable
    private data class BookmarkIdRow(
        val id: String
    )

    private fun requireUserId(): String {
        return client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Silakan login")
    }

    // Mengambil URL foto dari Supabase Storage
    private suspend fun photoUrl(path: String?): String? {

        if (path.isNullOrBlank()) return null

        return try {
            client.storage
                .from("cafe-photos")
                .createSignedUrl(path, 1.hours)

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            null
        }
    }

    // Mengambil daftar bookmark pengguna
    suspend fun observeBookmarks(
        limit: Int = 50
    ): List<BookmarkItem> {

        val userId = requireUserId()

        val rows = client.from("bookmarks")
            .select(
                columns = Columns.raw(
                    """
                    id,
                    cafe_id,
                    note,
                    created_at,
                    cafes:cafe_id(
                        id,
                        name,
                        address,
                        photo_path,
                        price_min,
                        price_max,
                        is_active,
                        has_wifi,
                        plug_count
                    )
                    """.trimIndent()
                )
            ) {
                filter {
                    eq("user_id", userId)
                }

                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<BookmarkRow>()

        if (rows.isEmpty()) return emptyList()

        val cafeIds = rows.map {
            it.cafeId
        }.distinct()

        // Mengambil rating cafe
        val ratings = client.from("cafes_with_rating")
            .select(
                columns = Columns.raw(
                    "id,name,rating,review_count"
                )
            ) {
                filter {
                    isIn("id", cafeIds)
                }

                limit(cafeIds.size.toLong())
            }
            .decodeList<BookmarkCafeRow>()
            .associateBy { it.id }

        // Mengambil relasi cafe dan vibe
        val vibeLinks = client.from("cafe_vibes")
            .select(
                columns = Columns.raw(
                    "cafe_id,vibe_id"
                )
            ) {
                filter {
                    isIn("cafe_id", cafeIds)
                }
            }
            .decodeList<BookmarkVibeLink>()

        val vibeIds = vibeLinks.map {
            it.vibeId
        }.distinct()

        val vibeNames = if (vibeIds.isEmpty()) {

            emptyMap<String, String>()

        } else {

            client.from("vibes")
                .select(
                    columns = Columns.raw("id,name")
                ) {
                    filter {
                        isIn("id", vibeIds)
                    }
                }
                .decodeList<BookmarkVibeRow>()
                .associate { it.id to it.name }
        }

        val vibesByCafe = vibeLinks.groupBy {
            it.cafeId
        }

        // Menggabungkan seluruh informasi cafe
        return rows.map { row ->

            val item = BookmarkMapper.toModel(row)

            val cafeRating = ratings[row.cafeId]

            val names = vibesByCafe[row.cafeId]
                .orEmpty()
                .mapNotNull { link ->
                    vibeNames[link.vibeId]
                }

            item.copy(
                rating = cafeRating?.rating,
                reviewCount = cafeRating?.reviewCount ?: 0,
                photoUrl = photoUrl(item.photoPath),
                vibes = names
            )
        }
    }

    // Memeriksa status bookmark
    suspend fun isBookmarked(
        cafeId: String
    ): Boolean {

        val userId = requireUserId()

        return client.from("bookmarks")
            .select(
                columns = Columns.list("id")
            ) {
                filter {
                    eq("user_id", userId)
                    eq("cafe_id", cafeId)
                }

                limit(1)
            }
            .decodeList<BookmarkIdRow>()
            .isNotEmpty()
    }

    // Menambahkan bookmark
    suspend fun add(cafeId: String) {

        val userId = requireUserId()

        client.from("bookmarks").upsert(
            BookmarkInsert(
                userId = userId,
                cafeId = cafeId
            )
        ) {
            onConflict = "user_id,cafe_id"
            ignoreDuplicates = true
        }
    }

    // Menghapus bookmark
    suspend fun remove(
        cafeId: String
    ): Int {

        val userId = requireUserId()

        return client.from("bookmarks")
            .delete {
                filter {
                    eq("user_id", userId)
                    eq("cafe_id", cafeId)
                }

                select(
                    columns = Columns.list("id")
                )
            }
            .decodeList<BookmarkIdRow>()
            .size
    }

    // Memperbarui catatan
    suspend fun updateNote(
        cafeId: String,
        note: String
    ): Int {

        val userId = requireUserId()

        return client.from("bookmarks")
            .update(
                {
                    set("note", note.trim())
                }
            ) {
                filter {
                    eq("user_id", userId)
                    eq("cafe_id", cafeId)
                }

                select(
                    columns = Columns.list("id")
                )
            }
            .decodeList<BookmarkIdRow>()
            .size
    }

    // Toggle bookmark
    suspend fun toggle(
        cafeId: String
    ): Boolean {

        return if (isBookmarked(cafeId)) {

            val affectedRows = remove(cafeId)

            check(affectedRows > 0) {
                "Bookmark gagal dihapus"
            }

            false

        } else {

            add(cafeId)

            true
        }
    }

    fun isLoggedIn(): Boolean {
        return client.auth.currentUserOrNull() != null
    }
}
