package se.tobias.padel.presentation

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WearDataSender {

    private const val TAG = "WearDataSender"
    private const val PATH_EVENT = "/padel/event"
    private const val PATH_END = "/padel/end"

    /** Returns true if message was sent to at least one node. */
    suspend fun sendPointEvent(context: Context, state: MatchState, team: Team): Boolean {
        val payload = buildEventPayload(state, team)
        return sendMessage(context, PATH_EVENT, payload)
    }

    suspend fun sendMatchEnd(context: Context, matchId: String): Boolean {
        val payload = """{"matchId":"$matchId"}"""
        return sendMessage(context, PATH_END, payload)
    }

    private fun buildEventPayload(state: MatchState, team: Team): String {
        val snapshot = Json.encodeToString(state)
        val clientEventId = java.util.UUID.randomUUID()
        return """{"matchId":"${state.matchId}","clientEventId":"$clientEventId","team":"${team.name}","timestamp":"${java.time.Instant.now()}","scoreSnapshot":${Json.encodeToString(snapshot)}}"""
    }

    private suspend fun sendMessage(context: Context, path: String, payload: String): Boolean {
        return try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.w(TAG, "No connected nodes")
                return false
            }
            for (node in nodes) {
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, path, payload.toByteArray(Charsets.UTF_8))
                    .await()
                Log.d(TAG, "Sent $path to ${node.displayName}")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}")
            false
        }
    }
}
