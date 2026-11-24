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
 * Badge status hanya ditampilkan bila [showStatus] true, supaya dipakai di daftar
 * dengan tinggi kartu yang sama seperti sebelumnya.
 */
@Composable
fun EventCard(
    event: EventPromo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    showStatus: Boolean = true
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                Text(
                    text = event.schedule,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
            Text(
                text = event.cafeName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
