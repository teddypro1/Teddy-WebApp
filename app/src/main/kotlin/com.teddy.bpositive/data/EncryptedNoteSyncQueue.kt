package com.teddy.bpositive.data

import android.content.Context
import com.teddy.bpositive.security.NoteEncryptionCodec
import com.teddy.bpositive.security.VaultSessionManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * Encrypted, account-scoped copy of pending note sync records.
 *
 * This is a migration boundary, not yet the active queue used by NoteSyncRepository.
 * The legacy queue is intentionally retained until a later cutover can switch every
 * producer and consumer atomically.
 */
object EncryptedNoteSyncQueue {

    private const val PREFS_NAME = "teddy_encrypted_note_sync_queue"
    private const val DATA_KEY = "users"
    private const val FORMAT_VERSION = 1

    data class MigrationResult(
        val copiedCount: Int,
        val alreadyPresent: Boolean
    )

    /**
     * Copies the current user's pending legacy records into an encrypted snapshot.
     * Every copied record is decrypted and compared before the snapshot is committed.
     * The original plaintext queue is never modified or deleted.
     */
    @Synchronized
    fun stageLegacyMigration(context: Context, userId: String): MigrationResult {
        require(userId.isNotBlank()) { "Account ID is missing." }
        check(VaultSessionManager.isUnlocked(userId)) {
            "Unlock Secure Vault before preparing the encrypted sync queue."
        }

        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val root = readRoot(preferences)
        val existing = root.optJSONObject(userId)
        if (existing != null && existing.optInt("format_version") == FORMAT_VERSION) {
            verifySnapshot(userId, existing)
            return MigrationResult(
                copiedCount = existing.optJSONArray("records")?.length() ?: 0,
                alreadyPresent = true
            )
        }

        val legacyRecords = NoteSyncStore.getPendingChanges(context, userId)
        val encryptedRecords = JSONArray()

        legacyRecords.forEach { record ->
            val encryptedFields = NoteEncryptionCodec.encode(
                userId = userId,
                title = record.title,
                content = record.content
            )
            encryptedRecords.put(
                JSONObject()
                    .put("local_note_id", record.localNoteId)
                    .put("client_id", record.clientId)
                    .put("operation", record.operation?.name ?: JSONObject.NULL)
                    .put("encrypted_fields", JSONObject(encryptedFields).toString())
                    .put("is_pinned", record.isPinned)
                    .put("cloud_note_id", record.cloudNoteId ?: JSONObject.NULL)
                    .put("cloud_updated_at", record.cloudUpdatedAt ?: JSONObject.NULL)
            )
        }

        val snapshot = JSONObject()
            .put("format_version", FORMAT_VERSION)
            .put("records", encryptedRecords)

        verifySnapshot(userId, snapshot)
        legacyRecords.forEachIndexed { index, original ->
            val encryptedRecord = encryptedRecords.getJSONObject(index)
            val decoded = NoteEncryptionCodec.decode(
                userId,
                encryptedRecord.getString("encrypted_fields")
            )
            require(
                decoded.title == original.title &&
                    decoded.content == original.content &&
                    encryptedRecord.getLong("local_note_id") == original.localNoteId &&
                    encryptedRecord.getString("client_id") == original.clientId &&
                    encryptedRecord.optString("operation") == (original.operation?.name ?: "null") &&
                    encryptedRecord.optBoolean("is_pinned", false) == original.isPinned
            ) { "Encrypted sync queue verification did not match the original record." }
        }

        val updatedRoot = readRoot(preferences)
        updatedRoot.put(userId, snapshot)
        check(
            preferences.edit().putString(DATA_KEY, updatedRoot.toString()).commit()
        ) { "Could not persist encrypted sync queue." }

        // Read back from disk and authenticate once more after persistence.
        val persisted = readRoot(preferences).optJSONObject(userId)
            ?: error("Encrypted sync queue was not persisted.")
        verifySnapshot(userId, persisted)

        return MigrationResult(
            copiedCount = encryptedRecords.length(),
            alreadyPresent = false
        )
    }

    /** Returns decrypted staged records only while the matching Vault is unlocked. */
    @Synchronized
    fun loadStagedRecords(context: Context, userId: String): List<NoteSyncStore.SyncRecord> {
        require(userId.isNotBlank()) { "Account ID is missing." }
        check(VaultSessionManager.isUnlocked(userId)) {
            "Unlock Secure Vault before reading the encrypted sync queue."
        }

        val snapshot = readRoot(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        ).optJSONObject(userId) ?: return emptyList()

        require(snapshot.optInt("format_version") == FORMAT_VERSION) {
            "Unsupported encrypted sync queue version."
        }

        val records = snapshot.optJSONArray("records") ?: JSONArray()
        return buildList(records.length()) {
            for (index in 0 until records.length()) {
                val item = records.getJSONObject(index)
                val fields = NoteEncryptionCodec.decode(
                    userId,
                    item.getString("encrypted_fields")
                )
                val operationName = item.optString("operation")
                val operation = NoteSyncStore.Operation.values()
                    .firstOrNull { it.name == operationName }

                add(
                    NoteSyncStore.SyncRecord(
                        localNoteId = item.getLong("local_note_id"),
                        clientId = item.getString("client_id"),
                        operation = operation,
                        title = fields.title,
                        content = fields.content,
                        isPinned = item.optBoolean("is_pinned", false),
                        cloudNoteId = if (item.isNull("cloud_note_id")) null
                            else item.getLong("cloud_note_id"),
                        cloudUpdatedAt = item.optString("cloud_updated_at")
                            .takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }
        }
    }

    private fun verifySnapshot(userId: String, snapshot: JSONObject) {
        require(snapshot.optInt("format_version") == FORMAT_VERSION) {
            "Unsupported encrypted sync queue version."
        }
        val records = snapshot.optJSONArray("records") ?: JSONArray()
        for (index in 0 until records.length()) {
            val item = records.getJSONObject(index)
            val decoded = NoteEncryptionCodec.decode(
                userId,
                item.getString("encrypted_fields")
            )
            require(item.has("local_note_id") && item.has("client_id")) {
                "Encrypted sync queue metadata is incomplete."
            }
            require(decoded.title.isNotEmpty() || decoded.content.isNotEmpty() ||
                item.has("encrypted_fields")) {
                "Encrypted sync queue record verification failed."
            }
        }
    }

    private fun readRoot(
        preferences: android.content.SharedPreferences
    ): JSONObject {
        val stored = preferences.getString(DATA_KEY, null)
        return if (stored.isNullOrBlank()) JSONObject() else JSONObject(stored)
    }
}
