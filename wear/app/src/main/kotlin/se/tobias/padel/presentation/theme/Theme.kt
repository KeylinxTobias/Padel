package se.tobias.padel.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.MaterialTheme

@Composable
fun PadelTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
