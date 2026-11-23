package com.ngopimalang.app.features.reviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngopimalang.app.core.components.RatingStars
import com.ngopimalang.app.core.components.ReviewCard
import com.ngopimalang.app.core.model.Review
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class ReviewSectionUiState(
    val cafeId: String = "",
    val cafeName: String = "",
    val defaultRating: Double = 4.5,
    val reviews: List<Review> = emptyList(),
    val myReview: Review? = null,
    val ratingFilter: Int? = null,
    val rating: Double? = null,
    val reviewCount: Int = 0,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val isAdmin: Boolean = false,
    val canWrite: Boolean = false
)

class ReviewSectionViewModel(
    private val repo: ReviewRepository = ReviewRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewSectionUiState())
    val uiState: StateFlow<ReviewSectionUiState> = _uiState.asStateFlow()

    private val _isFormVisible = MutableStateFlow(false)
    val isFormVisible: StateFlow<Boolean> = _isFormVisible.asStateFlow()

    private val _formError = MutableStateFlow<String?>(null)
    val formError: StateFlow<String?> = _formError.asStateFlow()

    fun load(
        cafeId: String,
        cafeName: String = "",
        defaultRating: Double = 4.5,
        ratingFilter: Int? = _uiState.value.ratingFilter
    ) {
        if (cafeId.isBlank() && cafeName.isBlank()) return
        val access = repo.currentUserRoleInfo()
        _uiState.update {
            it.copy(
                cafeId = cafeId,
                cafeName = cafeName,
                defaultRating = defaultRating,
                ratingFilter = ratingFilter,
                isLoading = true,
                errorMessage = null,
                isLoggedIn = access.isLoggedIn,
                isAdmin = access.isAdmin,
                canWrite = access.canWrite
            )
        }

        viewModelScope.launch {
            val resolvedCafeId = resolveSupabaseCafeId(cafeId, cafeName)
            val remoteReviews = runCatching {
                repo.observeReviews(cafeId = resolvedCafeId, ratingFilter = null, limit = 50)
            }.getOrDefault(emptyList())

            val localReviews = LocalUserReviewStore.getByCafe(resolvedCafeId, cafeName)
            val seededReviews = repo.defaultCommunityReviewsForCafe(
                cafeId = resolvedCafeId,
                cafeName = cafeName,
                targetRating = defaultRating
            )

            val allCombined = buildCombinedReviews(
                remoteReviews = remoteReviews,
                localReviews = localReviews,
                seededReviews = seededReviews
            )

            val filteredList = if (ratingFilter == null) {
                allCombined
            } else {
                allCombined.filter { it.rating == ratingFilter }
            }

            val averageRating = if (allCombined.isNotEmpty()) {
                allCombined.map { it.rating }.average()
            } else {
                0.0
            }

            val latestAccess = repo.currentUserRoleInfo()
            _uiState.update {
                it.copy(
                    cafeId = resolvedCafeId,
                    reviews = filteredList,
                    myReview = allCombined.firstOrNull { item -> item.isMine },
                    rating = averageRating,
                    reviewCount = allCombined.size,
                    isLoggedIn = latestAccess.isLoggedIn,
                    isAdmin = latestAccess.isAdmin,
                    canWrite = latestAccess.canWrite,
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    private fun buildCombinedReviews(
        remoteReviews: List<Review>,
        localReviews: List<Review>,
        seededReviews: List<Review>
    ): List<Review> {
        val userAndRemote = (localReviews + remoteReviews).distinctBy { it.id }
        val nonMineUserAndRemote = userAndRemote.filterNot { it.isMine }
        val myReviews = userAndRemote.filter { it.isMine }

        val combined = if (nonMineUserAndRemote.size >= 3) {
            myReviews + nonMineUserAndRemote
        } else {
            myReviews + nonMineUserAndRemote + seededReviews
        }

        return combined.distinctBy { it.id }
    }

    private suspend fun resolveSupabaseCafeId(rawCafeId: String, cafeName: String): String {
        if (repo.isUuid(rawCafeId)) return rawCafeId
        if (cafeName.isNotBlank()) {
            val matched = repo.loadCafesForSelection()
                .firstOrNull { it.name.equals(cafeName.trim(), ignoreCase = true) }
            if (matched != null) return matched.id
        }
        return rawCafeId
    }

    fun onRatingFilterChange(rating: Int?) {
        if (_uiState.value.ratingFilter == rating) return
        load(
            cafeId = _uiState.value.cafeId,
            cafeName = _uiState.value.cafeName,
            defaultRating = _uiState.value.defaultRating,
            ratingFilter = rating
        )
    }

    fun openForm() {
        _formError.value = null
        _isFormVisible.value = true
    }

    fun closeForm() {
        if (!_uiState.value.isSaving) {
            _formError.value = null
            _isFormVisible.value = false
        }
    }

    fun onSubmit(draft: ReviewDraft) {
        val currentCafeId = _uiState.value.cafeId
        val currentCafeName = _uiState.value.cafeName
        if (currentCafeId.isBlank()) return
        if (draft.rating !in 1..5) {
            _formError.value = "Rating wajib diisi antara 1 sampai 5 bintang."
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        _formError.value = null

        viewModelScope.launch {
            runCatching {
                val resolvedCafeId = resolveSupabaseCafeId(currentCafeId, currentCafeName)
                repo.upsertReview(
                    cafeId = resolvedCafeId,
                    cafeName = currentCafeName,
                    draft = draft
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                _isFormVisible.value = false
                load(
                    cafeId = currentCafeId,
                    cafeName = currentCafeName,
                    defaultRating = _uiState.value.defaultRating,
                    ratingFilter = _uiState.value.ratingFilter
                )
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false) }
                _formError.value = repo.mapErrorMessage(throwable)
            }
        }
    }

    fun onDeleteMine() {
        val currentCafeId = _uiState.value.cafeId
        val currentCafeName = _uiState.value.cafeName
        if (currentCafeId.isBlank()) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            runCatching {
                val resolvedCafeId = resolveSupabaseCafeId(currentCafeId, currentCafeName)
                val affectedRows = repo.deleteMyReview(resolvedCafeId, currentCafeName)
                if (affectedRows == 0) {
                    throw IllegalStateException("42501: Ulasan tidak dapat dihapus.")
                }
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                load(
                    cafeId = currentCafeId,
                    cafeName = currentCafeName,
                    defaultRating = _uiState.value.defaultRating,
                    ratingFilter = _uiState.value.ratingFilter
                )
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false) }
                _uiState.update { it.copy(errorMessage = repo.mapErrorMessage(throwable)) }
            }
        }
    }

    fun onDeleteReview(review: Review) {
        val access = repo.currentUserRoleInfo()
        if (!access.isAdmin && !review.isMine) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            runCatching {
                val affectedRows = if (access.isAdmin) {
                    repo.deleteReviewById(review.id)
                } else {
                    repo.deleteMyReview(_uiState.value.cafeId, _uiState.value.cafeName)
                }
                if (affectedRows == 0) {
                    throw IllegalStateException("42501: Ulasan tidak dapat dihapus.")
                }
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                load(
                    cafeId = _uiState.value.cafeId,
                    cafeName = _uiState.value.cafeName,
                    defaultRating = _uiState.value.defaultRating,
                    ratingFilter = _uiState.value.ratingFilter
                )
            }.onFailure { throwable ->
                _uiState.update { it.copy(isSaving = false) }
                _uiState.update { it.copy(errorMessage = repo.mapErrorMessage(throwable)) }
            }
        }
    }

    fun onRetry() {
        load(
            cafeId = _uiState.value.cafeId,
            cafeName = _uiState.value.cafeName,
            defaultRating = _uiState.value.defaultRating,
            ratingFilter = _uiState.value.ratingFilter
        )
    }
}

