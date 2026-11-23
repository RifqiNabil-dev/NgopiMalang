package com.ngopimalang.app.features.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngopimalang.app.core.data.DummyData
import com.ngopimalang.app.core.model.Review
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class ReviewsUiState(
    val allReviews: List<Review> = emptyList(),
    val selectedRating: Int? = null
)

class ReviewsViewModel : ViewModel() {

    private data class ReviewTemplate(
        val rating: Int,
        val favoriteMenu: String,
        val comment: String,
        val dateLabel: String
    )

    companion object {
        const val DEFAULT_OWNER = "Shelfina Khayla Anindita"

        private val _sharedReviews = MutableStateFlow<List<Review>>(emptyList())
        private val _seededCafes = mutableSetOf<String>()
        private val _seededOwners = mutableSetOf<String>()
        private var isInitialized = false

        private val ownerReviewTemplates = listOf(
            ReviewTemplate(
                rating = 5,
                favoriteMenu = "Es Kopi Susu Gula Aren",
                comment = "Tempat favoritku buat nugas lama, Wi-Fi kencang dan colokan tersedia di banyak meja. Kopi susu gula arennya creamy dan pas.",
                dateLabel = "1 hari lalu"
            ),
            ReviewTemplate(
                rating = 5,
                favoriteMenu = "Caramel Macchiato",
                comment = "Suasananya tenang dan estetik banget buat foto maupun kerja remote. Pelayanannya juga ramah.",
                dateLabel = "3 hari lalu"
            ),
            ReviewTemplate(
                rating = 4,
                favoriteMenu = "Matcha Latte",
                comment = "Matchanya pekat dan tidak terlalu manis, area duduknya nyaman buat diskusi santai bareng teman.",
                dateLabel = "5 hari lalu"
            ),
            ReviewTemplate(
                rating = 5,
                favoriteMenu = "V60 Arabica Gayo",
                comment = "Manual brew-nya enak dan aromanya wangi. Cocok banget buat ngopi sore sambil baca buku.",
                dateLabel = "1 minggu lalu"
            ),
            ReviewTemplate(
                rating = 4,
                favoriteMenu = "Butterscotch Sea Salt Latte",
                comment = "Rasa sea salt-nya gurih seimbang sama kopinya. Bakal sering mampir ke sini lagi.",
                dateLabel = "1 minggu lalu"
            ),
            ReviewTemplate(
                rating = 5,
                favoriteMenu = "Caffe Latte & Croissant",
                comment = "Croissant-nya renyah dan hangat, pas banget dinikmati bareng hot latte pas cuaca Malang lagi dingin.",
                dateLabel = "2 minggu lalu"
            )
        )

        private val reviewerPool = listOf(
            "Dinda Ayu",
            "Bagas Pratama",
            "Sari Wulandari",
            "Raka Mahendra",
            "Nabila Putri",
            "Farhan Rizky",
            "Aulia Rahma",
            "Dimas Saputra",
            "Keisha Maharani",
            "Alvin Kurniawan",
            "Nadia Salsabila",
            "Yoga Permana",
            "Intan Permatasari",
            "Fajar Nugroho",
            "Clarissa Putri",
            "Reza Pahlevi"
        )

        private val reviewTemplates = listOf(
            ReviewTemplate(
                rating = 5,
                favoriteMenu = "Es Kopi Susu Gula Aren",
                comment = "Tempatnya nyaman banget buat nugas lama, Wi-Fi stabil dan colokan gampang dicari. Kopi susu gula arennya juga pas manisnya.",
                dateLabel = "2 hari lalu"
            ),
            ReviewTemplate(
                rating = 5,
                favoriteMenu = "V60 Arabica Gayo",
                comment = "Baristanya ramah dan mau jelasin karakter biji kopinya. Manual brew-nya clean dan aromanya keluar banget.",
                dateLabel = "5 hari lalu"
            ),
            ReviewTemplate(
                rating = 5,
                favoriteMenu = "Caffe Latte & Croissant",
                comment = "Croissant-nya renyah di luar lembut di dalam, cocok banget dipairing sama hot latte pas cuaca Malang lagi dingin.",
                dateLabel = "1 minggu lalu"
            ),
            ReviewTemplate(
                rating = 5,
                favoriteMenu = "Kopi Tubruk Malang",
                comment = "Bikin betah berjam-jam, musiknya santai nggak terlalu keras jadi enak buat diskusi kelompok atau kerja remote.",
                dateLabel = "2 minggu lalu"
            ),
            ReviewTemplate(
                rating = 5,
                favoriteMenu = "Iced Lemon Tea & French Fries",
                comment = "Camilannya porsinya pas buat sharing bareng teman. Pelayanannya sigap dan tempatnya bersih.",
                dateLabel = "3 minggu lalu"
            ),
            ReviewTemplate(
                rating = 4,
                favoriteMenu = "Caramel Macchiato",
                comment = "Suasananya cozy dan estetik buat nongkrong sore. Agak ramai pas jam pulang kuliah, tapi pelayanan tetap cepat dan ramah.",
                dateLabel = "4 hari lalu"
            ),
            ReviewTemplate(
                rating = 4,
                favoriteMenu = "Matcha Latte",
                comment = "Menu non-coffee-nya juara, matchanya pekat dan nggak kemanisan. Area duduknya lega dan udaranya adem.",
                dateLabel = "1 minggu lalu"
            ),
            ReviewTemplate(
                rating = 4,
                favoriteMenu = "Butterscotch Sea Salt Latte",
                comment = "Menu signature-nya wajib dicoba, gurih sea salt-nya berasa. Bakal balik lagi ke sini buat WFC.",
                dateLabel = "2 minggu lalu"
            ),
            ReviewTemplate(
                rating = 4,
                favoriteMenu = "Chocolate Hazelnut",
                comment = "Pencahayaannya hangat dan banyak spot duduk nyaman. Harga menu makanan dan minumannya masih sangat terjangkau.",
                dateLabel = "1 bulan lalu"
            ),
            ReviewTemplate(
                rating = 3,
                favoriteMenu = "Iced Americano",
                comment = "Kopinya oke dan harganya ramah di kantong mahasiswa. Cuma kalau malam weekend area parkir agak penuh.",
                dateLabel = "3 minggu lalu"
            ),
            ReviewTemplate(
                rating = 2,
                favoriteMenu = "Kopi Susu Klasik",
                comment = "Tempatnya cukup oke untuk singgah sebentar, tapi saat jam sibuk pesanan agak lama keluar.",
                dateLabel = "1 bulan lalu"
            ),
            ReviewTemplate(
                rating = 1,
                favoriteMenu = "Es Kopi Hitam",
                comment = "Waktu datang kemarin kondisi sedang penuh sekali sehingga kurang kondusif untuk kerja.",
                dateLabel = "1 bulan lalu"
            )
        )
    }

