package com.ngopimalang.app.features.directory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ngopimalang.app.core.data.toUserMessage
import com.ngopimalang.app.core.model.Cafe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class DirectoryUiState(
    val cafes: List<Cafe> = emptyList(), val query: String = "",
    val wifiOnly: Boolean = false, val plugsOnly: Boolean = false,
    val isLoading: Boolean = false, val errorMessage: String? = null, val isAdmin: Boolean = false
)
class DirectoryViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val repo by lazy { CafeRepository() }
    private val mutable = MutableStateFlow(DirectoryUiState(query = saved["query"] ?: "",
        wifiOnly = saved["wifi"] ?: false, plugsOnly = saved["plugs"] ?: false))
    val uiState = mutable.asStateFlow()
    private var job: Job? = null
    fun configure(admin: Boolean) { mutable.update { it.copy(isAdmin = admin) }; refresh() }
    fun onSearchChange(value: String) { saved["query"] = value; mutable.update { it.copy(query = value) }; refresh(300) }
    fun toggleWifi() { mutable.update { it.copy(wifiOnly = !it.wifiOnly) }; saved["wifi"] = mutable.value.wifiOnly; refresh() }
    fun togglePlugs() { mutable.update { it.copy(plugsOnly = !it.plugsOnly) }; saved["plugs"] = mutable.value.plugsOnly; refresh() }
    fun resetFilters() { saved["query"] = ""; saved["wifi"] = false; saved["plugs"] = false
        mutable.update { it.copy(query = "", wifiOnly = false, plugsOnly = false) }; refresh() }
    fun refresh(debounce: Long = 0) {
        job?.cancel(); job = viewModelScope.launch {
            delay(debounce)
            mutable.update { it.copy(isLoading = true, errorMessage = null) }
            try { val s = mutable.value; val result = repo.list(s.query, s.wifiOnly, s.plugsOnly, s.isAdmin)
                mutable.update { it.copy(cafes = result, isLoading = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) } }
        }
    }
}
data class CafeDetailUiState(
    val cafe: Cafe? = null, val isLoading: Boolean = false, val isSaving: Boolean = false,
    val errorMessage: String? = null, val notFound: Boolean = false,
    val needsArchive: Boolean = false, val completed: Boolean = false
)
class CafeDetailViewModel : ViewModel() {
    private val repo by lazy { CafeRepository() }
    private val mutable = MutableStateFlow(CafeDetailUiState())
    val uiState = mutable.asStateFlow()
    private var job: Job? = null
    fun load(id: String) {
        job?.cancel(); job = viewModelScope.launch {
            mutable.update { it.copy(isLoading = true, errorMessage = null) }
            try { val cafe = repo.loadCafe(id); mutable.update { it.copy(cafe = cafe, isLoading = false, notFound = cafe == null) } }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) } }
        }
    }
    fun delete(archive: Boolean = false) {
        if (mutable.value.isSaving) return
        val cafe = mutable.value.cafe ?: return
        mutable.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                if (archive) repo.archive(cafe.id) else repo.delete(cafe.id, cafe.photoPath)
                mutable.update { it.copy(isSaving = false, completed = true, needsArchive = false) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val fk = generateSequence(e as Throwable?) { it.cause }.any { "23503" in it.message.orEmpty() || "foreign key" in it.message.orEmpty() }
                mutable.update { it.copy(isSaving = false, needsArchive = fk, errorMessage = e.toUserMessage()) }
            }
        }
    }
}

