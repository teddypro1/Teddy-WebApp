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
                val jsonObject = JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("content", note.content)
                    put("isPinned", note.isPinned)
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)
                    put("colorHex", note.colorHex)
                }
                jsonArray.put(jsonObject)
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
                for (index in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(index)
                    add(
                        Note(
                            id = jsonObject.optLong("id"),
                            title = jsonObject.optString("title"),
                            content = jsonObject.optString("content"),
                            isPinned = jsonObject.optBoolean("isPinned", false),
                            createdAt = jsonObject.optLong("createdAt"),
                            updatedAt = jsonObject.optLong("updatedAt"),
                            colorHex = jsonObject.optString("colorHex", "#FFFFFF")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
