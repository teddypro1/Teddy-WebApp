package com.teddy.bpositive.security

import com.teddy.bpositive.model.Note
import org.json.JSONObject

/**
 * Versioned serialization boundary for encrypted note fields and complete local notes.
 *
 * Callers must only use this codec after the matching user's Vault has been unlocked.
 */
object NoteEncryptionCodec {

    private const val FORMAT_VERSION = 1
    const val CLOUD_ENVELOPE_PREFIX = "teddy-note-encrypted-v1:"

    fun toCloudContent(encryptedPayload: String): String {
        require(encryptedPayload.isNotBlank())
        return CLOUD_ENVELOPE_PREFIX + encryptedPayload
    }

    fun extractCloudPayload(content: String): String? =
        content.takeIf { it.startsWith(CLOUD_ENVELOPE_PREFIX) }
            ?.removePrefix(CLOUD_ENVELOPE_PREFIX)?.takeIf { it.isNotBlank() }

    data class PlainNote(
        val title: String,
        val content: String
    )

    /**
     * Encrypts title and content independently using fresh AES-GCM nonces.
     * The returned JSON contains ciphertext only, not plaintext note fields.
     */
    fun encode(userId: String, title: String, content: String): String {
        val key = VaultSessionManager.getMasterKeyCopy(userId)
            ?: throw IllegalStateException("Unlock Secure Vault before encrypting notes.")

        return try {
            JSONObject()
                .put("format_version", FORMAT_VERSION)
                .put("title", NotesEncryption.encryptNoteField(key, title))
                .put("content", NotesEncryption.encryptNoteField(key, content))
                .toString()
        } finally {
            key.fill(0)
        }
    }

    /**
     * Authenticates and decrypts both note fields.
     * Any malformed payload, wrong key or modified ciphertext fails closed.
     */
    fun decode(userId: String, encryptedPayload: String): PlainNote {
        val key = VaultSessionManager.getMasterKeyCopy(userId)
            ?: throw IllegalStateException("Unlock Secure Vault before decrypting notes.")

        return try {
            val json = JSONObject(encryptedPayload)
            require(json.getInt("format_version") == FORMAT_VERSION) {
                "Unsupported encrypted note payload version."
            }

            PlainNote(
                title = NotesEncryption.decryptNoteField(key, json.getString("title")),
                content = NotesEncryption.decryptNoteField(key, json.getString("content"))
            )
        } finally {
            key.fill(0)
        }
    }

    /**
     * Encrypts the complete local Note object as one authenticated payload.
     * IDs, pin state, timestamps and color are therefore not exposed as plaintext
     * metadata in the encrypted local snapshot.
     */
    fun encodeNote(userId: String, note: Note): String {
        require(userId.isNotBlank()) { "User ID cannot be empty." }
        require(note.id > 0L) { "Local note ID is invalid." }
        require(note.colorHex.isNotBlank()) { "Local note color is invalid." }

        val key = VaultSessionManager.getMasterKeyCopy(userId)
            ?: throw IllegalStateException("Unlock Secure Vault before encrypting notes.")

        return try {
            val plaintext = JSONObject()
                .put("id", note.id)
                .put("title", note.title)
                .put("content", note.content)
                .put("isPinned", note.isPinned)
                .put("createdAt", note.createdAt)
                .put("updatedAt", note.updatedAt)
                .put("colorHex", note.colorHex)
                .toString()

            JSONObject()
                .put("format_version", FORMAT_VERSION)
                .put("kind", "full_note")
                .put("payload", NotesEncryption.encryptNoteField(key, plaintext))
                .toString()
        } finally {
            key.fill(0)
        }
    }

    /** Decrypts and validates a complete Note object; authentication failures propagate. */
    fun decodeNote(userId: String, encryptedPayload: String): Note {
        require(userId.isNotBlank()) { "User ID cannot be empty." }
        val key = VaultSessionManager.getMasterKeyCopy(userId)
            ?: throw IllegalStateException("Unlock Secure Vault before decrypting notes.")

        return try {
            val wrapper = JSONObject(encryptedPayload)
            require(wrapper.getInt("format_version") == FORMAT_VERSION) {
                "Unsupported encrypted local-note payload version."
            }
            require(wrapper.getString("kind") == "full_note") {
                "Unsupported encrypted local-note payload type."
            }

            val plaintext = NotesEncryption.decryptNoteField(key, wrapper.getString("payload"))
            val json = JSONObject(plaintext)
            val note = Note(
                id = json.getLong("id"),
                title = json.getString("title"),
                content = json.getString("content"),
                isPinned = json.getBoolean("isPinned"),
                createdAt = json.getLong("createdAt"),
                updatedAt = json.getLong("updatedAt"),
                colorHex = json.getString("colorHex")
            )
            require(note.id > 0L) { "Local note ID is invalid." }
            require(note.colorHex.isNotBlank()) { "Local note color is invalid." }
            note
        } finally {
            key.fill(0)
        }
    }
}
