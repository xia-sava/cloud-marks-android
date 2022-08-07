package to.sava.cloudmarksandroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = XiaGreen,
    onPrimary = Color.Black,
    surface = XiaGreen,
    onSurface = Color.Black,
)

private val LightColorPalette = lightColors(
    primary = XiaGreen,
    onPrimary = Color.Black,

)

@Composable
fun CloudMarksAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (darkTheme) DarkColorPalette else LightColorPalette,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}