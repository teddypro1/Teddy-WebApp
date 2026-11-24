package com.teddy.bpositive.data

import android.content.Context
import android.widget.Toast
import com.teddy.bpositive.model.Note
import org.json.JSONArray
import org.json.JSONObject

/**
 * Account-scoped local persistence for notes.
 *
 * The user ID namespace is intentional: data from one account must never be
 * loaded into another account's local workspace.
 */
object LocalNotesStorage {

    fun saveNotes(
        context: Context,
        userId: String,
        notes: List<Note>
    ) {
        if (userId.isBlank()) return

        try {
            val jsonArray = JSONArray()

            notes.forEach { note ->
                val item = JSONObject().apply {
                    put("id", note.id)
                    if (note.isLocked) {
                        put("isLocked", true)
                        put("encryptedPayload", note.encryptedPayload ?: error("Locked note payload missing."))
                    } else {
                        put("isLocked", false)
                        put("title", note.title); put("content", note.content)
                        put("isPinned", note.isPinned); put("createdAt", note.createdAt)
                        put("updatedAt", note.updatedAt); put("colorHex", note.colorHex)
                    }
                }
                jsonArray.put(item)
            }

            context.getSharedPreferences("teddy_notes", Context.MODE_PRIVATE)
                .edit()
                .putString("notes_data_$userId", jsonArray.toString())
                .commit()
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to save notes.", Toast.LENGTH_SHORT).show()
        }
    }

    fun loadNotes(
        context: Context,
        userId: String
    ): List<Note> {
        if (userId.isBlank()) return emptyList()

        return try {
            val preferences = context.getSharedPreferences(
                "teddy_notes",
                Context.MODE_PRIVATE
            )
            val savedData = preferences.getString("notes_data_$userId", "[]") ?: "[]"
            val jsonArray = JSONArray(savedData)

            buildList {
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val id = item.optLong("id")
                    if (item.optBoolean("isLocked", false)) {
                        val payload = item.optString("encryptedPayload")
                        if (payload.isBlank()) error("Locked note payload missing.")
                        val visible = if (com.teddy.bpositive.security.VaultSessionManager.isUnlocked(userId)) {
                            runCatching {
                                com.teddy.bpositive.security.NoteEncryptionCodec.decodeNote(userId, payload)
                                    .copy(id=id, isLocked=true, encryptedPayload=payload, isMasked=false)
                            }.getOrNull()
                        } else null
                        add(visible ?: Note(id, "Locked note", "Unlock Secure Vault to view this note.", false, 0L, 0L,
                            isLocked=true, encryptedPayload=payload, isMasked=true))
                    } else add(Note(
                        id=id, title=item.optString("title"), content=item.optString("content"),
                        isPinned=item.optBoolean("isPinned", false), createdAt=item.optLong("createdAt"),
                        updatedAt=item.optLong("updatedAt"), colorHex=item.optString("colorHex", "#FFFFFF")
                    ))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
