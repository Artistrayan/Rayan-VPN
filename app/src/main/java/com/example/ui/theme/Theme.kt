package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vpn.model.ContrastMode

@Immutable
data class ContrastCustomTokens(
    val cardBackground: Color,
    val cardBorder: Color,
    val cardBorderWidth: Dp,
    val accentGlow: Color,
    val powerButtonBorder: Color,
    val isHighContrast: Boolean,
    val speedGaugeColor: Color,
    val successColor: Color,
    val warningColor: Color
)

val LocalContrastTokens = staticCompositionLocalOf {
    ContrastCustomTokens(
        cardBackground = CyberNavyCard,
        cardBorder = CyberNavySurface,
        cardBorderWidth = 1.dp,
        accentGlow = CyberCyan,
        powerButtonBorder = CyberCyan,
        isHighContrast = false,
        speedGaugeColor = CyberCyan,
        successColor = CyberGreen,
        warningColor = CyberAmber
    )
}

private val CyberDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    primaryContainer = CyberNavySurface,
    onPrimaryContainer = CyberCyan,
    secondary = CyberCyanMuted,
    onSecondary = Color.Black,
    background = CyberNavyDark,
    onBackground = CyberTextPrimary,
    surface = CyberNavySurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberNavyCard,
    onSurfaceVariant = CyberTextSecondary,
    outline = Color(0xFF2C395B)
)

private val HighContrastColorScheme = darkColorScheme(
    primary = HighContrastCyan,
    onPrimary = Color.Black,
    primaryContainer = HighContrastBlack,
    onPrimaryContainer = HighContrastWhite,
    secondary = HighContrastYellow,
    onSecondary = Color.Black,
    background = HighContrastBlack,
    onBackground = HighContrastWhite,
    surface = HighContrastBlack,
    onSurface = HighContrastWhite,
    surfaceVariant = HighContrastCard,
    onSurfaceVariant = HighContrastTextSecondary,
    outline = HighContrastWhite
)

private val CleanLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = LightPrimary,
    secondary = LightAccent,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFEFF6FF),
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Composable
fun AriaVpnTheme(
    contrastMode: ContrastMode = ContrastMode.CYBER_DARK,
    content: @Composable () -> Unit
) {
    val (colorScheme, customTokens) = when (contrastMode) {
        ContrastMode.HIGH_CONTRAST -> {
            Pair(
                HighContrastColorScheme,
                ContrastCustomTokens(
                    cardBackground = HighContrastCard,
                    cardBorder = HighContrastCardBorder,
                    cardBorderWidth = 2.dp,
                    accentGlow = HighContrastCyan,
                    powerButtonBorder = HighContrastWhite,
                    isHighContrast = true,
                    speedGaugeColor = HighContrastCyan,
                    successColor = HighContrastGreen,
                    warningColor = HighContrastYellow
                )
            )
        }
        ContrastMode.CLEAN_LIGHT -> {
            Pair(
                CleanLightColorScheme,
                ContrastCustomTokens(
                    cardBackground = LightCard,
                    cardBorder = LightBorder,
                    cardBorderWidth = 1.dp,
                    accentGlow = LightAccent,
                    powerButtonBorder = LightPrimary,
                    isHighContrast = false,
                    speedGaugeColor = LightPrimary,
                    successColor = LightGreen,
                    warningColor = CyberAmber
                )
            )
        }
        ContrastMode.CYBER_DARK -> {
            Pair(
                CyberDarkColorScheme,
                ContrastCustomTokens(
                    cardBackground = CyberNavyCard,
                    cardBorder = Color(0xFF243050),
                    cardBorderWidth = 1.dp,
                    accentGlow = CyberCyan,
                    powerButtonBorder = CyberCyan,
                    isHighContrast = false,
                    speedGaugeColor = CyberCyan,
                    successColor = CyberGreen,
                    warningColor = CyberAmber
                )
            )
        }
    }

    CompositionLocalProvider(LocalContrastTokens provides customTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
