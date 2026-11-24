package com.teddy.bpositive.data

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/** Creates or retrieves the canonical one-to-one conversation for two users. */
object DirectConversationApi {
    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    data class Result(
        val success: Boolean,
        val conversationId: String? = null,
        val message: String
    )

    fun createOrGet(
        session: SupabaseSessionManager.Session,
        otherUserId: String,
        onResult: (Result) -> Unit
    ) {
        if (session.userId.isNullOrBlank() || otherUserId.isBlank()) {
            onResult(Result(false, message = "Signed-in user or selected user is missing."))
            return
        }
        if (session.userId == otherUserId) {
            onResult(Result(false, message = "You cannot start a conversation with yourself."))
            return
        }

        fun send(activeSession: SupabaseSessionManager.Session, retried: Boolean) {
            Thread {
                val payload = JSONObject().put("p_other_user_id", otherUserId)
                    .toString().toRequestBody(jsonType)
                val result = try {
                    val request = Request.Builder()
                        .url("${SupabaseConfig.BASE_URL}/rest/v1/rpc/create_direct_conversation")
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
                                "Could not open conversation. HTTP ${response.code}"
                            })
                        } else {
                            val id = body.trim().trim('"').takeIf { it.isNotBlank() && it != "null" }
                            if (id == null) Result(false, message = "Server returned an empty conversation ID.")
                            else Result(true, id, "Conversation ready.")
                        }
                    }
                } catch (error: Exception) {
                    Result(false, message = "Network error: ${error.message ?: "Unknown error"}")
                }

                val expired = result.message.contains("JWT expired", true) ||
                    result.message.contains("token is expired", true) ||
                    result.message.contains("invalid JWT", true)
                if (!retried && !result.success && expired) {
                    SupabaseSessionManager.refreshAfterUnauthorized(activeSession) { refresh ->
                        val renewed = refresh.session
                        if (refresh.success && renewed != null) send(renewed, true)
                        else mainHandler.post {
                            onResult(Result(false, message = "Session refresh failed: ${refresh.message}"))
                        }
                    }
                } else {
                    mainHandler.post { onResult(result) }
                }
            }.start()
        }

        send(session, false)
    }
}
