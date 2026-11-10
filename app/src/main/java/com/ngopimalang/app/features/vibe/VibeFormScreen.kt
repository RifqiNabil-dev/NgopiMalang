package com.ngopimalang.app.features.vibe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * vibeId == null  -> mode tambah
 * vibeId != null  -> mode ubah (field terisi dari vibe yang ada)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeFormScreen(
    vibeId: String?,
    onBack: () -> Unit,
    vibeViewModel: VibeViewModel = viewModel()
) {
    val state by vibeViewModel.uiState.collectAsStateWithLifecycle()
    val editing = state.vibeById(vibeId)

    var name by remember(vibeId) { mutableStateOf(editing?.name.orEmpty()) }
    var description by remember(vibeId) { mutableStateOf(editing?.description.orEmpty()) }
    var selectedCafes by remember(vibeId) {
        mutableStateOf(if (vibeId != null) state.cafeIdsByVibe[vibeId].orEmpty() else emptySet())
    }

    val canSave = name.isNotBlank() && description.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editing == null) "Tambah Vibe" else "Ubah Vibe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Vibe") },
                placeholder = { Text("Contoh: Nugas / WFC") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi Vibe") },
                placeholder = { Text("Jelaskan suasana yang dicari") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Pilih Cafe",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(state.allCafes, key = { it.id }) { cafe ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = cafe.id in selectedCafes,
                            onCheckedChange = { checked ->
                                selectedCafes = if (checked) {
                                    selectedCafes + cafe.id
                                } else {
                                    selectedCafes - cafe.id
                                }
                            }
                        )
                        Text(
                            text = cafe.name,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onBack) {
                    Text("Batal")
                }

                Button(
                    onClick = {
                        if (editing == null) {
                            vibeViewModel.addVibe(name.trim(), description.trim(), selectedCafes.toList())
                        } else {
                            vibeViewModel.updateVibe(
                                editing.id, name.trim(), description.trim(), selectedCafes.toList()
                            )
                        }
                        onBack()
                    },
                    enabled = canSave
                ) {
                    Text("Simpan")
                }
            }
        }
    }
}