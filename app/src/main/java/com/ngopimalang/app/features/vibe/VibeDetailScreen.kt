package com.ngopimalang.app.features.vibe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngopimalang.app.core.components.CafeCard
import com.ngopimalang.app.core.model.Cafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeDetailScreen(
    vibeId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCafeClick: (Cafe) -> Unit,
    vibeViewModel: VibeViewModel = viewModel()
) {
    val state by vibeViewModel.uiState.collectAsStateWithLifecycle()
    val vibe = state.vibeById(vibeId)
    val cafes = if (vibe != null) state.cafesFor(vibe.id) else emptyList()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Vibe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    if (vibe != null) {
                        IconButton(onClick = { onEdit(vibe.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Ubah vibe")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus vibe")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (vibe == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Vibe tidak ditemukan")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = vibe.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = vibe.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Rekomendasi Cafe (${cafes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (cafes.isEmpty()) {
                    Text(
                        text = "Belum ada cafe di vibe ini",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(cafes, key = { it.id }) { cafe ->
                            Column {
                                CafeCard(
                                    cafe = cafe,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onCafeClick(cafe) }
                                )
                                TextButton(
                                    onClick = { vibeViewModel.toggleCafe(vibe.id, cafe.id) }
                                ) {
                                    Text("Lepas dari vibe ini")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && vibe != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus vibe?") },
            text = { Text("Vibe \"${vibe.name}\" akan dihapus. Cafe-nya tidak ikut terhapus.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        vibeViewModel.deleteVibe(vibe.id)
                        onBack()
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}