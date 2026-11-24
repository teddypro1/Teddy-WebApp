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
    private const val TEMP_KEY_SUFFIX = "_pending_write"

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

        val noteIds = HashSet<Long>()
        val array = JSONArray()
        notes.forEach { note ->
            require(note.id > 0L) { "Local note ID is invalid." }
            require(note.colorHex.isNotBlank()) { "Local note color is invalid." }
            require(noteIds.add(note.id)) { "Duplicate local note ID detected." }
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

        val prefs = preferences(context)
        val targetKey = storageKey(userId)
        val temporaryKey = targetKey + TEMP_KEY_SUFFIX

        // Persist and authenticate a temporary snapshot before replacing the
        // last known-good encrypted copy.
        check(prefs.edit().putString(temporaryKey, document).commit()) {
            "Encrypted notes could not be staged for saving."
        }

        try {
            val staged = prefs.getString(temporaryKey, null)
                ?: error("Temporary encrypted snapshot is missing.")
            val stagedDocument = JSONObject(staged)
            verifySnapshot(userId, stagedDocument)
            check(loadNotesFromDocument(userId, stagedDocument) == notes) {
                "Encrypted snapshot read-back verification failed."
            }

            check(
                prefs.edit()
                    .putString(targetKey, staged)
                    .remove(temporaryKey)
                    .commit()
            ) { "Verified encrypted notes could not be finalized." }
        } catch (error: Exception) {
            prefs.edit().remove(temporaryKey).commit()
            throw error
        }
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
        require(document.length() == 2 && document.has("format_version") && document.has("notes")) {
            "Encrypted local-notes snapshot structure is invalid."
        }
        val version = document.getInt("format_version")
        require(version == FORMAT_VERSION || version == LEGACY_FORMAT_VERSION) {
            "Unsupported encrypted local-notes format."
        }
        val notes = document.getJSONArray("notes")
        val noteIds = HashSet<Long>()

        for (index in 0 until notes.length()) {
            val item = notes.getJSONObject(index)
            val note = if (version == FORMAT_VERSION) {
                require(item.length() == 1 && item.has("encrypted_note")) {
                    "Encrypted local-note payload structure is invalid."
                }
                NoteEncryptionCodec.decodeNote(userId, item.getString("encrypted_note"))
            } else {
                require(
                    item.length() == 6 &&
                        item.has("id") &&
                        item.has("encrypted_fields") &&
                        item.has("isPinned") &&
                        item.has("createdAt") &&
                        item.has("updatedAt") &&
                        item.has("colorHex")
                ) { "Encrypted local-notes metadata is incomplete or invalid." }

                val id = item.getLong("id")
                require(id > 0L) { "Encrypted local-note ID is invalid." }
                val color = item.getString("colorHex")
                require(color.isNotBlank()) { "Encrypted local-note color metadata is invalid." }

                val fields = NoteEncryptionCodec.decode(
                    userId,
                    item.getJSONObject("encrypted_fields").toString()
                )
                Note(
                    id = id,
                    title = fields.title,
                    content = fields.content,
                    isPinned = item.getBoolean("isPinned"),
                    createdAt = item.getLong("createdAt"),
                    updatedAt = item.getLong("updatedAt"),
                    colorHex = color
                )
            }

            require(note.id > 0L) { "Encrypted local-note ID is invalid." }
            require(note.colorHex.isNotBlank()) { "Encrypted local-note color is invalid." }
            require(noteIds.add(note.id)) { "Duplicate encrypted local-note ID detected." }
        }
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun storageKey(userId: String) = KEY_PREFIX + userId
}
