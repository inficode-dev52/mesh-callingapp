package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
      primary = PrimaryAccent,
      secondary = TextSecondary,
      tertiary = PrimaryAccent,
      background = BackgroundDark,
      surface = SurfaceDark,
      onPrimary = BackgroundDark,
      onSecondary = TextPrimary,
      onTertiary = BackgroundDark,
      onBackground = TextPrimary,
      onSurface = TextPrimary,
      surfaceVariant = SurfaceDark,
      onSurfaceVariant = TextSecondary,
      error = ErrorRed,
      onError = TextPrimary,
      primaryContainer = SurfaceDark,
      onPrimaryContainer = PrimaryAccent
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Green40,
    secondary = GreenGrey40,
    tertiary = DarkGreen40,

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
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme as requested
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false, // Disable dynamic to ensure green theme
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
