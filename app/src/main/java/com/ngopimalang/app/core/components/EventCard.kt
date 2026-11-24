package com.ngopimalang.app.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ngopimalang.app.core.model.EventCategory
import com.ngopimalang.app.core.model.EventPromo
import com.ngopimalang.app.core.model.EventStatus

/**
 * Kartu event/promo. Pemilik fitur: Rifqi (features.eventpromo).
 *
 * [showStatus]      — tampilkan badge status (UPCOMING/ONGOING/FINISHED). Default true (di daftar event).
 * [showDescription] — tampilkan cuplikan deskripsi. Default true; set false di beranda agar card lebih ringkas.
 * [showSchedule]    — tampilkan baris jadwal. Default true; set false di beranda agar tidak berdempetan.
 */
@Composable
fun EventCard(
    event: EventPromo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    showStatus: Boolean = true,
    showDescription: Boolean = true,
    showSchedule: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Baris atas: badge kategori (kiri) + jadwal singkat (kanan) — jadwal hanya jika showSchedule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (showSchedule) Arrangement.SpaceBetween else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (event.category == EventCategory.PROMO) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = event.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (event.category == EventCategory.PROMO) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (showSchedule) {
                    Text(
                        text = event.schedule,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (showStatus) {
                StatusPill(event.status)
            }
            if (event.cafeName.isNotBlank()) {
                Text(
                    text = event.cafeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showDescription && event.description.isNotBlank()) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Badge status event: Akan datang / Berlangsung / Selesai.
 * Warna diambil dari tema, tidak ada hex di sini (dok 08 bagian 1).
 */
@Composable
private fun StatusPill(status: EventStatus) {
    val container = when (status) {
        EventStatus.UPCOMING -> MaterialTheme.colorScheme.secondaryContainer
        EventStatus.ONGOING -> MaterialTheme.colorScheme.primaryContainer
        EventStatus.FINISHED -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (status) {
        EventStatus.UPCOMING -> MaterialTheme.colorScheme.onSecondaryContainer
        EventStatus.ONGOING -> MaterialTheme.colorScheme.onPrimaryContainer
        EventStatus.FINISHED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = container
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
