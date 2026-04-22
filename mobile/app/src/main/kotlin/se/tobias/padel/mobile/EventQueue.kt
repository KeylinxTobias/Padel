package se.tobias.padel.mobile

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentLinkedQueue

object EventQueue {

    private const val TAG = "EventQueue"
    private val queue = ConcurrentLinkedQueue<QueuedEvent>()

    private val _sizeFlow = MutableStateFlow(0)
    val sizeFlow: StateFlow<Int> = _sizeFlow.asStateFlow()

    data class QueuedEvent(val url: String, val body: String, val method: String = "POST")

    fun enqueue(url: String, body: String, method: String = "POST") {
        queue.add(QueuedEvent(url, body, method))
        _sizeFlow.value = queue.size
        Log.w(TAG, "Queued event — queue size: ${queue.size}")
    }

    fun flush(http: OkHttpClient, json: okhttp3.MediaType): Int {
        if (queue.isEmpty()) return 0
        var sent = 0
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val event = iterator.next()
            try {
                val request = Request.Builder()
                    .url(event.url)
                    .method(event.method, event.body.toRequestBody(json))
                    .build()
                http.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        iterator.remove()
                        sent++
                        _sizeFlow.value = queue.size
                        Log.d(TAG, "Flushed queued event — ${queue.size} remaining")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Flush failed, keeping in queue: ${e.message}")
                break
            }
        }
        return sent
    }

    val size: Int get() = queue.size
}
