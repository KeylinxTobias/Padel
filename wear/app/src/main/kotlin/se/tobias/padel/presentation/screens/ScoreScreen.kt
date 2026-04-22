package se.tobias.padel.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.*
import se.tobias.padel.presentation.MatchState
import se.tobias.padel.presentation.Team

@Composable
fun ScoreScreen(
    state: MatchState,
    syncOk: Boolean?,
    onPointUs: () -> Unit,
    onPointThem: () -> Unit,
    onUndo: () -> Unit,
    onEndMatchRequest: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Top half: score eller meny (klickbar för att toggla) ─────────────
        AnimatedContent(
            targetState = showMenu,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { showMenu = !showMenu },
            label = "topToggle",
        ) { isMenu ->
            if (isMenu) {
                MenuPanel(onEndMatchRequest = onEndMatchRequest)
            } else {
                ScorePanel(state = state, syncOk = syncOk)
            }
        }

        // ── Bottom half: stora score-knappar (fasta) ─────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 8.dp, start = 0.dp, end = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ScoreButton(
                label = "VI",
                color = MaterialTheme.colorScheme.primary,
                onClick = onPointUs,
                onLongClick = onUndo,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            ScoreButton(
                label = "DOM",
                color = MaterialTheme.colorScheme.error,
                onClick = onPointThem,
                onLongClick = onUndo,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

// ── Score-vy ──────────────────────────────────────────────────────────────────

@Composable
private fun ScorePanel(state: MatchState, syncOk: Boolean?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp, start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ServeIndicator(state, syncOk)

        Spacer(Modifier.height(4.dp))

        // Poäng (stora) med games + set i mitten
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Vår poäng
            Text(
                text = state.pointLabelUs,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )

            // Games + set centrerat mellan poängen
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "${state.gamesUs}–${state.gamesThem}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${state.setsUs}–${state.setsThem}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Deras poäng
            Text(
                text = state.pointLabelThem,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }

    }
}

// ── Meny-vy ───────────────────────────────────────────────────────────────────

@Composable
private fun MenuPanel(onEndMatchRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Meny",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onEndMatchRequest,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text("Avsluta match", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "tryck för att stänga",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}

// ── Score-knapp med long press ────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScoreButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
        contentAlignment = Alignment.Center,

    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }
    }
}

// ── Serve-indikator ───────────────────────────────────────────────────────────

@Composable
private fun ServeIndicator(state: MatchState, syncOk: Boolean?) {
    val serveLabel = buildString {
        append(if (state.servingTeam == Team.US) "Vi" else "Dom")
        append(" – spelare ${state.servingPlayer}")
        if (state.isTiebreak) append(" (TB)")
    }
    // Sync dot: grön = ok, gul = okänt, röd = misslyckades
    val syncColor = when (syncOk) {
        true  -> Color(0xFF4CAF50)
        false -> Color(0xFFEF5350)
        null  -> Color(0xFFFFB300)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
        Spacer(Modifier.width(4.dp))
        Text(text = serveLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(syncColor))
    }
}
