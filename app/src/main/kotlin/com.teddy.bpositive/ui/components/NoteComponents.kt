package com.teddy.bpositive.ui.components
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.tooling.preview.Preview
import com.teddy.bpositive.ui.theme.TeddyAppTheme

import com.teddy.bpositive.model.Note
import com.teddy.bpositive.data.SupabaseNotesApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Text(value.ifBlank { "Not set" }, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
fun TrashNoteCard(
    note: SupabaseNotesApi.CloudNote,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(note.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(7.dp))
            Text(note.content, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Text("Deleted: ${note.deletedAt ?: "Unknown"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRestore) { Text("↶ Restore") }
                TextButton(onClick = onDeletePermanently) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ConflictCopyCard(
    conflict: SupabaseNotesApi.ConflictCopy,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("⚠ Conflict copy", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(conflict.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(7.dp))
            Text(conflict.content, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, maxLines = 5, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Text("Saved: ${conflict.createdAt ?: "Unknown"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onRestore) {
                    Text("↶ Restore as Note")
                }
            }
        }
    }
}

@Composable
fun DashboardHeader() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF263F9D),
                        Color(0xFF6485F0)
                    )
                ),
                shape = RoundedCornerShape(25.dp)
            )
            .padding(25.dp)
    ) {

        Text(
            "YOUR PERSONAL SPACE",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(Modifier.height(15.dp))

        Text(
            "Organize your thoughts.",
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Organize your thoughts and keep everything in one place.",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            lineHeight = 21.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "✦",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 32.sp
        )
    }
}

@Composable
fun StatsCard(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    count: Int
) {

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(15.dp)
        ) {

            Text(
                emoji,
                fontSize = 23.sp
            )

            Spacer(Modifier.height(9.dp))

            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                count.toString(),
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NoteEditor(
    title: String,
    content: String,
    isEditing: Boolean,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(20.dp)
        ) {

            Text(
                "NOTE EDITOR",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    if (isEditing) "Edit Note" else "Create a Note",
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "✎",
                    fontSize = 27.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                "Note Title",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Give your note a title...")
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    "Note Content",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Text(
                    "${content.length} characters",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp),
                placeholder = {
                    Text("Write your thoughts here...")
                },
                minLines = 5,
                maxLines = 10,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {

                Text(
                    if (isEditing) "Update Note" else "+ Add Note",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun EmptyNotes() {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "📝",
                fontSize = 38.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "No notes available yet.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(5.dp))

            Text(
                "Create your first note to get started!",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val selectedNoteColor = try { Color(android.graphics.Color.parseColor(note.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.surface }
    // In dark theme, note cards follow the app surface instead of retaining a light custom note color.
    val noteBackground = if (isDarkTheme) MaterialTheme.colorScheme.surface else selectedNoteColor
    val noteTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface
        else if (noteBackground.luminance() > 0.5f) Color(0xFF202124) else Color(0xFFF5F5F5)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = noteBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(17.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    note.title,
                    color = noteTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                TextButton(
                    onClick = onPin
                ) {
                    Text(
                        if (note.isPinned) "📌" else "📍",
                        fontSize = 19.sp
                    )
                }
            }

            Spacer(Modifier.height(5.dp))

            Text(
                note.content,
                fontSize = 14.sp,
                color = noteTextColor.copy(alpha = 0.86f),
                lineHeight = 21.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Updated: ${
                    SimpleDateFormat(
                        "dd MMM yyyy, hh:mm a",
                        Locale.getDefault()
                    ).format(Date(note.updatedAt))
                }",
                fontSize = 11.sp,
                color = noteTextColor.copy(alpha = 0.68f)
            )

            Spacer(Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextButton(
                    onClick = onDelete
                ) {
                    Text(
                        "🗑 Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Note Components")
@Composable
private fun NoteComponentsPreview() {
    TeddyAppTheme(darkMode = false) {
        val sampleNote = Note(1L, "A sample note", "Preview content for the note card and reusable components.", true, 1730000000000L, 1730000000000L)
        val cloudNote = SupabaseNotesApi.CloudNote(2L, "preview-user", null, "Deleted sample note", "A deleted note in the Trash preview.", false, null, null, "2026-11-21T10:00:00Z")
        val conflict = SupabaseNotesApi.ConflictCopy("preview-conflict", "preview-client", "Conflict copy", "A local copy preserved after a sync conflict.", false, null, null, null)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ProfileInfoRow("Name", "Preview User")
            DashboardHeader()
            StatsCard(emoji = "📝", label = "Total notes", count = 12)
            NoteCard(note = sampleNote, onEdit = {}, onPin = {}, onDelete = {})
            NoteEditor(title = "Preview title", content = "Preview note content", isEditing = false, onTitleChange = {}, onContentChange = {}, onSave = {}, onCancel = {})
            TrashNoteCard(note = cloudNote, onRestore = {}, onDeletePermanently = {})
            ConflictCopyCard(conflict, onRestore = {}, onDismiss = {})
            EmptyNotes()
        }
    }
}
