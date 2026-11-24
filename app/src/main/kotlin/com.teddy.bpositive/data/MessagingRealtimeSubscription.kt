package com.teddy.bpositive.data

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Conversation-scoped Postgres Changes listener.
 * The channel is public because the project currently allows public channels;
 * message row visibility is still enforced by messages-table RLS.
 */
class MessagingRealtimeSubscription(
    private val session: SupabaseSessionManager.Session,
    private val conversationId: String,
    private val onMessagesChanged: () -> Unit
) {
    @Volatile private var currentSession = session
    @Volatile private var stopped = false
    @Volatile private var socket: WebSocket? = null
    @Volatile private var refreshing = false
    private val handler = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()
    private var reconnectDelayMs = 2_000L
    private var heartbeatRef = 0

    private val heartbeat = object : Runnable {
        override fun run() {
            if (stopped) return
            socket?.send(
                JSONObject().put("topic", "phoenix").put("event", "heartbeat")
                    .put("payload", JSONObject()).put("ref", "msg-hb-${++heartbeatRef}").toString()
            )
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
        socket?.close(1000, "Conversation screen closed")
        socket = null
        client.dispatcher.cancelAll()
        client.connectionPool.evictAll()
    }

    private fun connect() {
        val active = currentSession
        if (stopped || active.userId.isNullOrBlank() || conversationId.isBlank()) return
        val apiKey = URLEncoder.encode(SupabaseConfig.API_KEY, "UTF-8")
        val url = SupabaseConfig.BASE_URL.replaceFirst("https://", "wss://").trimEnd('/') +
            "/realtime/v1/websocket?apikey=$apiKey&vsn=1.0.0"
        socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (stopped) { webSocket.close(1000, "Stopped"); return }
                socket = webSocket
                reconnectDelayMs = 2_000L
                val config = JSONObject()
                    .put("broadcast", JSONObject().put("ack", false).put("self", false))
                    .put("presence", JSONObject().put("key", ""))
                    .put("postgres_changes", JSONArray().put(
                        JSONObject().put("event", "INSERT").put("schema", "public")
                            .put("table", "messages")
                            .put("filter", "conversation_id=eq.$conversationId")
                    ))
                    .put("private", true)
                val join = JSONObject().put("topic", "realtime:messaging:conversation:$conversationId")
                    .put("event", "phx_join")
                    .put("payload", JSONObject().put("config", config).put("access_token", active.accessToken))
                    .put("ref", "1")
                webSocket.send(join.toString())
                handler.removeCallbacks(heartbeat)
                handler.postDelayed(heartbeat, 25_000L)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = JSONObject(text)
                    when (message.optString("event")) {
                        "postgres_changes" -> handler.post { if (!stopped) onMessagesChanged() }
                        "phx_reply" -> {
                            val payload = message.optJSONObject("payload")
                            if (payload?.optString("status") == "ok") {
                                reconnectDelayMs = 2_000L
                                if (message.optString("ref") == "1") {
                                    handler.post { if (!stopped) onMessagesChanged() }
                                }
                            } else if (payload?.optString("status") == "error") {
                                webSocket.close(1011, "Realtime join rejected")
                            }
                        }
                        "phx_error", "phx_close" -> {
                            val detail = message.optJSONObject("payload")?.toString().orEmpty().lowercase()
                            if (detail.contains("jwt") || detail.contains("token") ||
                                detail.contains("unauthorized") || detail.contains("expired")
                            ) refreshAndReconnect(active)
                            else webSocket.close(1011, "Realtime channel unavailable")
                        }
                    }
                } catch (_: Exception) { }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handler.removeCallbacks(heartbeat)
                if (socket === webSocket) socket = null
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handler.removeCallbacks(heartbeat)
                if (socket === webSocket) socket = null
                if (response?.code == 401) refreshAndReconnect(active) else scheduleReconnect()
            }
        })
    }

    private fun refreshAndReconnect(rejected: SupabaseSessionManager.Session) {
        if (stopped || refreshing) return
        refreshing = true
        SupabaseSessionManager.refreshAfterUnauthorized(rejected) { result ->
            refreshing = false
            val renewed = result.session
            if (result.success && renewed != null) {
                currentSession = renewed
                handler.removeCallbacks(heartbeat)
                socket?.close(1000, "Refreshing session")
                socket = null
                handler.postDelayed({ if (!stopped) connect() }, 300L)
            } else scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (stopped || refreshing) return
        val delay = reconnectDelayMs
        reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30_000L)
        handler.postDelayed({ if (!stopped && socket == null) connect() }, delay)
    }
}
