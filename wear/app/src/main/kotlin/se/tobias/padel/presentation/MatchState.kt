package se.tobias.padel.presentation

import kotlinx.serialization.Serializable
import java.util.UUID

enum class Team { US, THEM }
enum class PointSystem { GOLDEN, ADVANTAGE }

/**
 * Immutable snapshot of the full match state at any point in time.
 * Used both as live state and as undo-stack entries.
 */
@Serializable
data class MatchState(
    val matchId: String = UUID.randomUUID().toString(),
    val pointSystem: PointSystem = PointSystem.GOLDEN,

    // Points within current game (0=0, 1=15, 2=30, 3=40)
    val pointsUs: Int = 0,
    val pointsThem: Int = 0,
    val isDeuce: Boolean = false,
    val advantageTeam: Team? = null,   // only used in ADVANTAGE mode

    // Games in current set
    val gamesUs: Int = 0,
    val gamesThem: Int = 0,
    val isTiebreak: Boolean = false,

    // Sets won
    val setsUs: Int = 0,
    val setsThem: Int = 0,

    // Serve
    val servingTeam: Team = Team.US,
    val servingPlayer: Int = 1,        // 1 or 2

    // Total games played (used to track serve rotation)
    val totalGamesPlayed: Int = 0,

    val matchEnded: Boolean = false,
) {
    val pointLabelUs: String get() = pointLabel(pointsUs, pointsThem, isDeuce, advantageTeam, isTiebreak, Team.US)
    val pointLabelThem: String get() = pointLabel(pointsThem, pointsUs, isDeuce, advantageTeam, isTiebreak, Team.THEM)
}

private fun pointLabel(
    myPoints: Int,
    theirPoints: Int,
    isDeuce: Boolean,
    advantageTeam: Team?,
    isTiebreak: Boolean,
    me: Team,
): String {
    // Tie-break: plain numbers (0, 1, 2 … 7+)
    if (isTiebreak) return myPoints.toString()

    // Regular game: tennis notation
    return when {
        isDeuce && advantageTeam == me -> "A"
        isDeuce && advantageTeam != null && advantageTeam != me -> ""
        isDeuce -> "D"
        else -> when (myPoints) {
            0 -> "0"
            1 -> "15"
            2 -> "30"
            3 -> "40"
            else -> ""
        }
    }
}
