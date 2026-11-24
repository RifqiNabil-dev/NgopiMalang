package com.ngopimalang.app.features.eventpromo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.ngopimalang.app.core.model.EventCategory

/**
 * Form tambah/edit event. Seluruh input ada di [EventFormViewModel] sehingga rotasi
 * layar tidak menghapus isian (state hoisting, dok 08 bagian 5).
 * Tombol Hapus hanya muncul di mode edit untuk admin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    eventId: String?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    isAdmin: Boolean = false,
    viewModel: EventFormViewModel = viewModel()
) {
    val state by viewModel.formState.collectAsStateWithLifecycle()

    LaunchedEffect(eventId) { viewModel.loadForEdit(eventId) }

    // Layar keluar setelah simpan/hapus berhasil; daftar induk memuat ulang datanya.
    LaunchedEffect(state.done) {
        if (state.done) onSaved()
    }

    var cafeMenuExpanded by remember { mutableStateOf(false) }
    var deleteDialogVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Event" else "Tambah Event") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Judul Event/Promo") },
                placeholder = { Text("Contoh: Live Acoustic Night") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.validationErrors.containsKey("title"),
                supportingText = {
                    state.validationErrors["title"]?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Kategori", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EventCategory.entries.forEach { category ->
                        FilterChip(
                            selected = state.category == category,
                            onClick = { viewModel.onCategoryChange(category) },
                            label = { Text(category.label) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.startDate,
                    onValueChange = viewModel::onStartDateChange,
                    label = { Text("Tanggal mulai") },
                    placeholder = { Text("2026-01-01") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.startTime,
                    onValueChange = viewModel::onStartTimeChange,
                    label = { Text("Jam mulai") },
                    placeholder = { Text("19:00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.endDate,
                    onValueChange = viewModel::onEndDateChange,
                    label = { Text("Tanggal selesai") },
                    placeholder = { Text("2026-01-01") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.endTime,
                    onValueChange = viewModel::onEndTimeChange,
                    label = { Text("Jam selesai") },
                    placeholder = { Text("22:00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            state.validationErrors["time"]?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column {
                OutlinedTextField(
                    value = state.cafeName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Cafe terkait (opsional)") },
                    placeholder = { Text("Pilih cafe") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { cafeMenuExpanded = true }) { Text("Pilih") }
                    }
                )
                DropdownMenu(
                    expanded = cafeMenuExpanded,
                    onDismissRequest = { cafeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tanpa cafe") },
                        onClick = {
                            viewModel.onCafeChange(null, "")
                            cafeMenuExpanded = false
                        }
                    )
                    state.availableCafes.forEach { cafe ->
                        DropdownMenuItem(
                            text = { Text(cafe.name) },
                            onClick = {
                                viewModel.onCafeChange(cafe.id, cafe.name)
                                cafeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.priceMinText,
                    onValueChange = viewModel::onPriceMinChange,
                    label = { Text("Harga minimum") },
                    placeholder = { Text("15000") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.priceMaxText,
                    onValueChange = viewModel::onPriceMaxChange,
                    label = { Text("Harga maksimum") },
                    placeholder = { Text("35000") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            state.validationErrors["price"]?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Deskripsi") },
                placeholder = { Text("Jelaskan detail acara atau promo") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                isError = state.validationErrors.containsKey("description"),
                supportingText = {
                    state.validationErrors["description"]?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Terbitkan", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Jika mati, event hanya terlihat admin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = state.isPublished, onCheckedChange = viewModel::onPublishChange)
            }

            state.errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = viewModel::onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isSaving) "Menyimpan..."
                    else if (state.isEditing) "Simpan Perubahan" else "Tambah Event"
                )
            }

            if (isAdmin && state.isEditing) {
                TextButton(
                    onClick = { deleteDialogVisible = true },
                    enabled = !state.isSaving,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text(
                        text = "Hapus Event",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }

    if (deleteDialogVisible) {
        AlertDialog(
            onDismissRequest = { deleteDialogVisible = false },
            title = { Text("Hapus Event") },
            text = { Text("Event ini akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteDialogVisible = false
                    viewModel.onDelete()
                }) { Text("Hapus", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialogVisible = false }) { Text("Batal") }
            }
        )
    }
}