package com.teddy.bpositive.data

import android.os.Handler
import android.os.Looper
import com.teddy.bpositive.security.NotesEncryption
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Syncs only the client-encrypted Vault envelope.
 * Passwords, Recovery Keys and unwrapped master keys never leave the device.
 */
object SupabaseVaultApi {

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    data class ApiResult<T>(
        val success: Boolean,
        val data: T? = null,
        val message: String
    )

    data class SyncResult(
        val success: Boolean,
        val alreadySynced: Boolean = false,
        val message: String
    )

    fun fetchEnvelope(
        session: SupabaseSessionManager.Session,
        onResult: (ApiResult<NotesEncryption.VaultEnvelope?>) -> Unit
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult(ApiResult(false, message = "Signed-in account ID is missing."))
            return
        }

        execute(
            method = "GET",
            path = "/rest/v1/vault_envelopes?select=envelope&user_id=eq.$userId&limit=1",
            session = session,
            onResult = onResult
        ) { body ->
            val rows = JSONArray(body)
            if (rows.length() == 0) null
            else NotesEncryption.VaultEnvelope.fromJson(
                rows.getJSONObject(0).getJSONObject("envelope").toString()
            )
        }
    }

    /**
     * Creates the account envelope only when it does not already exist.
     * A different remote envelope is never overwritten by this first sync flow.
     */
    fun syncEnvelope(
        session: SupabaseSessionManager.Session,
        envelope: NotesEncryption.VaultEnvelope,
        onResult: (SyncResult) -> Unit
    ) {
        fetchEnvelope(session) { fetched ->
            if (!fetched.success) {
                onResult(SyncResult(false, message = "Could not check account Vault: ${fetched.message}"))
                return@fetchEnvelope
            }

            val remote = fetched.data
            if (remote != null) {
                if (remote == envelope) {
                    onResult(SyncResult(true, alreadySynced = true, message = "This Vault is already synced to your account."))
                } else {
                    onResult(SyncResult(false, message = "A different Vault is already saved to this account. Nothing was overwritten."))
                }
                return@fetchEnvelope
            }

            createEnvelope(session, envelope) { created ->
                if (created.success) {
                    onResult(SyncResult(true, message = "Encrypted Vault envelope synced to your account."))
                } else {
                    // Resolve a simultaneous first-sync race without overwriting either envelope.
                    fetchEnvelope(session) { latest ->
                        if (latest.success && latest.data == envelope) {
                            onResult(SyncResult(true, alreadySynced = true, message = "This Vault is already synced to your account."))
                        } else if (latest.success && latest.data != null) {
                            onResult(SyncResult(false, message = "A different Vault was saved to this account first. Nothing was overwritten."))
                        } else {
                            onResult(SyncResult(false, message = "Could not sync Vault: ${created.message}"))
                        }
                    }
                }
            }
        }
    }

    private fun createEnvelope(
        session: SupabaseSessionManager.Session,
        envelope: NotesEncryption.VaultEnvelope,
        onResult: (ApiResult<Boolean>) -> Unit
    ) {
        val userId = session.userId
        if (userId.isNullOrBlank()) {
            onResult(ApiResult(false, message = "Signed-in account ID is missing."))
            return
        }

        val payload = JSONObject()
            .put("user_id", userId)
            .put("envelope", JSONObject(envelope.toJson()))

        execute(
            method = "POST",
            path = "/rest/v1/vault_envelopes?select=user_id",
            session = session,
            body = payload.toString(),
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
        fun send(activeSession: SupabaseSessionManager.Session, retried: Boolean) {
            Thread {
                val requestBody = body?.toRequestBody(jsonMediaType)
                val builder = Request.Builder()
                    .url("${SupabaseConfig.BASE_URL}$path")
                    .addHeader("apikey", SupabaseConfig.API_KEY)
                    .addHeader("Authorization", "Bearer ${activeSession.accessToken}")
                    .addHeader("Accept", "application/json")

                if (prefer != null) builder.addHeader("Prefer", prefer)

                when (method) {
                    "GET" -> builder.get()
                    "POST" -> builder.post(requestBody ?: "{}".toRequestBody(jsonMediaType))
                    else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
                }

                val result = try {
                    client.newCall(builder.build()).execute().use { response ->
                        val responseText = response.body?.string().orEmpty()
                        if (response.isSuccessful) {
                            try {
                                ApiResult(true, parse(responseText), "HTTP ${response.code}")
                            } catch (error: Exception) {
                                ApiResult<T>(false, message = "Could not parse Vault response: ${error.message ?: "Unknown error"}")
                            }
                        } else {
                            ApiResult<T>(false, message = extractError(responseText, response.code))
                        }
                    }
                } catch (error: Exception) {
                    ApiResult<T>(false, message = "Network error: ${error.message ?: "Unknown error"}")
                }

                val tokenExpired = result.message.contains("JWT expired", true) ||
                    result.message.contains("token is expired", true) ||
                    result.message.contains("invalid JWT", true)

                if (!retried && !result.success && tokenExpired) {
                    SupabaseSessionManager.refreshAfterUnauthorized(activeSession) { refreshed ->
                        val nextSession = refreshed.session
                        if (refreshed.success && nextSession != null) {
                            send(nextSession, true)
                        } else {
                            onResult(ApiResult(false, message = "Session refresh failed: ${refreshed.message}"))
                        }
                    }
                } else {
                    mainHandler.post { onResult(result) }
                }
            }.start()
        }

        send(session, false)
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
}
