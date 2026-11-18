package com.teddy.bpositive.data

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Retrieves and locally caches this installation's FCM registration token.
 * The token will be associated with the authenticated Supabase account in a later step.
 */
object FcmTokenManager {
    private const val PREFS_NAME = "teddy_fcm"
    private const val TOKEN_KEY = "registration_token"
    private const val TAG = "TeddyFCM"

    fun initialize(context: Context) {
        createNotificationChannel(context)
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Could not retrieve FCM token", task.exception)
                    return@addOnCompleteListener
                }

                val token = task.result ?: return@addOnCompleteListener
                saveToken(context, token)
                Log.d(TAG, "FCM registration token retrieved")
            }
    }

    fun saveToken(context: Context, token: String) {
        if (token.isBlank()) return
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(TOKEN_KEY, token)
            .apply()
    }

    fun getSavedToken(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(TOKEN_KEY, null)
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
