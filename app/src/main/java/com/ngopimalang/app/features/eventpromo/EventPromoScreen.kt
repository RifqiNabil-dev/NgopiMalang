package com.ngopimalang.app.features.eventpromo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngopimalang.app.core.components.EventCard
import com.ngopimalang.app.core.model.EventCategory

/**
 * Daftar Event & Promo. Sumber data dari Supabase lewat [EventPromoViewModel].
 * Lima elemen wajib dok 10 bagian 7.1: indikator muat, empty state,
 * kesalahan + coba lagi, umpan balik simpan, dan daftar berat-key.
 */
@Composable
fun EventPromoScreen(
    onEventClick: (String) -> Unit,
    onAddEvent: () -> Unit,
    modifier: Modifier = Modifier,
    isAdmin: Boolean = false,
    viewModel: EventPromoViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(isAdmin) {
        viewModel.setAdmin(isAdmin)
        viewModel.ensureLoaded()
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Event & Promo",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
            )

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Cari judul atau nama cafe") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                singleLine = true
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedCategory == null,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text("Semua") },
                        shape = CircleShape,
                        colors = filterChipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedCategory == EventCategory.EVENT,
                        onClick = { viewModel.onCategorySelected(EventCategory.EVENT) },
                        label = { Text(EventCategory.EVENT.label) },
                        shape = CircleShape,
                        colors = filterChipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = state.selectedCategory == EventCategory.PROMO,
                        onClick = { viewModel.onCategorySelected(EventCategory.PROMO) },
                        label = { Text(EventCategory.PROMO.label) },
                        shape = CircleShape,
                        colors = filterChipColors()
                    )
                }
                item {
                    FilterChip(
                        selected = state.onlyUpcoming,
                        onClick = { viewModel.onOnlyUpcomingChange(!state.onlyUpcoming) },
                        label = { Text("Belum selesai") },
                        shape = CircleShape
                    )
                }
            }

            when {
                state.isLoading -> BoxLoading()
                state.errorMessage != null ->
                    ErrorState(message = state.errorMessage!!, onRetry = viewModel::onRetry)
                state.isEmpty -> EmptyState(hasFilter = state.query.isNotBlank() || state.selectedCategory != null)
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.visibleEvents, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onEventClick(event.id) }
                        )
                    }
                }
            }
        }
        if (isAdmin) {
            FloatingActionButton(
                onClick = onAddEvent,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 20.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Tambah Event",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun filterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
)

@Composable
private fun BoxLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun EmptyState(hasFilter: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (hasFilter) "Tidak ada event yang cocok" else "Belum ada event atau promo",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (hasFilter) "Coba kata kunci atau filter lain"
            else "Event akan muncul di sini setelah ditambahkan",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(onClick = onRetry) { Text("Coba lagi") }
    }
}