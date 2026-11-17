package com.teddy.bpositive.data

import android.content.Context
import org.json.JSONObject
import java.util.UUID

/**
 * Persists local-to-cloud note identifiers and queued sync operations.
 *
 * This store deliberately does not perform network requests. It only records
 * what must be synchronized; a sync worker/repository can process the queue
 * later when a valid Supabase session is available.
 */
object NoteSyncStore {

    private const val PREFS_NAME = "teddy_note_sync_metadata"
    private const val DATA_KEY = "users"

    enum class Operation {
        UPSERT,
        DELETE
    }

    data class SyncRecord(
        val localNoteId: Long,
        val clientId: String,
        val operation: Operation?,
        val title: String,
        val content: String,
        val isPinned: Boolean,
        val cloudNoteId: Long?,
        val cloudUpdatedAt: String?
    )

    /**
     * Returns the stable client ID for a local note, creating and persisting
     * one when this note has not been registered for this user before.
     */
    @Synchronized
    fun getOrCreateClientId(
        context: Context,
        userId: String,
        localNoteId: Long
    ): String {
        require(userId.isNotBlank()) { "User ID cannot be empty." }

        val root = readRoot(context)
        val userRecords = root.optJSONObject(userId) ?: JSONObject()
        val localKey = localNoteId.toString()
        val existing = userRecords.optJSONObject(localKey)

        if (existing != null) {
            val existingClientId = existing.optString("client_id")
            if (existingClientId.isNotBlank()) return existingClientId
        }

        val clientId = UUID.randomUUID().toString()
        val record = existing ?: JSONObject()
        record.put("local_note_id", localNoteId)
        record.put("client_id", clientId)
        if (!record.has("operation")) record.put("operation", JSONObject.NULL)
        if (!record.has("title")) record.put("title", "")
        if (!record.has("content")) record.put("content", "")
        if (!record.has("is_pinned")) record.put("is_pinned", false)
        userRecords.put(localKey, record)
        root.put(userId, userRecords)
        writeRoot(context, root)
        return clientId
    }

    /** Adds or replaces the pending UPSERT operation for a local note. */
    @Synchronized
    fun enqueueUpsert(
        context: Context,
        userId: String,
        localNoteId: Long,
        title: String,
        content: String,
        isPinned: Boolean
    ): SyncRecord {
        val root = readRoot(context)
        val userRecords = root.optJSONObject(userId) ?: JSONObject()
        val localKey = localNoteId.toString()
        val stored = userRecords.optJSONObject(localKey) ?: JSONObject()
        val clientId = stored.optString("client_id").ifBlank {
            UUID.randomUUID().toString()
        }

        stored.put("local_note_id", localNoteId)
        stored.put("client_id", clientId)
        stored.put("operation", Operation.UPSERT.name)
        stored.put("title", title)
        stored.put("content", content)
        stored.put("is_pinned", isPinned)
        userRecords.put(localKey, stored)
        root.put(userId, userRecords)
        writeRoot(context, root)

        return stored.toSyncRecord(localNoteId)
    }

    /**
     * Queues a DELETE tombstone before the local note is removed.
     * The last note contents are retained in case the cloud row needs to be
     * created as a tombstone during the first synchronization.
     */
    @Synchronized
    fun enqueueDelete(
        context: Context,
        userId: String,
        localNoteId: Long,
        title: String,
        content: String,
        isPinned: Boolean
    ): SyncRecord {
        val root = readRoot(context)
        val userRecords = root.optJSONObject(userId) ?: JSONObject()
        val localKey = localNoteId.toString()
        val stored = userRecords.optJSONObject(localKey) ?: JSONObject()
        val clientId = stored.optString("client_id").ifBlank {
            UUID.randomUUID().toString()
        }

        stored.put("local_note_id", localNoteId)
        stored.put("client_id", clientId)
        stored.put("operation", Operation.DELETE.name)
        stored.put("title", title)
        stored.put("content", content)
        stored.put("is_pinned", isPinned)
        userRecords.put(localKey, stored)
        root.put(userId, userRecords)
        writeRoot(context, root)

        return stored.toSyncRecord(localNoteId)
    }

    /** Returns all queued operations for one authenticated user. */
    @Synchronized
    fun getPendingChanges(context: Context, userId: String): List<SyncRecord> {
        if (userId.isBlank()) return emptyList()

        val userRecords = readRoot(context).optJSONObject(userId) ?: return emptyList()
        val keys = userRecords.keys()
        val records = mutableListOf<SyncRecord>()

        while (keys.hasNext()) {
            val key = keys.next()
            val json = userRecords.optJSONObject(key) ?: continue
            val localNoteId = json.optLong("local_note_id", key.toLongOrNull() ?: continue)
            val operation = json.optString("operation")
            if (operation == Operation.UPSERT.name || operation == Operation.DELETE.name) {
                records += json.toSyncRecord(localNoteId)
            }
        }

        return records
    }

    /** Returns stored metadata for a local note, whether pending or synced. */
    @Synchronized
    fun getRecord(
        context: Context,
        userId: String,
        localNoteId: Long
    ): SyncRecord? {
        if (userId.isBlank()) return null
        val json = readRoot(context)
            .optJSONObject(userId)
            ?.optJSONObject(localNoteId.toString())
            ?: return null
        return json.toSyncRecord(localNoteId)
    }

