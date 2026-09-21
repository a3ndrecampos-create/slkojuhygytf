package com.listainteligente.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Cores ──────────────────────────────────────────────────────────────────

val Green700  = Color(0xFF2E7D32)
val Green500  = Color(0xFF4CAF50)
val Green100  = Color(0xFFE8F5E9)
val Orange700 = Color(0xFFE65100)
val Orange500 = Color(0xFFFF9800)
val Orange100 = Color(0xFFFFF3E0)
val Red700    = Color(0xFFC62828)
val Red100    = Color(0xFFFFEBEE)
val Blue700   = Color(0xFF1565C0)
val Blue100   = Color(0xFFE3F2FD)
val Gray900   = Color(0xFF212121)
val Gray600   = Color(0xFF757575)
val Gray100   = Color(0xFFF5F5F5)

private val LightColors = lightColorScheme(
    primary         = Green700,
    onPrimary       = Color.White,
    primaryContainer = Green100,
    secondary       = Orange700,
    onSecondary     = Color.White,
    secondaryContainer = Orange100,
    error           = Red700,
    background      = Color(0xFFFAFAFA),
    surface         = Color.White,
    onSurface       = Gray900,
    onSurfaceVariant = Gray600,
    outline         = Color(0xFFBDBDBD)
)

@Composable
fun ListaInteligenteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography  = Typography(),
        content     = content
    )
}
