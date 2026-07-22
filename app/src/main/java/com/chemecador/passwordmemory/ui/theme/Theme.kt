package com.chemecador.passwordmemory.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Indigo40,
    onPrimary = Neutral100,
    primaryContainer = Indigo90,
    onPrimaryContainer = Indigo10,
    inversePrimary = Indigo80,
    secondary = Cyan40,
    onSecondary = Neutral100,
    secondaryContainer = Cyan90,
    onSecondaryContainer = Cyan10,
    tertiary = Rose40,
    onTertiary = Neutral100,
    tertiaryContainer = Rose90,
    onTertiaryContainer = Rose10,
    error = Error40,
    onError = Neutral100,
    errorContainer = Error90,
    onErrorContainer = Error10,
    background = Neutral99,
    onBackground = Neutral12,
    surface = Neutral99,
    onSurface = Neutral12,
    surfaceVariant = Neutral90,
    onSurfaceVariant = Neutral30,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = Neutral98,
    surfaceContainer = Neutral96,
    surfaceContainerHigh = Neutral94,
    surfaceContainerHighest = Neutral92,
    inverseSurface = NeutralInverse20,
    inverseOnSurface = Neutral98,
    outline = Neutral60,
    outlineVariant = Neutral80,
    scrim = Neutral0
)

private val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    onPrimary = Indigo20,
    primaryContainer = Indigo30,
    onPrimaryContainer = Indigo90,
    inversePrimary = Indigo40,
    secondary = Cyan80,
    onSecondary = Cyan20,
    secondaryContainer = Cyan30,
    onSecondaryContainer = Cyan90,
    tertiary = Rose80,
    onTertiary = Rose20,
    tertiaryContainer = Rose30,
    onTertiaryContainer = Rose90,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    background = Neutral10,
    onBackground = NeutralInverse90,
    surface = Neutral10,
    onSurface = NeutralInverse90,
    surfaceVariant = Neutral30,
    onSurfaceVariant = Neutral80,
    surfaceContainerLowest = Neutral6,
    surfaceContainerLow = Neutral12,
    surfaceContainer = Neutral17,
    surfaceContainerHigh = Neutral22,
    surfaceContainerHighest = Neutral24,
    inverseSurface = NeutralInverse90,
    inverseOnSurface = NeutralInverse20,
    outline = Neutral70,
    outlineVariant = Neutral30,
    scrim = Neutral0
)

@Composable
fun PasswordMemoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default: the brand palette is part of how the app identifies itself, and the launcher
    // icon, the lock screen and the in-app accents are meant to match.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