@Composable
fun ReviewSection(
    cafeId: String,
    cafeName: String = "",
    defaultRating: Double = 4.5,
    sectionViewModel: ReviewSectionViewModel = viewModel(key = "review_section_$cafeId")
) {
    val uiState by sectionViewModel.uiState.collectAsState()
    val isFormVisible by sectionViewModel.isFormVisible.collectAsState()
    val formError by sectionViewModel.formError.collectAsState()
    var selectedReviewDetail by remember { mutableStateOf<Review?>(null) }
    var reviewPendingDelete by remember { mutableStateOf<Review?>(null) }
    var showLoginRequiredDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cafeId, cafeName, defaultRating) {
        sectionViewModel.load(
            cafeId = cafeId,
            cafeName = cafeName,
            defaultRating = defaultRating
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Ulasan & Rating",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (!uiState.isAdmin) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            if (uiState.isLoggedIn) {
                                sectionViewModel.openForm()
                            } else {
                                showLoginRequiredDialog = true
                            }
                        },
                        enabled = !uiState.isSaving
                    ) {
                        Icon(
                            imageVector = if (uiState.myReview != null) Icons.Default.Edit else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (uiState.myReview != null) "Edit ulasan" else "Tulis Ulasan")
                    }

                    if (uiState.myReview != null && uiState.canWrite) {
                        OutlinedButton(
                            onClick = { sectionViewModel.onDeleteMine() },
                            enabled = !uiState.isSaving
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus ulasan saya",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Rata-rata Rating Cafe",
                        style = MaterialTheme.typography.labelLarge
                    )
                    val displayRating = uiState.rating ?: defaultRating
                    Text(
                        text = String.format(Locale.US, "%.1f / 5.0", displayRating),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    RatingStars(rating = displayRating.toInt())
                }
                Text(
                    text = "${uiState.reviewCount} Ulasan",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                null to "Semua",
                5 to "⭐ 5",
                4 to "⭐ 4",
                3 to "⭐ 3",
                2 to "⭐ 2",
                1 to "⭐ 1"
            )
            items(filters) { (ratingValue, label) ->
                FilterChip(
                    selected = uiState.ratingFilter == ratingValue,
                    onClick = { sectionViewModel.onRatingFilterChange(ratingValue) },
                    label = { Text(label) }
                )
            }
        }

        if (uiState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { sectionViewModel.onRetry() }) {
                        Text("Coba Lagi")
                    }
                }
            }
        }

        if (!uiState.isLoading && uiState.errorMessage == null && uiState.reviews.isEmpty()) {
            Text(
                text = if (uiState.ratingFilter == null) {
                    "Belum ada ulasan untuk cafe ini."
                } else {
                    "Belum ada ulasan dengan rating ini."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.reviews.forEach { review ->
                    key(review.id) {
                        ReviewCard(
                            review = review,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { selectedReviewDetail = review },
                            onEditClick = if (review.isMine && uiState.canWrite) {
                                { sectionViewModel.openForm() }
                            } else null,
                            onDeleteClick = when {
                                uiState.isAdmin -> {
                                    { reviewPendingDelete = review }
                                }
                                review.isMine && uiState.canWrite -> {
                                    { sectionViewModel.onDeleteMine() }
                                }
                                else -> null
                            }
                        )
                    }
                }
            }
        }
    }

    reviewPendingDelete?.let { targetReview ->
        AlertDialog(
            onDismissRequest = { if (!uiState.isSaving) reviewPendingDelete = null },
            title = { Text("Hapus Ulasan (Admin)") },
            text = {
                Text("Hapus ulasan dari ${targetReview.userName} (${targetReview.rating} bintang)?")
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        sectionViewModel.onDeleteReview(targetReview)
                        reviewPendingDelete = null
                    }
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !uiState.isSaving,
                    onClick = { reviewPendingDelete = null }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showLoginRequiredDialog) {
        AlertDialog(
            onDismissRequest = { showLoginRequiredDialog = false },
            title = { Text("Login Diperlukan") },
            text = { Text("Silakan login terlebih dahulu di tab Profil untuk menulis ulasan.") },
            confirmButton = {
                TextButton(onClick = { showLoginRequiredDialog = false }) {
                    Text("Mengerti")
                }
            }
        )
    }

    if (isFormVisible) {
        ReviewFormDialog(
            existingReview = uiState.myReview,
            isSaving = uiState.isSaving,
            externalErrorMessage = formError,
            onDismiss = { sectionViewModel.closeForm() },
            onSubmit = { draft -> sectionViewModel.onSubmit(draft) }
        )
    }

    selectedReviewDetail?.let { review ->
        ReviewDetailDialog(
            review = review,
            onDismiss = { selectedReviewDetail = null }
        )
    }
}