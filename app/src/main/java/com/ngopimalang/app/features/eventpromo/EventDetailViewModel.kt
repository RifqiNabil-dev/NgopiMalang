package com.ngopimalang.app.features.eventpromo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngopimalang.app.core.data.toUserMessage
import com.ngopimalang.app.core.model.EventPromo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventDetailUiState(
    val event: EventPromo? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val notFound: Boolean = false
)

/**
 * Detail satu event. Memuat sendiri berdasarkan id, tidak mengandalkan daftar
 * layar pemanggil (dok 10 bagian 4 dan dok 09 bagian 11.1).
 */
class EventDetailViewModel(
    private val repo: EventPromoRepository = EventPromoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    private var currentId: String? = null

    fun load(eventId: String) {
        currentId = eventId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, notFound = false) }
            try {
                val event = repo.loadEvent(eventId)
                _uiState.update {
                    if (event == null) {
                        it.copy(isLoading = false, notFound = true, event = null)
                    } else {
                        it.copy(isLoading = false, event = event)
                    }
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

    fun onDelete() {
        val id = currentId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                repo.delete(id)
                // Setelah hapus berhasil: state event dikosongkan; layar pemanggil
                // harus memanggil onNavigateBack() begitu event == null.
                _uiState.update { it.copy(isLoading = false, event = null) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
            }
        }
    }
}