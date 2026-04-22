package se.tobias.padel.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import se.tobias.padel.presentation.screens.*
import se.tobias.padel.presentation.theme.PadelTheme

private const val ROUTE_START = "start"
private const val ROUTE_SCORE = "score"
private const val ROUTE_END_DIALOG = "end_dialog"
private const val ROUTE_SUMMARY = "summary"

class MainActivity : ComponentActivity() {

    private val viewModel: MatchViewModel by viewModels {
        MatchViewModel.Factory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PadelTheme {
                PadelApp(viewModel)
            }
        }
    }
}

@Composable
private fun PadelApp(viewModel: MatchViewModel) {
    val navController = rememberSwipeDismissableNavController()
    val state by viewModel.state.collectAsState()
    val syncOk by viewModel.syncOk.collectAsState()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = ROUTE_START,
    ) {
        composable(ROUTE_START) {
            StartScreen(
                onStart = { pointSystem, firstServe ->
                    viewModel.startMatch(pointSystem, firstServe)
                    navController.navigate(ROUTE_SCORE) {
                        popUpTo(ROUTE_START) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_SCORE) {
            if (state.matchEnded) {
                navController.navigate(ROUTE_SUMMARY) {
                    popUpTo(ROUTE_SCORE) { inclusive = true }
                }
            } else {
                ScoreScreen(
                    state = state,
                    syncOk = syncOk,
                    onPointUs = { viewModel.addPoint(Team.US) },
                    onPointThem = { viewModel.addPoint(Team.THEM) },
                    onUndo = { viewModel.undo() },
                    onEndMatchRequest = { navController.navigate(ROUTE_END_DIALOG) },
                )
            }
        }

        composable(ROUTE_END_DIALOG) {
            EndMatchDialog(
                state = state,
                onConfirm = {
                    viewModel.endMatch()
                    navController.navigate(ROUTE_SUMMARY) {
                        popUpTo(ROUTE_SCORE) { inclusive = true }
                    }
                },
                onDismiss = { navController.popBackStack() },
            )
        }

        composable(ROUTE_SUMMARY) {
            MatchSummaryScreen(
                state = state,
                onNewMatch = {
                    navController.navigate(ROUTE_START) {
                        popUpTo(ROUTE_SUMMARY) { inclusive = true }
                    }
                },
            )
        }
    }
}
