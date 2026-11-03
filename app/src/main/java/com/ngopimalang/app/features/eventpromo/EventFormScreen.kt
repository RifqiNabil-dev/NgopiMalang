package com.ngopimalang.app.features.eventpromo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngopimalang.app.core.model.EventCategory
import com.ngopimalang.app.core.model.EventPromo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    eventId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventPromoViewModel = viewModel()
) {
    val isEditMode = eventId != null
    val existingEvent = if (isEditMode) viewModel.getEventById(eventId!!) else null

    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var cafeName by remember { mutableStateOf(existingEvent?.cafeName ?: "") }
    var category by remember { mutableStateOf(existingEvent?.category ?: EventCategory.EVENT) }
    var schedule by remember { mutableStateOf(existingEvent?.schedule ?: "") }
    var description by remember { mutableStateOf(existingEvent?.description ?: "") }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Event" else "Tambah Event") },
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
            // Judul
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    errorMessage = ""
                },
                label = { Text("Judul Event/Promo") },
                placeholder = { Text("Contoh: Live Acoustic Night") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage.contains("judul", ignoreCase = true)
            )

            // Nama Cafe
            OutlinedTextField(
                value = cafeName,
                onValueChange = {
                    cafeName = it
                    errorMessage = ""
                },
                label = { Text("Nama Cafe") },
                placeholder = { Text("Contoh: Bara Coffee House") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage.contains("cafe", ignoreCase = true)
            )

            // Kategori
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Kategori",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = category == EventCategory.EVENT,
                        onClick = { category = EventCategory.EVENT },
                        label = {
                            Text(
                                text = EventCategory.EVENT.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = category == EventCategory.PROMO,
                        onClick = { category = EventCategory.PROMO },
                        label = {
                            Text(
                                text = EventCategory.PROMO.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                }
            }

            // Jadwal
            OutlinedTextField(
                value = schedule,
                onValueChange = {
                    schedule = it
                    errorMessage = ""
                },
                label = { Text("Jadwal") },
                placeholder = { Text("Contoh: Sab, 21 Des 2025 • 19.00 WIB") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = errorMessage.contains("jadwal", ignoreCase = true)
            )

            // Deskripsi
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    errorMessage = ""
                },
                label = { Text("Deskripsi") },
                placeholder = { Text("Jelaskan detail acara atau promo") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                isError = errorMessage.contains("deskripsi", ignoreCase = true)
            )

            // Error message
            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Tombol Simpan
            Button(
                onClick = {
                    val validation = validateForm(title, cafeName, schedule, description)
                    if (validation != null) {
                        errorMessage = validation
                        return@Button
                    }

                    val event = EventPromo(
                        id = eventId ?: generateEventId(),
                        title = title.trim(),
                        cafeName = cafeName.trim(),
                        category = category,
                        schedule = schedule.trim(),
                        description = description.trim()
                    )

                    if (isEditMode) {
                        viewModel.updateEvent(event)
                    } else {
                        viewModel.addEvent(event)
                    }

                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditMode) "Simpan Perubahan" else "Tambah Event")
            }
        }
    }
}

private fun validateForm(
    title: String,
    cafeName: String,
    schedule: String,
    description: String
): String? {
    return when {
        title.isBlank() -> "Judul event wajib diisi"
        cafeName.isBlank() -> "Nama cafe wajib diisi"
        schedule.isBlank() -> "Jadwal wajib diisi"
        description.isBlank() -> "Deskripsi wajib diisi"
        else -> null
    }
}

private fun generateEventId(): String {
    return "e${System.currentTimeMillis()}"
}
