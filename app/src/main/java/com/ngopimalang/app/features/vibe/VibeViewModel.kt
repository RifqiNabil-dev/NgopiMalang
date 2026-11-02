package com.ngopimalang.app.features.vibe

import androidx.lifecycle.ViewModel
import com.ngopimalang.app.core.data.DummyData
import com.ngopimalang.app.core.model.Cafe
import com.ngopimalang.app.core.model.Vibe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VibeUiState(
    val vibes: List<Vibe> = emptyList(),
    val selectedIndex: Int = 0,
    val cafesByVibe: Map<String, List<Cafe>> = emptyMap()
)

class VibeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VibeUiState())
    val uiState: StateFlow<VibeUiState> = _uiState.asStateFlow()

    init {
        loadVibes()
    }

    private fun loadVibes() {
        val vibes = DummyData.vibes

        val cafesByVibe = vibes.associate { vibe ->
            vibe.id to DummyData.cafes.filter { cafe ->
                vibe.name in cafe.vibes
            }
        }

        _uiState.value = VibeUiState(
            vibes = vibes,
            cafesByVibe = cafesByVibe
        )
    }

    fun onVibeSelected(index: Int) {
        _uiState.update {
            it.copy(selectedIndex = index)
        }
    }

    fun getVibeById(id: String): Vibe? {
        return _uiState.value.vibes.find {
            it.id == id
        }
    }

    fun getCafesForVibe(id: String): List<Cafe> {
        return _uiState.value.cafesByVibe[id].orEmpty()
    }
}