data class CafeFormState(
    val name: String = "", val address: String = "", val description: String = "", val hours: String = "",
    val min: String = "", val max: String = "", val plugs: String = "0", val wifi: Boolean = false,
    val selected: Set<String> = emptySet(), val vibes: List<VibeOption> = emptyList(),
    val photoUri: String? = null, val photo: CafePhoto? = null, val initialPhotoUrl: String? = null,
    val loading: Boolean = true, val ready: Boolean = false, val saving: Boolean = false, val error: String? = null,
    val errors: Map<String, String> = emptyMap(), val completed: Boolean = false
)
class CafeFormViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val repo by lazy { CafeRepository() }
    private val mutable = MutableStateFlow(CafeFormState(
        name = saved["name"] ?: "", address = saved["address"] ?: "", description = saved["description"] ?: "",
        hours = saved["hours"] ?: "", min = saved["min"] ?: "", max = saved["max"] ?: "",
        plugs = saved["plugs"] ?: "0", wifi = saved["wifi"] ?: false,
        selected = (saved.get<ArrayList<String>>("vibes") ?: arrayListOf()).toSet(), photoUri = saved["photoUri"]
    ))
    val uiState = mutable.asStateFlow()
    private var loaded = false
    private var oldPhoto: String? = null
    fun load(id: String?) {
        if (loaded) return; loaded = true
        viewModelScope.launch {
            try {
                val options = repo.vibes()
                val cafe = id?.let { repo.loadCafe(it) ?: error("Cafe tidak ditemukan") }
                oldPhoto = cafe?.photoPath
                val links = if (id == null) emptySet() else repo.links(id).map { it.vibeId }.toSet()
                mutable.update { s -> if (cafe != null) s.copy(name = saved["name"] ?: cafe.name,
                    address = saved["address"] ?: cafe.address,
                    description = saved["description"] ?: cafe.description.orEmpty(), hours = saved["hours"] ?: cafe.openHours.orEmpty(),
                    min = saved["min"] ?: cafe.priceMin?.toString().orEmpty(), max = saved["max"] ?: cafe.priceMax?.toString().orEmpty(),
                    plugs = saved["plugs"] ?: cafe.plugCount.toString(), wifi = saved["wifi"] ?: cafe.hasWifi,
                    selected = saved.get<ArrayList<String>>("vibes")?.toSet() ?: links,
                    vibes = options, loading = false, ready = true, initialPhotoUrl = cafe.photoUrl)
                    else s.copy(vibes = options, loading = false, ready = true) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { loaded = false; mutable.update { it.copy(loading = false, error = e.toUserMessage()) } }
        }
    }
    fun field(key: String, value: String) {
        saved["edited"] = true; saved[key] = value
        mutable.update { s -> when (key) {
            "name" -> s.copy(name = value); "address" -> s.copy(address = value)
            "description" -> s.copy(description = value); "hours" -> s.copy(hours = value)
            "min" -> s.copy(min = value); "max" -> s.copy(max = value); "plugs" -> s.copy(plugs = value)
            else -> s
        }.copy(errors = emptyMap(), error = null) }
    }
    fun wifi(value: Boolean) { saved["edited"] = true; saved["wifi"] = value; mutable.update { it.copy(wifi = value) } }
    fun vibe(id: String) { saved["edited"] = true; mutable.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) }; saved["vibes"] = ArrayList(mutable.value.selected) }
    fun photo(uri: String, photo: CafePhoto) { saved["photoUri"] = uri; mutable.update { it.copy(photoUri = uri, photo = photo, error = null) } }
    fun photoError(message: String) { mutable.update { it.copy(error = message) } }
    fun save(id: String?, admin: Boolean) {
        if (!admin || mutable.value.saving || !mutable.value.ready) return
        val s = mutable.value
        val errors = validateCafeForm(s)
        if (errors.isNotEmpty()) { mutable.update { it.copy(errors = errors) }; return }
        if (s.photoUri != null && s.photo == null) { photoError("Pilih ulang foto setelah aplikasi dipulihkan sebelum menyimpan."); return }
        mutable.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            var persisted = false
            try {
                val draft = CafeDraft(s.name, s.address, s.description, s.hours, s.min.toIntOrNull(),
                    s.max.toIntOrNull(), s.wifi, s.plugs.toInt(), s.selected)
                // Keep the returned ID before further network operations: retry updates, never duplicates.
                val target = repo.save(saved["persistedId"] ?: id, draft)
                saved["persistedId"] = target; persisted = true
                repo.saveVibes(target, draft.vibeIds)
                s.photo?.let { repo.attachPhoto(target, it, oldPhoto) }
                mutable.update { it.copy(saving = false, completed = true) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { mutable.update { it.copy(saving = false, error =
                (if (persisted) "Data cafe sudah tersimpan, tetapi relasi/foto belum selesai. Tekan simpan untuk mencoba lagi. " else "") + e.toUserMessage()) } }
        }
    }
}
fun validateCafeForm(s: CafeFormState): Map<String, String> = buildMap {
    if (s.name.isBlank()) put("name", "Nama cafe wajib diisi")
    if (s.name.trim().length > 150) put("name", "Nama maksimal 150 karakter")
    for ((key, value) in listOf("min" to s.min, "max" to s.max))
        if (value.isNotBlank() && (value.toIntOrNull() == null || (value.toIntOrNull() ?: -1) < 0)) put(key, "Isi angka rupiah minimal 0")
    if (s.min.toIntOrNull() != null && s.max.toIntOrNull() != null && s.min.toInt() > s.max.toInt()) put("max", "Harga maksimum harus ≥ minimum")
    if (s.plugs.toIntOrNull() == null || (s.plugs.toIntOrNull() ?: -1) < 0) put("plugs", "Jumlah colokan minimal 0")
}
