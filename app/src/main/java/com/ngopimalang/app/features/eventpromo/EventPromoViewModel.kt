package com.ngopimalang.app.features.eventpromo

import androidx.lifecycle.ViewModel
import com.ngopimalang.app.core.data.DummyData
import com.ngopimalang.app.core.model.EventCategory
import com.ngopimalang.app.core.model.EventPromo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EventPromoUiState(
    val allEvents: List<EventPromo> = emptyList(),
    val filteredEvents: List<EventPromo> = emptyList(),
    val selectedCategory: EventCategory? = null
)

/**
 * ViewModel untuk fitur Event & Promo Cafe.
 * Mengelola daftar event/promo dan filter kategori.
 * Progress 1: data disimpan di memory (mutableList), Progress 2 akan diganti Supabase.
 */
class EventPromoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EventPromoUiState())
    val uiState: StateFlow<EventPromoUiState> = _uiState.asStateFlow()

    // Temporary storage untuk Progress 1 (diganti Supabase di Progress 2)
    private val eventStorage = DummyData.events.toMutableList()

    init {
        loadEvents()
    }

    private fun loadEvents() {
        _uiState.update {
            it.copy(
                allEvents = eventStorage,
                filteredEvents = applyFilter(eventStorage, it.selectedCategory)
            )
        }
    }

    fun onCategorySelected(category: EventCategory?) {
        _uiState.update { currentState ->
            currentState.copy(
                selectedCategory = category,
                filteredEvents = applyFilter(currentState.allEvents, category)
            )
        }
    }

    private fun applyFilter(events: List<EventPromo>, category: EventCategory?): List<EventPromo> {
        return if (category == null) {
            events
        } else {
            events.filter { it.category == category }
        }
    }

    fun getEventById(eventId: String): EventPromo? {
        return _uiState.value.allEvents.find { it.id == eventId }
    }

    fun addEvent(event: EventPromo) {
        // TODO Progress 2: ganti dengan Supabase insert
        eventStorage.add(event)
        loadEvents()
    }

    fun updateEvent(event: EventPromo) {
        // TODO Progress 2: ganti dengan Supabase update
        val index = eventStorage.indexOfFirst { it.id == event.id }
        if (index != -1) {
            eventStorage[index] = event
            loadEvents()
        }
    }

    fun deleteEvent(eventId: String) {
        // TODO Progress 2: ganti dengan Supabase delete
        eventStorage.removeAll { it.id == eventId }
        loadEvents()
    }
}
