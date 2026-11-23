package com.ngopimalang.app.features.reviews

import com.ngopimalang.app.core.data.Supabase
import com.ngopimalang.app.core.model.Review
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class CafeRating(
    val rating: Double?,
    val reviewCount: Int
)

data class CurrentUserRoleInfo(
    val userId: String?,
    val isLoggedIn: Boolean,
    val isAdmin: Boolean
) {
    val canWrite: Boolean
        get() = isLoggedIn && !isAdmin
}

@Serializable
private data class ProfileWithIdRow(
    val id: String,
    val display_name: String? = null
)

object LocalUserReviewStore {
    private val localReviews = CopyOnWriteArrayList<Review>()
    private val dismissedSeedIds = ConcurrentHashMap.newKeySet<String>()

    fun getAll(): List<Review> = localReviews.toList()

    fun getByCafe(cafeId: String, cafeName: String): List<Review> {
        return localReviews.filter {
            it.cafeId == cafeId || (cafeName.isNotBlank() && it.cafeName.equals(cafeName, ignoreCase = true))
        }
    }

    fun upsert(review: Review) {
        localReviews.removeAll {
            it.isMine && (it.cafeId == review.cafeId || it.cafeName.equals(review.cafeName, ignoreCase = true))
        }
        localReviews.add(0, review)
    }

    fun delete(cafeId: String, cafeName: String): Boolean {
        return localReviews.removeAll {
            it.isMine && (it.cafeId == cafeId || (cafeName.isNotBlank() && it.cafeName.equals(cafeName, ignoreCase = true)))
        }
    }

    fun deleteById(reviewId: String): Boolean {
        if (reviewId.startsWith("seed_")) {
            return dismissedSeedIds.add(reviewId)
        }
        return localReviews.removeAll { it.id == reviewId }
    }

    fun isSeedDismissed(seedId: String): Boolean = dismissedSeedIds.contains(seedId)
}

