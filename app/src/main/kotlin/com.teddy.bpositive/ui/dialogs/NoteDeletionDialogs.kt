package com.teddy.bpositive.ui.dialogs
import androidx.compose.ui.tooling.preview.Preview
import com.teddy.bpositive.ui.theme.TeddyAppTheme

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {

    AlertDialog(

        onDismissRequest = onCancel,

        title = {
            Text(
                "Delete Note?",
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Text(
                "Are you sure you want to delete this note? This action cannot be undone."
            )
        },

        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    "Delete",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PermanentDeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete permanently?", fontWeight = FontWeight.Bold) },
        text = {
            Text("This note will be permanently removed from your cloud database. This action cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete permanently", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true, name = "Delete Confirmation")
@Composable
private fun DeleteConfirmationDialogPreview() {
    TeddyAppTheme(darkMode = false) { DeleteConfirmationDialog(onConfirm = {}, onCancel = {}) }
}
@Preview(showBackground = true, name = "Permanent Delete Confirmation")
@Composable
private fun PermanentDeleteConfirmationDialogPreview() {
    TeddyAppTheme(darkMode = false) { PermanentDeleteConfirmationDialog(onConfirm = {}, onCancel = {}) }
}
