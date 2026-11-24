package com.ngopimalang.app.features.bookmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngopimalang.app.core.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookmarkUiState(
    val items: List<BookmarkItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val editingCafeId: String? = null,
    val deletingCafeId: String? = null,
    val noteDraft: String = ""
)

class BookmarkViewModel(
    private val repo: BookmarkRepository = BookmarkRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarkUiState())

    val uiState: StateFlow<BookmarkUiState> =
        _uiState.asStateFlow()

    // Memuat daftar bookmark dari Supabase
    fun load() {
        if (_uiState.value.isLoading ||
            _uiState.value.isSaving
        ) return

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val items = repo.observeBookmarks()

                _uiState.update {
                    it.copy(items = items)
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        items = emptyList(),
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

    // Mencoba memuat ulang
    fun onRetry() {
        load()
    }

    // Membuka dialog hapus
    fun onRemoveClick(cafeId: String) {
        if (_uiState.value.isSaving) return

        _uiState.update {
            it.copy(
                deletingCafeId = cafeId,
                errorMessage = null
            )
        }
    }

    // Menutup dialog hapus
    fun onRemoveDismiss() {
        if (_uiState.value.isSaving) return

        _uiState.update {
            it.copy(deletingCafeId = null)
        }
    }

    // Menghapus bookmark dari database
    fun onRemoveConfirm() {
        val cafeId = _uiState.value.deletingCafeId
            ?: return

        if (_uiState.value.isSaving ||
            _uiState.value.isLoading
        ) return

        _uiState.update {
            it.copy(
                isSaving = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val affectedRows = repo.remove(cafeId)

                check(affectedRows > 0) {
                    "Bookmark tidak ditemukan atau sudah dihapus"
                }

                _uiState.update {
                    it.copy(deletingCafeId = null)
                }

                val items = repo.observeBookmarks()

                _uiState.update {
                    it.copy(items = items)
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

    // Membuka dialog edit catatan
    fun onEditNoteClick(cafeId: String) {
        if (_uiState.value.isSaving) return

        val item = _uiState.value.items.firstOrNull {
            it.cafeId == cafeId
        } ?: return

        _uiState.update {
            it.copy(
                editingCafeId = cafeId,
                noteDraft = item.note,
                errorMessage = null
            )
        }
    }

    // Menampung perubahan isi catatan
    fun onNoteChange(text: String) {
        if (_uiState.value.isSaving) return

        _uiState.update {
            it.copy(noteDraft = text)
        }
    }

    // Menutup dialog edit
    fun onEditNoteDismiss() {
        if (_uiState.value.isSaving) return

        _uiState.update {
            it.copy(
                editingCafeId = null,
                noteDraft = ""
            )
        }
    }

    // Menyimpan catatan ke Supabase
    fun onNoteSave() {
        val cafeId = _uiState.value.editingCafeId
            ?: return

        if (_uiState.value.isSaving ||
            _uiState.value.isLoading
        ) return

        val note = _uiState.value.noteDraft.trim()

        _uiState.update {
            it.copy(
                isSaving = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val affectedRows =
                    repo.updateNote(cafeId, note)

                check(affectedRows > 0) {
                    "Catatan gagal diperbarui"
                }

                _uiState.update {
                    it.copy(
                        editingCafeId = null,
                        noteDraft = ""
                    )
                }

                val items = repo.observeBookmarks()

                _uiState.update {
                    it.copy(items = items)
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
