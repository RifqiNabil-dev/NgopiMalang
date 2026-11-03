package com.ngopimalang.app.features.reviews

import androidx.lifecycle.ViewModel
import com.ngopimalang.app.core.data.DummyData
import com.ngopimalang.app.core.model.Review
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ReviewsUiState(
    val allReviews: List<Review> = emptyList(),
    val filteredReviews: List<Review> = emptyList(),
    val selectedRating: Int? = null,
    val averageRating: Double = 0.0
)

class ReviewsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewsUiState())
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()

    init {
        loadInitialReviews()
    }

    private fun loadInitialReviews() {
        val initialList = DummyData.reviews
        val avg = if (initialList.isNotEmpty()) initialList.map { it.rating }.average() else 0.0

        _uiState.update {
            it.copy(
                allReviews = initialList,
                filteredReviews = initialList,
                averageRating = avg
            )
        }
    }

    fun filterByRating(rating: Int?) {
        _uiState.update { currentState ->
            val filtered = if (rating == null) {
                currentState.allReviews
            } else {
                currentState.allReviews.filter { it.rating == rating }
            }
            currentState.copy(
                selectedRating = rating,
                filteredReviews = filtered
            )
        }
    }

    fun addReview(userName: String, cafeName: String, rating: Int, comment: String, favoriteMenu: String) {
        val newReview = Review(
            id = "r${System.currentTimeMillis()}",
            userName = userName.ifBlank { "Pengguna" },
            cafeName = cafeName.ifBlank { "Cafe Malang" },
            rating = rating,
            comment = comment,
            favoriteMenu = favoriteMenu.ifBlank { "-" },
            dateLabel = "Baru saja"
        )

        _uiState.update { currentState ->
            val updatedAll = listOf(newReview) + currentState.allReviews
            val newAvg = updatedAll.map { it.rating }.average()
            val filtered = if (currentState.selectedRating == null) {
                updatedAll
            } else {
                updatedAll.filter { it.rating == currentState.selectedRating }
            }
            currentState.copy(
                allReviews = updatedAll,
                filteredReviews = filtered,
                averageRating = newAvg
            )
        }
    }
}