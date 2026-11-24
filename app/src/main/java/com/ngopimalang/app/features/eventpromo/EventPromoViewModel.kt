package com.ngopimalang.app.features.eventpromo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngopimalang.app.core.data.toUserMessage
import com.ngopimalang.app.core.model.EventCategory
import com.ngopimalang.app.core.model.EventPromo
import com.ngopimalang.app.core.model.EventStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventPromoUiState(
    val events: List<EventPromo> = emptyList(),
    val query: String = "",
    val selectedCategory: EventCategory? = null,
    val onlyUpcoming: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAdmin: Boolean = false
) {
    /**
     * Daftar yang ditampilkan layar. Dihitung, tidak disimpan (dok 10 aturan A4):
     * tidak ada `allEvents` + `filteredEvents` yang bisa tidak sinkron.
     */
    val visibleEvents: List<EventPromo>
        get() {
            val keyword = query.trim()
            return events.filter { event ->
                (selectedCategory == null || event.category == selectedCategory) &&
                    (!onlyUpcoming || event.status != EventStatus.FINISHED) &&
                    (keyword.isEmpty() ||
                        event.title.contains(keyword, true) ||
                        event.cafeName.contains(keyword, true) ||
                        event.description.contains(keyword, true))
            }
        }

    val isEmpty: Boolean get() = !isLoading && visibleEvents.isEmpty()
}

/**
 * Daftar Event & Promo. Data dari Supabase lewat repository; layar tidak pernah
 * memegang data.
 */
class EventPromoViewModel(
    private val repo: EventPromoRepository = EventPromoRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventPromoUiState(isLoading = true))
    val uiState: StateFlow<EventPromoUiState> = _uiState.asStateFlow()

    fun setAdmin(isAdmin: Boolean) {
        _uiState.update { it.copy(isAdmin = isAdmin) }
    }

    /** Dipanggil sekali saat layar dibuka; ulangi hanya setelah simpan/hapus. */
    fun ensureLoaded() {
        if (_uiState.value.events.isEmpty() && !_uiState.value.isLoading) load()
    }

    fun onQueryChange(text: String) = _uiState.update { it.copy(query = text) }

    fun onCategorySelected(category: EventCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
        // Kategori masuk query repository, bukan hanya filter di layar.
        load()
    }

    fun onOnlyUpcomingChange(enabled: Boolean) =
        _uiState.update { it.copy(onlyUpcoming = enabled) }

    fun load() {
        val category = _uiState.value.selectedCategory
        val isAdmin = _uiState.value.isAdmin
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.events.isEmpty(), errorMessage = null) }
            try {
                val events = repo.observeEvents(category, isAdmin)
                _uiState.update { it.copy(events = events, isLoading = false) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
            }
        }
    }

    fun onRetry() = load()

    fun getEventById(eventId: String): EventPromo? =
        _uiState.value.events.firstOrNull { it.id == eventId }
}