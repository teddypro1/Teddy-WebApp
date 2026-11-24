package com.teddy.bpositive.security

import org.json.JSONObject

/**
 * Versioned serialization boundary for encrypted note fields.
 *
 * This codec is deliberately not wired into LocalNotesStorage or cloud sync yet.
 * Callers must only use it after the matching user's Vault has been unlocked.
 */
object NoteEncryptionCodec {

    private const val FORMAT_VERSION = 1

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
}
