package com.ngopimalang.app.features.bookmark

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ngopimalang.app.core.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookmarkActionUiState(
    val cafeId: String = "",
    val isBookmarked: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val requiresLogin: Boolean = false,
    val successMessage: String? = null
)

class BookmarkActionViewModel(
    private val repo: BookmarkRepository = BookmarkRepository()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(BookmarkActionUiState())

    val uiState: StateFlow<BookmarkActionUiState> =
        _uiState.asStateFlow()

    // Memeriksa status bookmark cafe
    fun load(cafeId: String) {

        if (_uiState.value.isSaving) return

        _uiState.value = BookmarkActionUiState(
            cafeId = cafeId,
            isLoading = true
        )

        viewModelScope.launch {
            try {
                if (!repo.isLoggedIn()) {

                    _uiState.update {
                        it.copy(
                            requiresLogin = true
                        )
                    }

                    return@launch
                }

                val bookmarked =
                    repo.isBookmarked(cafeId)

                _uiState.update {
                    it.copy(
                        isBookmarked = bookmarked,
                        requiresLogin = false
                    )
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.toUserMessage()
                    )
                }

            } finally {
                _uiState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    // Menyimpan atau menghapus cafe
    fun onToggle() {

        val state = _uiState.value

        if (state.isLoading ||
            state.isSaving ||
            state.cafeId.isBlank()
        ) return

        if (!repo.isLoggedIn()) {

            _uiState.update {
                it.copy(
                    requiresLogin = true,
                    errorMessage = "Silakan login terlebih dahulu"
                )
            }

            return
        }

        _uiState.update {
            it.copy(
                isSaving = true,
                requiresLogin = false,
                errorMessage = null,
                successMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val bookmarked =
                    repo.toggle(state.cafeId)

                _uiState.update {
                    it.copy(
                        isBookmarked = bookmarked,
                        successMessage =
                            if (bookmarked) {
                                "Disimpan ke Saved Spot"
                            } else {
                                "Dihapus dari Saved Spot"
                            }
                    )
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = e.toUserMessage()
                    )
                }

            } finally {
                _uiState.update {
                    it.copy(isSaving = false)
                }
            }
        }
    }
}

// TOMBOL BOOKMARK PADA DETAIL CAFE
@Composable
fun BookmarkAction(cafeId: String) {

    val viewModel: BookmarkActionViewModel =
        viewModel(key = "bookmark-action-$cafeId")

    val state by viewModel.uiState
        .collectAsStateWithLifecycle()

    LaunchedEffect(cafeId) {
        viewModel.load(cafeId)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        OutlinedButton(
            onClick = viewModel::onToggle,
            enabled = !state.isLoading &&
                    !state.isSaving &&
                    state.errorMessage == null &&
                    !state.requiresLogin,
            modifier = Modifier.fillMaxWidth()
        ) {

            if (state.isLoading || state.isSaving) {

                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    if (state.isSaving) {
                        "Memproses..."
                    } else {
                        "Memuat..."
                    }
                )

            } else {

                Text(
                    text = if (state.isBookmarked) {
                        "♥"
                    } else {
                        "♡"
                    },
                    style =
                        MaterialTheme.typography.titleLarge,
                    color =
                        MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = when {
                        state.requiresLogin ->
                            "Masuk untuk menyimpan"

                        state.isBookmarked ->
                            "Tersimpan di Saved Spot"

                        else ->
                            "Simpan ke Saved Spot"
                    }
                )
            }
        }

        // Pesan login
        if (state.requiresLogin) {

            Text(
                text = "Silakan login terlebih dahulu untuk menyimpan cafe.",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.error
            )
        }

        // Pesan berhasil
        state.successMessage?.let { message ->

            Text(
                text = message,
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.primary
            )
        }

        // Pesan error
        state.errorMessage?.let { message ->

            Text(
                text = message,
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.error
            )

            TextButton(
                onClick = {
                    viewModel.load(cafeId)
                },
                enabled = !state.isSaving &&
                        !state.isLoading
            ) {
                Text("Coba Lagi")
            }
        }
    }
}
