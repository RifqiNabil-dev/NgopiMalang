package com.ngopimalang.app.features.vibe

import androidx.lifecycle.ViewModel
import com.ngopimalang.app.core.data.DummyData
import com.ngopimalang.app.core.model.Cafe
import com.ngopimalang.app.core.model.Vibe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * cafeIdsByVibe meniru tabel `cafe_vibes` di Supabase nanti (vibe_id -> kumpulan cafe_id).
 * Pencocokan lewat NAMA vibe hanya dilakukan sekali, saat memuat data dummy awal.
 */
data class VibeUiState(
    val vibes: List<Vibe> = emptyList(),
    val selectedIndex: Int = 0,
    val allCafes: List<Cafe> = emptyList(),
    val cafeIdsByVibe: Map<String, Set<String>> = emptyMap()
) {
    fun vibeById(id: String?): Vibe? = vibes.find { it.id == id }

    fun cafesFor(vibeId: String): List<Cafe> {
        val ids = cafeIdsByVibe[vibeId].orEmpty()
        return allCafes.filter { it.id in ids }
    }
}

class VibeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VibeUiState())
    val uiState: StateFlow<VibeUiState> = _uiState.asStateFlow()

    init {
        loadVibes()
    }

    private fun loadVibes() {
        val vibes = DummyData.vibes
        val cafes = DummyData.cafes
        _uiState.value = VibeUiState(
            vibes = vibes,
            selectedIndex = 0,
            allCafes = cafes,
            cafeIdsByVibe = vibes.associate { vibe ->
                vibe.id to cafes
                    .filter { vibe.name in it.vibes }
                    .map { it.id }
                    .toSet()
            }
        )
    }

    fun onVibeSelected(index: Int) {
        _uiState.update { state ->
            if (index in state.vibes.indices) state.copy(selectedIndex = index) else state
        }
    }

    fun addVibe(name: String, description: String, cafeIds: List<String>) {
        _uiState.update { state ->
            val id = "v${System.currentTimeMillis()}"
            val ids = cafeIds.toSet()
            val vibe = Vibe(
                id = id,
                name = name,
                description = description,
                cafeCount = ids.size
            )
            state.copy(
                vibes = state.vibes + vibe,
                cafeIdsByVibe = state.cafeIdsByVibe + (id to ids)
            )
        }
    }

    fun updateVibe(id: String, name: String, description: String, cafeIds: List<String>) {
        _uiState.update { state ->
            val ids = cafeIds.toSet()
            state.copy(
                vibes = state.vibes.map { vibe ->
                    if (vibe.id == id) {
                        vibe.copy(name = name, description = description, cafeCount = ids.size)
                    } else {
                        vibe
                    }
                },
                cafeIdsByVibe = state.cafeIdsByVibe + (id to ids)
            )
        }
    }

    fun deleteVibe(id: String) {
        _uiState.update { state ->
            val vibes = state.vibes.filterNot { it.id == id }
            state.copy(
                vibes = vibes,
                selectedIndex = state.selectedIndex.coerceIn(0, (vibes.size - 1).coerceAtLeast(0)),
                cafeIdsByVibe = state.cafeIdsByVibe - id
            )
        }
    }

    /** Tandai cafe ke vibe, atau lepas tandanya kalau sudah ada. */
    fun toggleCafe(vibeId: String, cafeId: String) {
        _uiState.update { state ->
            val current = state.cafeIdsByVibe[vibeId].orEmpty()
            val updated = if (cafeId in current) current - cafeId else current + cafeId
            state.copy(
                vibes = state.vibes.map { vibe ->
                    if (vibe.id == vibeId) vibe.copy(cafeCount = updated.size) else vibe
                },
                cafeIdsByVibe = state.cafeIdsByVibe + (vibeId to updated)
            )
        }
    }
}