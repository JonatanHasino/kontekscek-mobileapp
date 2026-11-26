package com.aldointelligence.kontekscek.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import com.aldointelligence.kontekscek.data.TextSize
import com.aldointelligence.kontekscek.data.ThemeMode

val LocalKonteksColors = staticCompositionLocalOf { LightKonteksColors }

private val LightScheme =
    lightColorScheme(
        primary = LightKonteksColors.primary,
        onPrimary = LightKonteksColors.onPrimary,
        primaryContainer = LightKonteksColors.primarySoft,
        onPrimaryContainer = LightKonteksColors.primaryPressed,
        secondary = LightKonteksColors.accent,
        onSecondary = Color.White,
        secondaryContainer = LightKonteksColors.accentSoft,
        onSecondaryContainer = LightKonteksColors.accent,
        tertiary = LightKonteksColors.success,
        background = LightKonteksColors.canvas,
        onBackground = LightKonteksColors.ink,
        surface = LightKonteksColors.surface,
        onSurface = LightKonteksColors.ink,
        surfaceVariant = LightKonteksColors.surfaceMuted,
        onSurfaceVariant = LightKonteksColors.inkSecondary,
        outline = LightKonteksColors.lineStrong,
        outlineVariant = LightKonteksColors.line,
        error = LightKonteksColors.danger,
        onError = Color.White,
    )

private val DarkScheme =
    darkColorScheme(
        primary = DarkKonteksColors.primary,
        onPrimary = DarkKonteksColors.onPrimary,
        primaryContainer = DarkKonteksColors.primarySoft,
        onPrimaryContainer = DarkKonteksColors.primary,
        secondary = DarkKonteksColors.accent,
        onSecondary = DarkKonteksColors.onInverse,
        secondaryContainer = DarkKonteksColors.accentSoft,
        onSecondaryContainer = DarkKonteksColors.accent,
        tertiary = DarkKonteksColors.success,
        background = DarkKonteksColors.canvas,
        onBackground = DarkKonteksColors.ink,
        surface = DarkKonteksColors.surface,
        onSurface = DarkKonteksColors.ink,
        surfaceVariant = DarkKonteksColors.surfaceMuted,
        onSurfaceVariant = DarkKonteksColors.inkSecondary,
        outline = DarkKonteksColors.lineStrong,
        outlineVariant = DarkKonteksColors.line,
        error = DarkKonteksColors.danger,
        onError = DarkKonteksColors.onInverse,
    )

@Composable
fun KonteksCekTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    textSize: TextSize = TextSize.NORMAL,
    content: @Composable () -> Unit,
) {
    val dark =
        when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    val colors = if (dark) DarkKonteksColors else LightKonteksColors
    val density = LocalDensity.current
    val scale = if (textSize == TextSize.LARGE) 1.12f else 1f
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(
        LocalKonteksColors provides colors,
        LocalDensity provides Density(density.density, density.fontScale * scale),
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = KonteksTypography,
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
