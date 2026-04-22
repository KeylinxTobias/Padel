package se.tobias.padel.mobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val http = OkHttpClient()
    private val json = "application/json".toMediaType()
    private val backendUrl = BuildConfig.BACKEND_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                DebugScreen(
                    backendUrl = backendUrl,
                    http = http,
                    json = json,
                )
            }
        }
    }
}

@Composable
private fun DebugScreen(
    backendUrl: String,
    http: OkHttpClient,
    json: okhttp3.MediaType,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var backendStatus by remember { mutableStateOf("–") }
    var watchNodes by remember { mutableStateOf("–") }
    val queueSize by EventQueue.sizeFlow.collectAsStateWithLifecycle()
    val log = remember { mutableStateListOf<LogEntry>() }
    val listState = rememberLazyListState()

    fun addLog(msg: String, ok: Boolean = true) {
        log.add(0, LogEntry(msg, ok))
        if (log.size > 50) log.removeAt(log.lastIndex)
    }

    // Lyssna på broadcasts från WearableListenerService
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val path = intent.getStringExtra(PadelWearListenerService.EXTRA_PATH) ?: ""
                val status = intent.getStringExtra(PadelWearListenerService.EXTRA_STATUS) ?: ""
                addLog("Watch event: $path → $status", status == "ok")
            }
        }
        val filter = IntentFilter(PadelWearListenerService.ACTION_EVENT_RECEIVED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Padel Debug", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            // ── Statuskort ────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip("Backend", backendStatus)
                StatusChip("Klocka", watchNodes)
                StatusChip("Kö", "$queueSize")
            }

            // ── Knappar ───────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                Button(
                    onClick = {
                        scope.launch {
                            backendStatus = "kollar…"
                            val (ok, msg) = checkBackendHealth(http, backendUrl)
                            backendStatus = if (ok) "✓ online" else "✗ offline"
                            addLog("Health: $msg", ok)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Hälsokontroll backend") }

                Button(
                    onClick = {
                        scope.launch {
                            watchNodes = "söker…"
                            val nodes = withContext(Dispatchers.IO) {
                                try {
                                    Wearable.getNodeClient(context).connectedNodes.await()
                                } catch (e: Exception) { emptyList() }
                            }
                            watchNodes = if (nodes.isEmpty()) "ingen" else nodes.joinToString { it.displayName }
                            addLog("Anslutna klockor: ${nodes.map { it.displayName }}", nodes.isNotEmpty())
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Kolla watch-anslutning") }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val (ok, msg) = sendTestEvent(http, json, backendUrl)
                            addLog("Test-event: $msg", ok)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skicka test-event till backend") }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val sent = withContext(Dispatchers.IO) {
                                EventQueue.flush(http, json)
                            }
                            addLog("Flush: skickade $sent events, ${EventQueue.size} kvar", sent > 0 || EventQueue.size == 0)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Töm offline-kön (${queueSize} events)") }
            }

            HorizontalDivider()

            // ── Logg ──────────────────────────────────────────────────────────
            Text("Logg", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                items(log) { entry ->
                    Text(
                        text = "${entry.time}  ${entry.msg}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (entry.ok) Color(0xFF81C784) else Color(0xFFEF9A9A),
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, value: String) {
    val ok = value.startsWith("✓") || value.all { it.isDigit() } && value == "0"
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (ok) Color(0xFF1B5E20) else Color(0xFF37474F),
        modifier = Modifier.padding(0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, fontSize = 10.sp, color = Color(0xFFB0BEC5))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private data class LogEntry(val msg: String, val ok: Boolean, val time: String = Instant.now().toString().substring(11, 19))

// ── Hjälpfunktioner ───────────────────────────────────────────────────────────

private suspend fun checkBackendHealth(http: OkHttpClient, backendUrl: String): Pair<Boolean, String> =
    withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$backendUrl/health").get().build()
            http.newCall(req).execute().use { resp ->
                Pair(resp.isSuccessful, "${resp.code} ${resp.body?.string()?.take(80)}")
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "okänt fel")
        }
    }

private suspend fun sendTestEvent(http: OkHttpClient, json: okhttp3.MediaType, backendUrl: String): Pair<Boolean, String> =
    withContext(Dispatchers.IO) {
        try {
            // 1. Skapa en testmatch
            val matchBody = """{"pointSystem":"GOLDEN","servingTeam":"US"}"""
            val matchReq = Request.Builder()
                .url("$backendUrl/api/matches")
                .post(matchBody.toRequestBody(json))
                .build()
            val matchId = http.newCall(matchReq).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Pair(false, "Kunde inte skapa match: ${resp.code}")
                // Enkel UUID-extraktion ur JSON-svaret
                val body = resp.body?.string() ?: ""
                Regex(""""id"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
                    ?: return@withContext Pair(false, "Inget match-ID i svar")
            }

            // 2. Skicka ett test-event
            val eventBody = """
                {
                  "clientEventId": "${UUID.randomUUID()}",
                  "team": "US",
                  "timestamp": "${Instant.now()}",
                  "scoreSnapshot": "{\"pointsUs\":1,\"pointsThem\":0}"
                }
            """.trimIndent()
            val eventReq = Request.Builder()
                .url("$backendUrl/api/matches/$matchId/events")
                .post(eventBody.toRequestBody(json))
                .build()
            http.newCall(eventReq).execute().use { resp ->
                Pair(resp.isSuccessful, "match=$matchId → event ${resp.code}")
            }
        } catch (e: Exception) {
            Pair(false, e.message ?: "okänt fel")
        }
    }
