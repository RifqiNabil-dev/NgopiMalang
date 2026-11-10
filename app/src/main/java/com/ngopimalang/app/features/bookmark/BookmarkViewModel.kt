
package com.ngopimalang.app.features.bookmark

import androidx.lifecycle.ViewModel
import com.ngopimalang.app.core.model.Cafe
import com.ngopimalang.app.core.data.DummyData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BookmarkUiState(
    val bookmarks: List<BookmarkItem> = emptyList(),
    val cafes: List<Cafe> = emptyList()
)

class BookmarkViewModel : ViewModel() {

    private val initialCafes = DummyData.cafes

    private val initialBookmarks = initialCafes
        .take(3)
        .mapIndexed { index, cafe ->
            BookmarkItem(
                id = "bookmark-${index + 1}",
                cafeId = cafe.id,
                cafeName = cafe.name,
                note = when (index) {
                    0 -> "Cocok buat nugas sore"
                    1 -> "Mau coba menu favoritnya"
                    else -> "Tempat nongkrong bareng teman"
                },
                savedAtLabel = "Baru disimpan"
            )
        }

    private val _uiState = MutableStateFlow(
        BookmarkUiState(
            bookmarks = initialBookmarks,
            cafes = initialCafes
        )
    )

    val uiState: StateFlow<BookmarkUiState> =
        _uiState.asStateFlow()

    fun toggleBookmark(cafe: Cafe) {
        _uiState.update { state ->
            val existing = state.bookmarks
                .firstOrNull { it.cafeId == cafe.id }

            val updated = if (existing != null) {
                state.bookmarks.filterNot {
                    it.id == existing.id
                }
            } else {
                state.bookmarks + BookmarkItem(
                    id = "bookmark-${cafe.id}",
                    cafeId = cafe.id,
                    cafeName = cafe.name
                )
            }

            state.copy(bookmarks = updated)
        }
    }

    fun removeBookmark(id: String) {
        _uiState.update { state ->
            state.copy(
                bookmarks = state.bookmarks.filterNot {
                    it.id == id
                }
            )
        }
    }

    fun updateNote(id: String, note: String) {
        _uiState.update { state ->
            state.copy(
                bookmarks = state.bookmarks.map { item ->
                    if (item.id == id) {
                        item.copy(note = note.trim())
                    } else {
                        item
                    }
                }
            )
        }
    }
}
