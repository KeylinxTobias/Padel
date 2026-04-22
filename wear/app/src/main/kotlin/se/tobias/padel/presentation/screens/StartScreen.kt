package se.tobias.padel.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.*
import se.tobias.padel.presentation.PointSystem
import se.tobias.padel.presentation.Team

@Composable
fun StartScreen(onStart: (PointSystem, Team) -> Unit) {
    var pointSystem by remember { mutableStateOf(PointSystem.GOLDEN) }
    var firstServe by remember { mutableStateOf(Team.US) }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "Padel",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }

        item { Spacer(Modifier.height(4.dp)) }

        // Point system toggle
        item {
            Text(
                text = "Poängsystem",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(PointSystem.GOLDEN to "Guldpoäng", PointSystem.ADVANTAGE to "Fördel").forEach { (ps, label) ->
                    Button(
                        onClick = { pointSystem = ps },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = if (pointSystem == ps)
                            ButtonDefaults.buttonColors()
                        else
                            ButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(label, fontSize = 10.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(4.dp)) }

        // First serve toggle
        item {
            Text(
                text = "Servar först",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Team.US to "Vi", Team.THEM to "Dom").forEach { (team, label) ->
                    Button(
                        onClick = { firstServe = team },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = if (firstServe == team)
                            ButtonDefaults.buttonColors()
                        else
                            ButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(label, fontSize = 12.sp)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        item {
            Button(
                onClick = { onStart(pointSystem, firstServe) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Text("Starta match", fontWeight = FontWeight.Bold)
            }
        }
    }
}
