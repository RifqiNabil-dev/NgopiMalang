package com.ngopimalang.app.features.reviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ngopimalang.app.core.model.Review

data class ReviewFormUiState(
    val rating: Int = 0,
    val comment: String = "",
    val favoriteMenu: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val validationError: String? = null
)

@Composable
fun ReviewFormDialog(
    existingReview: Review? = null,
    isSaving: Boolean = false,
    externalErrorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (ReviewDraft) -> Unit
) {
    var formState by remember(existingReview) {
        mutableStateOf(
            ReviewFormUiState(
                rating = existingReview?.rating ?: 0,
                comment = existingReview?.comment.orEmpty(),
                favoriteMenu = existingReview?.favoriteMenu.orEmpty()
            )
        )
    }

    val isEditMode = existingReview != null
    val dialogTitle = if (isEditMode) "Edit ulasan" else "Beri ulasan"

    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        title = {
            Text(
                text = dialogTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Rating (wajib 1–5 bintang)",
                    style = MaterialTheme.typography.labelLarge
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (star in 1..5) {
                        val selected = star <= formState.rating
                        IconButton(
                            onClick = {
                                formState = formState.copy(
                                    rating = star,
                                    validationError = null
                                )
                            },
                            enabled = !isSaving
                        ) {
                            Icon(
                                imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "$star Bintang",
                                tint = if (selected) Color(0xFFFFB300) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                formState.validationError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                OutlinedTextField(
                    value = formState.favoriteMenu,
                    onValueChange = { formState = formState.copy(favoriteMenu = it) },
                    label = { Text("Menu Favorit (opsional)") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = formState.comment,
                    onValueChange = { formState = formState.copy(comment = it) },
                    label = { Text("Komentar (opsional)") },
                    minLines = 3,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                )

                val errorToShow = externalErrorMessage ?: formState.errorMessage
                if (!errorToShow.isNullOrBlank()) {
                    Text(
                        text = errorToShow,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = formState.rating in 1..5 && !isSaving,
                onClick = {
                    if (formState.rating !in 1..5) {
                        formState = formState.copy(validationError = "Pilih rating 1 sampai 5 bintang.")
                    } else {
                        onSubmit(
                            ReviewDraft(
                                rating = formState.rating,
                                comment = formState.comment,
                                favoriteMenu = formState.favoriteMenu
                            )
                        )
                    }
                }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isSaving) "Menyimpan..." else "Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Batal")
            }
        }
    )
}