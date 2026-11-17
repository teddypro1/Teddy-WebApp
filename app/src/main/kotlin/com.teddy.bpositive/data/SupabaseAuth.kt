package com.teddy.bpositive.data

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Supabase email/password authentication API.
 *
 * This class only performs network requests. It does not store credentials
 * or change the app UI; session persistence will be added separately.
 */
object SupabaseAuth {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val mainHandler = Handler(Looper.getMainLooper())

    data class AuthResult(
        val success: Boolean,
        val message: String,
        val accessToken: String? = null,
        val refreshToken: String? = null,
        val userId: String? = null,
        val email: String? = null
    )

    fun signUp(
        email: String,
        password: String,
        onResult: (AuthResult) -> Unit
    ) {
        val payload = JSONObject()
            .put("email", email.trim())
            .put("password", password)

        executeAuthRequest(
            path = "/auth/v1/signup",
            payload = payload,
            onResult = onResult
        )
    }

    fun signIn(
        email: String,
        password: String,
        onResult: (AuthResult) -> Unit
    ) {
        val payload = JSONObject()
            .put("email", email.trim())
            .put("password", password)

        executeAuthRequest(
            path = "/auth/v1/token?grant_type=password",
            payload = payload,
            onResult = onResult
        )
    }

    fun refreshSession(
        refreshToken: String,
        onResult: (AuthResult) -> Unit
    ) {
        val payload = JSONObject()
            .put("refresh_token", refreshToken)

        executeAuthRequest(
            path = "/auth/v1/token?grant_type=refresh_token",
            payload = payload,
            onResult = onResult
        )
    }

    fun signOut(
        accessToken: String,
        onResult: (AuthResult) -> Unit
    ) {
        Thread {
            val request = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}/auth/v1/logout")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Authorization", "Bearer $accessToken")
                .post("{}".toRequestBody(jsonMediaType))
                .build()

            val result = try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        AuthResult(success = true, message = "Signed out successfully.")
                    } else {
                        AuthResult(
                            success = false,
                            message = extractErrorMessage(body, response.code)
                        )
                    }
                }
            } catch (e: Exception) {
                AuthResult(success = false, message = "Network error: ${e.message ?: "Unknown error"}")
            }

            mainHandler.post { onResult(result) }
        }.start()
    }

    private fun executeAuthRequest(
        path: String,
        payload: JSONObject,
        onResult: (AuthResult) -> Unit
    ) {
        Thread {
            val request = Request.Builder()
                .url("${SupabaseConfig.BASE_URL}$path")
                .addHeader("apikey", SupabaseConfig.API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val result = try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    val json = try {
                        JSONObject(body)
                    } catch (_: Exception) {
                        JSONObject()
                    }

                    if (response.isSuccessful) {
                        val user = json.optJSONObject("user")
                        val session = json.optJSONObject("session")
                        AuthResult(
                            success = true,
                            message = if (json.has("access_token") || session?.has("access_token") == true) {
                                "Authentication successful."
                            } else {
                                "Request accepted. Check your email if confirmation is required."
                            },
                            accessToken = json.optStringOrNull("access_token")
                                ?: session?.optStringOrNull("access_token"),
                            refreshToken = json.optStringOrNull("refresh_token")
                                ?: session?.optStringOrNull("refresh_token"),
                            userId = user?.optStringOrNull("id"),
                            email = user?.optStringOrNull("email")
                        )
                    } else {
                        AuthResult(
                            success = false,
                            message = extractErrorMessage(body, response.code)
                        )
                    }
                }
            } catch (e: Exception) {
                AuthResult(success = false, message = "Network error: ${e.message ?: "Unknown error"}")
            }

            mainHandler.post { onResult(result) }
        }.start()
    }

    private fun extractErrorMessage(body: String, statusCode: Int): String {
        val json = try {
            JSONObject(body)
        } catch (_: Exception) {
            JSONObject()
        }

        return json.optString("msg").takeIf { it.isNotBlank() }
            ?: json.optString("message").takeIf { it.isNotBlank() }
            ?: json.optString("error_description").takeIf { it.isNotBlank() }
            ?: "Authentication failed. HTTP $statusCode"
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        return optString(key).takeIf { it.isNotBlank() && it != "null" }
    }
}
