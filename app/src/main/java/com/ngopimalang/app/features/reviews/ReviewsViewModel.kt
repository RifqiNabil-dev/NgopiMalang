package com.ngopimalang.app.features.reviews

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    onNavigateBack: () -> Unit = {},
    title: String = "Ulasan & Rating",
    owner: String = "Shelfina Khayla Anindita",
    listComponent: String = "HorizontalPager + LazyColumn",
    flows: List<String> = listOf("Daftar Ulasan", "Detail Ulasan", "Form Ulasan"),
    viewModel: ReviewsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) }
            )
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
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                            text = "Rata-rata Rating",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = String.format("%.1f / 5.0", uiState.averageRating),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        RatingStars(rating = uiState.averageRating.toInt())
                    }
                    Text(
                        text = "${uiState.allReviews.size} Ulasan",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
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
                        selected = uiState.selectedRating == ratingValue,
                        onClick = { viewModel.filterByRating(ratingValue) },
                        label = { Text(label) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (uiState.filteredReviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada ulasan untuk rating ini.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.filteredReviews, key = { it.id }) { review ->
                        ReviewCard(
                            review = review,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReviewDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { userName, cafeName, rating, comment, menu ->
                viewModel.addReview(userName, cafeName, rating, comment, menu)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddReviewDialog(
    onDismiss: () -> Unit,
    onSubmit: (userName: String, cafeName: String, rating: Int, comment: String, menu: String) -> Unit
) {
    var userName by remember { mutableStateOf("") }
    var cafeName by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var favoriteMenu by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tulis Ulasan Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Nama Anda") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cafeName,
                    onValueChange = { cafeName = it },
                    label = { Text("Nama Cafe") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                    if (comment.isNotBlank()) {
                        onSubmit(userName, cafeName, rating, comment, favoriteMenu)
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