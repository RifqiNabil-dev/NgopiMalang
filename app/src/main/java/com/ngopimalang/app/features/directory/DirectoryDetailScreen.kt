package com.ngopimalang.app.features.directory

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ngopimalang.app.core.components.AppCard
import com.ngopimalang.app.core.model.Cafe
import java.util.Locale

/** Shared detail entry: callers pass a Cloud UUID; each feature can supply its own section. */
@Composable
fun DirectoryDetailScreen(
    cafeId: String, onBack: () -> Unit, isAdmin: Boolean = false,
    onEdit: (String) -> Unit = {}, onChanged: () -> Unit = {},
    reviewSection: @Composable (String) -> Unit = {},
    bookmarkAction: @Composable (String) -> Unit = {},
    modifier: Modifier = Modifier, detailViewModel: CafeDetailViewModel = viewModel()
) {
    val state by detailViewModel.uiState.collectAsStateWithLifecycle()
    var confirm by rememberSaveable { mutableStateOf(false) }
    var intentError by remember(cafeId) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    LaunchedEffect(cafeId) { detailViewModel.load(cafeId) }
    LaunchedEffect(state.completed) { if (state.completed) { onChanged(); onBack() } }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                Spacer(Modifier.width(6.dp))
                Text("Kembali")
            }
            Text("Detail Cafe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        state.errorMessage?.let {
            AppCard(Modifier.padding(horizontal = 16.dp)) {
                Text(it, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { detailViewModel.load(cafeId) }) { Text("Coba lagi") }
            }
        }
        if (state.notFound) AppCard(Modifier.padding(horizontal = 16.dp)) {
            Text("Cafe tidak ditemukan atau tidak dapat diakses", style = MaterialTheme.typography.titleMedium)
        }
        state.cafe?.let { cafe ->
            CafeHero(cafe)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!cafe.isActive) SuggestionChip(onClick = {}, label = { Text("Tidak aktif / arsip") })
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(cafe.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(cafe.address.ifBlank { "Alamat belum tersedia" }, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                CafeVibes(cafe.vibes)
                AppCard {
                    DetailItem("Kisaran Harga", priceLabel(cafe.priceMin, cafe.priceMax))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailItem("Koneksi Wi-Fi", if (cafe.hasWifi) "Wi-Fi tersedia" else "Wi-Fi tidak tersedia")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailItem("Fasilitas Colokan", if (cafe.plugCount > 0) "${cafe.plugCount} titik colokan listrik" else "Tidak tersedia")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailItem("Jam buka", cafe.openHours?.takeIf { it.isNotBlank() } ?: "Belum tersedia")
                }
                cafe.description?.takeIf { it.isNotBlank() }?.let { description ->
                    AppCard { DetailItem("Tentang cafe", description) }
                }
                AppCard {
                    DetailItem("Ulasan pengunjung", cafe.rating?.let {
                        String.format(Locale.US, "%.1f / 5 • %d ulasan", it, cafe.reviewCount)
                    } ?: "Belum ada ulasan")
                }
                if (cafe.address.isNotBlank()) OutlinedButton(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("geo:0,0?q=${Uri.encode(cafe.name + " " + cafe.address)}")))
                    } catch (_: ActivityNotFoundException) { intentError = "Tidak ada aplikasi peta yang dapat dibuka" }
                }, modifier = Modifier.fillMaxWidth()) { Text("Buka di peta") }
                intentError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                bookmarkAction(cafe.id)
                reviewSection(cafe.id)
                if (isAdmin) AppCard {
                    Text("Kelola cafe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(onClick = { onEdit(cafe.id) }, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) { Text("Edit cafe") }
                    OutlinedButton(onClick = { confirm = true }, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) { Text("Hapus cafe") }
                    if (state.needsArchive) {
                        Text("Cafe masih dipakai fitur lain. Arsipkan agar ulasan dan bookmark tetap tersimpan.")
                        Button(onClick = { detailViewModel.delete(archive = true) }, enabled = !state.isSaving) { Text("Arsipkan cafe") }
                    }
                }
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false }, title = { Text("Hapus cafe?") },
        text = { Text("Penghapusan permanen hanya dapat dilakukan jika cafe tidak dipakai fitur lain. Jika masih dipakai, kamu dapat memilih arsip.") },
        confirmButton = { TextButton(onClick = { confirm = false; detailViewModel.delete() }) { Text("Hapus") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Batal") } }
    )
}

@Composable
private fun CafeHero(cafe: Cafe) {
    Box(
        Modifier.fillMaxWidth().height(240.dp).padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer))),
        contentAlignment = Alignment.Center
    ) {
        Text(cafe.name.take(1).uppercase(), style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        when {
            cafe.photoUrl != null -> AsyncImage(cafe.photoUrl, "Foto ${cafe.name}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            cafe.imageResId != null -> Image(painterResource(cafe.imageResId), "Foto ${cafe.name}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))))
        Surface(Modifier.align(Alignment.BottomEnd).padding(16.dp), shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), shadowElevation = 4.dp) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Filled.Star, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(cafe.rating?.let { String.format(Locale.US, "%.1f / 5", it) } ?: "Belum ada ulasan",
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CafeVibes(names: List<String>) {
    if (names.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        names.forEach { name ->
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Text(name, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