class ReviewRepository(
    private val client: SupabaseClient = Supabase.client
) {
    private val uuidRegex =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    private val reviewSelectColumns = Columns.raw(
        "id, user_id, cafe_id, rating, comment, favorite_menu, created_at, profiles:user_id(display_name), cafes:cafe_id(name)"
    )

    private val reviewPlainSelectColumns = Columns.raw(
        "id, user_id, cafe_id, rating, comment, favorite_menu, created_at, cafes:cafe_id(name)"
    )

    fun isUuid(value: String): Boolean = uuidRegex.matches(value.trim())

    fun currentUserRoleInfo(): CurrentUserRoleInfo {
        val user = client.auth.currentUserOrNull()
        if (user == null) {
            return CurrentUserRoleInfo(userId = null, isLoggedIn = false, isAdmin = false)
        }
        val role = user.appMetadata?.get("role")?.jsonPrimitive?.contentOrNull
            ?: user.userMetadata?.get("role")?.jsonPrimitive?.contentOrNull
        val isAdmin = role.equals("admin", ignoreCase = true) ||
                user.email.equals("admin@ngopimalang.app", ignoreCase = true)

        return CurrentUserRoleInfo(
            userId = user.id,
            isLoggedIn = true,
            isAdmin = isAdmin
        )
    }

    suspend fun currentUserDisplayName(): String? {
        val user = client.auth.currentUserOrNull() ?: return null
        val fromProfile = runCatching {
            client.from("profiles")
                .select(columns = Columns.raw("display_name")) {
                    filter { eq("id", user.id) }
                    limit(1)
                }
                .decodeList<ProfileNameRow>()
                .firstOrNull()
                ?.displayName
                ?.trim()
        }.getOrNull()

        if (!fromProfile.isNullOrBlank() && !fromProfile.contains("@")) {
            return fromProfile
        }

        val fromMeta = (user.userMetadata?.get("display_name")?.jsonPrimitive?.contentOrNull
            ?: user.userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull)?.trim()
        if (!fromMeta.isNullOrBlank() && !fromMeta.contains("@")) {
            return fromMeta
        }

        val emailPrefix = user.email?.substringBefore("@")?.trim().orEmpty()
        if (emailPrefix.isNotBlank()) {
            return emailPrefix.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }

        return null
    }

    suspend fun observeReviews(
        cafeId: String? = null,
        ratingFilter: Int? = null,
        limit: Int = 50
    ): List<Review> {
        if (cafeId != null && !isUuid(cafeId)) {
            return emptyList()
        }

        val currentUserId = client.auth.currentUserOrNull()?.id
        val rows = runCatching {
            client.from("reviews")
                .select(columns = reviewSelectColumns) {
                    filter {
                        if (cafeId != null) eq("cafe_id", cafeId)
                        if (ratingFilter != null) eq("rating", ratingFilter)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<ReviewRow>()
        }.getOrElse {
            val plainRows = client.from("reviews")
                .select(columns = reviewPlainSelectColumns) {
                    filter {
                        if (cafeId != null) eq("cafe_id", cafeId)
                        if (ratingFilter != null) eq("rating", ratingFilter)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<ReviewRow>()

            val userIds = plainRows.map { it.userId }.distinct()
            val profileMap = if (userIds.isNotEmpty()) {
                runCatching {
                    client.from("profiles")
                        .select(columns = Columns.raw("id, display_name")) {
                            filter { isIn("id", userIds) }
                        }
                        .decodeList<ProfileWithIdRow>()
                        .associate { it.id to it.display_name }
                }.getOrDefault(emptyMap())
            } else {
                emptyMap()
            }

            plainRows.map { row ->
                row.copy(profiles = ProfileNameRow(displayName = profileMap[row.userId]))
            }
        }

        return rows.map { ReviewMapper.toModel(it, currentUserId) }
    }

    suspend fun loadMyReview(cafeId: String): Review? {
        if (!isUuid(cafeId)) return null
        val currentUserId = client.auth.currentUserOrNull()?.id ?: return null
        val rows = observeReviews(cafeId = cafeId, limit = 50)
        return rows.firstOrNull { it.userId == currentUserId }
    }

    suspend fun upsertReview(
        cafeId: String,
        cafeName: String = "",
        draft: ReviewDraft
    ): Review {
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Silakan login terlebih dahulu untuk menulis ulasan.")

        if (!isUuid(cafeId)) {
            val userName = currentUserDisplayName() ?: "Pengguna"
            val localReview = Review(
                id = "local_${currentUserId}_$cafeId",
                userId = currentUserId,
                cafeId = cafeId,
                userName = userName,
                cafeName = cafeName,
                rating = draft.rating,
                comment = draft.comment.trim(),
                favoriteMenu = draft.favoriteMenu.trim(),
                createdAt = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
                isMine = true
            )
            LocalUserReviewStore.upsert(localReview)
            return localReview
        }

        val payload = ReviewUpsertPayload(
            userId = currentUserId,
            cafeId = cafeId,
            rating = draft.rating,
            comment = draft.comment.trim(),
            favoriteMenu = draft.favoriteMenu.trim()
        )

        val row = runCatching {
            client.from("reviews")
                .upsert(value = payload) {
                    onConflict = "user_id,cafe_id"
                    select(columns = reviewSelectColumns)
                }
                .decodeSingle<ReviewRow>()
        }.getOrElse {
            client.from("reviews")
                .upsert(value = payload) {
                    onConflict = "user_id,cafe_id"
                    select(columns = reviewPlainSelectColumns)
                }
                .decodeSingle<ReviewRow>()
        }

        return ReviewMapper.toModel(row, currentUserId)
    }

    suspend fun deleteMyReview(cafeId: String, cafeName: String = ""): Int {
        val currentUserId = client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Silakan login terlebih dahulu.")

        if (!isUuid(cafeId)) {
            return if (LocalUserReviewStore.delete(cafeId, cafeName)) 1 else 0
        }

        val deleted = client.from("reviews")
            .delete {
                select(columns = Columns.raw("id"))
                filter {
                    eq("user_id", currentUserId)
                    eq("cafe_id", cafeId)
                }
            }
            .decodeList<DeletedReviewRow>()

        return deleted.size
    }

    suspend fun deleteReviewById(reviewId: String): Int {
        if (reviewId.startsWith("seed_") || reviewId.startsWith("local_")) {
            return if (LocalUserReviewStore.deleteById(reviewId)) 1 else 0
        }

        val deleted = client.from("reviews")
            .delete {
                select(columns = Columns.raw("id"))
                filter {
                    eq("id", reviewId)
                }
            }
            .decodeList<DeletedReviewRow>()

        return deleted.size
    }

    suspend fun cafeRating(cafeId: String): CafeRating {
        if (!isUuid(cafeId)) {
            return CafeRating(rating = null, reviewCount = 0)
        }
        val row = runCatching {
            client.from("cafes_with_rating")
                .select(columns = Columns.raw("rating, review_count")) {
                    filter { eq("id", cafeId) }
                    limit(1)
                }
                .decodeList<CafeRatingRow>()
                .firstOrNull()
        }.getOrNull()

        return CafeRating(
            rating = row?.rating,
            reviewCount = row?.reviewCount ?: 0
        )
    }

    suspend fun overallCafesRating(): CafeRating {
        val rows = runCatching {
            client.from("cafes_with_rating")
                .select(columns = Columns.raw("rating, review_count")) {
                    limit(100)
                }
                .decodeList<CafeRatingRow>()
        }.getOrDefault(emptyList())

        val totalCount = rows.sumOf { it.reviewCount }
        if (totalCount == 0) {
            return CafeRating(rating = null, reviewCount = 0)
        }

        val weightedSum = rows.sumOf { (it.rating ?: 0.0) * it.reviewCount }
        return CafeRating(
            rating = weightedSum / totalCount,
            reviewCount = totalCount
        )
    }

    suspend fun loadCafesForSelection(): List<CafeOptionRow> {
        return runCatching {
            client.from("cafes")
                .select(columns = Columns.raw("id, name")) {
                    order("name", Order.ASCENDING)
                    limit(100)
                }
                .decodeList<CafeOptionRow>()
        }.getOrDefault(emptyList())
    }

    fun defaultCommunityReviewsForCafe(
        cafeId: String,
        cafeName: String,
        targetRating: Double
    ): List<Review> {
        val safeName = cafeName.ifBlank { "Cafe Malang" }
        val ratings = when {
            targetRating >= 4.85 -> listOf(5, 5, 5, 5)
            targetRating >= 4.65 -> listOf(5, 5, 5, 4)
            targetRating >= 4.45 -> listOf(5, 5, 4, 4)
            targetRating >= 4.20 -> listOf(5, 4, 4, 4)
            else -> listOf(4, 4, 4, 4)
        }
        val now = System.currentTimeMillis()
        val dayMillis = 86_400_000L

        return listOf(
            Review(
                id = "seed_${cafeId}_1",
                userId = "seed_u1",
                cafeId = cafeId,
                userName = "Sari Wulandari",
                cafeName = safeName,
                rating = ratings[0],
                comment = "Suasananya nyaman banget buat santai maupun nugas. Racikan kopinya pas dan pelayanan baristanya ramah.",
                favoriteMenu = "Es Kopi Susu Gula Aren",
                createdAt = Instant.fromEpochMilliseconds(now - 2L * dayMillis),
                isMine = false
            ),
            Review(
                id = "seed_${cafeId}_2",
                userId = "seed_u2",
                cafeId = cafeId,
                userName = "Bagas Pratama",
                cafeName = safeName,
                rating = ratings[1],
                comment = "Tempat bersih, estetik, dan pencahayaannya hangat. Cocok buat nongkrong sore bareng teman.",
                favoriteMenu = "Caramel Macchiato",
                createdAt = Instant.fromEpochMilliseconds(now - 4L * dayMillis),
                isMine = false
            ),
            Review(
                id = "seed_${cafeId}_3",
                userId = "seed_u3",
                cafeId = cafeId,
                userName = "Dinda Ayu",
                cafeName = safeName,
                rating = ratings[2],
                comment = "Menu minumannya variatif dan harganya masih ramah di kantong mahasiswa Malang.",
                favoriteMenu = "Matcha Cream Latte",
                createdAt = Instant.fromEpochMilliseconds(now - 6L * dayMillis),
                isMine = false
            ),
            Review(
                id = "seed_${cafeId}_4",
                userId = "seed_u4",
                cafeId = cafeId,
                userName = "Fajar Hidayat",
                cafeName = safeName,
                rating = ratings[3],
                comment = "Area duduknya nyaman buat kerja remote, manual brew-nya juga wangi dan seimbang.",
                favoriteMenu = "V60 Gayo",
                createdAt = Instant.fromEpochMilliseconds(now - 9L * dayMillis),
                isMine = false
            )
        ).filterNot { LocalUserReviewStore.isSeedDismissed(it.id) }
    }

    fun mapErrorMessage(throwable: Throwable): String {
        val raw = throwable.message.orEmpty()
        return when {
            raw.contains("42501") || raw.contains("policy", ignoreCase = true) ->
                "Kamu tidak memiliki izin untuk melakukan aksi ini."
            raw.contains("Unable to resolve host", ignoreCase = true) ||
                    raw.contains("timeout", ignoreCase = true) ||
                    raw.contains("Failed to connect", ignoreCase = true) ->
                "Koneksi internet terputus. Periksa jaringan lalu coba lagi."
            else -> "Terjadi kesalahan saat memproses ulasan. Silakan coba lagi."
        }
    }
}