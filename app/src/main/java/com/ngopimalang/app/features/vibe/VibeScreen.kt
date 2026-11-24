package com.ngopimalang.app.features.vibe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngopimalang.app.core.components.CafeCard
import com.ngopimalang.app.core.model.Cafe
import com.ngopimalang.app.core.model.Vibe
import kotlinx.coroutines.launch

@Composable
fun VibeScreen(
    onCafeClick: (String) -> Unit,
    onVibeDetail: (String) -> Unit,
    onAddVibe: () -> Unit,
    onEditVibe: (String) -> Unit,
    isAdmin: Boolean,
    vibeViewModel: VibeViewModel = viewModel()
) {
    val state by vibeViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<Vibe?>(null) }

    LaunchedEffect(Unit) { vibeViewModel.load() }
    LaunchedEffect(isAdmin) { vibeViewModel.setAdmin(isAdmin) }

    val pagerState = rememberPagerState(
        initialPage = state.selectedIndex.coerceIn(0, (state.vibes.size - 1).coerceAtLeast(0)),
        pageCount = { state.vibes.size.coerceAtLeast(0) }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { vibeViewModel.onVibeSelected(it) }
    }

    // Muat cafe untuk tab yang aktif saja (bukan di dalam items).
    LaunchedEffect(pagerState.currentPage, state.vibes, state.cafesByVibe.keys) {
        state.vibes.getOrNull(pagerState.currentPage)?.let { vibeViewModel.loadCafesFor(it.id) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.vibes.isEmpty() && state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            state.vibes.isEmpty() && state.errorMessage != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = vibeViewModel::onRetry) { Text("Coba lagi") }
                }
            }

            state.vibes.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Belum ada vibe.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAdmin) Button(onClick = onAddVibe) { Text("Tambah vibe") }
                }
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (state.errorMessage != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                state.errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = vibeViewModel::onRetry) { Text("Coba lagi") }
                        }
                    }

                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage.coerceIn(0, state.vibes.lastIndex)
                    ) {
                        state.vibes.forEachIndexed { index, vibe ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = { Text("${vibe.name} (${vibe.cafeCount})") },
                                icon = { Icon(vibeIcon(vibe.iconKey), contentDescription = null) }
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        key = { page -> state.vibes.getOrNull(page)?.id ?: page }
                    ) { page ->
                        val vibe = state.vibes.getOrNull(page)
                        if (vibe != null) {
                            VibePage(
                                vibe = vibe,
                                cafes = state.cafesByVibe[vibe.id],
                                isAdmin = isAdmin,
                                onCafeClick = onCafeClick,
                                onVibeDetail = { onVibeDetail(vibe.id) },
                                onEdit = { onEditVibe(vibe.id) },
                                onDelete = { deleteTarget = vibe }
                            )
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            FloatingActionButton(
                onClick = onAddVibe,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah vibe")
            }
        }
    }

    deleteTarget?.let { vibe ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Hapus vibe?") },
            text = {
                Text(
                    "Vibe \"${vibe.name}\" akan dihapus. ${vibe.cafeCount} cafe akan " +
                        "kehilangan label vibe ini. Cafe-nya tidak ikut terhapus."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vibeViewModel.onDeleteVibe(vibe.id)
                    deleteTarget = null
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun VibePage(
    vibe: Vibe,
    cafes: List<Cafe>?, // null = belum selesai dimuat
    isAdmin: Boolean,
    onCafeClick: (String) -> Unit,
    onVibeDetail: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = vibe.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${vibe.cafeCount} cafe",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onVibeDetail) { Text("Detail vibe") }
                if (isAdmin) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Ubah vibe")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus vibe")
                    }
                }
            }
        }

        when {
            cafes == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            cafes.isEmpty() -> Text(
                text = "Belum ada cafe di vibe ini",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cafes, key = { it.id }) { cafe ->
                    CafeCard(
                        cafe = cafe,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onCafeClick(cafe.id) }
                    )
                }
            }
        }
    }
}