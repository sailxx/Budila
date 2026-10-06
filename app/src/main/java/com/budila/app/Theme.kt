package com.budila.app

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F5B92),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF071452),
    secondary = Color(0xFF5A5D72),
    secondaryContainer = Color(0xFFDFE1F9),
    onSecondaryContainer = Color(0xFF171B2C),
    tertiary = Color(0xFF75546F),
    tertiaryContainer = Color(0xFFFFD7F5),
    onTertiaryContainer = Color(0xFF2C122A),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB8C4FF),
    onPrimary = Color(0xFF202C61),
    primaryContainer = Color(0xFF374379),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFC3C5DD),
    secondaryContainer = Color(0xFF424659),
    onSecondaryContainer = Color(0xFFDFE1F9),
    tertiary = Color(0xFFE4BAD9),
    tertiaryContainer = Color(0xFF5C3D57),
    onTertiaryContainer = Color(0xFFFFD7F5),
    background = Color(0xFF121318),
    surface = Color(0xFF121318),
)

/** Material 3 тема: на Android 12+ берёт цвета из обоев (Material You). */
@Composable
fun BudilaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