    private val _uiState = MutableStateFlow(ReviewsUiState())
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()

    init {
        if (!isInitialized) {
            _sharedReviews.value = DummyData.reviews
            isInitialized = true
            seedAllDirectoryCafesForOwner(DEFAULT_OWNER)
        }
        viewModelScope.launch {
            _sharedReviews.collect { latestReviews ->
                _uiState.update { it.copy(allReviews = latestReviews) }
            }
        }
    }

    fun filterByRating(rating: Int?) {
        _uiState.update { it.copy(selectedRating = rating) }
    }

    /**
     * Menghitung rata-rata rating terbaru sebuah cafe dari seluruh ulasan yang ada.
     */
    fun calculateCafeRating(
        reviews: List<Review>,
        cafeName: String,
        fallbackRating: Double = 0.0
    ): Double {
        val cafeReviews = reviews.filter { it.cafeName.equals(cafeName, ignoreCase = true) }
        return if (cafeReviews.isNotEmpty()) {
            cafeReviews.map { it.rating }.average()
        } else {
            fallbackRating
        }
    }

    fun formatRating(rating: Double): String {
        return String.format(Locale.US, "%.1f", rating)
    }

    fun seedAllDirectoryCafesForOwner(ownerName: String) {
        val normalizedOwner = ownerName.trim().lowercase()
        if (normalizedOwner.isBlank() || normalizedOwner in _seededOwners) return
        _seededOwners.add(normalizedOwner)

        val directoryCafes = DummyData.cafes
        val ownerReviewsToAdd = mutableListOf<Review>()

        directoryCafes.forEachIndexed { index, cafe ->
            val alreadyReviewedByOwner = _sharedReviews.value.any {
                it.userName.equals(ownerName, ignoreCase = true) &&
                        it.cafeName.equals(cafe.name, ignoreCase = true)
            }
            if (!alreadyReviewedByOwner) {
                val template = ownerReviewTemplates[index % ownerReviewTemplates.size]
                val ownerRating = if (cafe.rating >= 4.6) 5 else template.rating
                ownerReviewsToAdd.add(
                    Review(
                        id = "owner_${normalizedOwner.hashCode()}_${index}",
                        userName = ownerName,
                        cafeName = cafe.name,
                        rating = ownerRating,
                        comment = template.comment,
                        favoriteMenu = template.favoriteMenu,
                        dateLabel = template.dateLabel
                    )
                )
            }
        }

        if (ownerReviewsToAdd.isNotEmpty()) {
            _sharedReviews.value = ownerReviewsToAdd + _sharedReviews.value
        }

        directoryCafes.forEach { cafe ->
            ensureReviewsForCafe(cafe.name, cafe.rating)
        }
    }

