package com.ngopimalang.app.features.bookmark

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngopimalang.app.core.components.CafeCard
import com.ngopimalang.app.core.components.SectionHeader
import com.ngopimalang.app.core.model.Cafe
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BookmarkScreen(
    viewModel: BookmarkViewModel = viewModel(),
    onCafeClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val editingItem = state.items.firstOrNull {
        it.cafeId == state.editingCafeId
    }

    val deletingItem = state.items.firstOrNull {
        it.cafeId == state.deletingCafeId
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
    ) {

        // HEADER
        Text(
            text = "Saved Spot",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Tempat ngopi favoritmu, semua tersimpan di sini.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // LOADING STATE
        if (state.isLoading) {

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        // ERROR STATE
        state.errorMessage?.let { message ->

            if (editingItem == null && deletingItem == null) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Text(
                            text = message,
                            color =
                                MaterialTheme.colorScheme.onErrorContainer,
                            style =
                                MaterialTheme.typography.bodyMedium
                        )

                        TextButton(
                            onClick = viewModel::onRetry,
                            enabled = !state.isLoading &&
                                    !state.isSaving
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // EMPTY STATE
        if (
            state.items.isEmpty() &&
            !state.isLoading &&
            state.errorMessage == null
        ) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally,
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "♡",
                        style =
                            MaterialTheme.typography.displayLarge,
                        color =
                            MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Belum ada Saved Spot",
                        style =
                            MaterialTheme.typography.titleLarge,
                        color =
                            MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Yuk, simpan cafe favoritmu dari halaman Direktori!",
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

        } else if (state.items.isNotEmpty()) {

            // JUMLAH BOOKMARK
            SectionHeader(
                title = "Tersimpan (${state.items.size})"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // DAFTAR BOOKMARK
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp),
                contentPadding =
                    PaddingValues(bottom = 24.dp)
            ) {

                items(
                    items = state.items,
                    key = { it.id }
                ) { item ->

                    // DATA CAFE DARI REPOSITORY
                    val cafe = Cafe(
                        id = item.cafeId,
                        name = item.cafeName,
                        address = item.address,
                        priceRange = formatBookmarkPrice(
                            item.priceMin,
                            item.priceMax
                        ),
                        hasWifi = item.hasWifi,
                        plugCount = item.plugCount,
                        rating = item.rating,
                        vibes = item.vibes,
                        priceMin = item.priceMin,
                        priceMax = item.priceMax,
                        photoPath = item.photoPath,
                        photoUrl = item.photoUrl,
                        reviewCount = item.reviewCount,
                        isActive = item.isActive
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surface
                        ),
                        border =
                            CardDefaults.outlinedCardBorder()
                    ) {

                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            // KARTU CAFE
                            CafeCard(
                                cafe = cafe,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onCafeClick(item.cafeId)
                                }
                            )

                            // WAKTU DISIMPAN
                            Text(
                                text = formatBookmarkDate(
                                    item.savedAt
                                ),
                                style =
                                    MaterialTheme.typography.labelSmall,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(
                                color =
                                    MaterialTheme.colorScheme.outlineVariant
                            )

                            // CATATAN PRIBADI
                            Text(
                                text = "Catatan pribadi",
                                style =
                                    MaterialTheme.typography.labelMedium,
                                color =
                                    MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = item.note.ifBlank {
                                    "Belum ada catatan pribadi"
                                },
                                style =
                                    MaterialTheme.typography.bodyMedium,
                                color =
                                    MaterialTheme.colorScheme.onSurface
                            )

                            // TOMBOL AKSI
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.End,
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                // EDIT CATATAN
                                TextButton(
                                    enabled = !state.isSaving &&
                                            !state.isLoading,
                                    onClick = {
                                        viewModel.onEditNoteClick(
                                            item.cafeId
                                        )
                                    }
                                ) {
                                    Text("Edit Catatan")
                                }

                                Spacer(
                                    modifier = Modifier.width(8.dp)
                                )

                                // HAPUS BOOKMARK
                                TextButton(
                                    enabled = !state.isSaving &&
                                            !state.isLoading,
                                    onClick = {
                                        viewModel.onRemoveClick(
                                            item.cafeId
                                        )
                                    },
                                    colors =
                                        ButtonDefaults.textButtonColors(
                                            contentColor =
                                                MaterialTheme.colorScheme.error
                                        )
                                ) {
                                    Text("Hapus")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG EDIT CATATAN
    editingItem?.let { item ->

        AlertDialog(
            onDismissRequest = {
                viewModel.onEditNoteDismiss()
            },

            title = {
                Text("Edit Catatan")
            },

            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = item.cafeName,
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = state.noteDraft,
                        onValueChange = viewModel::onNoteChange,
                        enabled = !state.isSaving,
                        label = {
                            Text("Catatan pribadi")
                        },
                        placeholder = {
                            Text("Contoh: cocok buat nugas sore")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    // ERROR EDIT CATATAN
                    state.errorMessage?.let { message ->

                        Text(
                            text = message,
                            color =
                                MaterialTheme.colorScheme.error,
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }

                    if (state.isSaving) {

                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(
                    enabled = !state.isSaving &&
                            !state.isLoading,
                    onClick = viewModel::onNoteSave
                ) {

                    Text(
                        if (state.isSaving) {
                            "Menyimpan..."
                        } else {
                            "Simpan"
                        }
                    )
                }
            },

            dismissButton = {

                TextButton(
                    enabled = !state.isSaving,
                    onClick = viewModel::onEditNoteDismiss
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // DIALOG KONFIRMASI HAPUS
    deletingItem?.let { item ->

        AlertDialog(
            onDismissRequest = {
                viewModel.onRemoveDismiss()
            },

            title = {
                Text("Hapus Saved Spot?")
            },

            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        "Yakin ingin menghapus ${item.cafeName} dari daftar Saved Spot?"
                    )

                    // ERROR HAPUS BOOKMARK
                    state.errorMessage?.let { message ->

                        Text(
                            text = message,
                            color =
                                MaterialTheme.colorScheme.error,
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }

                    if (state.isSaving) {

                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },

            confirmButton = {

                TextButton(
                    enabled = !state.isSaving &&
                            !state.isLoading,
                    onClick = viewModel::onRemoveConfirm,
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor =
                                MaterialTheme.colorScheme.error
                        )
                ) {

                    Text(
                        if (state.isSaving) {
                            "Menghapus..."
                        } else {
                            "Hapus"
                        }
                    )
                }
            },

            dismissButton = {

                TextButton(
                    enabled = !state.isSaving,
                    onClick = viewModel::onRemoveDismiss
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

// FORMAT HARGA CAFE
private fun formatBookmarkPrice(
    min: Int?,
    max: Int?
): String {

    val formatter =
        NumberFormat.getIntegerInstance(
            Locale("id", "ID")
        )

    fun rupiah(value: Int): String {
        return "Rp${formatter.format(value)}"
    }

    return when {

        min != null && max != null ->
            "${rupiah(min)} - ${rupiah(max)}"

        min != null ->
            "Mulai ${rupiah(min)}"

        max != null ->
            "Hingga ${rupiah(max)}"

        else ->
            "Harga belum tersedia"
    }
}

// FORMAT WAKTU PENYIMPANAN
private fun formatBookmarkDate(
    savedAt: Instant
): String {

    val diffMillis = (
            System.currentTimeMillis() -
                    savedAt.toEpochMilliseconds()
            ).coerceAtLeast(0L)

    val minutes = diffMillis / 60_000L
    val hours = minutes / 60L
    val days = hours / 24L

    return when {

        minutes < 1 ->
            "Baru disimpan"

        minutes < 60 ->
            "$minutes menit lalu"

        hours < 24 ->
            "$hours jam lalu"

        days < 7 ->
            "$days hari lalu"

        else -> {

            val date = savedAt.toLocalDateTime(
                TimeZone.currentSystemDefault()
            ).date

            val months = listOf(
                "Jan", "Feb", "Mar", "Apr",
                "Mei", "Jun", "Jul", "Agu",
                "Sep", "Okt", "Nov", "Des"
            )

            "${date.dayOfMonth} " +
                    "${months[date.monthNumber - 1]} " +
                    "${date.year}"
        }
    }
}
