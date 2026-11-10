package com.ngopimalang.app.features.directory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ngopimalang.app.core.components.CafeCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DirectoryScreen(
    state: DirectoryUiState,
    onSearchChange: (String) -> Unit,
    onToggleWifi: () -> Unit,
    onTogglePlugs: () -> Unit,
    onResetFilters: () -> Unit,
    onCafeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Direktori Cafe", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Temukan tempat ngopi yang cocok di Malang.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = onSearchChange,
                label = { Text("Cari nama atau alamat cafe") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        TextButton(onClick = { onSearchChange("") }) { Text("Hapus") }
                    }
                }
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.wifiOnly, onClick = onToggleWifi, label = { Text("Ada Wi-Fi") })
                FilterChip(selected = state.plugsOnly, onClick = onTogglePlugs, label = { Text("Ada colokan") })
            }
            Text("${state.cafes.size} cafe ditemukan", style = MaterialTheme.typography.labelLarge)
        }
        if (state.cafes.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Text("Belum ada cafe yang cocok", style = MaterialTheme.typography.titleMedium)
                Text("Coba kata kunci lain atau hapus filter.")
                OutlinedButton(onClick = onResetFilters) { Text("Reset pencarian & filter") }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.cafes, key = { it.id }) { cafe ->
                    CafeCard(cafe = cafe, modifier = Modifier.fillMaxWidth(), onClick = { onCafeClick(cafe.id) })
                }
            }
        }
    }
}
