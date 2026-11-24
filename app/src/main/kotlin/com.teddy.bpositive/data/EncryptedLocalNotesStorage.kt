package com.teddy.bpositive.data

import android.content.Context
import com.teddy.bpositive.model.Note
import com.teddy.bpositive.security.NoteEncryptionCodec
import org.json.JSONArray
import org.json.JSONObject

/**
 * Account-scoped encrypted local-note storage foundation.
 *
 * This store is intentionally separate from LocalNotesStorage until the app's
 * note editor, pending-sync queue, realtime and conflict flows are migrated
 * together. It never deletes or overwrites the legacy plaintext cache.
 */
object EncryptedLocalNotesStorage {

    private const val PREFS_NAME = "teddy_notes"
    private const val KEY_PREFIX = "encrypted_notes_data_v1_"
    private const val FORMAT_VERSION = 2
    private const val LEGACY_FORMAT_VERSION = 1

    sealed class MigrationResult {
        data class Staged(val noteCount: Int) : MigrationResult()
        data class Refreshed(val noteCount: Int) : MigrationResult()
        object AlreadyStaged : MigrationResult()
        object NoLegacyNotes : MigrationResult()
    }

    /**
     * Writes a version-2 encrypted snapshot. The complete Note object,
     * including title, content and metadata, is encrypted as one authenticated payload.
     */
    @Synchronized
    fun saveNotes(context: Context, userId: String, notes: List<Note>) {
        require(userId.isNotBlank()) { "Account ID is missing." }

        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject().put(
                    "encrypted_note",
                    NoteEncryptionCodec.encodeNote(userId, note)
                )
            )
        }

        val document = JSONObject()
            .put("format_version", FORMAT_VERSION)
            .put("notes", array)
            .toString()

        val written = preferences(context).edit()
            .putString(storageKey(userId), document)
            .commit()
        check(written) { "Encrypted notes could not be saved." }
    }

    /** Returns null when no encrypted snapshot exists; decryption errors propagate. */
    @Synchronized
    fun loadNotes(context: Context, userId: String): List<Note>? {
        require(userId.isNotBlank()) { "Account ID is missing." }
        val saved = preferences(context).getString(storageKey(userId), null) ?: return null
        val document = JSONObject(saved)
        verifySnapshot(userId, document)

        val version = document.getInt("format_version")
        val array = document.getJSONArray("notes")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                if (version == FORMAT_VERSION) {
                    add(NoteEncryptionCodec.decodeNote(userId, item.getString("encrypted_note")))
                } else {
                    val fields = NoteEncryptionCodec.decode(
                        userId,
                        item.getJSONObject("encrypted_fields").toString()
                    )
                    add(
                        Note(
                            id = item.getLong("id"),
                            title = fields.title,
                            content = fields.content,
                            isPinned = item.getBoolean("isPinned"),
                            createdAt = item.getLong("createdAt"),
                            updatedAt = item.getLong("updatedAt"),
                            colorHex = item.getString("colorHex")
                        )
                    )
                }
            }
        }
    }

    /**
     * Copies the current plaintext cache into encrypted storage and verifies
     * every field after reading it back. If the source changed since a previous
     * staging run, the encrypted snapshot is refreshed. The plaintext source
     * is deliberately retained and is never deleted or overwritten.
     */
    @Synchronized
    fun stageLegacyMigration(context: Context, userId: String): MigrationResult {
        require(userId.isNotBlank()) { "Account ID is missing." }
        val prefs = preferences(context)
        val legacy = LocalNotesStorage.loadNotes(context, userId)
        val existing = prefs.getString(storageKey(userId), null)

        if (existing != null) {
            val existingDocument = JSONObject(existing)
            // Reject malformed or unauthenticated existing snapshots before writing.
            verifySnapshot(userId, existingDocument)

            if (
                existingDocument.getInt("format_version") == FORMAT_VERSION &&
                snapshotMatchesLegacyNotes(userId, existingDocument, legacy)
            ) {
                return MigrationResult.AlreadyStaged
            }

            // An empty legacy cache is ambiguous: it may have been cleared after
            // staging. Preserve the existing encrypted copy instead of replacing it.
            if (legacy.isEmpty()) {
                return MigrationResult.AlreadyStaged
            }
        } else if (legacy.isEmpty()) {
            return MigrationResult.NoLegacyNotes
        }

        saveNotes(context, userId, legacy)
        val restored = loadNotes(context, userId)
            ?: error("Encrypted migration snapshot was not written.")

        check(restored == legacy) {
            "Encrypted migration verification failed; legacy notes were preserved."
        }

        return if (existing == null) {
            MigrationResult.Staged(legacy.size)
        } else {
            MigrationResult.Refreshed(legacy.size)
        }
    }

    @Synchronized
    fun hasEncryptedSnapshot(context: Context, userId: String): Boolean {
        if (userId.isBlank()) return false
        return preferences(context).contains(storageKey(userId))
    }

    private fun snapshotMatchesLegacyNotes(
        userId: String,
        snapshot: JSONObject,
        legacyNotes: List<Note>
    ): Boolean {
        return try {
            verifySnapshot(userId, snapshot)
            loadNotesFromDocument(userId, snapshot) == legacyNotes
        } catch (_: Exception) {
            false
        }
    }

    private fun loadNotesFromDocument(userId: String, document: JSONObject): List<Note> {
        val version = document.getInt("format_version")
        val array = document.getJSONArray("notes")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                if (version == FORMAT_VERSION) {
                    add(NoteEncryptionCodec.decodeNote(userId, item.getString("encrypted_note")))
                } else {
                    val fields = NoteEncryptionCodec.decode(
                        userId,
                        item.getJSONObject("encrypted_fields").toString()
                    )
                    add(
                        Note(
                            id = item.getLong("id"),
                            title = fields.title,
                            content = fields.content,
                            isPinned = item.getBoolean("isPinned"),
                            createdAt = item.getLong("createdAt"),
                            updatedAt = item.getLong("updatedAt"),
                            colorHex = item.getString("colorHex")
                        )
                    )
                }
            }
        }
    }

    private fun verifySnapshot(userId: String, document: JSONObject) {
        val version = document.getInt("format_version")
        require(version == FORMAT_VERSION || version == LEGACY_FORMAT_VERSION) {
            "Unsupported encrypted local-notes format."
        }
        val notes = document.getJSONArray("notes")
        for (index in 0 until notes.length()) {
            val item = notes.getJSONObject(index)
            if (version == FORMAT_VERSION) {
                require(item.has("encrypted_note")) {
                    "Encrypted local-note payload is missing."
                }
                NoteEncryptionCodec.decodeNote(userId, item.getString("encrypted_note"))
            } else {
                require(
                    item.has("id") &&
                        item.has("encrypted_fields") &&
                        item.has("isPinned") &&
                        item.has("createdAt") &&
                        item.has("updatedAt") &&
                        item.has("colorHex")
                ) { "Encrypted local-notes metadata is incomplete." }

                require(item.getLong("id") > 0L) {
                    "Encrypted local-note ID is invalid."
                }
                item.getBoolean("isPinned")
                item.getLong("createdAt")
                item.getLong("updatedAt")
                require(item.getString("colorHex").isNotBlank()) {
                    "Encrypted local-note color metadata is invalid."
                }

                val encryptedFields = item.getJSONObject("encrypted_fields")
                NoteEncryptionCodec.decode(userId, encryptedFields.toString())
            }
        }
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun storageKey(userId: String) = KEY_PREFIX + userId
}
