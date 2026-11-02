package com.ngopimalang.app.features.vibe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngopimalang.app.core.model.Cafe

@Composable
fun VibeScreen(
    onCafeClick: (Cafe) -> Unit,
    onVibeDetail: (String) -> Unit,
    onAddVibe: () -> Unit,
    viewModel: VibeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val selectedVibe = state.vibes.getOrNull(state.selectedIndex)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddVibe
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Vibe"
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Text(
                text = "Temukan Cafe Sesuai Vibe Kamu",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 20.dp,
                    bottom = 12.dp
                )
            )

            if (state.vibes.isNotEmpty()) {

                ScrollableTabRow(
                    selectedTabIndex = state.selectedIndex
                ) {
                    state.vibes.forEachIndexed { index, vibe ->

                        Tab(
                            selected = state.selectedIndex == index,
                            onClick = {
                                viewModel.onVibeSelected(index)
                            },
                            text = {
                                Text(vibe.name)
                            }
                        )
                    }
                }

                selectedVibe?.let { vibe ->

                    val cafes = state.cafesByVibe[vibe.id].orEmpty()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = vibe.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                TextButton(
                                    onClick = {
                                        onVibeDetail(vibe.id)
                                    }
                                ) {
                                    Text("Lihat Detail Vibe")
                                }

                                Text(
                                    text = "Cafe Rekomendasi",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        if (cafes.isEmpty()) {
                            item {
                                Text("Belum ada cafe untuk kategori ini.")
                            }
                        } else {
                            items(
                                items = cafes,
                                key = { it.id }
                            ) { cafe ->

                                CafeItem(
                                    cafe = cafe,
                                    onClick = {
                                        onCafeClick(cafe)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CafeItem(
    cafe: Cafe,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = cafe.name,
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null
                )

                Text(
                    text = cafe.address,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = cafe.priceRange,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Rating: ${cafe.rating} | WiFi: ${
                    if (cafe.hasWifi) "Tersedia" else "Tidak tersedia"
                }",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Colokan: ${cafe.plugCount}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}