package se.tobias.padel.mobile

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class PadelWearListenerService : WearableListenerService() {

    private val tag = "PadelWearListener"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient()
    private val json = "application/json".toMediaType()
    private val backendUrl: String get() = BuildConfig.BACKEND_URL

    companion object {
        const val ACTION_EVENT_RECEIVED = "se.tobias.padel.EVENT_RECEIVED"
        const val EXTRA_PATH = "path"
        const val EXTRA_STATUS = "status"
        const val EXTRA_QUEUE_SIZE = "queueSize"
    }

    override fun onMessageReceived(event: MessageEvent) {
        val payload = String(event.data, Charsets.UTF_8)
        Log.d(tag, "Received ${event.path}: $payload")

        scope.launch {
            // Try to flush any previously queued events first
            EventQueue.flush(http, json)

            val success = when (event.path) {
                "/padel/event" -> forwardEvent(payload)
                "/padel/end"   -> forwardEnd(payload)
                else -> { Log.w(tag, "Unknown path: ${event.path}"); true }
            }

            // Broadcast result to MainActivity (if open)
            broadcastStatus(event.path, success)
        }
    }

    private fun forwardEvent(body: String): Boolean {
        val matchId = extractField(body, "matchId") ?: run {
            Log.e(tag, "No matchId in payload"); return false
        }
        return post("$backendUrl/api/matches/$matchId/events", body)
    }

    private fun forwardEnd(body: String): Boolean {
        val matchId = extractField(body, "matchId") ?: run {
            Log.e(tag, "No matchId in end payload"); return false
        }
        return patch("$backendUrl/api/matches/$matchId/end", "{}")
    }

    private fun post(url: String, body: String): Boolean = send("POST", url, body)
    private fun patch(url: String, body: String): Boolean = send("PATCH", url, body)

    private fun send(method: String, url: String, body: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .method(method, body.toRequestBody(json))
                .build()
            http.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(tag, "$method $url → ${response.code}")
                    true
                } else {
                    Log.w(tag, "$method $url → ${response.code}, queuing")
                    EventQueue.enqueue(url, body, method)
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Network error, queuing: ${e.message}")
            EventQueue.enqueue(url, body, method)
            false
        }
    }

    private fun broadcastStatus(path: String, success: Boolean) {
        sendBroadcast(Intent(ACTION_EVENT_RECEIVED).apply {
            putExtra(EXTRA_PATH, path)
            putExtra(EXTRA_STATUS, if (success) "ok" else "queued")
            putExtra(EXTRA_QUEUE_SIZE, EventQueue.size)
        })
    }

    private fun extractField(json: String, field: String): String? =
        Regex(""""$field"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
}
