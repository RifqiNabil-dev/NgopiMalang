package com.ngopimalang.app.features.reviews

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngopimalang.app.core.components.RatingStars
import com.ngopimalang.app.core.components.ReviewCard
import com.ngopimalang.app.core.components.SectionHeader
import com.ngopimalang.app.core.model.Review
import java.util.Locale

@Composable
fun ReviewsScreen(
    onNavigateBack: (() -> Unit)? = null,
    title: String = "Ulasan Saya",
    owner: String = ReviewsViewModel.DEFAULT_OWNER,
    cafeName: String? = null,
    listComponent: String = "HorizontalPager + LazyColumn",
    flows: List<String> = listOf("Daftar Ulasan", "Detail Ulasan", "Form Ulasan"),
    viewModel: ReviewsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedReviewDetail by remember { mutableStateOf<Review?>(null) }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(cafeName, owner) {
        if (cafeName != null) {
            viewModel.ensureReviewsForCafe(cafeName)
        } else {
            viewModel.seedAllDirectoryCafesForOwner(owner)
        }
    }

    val baseReviews = remember(uiState.allReviews, owner, cafeName) {
        if (cafeName != null) {
            uiState.allReviews.filter { it.cafeName.equals(cafeName, ignoreCase = true) }
        } else {
            uiState.allReviews.filter { it.userName.equals(owner, ignoreCase = true) }
        }
    }

    val filteredReviews = remember(baseReviews, uiState.selectedRating) {
        if (uiState.selectedRating == null) {
            baseReviews
        } else {
            baseReviews.filter { it.rating == uiState.selectedRating }
        }
    }

    val averageRating = remember(baseReviews) {
        if (baseReviews.isNotEmpty()) baseReviews.map { it.rating }.average() else 0.0
    }

    Scaffold(
        topBar = {
            // Diselaraskan dengan header di Detail Cafe
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
                    text = if (cafeName != null) "Ulasan $cafeName" else title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tulis Ulasan")
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
                    Column {
                        Text(
                            text = if (cafeName != null) "Rating $cafeName" else "Ulasan oleh $owner",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f / 5.0", averageRating),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        RatingStars(rating = averageRating.toInt())
                    }
                    Text(
                        text = "${baseReviews.size} Ulasan",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            SectionHeader(title = "Filter Rating", actionLabel = "")
            RatingFilterRow(
                selectedRating = uiState.selectedRating,
                onSelectRating = { viewModel.filterByRating(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (filteredReviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (cafeName == null) {
                                "Belum ada ulasan${if (uiState.selectedRating != null) " untuk rating ini" else ""}."
                            } else {
                                "Belum ada ulasan untuk cafe ini."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { showAddDialog = true }) {
                            Text("Tulis Ulasan Sekarang")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredReviews, key = { it.id }) { review ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ReviewCard(
                                review = review,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedReviewDetail = review }
                            )
                            if (cafeName == null && review.userName.equals(owner, ignoreCase = true)) {
                                IconButton(
                                    onClick = { viewModel.deleteReview(review.id) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus Ulasan",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReviewDialog(
            fixedUserName = owner,
            fixedCafeName = cafeName,
            availableCafes = viewModel.getAvailableCafes(),
            onDismiss = { showAddDialog = false },
            onSubmit = { userName, selectedCafe, rating, comment, menu ->
                viewModel.addReview(userName, selectedCafe, rating, comment, menu)
                showAddDialog = false
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
fun CafeReviewsSection(
    cafeName: String,
    defaultRating: Double? = null,
    currentUserName: String = ReviewsViewModel.DEFAULT_OWNER,
    modifier: Modifier = Modifier,
    viewModel: ReviewsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedRating by remember { mutableStateOf<Int?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedReviewDetail by remember { mutableStateOf<Review?>(null) }

    LaunchedEffect(cafeName, defaultRating) {
        viewModel.ensureReviewsForCafe(cafeName, defaultRating)
    }

    val cafeReviews = remember(uiState.allReviews, cafeName) {
        uiState.allReviews.filter { it.cafeName.equals(cafeName, ignoreCase = true) }
    }

    val filteredCafeReviews = remember(cafeReviews, selectedRating) {
        if (selectedRating == null) {
            cafeReviews
        } else {
            cafeReviews.filter { it.rating == selectedRating }
        }
    }

    val averageRating = remember(cafeReviews, defaultRating) {
        if (cafeReviews.isNotEmpty()) {
            cafeReviews.map { it.rating }.average()
        } else {
            defaultRating ?: 0.0
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
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
            FilledTonalButton(onClick = { showAddDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tulis Ulasan")
            }
        }

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
                Column {
                    Text(
                        text = "Rata-rata Rating Cafe",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f / 5.0", averageRating),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    RatingStars(rating = averageRating.toInt())
                }
                Text(
                    text = "${cafeReviews.size} Ulasan",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        RatingFilterRow(
            selectedRating = selectedRating,
            onSelectRating = { selectedRating = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredCafeReviews.isEmpty()) {
            Text(
                text = "Belum ada ulasan dengan rating ini untuk $cafeName.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                filteredCafeReviews.forEach { review ->
                    ReviewCard(
                        review = review,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedReviewDetail = review }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddReviewDialog(
            fixedUserName = currentUserName,
            fixedCafeName = cafeName,
            availableCafes = viewModel.getAvailableCafes(),
            onDismiss = { showAddDialog = false },
            onSubmit = { userName, selectedCafe, rating, comment, menu ->
                viewModel.addReview(userName, selectedCafe, rating, comment, menu)
                showAddDialog = false
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
                    text = review.cafeName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Oleh ${review.userName} • ${review.dateLabel}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RatingStars(rating = review.rating)
                Text(
                    text = "Menu Favorit: ${review.favoriteMenu}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                HorizontalDivider()
                Text(
                    text = review.comment,
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

@Composable
private fun RatingFilterRow(
    selectedRating: Int?,
    onSelectRating: (Int?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 4.dp)
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
                selected = selectedRating == ratingValue,
                onClick = { onSelectRating(ratingValue) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun AddReviewDialog(
    fixedUserName: String = ReviewsViewModel.DEFAULT_OWNER,
    fixedCafeName: String? = null,
    availableCafes: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSubmit: (userName: String, cafeName: String, rating: Int, comment: String, menu: String) -> Unit
) {
    var cafeName by remember { mutableStateOf(fixedCafeName ?: availableCafes.firstOrNull().orEmpty()) }
    var comment by remember { mutableStateOf("") }
    var favoriteMenu by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tulis Ulasan Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fixedUserName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Nama Pengulas (Profil)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (fixedCafeName != null) {
                    OutlinedTextField(
                        value = fixedCafeName,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Nama Cafe") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = cafeName,
                        onValueChange = { cafeName = it },
                        label = { Text("Nama Cafe") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (availableCafes.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(availableCafes) { suggestion ->
                                FilterChip(
                                    selected = cafeName.equals(suggestion, ignoreCase = true),
                                    onClick = { cafeName = suggestion },
                                    label = { Text(suggestion) }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = favoriteMenu,
                    onValueChange = { favoriteMenu = it },
                    label = { Text("Menu Favorit") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Komentar / Ulasan") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Rating:")
                    Slider(
                        value = rating.toFloat(),
                        onValueChange = { rating = it.toInt() },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Text("$rating ⭐")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (comment.isNotBlank() && cafeName.isNotBlank()) {
                        onSubmit(fixedUserName, cafeName, rating, comment, favoriteMenu)
                    }
                }
            ) {
                Text("Kirim")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}