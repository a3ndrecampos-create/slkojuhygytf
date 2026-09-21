package com.listainteligente.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Cores ──────────────────────────────────────────────────────────────────

val Green900  = Color(0xFF1B5E20)
val Green700  = Color(0xFF2E7D32)
val Green500  = Color(0xFF4CAF50)
val Green200  = Color(0xFFA5D6A7)
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
    primary            = Green700,
    onPrimary          = Color.White,
    primaryContainer   = Green100,
    onPrimaryContainer = Green900,
    secondary          = Orange700,
    onSecondary        = Color.White,
    secondaryContainer = Orange100,
    onSecondaryContainer = Orange700,
    tertiary           = Blue700,
    tertiaryContainer  = Blue100,
    error              = Red700,
    errorContainer     = Red100,
    background         = Color(0xFFF7F8F6),
    surface            = Color.White,
    surfaceVariant     = Color(0xFFEDF1EC),
    onSurface          = Gray900,
    onSurfaceVariant   = Gray600,
    outline            = Color(0xFFBDBDBD),
    outlineVariant     = Color(0xFFE0E0E0)
)

private val DarkColors = darkColorScheme(
    primary            = Green200,
    onPrimary          = Green900,
    primaryContainer   = Green700,
    onPrimaryContainer = Green100,
    secondary          = Orange500,
    onSecondary        = Color(0xFF3E2200),
    secondaryContainer = Color(0xFF5A3A00),
    onSecondaryContainer = Orange100,
    tertiary           = Color(0xFF90CAF9),
    tertiaryContainer  = Color(0xFF0D47A1),
    error              = Color(0xFFEF9A9A),
    errorContainer     = Color(0xFF7F0000),
    background         = Color(0xFF121412),
    surface            = Color(0xFF1B1D1B),
    surfaceVariant     = Color(0xFF262A26),
    onSurface          = Color(0xFFE3E3E1),
    onSurfaceVariant   = Color(0xFFAEB4AC),
    outline            = Color(0xFF5B615B),
    outlineVariant     = Color(0xFF3A3E3A)
)

// ── Tipografia ────────────────────────────────────────────────────────────
// Ajusta o peso/tamanho padrão do Material3 pra dar mais hierarquia visual
// entre títulos, preços em destaque e texto secundário.

private val AppTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge    = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium   = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge     = base.bodyLarge.copy(letterSpacing = 0.1.sp),
        labelLarge    = base.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

// ── Formas ────────────────────────────────────────────────────────────────
// Cantos mais arredondados de forma consistente em cards, dialogs e botões.

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ListaInteligenteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = AppTypography,
        shapes      = AppShapes,
        content     = content
    )
}
