package com.chasinglemons.empeg.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmpegBlue,
    onPrimary = OnEmpegBlue,
    primaryContainer = EmpegBlueContainer,
    onPrimaryContainer = OnEmpegBlueContainer,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = EmpegBlue,
    onPrimary = OnEmpegBlue,
    primaryContainer = EmpegBlueContainer,
    onPrimaryContainer = OnEmpegBlueContainer,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun EmpegRemoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Dynamic color may supply the surfaces, but the accent is always the app's
    // own blue: a wallpaper-derived primary (#3879FF on the test device) made
    // playlist headings, the selected tab and buttons look like another app.
    val colorScheme = baseScheme.copy(
        primary = EmpegBlue,
        onPrimary = OnEmpegBlue,
        primaryContainer = EmpegBlueContainer,
        onPrimaryContainer = OnEmpegBlueContainer
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}