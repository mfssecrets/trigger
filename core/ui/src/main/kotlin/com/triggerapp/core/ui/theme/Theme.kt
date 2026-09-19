package com.triggerapp.core.ui.theme

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

/**
 * Fixed Material 3 dark color scheme using Trigger brand purples, greens, and charcoal surfaces.
 * @author udit
 */
private val TriggerBrandDarkScheme = darkColorScheme(
    primary = TriggerAccent,
    onPrimary = Color.Black,
    primaryContainer = TriggerSurfaceMuted,
    onPrimaryContainer = Color.White,
    secondary = TriggerAccentSecondary,
    onSecondary = Color.Black,
    tertiary = TriggerAccent,
    background = TriggerScreenBackground,
    onBackground = Color.White,
    surface = TriggerScreenBackground,
    onSurface = Color.White,
    surfaceVariant = TriggerSurfaceMuted,
    onSurfaceVariant = Color(0xFFAFACAC),
    outline = Color(0xFF727180),
    error = Color(0xFFFFB4AB),
    onError = Color.Black,
)

/**
 * Root Compose theme for Trigger: selects a Material 3 color scheme then wraps [MaterialTheme].
 *
 * @param darkTheme Whether to prefer dark palettes when not using fixed brand dark.
 * @param dynamicColor Use Material You dynamic colors on supported API levels.
 * @param useBrandDarkColors Forces the custom dark brand scheme when [darkTheme] is true.
 * @param content Root composable subtree receiving typography and colors.
 * @author udit
 */
@Composable
fun TriggerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    useBrandDarkColors: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        useBrandDarkColors && darkTheme -> TriggerBrandDarkScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = TriggerPurple,
            secondary = TriggerPurpleDark,
            tertiary = TriggerPurple,
        )
        else -> lightColorScheme(
            primary = TriggerPurple,
            secondary = TriggerPurpleDark,
            tertiary = TriggerPurple,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
