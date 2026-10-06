package com.budila.app

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/** Палитра Material 3 для темы: из обоев (Material You) или построенная из цвета темы. */
fun colorSchemeFor(
    context: Context,
    theme: AppTheme,
    dark: Boolean,
    amoled: Boolean,
    settings: AppSettings = SettingsRepository.current,
): ColorScheme {
    if (theme == AppTheme.DYNAMIC && Build.VERSION.SDK_INT >= 31) {
        val base = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        return if (dark && amoled) {
            base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerLow = Color(0xFF0B0B0D),
                surfaceContainer = Color(0xFF111114),
            )
        } else {
            base
        }
    }
    return dynamicColorScheme(
        seedColor = if (theme == AppTheme.CUSTOM) settings.customSeed else theme.seed,
        isDark = dark,
        isAmoled = amoled,
        style = when (theme) {
            AppTheme.GRAPHITE -> PaletteStyle.Monochrome
            AppTheme.CUSTOM -> PaletteStyle.valueOf(settings.customStyle.name)
            else -> PaletteStyle.TonalSpot
        },
    )
}

/** Светлая или тёмная по выбору «Авто / Светлая / Тёмная». */
@Composable
private fun darkModeOn(settings: AppSettings): Boolean {
    val system = isSystemInDarkTheme()
    return when (settings.darkMode) {
        DarkMode.SYSTEM -> system
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
}

/** Тёмный ли экран сейчас; у тем «Инструмента» вроде «Бумаги» или «Полночи» яркость своя. */
@Composable
fun isAppInDarkTheme(): Boolean {
    val settings by SettingsRepository.settings.collectAsStateWithLifecycle()
    val dark = darkModeOn(settings)
    return if (settings.design == Design.INSTRUMENT) {
        settings.instrumentTheme.palette(dark, settings.amoled).dark
    } else {
        dark
    }
}

@Composable
fun BudilaTheme(content: @Composable () -> Unit) {
    val settings by SettingsRepository.settings.collectAsStateWithLifecycle()
    val dark = darkModeOn(settings)
    if (settings.design == Design.INSTRUMENT) {
        val palette = settings.instrumentTheme.palette(dark, settings.amoled)
        val colors = remember(palette) { palette.toColorScheme() }
        val boot = rememberBoot()
        CompositionLocalProvider(LocalInstrument provides palette, LocalBoot provides boot) {
            MaterialTheme(colorScheme = colors, typography = GolosTypography, content = content)
        }
        return
    }
    val context = LocalContext.current
    val colors = remember(settings, dark) {
        colorSchemeFor(context, settings.theme, dark, settings.amoled, settings)
    }
    MaterialTheme(colorScheme = colors, content = content)
}
