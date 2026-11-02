package com.ngopimalang.app.features.directory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ngopimalang.app.core.model.Cafe

@Composable
fun DirectoryDetailScreen(cafe: Cafe?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TextButton(onClick = onBack) { Text("Kembali ke direktori") }
        if (cafe == null) {
            Text("Cafe tidak ditemukan", style = MaterialTheme.typography.headlineSmall)
            Text("Kembali ke direktori untuk memilih cafe lain.")
            return@Column
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center
        ) {
            Text(
                cafe.name.take(1).uppercase(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(cafe.name, style = MaterialTheme.typography.headlineMedium)
        Text("Rating ${cafe.rating} / 5", style = MaterialTheme.typography.titleMedium)
        DetailField("Alamat", cafe.address)
        DetailField("Kisaran harga", cafe.priceRange)
        DetailField("Wi-Fi", if (cafe.hasWifi) "Tersedia" else "Tidak tersedia")
        DetailField("Colokan listrik", if (cafe.plugCount > 0) "${cafe.plugCount} colokan" else "Tidak tersedia")
        DetailField("Suasana", cafe.vibes.joinToString(" • ").ifBlank { "Belum ada informasi" })
        Text(
            "Data contoh Progress 1. Foto dan kualitas koneksi Wi-Fi belum tersedia.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
