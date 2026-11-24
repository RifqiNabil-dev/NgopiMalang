package com.ngopimalang.app.features.eventpromo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngopimalang.app.core.data.toUserMessage
import com.ngopimalang.app.core.model.EventCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class EventFormUiState(
    val title: String = "",
    val description: String = "",
    val category: EventCategory = EventCategory.EVENT,
    val startDate: String = "",
    val startTime: String = "",
    val endDate: String = "",
    val endTime: String = "",
    val cafeId: String? = null,
    val cafeName: String = "",
    val priceMinText: String = "",
    val priceMaxText: String = "",
    val isPublished: Boolean = true,
    val availableCafes: List<EventCafeOptionRow> = emptyList(),
    val isSaving: Boolean = false,
    val isEditing: Boolean = false,
    val validationErrors: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val done: Boolean = false
)

/**
 * Form tambah/edit event. Memegang seluruh input supaya rotasi layar tidak
 * menghilangkan isian (state hoisting, dok 08 bagian 5).
 */
class EventFormViewModel(
    private val repo: EventPromoRepository = EventPromoRepository()
) : ViewModel() {

    private val _formState = MutableStateFlow(EventFormUiState())
    val formState: StateFlow<EventFormUiState> = _formState.asStateFlow()

    private var editingId: String? = null

    /** [eventId] null berarti tambah baru. */
    fun loadForEdit(eventId: String?) {
        editingId = eventId
        if (eventId == null) {
            _formState.update { it.copy(isEditing = false) }
        } else {
            _formState.update { it.copy(isEditing = true) }
            viewModelScope.launch {
                try {
                    val event = repo.loadEvent(eventId)
                    if (event == null) {
                        _formState.update {
                            it.copy(errorMessage = "Event tidak ditemukan")
                        }
                    } else {
                        val (sd, st) = event.startsAt.toLocalDateAndTime()
                        val (ed, et) = event.endsAt.toLocalDateAndTime()
                        _formState.update {
                            it.copy(
                                title = event.title,
                                description = event.description,
                                category = event.category,
                                startDate = sd,
                                startTime = st,
                                endDate = ed,
                                endTime = et,
                                cafeId = event.cafeId,
                                cafeName = event.cafeName,
                                priceMinText = event.priceMin?.toString().orEmpty(),
                                priceMaxText = event.priceMax?.toString().orEmpty(),
                                isPublished = event.isPublished
                            )
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    _formState.update { it.copy(errorMessage = e.toUserMessage()) }
                }
            }
        }
        loadCafes()
    }

    private fun loadCafes() {
        viewModelScope.launch {
            try {
                val cafes = repo.cafesForPicker()
                _formState.update { it.copy(availableCafes = cafes) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                // Daftar cafe hanya pelengkap; kegagalan tidak boleh memblokir form.
                _formState.update { it.copy(errorMessage = e.toUserMessage()) }
            }
        }
    }

    fun onTitleChange(text: String) =
        _formState.update { it.copy(title = text, validationErrors = it.validationErrors - "title") }

    fun onDescriptionChange(text: String) =
        _formState.update { it.copy(description = text, validationErrors = it.validationErrors - "description") }

    fun onCategoryChange(category: EventCategory) =
        _formState.update { it.copy(category = category) }

    fun onStartDateChange(text: String) =
        _formState.update { it.copy(startDate = text, validationErrors = it.validationErrors - "time") }

    fun onStartTimeChange(text: String) =
        _formState.update { it.copy(startTime = text, validationErrors = it.validationErrors - "time") }

    fun onEndDateChange(text: String) =
        _formState.update { it.copy(endDate = text, validationErrors = it.validationErrors - "time") }

    fun onEndTimeChange(text: String) =
        _formState.update { it.copy(endTime = text, validationErrors = it.validationErrors - "time") }

    fun onCafeChange(cafeId: String?, cafeName: String) =
        _formState.update { it.copy(cafeId = cafeId, cafeName = cafeName) }

    fun onPriceMinChange(text: String) =
        _formState.update { it.copy(priceMinText = text, validationErrors = it.validationErrors - "price") }

    fun onPriceMaxChange(text: String) =
        _formState.update { it.copy(priceMaxText = text, validationErrors = it.validationErrors - "price") }

    fun onPublishChange(published: Boolean) =
        _formState.update { it.copy(isPublished = published) }

    fun onSave() {
        val s = _formState.value
        if (s.isSaving) return

        val errors = validate(s)
        if (errors.isNotEmpty()) {
            _formState.update { it.copy(validationErrors = errors) }
            return
        }

        val startsAt = combine(s.startDate, s.startTime)
        val endsAt = combine(s.endDate, s.endTime)
        val min = s.priceMinText.trim().toIntOrNull()
        val max = s.priceMaxText.trim().toIntOrNull()
        val draft = EventPromoDraft(
            title = s.title,
            description = s.description,
            category = s.category.dbValue,
            startsAt = startsAt,
            endsAt = endsAt,
            cafeId = s.cafeId,
            priceMin = min,
            priceMax = max,
            isPublished = s.isPublished
        )

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, errorMessage = null, validationErrors = emptyMap()) }
            try {
                val id = editingId
                if (id == null) {
                    repo.create(draft, s.cafeName)
                } else {
                    repo.update(id, draft)
                }
                editingId = id ?: editingId
                _formState.update { it.copy(isSaving = false, done = true) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val text = e.toUserMessage()
                _formState.update {
                    it.copy(isSaving = false, errorMessage = text)
                }
            }
        }
    }

    /** Hapus event yang sedang diedit. */
    fun onDelete() {
        val id = editingId ?: return
        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val deleted = repo.delete(id)
                if (deleted == 0) {
                    _formState.update {
                        it.copy(isSaving = false, errorMessage = "Tidak punya hak menghapus event ini")
                    }
                } else {
                    _formState.update { it.copy(isSaving = false, done = true) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _formState.update { it.copy(isSaving = false, errorMessage = e.toUserMessage()) }
            }
        }
    }

    private fun validate(s: EventFormUiState): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (s.title.isBlank()) errors["title"] = "Judul wajib diisi"
        if (s.description.isBlank()) errors["description"] = "Deskripsi wajib diisi"

        val start = runCatching { parseDateTime(s.startDate, s.startTime) }.getOrNull()
        val end = runCatching { parseDateTime(s.endDate, s.endTime) }.getOrNull()
        if (start == null || end == null) {
            errors["time"] = "Tanggal dan jam wajib diisi dengan benar"
        } else if (end <= start) {
            errors["time"] = "Waktu selesai harus setelah waktu mulai"
        }

        val min = s.priceMinText.trim().toIntOrNull()
        val max = s.priceMaxText.trim().toIntOrNull()
        when {
            s.priceMinText.isNotBlank() && min == null ->
                errors["price"] = "Harga minimum harus berupa angka"
            s.priceMaxText.isNotBlank() && max == null ->
                errors["price"] = "Harga maksimum harus berupa angka"
            min != null && min < 0 ->
                errors["price"] = "Harga tidak boleh negatif"
            max != null && max < 0 ->
                errors["price"] = "Harga tidak boleh negatif"
            min != null && max != null && min > max ->
                errors["price"] = "Harga minimum tidak boleh lebih besar dari maksimum"
        }
        return errors
    }
}

private fun Instant.toLocalDateAndTime(): Pair<String, String> {
    val local = toLocalDateTime(TimeZone.currentSystemDefault())
    val date = "%04d-%02d-%02d".format(local.year, local.monthNumber, local.dayOfMonth)
    val time = "%02d:%02d".format(local.hour, local.minute)
    return date to time
}

/** Gabungkan input tanggal `yyyy-mm-dd` dan jam `HH:mm` menjadi timestamp ISO. */
private fun combine(date: String, time: String): String {
    val parsedDate = requireNotNull(parseLocalDate(date))
    val parsedTime = requireNotNull(parseLocalTime(time))
    return "${parsedDate}T${parsedTime}:00+07:00"
}

private fun parseDateTime(date: String, time: String): Instant {
    val combined = combine(date, time)
    return Instant.parse(combined)
}

private fun parseLocalDate(value: String): String? {
    val parts = value.split("-")
    if (parts.size != 3) return null
    return "%04d-%02d-%02d".format(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
}

private fun parseLocalTime(value: String): String? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    return "%02d:%02d".format(parts[0].toInt(), parts[1].toInt())
}