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

    data class DeliveryDetail(
        val userId: String,
        val tokenSuffix: String,
        val success: Boolean,
        val httpStatus: Int,
        val errorCode: String?,
        val errorMessage: String?,
        val attempts: Int,
        val staleTokenRemoved: Boolean
    )

    data class UserRecord(
        val id: String,
        val email: String,
        val fullName: String,
        val username: String?,
        val createdAt: String,
        val androidDeviceCount: Int
    )

    data class NotificationRecord(
        val id: String,
        val title: String,
        val body: String,
        val audience: String,
        val totalDevices: Int,
        val sentCount: Int,
        val failedCount: Int,
        val createdAt: String,
        val deliveryDetails: List<DeliveryDetail>
    )

    data class Result<T>(
        val success: Boolean,
        val data: T? = null,
        val message: String,
        val deliveryDetails: List<DeliveryDetail> = emptyList()
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
        audience: String,
        recipientEmails: List<String>,
        onResult: (Result<String>) -> Unit
    ) {
        val payload = JSONObject()
            .put("action", "send")
            .put("title", title)
            .put("body", body)
            .put("audience", audience)
        if (audience == "selected_users") {
            payload.put("recipient_emails", JSONArray(recipientEmails))
        }
        call(session, payload) { response ->
            val data = response.data
            val sent = data?.optInt("sent", 0) ?: 0
            val total = data?.optInt("total", 0) ?: 0
            val failed = data?.optInt("failed", (total - sent).coerceAtLeast(0)) ?: 0
            val historySaved = data?.optBoolean("history_saved", false) == true
            val inboxSaved = data?.optInt("inbox_saved", 0) ?: 0
            val details = parseDeliveryDetails(data?.optJSONArray("results"))
            val summary = if (response.success) {
                "Inbox saved for $inboxSaved account(s). FCM accepted $sent of $total device message(s); failed: $failed." +
                    if (historySaved) " Detailed history saved." else " Detailed history could not be saved."
            } else response.message
            val diagnosticText = details.joinToString("\n") { detail ->
                val account = detail.userId.takeLast(6)
                val token = detail.tokenSuffix.ifBlank { "unknown" }
                if (detail.success) {
                    "✓ Account …$account · Token …$token · FCM accepted (HTTP ${detail.httpStatus})"
                } else {
                    val code = detail.errorCode ?: "UNKNOWN_ERROR"
                    val explanation = friendlyError(code, detail.staleTokenRemoved)
                    "✕ Account …$account · Token …$token · $code (HTTP ${detail.httpStatus}) · $explanation · Attempts: ${detail.attempts}"
                }
            }
            val fullMessage = listOf(summary, diagnosticText).filter { it.isNotBlank() }.joinToString("\n")
            onResult(Result(response.success, summary.takeIf { response.success }, fullMessage, details))
        }
    }

    fun loadUserDirectory(
        session: SupabaseSessionManager.Session,
        onResult: (Result<List<UserRecord>>) -> Unit
    ) {
        call(session, JSONObject().put("action", "user_directory")) { response ->
            if (!response.success || response.data == null) {
                onResult(Result(false, null, response.message))
                return@call
            }
            try {
                val rows = response.data.optJSONArray("users") ?: JSONArray()
                val users = buildList {
                    for (index in 0 until rows.length()) {
                        val row = rows.optJSONObject(index) ?: continue
                        add(
                            UserRecord(
                                id = row.optString("id"),
                                email = row.optString("email"),
                                fullName = row.optString("full_name").ifBlank { "Unnamed user" },
                                username = row.optString("username").takeIf { it.isNotBlank() && it != "null" },
                                createdAt = row.optString("created_at"),
                                androidDeviceCount = row.optInt("android_device_count")
                            )
                        )
                    }
                }
                onResult(Result(true, users, "Loaded \${users.size} user account(s)."))
            } catch (error: Exception) {
                onResult(Result(false, null, "Could not read user directory: \${error.message}"))
            }
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
                                createdAt = row.optString("created_at"),
                                deliveryDetails = parseDeliveryDetails(row.optJSONArray("delivery_details"))
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

    private fun parseDeliveryDetails(rows: JSONArray?): List<DeliveryDetail> {
        if (rows == null) return emptyList()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                add(
                    DeliveryDetail(
                        userId = row.optString("user_id"),
                        tokenSuffix = row.optString("token_suffix"),
                        success = row.optBoolean("success"),
                        httpStatus = row.optInt("http_status"),
                        errorCode = row.optString("error_code").takeIf { it.isNotBlank() && it != "null" },
                        errorMessage = row.optString("error_message").takeIf { it.isNotBlank() && it != "null" },
                        attempts = row.optInt("attempts", 1),
                        staleTokenRemoved = row.optBoolean("stale_token_removed")
                    )
                )
            }
        }
    }

    fun friendlyError(code: String, staleTokenRemoved: Boolean = false): String {
        return when (code) {
            "UNREGISTERED" -> if (staleTokenRemoved) "Expired token automatically removed." else "Token is no longer registered; cleanup needs review."
            "SENDER_ID_MISMATCH" -> "Token belongs to a different Firebase sender/project."
            "INVALID_ARGUMENT" -> "FCM rejected the token or message payload; token was kept for safety."
            "QUOTA_EXCEEDED" -> "FCM rate limit reached; retry was attempted."
            "UNAVAILABLE", "INTERNAL", "FCM_SERVER_ERROR" -> "Temporary FCM server problem; retry was attempted."
            "UNAUTHENTICATED" -> "Firebase OAuth access was rejected."
            "PERMISSION_DENIED" -> "Firebase service account lacks permission for this project."
            "NETWORK_ERROR" -> "Network failure while contacting FCM."
            else -> "FCM could not accept this device message."
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
