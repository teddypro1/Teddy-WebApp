package com.teddy.bpositive.data

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * REST API for the authenticated user's notes.
 *
 * Row-level security must remain enabled in Supabase. Every request uses the
 * user's access token; user_id is also sent on inserts and filters.
 */
object SupabaseNotesApi {

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class CloudNote(
        val id: Long,
        val userId: String,
        val title: String,
        val content: String,
        val isPinned: Boolean,
        val createdAt: String?,
        val updatedAt: String?
    )

    data class ApiResult<T>(
        val success: Boolean,
        val data: T? = null,
        val message: String
    )

    fun fetchNotes(
        session: SupabaseSessionManager.Session,
        onResult: (ApiResult<List<CloudNote>>) -> Unit
    ) {
        execute(
            method = "GET",
            path = "/rest/v1/notes?select=id,user_id,title,content,is_pinned,created_at,updated_at&user_id=eq.${session.userId.orEmpty()}&order=updated_at.desc",
            session = session,
            onResult = onResult
        ) { body ->
            val array = JSONArray(body)
            val notes = (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                CloudNote(
                    id = item.getLong("id"),
                    userId = item.getString("user_id"),
                    title = item.getString("title"),
                    content = item.getString("content"),
                    isPinned = item.optBoolean("is_pinned", false),
                    createdAt = item.optNullableString("created_at"),
                    updatedAt = item.optNullableString("updated_at")
                )
            }
            notes
        }
    }

    fun createNote(
        session: SupabaseSessionManager.Session,
        title: String,
        content: String,
        isPinned: Boolean = false,
        onResult: (ApiResult<CloudNote?>) -> Unit
    ) {
        if (session.userId.isNullOrBlank()) {
            onResult(ApiResult(false, message = "User ID is missing from the session."))
            return
        }

        val payload = JSONObject()
            .put("user_id", session.userId)
            .put("title", title.trim())
            .put("content", content.trim())
            .put("is_pinned", isPinned)

        execute(
            method = "POST",
            path = "/rest/v1/notes?select=id,user_id,title,content,is_pinned,created_at,updated_at",
            session = session,
            body = payload.toString(),
            prefer = "return=representation",
            onResult = onResult
        ) { body ->
            val array = JSONArray(body)
            if (array.length() == 0) null else array.getJSONObject(0).toCloudNote()
        }
    }

    fun updateNote(
        session: SupabaseSessionManager.Session,
        noteId: Long,
        title: String,
        content: String,
        isPinned: Boolean,
        onResult: (ApiResult<CloudNote?>) -> Unit
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult(ApiResult(false, message = "User ID is missing from the session."))
            return
        }

        val payload = JSONObject()
            .put("title", title.trim())
            .put("content", content.trim())
            .put("is_pinned", isPinned)

        execute(
            method = "PATCH",
            path = "/rest/v1/notes?id=eq.$noteId&user_id=eq.$userId&select=id,user_id,title,content,is_pinned,created_at,updated_at",
            session = session,
            body = payload.toString(),
            prefer = "return=representation",
            onResult = onResult
        ) { body ->
            val array = JSONArray(body)
            if (array.length() == 0) null else array.getJSONObject(0).toCloudNote()
        }
    }

    fun deleteNote(
        session: SupabaseSessionManager.Session,
        noteId: Long,
        onResult: (ApiResult<Boolean>) -> Unit
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult(ApiResult(false, message = "User ID is missing from the session."))
            return
        }

        execute(
            method = "DELETE",
            path = "/rest/v1/notes?id=eq.$noteId&user_id=eq.$userId",
            session = session,
            prefer = "return=minimal",
            onResult = onResult
        ) { true }
    }

    private fun <T> execute(
        method: String,
        path: String,
        session: SupabaseSessionManager.Session,
        body: String? = null,
        prefer: String? = null,
        onResult: (ApiResult<T>) -> Unit,
        parse: (String) -> T
    ) {
        Thread {
            val requestBody = body?.toRequestBody(jsonMediaType)
            val builder = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}$path")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .addHeader("Accept", "application/json")

            if (prefer != null) builder.addHeader("Prefer", prefer)

            when (method) {
                "GET" -> builder.get()
                "POST" -> builder.post(requestBody ?: "{}".toRequestBody(jsonMediaType))
                "PATCH" -> builder.patch(requestBody ?: "{}".toRequestBody(jsonMediaType))
                "DELETE" -> builder.delete(requestBody)
                else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
            }

            val result = try {
                client.newCall(builder.build()).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        try {
                            ApiResult(true, parse(responseText), "HTTP ${response.code}")
                        } catch (e: Exception) {
                            ApiResult<T>(false, message = "Could not parse server response: ${e.message ?: "Unknown error"}")
                        }
                    } else {
                        ApiResult<T>(
                            false,
                            message = extractError(responseText, response.code)
                        )
                    }
                }
            } catch (e: Exception) {
                ApiResult<T>(false, message = "Network error: ${e.message ?: "Unknown error"}")
            }

            mainHandler.post { onResult(result) }
        }.start()
    }

    private fun extractError(body: String, statusCode: Int): String {
        val json = try {
            JSONObject(body)
        } catch (_: Exception) {
            JSONObject()
        }
        return json.optString("message").takeIf { it.isNotBlank() }
            ?: json.optString("details").takeIf { it.isNotBlank() }
            ?: json.optString("hint").takeIf { it.isNotBlank() }
            ?: "Supabase request failed. HTTP $statusCode"
    }

    private fun JSONObject.optNullableString(key: String): String? =
        optString(key).takeIf { it.isNotBlank() && it != "null" }

    private fun JSONObject.toCloudNote() = CloudNote(
        id = getLong("id"),
        userId = getString("user_id"),
        title = getString("title"),
        content = getString("content"),
        isPinned = optBoolean("is_pinned", false),
        createdAt = optNullableString("created_at"),
        updatedAt = optNullableString("updated_at")
    )
}
