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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** vibeId == null -> tambah, selain itu -> ubah. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeFormScreen(
    vibeId: String?,
    onBack: () -> Unit,
    viewModel: VibeFormViewModel = viewModel()
) {
    val state by viewModel.formState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(vibeId) { viewModel.loadForEdit(vibeId) }
    LaunchedEffect(state.done) { if (state.done) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vibeId == null) "Tambah Vibe" else "Ubah Vibe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Nama Vibe") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.validationError != null
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Deskripsi Vibe") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(Modifier.height(12.dp))

            Text("Ikon", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickIconKeys) { key ->
                    FilterChip(
                        selected = state.iconKey.trim().lowercase() == key,
                        onClick = { viewModel.onIconChange(key) },
                        label = { Text(key) }
                    )
                }
            }
            OutlinedTextField(
                value = state.iconKey,
                onValueChange = viewModel::onIconChange,
                label = { Text("Kunci ikon lain (kosong = work)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            state.validationError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(12.dp))
            Text("Pilih Cafe", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(state.availableCafes, key = { it.id }) { cafe ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = cafe.id in state.selectedCafeIds,
                            onCheckedChange = { viewModel.onToggleCafe(cafe.id) }
                        )
                        Text(cafe.name, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (vibeId != null) {
                    TextButton(onClick = { showDeleteDialog = true }, enabled = !state.isSaving) {
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Spacer(Modifier.height(1.dp))
                }
                Row {
                    TextButton(onClick = onBack) { Text("Batal") }
                    Button(
                        onClick = viewModel::onSave,
                        enabled = state.name.isNotBlank() && !state.isSaving
                    ) { Text(if (state.isSaving) "Menyimpan..." else "Simpan") }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus vibe?") },
            text = {
                Text(
                    "Vibe \"${state.name}\" akan dihapus. ${state.cafeCount} cafe akan " +
                        "kehilangan label vibe ini. Cafe-nya tidak ikut terhapus."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.onDelete()
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Batal") } }
        )
    }
}