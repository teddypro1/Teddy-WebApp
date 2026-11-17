package com.teddy.bpositive.data

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * REST API for the authenticated user's profile.
 * Row Level Security is the authority for profile ownership.
 */
object SupabaseProfilesApi {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class Profile(
        val id: String,
        val fullName: String,
        val username: String?,
        val bio: String,
        val avatarUrl: String?,
        val createdAt: String?,
        val updatedAt: String?
    )

    data class ApiResult<T>(
        val success: Boolean,
        val data: T? = null,
        val message: String
    )

    fun fetchOrCreateProfile(
        context: Context,
        session: SupabaseSessionManager.Session,
        onResult: (ApiResult<Profile>) -> Unit
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult(ApiResult(false, message = "User ID is missing from the session."))
            return
        }

        execute(
            method = "GET",
            path = "/rest/v1/profiles?select=id,full_name,username,bio,avatar_url,created_at,updated_at&id=eq.$userId&limit=1",
            context = context,
            session = session,
            onResult = { result ->
                if (!result.success) {
                    onResult(ApiResult(false, message = result.message))
                } else {
                    val existing = result.data?.firstOrNull()
                    if (existing != null) {
                        onResult(ApiResult(true, existing, "Profile loaded."))
                    } else {
                        val initialName = session.email?.substringBefore("@").orEmpty()
                        saveProfile(
                            context = context,
                            session = session,
                            fullName = initialName,
                            username = null,
                            bio = "",
                            avatarUrl = null,
                            onResult = onResult
                        )
                    }
                }
            }
        ) { body ->
            val array = JSONArray(body)
            (0 until array.length()).map { index -> array.getJSONObject(index).toProfile() }
        }
    }

    fun saveProfile(
        context: Context,
        session: SupabaseSessionManager.Session,
        fullName: String,
        username: String?,
        bio: String,
        avatarUrl: String?,
        onResult: (ApiResult<Profile>) -> Unit
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult(ApiResult(false, message = "User ID is missing from the session."))
            return
        }
        val cleanUsername = username?.trim()?.removePrefix("@")?.takeIf { it.isNotBlank() }
        val payload = JSONObject()
            .put("id", userId)
            .put("full_name", fullName.trim())
            .put("username", cleanUsername ?: JSONObject.NULL)
            .put("bio", bio.trim())
            .put("avatar_url", avatarUrl?.trim()?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)

        execute(
            context = context,
            method = "POST",
            path = "/rest/v1/profiles?on_conflict=id&select=id,full_name,username,bio,avatar_url,created_at,updated_at",
            session = session,
            body = payload.toString(),
            prefer = "resolution=merge-duplicates,return=representation",
            onResult = onResult
        ) { body ->
            val array = JSONArray(body)
            if (array.length() == 0) throw IllegalStateException("Profile response was empty.")
            array.getJSONObject(0).toProfile()
        }
    }

    private fun <T> execute(
        context: Context,
        method: String,
        path: String,
        session: SupabaseSessionManager.Session,
        body: String? = null,
        prefer: String? = null,
        onResult: (ApiResult<T>) -> Unit,
        parse: (String) -> T
    ) {
        val appContext = context.applicationContext
        fun send(activeSession: SupabaseSessionManager.Session, retried: Boolean) {
            Thread {
                val builder = Request.Builder()
                    .url("${SupabaseConfig.BASE_URL}$path")
                    .addHeader("apikey", SupabaseConfig.API_KEY)
                    .addHeader("Authorization", "Bearer ${activeSession.accessToken}")
                    .addHeader("Accept", "application/json")
                if (prefer != null) builder.addHeader("Prefer", prefer)
                val requestBody = body?.toRequestBody(jsonMediaType)
                when (method) {
                    "GET" -> builder.get()
                    "POST" -> builder.post(requestBody ?: "{}".toRequestBody(jsonMediaType))
                    else -> throw IllegalArgumentException("Unsupported method: $method")
                }
                val result = try {
                    client.newCall(builder.build()).execute().use { response ->
                        val responseText = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            val error = try { JSONObject(responseText).optString("message") } catch (_: Exception) { "" }
                            ApiResult<T>(false, message = error.ifBlank { "Supabase request failed. HTTP ${response.code}" })
                        } else {
                            try { ApiResult(true, parse(responseText), "Profile loaded.") }
                            catch (e: Exception) { ApiResult<T>(false, message = "Could not parse profile: ${e.message ?: "Unknown error"}") }
                        }
                    }
                } catch (e: Exception) { ApiResult<T>(false, message = "Network error: ${e.message ?: "Unknown error"}") }
                val expired = result.message.contains("JWT expired", true) || result.message.contains("token is expired", true)
                if (!retried && !result.success && expired) {
                    SupabaseAuth.refreshSession(activeSession.refreshToken) { refreshed ->
                        val access = refreshed.accessToken
                        val refresh = refreshed.refreshToken
                        if (!refreshed.success || access.isNullOrBlank() || refresh.isNullOrBlank()) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onResult(ApiResult(false, message = "Session expired. Please sign in again. ${refreshed.message}"))
                            }
                            return@refreshSession
                        }
                        val newSession = SupabaseSessionManager.Session(
                            accessToken = access, refreshToken = refresh,
                            userId = refreshed.userId ?: activeSession.userId,
                            email = refreshed.email ?: activeSession.email
                        )
                        try {
                            SupabaseSessionManager.save(appContext, newSession)
                            send(newSession, true)
                        } catch (e: Exception) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onResult(ApiResult(false, message = "Could not save refreshed session: ${e.message ?: "Unknown error"}"))
                            }
                        }
                    }
                } else {
                    android.os.Handler(android.os.Looper.getMainLooper()).post { onResult(result) }
                }
            }.start()
        }
        send(session, false)
    }
    private fun JSONObject.toProfile() = Profile(
        id = getString("id"),
        fullName = optString("full_name"),
        username = optNullableString("username"),
        bio = optString("bio"),
        avatarUrl = optNullableString("avatar_url"),
        createdAt = optNullableString("created_at"),
        updatedAt = optNullableString("updated_at")
    )

    private fun JSONObject.optNullableString(key: String): String? =
        optString(key).takeIf { it.isNotBlank() && it != "null" }
}
