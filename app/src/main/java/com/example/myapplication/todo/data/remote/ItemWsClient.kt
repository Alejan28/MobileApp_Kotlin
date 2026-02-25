package com.example.myapplication.todo.data.remote

import android.util.Log
import com.example.myapplication.core.Api
import com.example.myapplication.core.TAG
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory // 🔑 REQUIRED IMPORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class ItemWsClient(private val okHttpClient: OkHttpClient) {

    // 🔑 FIX 1: Change lateinit var to nullable var to avoid UninitializedPropertyAccessException
    var webSocket: WebSocket? = null

    suspend fun openSocket(
        onEvent: (itemEvent: ItemEvent?) -> Unit,
        onClosed: () -> Unit,
        onFailure: () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            Log.d(TAG, "openSocket")
            val request = Request.Builder().url(Api.wsUrl).build()
            webSocket = okHttpClient.newWebSocket(
                request,
                ItemWebSocketListener(onEvent = onEvent, onClosed = onClosed, onFailure = onFailure)
            )
            // Note: Dispatcher shutdown here might be risky if OkHttpClient is used elsewhere.
            // Consider managing the OkHttpClient lifecycle outside this client.
            okHttpClient.dispatcher.executorService.shutdown()
        }
    }

    fun closeSocket() {
        Log.d(TAG, "closeSocket")

        webSocket?.close(1000, "");
        webSocket = null
    }

    inner class ItemWebSocketListener(
        private val onEvent: (itemEvent: ItemEvent?) -> Unit,
        private val onClosed: () -> Unit,
        private val onFailure: () -> Unit
    ) : WebSocketListener() {

        // 🔑 FIX 2: Add KotlinJsonAdapterFactory to the Moshi Builder
        private val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory()) // <--- REQUIRED FIX
            .build()

        private val itemEventJsonAdapter: JsonAdapter<ItemEvent> =
            moshi.adapter(ItemEvent::class.java)

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "onOpen")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "onMessage string $text")
            val itemEvent = itemEventJsonAdapter.fromJson(text)
            onEvent(itemEvent)
        }

        // ... other WebSocketListener overrides

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "onClosed bytes $code $reason")
            onClosed()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.d(TAG, "onFailure bytes $t")
            onFailure()
        }
    }
    fun authorize(token: String) {
        val auth = """
            {
              "type":"authorization",
              "payload":{
                "token": "$token"
              }
            }
        """.trimIndent()
        Log.d(TAG, "auth $auth")

        // 🔑 FIX: Use the safe call operator (?.)
        // This ensures .send(auth) is only called if webSocket is NOT null.
        webSocket?.send(auth)

        // Optional: Add a log if the socket is null, for debugging
        if (webSocket == null) {
            Log.w(TAG, "Authorization failed: WebSocket is null or closed.")
        }
    }
}