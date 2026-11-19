package com.teddy.bpositive.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Registers this installation's FCM token against the currently authenticated
 * Supabase user. The user's access token and database RLS enforce ownership.
 */
object FcmTokenRepository {
    private const val TAG = "TeddyFCM"
    private const val TABLE_PATH = "/rest/v1/device_tokens"
    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun registerCurrentDevice(
        context: Context,
        session: SupabaseSessionManager.Session,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult?.invoke(false, "Supabase user ID is missing.")
            return
        }

        Log.i(TAG, "Starting FCM token retrieval for signed-in user")
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    val message = task.exception?.message ?: "Could not retrieve FCM token."
                    Log.e(TAG, "FCM token retrieval FAILED during Supabase registration", task.exception)
                    onResult?.invoke(false, message)
                    return@addOnCompleteListener
                }

                val token = task.result
                if (token.isNullOrBlank()) {
                    Log.e(TAG, "FCM returned an empty token during Supabase registration")
                    onResult?.invoke(false, "FCM registration token is empty.")
                    return@addOnCompleteListener
                }

                Log.i(TAG, "FCM token obtained for Supabase registration; length=${token.length}")
                FcmTokenManager.saveToken(context.applicationContext, token)
                registerToken(context.applicationContext, session, token, onResult)
            }
    }

    fun registerToken(
        context: Context,
        session: SupabaseSessionManager.Session,
        token: String,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank() || token.isBlank()) {
            onResult?.invoke(false, "User ID or FCM token is missing.")
            return
        }

        val payload = JSONObject()
            .put("user_id", userId)
            .put("fcm_token", token)
            .put("platform", "android")

        Log.i(TAG, "Sending FCM token registration request to Supabase")
        Thread {
            // A token refresh can race with sign-out or account switching.
            // Re-read the encrypted session and never register for a different account.
            val latestSession = SupabaseSessionManager.load(context.applicationContext)
            if (latestSession == null || latestSession.userId != userId) {
                mainHandler.post { onResult?.invoke(false, "Session changed; FCM registration skipped.") }
                return@Thread
            }

            val payloadWithFreshTimestamp = JSONObject(payload.toString())
                .put("updated_at", java.time.Instant.now().toString())

            val request = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}$TABLE_PATH?on_conflict=fcm_token")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer ${latestSession.accessToken}")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
                .post(payloadWithFreshTimestamp.toString().toRequestBody(jsonMediaType))
                .build()

            val result = try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.i(TAG, "Supabase accepted FCM token registration; HTTP ${response.code}")
                        true to "FCM token registered."
                    } else {
                        val errorBody = response.body?.string().orEmpty()
                        Log.e(TAG, "Supabase rejected FCM token registration; HTTP ${response.code}: $errorBody")
                        false to "FCM token registration failed. HTTP ${response.code}: $errorBody"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Supabase FCM registration network error", e)
                false to "FCM token registration network error: ${e.message ?: "Unknown error"}"
            }

            if (!result.first) Log.w(TAG, result.second)
            mainHandler.post { onResult?.invoke(result.first, result.second) }
        }.start()
    }

    /** Sends a one-time test push through the authenticated Supabase Edge Function. */
    fun sendTestNotification(
        session: SupabaseSessionManager.Session,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        val payload = JSONObject()
            .put("title", "Teddy WebApp")
            .put("body", "Push notification test successful!")

        Thread {
            val request = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}/functions/v1/send-push-notification")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val result = try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    val data = runCatching { JSONObject(responseBody) }.getOrNull()
                    if (response.isSuccessful && data?.optBoolean("success") == true) {
                        true to "Test notification accepted by FCM (${data.optInt("sent")}/${data.optInt("total")} devices)."
                    } else {
                        false to (data?.optString("error")?.takeIf { it.isNotBlank() }
                            ?: "Push test failed. HTTP ${response.code}.")
                    }
                }
            } catch (e: Exception) {
                false to "Push test network error: ${e.message ?: "Unknown error"}"
            }
            mainHandler.post { onResult?.invoke(result.first, result.second) }
        }.start()
    }

    /**
     * Best-effort removal for the current installation before local sign-out.
     * The local token is retained so a later sign-in can register it again.
     */
    fun unregisterCurrentDevice(
        context: Context,
        session: SupabaseSessionManager.Session
    ) {
        val userId = session.userId ?: return
        val token = FcmTokenManager.getSavedToken(context) ?: return
        val encodedToken = java.net.URLEncoder.encode(token, "UTF-8")
        val encodedUserId = java.net.URLEncoder.encode(userId, "UTF-8")

        Thread {
            val request = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}$TABLE_PATH?user_id=eq.$encodedUserId&fcm_token=eq.$encodedToken")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer ${session.accessToken}")
                .delete()
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Could not unregister FCM token. HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "FCM token unregistration failed", e)
            }
        }.start()
    }
}
