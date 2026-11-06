package no.muscly.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Lime = Color(0xFFB4F43C)
val Sky = Color(0xFF38BDF8)
val Orange = Color(0xFFFB923C)
val Pink = Color(0xFFF472B6)
val Violet = Color(0xFFA78BFA)

private val DarkColors = darkColorScheme(
    primary = Lime, onPrimary = Color(0xFF0B0F14), primaryContainer = Color(0xFF2B3A12), onPrimaryContainer = Color(0xFFDDF9A8),
    secondary = Sky, onSecondary = Color(0xFF04131C), secondaryContainer = Color(0xFF0E2E40), onSecondaryContainer = Color(0xFFBDE8FB),
    tertiary = Orange, onTertiary = Color(0xFF1E0E02), tertiaryContainer = Color(0xFF3D2410), onTertiaryContainer = Color(0xFFFFD9BC),
    background = Color(0xFF0B0F14), onBackground = Color(0xFFE8EEF5),
    surface = Color(0xFF0B0F14), onSurface = Color(0xFFE8EEF5),
    surfaceVariant = Color(0xFF1A2430), onSurfaceVariant = Color(0xFF93A3B5),
    surfaceContainerLowest = Color(0xFF080B0F), surfaceContainerLow = Color(0xFF10161E), surfaceContainer = Color(0xFF131B24),
    surfaceContainerHigh = Color(0xFF18222D), surfaceContainerHighest = Color(0xFF1E2A37),
    outline = Color(0xFF34424F), outlineVariant = Color(0xFF243039),
    error = Color(0xFFF87171), onError = Color(0xFF2B0606),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F6B0A), onPrimary = Color.White, primaryContainer = Color(0xFFD7F5A3), onPrimaryContainer = Color(0xFF15240A),
    secondary = Color(0xFF0369A1), onSecondary = Color.White, secondaryContainer = Color(0xFFD4EEFB), onSecondaryContainer = Color(0xFF062435),
    tertiary = Color(0xFFC2410C), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFE2CC), onTertiaryContainer = Color(0xFF3A1404),
    background = Color(0xFFF4F6F9), onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF4F6F9), onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE6EBF1), onSurfaceVariant = Color(0xFF4B5A6B),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFAFBFC), surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0F3F7), surfaceContainerHighest = Color(0xFFE8EDF2),
    outline = Color(0xFFB7C2CE), outlineVariant = Color(0xFFDCE2E9),
    error = Color(0xFFDC2626),
)

private val base = Typography()
val MusclyTypography = Typography(
    displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

val NumberStyle = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Black, fontSize = 44.sp, letterSpacing = (-1).sp)

val MusclyShapes = Shapes(
    small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp),
)

val LocalIsDark = staticCompositionLocalOf { true }

@Composable
fun MusclyTheme(theme: String = "dark", content: @Composable () -> Unit) {
    val dark = when (theme) { "light" -> false; "system" -> isSystemInDarkTheme(); else -> true }
    androidx.compose.runtime.CompositionLocalProvider(LocalIsDark provides dark) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = MusclyTypography, shapes = MusclyShapes, content = content)
    }
}
