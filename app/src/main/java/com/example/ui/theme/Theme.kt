package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ScamShieldDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF002633),
    primaryContainer = Color(0xFF0A2A4A),
    onPrimaryContainer = ElectricCyan,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF23184B),
    onSecondaryContainer = VioletGlow,
    tertiary = CyberTeal,
    onTertiary = Color(0xFF002620),
    tertiaryContainer = Color(0xFF0A3533),
    onTertiaryContainer = CyberTeal,
    background = MidnightNavy,
    onBackground = DarkTextPrimary,
    surface = DeepSpaceBlue,
    onSurface = DarkTextPrimary,
    surfaceVariant = CyberCardSurface,
    onSurfaceVariant = DarkTextSecondary,
    error = HighRiskRed,
    onError = Color.White,
    errorContainer = HighRiskRedDark,
    onErrorContainer = Color(0xFFFFB4AB),
    outline = Color(0xFF283766)
)

private val ScamShieldLightColorScheme = lightColorScheme(
    primary = Color(0xFF006780),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EAFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF5E35B1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADDFF),
    onSecondaryContainer = Color(0xFF21005D),
    tertiary = Color(0xFF006B5E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF9FF2E2),
    onTertiaryContainer = Color(0xFF00201B),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun ScamShieldTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ScamShieldDarkColorScheme else ScamShieldLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
