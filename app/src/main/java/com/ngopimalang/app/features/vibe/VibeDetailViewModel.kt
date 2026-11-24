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

data class VibeDetailUiState(
    val vibe: Vibe? = null,
    val cafes: List<Cafe> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val notFound: Boolean = false
)

class VibeDetailViewModel(private val repo: VibeRepository = VibeRepository()) : ViewModel() {

    private val _uiState = MutableStateFlow(VibeDetailUiState(isLoading = true))
    val uiState: StateFlow<VibeDetailUiState> = _uiState.asStateFlow()

    private var currentId: String? = null

    fun load(vibeId: String) {
        currentId = vibeId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, notFound = false) }
            try {
                val vibe = repo.loadVibe(vibeId)
                if (vibe == null) {
                    _uiState.update { it.copy(isLoading = false, notFound = true, vibe = null) }
                } else {
                    val cafes = repo.cafesForVibe(vibeId)
                    _uiState.update { it.copy(isLoading = false, vibe = vibe, cafes = cafes) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
            }
        }
    }

    fun onRetry() {
        currentId?.let { load(it) }
    }
}