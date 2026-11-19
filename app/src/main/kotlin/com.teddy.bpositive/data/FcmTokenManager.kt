package com.teddy.bpositive.data

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Stores the current installation token and keeps a small queue of tokens
 * replaced by Firebase. Old tokens are removed from Supabase only after the
 * replacement token has been registered successfully for the same account.
 */
object FcmTokenManager {
    private const val PREFS_NAME = "teddy_fcm"
    private const val TOKEN_KEY = "registration_token"
    private const val PENDING_OLD_TOKENS_KEY = "pending_old_registration_tokens"
    private const val TAG = "TeddyFCM"

    fun initialize(context: Context) {
        Log.i(TAG, "FCM initialization started")
        createNotificationChannel(context)

        val hasGooglePlayServices = try {
            context.packageManager.getPackageInfo("com.google.android.gms", 0)
            true
        } catch (_: Exception) {
            false
        }
        Log.i(TAG, "Google Play services package present: $hasGooglePlayServices")

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "FCM token retrieval FAILED", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result
                if (token.isNullOrBlank()) {
                    Log.e(TAG, "FCM token retrieval returned an empty token")
                    return@addOnCompleteListener
                }

                // saveToken preserves the replaced token in a pending queue.
                saveToken(context, token)
                Log.i(TAG, "FCM token retrieved successfully; length=${token.length}")
            }
    }

    @Synchronized
    fun saveToken(context: Context, token: String) {
        if (token.isBlank()) return
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previous = prefs.getString(TOKEN_KEY, null)
        val pending = prefs.getStringSet(PENDING_OLD_TOKENS_KEY, emptySet())
            .orEmpty()
            .toMutableSet()

        if (!previous.isNullOrBlank() && previous != token) {
            pending.add(previous)
            Log.i(TAG, "Previous FCM token queued for account-scoped replacement")
        }

        prefs.edit()
            .putString(TOKEN_KEY, token)
            .putStringSet(PENDING_OLD_TOKENS_KEY, pending)
            .apply()
    }

    fun getSavedToken(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(TOKEN_KEY, null)
    }

    @Synchronized
    fun getPendingOldTokens(context: Context): Set<String> {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(PENDING_OLD_TOKENS_KEY, emptySet())
            .orEmpty()
            .toSet()
    }

    @Synchronized
    fun clearPendingOldToken(context: Context, token: String) {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pending = prefs.getStringSet(PENDING_OLD_TOKENS_KEY, emptySet())
            .orEmpty()
            .toMutableSet()
        if (pending.remove(token)) {
            prefs.edit().putStringSet(PENDING_OLD_TOKENS_KEY, pending).apply()
        }
    }

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = android.app.NotificationChannel(
            TeddyFirebaseMessagingService.CHANNEL_ID,
            "Teddy-WebApp notifications",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications from Teddy-WebApp"
        }
        manager.createNotificationChannel(channel)
    }
}
