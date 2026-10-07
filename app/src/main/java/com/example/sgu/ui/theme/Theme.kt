package com.example.sgu.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PoliceBlue,
    secondary = PoliceRed,
    tertiary = PoliceAmber,
    background = Carbon950,
    surface = Carbon900,
    surfaceVariant = Carbon850,
    onPrimary = Carbon950,
    onSecondary = TextPrimary,
    onTertiary = Carbon950,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = CarbonBorder
)

@Composable
fun SguTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
