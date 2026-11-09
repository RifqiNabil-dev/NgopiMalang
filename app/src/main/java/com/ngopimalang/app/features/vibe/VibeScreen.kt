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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
    onCafeClick: (Cafe) -> Unit,
    onVibeDetail: (String) -> Unit,
    onAddVibe: () -> Unit,
    vibeViewModel: VibeViewModel = viewModel()
) {
    val state by vibeViewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = state.selectedIndex.coerceIn(0, (state.vibes.size - 1).coerceAtLeast(0)),
        pageCount = { state.vibes.size }
    )

    // Pager digeser -> simpan halaman aktif ke ViewModel.
    // Arah sebaliknya (tab diklik -> pager pindah) ada di onClick Tab.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            vibeViewModel.onVibeSelected(page)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.vibes.isEmpty()) {
            Text(
                text = "Belum ada vibe. Tekan + untuk menambah.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage.coerceIn(0, state.vibes.lastIndex)
                ) {
                    state.vibes.forEachIndexed { index, vibe ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = { Text(text = vibe.name) }
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val vibe = state.vibes.getOrNull(page)
                    if (vibe != null) {
                        VibePage(
                            vibe = vibe,
                            cafes = state.cafesFor(vibe.id),
                            onCafeClick = onCafeClick,
                            onVibeDetail = { onVibeDetail(vibe.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onAddVibe,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah vibe")
        }
    }
}

@Composable
private fun VibePage(
    vibe: Vibe,
    cafes: List<Cafe>,
    onCafeClick: (Cafe) -> Unit,
    onVibeDetail: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                text = "${cafes.size} cafe",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            TextButton(onClick = onVibeDetail) {
                Text("Detail vibe")
            }
        }

        if (cafes.isEmpty()) {
            Text(
                text = "Belum ada cafe di vibe ini",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp), // ruang untuk FAB
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cafes, key = { it.id }) { cafe ->
                    CafeCard(
                        cafe = cafe,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onCafeClick(cafe) }
                    )
                }
            }
        }
    }
}