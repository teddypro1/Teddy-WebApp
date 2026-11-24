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
    private const val FORMAT_VERSION = 1

    sealed class MigrationResult {
        data class Staged(val noteCount: Int) : MigrationResult()
        object AlreadyStaged : MigrationResult()
        object NoLegacyNotes : MigrationResult()
    }

    /**
     * Writes an encrypted snapshot. Note title/content are encrypted; only
     * non-sensitive note metadata remains visible in SharedPreferences.
     */
    @Synchronized
    fun saveNotes(context: Context, userId: String, notes: List<Note>) {
        require(userId.isNotBlank()) { "Account ID is missing." }

        val array = JSONArray()
        notes.forEach { note ->
            val encryptedFields = NoteEncryptionCodec.encode(userId, note.title, note.content)
            val fields = JSONObject(encryptedFields)

            array.put(
                JSONObject()
                    .put("id", note.id)
                    .put("encrypted_fields", fields)
                    .put("isPinned", note.isPinned)
                    .put("createdAt", note.createdAt)
                    .put("updatedAt", note.updatedAt)
                    .put("colorHex", note.colorHex)
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
        require(document.getInt("format_version") == FORMAT_VERSION) {
            "Unsupported encrypted local-notes format."
        }

        val array = document.getJSONArray("notes")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val fields = NoteEncryptionCodec.decode(
                    userId,
                    item.getJSONObject("encrypted_fields").toString()
                )
                add(
                    Note(
                        id = item.getLong("id"),
                        title = fields.title,
                        content = fields.content,
                        isPinned = item.optBoolean("isPinned", false),
                        createdAt = item.optLong("createdAt"),
                        updatedAt = item.optLong("updatedAt"),
                        colorHex = item.optString("colorHex", "#FFFFFF")
                    )
                )
            }
        }
    }

    /**
     * Copies the existing plaintext cache into encrypted storage and verifies
     * a full read-back before reporting success. The original plaintext cache
     * is deliberately retained until a later coordinated cutover Build.
     */
    @Synchronized
    fun stageLegacyMigration(context: Context, userId: String): MigrationResult {
        require(userId.isNotBlank()) { "Account ID is missing." }
        val prefs = preferences(context)
        if (prefs.contains(storageKey(userId))) return MigrationResult.AlreadyStaged

        val legacy = LocalNotesStorage.loadNotes(context, userId)
        if (legacy.isEmpty()) return MigrationResult.NoLegacyNotes

        saveNotes(context, userId, legacy)
        val restored = loadNotes(context, userId)
            ?: error("Encrypted migration snapshot was not written.")

        check(restored == legacy) {
            "Encrypted migration verification failed; legacy notes were preserved."
        }
        return MigrationResult.Staged(legacy.size)
    }

    @Synchronized
    fun hasEncryptedSnapshot(context: Context, userId: String): Boolean {
        if (userId.isBlank()) return false
        return preferences(context).contains(storageKey(userId))
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun storageKey(userId: String) = KEY_PREFIX + userId
}
