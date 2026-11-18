package com.teddy.bpositive.model

/**
 * Domain model for a note stored locally and synchronized with the cloud.
 */
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val isPinned: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
