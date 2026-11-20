package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BharatDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = DeepVioletContainer,
    onPrimaryContainer = SoftViolet,
    secondary = ElectricBlue,
    onSecondary = ObsidianBlack,
    secondaryContainer = DeepBlueContainer,
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = SaffronGold,
    onTertiary = ObsidianBlack,
    tertiaryContainer = DeepSaffronContainer,
    onTertiaryContainer = WarmAmber,
    background = ObsidianBlack,
    onBackground = InkPrimary,
    surface = DeepNavy,
    onSurface = InkPrimary,
    surfaceVariant = StudioSurface,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = DeepNavy,
    surfaceContainer = StudioSurface,
    surfaceContainerHigh = StudioSurfaceElevated,
    surfaceContainerHighest = Color(0xFF212E57),
    outline = StudioCardBorder,
    outlineVariant = StudioDivider,
    error = StatusRose,
    onError = Color.White,
    errorContainer = StatusRoseContainer,
    onErrorContainer = Color(0xFFFDA4AF)
)

@Immutable
data class StudioExtendedColors(
    val cardBorder: Color = StudioCardBorder,
    val success: Color = StatusEmerald,
    val successContainer: Color = StatusEmeraldContainer,
    val accentBlue: Color = ElectricBlue,
    val accentCobalt: Color = CobaltBlue,
    val accentSaffron: Color = SaffronGold,
    val inkMuted: Color = InkMuted
)

val LocalStudioColors = staticCompositionLocalOf { StudioExtendedColors() }

val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun BharatStoryboardTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalStudioColors provides StudioExtendedColors()) {
        MaterialTheme(
            colorScheme = BharatDarkColorScheme,
            typography = Typography,
            shapes = StudioShapes,
            content = content
        )
    }
}
