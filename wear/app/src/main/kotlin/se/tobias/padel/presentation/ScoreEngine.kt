package se.tobias.padel.presentation

/**
 * Pure scoring logic. All functions are stateless — they take a MatchState and return a new one.
 */
object ScoreEngine {

    fun addPoint(state: MatchState, team: Team): MatchState {
        if (state.matchEnded) return state
        return if (state.isTiebreak) handleTiebreakPoint(state, team)
        else handleRegularPoint(state, team)
    }

    // ── Regular game ──────────────────────────────────────────────────────────

    private fun handleRegularPoint(state: MatchState, team: Team): MatchState {
        val us = state.pointsUs
        val them = state.pointsThem

        // Deuce / advantage handling
        if (state.isDeuce) {
            return when (state.pointSystem) {
                PointSystem.GOLDEN -> gameWon(state, team)   // golden point: one point decides
                PointSystem.ADVANTAGE -> {
                    when {
                        state.advantageTeam == null -> state.copy(advantageTeam = team)
                        state.advantageTeam == team -> gameWon(state, team)
                        else -> state.copy(advantageTeam = null) // back to deuce
                    }
                }
            }
        }

        // Normal point progression
        val newUs = if (team == Team.US) us + 1 else us
        val newThem = if (team == Team.THEM) them + 1 else them

        // Check deuce (both at 40 = index 3)
        if (newUs == 3 && newThem == 3) {
            return state.copy(pointsUs = newUs, pointsThem = newThem, isDeuce = true, advantageTeam = null)
        }

        // Check game won (reached index 4)
        if (newUs == 4 || newThem == 4) {
            return gameWon(state, team)
        }

        return state.copy(pointsUs = newUs, pointsThem = newThem)
    }

    // ── Tiebreak ─────────────────────────────────────────────────────────────

    private fun handleTiebreakPoint(state: MatchState, team: Team): MatchState {
        val newUs = if (team == Team.US) state.pointsUs + 1 else state.pointsUs
        val newThem = if (team == Team.THEM) state.pointsThem + 1 else state.pointsThem

        // Win tiebreak: first to 7+, win by 2
        val winner = when {
            newUs >= 7 && newUs - newThem >= 2 -> Team.US
            newThem >= 7 && newThem - newUs >= 2 -> Team.THEM
            else -> null
        }

        val afterPoint = state.copy(pointsUs = newUs, pointsThem = newThem)

        // Serve changes every 2 points in tiebreak (after first point, then every 2)
        val totalPoints = newUs + newThem
        val newServe = if (totalPoints % 2 == 1) {
            rotateServingTeam(afterPoint)
        } else {
            afterPoint
        }

        return if (winner != null) setWon(newServe, winner) else newServe
    }

    // ── Game won ─────────────────────────────────────────────────────────────

    private fun gameWon(state: MatchState, team: Team): MatchState {
        val newGamesUs = if (team == Team.US) state.gamesUs + 1 else state.gamesUs
        val newGamesThem = if (team == Team.THEM) state.gamesThem + 1 else state.gamesThem
        val newTotal = state.totalGamesPlayed + 1

        val afterGame = state.copy(
            pointsUs = 0, pointsThem = 0,
            isDeuce = false, advantageTeam = null,
            gamesUs = newGamesUs, gamesThem = newGamesThem,
            totalGamesPlayed = newTotal,
        )

        // Rotate serve after each game (+ rotate player every 2 games)
        val afterServe = rotateServingTeam(afterGame)
            .let { rotateServingPlayer(it, newTotal) }

        // Check set won
        val setWinner = when {
            newGamesUs >= 6 && newGamesUs - newGamesThem >= 2 -> Team.US
            newGamesThem >= 6 && newGamesThem - newGamesUs >= 2 -> Team.THEM
            newGamesUs == 7 || newGamesThem == 7 -> team  // 7-5
            else -> null
        }

        // Check tiebreak at 6-6
        val tiebreak = newGamesUs == 6 && newGamesThem == 6

        return when {
            setWinner != null -> setWon(afterServe, setWinner)
            tiebreak -> afterServe.copy(isTiebreak = true)
            else -> afterServe
        }
    }

    // ── Set won ──────────────────────────────────────────────────────────────

    private fun setWon(state: MatchState, team: Team): MatchState {
        val newSetsUs = if (team == Team.US) state.setsUs + 1 else state.setsUs
        val newSetsThem = if (team == Team.THEM) state.setsThem + 1 else state.setsThem

        return state.copy(
            pointsUs = 0, pointsThem = 0,
            isDeuce = false, advantageTeam = null,
            gamesUs = 0, gamesThem = 0,
            isTiebreak = false,
            setsUs = newSetsUs,
            setsThem = newSetsThem,
        )
    }

    // ── Serve helpers ─────────────────────────────────────────────────────────

    private fun rotateServingTeam(state: MatchState): MatchState =
        state.copy(servingTeam = if (state.servingTeam == Team.US) Team.THEM else Team.US)

    /** Rotate serving player within team every 2 games */
    private fun rotateServingPlayer(state: MatchState, totalGames: Int): MatchState =
        if (totalGames % 2 == 0) state
        else state.copy(servingPlayer = if (state.servingPlayer == 1) 2 else 1)
}
