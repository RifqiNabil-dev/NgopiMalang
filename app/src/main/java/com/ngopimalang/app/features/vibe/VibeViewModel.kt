package com.ngopimalang.app.features.vibe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngopimalang.app.core.data.toUserMessage
import com.ngopimalang.app.core.model.Cafe
import com.ngopimalang.app.core.model.Vibe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VibeUiState(
    val vibes: List<Vibe> = emptyList(),
    val selectedIndex: Int = 0,
    val cafesByVibe: Map<String, List<Cafe>> = emptyMap(), // hanya tab yang sudah dibuka
    val loadingVibeIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAdmin: Boolean = false
)

class VibeViewModel(private val repo: VibeRepository = VibeRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(VibeUiState(isLoading = true))
    val uiState: StateFlow<VibeUiState> = _uiState.asStateFlow()

    fun setAdmin(isAdmin: Boolean) {
        _uiState.update { it.copy(isAdmin = isAdmin) }
    }

    /** Dipanggil sekali saat layar dibuka; ulangi hanya setelah tambah/edit/hapus. */
    fun ensureLoaded() {
        if (_uiState.value.vibes.isEmpty() && !_uiState.value.isLoading) load()
    }

    /** Memuat daftar vibe (dengan cafe_count dari view). Cafe per tab dimuat ulang saat tab aktif. */
    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.vibes.isEmpty(), errorMessage = null) }
            try {
                val vibes = repo.observeVibes()
                _uiState.update { s ->
                    s.copy(
                        vibes = vibes,
                        isLoading = false,
                        selectedIndex = s.selectedIndex.coerceIn(0, (vibes.size - 1).coerceAtLeast(0)),
                        cafesByVibe = emptyMap(),
                        loadingVibeIds = emptySet()
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
            }
        }
    }

    fun onVibeSelected(index: Int) {
        _uiState.update { s ->
            if (index in s.vibes.indices) s.copy(selectedIndex = index) else s
        }
    }

    /** Dipanggil saat halaman pager aktif. Yang sudah dimuat tidak dimuat ulang. */
    fun loadCafesFor(vibeId: String) {
        val s = _uiState.value
        if (vibeId in s.cafesByVibe || vibeId in s.loadingVibeIds) return
        _uiState.update { it.copy(loadingVibeIds = it.loadingVibeIds + vibeId) }
        viewModelScope.launch {
            try {
                val cafes = repo.cafesForVibe(vibeId)
                _uiState.update {
                    it.copy(
                        cafesByVibe = it.cafesByVibe + (vibeId to cafes),
                        loadingVibeIds = it.loadingVibeIds - vibeId
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(loadingVibeIds = it.loadingVibeIds - vibeId, errorMessage = e.toUserMessage())
                }
            }
        }
    }

    fun onRetry() = load()

    /** Admin. Relasi cafe_vibes ikut terhapus; dialog di layar sudah menyebut jumlah cafe. */
    fun onDeleteVibe(id: String) {
        viewModelScope.launch {
            try {
                repo.delete(id)
                load()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(errorMessage = e.toUserMessage()) }
            }
        }
    }
}