    fun ensureReviewsForCafe(cafeName: String, targetRating: Double? = null) {
        val normalizedKey = cafeName.trim().lowercase()
        if (normalizedKey.isBlank() || normalizedKey in _seededCafes) return
        _seededCafes.add(normalizedKey)

        val existingForCafe = _sharedReviews.value.filter {
            it.cafeName.equals(cafeName, ignoreCase = true)
        }
        val seed = abs(normalizedKey.hashCode())

        val targetRatingsList: List<Int> = if (targetRating != null) {
            computeTargetRatings(existingForCafe.map { it.rating }, targetRating)
        } else {
            val targetCount = 4 + (seed % 2)
            val neededCount = (targetCount - existingForCafe.size).coerceAtLeast(0)
            List(neededCount) { idx -> if ((seed + idx) % 3 == 0) 4 else 5 }
        }

        if (targetRatingsList.isEmpty()) return

        val usedReviewers = existingForCafe.map { it.userName.lowercase() }.toMutableSet()
        val generatedReviews = mutableListOf<Review>()

        var offset = 0
        for (desiredRating in targetRatingsList) {
            var reviewer = reviewerPool[(seed + offset * 3) % reviewerPool.size]
            var guard = 0
            while (reviewer.lowercase() in usedReviewers && guard < reviewerPool.size) {
                offset++
                guard++
                reviewer = reviewerPool[(seed + offset * 3) % reviewerPool.size]
            }
            usedReviewers.add(reviewer.lowercase())

            val matchingTemplates = reviewTemplates.filter { it.rating == desiredRating }
            val template = matchingTemplates[(seed + offset) % matchingTemplates.size]

            generatedReviews.add(
                Review(
                    id = "seed_${normalizedKey.hashCode()}_${generatedReviews.size}",
                    userName = reviewer,
                    cafeName = cafeName,
                    rating = desiredRating,
                    comment = template.comment,
                    favoriteMenu = template.favoriteMenu,
                    dateLabel = template.dateLabel
                )
            )
            offset++
        }

        if (generatedReviews.isNotEmpty()) {
            _sharedReviews.value = _sharedReviews.value + generatedReviews
        }
    }

    private fun computeTargetRatings(existingRatings: List<Int>, targetRating: Double): List<Int> {
        val existingCount = existingRatings.size
        val existingSum = existingRatings.sum()
        val targetFormatted = String.format(Locale.US, "%.1f", targetRating)

        for (totalCount in maxOf(4, existingCount + 3)..10) {
            val neededCount = totalCount - existingCount
            val idealTotalSum = (targetRating * totalCount).roundToInt()
            val neededSum = idealTotalSum - existingSum

            if (neededSum in neededCount..(neededCount * 5)) {
                val actualAvg = (existingSum + neededSum).toDouble() / totalCount
                if (String.format(Locale.US, "%.1f", actualAvg) == targetFormatted) {
                    val base = neededSum / neededCount
                    val remainder = neededSum % neededCount
                    return List(neededCount) { index ->
                        if (index < remainder) base + 1 else base
                    }
                }
            }
        }

        val fallbackRating = targetRating.roundToInt().coerceIn(1, 5)
        return List(4) { fallbackRating }
    }

    fun getAvailableCafes(): List<String> {
        val directoryNames = DummyData.cafes.map { it.name }
        return (directoryNames + _sharedReviews.value.map { it.cafeName }).distinct()
    }

    fun addReview(
        userName: String,
        cafeName: String,
        rating: Int,
        comment: String,
        favoriteMenu: String
    ) {
        val newReview = Review(
            id = "r${System.currentTimeMillis()}",
            userName = userName.ifBlank { DEFAULT_OWNER },
            cafeName = cafeName.ifBlank { "Kopi Sae Malang" },
            rating = rating,
            comment = comment.trim(),
            favoriteMenu = favoriteMenu.ifBlank { "-" },
            dateLabel = "Baru saja"
        )

        _sharedReviews.value = listOf(newReview) + _sharedReviews.value
    }

    fun deleteReview(reviewId: String) {
        _sharedReviews.value = _sharedReviews.value.filterNot { it.id == reviewId }
    }
}