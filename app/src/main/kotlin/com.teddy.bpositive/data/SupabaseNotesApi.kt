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
 * RLS remains the security boundary. Every request uses the user's access
 * token and scopes records to the authenticated user.
 */
object SupabaseNotesApi {

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class CloudNote(
        val id: Long,
        val userId: String,
        val clientId: String?,
        val title: String,
        val content: String,
        val isPinned: Boolean,
        val createdAt: String?,
        val updatedAt: String?,
        val deletedAt: String?
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
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult(ApiResult(false, message = "User ID is missing from the session."))
            return
        }

        execute(
            method = "GET",
            path = "/rest/v1/notes?select=id,user_id,client_id,title,content,is_pinned,created_at,updated_at,deleted_at&user_id=eq.$userId&order=updated_at.desc",
            session = session,
            onResult = onResult
        ) { body ->
            val array = JSONArray(body)
            (0 until array.length()).map { index ->
                array.getJSONObject(index).toCloudNote()
            }
        }
    }

    /**
     * Inserts a note or updates the existing row identified by
     * (user_id, client_id). Supplying deletedAt creates/updates a tombstone.
     *
     * This operation is idempotent, so retrying a pending sync will not
     * create duplicate cloud notes.
     */
    fun upsertNote(
        session: SupabaseSessionManager.Session,
        clientId: String,
        title: String,
        content: String,
        isPinned: Boolean,
        deletedAt: String? = null,
        onResult: (ApiResult<CloudNote?>) -> Unit
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult(ApiResult(false, message = "User ID is missing from the session."))
            return
        }
        if (clientId.isBlank()) {
            onResult(ApiResult(false, message = "Client ID cannot be empty."))
            return
        }

        val payload = JSONObject()
            .put("user_id", userId)
            .put("client_id", clientId)
            .put("title", title.trim())
            .put("content", content)
            .put("is_pinned", isPinned)
            .put("deleted_at", deletedAt ?: JSONObject.NULL)

        execute(
            method = "POST",
            path = "/rest/v1/notes?on_conflict=user_id,client_id&select=id,user_id,client_id,title,content,is_pinned,created_at,updated_at,deleted_at",
            session = session,
            body = payload.toString(),
            prefer = "resolution=merge-duplicates,return=representation",
            onResult = onResult
        ) { body ->
            val array = JSONArray(body)
            if (array.length() == 0) null else array.getJSONObject(0).toCloudNote()
        }
    }

    fun createNote(
        session: SupabaseSessionManager.Session,
        clientId: String,
        title: String,
        content: String,
        isPinned: Boolean = false,
        onResult: (ApiResult<CloudNote?>) -> Unit
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult(ApiResult(false, message = "User ID is missing from the session."))
            return
        }
        if (clientId.isBlank()) {
            onResult(ApiResult(false, message = "Client ID cannot be empty."))
            return
        }

        val payload = JSONObject()
            .put("user_id", userId)
            .put("client_id", clientId)
            .put("title", title.trim())
            .put("content", content.trim())
            .put("is_pinned", isPinned)

        execute(
            method = "POST",
            path = "/rest/v1/notes?select=id,user_id,client_id,title,content,is_pinned,created_at,updated_at,deleted_at",
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
            .put("deleted_at", JSONObject.NULL)

        execute(
            method = "PATCH",
            path = "/rest/v1/notes?id=eq.$noteId&user_id=eq.$userId&select=id,user_id,client_id,title,content,is_pinned,created_at,updated_at,deleted_at",
            session = session,
            body = payload.toString(),
            prefer = "return=representation",
            onResult = onResult
        ) { body ->
            val array = JSONArray(body)
            if (array.length() == 0) null else array.getJSONObject(0).toCloudNote()
        }
    }

    /**
     * Soft-deletes a note. The tombstone is retained for cross-device sync.
     */
    fun deleteNote(
        session: SupabaseSessionManager.Session,
        noteId: Long,
        onResult: (ApiResult<CloudNote?>) -> Unit
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult(ApiResult(false, message = "User ID is missing from the session."))
            return
        }

        val payload = JSONObject().put("deleted_at", "now()")
        // PostgREST expects a timestamp value, so send an ISO-8601 UTC timestamp.
        val deletedAt = java.time.Instant.now().toString()
        payload.put("deleted_at", deletedAt)

        execute(
            method = "PATCH",
            path = "/rest/v1/notes?id=eq.$noteId&user_id=eq.$userId&select=id,user_id,client_id,title,content,is_pinned,created_at,updated_at,deleted_at",
            session = session,
            body = payload.toString(),
            prefer = "return=representation",
            onResult = onResult
        ) { body ->
            val array = JSONArray(body)
            if (array.length() == 0) null else array.getJSONObject(0).toCloudNote()
        }
    }

    /**
     * Permanently removes a note row. Supabase RLS must allow DELETE for its owner.
     */
    fun permanentlyDeleteNote(
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
            path = "/rest/v1/notes?id=eq.$noteId&user_id=eq.$userId&select=id",
            session = session,
            prefer = "return=representation",
            onResult = onResult
        ) { body ->
            JSONArray(body).length() > 0
        }
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
                "DELETE" -> builder.delete()
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
        clientId = optNullableString("client_id"),
        title = getString("title"),
        content = getString("content"),
        isPinned = optBoolean("is_pinned", false),
        createdAt = optNullableString("created_at"),
        updatedAt = optNullableString("updated_at"),
        deletedAt = optNullableString("deleted_at")
    )
}
