package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class WebSocketManager {
    private val client = OkHttpClient()
    private var ws: WebSocket? = null
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val messages = _messages.asSharedFlow()

    fun connect() {
        if (ws != null) return
        val request = Request.Builder().url(ApiConfig.WS_URL).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // pripojeno
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                _messages.tryEmit(text)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                ws = null
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ connect() }, 3000)
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                ws = null
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ connect() }, 3000)
            }
        })
    }

    fun disconnect() { ws?.close(1000, "bye"); ws = null }
}