    /** Finds a local sync record using its stable cross-device client ID. */
    @Synchronized
    fun findByClientId(
        context: Context,
        userId: String,
        clientId: String
    ): SyncRecord? {
        if (userId.isBlank() || clientId.isBlank()) return null
        val records = readRoot(context).optJSONObject(userId) ?: return null
        val keys = records.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val json = records.optJSONObject(key) ?: continue
            if (json.optString("client_id") == clientId) {
                val localId = json.optLong("local_note_id", key.toLongOrNull() ?: continue)
                return json.toSyncRecord(localId)
            }
        }
        return null
    }

    /** Registers a note received from the cloud without adding a pending upload. */
    @Synchronized
    fun registerCloudNote(
        context: Context,
        userId: String,
        localNoteId: Long,
        clientId: String,
        cloudNoteId: Long,
        title: String,
        content: String,
        isPinned: Boolean,
        cloudUpdatedAt: String? = null
    ) {
        require(userId.isNotBlank()) { "User ID cannot be empty." }
        require(clientId.isNotBlank()) { "Client ID cannot be empty." }

        val root = readRoot(context)
        val userRecords = root.optJSONObject(userId) ?: JSONObject()
        val record = userRecords.optJSONObject(localNoteId.toString()) ?: JSONObject()
        record.put("local_note_id", localNoteId)
        record.put("client_id", clientId)
        record.put("cloud_note_id", cloudNoteId)
        if (cloudUpdatedAt != null) record.put("cloud_updated_at", cloudUpdatedAt)
        record.put("operation", JSONObject.NULL)
        record.put("title", title)
        record.put("content", content)
        record.put("is_pinned", isPinned)
        userRecords.put(localNoteId.toString(), record)
        root.put(userId, userRecords)
        writeRoot(context, root)
    }

    /**
     * Marks a queued UPSERT as synchronized only if it has not changed
     * while the network request was in flight.
     */
    @Synchronized
    fun markSyncedIfUnchanged(
        context: Context,
        userId: String,
        expected: SyncRecord,
        cloudNoteId: Long,
        cloudUpdatedAt: String?
    ): Boolean {
        val root = readRoot(context)
        val userRecords = root.optJSONObject(userId) ?: return false
        val json = userRecords.optJSONObject(expected.localNoteId.toString()) ?: return false
        val current = json.toSyncRecord(expected.localNoteId)
        if (
            current.clientId != expected.clientId ||
            current.operation != Operation.UPSERT ||
            current.title != expected.title ||
            current.content != expected.content ||
            current.isPinned != expected.isPinned
        ) return false

        json.put("cloud_note_id", cloudNoteId)
        if (cloudUpdatedAt != null) json.put("cloud_updated_at", cloudUpdatedAt)
        json.put("operation", JSONObject.NULL)
        userRecords.put(expected.localNoteId.toString(), json)
        root.put(userId, userRecords)
        writeRoot(context, root)
        return true
    }

    /** Removes a DELETE tombstone only if it is still the queued operation. */
    @Synchronized
    fun markDeleteSyncedIfUnchanged(
        context: Context,
        userId: String,
        expected: SyncRecord
    ): Boolean {
        val root = readRoot(context)
        val userRecords = root.optJSONObject(userId) ?: return false
        val json = userRecords.optJSONObject(expected.localNoteId.toString()) ?: return false
        val current = json.toSyncRecord(expected.localNoteId)
        if (
            current.clientId != expected.clientId ||
            current.operation != Operation.DELETE ||
            current.title != expected.title ||
            current.content != expected.content ||
            current.isPinned != expected.isPinned
        ) return false

        userRecords.remove(expected.localNoteId.toString())
        if (userRecords.length() == 0) root.remove(userId) else root.put(userId, userRecords)
        writeRoot(context, root)
        return true
    }

    /** Marks an UPSERT as synchronized and remembers its Supabase row ID. */
    @Synchronized
    fun markSynced(
        context: Context,
        userId: String,
        localNoteId: Long,
        cloudNoteId: Long
    ) {
        updateStoredRecord(context, userId, localNoteId) { json ->
            json.put("cloud_note_id", cloudNoteId)
            json.put("operation", JSONObject.NULL)
        }
    }

    /** Removes a DELETE tombstone after the server confirms the deletion. */
    @Synchronized
    fun markDeleteSynced(
        context: Context,
        userId: String,
        localNoteId: Long
    ) {
        if (userId.isBlank()) return
        val root = readRoot(context)
        val userRecords = root.optJSONObject(userId) ?: return
        userRecords.remove(localNoteId.toString())
        if (userRecords.length() == 0) {
            root.remove(userId)
        } else {
            root.put(userId, userRecords)
        }
        writeRoot(context, root)
    }

    private fun updateStoredRecord(
        context: Context,
        userId: String,
        localNoteId: Long,
        update: (JSONObject) -> Unit
    ) {
        if (userId.isBlank()) return
        val root = readRoot(context)
        val userRecords = root.optJSONObject(userId) ?: return
        val json = userRecords.optJSONObject(localNoteId.toString()) ?: return
        update(json)
        userRecords.put(localNoteId.toString(), json)
        root.put(userId, userRecords)
        writeRoot(context, root)
    }

    private fun readRoot(context: Context): JSONObject {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(DATA_KEY, null)
        return try {
            if (stored.isNullOrBlank()) JSONObject() else JSONObject(stored)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun writeRoot(context: Context, root: JSONObject) {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(DATA_KEY, root.toString())
            .commit()
        check(saved) { "Could not persist note sync metadata." }
    }

    private fun JSONObject.toSyncRecord(localNoteId: Long): SyncRecord {
        val operationName = optString("operation")
        val operation = Operation.values().firstOrNull { it.name == operationName }

        return SyncRecord(
            localNoteId = localNoteId,
            clientId = optString("client_id"),
            operation = operation,
            title = optString("title"),
            content = optString("content"),
            isPinned = optBoolean("is_pinned", false),
            cloudNoteId = if (isNull("cloud_note_id")) null else optLong("cloud_note_id"),
            cloudUpdatedAt = optString("cloud_updated_at").takeIf { it.isNotBlank() && it != "null" }
        )
    }
}
