package com.ngopimalang.app.features.directory

import androidx.lifecycle.ViewModel
import com.ngopimalang.app.core.data.DummyData
import com.ngopimalang.app.core.model.Cafe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DirectoryUiState(
    val cafes: List<Cafe> = emptyList(),
    val query: String = "",
    val wifiOnly: Boolean = false,
    val plugsOnly: Boolean = false
)

class DirectoryViewModel : ViewModel() {
    private val allCafes = DummyData.cafes.toList()
    private val _uiState = MutableStateFlow(DirectoryUiState(cafes = allCafes))
    val uiState = _uiState.asStateFlow()

    fun onSearchChange(query: String) = applyFilters { it.copy(query = query) }
    fun toggleWifi() = applyFilters { it.copy(wifiOnly = !it.wifiOnly) }
    fun togglePlugs() = applyFilters { it.copy(plugsOnly = !it.plugsOnly) }
    fun resetFilters() { _uiState.value = DirectoryUiState(cafes = allCafes) }
    fun findCafe(id: String?): Cafe? = allCafes.find { it.id == id }

    private fun applyFilters(transform: (DirectoryUiState) -> DirectoryUiState) {
        _uiState.update { previous ->
            val next = transform(previous)
            val keyword = next.query.trim()
            next.copy(cafes = allCafes.filter { cafe ->
                (keyword.isEmpty() || cafe.name.contains(keyword, ignoreCase = true) ||
                    cafe.address.contains(keyword, ignoreCase = true)) &&
                    (!next.wifiOnly || cafe.hasWifi) &&
                    (!next.plugsOnly || cafe.plugCount > 0)
            })
        }
    }
}
