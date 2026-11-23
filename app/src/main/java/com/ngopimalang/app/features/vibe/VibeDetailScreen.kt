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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngopimalang.app.core.components.CafeCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeDetailScreen(
    vibeId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onCafeClick: (String) -> Unit,
    isAdmin: Boolean = false,
    viewModel: VibeDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(vibeId) { viewModel.load(vibeId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Vibe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    if (isAdmin && state.vibe != null) {
                        IconButton(onClick = { onEdit(vibeId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Ubah vibe")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val vibe = state.vibe
        when {
            state.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }

            state.notFound -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { Text("Vibe tidak ditemukan") }

            state.errorMessage != null -> Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::onRetry) { Text("Coba lagi") }
            }

            vibe != null -> Column(
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
                Spacer(Modifier.height(8.dp))
                Text(
                    text = vibe.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Rekomendasi Cafe (${vibe.cafeCount})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                if (state.cafes.isEmpty()) {
                    Text(
                        text = "Belum ada cafe di vibe ini",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.cafes, key = { it.id }) { cafe ->
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
    }
}