package com.ngopimalang.app.features.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngopimalang.app.core.model.Review
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewsUiState(
    val reviews: List<Review> = emptyList(),
    val ratingFilter: Int? = null,
    val selectedCafeFilter: String? = null,
    val onlyMine: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val isAdmin: Boolean = false,
    val summaryRating: Double? = null,
    val summaryReviewCount: Int = 0,
    val canWrite: Boolean = false,
    val cafeOptions: List<CafeOptionRow> = emptyList(),
    val currentUserName: String = ""
)

class ReviewsViewModel(
    private val repo: ReviewRepository = ReviewRepository()
) : ViewModel() {

    companion object {
        const val DEFAULT_OWNER = "Pengguna"
    }

    private val _uiState = MutableStateFlow(ReviewsUiState())
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()

    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    init {
        val initialAccess = repo.currentUserRoleInfo()
        _uiState.update {
            it.copy(
                isLoggedIn = initialAccess.isLoggedIn,
                isAdmin = initialAccess.isAdmin,
                canWrite = initialAccess.canWrite,
                onlyMine = !initialAccess.isAdmin
            )
        }
        loadReviews(ratingFilter = null)
    }

    fun setOnlyMine(onlyMine: Boolean) {
        _uiState.update { it.copy(onlyMine = onlyMine) }
    }

    fun onCafeFilterChange(cafeName: String?) {
        _uiState.update { it.copy(selectedCafeFilter = cafeName) }
    }

    fun onRatingFilterChange(rating: Int?) {
        if (_uiState.value.ratingFilter == rating) return
        loadReviews(ratingFilter = rating)
    }

    fun onRetry() {
        loadReviews(ratingFilter = _uiState.value.ratingFilter)
    }

    private fun loadReviews(ratingFilter: Int?) {
        val access = repo.currentUserRoleInfo()
        _uiState.update {
            it.copy(
                ratingFilter = ratingFilter,
                isLoading = true,
                errorMessage = null,
                isLoggedIn = access.isLoggedIn,
                isAdmin = access.isAdmin,
                canWrite = access.canWrite
            )
        }

        viewModelScope.launch {
            try {
                val latestAccess = repo.refreshCurrentUserRoleInfo()
                val reviews = repo.observeReviews(limit = 100)
                val cafes = repo.loadCafesForSelection()
                val summary = repo.overallCafesRating()
                val profileName = if (latestAccess.isLoggedIn) repo.currentUserDisplayName() else null
                _uiState.update {
                    it.copy(
                        reviews = if (ratingFilter == null) reviews else reviews.filter { item -> item.rating == ratingFilter },
                        summaryRating = summary.rating,
                        summaryReviewCount = summary.reviewCount,
                        cafeOptions = cafes,
                        currentUserName = profileName ?: it.currentUserName,
                        isLoggedIn = latestAccess.isLoggedIn,
                        isAdmin = latestAccess.isAdmin,
                        canWrite = latestAccess.canWrite,
                        onlyMine = !latestAccess.isAdmin,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, canWrite = false, errorMessage = repo.mapErrorMessage(e)) }
            }
        }
    }

    fun clearFormError() {
        _formError.value = null
    }

    fun upsertReviewForCafe(
        cafeId: String,
        draft: ReviewDraft,
        onSuccess: () -> Unit
    ) {
        if (draft.rating !in 1..5) {
            _formError.value = "Rating wajib diisi antara 1 sampai 5 bintang."
            return
        }

        val cafeName = _uiState.value.cafeOptions.firstOrNull { it.id == cafeId }?.name.orEmpty()
        _uiState.update { it.copy(isSaving = true) }
        _formError.value = null

        viewModelScope.launch {
            runCatching {
                repo.upsertReview(cafeId = cafeId, cafeName = cafeName, draft = draft)
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                loadReviews(ratingFilter = _uiState.value.ratingFilter)
                onSuccess()
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false) }
                _formError.value = repo.mapErrorMessage(throwable)
            }
        }
    }

    fun deleteMyReviewForCafe(cafeId: String) {
        val cafeName = _uiState.value.reviews.firstOrNull { it.cafeId == cafeId }?.cafeName.orEmpty()
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            runCatching {
                val affectedRows = repo.deleteMyReview(cafeId, cafeName)
                if (affectedRows == 0) {
                    throw IllegalStateException("42501: Ulasan tidak dapat dihapus.")
                }
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                loadReviews(ratingFilter = _uiState.value.ratingFilter)
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false) }
                _formError.value = repo.mapErrorMessage(throwable)
            }
        }
    }

    fun deleteReviewByAdmin(review: Review) {
        if (!repo.currentUserRoleInfo().isAdmin) return
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            runCatching {
                val affectedRows = repo.deleteReviewById(review.id)
                if (affectedRows == 0) {
                    throw IllegalStateException("42501: Ulasan tidak dapat dihapus.")
                }
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                loadReviews(ratingFilter = _uiState.value.ratingFilter)
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false) }
                _uiState.update { it.copy(errorMessage = repo.mapErrorMessage(throwable)) }
            }
        }
    }
}