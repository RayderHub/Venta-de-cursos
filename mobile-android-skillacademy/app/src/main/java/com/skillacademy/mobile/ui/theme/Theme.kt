package com.skillacademy.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = SkillNavy,
    onPrimary = SkillSurface,
    primaryContainer = SkillNavyDark,
    onPrimaryContainer = SkillSurface,
    secondary = SkillEmerald,
    onSecondary = SkillSurface,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = SkillNavy,
    tertiary = SkillCyan,
    background = SkillBackground,
    onBackground = SkillTextPrimary,
    surface = SkillSurface,
    onSurface = SkillTextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = SkillTextSecondary,
    outline = SkillBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = SkillCyanLight,
    onPrimary = SkillNavyDark,
    primaryContainer = SkillNavy,
    onPrimaryContainer = SkillSurface,
    secondary = SkillEmeraldLight,
    onSecondary = SkillNavyDark,
    background = Color(0xFF0B1320),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF131F33),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155)
)

@Composable
fun SkillAcademyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SkillNavy.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
