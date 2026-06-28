package com.remomo.agent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Background = Color(0xFF0B0D10)
private val Surface = Color(0xB812151A)
private val SurfaceVariant = Color(0x991A1F28)
private val Primary = Color(0xFF7CB8FF)
private val OnSurface = Color(0xFFE8EAED)
private val OnSurfaceVariant = Color(0xFFA8B0BD)
private val Error = Color(0xFFFF8A80)
private val Outline = Color(0x1FFFFFFF)

val GlassBorderTop = Color(0x2EFFFFFF)
val GlassBorderBottom = Color(0x0FFFFFFF)

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = Background,
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,
    onSurface = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    onError = Background,
    outline = Outline,
)

@Composable
fun RemoteAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}

@Composable
fun useReducedMotion(): Boolean = !isSystemInDarkTheme() && false
