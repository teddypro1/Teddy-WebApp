package com.teddy.bpositive.ui.screens

import com.teddy.bpositive.model.Note
import com.teddy.bpositive.data.SupabaseNotesApi
import com.teddy.bpositive.ui.components.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun HomeScreen(
    notes: List<Note>,
    deletedNotes: List<SupabaseNotesApi.CloudNote>,
    conflictCopies: List<SupabaseNotesApi.ConflictCopy>,
    contentPadding: PaddingValues,
    searchQuery: String,
    showPinnedOnly: Boolean,
    showTrash: Boolean,
    showConflicts: Boolean,
    sortNewest: Boolean,
    showEditor: Boolean,
    title: String,
    content: String,
    editingId: Long?,
    filteredNotes: List<Note>,
    onSearchQueryChange: (String) -> Unit,
    onShowPinnedOnlyChange: (Boolean) -> Unit,
    onToggleSort: () -> Unit,
    onNewNote: () -> Unit,
    onToggleTrash: () -> Unit,
    onToggleConflicts: () -> Unit,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onEditNote: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onRestoreDeleted: (SupabaseNotesApi.CloudNote) -> Unit,
    onDeletePermanently: (SupabaseNotesApi.CloudNote) -> Unit,
    onRestoreConflict: (SupabaseNotesApi.ConflictCopy) -> Unit,
    onDismissConflict: (SupabaseNotesApi.ConflictCopy) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
    
        DashboardHeader()
    
        Spacer(Modifier.height(18.dp))
    
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
    
            StatsCard(
                modifier = Modifier.weight(1f),
                emoji = "▤",
                label = "Total Notes",
                count = notes.size
            )
    
            StatsCard(
                modifier = Modifier.weight(1f),
                emoji = "📌",
                label = "Pinned Notes",
                count = notes.count { it.isPinned }
            )
        }
    
        Spacer(Modifier.height(25.dp))
    
        if (showEditor) {
    
            NoteEditor(
                title = title,
                content = content,
                isEditing = editingId != null,
    
                onTitleChange = onTitleChange,
    
                onContentChange = onContentChange,
    
                onSave = onSave,
    
                onCancel = onCancel
            )
    
            Spacer(Modifier.height(25.dp))
        }
    
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
    
            Column {
                Text(
                    "YOUR COLLECTION",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
    
                Spacer(Modifier.height(5.dp))
    
                Text(
                    if (showConflicts) {
                        "Conflict Copies (${conflictCopies.size})"
                    } else if (showTrash) {
                        "Trash (${deletedNotes.size})"
                    } else if (showPinnedOnly) {
                        "Pinned Notes (${notes.count { it.isPinned }})
                    } else {
                        "All Notes (${notes.size})
                    },
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
            }
    
            if (!showTrash && !showConflicts) Button(
                onClick = onNewNote,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(
                    horizontal = 14.dp,
                    vertical = 10.dp
                )
            ) {
                Text("+ New Note")
            }
        }
    
        Spacer(Modifier.height(16.dp))
    
        OutlinedButton(
            onClick = onToggleTrash,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showTrash) "← Back to All Notes" else "🗑 Recently Deleted (${deletedNotes.size})")
        }
    
        Spacer(Modifier.height(8.dp))
    
        OutlinedButton(
            onClick = onToggleConflicts,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showConflicts) "← Back to All Notes" else "⚠ Conflict Copies (${conflictCopies.size})")
        }
    
        if (!showConflicts && !showTrash) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                onSearchQueryChange(it)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Search your notes...")
            },
            leadingIcon = {
                Text("⌕", fontSize = 25.sp)
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
            )
    
            Spacer(Modifier.height(12.dp))
        }
    
        if (!showConflicts && !showTrash) Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
    
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
    
                FilterChip(
                    selected = !showPinnedOnly,
                    onClick = {
                        onShowPinnedOnlyChange(false)
                    },
                    label = {
                        Text("All Notes")
                    }
                )
    
                FilterChip(
                    selected = showPinnedOnly,
                    onClick = {
                        onShowPinnedOnlyChange(true)
                    },
                    label = {
                        Text("📌 Pinned")
                    }
                )
            }
    
            TextButton(
                onClick = {
                    onToggleSort()
                }
            ) {
                Text(
                    if (sortNewest) "Newest ↓" else "Oldest ↑",
                    fontSize = 12.sp
                )
            }
        }
    
        Spacer(Modifier.height(12.dp))
    
        if (showConflicts) {
            if (conflictCopies.isEmpty()) {
                EmptyNotes()
            } else {
                conflictCopies.forEach { copy ->
                    ConflictCopyCard(
                        conflict = copy,
                        onRestore = { onRestoreConflict(copy) },
                        onDismiss = { onDismissConflict(copy) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        } else if (showTrash) {
            if (deletedNotes.isEmpty()) {
                EmptyNotes()
            } else {
                deletedNotes.forEach { cloudNote ->
                    TrashNoteCard(
                        note = cloudNote,
                        onRestore = { onRestoreDeleted(cloudNote) },
                        onDeletePermanently = { onDeletePermanently(cloudNote) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        } else if (filteredNotes.isEmpty()) {
            EmptyNotes()
        } else {
            filteredNotes.forEach { note ->
                NoteCard(
                    note = note,
                    onEdit = { onEditNote(note) },
                    onPin = { onTogglePin(note) },
                    onDelete = { onDeleteNote(note) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    
        Spacer(Modifier.height(25.dp))
    
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )
    
        Spacer(Modifier.height(15.dp))
    
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
    
            Text(
                "Teddy WebApp",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
    
            Spacer(Modifier.height(5.dp))
    
            Text(
                "Your thoughts, organized beautifully.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
    
            Spacer(Modifier.height(12.dp))
    
            Text(
                "© 2026 Teddy WebApp. All rights reserved.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    
        Spacer(Modifier.height(15.dp))
    }
}
