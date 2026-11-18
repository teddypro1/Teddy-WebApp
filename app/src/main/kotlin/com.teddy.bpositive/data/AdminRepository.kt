package com.teddy.bpositive.data

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object AdminRepository {
    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private const val FUNCTION_URL = "/functions/v1/send-push-notification"

    data class NotificationRecord(
        val id: String,
        val title: String,
        val body: String,
        val audience: String,
        val totalDevices: Int,
        val sentCount: Int,
        val failedCount: Int,
        val createdAt: String
    )

    data class Result<T>(
        val success: Boolean,
        val data: T? = null,
        val message: String
    )

    fun checkAdmin(session: SupabaseSessionManager.Session, onResult: (Result<Boolean>) -> Unit) {
        call(session, JSONObject().put("action", "check_admin")) { response ->
            val isAdmin = response.data?.optBoolean("is_admin", false) == true
            onResult(Result(response.success, if (response.success) isAdmin else null, response.message))
        }
    }

    fun sendNotification(
        session: SupabaseSessionManager.Session,
        title: String,
        body: String,
        onResult: (Result<String>) -> Unit
    ) {
        val payload = JSONObject()
            .put("action", "send")
            .put("title", title)
            .put("body", body)
        call(session, payload) { response ->
            val data = response.data
            val sent = data?.optInt("sent", 0) ?: 0
            val total = data?.optInt("total", 0) ?: 0
            val historySaved = data?.optBoolean("history_saved", false) == true
            val message = if (response.success) {
                "Sent to $sent of $total registered Android device(s)." +
                    if (historySaved) " History saved." else " History could not be saved."
            } else response.message
            onResult(Result(response.success, message.takeIf { response.success }, message))
        }
    }

    fun loadHistory(
        session: SupabaseSessionManager.Session,
        onResult: (Result<List<NotificationRecord>>) -> Unit
    ) {
        call(session, JSONObject().put("action", "history")) { response ->
            if (!response.success || response.data == null) {
                onResult(Result(false, null, response.message))
                return@call
            }
            try {
                val rows = response.data.optJSONArray("history") ?: JSONArray()
                val records = buildList {
                    for (index in 0 until rows.length()) {
                        val row = rows.optJSONObject(index) ?: continue
                        add(
                            NotificationRecord(
                                id = row.optString("id"),
                                title = row.optString("title"),
                                body = row.optString("body"),
                                audience = row.optString("audience"),
                                totalDevices = row.optInt("total_devices"),
                                sentCount = row.optInt("sent_count"),
                                failedCount = row.optInt("failed_count"),
                                createdAt = row.optString("created_at")
                            )
                        )
                    }
                }
                onResult(Result(true, records, "Notification history loaded."))
            } catch (error: Exception) {
                onResult(Result(false, null, "Could not read notification history: ${error.message}"))
            }
        }
    }

    private data class CallResult(val success: Boolean, val data: JSONObject?, val message: String)

    private fun call(
        session: SupabaseSessionManager.Session,
        payload: JSONObject,
        onResult: (CallResult) -> Unit
    ) {
        Thread {
            val request = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}$FUNCTION_URL")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val result = try {
                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    val json = runCatching { JSONObject(raw) }.getOrNull()
                    if (response.isSuccessful && json?.optBoolean("success", true) != false) {
                        CallResult(true, json, "Request completed.")
                    } else {
                        val message = json?.optString("error")?.takeIf { it.isNotBlank() }
                            ?: "Admin request failed. HTTP ${response.code}."
                        CallResult(false, json, message)
                    }
                }
            } catch (error: Exception) {
                CallResult(false, null, "Network error: ${error.message ?: "Unknown error"}")
            }
            mainHandler.post { onResult(result) }
        }.start()
    }
}
