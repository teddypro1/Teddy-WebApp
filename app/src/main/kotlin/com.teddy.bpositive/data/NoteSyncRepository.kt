package com.teddy.bpositive.data

import android.content.Context
import java.time.Instant

/**
 * Processes the local pending-note queue against Supabase.
 *
 * This repository is intentionally independent of the UI. It processes one
 * queued operation at a time and removes/marks it only after Supabase confirms
 * the upsert, allowing failed operations to be retried later.
 */
object NoteSyncRepository {

    data class SyncResult(
        val success: Boolean,
        val processed: Int,
        val remaining: Int,
        val message: String
    )

    fun syncPendingChanges(
        context: Context,
        session: SupabaseSessionManager.Session,
        onResult: (SyncResult) -> Unit
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult(
                SyncResult(
                    success = false,
                    processed = 0,
                    remaining = 0,
                    message = "User ID is missing from the session."
                )
            )
            return
        }

        val appContext = context.applicationContext
        val pending = try {
            NoteSyncStore.getPendingChanges(appContext, userId)
        } catch (e: Exception) {
            onResult(
                SyncResult(
                    success = false,
                    processed = 0,
                    remaining = 0,
                    message = "Could not read pending changes: ${e.message ?: "Unknown error"}"
                )
            )
            return
        }

        if (pending.isEmpty()) {
            onResult(
                SyncResult(
                    success = true,
                    processed = 0,
                    remaining = 0,
                    message = "There are no pending note changes."
                )
            )
            return
        }

        processNext(
            context = appContext,
            session = session,
            records = pending,
            index = 0,
            processed = 0,
            onResult = onResult
        )
    }

    private fun processNext(
        context: Context,
        session: SupabaseSessionManager.Session,
        records: List<NoteSyncStore.SyncRecord>,
        index: Int,
        processed: Int,
        onResult: (SyncResult) -> Unit
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult(
                SyncResult(
                    success = false,
                    processed = processed,
                    remaining = records.size - index,
                    message = "User ID is missing from the session."
                )
            )
            return
        }

        if (index >= records.size) {
            val remaining = NoteSyncStore.getPendingChanges(context, userId).size
            onResult(
                SyncResult(
                    success = remaining == 0,
                    processed = processed,
                    remaining = remaining,
                    message = if (remaining == 0) {
                        "All pending note changes were synchronized."
                    } else {
                        "Some note changes are still pending."
                    }
                )
            )
            return
        }

        val record = records[index]
        val isDelete = record.operation == NoteSyncStore.Operation.DELETE
        val deletedAt = if (isDelete) Instant.now().toString() else null

        SupabaseNotesApi.upsertNote(
            session = session,
            clientId = record.clientId,
            title = record.title,
            content = record.content,
            isPinned = record.isPinned,
            deletedAt = deletedAt
        ) { result ->
            if (!result.success || result.data == null) {
                val remaining = NoteSyncStore.getPendingChanges(context, userId).size
                onResult(
                    SyncResult(
                        success = false,
                        processed = processed,
                        remaining = remaining,
                        message = "Sync stopped for local note ${record.localNoteId}: ${result.message}"
                    )
                )
                return@upsertNote
            }

            try {
                if (isDelete) {
                    NoteSyncStore.markDeleteSynced(
                        context,
                        userId,
                        record.localNoteId
                    )
                } else {
                    NoteSyncStore.markSynced(
                        context,
                        userId,
                        record.localNoteId,
                        result.data.id
                    )
                }
            } catch (e: Exception) {
                val remaining = NoteSyncStore.getPendingChanges(context, userId).size
                onResult(
                    SyncResult(
                        success = false,
                        processed = processed,
                        remaining = remaining,
                        message = "Cloud sync succeeded, but local sync state could not be updated: ${e.message ?: "Unknown error"}"
                    )
                )
                return@upsertNote
            }

            processNext(
                context = context,
                session = session,
                records = records,
                index = index + 1,
                processed = processed + 1,
                onResult = onResult
            )
        }
    }
}
