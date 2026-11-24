package com.ngopimalang.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngopimalang.app.core.data.toUserMessage
import com.ngopimalang.app.core.model.Cafe
import com.ngopimalang.app.core.model.EventPromo
import com.ngopimalang.app.core.model.Review
import com.ngopimalang.app.features.directory.CafeRepository
import com.ngopimalang.app.features.eventpromo.EventPromoRepository
import com.ngopimalang.app.features.reviews.ReviewRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeCafeState(
    val cafes: List<Cafe> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val reviews: List<Review> = emptyList(),
    val reviewsLoading: Boolean = true,
    val reviewsError: String? = null,
    val events: List<EventPromo> = emptyList(),
    val eventsLoading: Boolean = true,
    val eventsError: String? = null
)

/** Home and Directory use the same Cloud models and UUIDs for the shared detail page. */
class HomeCafeViewModel : ViewModel() {
    private val repo by lazy { CafeRepository() }
    private val reviewRepo by lazy { ReviewRepository() }
    private val eventRepo by lazy { EventPromoRepository() }
    private val mutable = MutableStateFlow(HomeCafeState())
    val uiState = mutable.asStateFlow()
    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            launch {
                mutable.update { it.copy(loading = true, error = null) }
                try {
                    val cafes = repo.list("", wifi = false, plugs = false, admin = false)
                    mutable.update { it.copy(cafes = cafes, loading = false) }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { mutable.update { it.copy(loading = false, error = e.toUserMessage()) } }
            }
            launch {
                mutable.update { it.copy(reviewsLoading = true, reviewsError = null) }
                try {
                    val reviews = reviewRepo.observeReviews(limit = 6)
                    mutable.update { it.copy(reviews = reviews, reviewsLoading = false) }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { mutable.update { it.copy(reviewsLoading = false, reviewsError = e.toUserMessage()) } }
            }
            launch {
                mutable.update { it.copy(eventsLoading = true, eventsError = null) }
                try {
                    // Ambil 6 event terbaru untuk preview di home; tidak filter admin
                    val events = eventRepo.observeEvents(category = null, isAdmin = false, limit = 6)
                    mutable.update { it.copy(events = events, eventsLoading = false) }
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { mutable.update { it.copy(eventsLoading = false, eventsError = e.toUserMessage()) } }
            }
        }
    }
}
