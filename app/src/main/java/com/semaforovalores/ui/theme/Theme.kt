package com.semaforovalores.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.semaforovalores.model.TripColor

val SemaforoGreen = Color(0xFF22C55E)
val SemaforoYellow = Color(0xFFF59E0B)
val SemaforoRed = Color(0xFFEF4444)

fun TripColor.composeColor(): Color = when (this) {
    TripColor.GREEN -> SemaforoGreen
    TripColor.YELLOW -> SemaforoYellow
    TripColor.RED -> SemaforoRed
}

private val DarkColors = darkColorScheme(
    primary = SemaforoGreen,
    onPrimary = Color(0xFF052E16),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFFCBD5E1),
    onBackground = Color(0xFFF1F5F9)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF15803D),
    onPrimary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    onBackground = Color(0xFF0F172A)
)

@Composable
fun SemaforoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
