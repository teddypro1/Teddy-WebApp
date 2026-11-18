package com.teddy.bpositive.data

import android.content.Context
import java.time.Instant

/**
 * Processes the local pending-note queue against Supabase.
 *
 * UPSERTs use optimistic concurrency. If the server version changed since this
 * device last observed it, this device's version is saved to note_conflicts
 * before the latest server version is adopted as the synchronized base.
 */
object NoteSyncRepository {

    private val activeSyncUsers = mutableSetOf<String>()
    private val waitingCallbacks = mutableMapOf<String, MutableList<(SyncResult) -> Unit>>()

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
            onResult(SyncResult(false, 0, 0, "User ID is missing from the session."))
            return
        }

        val appContext = context.applicationContext
        val shouldStart = synchronized(activeSyncUsers) {
            if (userId in activeSyncUsers) {
                waitingCallbacks.getOrPut(userId) { mutableListOf() }.add(onResult)
                false
            } else {
                activeSyncUsers.add(userId)
                true
            }
        }
        if (!shouldStart) return

        val complete: (SyncResult) -> Unit = { result ->
            val callbacks = synchronized(activeSyncUsers) {
                activeSyncUsers.remove(userId)
                waitingCallbacks.remove(userId).orEmpty()
            }
            onResult(result)
            callbacks.forEach { callback -> callback(result) }
        }

        val pending = try {
            NoteSyncStore.getPendingChanges(appContext, userId)
        } catch (e: Exception) {
            complete(SyncResult(false, 0, 0, "Could not read pending changes: ${e.message ?: "Unknown error"}"))
            return
        }

        if (pending.isEmpty()) {
            complete(SyncResult(true, 0, 0, "There are no pending note changes."))
            return
        }

        processNext(appContext, session, pending, 0, 0, complete)
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
            onResult(SyncResult(false, processed, records.size - index, "User ID is missing from the session."))
            return
        }

        if (index >= records.size) {
            val latestPending = NoteSyncStore.getPendingChanges(context, userId)
            if (latestPending.isNotEmpty()) {
                processNext(context, session, latestPending, 0, processed, onResult)
            } else {
                onResult(SyncResult(true, processed, 0, "All pending note changes were synchronized."))
            }
            return
        }

        val record = records[index]
        val next: (Boolean, String) -> Unit = { success, message ->
            if (!success) {
                val remaining = NoteSyncStore.getPendingChanges(context, userId).size
                onResult(SyncResult(false, processed, remaining, message))
            } else {
                processNext(context, session, records, index + 1, processed + 1, onResult)
            }
        }

        if (record.operation == NoteSyncStore.Operation.DELETE) {
            syncDelete(context, session, record, next)
        } else {
            syncUpsert(context, session, record, next)
        }
    }

    private fun syncDelete(
        context: Context,
        session: SupabaseSessionManager.Session,
        record: NoteSyncStore.SyncRecord,
        onComplete: (Boolean, String) -> Unit
    ) {
        SupabaseNotesApi.upsertNote(
            session = session,
            clientId = record.clientId,
            title = record.title,
            content = record.content,
            isPinned = record.isPinned,
            deletedAt = Instant.now().toString()
        ) { result ->
            if (!result.success || result.data == null) {
                onComplete(false, "Sync stopped for local note ${record.localNoteId}: ${result.message}")
                return@upsertNote
            }
            try {
                NoteSyncStore.markDeleteSyncedIfUnchanged(context, session.userId.orEmpty(), record)
                onComplete(true, "")
            } catch (e: Exception) {
                onComplete(false, "Cloud deletion synced, but local sync state could not be updated: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private fun syncUpsert(
        context: Context,
        session: SupabaseSessionManager.Session,
        record: NoteSyncStore.SyncRecord,
        onComplete: (Boolean, String) -> Unit
    ) {
        // Read the server row before writing. A retry/reconnect may leave a queued
        // UPSERT whose payload is already identical to Cloud. Treat that as synced
        // without issuing PATCH/UPSERT, because database updated_at triggers fire
        // on writes even when the note's visible fields have not changed.
        SupabaseNotesApi.fetchNoteByClientId(session, record.clientId) { cloudResult ->
            if (!cloudResult.success) {
                onComplete(false, "Could not inspect cloud note \${record.localNoteId}: \${cloudResult.message}")
                return@fetchNoteByClientId
            }

            val cloudNote = cloudResult.data
            if (cloudNote == null) {
                uploadNewNote(context, session, record, onComplete)
                return@fetchNoteByClientId
            }

            val contentAlreadyMatches =
                cloudNote.userId == session.userId &&
                cloudNote.deletedAt == null &&
                cloudNote.title == record.title.trim() &&
                cloudNote.content == record.content &&
                cloudNote.isPinned == record.isPinned

            if (contentAlreadyMatches) {
                // No content mutation is needed. Adopt the latest server version
                // and clear only this exact queued operation.
                markUpsertSynced(context, session, record, cloudNote, onComplete)
                return@fetchNoteByClientId
            }

            val hasBaseVersion = record.cloudNoteId == cloudNote.id &&
                !record.cloudUpdatedAt.isNullOrBlank()

            if (hasBaseVersion) {
                SupabaseNotesApi.updateNoteIfUnchanged(
                    session = session,
                    noteId = cloudNote.id,
                    expectedUpdatedAt = record.cloudUpdatedAt,
                    title = record.title,
                    content = record.content,
                    isPinned = record.isPinned
                ) { result ->
                    if (!result.success) {
                        onComplete(false, "Sync stopped for local note \${record.localNoteId}: \${result.message}")
                    } else if (result.data != null) {
                        markUpsertSynced(context, session, record, result.data, onComplete)
                    } else {
                        preserveLocalConflictAndAdoptCloud(
                            context, session, record, record.cloudUpdatedAt, onComplete
                        )
                    }
                }
            } else {
                // No trusted base version exists. Preserve the local version as a
                // conflict instead of overwriting an existing server row.
                preserveConflictCopy(
                    context,
                    session,
                    record,
                    baseCloudUpdatedAt = record.cloudUpdatedAt,
                    latestCloud = cloudNote,
                    onComplete = onComplete
                )
            }
        }
    }

    private fun preserveLocalConflictAndAdoptCloud(
        context: Context,
        session: SupabaseSessionManager.Session,
        record: NoteSyncStore.SyncRecord,
        baseCloudUpdatedAt: String?,
        onComplete: (Boolean, String) -> Unit
    ) {
        SupabaseNotesApi.fetchNoteByClientId(session, record.clientId) { result ->
            if (!result.success) {
                onComplete(false, "A version conflict was detected, but the latest cloud version could not be fetched: ${result.message}")
            } else if (result.data == null) {
                // The row disappeared after the conditional update. Recreate it
                // using the stable client ID; there is no competing version left.
                uploadNewNote(context, session, record, onComplete)
            } else {
                preserveConflictCopy(context, session, record, baseCloudUpdatedAt, result.data, onComplete)
            }
        }
    }

    private fun preserveConflictCopy(
        context: Context,
        session: SupabaseSessionManager.Session,
        record: NoteSyncStore.SyncRecord,
        baseCloudUpdatedAt: String?,
        latestCloud: SupabaseNotesApi.CloudNote,
        onComplete: (Boolean, String) -> Unit
    ) {
        SupabaseNotesApi.createConflictCopy(
            session = session,
            originalClientId = record.clientId,
            title = record.title,
            content = record.content,
            isPinned = record.isPinned,
            baseCloudUpdatedAt = baseCloudUpdatedAt,
            observedCloudUpdatedAt = latestCloud.updatedAt
        ) { conflictResult ->
            if (!conflictResult.success || conflictResult.data.isNullOrBlank()) {
                onComplete(false, "Could not preserve the conflicting local version: ${conflictResult.message}")
                return@createConflictCopy
            }

            val userId = session.userId.orEmpty()
            val marked = try {
                NoteSyncStore.markSyncedIfUnchanged(
                    context,
                    userId,
                    record,
                    latestCloud.id,
                    latestCloud.updatedAt
                )
            } catch (e: Exception) {
                onComplete(false, "Conflict copy was saved, but local sync state could not be updated: ${e.message ?: "Unknown error"}")
                return@createConflictCopy
            }

            if (!marked) {
                // The local note changed while the conflict copy was being
                // saved. Leave its newer queue entry intact for another pass.
                onComplete(true, "")
            } else {
                onComplete(true, "")
            }
        }
    }

    private fun uploadNewNote(
        context: Context,
        session: SupabaseSessionManager.Session,
        record: NoteSyncStore.SyncRecord,
        onComplete: (Boolean, String) -> Unit
    ) {
        SupabaseNotesApi.upsertNote(
            session = session,
            clientId = record.clientId,
            title = record.title,
            content = record.content,
            isPinned = record.isPinned,
            deletedAt = null
        ) { result ->
            if (!result.success || result.data == null) {
                onComplete(false, "Could not upload local note ${record.localNoteId}: ${result.message}")
            } else {
                markUpsertSynced(context, session, record, result.data, onComplete)
            }
        }
    }

    private fun markUpsertSynced(
        context: Context,
        session: SupabaseSessionManager.Session,
        record: NoteSyncStore.SyncRecord,
        cloudNote: SupabaseNotesApi.CloudNote,
        onComplete: (Boolean, String) -> Unit
    ) {
        try {
            NoteSyncStore.markSyncedIfUnchanged(
                context,
                session.userId.orEmpty(),
                record,
                cloudNote.id,
                cloudNote.updatedAt
            )
            onComplete(true, "")
        } catch (e: Exception) {
            onComplete(false, "Cloud sync succeeded, but local sync state could not be updated: ${e.message ?: "Unknown error"}")
        }
    }
}
