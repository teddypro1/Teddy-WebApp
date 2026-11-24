package com.teddy.bpositive.data

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID

/** Authenticated message history and send operations for one conversation. */
object MessagingRepository {
    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    data class Message(
        val id: String,
        val conversationId: String,
        val senderId: String,
        val body: String,
        val createdAt: String
    )

    data class Result(
        val success: Boolean,
        val messages: List<Message> = emptyList(),
        val message: String
    )

    fun loadMessages(
        session: SupabaseSessionManager.Session,
        conversationId: String,
        onResult: (Result) -> Unit
    ) {
        requestWithRefresh(session, false, onResult) { active ->
            val encodedId = URLEncoder.encode(conversationId, "UTF-8")
            val url = "${SupabaseConfig.BASE_URL}/rest/v1/messages?select=id,conversation_id,sender_id,body,created_at&conversation_id=eq.$encodedId&deleted_at=is.null&order=created_at.asc&limit=100"
            val request = Request.Builder().url(url)
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer ${active.accessToken}")
                .addHeader("Accept", "application/json")
                .get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = try { JSONObject(body).optString("message") } catch (_: Exception) { "" }
                    Result(false, message = detail.ifBlank { "Could not load messages. HTTP ${response.code}" })
                } else {
                    try {
                        val array = JSONArray(body)
                        val rows = (0 until array.length()).map { index ->
                            val item = array.getJSONObject(index)
                            Message(
                                id = item.getString("id"),
                                conversationId = item.getString("conversation_id"),
                                senderId = item.getString("sender_id"),
                                body = item.optString("body"),
                                createdAt = item.optString("created_at")
                            )
                        }.filter { it.body.isNotBlank() }
                        Result(true, rows, if (rows.isEmpty()) "No messages yet. Send the first message." else "")
                    } catch (error: Exception) {
                        Result(false, message = "Could not parse message history: ${error.message ?: "Unknown error"}")
                    }
                }
            }
        }
    }

    fun sendMessage(
        session: SupabaseSessionManager.Session,
        conversationId: String,
        body: String,
        onResult: (Result) -> Unit
    ) {
        val cleanBody = body.trim()
        if (cleanBody.isBlank() || cleanBody.length > 10000) {
            onResult(Result(false, message = "Message must contain 1–10,000 characters."))
            return
        }
        requestWithRefresh(session, false, onResult) { active ->
            val payload = JSONObject()
                .put("p_conversation_id", conversationId)
                .put("p_client_message_id", UUID.randomUUID().toString())
                .put("p_body", cleanBody)
                .toString().toRequestBody(jsonType)
            val request = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}/rest/v1/rpc/send_message")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer ${active.accessToken}")
                .addHeader("Accept", "application/json")
                .post(payload).build()
            client.newCall(request).execute().use { response ->
                val bodyText = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = try { JSONObject(bodyText).optString("message") } catch (_: Exception) { "" }
                    Result(false, message = detail.ifBlank { "Message send failed. HTTP ${response.code}" })
                } else Result(true, message = "Message sent.")
            }
        }
    }

    private fun requestWithRefresh(
        session: SupabaseSessionManager.Session,
        retried: Boolean,
        onResult: (Result) -> Unit,
        operation: (SupabaseSessionManager.Session) -> Result
    ) {
        Thread {
            val result = try { operation(session) }
            catch (error: Exception) {
                Result(false, message = "Network error: ${error.message ?: "Unknown error"}")
            }
            val expired = result.message.contains("JWT expired", true) ||
                result.message.contains("token is expired", true) ||
                result.message.contains("invalid JWT", true)
            if (!retried && !result.success && expired) {
                SupabaseSessionManager.refreshAfterUnauthorized(session) { refresh ->
                    val renewed = refresh.session
                    if (refresh.success && renewed != null) {
                        requestWithRefresh(renewed, true, onResult, operation)
                    } else mainHandler.post {
                        onResult(Result(false, message = "Session refresh failed: ${refresh.message}"))
                    }
                }
            } else mainHandler.post { onResult(result) }
        }.start()
    }
}
