package edu.ap.takeexamapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ApLightColorScheme = lightColorScheme(
    primary = ApRed,
    onPrimary = Color.White,
    primaryContainer = ApRedContainer,
    onPrimaryContainer = ApOnRedContainer,
    secondary = ApDarkRed,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD5),
    onSecondaryContainer = Color(0xFF410001),
    tertiary = ApGrey,
    onTertiary = Color.White,
    background = ApBackground,
    onBackground = ApNearBlack,
    surface = ApSurface,
    onSurface = ApNearBlack,
    surfaceVariant = Color(0xFFF6EDEC),
    onSurfaceVariant = ApDarkGrey,
    outline = ApOutline,
    outlineVariant = Color(0xFFCFC3C2),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun TakeExamAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ApLightColorScheme,
        typography = Typography,
        content = content
    )
}
