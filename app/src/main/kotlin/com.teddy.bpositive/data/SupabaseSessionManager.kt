package com.teddy.bpositive.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the signed-in user's Supabase session encrypted with an Android
 * Keystore AES-GCM key. The key itself is never stored in app preferences.
 */
object SupabaseSessionManager {

    private const val KEY_ALIAS = "teddy_supabase_session_key"
    private const val PREFS_NAME = "teddy_secure_session"
    private const val SESSION_KEY = "encrypted_session"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    data class Session(
        val accessToken: String,
        val refreshToken: String,
        val userId: String?,
        val email: String?
    )

    data class RefreshResult(
        val success: Boolean,
        val session: Session? = null,
        val message: String
    )

    @Volatile private var applicationContext: Context? = null
    private var refreshInFlight = false
    private val refreshCallbacks = mutableListOf<(RefreshResult) -> Unit>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Synchronized
    fun save(context: Context, session: Session) {
        applicationContext = context.applicationContext
        val json = JSONObject()
            .put("access_token", session.accessToken)
            .put("refresh_token", session.refreshToken)
            .put("user_id", session.userId)
            .put("email", session.email)

        val encrypted = encrypt(json.toString().toByteArray(Charsets.UTF_8))
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(SESSION_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    @Synchronized
    fun load(context: Context): Session? {
        applicationContext = context.applicationContext
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(SESSION_KEY, null) ?: return null

        return try {
            val encrypted = Base64.decode(stored, Base64.NO_WRAP)
            val json = JSONObject(String(decrypt(encrypted), Charsets.UTF_8))
            val accessToken = json.optString("access_token")
            val refreshToken = json.optString("refresh_token")

            if (accessToken.isBlank() || refreshToken.isBlank()) {
                clear(context)
                null
            } else {
                Session(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    userId = json.optNullableString("user_id"),
                    email = json.optNullableString("email")
                )
            }
        } catch (_: Exception) {
            clear(context)
            null
        }
    }


    fun refreshAfterUnauthorized(
        rejectedSession: Session,
        onResult: (RefreshResult) -> Unit
    ) {
        val context = applicationContext
        if (context == null) {
            mainHandler.post {
                onResult(RefreshResult(false, message = "Session storage is not initialized."))
            }
            return
        }

        var shouldStartRefresh = false
        var alreadyRefreshed: Session? = null
        synchronized(this) {
            val latest = load(context)
            if (latest != null && latest.accessToken != rejectedSession.accessToken) {
                alreadyRefreshed = latest
            } else {
                refreshCallbacks.add(onResult)
                if (!refreshInFlight) {
                    refreshInFlight = true
                    shouldStartRefresh = true
                }
            }
        }

        alreadyRefreshed?.let {
            onResult(RefreshResult(true, it, "A newer session is already available."))
            return
        }
        if (!shouldStartRefresh) return

        SupabaseAuth.refreshSession(rejectedSession.refreshToken) { authResult ->
            val access = authResult.accessToken
            val refresh = authResult.refreshToken
            val result = if (authResult.success && !access.isNullOrBlank() && !refresh.isNullOrBlank()) {
                val refreshed = Session(
                    accessToken = access,
                    refreshToken = refresh,
                    userId = authResult.userId ?: rejectedSession.userId,
                    email = authResult.email ?: rejectedSession.email
                )
                try {
                    save(context, refreshed)
                    RefreshResult(true, refreshed, "Session refreshed successfully.")
                } catch (e: Exception) {
                    RefreshResult(false, message = "Could not save refreshed session: ${e.message ?: "Unknown error"}")
                }
            } else {
                RefreshResult(false, message = authResult.message.ifBlank { "Session refresh failed." })
            }

            val callbacks = synchronized(this) {
                refreshInFlight = false
                refreshCallbacks.toList().also { refreshCallbacks.clear() }
            }
            callbacks.forEach { callback -> callback(result) }
        }
    }

    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(SESSION_KEY)
            .apply()
    }

    private fun encrypt(plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plainText)
        return ByteBuffer.allocate(4 + cipher.iv.size + encrypted.size)
            .putInt(cipher.iv.size)
            .put(cipher.iv)
            .put(encrypted)
            .array()
    }

    private fun decrypt(payload: ByteArray): ByteArray {
        val buffer = ByteBuffer.wrap(payload)
        val ivLength = buffer.int
        require(ivLength in 12..16) { "Invalid encrypted session data" }
        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, iv)
        )
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return optString(key).takeIf { it.isNotBlank() && it != "null" }
    }
}
