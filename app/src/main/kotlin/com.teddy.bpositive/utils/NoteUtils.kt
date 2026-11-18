package com.teddy.bpositive.utils

import com.teddy.bpositive.model.Note
import java.time.Instant

/**
 * Shared, side-effect-free note transformations used by the workspace UI.
 */
fun filterAndSortNotes(
    notes: List<Note>,
    searchQuery: String,
    showPinnedOnly: Boolean,
    sortNewest: Boolean
): List<Note> {
    return notes
        .filter { note ->
            (!showPinnedOnly || note.isPinned) &&
                (
                    note.title.contains(searchQuery, ignoreCase = true) ||
                        note.content.contains(searchQuery, ignoreCase = true)
                    )
        }
        .let { filtered ->
            if (sortNewest) {
                filtered.sortedByDescending { it.updatedAt }
            } else {
                filtered.sortedBy { it.updatedAt }
            }
        }
}

fun toggleNotePinned(note: Note, updatedAt: Long = System.currentTimeMillis()): Note {
    return note.copy(
        isPinned = !note.isPinned,
        updatedAt = updatedAt
    )
}

fun parseCloudTimeMillis(value: String?): Long {
    return try {
        if (value.isNullOrBlank()) System.currentTimeMillis()
        else Instant.parse(value).toEpochMilli()
    } catch (_: Exception) {
        System.currentTimeMillis()
    }
}
