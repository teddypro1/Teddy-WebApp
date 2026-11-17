package com.teddy.bpositive.data

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Subscribes to row changes for the signed-in user's notes.
 *
 * Realtime events are treated as invalidation signals: the caller fetches the
 * latest rows through the existing REST API, preserving its RLS and merge rules.
 */
class SupabaseRealtimeSubscription(
    private val session: SupabaseSessionManager.Session,
    private val onNotesChanged: () -> Unit
) {
    @Volatile private var currentSession = session
    @Volatile private var refreshingSession = false
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    @Volatile private var stopped = false
    @Volatile private var socket: WebSocket? = null
    private var reconnectDelayMs = 2_000L
    private var heartbeatRef = 0

    private val heartbeat = object : Runnable {
        override fun run() {
            if (stopped) return
            val message = JSONObject()
                .put("topic", "phoenix")
                .put("event", "heartbeat")
                .put("payload", JSONObject())
                .put("ref", "hb-${++heartbeatRef}")
            socket?.send(message.toString())
            handler.postDelayed(this, 25_000L)
        }
    }

    fun start() {
        stopped = false
        connect()
    }

    fun stop() {
        stopped = true
        handler.removeCallbacks(heartbeat)
        socket?.close(1000, "Subscription stopped")
        socket = null
        client.dispatcher.cancelAll()
        client.connectionPool.evictAll()
    }

    private fun connect() {
        val activeSession = currentSession
        if (stopped || activeSession.userId.isNullOrBlank()) return

        val apiKey = URLEncoder.encode(SupabaseConfig.API_KEY, "UTF-8")
        val url = SupabaseConfig.BASE_URL
            .replaceFirst("https://", "wss://")
            .trimEnd('/') +
            "/realtime/v1/websocket?apikey=$apiKey&vsn=1.0.0"

        val request = Request.Builder()
            .url(url)
            .build()

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (stopped) {
                    webSocket.close(1000, "Subscription stopped")
                    return
                }
                socket = webSocket
                reconnectDelayMs = 2_000L

                val config = JSONObject()
                    .put("broadcast", JSONObject().put("ack", false).put("self", false))
                    .put("presence", JSONObject().put("key", ""))
                    .put(
                        "postgres_changes",
                        org.json.JSONArray().put(
                            JSONObject()
                                .put("event", "*")
                                .put("schema", "public")
                                .put("table", "notes")
                                .put("filter", "user_id=eq.${activeSession.userId}")
                        )
                    )
                    .put("private", false)

                val joinPayload = JSONObject()
                    .put("config", config)
                    .put("access_token", activeSession.accessToken)

                val join = JSONObject()
                    .put("topic", "realtime:public:notes")
                    .put("event", "phx_join")
                    .put("payload", joinPayload)
                    .put("ref", "1")

                webSocket.send(join.toString())
                handler.removeCallbacks(heartbeat)
                handler.postDelayed(heartbeat, 25_000L)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = JSONObject(text)
                    val event = message.optString("event")
                    if (event == "postgres_changes") {
                        handler.post {
                            if (!stopped) onNotesChanged()
                        }
                    } else if (event == "phx_reply") {
                        val payload = message.optJSONObject("payload")
                        if (payload?.optString("status") == "ok") {
                            reconnectDelayMs = 2_000L
                            if (message.optString("ref") == "1") {
                                handler.post {
                                    if (!stopped) onNotesChanged()
                                }
                            }
                        }
                    } else if (event == "phx_error" || event == "phx_close") {
                        val detail = message.optJSONObject("payload")?.toString().orEmpty().lowercase()
                        webSocket.close(1011, "Realtime channel unavailable")
                        if (detail.contains("jwt") || detail.contains("token") ||
                            detail.contains("unauthorized") || detail.contains("expired")) {
                            refreshAndReconnect(activeSession)
                        }
                    }
                } catch (_: Exception) {
                    // Ignore malformed or unrelated protocol messages.
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handler.removeCallbacks(heartbeat)
                if (socket === webSocket) socket = null
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handler.removeCallbacks(heartbeat)
                if (socket === webSocket) socket = null
                if (response?.code == 401) {
                    refreshAndReconnect(activeSession)
                } else {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun refreshAndReconnect(rejectedSession: SupabaseSessionManager.Session) {
        if (stopped || refreshingSession) return
        refreshingSession = true
        SupabaseSessionManager.refreshAfterUnauthorized(rejectedSession) { result ->
            refreshingSession = false
            val refreshed = result.session
            if (result.success && refreshed != null) {
                currentSession = refreshed
                handler.removeCallbacks(heartbeat)
                socket?.close(1000, "Refreshing authenticated Realtime session")
                socket = null
                handler.postDelayed({ if (!stopped) connect() }, 300L)
            } else {
                scheduleReconnect()
            }
        }
    }
    private fun scheduleReconnect() {
        if (stopped || refreshingSession) return
        val delay = reconnectDelayMs
        reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30_000L)
        handler.postDelayed({ if (!stopped && socket == null) connect() }, delay)
    }
}
