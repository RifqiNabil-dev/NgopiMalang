package com.ngopimalang.app.features.directory

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CafeFormScreen(cafeId: String?, isAdmin: Boolean, onBack: () -> Unit,
    onSaved: () -> Unit, formViewModel: CafeFormViewModel = viewModel()) {
    val state by formViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reading by remember { mutableStateOf(false) }
    BackHandler(enabled = state.saving) { /* Keep the form until its write completes. */ }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            reading = true
            try { val photo = withContext(Dispatchers.IO) { readCafePhoto(context, uri) }; formViewModel.photo(uri.toString(), photo) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { formViewModel.photoError(e.message ?: "Tidak dapat membaca foto") }
            finally { reading = false }
        }
    }
    LaunchedEffect(cafeId) { if (isAdmin) formViewModel.load(cafeId) }
    LaunchedEffect(state.completed) { if (state.completed) onSaved() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack, enabled = !state.saving) { Text("Kembali") }
        Text(if (cafeId == null) "Tambah Cafe" else "Edit Cafe", style = MaterialTheme.typography.headlineSmall)
        if (!isAdmin) { Text("Hanya admin yang dapat mengelola cafe"); return@Column }
        if (state.loading || state.saving || reading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.error != null && !state.loading) TextButton(onClick = { formViewModel.load(cafeId) }) { Text("Muat ulang pilihan vibe") }
        val enabled = !state.saving && state.ready && !reading
        @Composable fun field(key: String, label: String, value: String, number: Boolean = false, lines: Int = 1) {
            OutlinedTextField(value = value, onValueChange = { formViewModel.field(key, it) }, label = { Text(label) },
                modifier = Modifier.fillMaxWidth(), enabled = enabled, singleLine = lines == 1, minLines = lines,
                isError = key in state.errors,
                supportingText = { state.errors[key]?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else KeyboardType.Text))
        }
        field("name", "Nama cafe *", state.name)
        field("address", "Alamat", state.address)
        field("hours", "Jam buka", state.hours)
        field("min", "Harga minimum (rupiah)", state.min, true)
        field("max", "Harga maksimum (rupiah)", state.max, true)
        field("plugs", "Jumlah colokan", state.plugs, true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Wi-Fi tersedia"); Switch(checked = state.wifi, onCheckedChange = formViewModel::wifi, enabled = enabled)
        }
        field("description", "Deskripsi", state.description, lines = 3)
        Text("Suasana cafe", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.vibes.forEach { vibe -> FilterChip(selected = vibe.id in state.selected,
                onClick = { formViewModel.vibe(vibe.id) }, enabled = enabled, label = { Text(vibe.name) }) }
        }
        val preview: Any? = state.photoUri ?: state.initialPhotoUrl
        if (preview != null) AsyncImage(preview, "Pratinjau foto cafe", Modifier.fillMaxWidth().height(190.dp))
        OutlinedButton(onClick = { picker.launch("image/*") }, enabled = enabled) { Text("Pilih foto (maksimal 5 MB)") }
        Button(onClick = { formViewModel.save(cafeId, isAdmin) }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.saving) "Menyimpan…" else "Simpan cafe")
        }
    }
}
internal fun readCafePhoto(context: Context, uri: Uri): CafePhoto {
    val mime = context.contentResolver.getType(uri) ?: error("Jenis foto tidak diketahui")
    val extension = when (mime) { "image/jpeg" -> "jpg"; "image/png" -> "png"; "image/webp" -> "webp"
        else -> error("Gunakan JPEG, PNG, atau WebP") }
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
        while (true) { val size = input.read(buffer); if (size < 0) break
            require(output.size() + size <= 5 * 1024 * 1024) { "Foto maksimal 5 MB" }; output.write(buffer, 0, size) }
        output.toByteArray()
    } ?: error("Foto tidak dapat dibaca")
    require(bytes.isNotEmpty()) { "File foto kosong" }
    return CafePhoto(bytes, mime, extension)
}
