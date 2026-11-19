package com.teddy.bpositive.data

import android.app.PendingIntent
import android.content.Intent
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.teddy.bpositive.MainActivity

/**
 * Receives FCM token rotations and foreground messages for Teddy-WebApp.
 */
class TeddyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FcmTokenManager.saveToken(applicationContext, token)
        Log.i(TAG, "FCM registration token refreshed")

        // Keep the server-side token record in sync even when FCM rotates
        // the token while the user remains signed in.
        val session = SupabaseSessionManager.load(applicationContext)
        if (session != null) {
            FcmTokenRepository.registerToken(applicationContext, session, token) { success, message ->
                if (success) Log.i(TAG, "Refreshed FCM token registered with Supabase")
                else Log.w(TAG, "Refreshed FCM token registration skipped or failed: $message")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "Teddy-WebApp"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        FcmTokenManager.createNotificationChannel(this)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "teddy_webapp_general"
        private const val TAG = "TeddyFCM"
    }
}
