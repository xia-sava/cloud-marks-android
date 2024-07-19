package to.sava.cloudmarksandroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val XiaGreen = Color(0xFF80FFC0)
val XiaDarkGreen = Color(0xFF408060)
val XiaDeepGreen = Color(0xFF204030)

private val DarkColorPalette = darkColors(
    primary = XiaGreen,
    onPrimary = Color.White,
    surface = XiaDeepGreen,
    onSurface = Color.White,
    secondary = XiaDeepGreen,
    onSecondary = Color.White,
)

private val LightColorPalette = lightColors(
    primary = XiaGreen,
    onPrimary = Color.Black,
    secondary = XiaDarkGreen,
    onSecondary = Color.White,
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