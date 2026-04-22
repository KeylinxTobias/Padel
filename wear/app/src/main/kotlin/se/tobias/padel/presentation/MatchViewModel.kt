package se.tobias.padel.presentation

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MatchViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(MatchState())
    val state: StateFlow<MatchState> = _state.asStateFlow()

    // null = okänt, true = senaste sync lyckades, false = misslyckades
    private val _syncOk = MutableStateFlow<Boolean?>(null)
    val syncOk: StateFlow<Boolean?> = _syncOk.asStateFlow()

    // Undo stack — each entry is the full state BEFORE that point was added
    private val undoStack = ArrayDeque<MatchState>(50)

    fun startMatch(pointSystem: PointSystem, firstServe: Team) {
        undoStack.clear()
        _state.value = MatchState(
            pointSystem = pointSystem,
            servingTeam = firstServe,
        )
    }

    fun addPoint(team: Team) {
        val current = _state.value
        if (current.matchEnded) return

        undoStack.addLast(current)
        if (undoStack.size > 50) undoStack.removeFirst()

        val next = ScoreEngine.addPoint(current, team)
        _state.value = next

        triggerHaptic(current, next)

        viewModelScope.launch {
            val ok = WearDataSender.sendPointEvent(context, next, team)
            _syncOk.value = ok
        }
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        _state.value = undoStack.removeLast()
    }

    fun endMatch() {
        val matchId = _state.value.matchId
        _state.value = _state.value.copy(matchEnded = true)
        vibrate(longArrayOf(0, 100, 50, 100, 50, 200))
        viewModelScope.launch {
            WearDataSender.sendMatchEnd(context, matchId)
        }
    }

    // ── Haptic feedback ───────────────────────────────────────────────────────

    private fun triggerHaptic(before: MatchState, after: MatchState) {
        when {
            // Match-level event (set won)
            after.setsUs != before.setsUs || after.setsThem != before.setsThem ->
                vibrate(longArrayOf(0, 100, 50, 100, 50, 100))
            // Game won
            after.gamesUs != before.gamesUs || after.gamesThem != before.gamesThem ->
                vibrate(longArrayOf(0, 80, 40, 80))
            // Normal point
            else ->
                vibrate(longArrayOf(0, 40))
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(pattern: LongArray) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MatchViewModel(context.applicationContext) as T
    }
}
