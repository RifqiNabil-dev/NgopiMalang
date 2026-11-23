
package com.ngopimalang.app.features.bookmark

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngopimalang.app.core.components.CafeCard
import com.ngopimalang.app.core.components.SectionHeader

@Composable
fun BookmarkScreen(
    viewModel: BookmarkViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var editingItem by remember {
        mutableStateOf<BookmarkItem?>(null)
    }

    var deletingItem by remember {
        mutableStateOf<BookmarkItem?>(null)
    }

    var selectedItem by remember {
        mutableStateOf<BookmarkItem?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp
            )
    ) {

        // HEADER
        Text(
            text = "Saved Spot",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Tempat ngopi favoritmu, semua tersimpan di sini.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // KONDISI SAAT BOOKMARK KOSONG
        if (state.bookmarks.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "♡",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Belum ada Saved Spot",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Yuk, simpan cafe favoritmu supaya gampang ditemukan lagi!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

        } else {

            // JUDUL DAFTAR
            SectionHeader(
                title = "Tersimpan (${state.bookmarks.size})"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // DAFTAR BOOKMARK
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {

                items(
                    items = state.bookmarks,
                    key = { it.id }
                ) { item ->

                    val cafe = state.cafes.firstOrNull {
                        it.id == item.cafeId
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {

                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            // KARTU CAFE DARI KOMPONEN BERSAMA
                            if (cafe != null) {

                                CafeCard(
                                    cafe = cafe,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        selectedItem = item
                                    }
                                )

                            } else {

                                Text(
                                    text = item.cafeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // WAKTU DISIMPAN
                            Text(
                                text = item.savedAtLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            // CATATAN PRIBADI
                            Text(
                                text = "Catatan pribadi",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = item.note.ifBlank {
                                    "Belum ada catatan pribadi"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // TOMBOL AKSI
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                TextButton(
                                    onClick = {
                                        editingItem = item
                                    }
                                ) {
                                    Text("Edit Catatan")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                TextButton(
                                    onClick = {
                                        deletingItem = item
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Hapus")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG EDIT CATATAN
    editingItem?.let { item ->

        var note by remember(item.id) {
            mutableStateOf(item.note)
        }

        AlertDialog(
            onDismissRequest = {
                editingItem = null
            },
            title = {
                Text("Edit Catatan")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = item.cafeName,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            note = it
                        },
                        label = {
                            Text("Catatan pribadi")
                        },
                        placeholder = {
                            Text("Contoh: cocok buat nugas sore")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateNote(
                            id = item.id,
                            note = note
                        )
                        editingItem = null
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingItem = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // DIALOG KONFIRMASI HAPUS
    deletingItem?.let { item ->

        AlertDialog(
            onDismissRequest = {
                deletingItem = null
            },
            title = {
                Text("Hapus Saved Spot?")
            },
            text = {
                Text(
                    "Yakin ingin menghapus ${item.cafeName} dari daftar Saved Spot?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeBookmark(item.id)
                        deletingItem = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deletingItem = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // DIALOG DETAIL BOOKMARK SEMENTARA
    selectedItem?.let { item ->

        AlertDialog(
            onDismissRequest = {
                selectedItem = null
            },
            title = {
                Text(item.cafeName)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Text(
                        text = "Catatan pribadi",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = item.note.ifBlank {
                            "Belum ada catatan pribadi"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = item.savedAtLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedItem = null
                    }
                ) {
                    Text("Tutup")
                }
            }
        )
    }
}
