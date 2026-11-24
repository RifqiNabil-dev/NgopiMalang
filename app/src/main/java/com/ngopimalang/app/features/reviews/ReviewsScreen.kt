package com.ngopimalang.app.features.reviews

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ngopimalang.app.core.components.RatingStars
import com.ngopimalang.app.core.components.ReviewCard
import com.ngopimalang.app.core.components.SectionHeader
import com.ngopimalang.app.core.components.formatReviewDate
import com.ngopimalang.app.core.model.Review
import java.util.Locale

@Composable
fun ReviewsScreen(
    onNavigateBack: (() -> Unit)? = null,
    title: String = "Ulasan & Rating",
    owner: String = ReviewsViewModel.DEFAULT_OWNER,
    cafeName: String? = null,
    listComponent: String = "HorizontalPager + LazyColumn",
    flows: List<String> = listOf("Daftar Ulasan", "Detail Ulasan", "Form Ulasan"),
    viewModel: ReviewsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val formError by viewModel.formError.collectAsStateWithLifecycle()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    var selectedReviewDetail by remember { mutableStateOf<Review?>(null) }
    var reviewPendingDelete by remember { mutableStateOf<Review?>(null) }
    var showCafePicker by remember { mutableStateOf(false) }
    var showLoginRequiredDialog by remember { mutableStateOf(false) }
    var targetCafeIdForForm by remember { mutableStateOf<String?>(null) }
    var editingReview by remember { mutableStateOf<Review?>(null) }

    val isRegularUserMode = !uiState.isAdmin && cafeName == null

    LaunchedEffect(uiState.isAdmin, cafeName) {
        viewModel.setOnlyMine(!uiState.isAdmin && cafeName == null)
    }

    val displayedReviews = remember(
        uiState.reviews,
        cafeName,
        isRegularUserMode,
        uiState.selectedCafeFilter
    ) {
        when {
            cafeName != null -> uiState.reviews.filter {
                it.cafeName.equals(cafeName, ignoreCase = true)
            }
            isRegularUserMode -> uiState.reviews.filter { it.isMine }
            uiState.selectedCafeFilter != null -> uiState.reviews.filter {
                it.cafeName.equals(uiState.selectedCafeFilter, ignoreCase = true)
            }
            else -> uiState.reviews
        }
    }

    val displayedReviewCount = remember(
        displayedReviews,
        cafeName,
        isRegularUserMode,
        uiState.selectedCafeFilter,
        uiState.ratingFilter,
        uiState.summaryReviewCount
    ) {
        if (cafeName != null ||
            isRegularUserMode ||
            uiState.selectedCafeFilter != null ||
            uiState.ratingFilter != null
        ) {
            displayedReviews.size
        } else {
            uiState.summaryReviewCount
        }
    }

    val displayedAverageRating = remember(
        displayedReviews,
        cafeName,
        isRegularUserMode,
        uiState.selectedCafeFilter,
        uiState.ratingFilter,
        uiState.summaryRating
    ) {
        if (cafeName != null ||
            isRegularUserMode ||
            uiState.selectedCafeFilter != null ||
            uiState.ratingFilter != null
        ) {
            if (displayedReviews.isNotEmpty()) {
                displayedReviews.map { it.rating }.average()
            } else {
                0.0
            }
        } else {
            uiState.summaryRating ?: 0.0
        }
    }

    val openWriteForm: () -> Unit = {
        if (!uiState.isLoggedIn) {
            showLoginRequiredDialog = true
        } else {
            viewModel.clearFormError()
            val matchedCafe = if (cafeName != null) {
                uiState.cafeOptions.firstOrNull { it.name.equals(cafeName, ignoreCase = true) }
            } else {
                null
            }

            if (matchedCafe != null) {
                editingReview = uiState.reviews.firstOrNull {
                    it.isMine && it.cafeId == matchedCafe.id
                }
                targetCafeIdForForm = matchedCafe.id
            } else {
                showCafePicker = true
            }
        }
    }

    val headerTitle = when {
        cafeName != null -> "Ulasan $cafeName"
        isRegularUserMode -> "Ulasan Saya"
        else -> title
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (onNavigateBack != null) {
                            onNavigateBack()
                        } else {
                            backDispatcher?.onBackPressed()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali"
                    )
                }
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        floatingActionButton = {
            if (uiState.canWrite || !uiState.isLoggedIn) {
                FloatingActionButton(onClick = openWriteForm) {
                    Icon(Icons.Default.Add, contentDescription = "Tulis Ulasan")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        val ownerLabel = uiState.currentUserName.ifBlank { owner }
                        Text(
                            text = when {
                                cafeName != null -> "Rating $cafeName"
                                isRegularUserMode -> "Ulasan oleh $ownerLabel"
                                uiState.selectedCafeFilter != null -> "Rating ${uiState.selectedCafeFilter}"
                                else -> "Rata-rata Rating Semua Cafe"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f / 5.0", displayedAverageRating),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        RatingStars(rating = displayedAverageRating.toInt())
                    }
                    Text(
                        text = "$displayedReviewCount Ulasan",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (uiState.isAdmin && cafeName == null && uiState.cafeOptions.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCafeFilter == null,
                            onClick = { viewModel.onCafeFilterChange(null) },
                            label = { Text("Semua Cafe") }
                        )
                    }
                    items(uiState.cafeOptions, key = { it.id }) { cafeOption ->
                        FilterChip(
                            selected = uiState.selectedCafeFilter.equals(
                                cafeOption.name,
                                ignoreCase = true
                            ),
                            onClick = { viewModel.onCafeFilterChange(cafeOption.name) },
                            label = { Text(cafeOption.name) }
                        )
                    }
                }
            }

            SectionHeader(title = "Filter Rating", actionLabel = "")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
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
                        onClick = { viewModel.onRatingFilterChange(ratingValue) },
                        label = { Text(label) }
                    )
                }
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (!uiState.errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.onRetry() }) {
                            Text("Coba Lagi")
                        }
                    }
                }
            } else if (!uiState.isLoading && displayedReviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when {
                                isRegularUserMode && uiState.ratingFilter != null ->
                                    "Kamu belum menulis ulasan untuk rating ini."
                                isRegularUserMode ->
                                    "Kamu belum menulis ulasan."
                                cafeName != null ->
                                    "Belum ada ulasan untuk cafe ini."
                                uiState.ratingFilter != null ->
                                    "Belum ada ulasan untuk rating ini."
                                else ->
                                    "Belum ada ulasan."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (uiState.canWrite || !uiState.isLoggedIn) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = openWriteForm) {
                                Text("Tulis Ulasan Sekarang")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayedReviews, key = { it.id }) { review ->
                        ReviewCard(
                            review = review,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { selectedReviewDetail = review },
                            onEditClick = if (review.isMine && uiState.canWrite) {
                                {
                                    viewModel.clearFormError()
                                    editingReview = review
                                    targetCafeIdForForm = review.cafeId
                                }
                            } else null,
                            onDeleteClick = when {
                                uiState.isAdmin -> {
                                    { reviewPendingDelete = review }
                                }
                                review.isMine && uiState.canWrite -> {
                                    { viewModel.deleteMyReviewForCafe(review.cafeId) }
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
                Text(
                    "Hapus ulasan dari ${targetReview.userName} untuk ${targetReview.cafeName.ifBlank { "cafe ini" }}?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    onClick = {
                        viewModel.deleteReviewByAdmin(targetReview)
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

    if (showCafePicker) {
        AlertDialog(
            onDismissRequest = { showCafePicker = false },
            title = { Text("Pilih Cafe yang Diulas") },
            text = {
                if (uiState.cafeOptions.isEmpty()) {
                    Text("Daftar cafe belum tersedia.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.cafeOptions, key = { it.id }) { cafeOption ->
                            ListItem(
                                headlineContent = { Text(cafeOption.name) },
                                modifier = Modifier.clickable {
                                    showCafePicker = false
                                    viewModel.clearFormError()
                                    editingReview = uiState.reviews.firstOrNull {
                                        it.isMine && it.cafeId == cafeOption.id
                                    }
                                    targetCafeIdForForm = cafeOption.id
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCafePicker = false }) {
                    Text("Batal")
                }
            }
        )
    }

    targetCafeIdForForm?.let { cafeId ->
        ReviewFormDialog(
            existingReview = editingReview,
            isSaving = uiState.isSaving,
            externalErrorMessage = formError,
            onDismiss = {
                if (!uiState.isSaving) {
                    targetCafeIdForForm = null
                    editingReview = null
                }
            },
            onSubmit = { draft ->
                viewModel.upsertReviewForCafe(
                    cafeId = cafeId,
                    draft = draft,
                    onSuccess = {
                        targetCafeIdForForm = null
                        editingReview = null
                    }
                )
            }
        )
    }

    selectedReviewDetail?.let { review ->
        ReviewDetailDialog(
            review = review,
            onDismiss = { selectedReviewDetail = null }
        )
    }
}

@Composable
fun ReviewDetailDialog(
    review: Review,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = review.cafeName.ifBlank { "Detail Ulasan" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Oleh ${review.userName} • ${formatReviewDate(review.createdAt)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RatingStars(rating = review.rating)
                if (review.favoriteMenu.isNotBlank()) {
                    Text(
                        text = "Menu Favorit: ${review.favoriteMenu}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                HorizontalDivider()
                Text(
                    text = review.comment.ifBlank { "(Tidak ada komentar tertulis)" },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
