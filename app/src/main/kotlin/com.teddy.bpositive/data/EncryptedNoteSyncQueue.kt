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

    sealed class MigrationAuditResult {
        data class MatchesLegacy(val recordCount: Int) : MigrationAuditResult()
        data class LegacyQueueEmpty(val encryptedRecordCount: Int) : MigrationAuditResult()
        data class Mismatch(val legacyRecordCount: Int, val encryptedRecordCount: Int) : MigrationAuditResult()
        object NoEncryptedSnapshot : MigrationAuditResult()
    }

    /**
     * Copies the current user's pending legacy records into an encrypted snapshot.
     * Existing snapshots are compared with the complete current queue and refreshed
     * when the legacy queue has changed. The original plaintext queue is never modified.
     */
    @Synchronized
    fun stageLegacyMigration(context: Context, userId: String): MigrationResult {
        require(userId.isNotBlank()) { "Account ID is missing." }
        check(VaultSessionManager.isUnlocked(userId)) {
            "Unlock Secure Vault before preparing the encrypted sync queue."
        }

        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val legacyRecords = NoteSyncStore.getPendingChanges(context, userId)
        val root = readRoot(preferences)
        val existing = root.optJSONObject(userId)
        if (existing != null && snapshotMatchesLegacyRecords(userId, existing, legacyRecords)) {
            return MigrationResult(
                copiedCount = existing.getJSONArray("records").length(),
                alreadyPresent = true
            )
        }

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

        require(snapshotMatchesLegacyRecords(userId, snapshot, legacyRecords)) {
            "Encrypted sync queue verification did not match the complete original queue."
        }

        // Merge into the latest stored root so another account's snapshot is preserved.
        val updatedRoot = readRoot(preferences)
        updatedRoot.put(userId, snapshot)
        check(
            preferences.edit().putString(DATA_KEY, updatedRoot.toString()).commit()
        ) { "Could not persist encrypted sync queue." }

        // Read back from disk and verify ciphertext plus every record field again.
        val persisted = readRoot(preferences).optJSONObject(userId)
            ?: error("Encrypted sync queue was not persisted.")
        require(snapshotMatchesLegacyRecords(userId, persisted, legacyRecords)) {
            "Persisted encrypted sync queue did not match the original queue."
        }

        return MigrationResult(
            copiedCount = encryptedRecords.length(),
            alreadyPresent = false
        )
    }

    /**
     * Performs a read-only integrity and source-comparison audit of the staged
     * encrypted queue. It never writes, refreshes or deletes either copy.
     */
    @Synchronized
    fun inspectMigration(context: Context, userId: String): MigrationAuditResult {
        require(userId.isNotBlank()) { "Account ID is missing." }
        check(VaultSessionManager.isUnlocked(userId)) {
            "Unlock Secure Vault before auditing the encrypted sync queue."
        }
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val snapshot = readRoot(preferences).optJSONObject(userId)
            ?: return MigrationAuditResult.NoEncryptedSnapshot
        verifySnapshot(userId, snapshot)
        val legacy = NoteSyncStore.getPendingChanges(context, userId)
        val encrypted = loadStagedRecords(context, userId)
        if (legacy.isEmpty()) return MigrationAuditResult.LegacyQueueEmpty(encrypted.size)
        return if (encrypted == legacy) MigrationAuditResult.MatchesLegacy(legacy.size)
        else MigrationAuditResult.Mismatch(legacy.size, encrypted.size)
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

        verifySnapshot(userId, snapshot)
        val records = snapshot.getJSONArray("records")
        return buildList(records.length()) {
            for (index in 0 until records.length()) {
                val item = records.getJSONObject(index)
                val fields = NoteEncryptionCodec.decode(
                    userId,
                    item.getString("encrypted_fields")
                )
                val operation = if (item.isNull("operation")) null
                    else NoteSyncStore.Operation.valueOf(item.getString("operation"))

                add(
                    NoteSyncStore.SyncRecord(
                        localNoteId = item.getLong("local_note_id"),
                        clientId = item.getString("client_id"),
                        operation = operation,
                        title = fields.title,
                        content = fields.content,
                        isPinned = item.getBoolean("is_pinned"),
                        cloudNoteId = if (item.isNull("cloud_note_id")) null
                            else item.getLong("cloud_note_id"),
                        cloudUpdatedAt = if (item.isNull("cloud_updated_at")) null
                            else item.getString("cloud_updated_at")
                    )
                )
            }
        }
    }

    private fun snapshotMatchesLegacyRecords(
        userId: String,
        snapshot: JSONObject,
        legacyRecords: List<NoteSyncStore.SyncRecord>
    ): Boolean {
        return try {
            verifySnapshot(userId, snapshot)
            val records = snapshot.getJSONArray("records")
            if (records.length() != legacyRecords.size) return false

            legacyRecords.forEachIndexed { index, original ->
                val item = records.getJSONObject(index)
                val decoded = NoteEncryptionCodec.decode(
                    userId,
                    item.getString("encrypted_fields")
                )
                val operation = if (item.isNull("operation")) null
                    else NoteSyncStore.Operation.valueOf(item.getString("operation"))
                val cloudNoteId = if (item.isNull("cloud_note_id")) null
                    else item.getLong("cloud_note_id")
                val cloudUpdatedAt = if (item.isNull("cloud_updated_at")) null
                    else item.getString("cloud_updated_at")

                if (
                    decoded.title != original.title ||
                    decoded.content != original.content ||
                    item.getLong("local_note_id") != original.localNoteId ||
                    item.getString("client_id") != original.clientId ||
                    operation != original.operation ||
                    item.getBoolean("is_pinned") != original.isPinned ||
                    cloudNoteId != original.cloudNoteId ||
                    cloudUpdatedAt != original.cloudUpdatedAt
                ) return false
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun verifySnapshot(userId: String, snapshot: JSONObject) {
        require(snapshot.length() == 2 &&
            snapshot.has("format_version") &&
            snapshot.has("records") &&
            snapshot.getInt("format_version") == FORMAT_VERSION) {
            "Encrypted sync queue structure or version is invalid."
        }
        val records = snapshot.getJSONArray("records")
        val localNoteIds = HashSet<Long>()
        val clientIds = HashSet<String>()

        for (index in 0 until records.length()) {
            val item = records.getJSONObject(index)
            require(item.length() == 7 &&
                item.has("local_note_id") &&
                item.has("client_id") &&
                item.has("operation") &&
                item.has("encrypted_fields") &&
                item.has("is_pinned") &&
                item.has("cloud_note_id") &&
                item.has("cloud_updated_at")) {
                "Encrypted sync queue metadata is incomplete or contains unexpected fields."
            }

            val localNoteId = item.getLong("local_note_id")
            require(localNoteId > 0L && localNoteIds.add(localNoteId)) {
                "Encrypted sync queue local note ID is invalid or duplicated."
            }

            val clientId = item.getString("client_id")
            require(clientId.isNotBlank() && clientIds.add(clientId)) {
                "Encrypted sync queue client ID is invalid or duplicated."
            }

            require(!item.isNull("operation")) {
                "Encrypted sync queue operation is missing."
            }
            val operation = NoteSyncStore.Operation.valueOf(item.getString("operation"))
            require(operation == NoteSyncStore.Operation.UPSERT ||
                operation == NoteSyncStore.Operation.DELETE) {
                "Encrypted sync queue operation is unsupported."
            }

            item.getBoolean("is_pinned")
            if (!item.isNull("cloud_note_id")) {
                require(item.getLong("cloud_note_id") > 0L) {
                    "Encrypted sync queue cloud note ID is invalid."
                }
            }
            if (!item.isNull("cloud_updated_at")) {
                require(item.getString("cloud_updated_at").isNotBlank()) {
                    "Encrypted sync queue cloud timestamp is invalid."
                }
            }

            NoteEncryptionCodec.decode(userId, item.getString("encrypted_fields"))
        }
    }

    private fun readRoot(
        preferences: android.content.SharedPreferences
    ): JSONObject {
        val stored = preferences.getString(DATA_KEY, null)
        return if (stored.isNullOrBlank()) JSONObject() else JSONObject(stored)
    }
}
