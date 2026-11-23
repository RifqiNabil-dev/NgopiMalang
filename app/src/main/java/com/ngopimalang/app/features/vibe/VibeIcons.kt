package com.ngopimalang.app.features.vibe

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

val quickIconKeys = listOf("work", "aesthetic", "night", "outdoor", "vintage", "rooftop", "minimal")

/** Kunci tidak dikenal memakai ikon cadangan (Info), tidak crash. */
fun vibeIcon(key: String): ImageVector = when (key) {
    "work" -> Icons.Default.Build
    "aesthetic" -> Icons.Default.Star
    "night" -> Icons.Default.Notifications
    "outdoor" -> Icons.Default.Place
    "vintage" -> Icons.Default.Favorite
    "rooftop" -> Icons.Default.Home
    "minimal" -> Icons.Default.Check
    else -> Icons.Default.Info
}