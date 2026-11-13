package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FFIDarkColorScheme = darkColorScheme(
    primary = NeonOrange,
    onPrimary = MatteBlack,
    primaryContainer = NeonOrangeDim,
    onPrimaryContainer = NeonOrangeBright,
    secondary = CrimsonRed,
    onSecondary = TextPrimary,
    secondaryContainer = CrimsonDim,
    onSecondaryContainer = CrimsonRed,
    tertiary = TacticalGreen,
    onTertiary = MatteBlack,
    tertiaryContainer = TacticalGreenDim,
    onTertiaryContainer = TacticalGreen,
    background = MatteBlack,
    onBackground = TextPrimary,
    surface = ObsidianPanel,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedDarkGrey,
    onSurfaceVariant = TextSecondary,
    outline = TacticalBorder,
    outlineVariant = ElevatedDarkGrey,
    error = CrimsonRed,
    onError = TextPrimary
)

val FFIShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun FFITrainerProTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FFIDarkColorScheme,
        typography = FFITypography,
        shapes = FFIShapes,
        content = content
    )
}
