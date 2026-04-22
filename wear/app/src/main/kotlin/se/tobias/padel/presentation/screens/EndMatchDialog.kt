package se.tobias.padel.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.*
import se.tobias.padel.presentation.MatchState

@Composable
fun EndMatchDialog(
    state: MatchState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Avsluta match?",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Set: ${state.setsUs}–${state.setsThem}\nGames: ${state.gamesUs}–${state.gamesThem}",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Avbryt", fontSize = 12.sp)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Ja, avsluta", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun MatchSummaryScreen(state: MatchState, onNewMatch: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Match slut!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "Set\n${state.setsUs} – ${state.setsThem}",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onNewMatch) {
                Text("Ny match")
            }
        }
    }
}
