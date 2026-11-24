package com.ngopimalang.app.features.vibe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngopimalang.app.core.data.toUserMessage
import com.ngopimalang.app.core.model.Cafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VibeFormUiState(
    val name: String = "",
    val description: String = "",
    val iconKey: String = "",
    val selectedCafeIds: Set<String> = emptySet(),
    val availableCafes: List<Cafe> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val validationError: String? = null,
    val isEditing: Boolean = false,
    val cafeCount: Int = 0,   // untuk dialog hapus
    val done: Boolean = false // true setelah simpan/hapus berhasil -> layar kembali
)

class VibeFormViewModel(private val repo: VibeRepository = VibeRepository()) : ViewModel() {

    private val _formState = MutableStateFlow(VibeFormUiState())
    val formState: StateFlow<VibeFormUiState> = _formState.asStateFlow()

    private var editingId: String? = null

    /** null = tambah. */
    fun loadForEdit(vibeId: String?) {
        editingId = vibeId
        viewModelScope.launch {
            try {
                val cafes = repo.cafesForPicker()
                if (vibeId == null) {
                    _formState.update { it.copy(availableCafes = cafes, isEditing = false) }
                } else {
                    val vibe = repo.loadVibe(vibeId)
                    val ids = repo.cafeIdsOfVibe(vibeId)
                    _formState.update {
                        it.copy(
                            availableCafes = cafes,
                            isEditing = true,
                            name = vibe?.name.orEmpty(),
                            description = vibe?.description.orEmpty(),
                            iconKey = vibe?.iconKey.orEmpty(),
                            cafeCount = vibe?.cafeCount ?: 0,
                            selectedCafeIds = ids
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _formState.update { it.copy(errorMessage = e.toUserMessage()) }
            }
        }
    }

    fun onNameChange(text: String) =
        _formState.update { it.copy(name = text, validationError = null) }

    fun onDescriptionChange(text: String) =
        _formState.update { it.copy(description = text) }

    fun onIconChange(key: String) =
        _formState.update { it.copy(iconKey = key.take(24)) }

    fun onToggleCafe(cafeId: String) = _formState.update {
        val next = if (cafeId in it.selectedCafeIds) it.selectedCafeIds - cafeId else it.selectedCafeIds + cafeId
        it.copy(selectedCafeIds = next)
    }

    fun onSave() {
        val s = _formState.value
        if (s.isSaving) return
        if (s.name.isBlank()) {
            _formState.update { it.copy(validationError = "Nama vibe wajib diisi") }
            return
        }
        val draft = VibeDraft(s.name, s.description, s.iconKey, s.selectedCafeIds.toList())

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, errorMessage = null, validationError = null) }

            // Langkah 1: simpan baris vibes. Gagal -> tetap di form.
            val savedId: String
            try {
                val id = editingId
                savedId = if (id == null) repo.create(draft).id else repo.update(id, draft).id
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val t = e.message.orEmpty().lowercase()
                val msg = if ("23505" in t || "duplicate key" in t) {
                    "Nama vibe sudah dipakai"
                } else {
                    e.toUserMessage()
                }
                _formState.update { it.copy(isSaving = false, errorMessage = msg) }
                return@launch
            }

            // Vibe sudah punya id: simpan lagi nanti berarti update, bukan insert ganda.
            editingId = savedId

            // Langkah 2-3: relasi cafe_vibes.
            try {
                repo.setCafeRelations(savedId, draft.cafeIds)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _formState.update {
                    it.copy(
                        isSaving = false,
                        isEditing = true,
                        errorMessage = "Data vibe sudah tersimpan, tetapi relasi cafe gagal: ${e.toUserMessage()}. Tekan Simpan untuk mengulang."
                    )
                }
                return@launch
            }

            _formState.update { it.copy(isSaving = false, done = true) }
        }
    }

    fun onDelete() {
        val id = editingId ?: return
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                repo.delete(id)
                _formState.update { it.copy(isSaving = false, done = true) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _formState.update { it.copy(isSaving = false, errorMessage = e.toUserMessage()) }
            }
        }
    }
}