package com.teddy.bpositive.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** Authenticated, minimal-field username discovery through a narrowly scoped RPC. */
object UserSearchApi {
    private val client = OkHttpClient()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val mainHandler = Handler(Looper.getMainLooper())

    data class PublicProfile(
        val id: String,
        val fullName: String,
        val username: String,
        val bio: String,
        val avatarUrl: String?
    )

    data class Result(
        val success: Boolean,
        val profiles: List<PublicProfile> = emptyList(),
        val message: String
    )

    fun search(
        context: Context,
        session: SupabaseSessionManager.Session,
        query: String,
        onResult: (Result) -> Unit
    ) {
        val cleanQuery = query.trim().removePrefix("@")
        if (cleanQuery.length < 2) {
            onResult(Result(false, message = "Enter at least 2 characters."))
            return
        }
        SupabaseSessionManager.load(context.applicationContext)

        fun send(activeSession: SupabaseSessionManager.Session, retried: Boolean) {
            Thread {
                val payload = JSONObject()
                    .put("search_query", cleanQuery.take(30))
                    .put("result_limit", 20)
                    .toString()
                    .toRequestBody(jsonType)

                val responseResult = try {
                    val request = Request.Builder()
                        .url("${SupabaseConfig.BASE_URL}/rest/v1/rpc/search_public_profiles")
                        .addHeader("apikey", SupabaseConfig.API_KEY)
                        .addHeader("Authorization", "Bearer ${activeSession.accessToken}")
                        .addHeader("Accept", "application/json")
                        .post(payload)
                        .build()

                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            val message = try { JSONObject(body).optString("message") }
                            catch (_: Exception) { "" }
                            Result(false, message = message.ifBlank {
                                "User search failed. HTTP ${response.code}"
                            })
                        } else {
                            try {
                                val array = JSONArray(body)
                                val profiles = (0 until array.length()).map { index ->
                                    val item = array.getJSONObject(index)
                                    PublicProfile(
                                        id = item.getString("id"),
                                        fullName = item.optString("full_name").ifBlank { "Teddy user" },
                                        username = item.optString("username"),
                                        bio = item.optString("bio"),
                                        avatarUrl = item.optString("avatar_url")
                                            .takeIf { it.isNotBlank() && it != "null" }
                                    )
                                }
                                Result(true, profiles, if (profiles.isEmpty()) "No users found." else "${profiles.size} user(s) found.")
                            } catch (error: Exception) {
                                Result(false, message = "Could not read search results: ${error.message ?: "Unknown error"}")
                            }
                        }
                    }
                } catch (error: Exception) {
                    Result(false, message = "Network error: ${error.message ?: "Unknown error"}")
                }

                val expired = responseResult.message.contains("JWT expired", true) ||
                    responseResult.message.contains("token is expired", true) ||
                    responseResult.message.contains("invalid JWT", true)

                if (!retried && !responseResult.success && expired) {
                    SupabaseSessionManager.refreshAfterUnauthorized(activeSession) { refresh ->
                        val renewed = refresh.session
                        if (refresh.success && renewed != null) send(renewed, true)
                        else mainHandler.post {
                            onResult(Result(false, message = "Session refresh failed: ${refresh.message}"))
                        }
                    }
                } else {
                    mainHandler.post { onResult(responseResult) }
                }
            }.start()
        }

        send(session, false)
    }
}
