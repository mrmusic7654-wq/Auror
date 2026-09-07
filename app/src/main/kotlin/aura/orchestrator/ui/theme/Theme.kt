package aura.orchestrator.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

// ---- AURA palette: luxury cyber-dragon. ------------------------------------
val EmberViolet = Color(0xFF8B7CFF)
val DragonIndigo = Color(0xFF6C4DFF)
val FrostCyan = Color(0xFF00CFFF)
val CrystalLavender = Color(0xFFC9B8FF)
val DeepGraphite = Color(0xFF120C1E)
val InkBlack = Color(0xFF0B0714)
val GlassSurface = Color(0xFF1A1228)
val MistIndigo = Color(0xFF2A2140)
val LightBg = Color(0xFFF7F6FB)
val LightSurface = Color(0xFFFFFFFF)
val OnDark = Color(0xFFEDE9F8)
val MutedText = Color(0xFF9D93B8)

private val DarkColors = darkColorScheme(
    primary = EmberViolet,
    onPrimary = InkBlack,
    secondary = FrostCyan,
    onSecondary = InkBlack,
    tertiary = CrystalLavender,
    onTertiary = InkBlack,
    background = InkBlack,
    onBackground = OnDark,
    surface = GlassSurface,
    onSurface = OnDark,
    surfaceVariant = MistIndigo,
    onSurfaceVariant = MutedText,
    outline = Color(0xFF3A3154),
    error = Color(0xFFFF6B6B),
    onError = InkBlack
)

private val LightColors = lightColorScheme(
    primary = DragonIndigo,
    onPrimary = Color.White,
    secondary = Color(0xFF0087A8),
    onSecondary = Color.White,
    tertiary = Color(0xFF5B3FA8),
    onTertiary = Color.White,
    background = LightBg,
    onBackground = Color(0xFF201A2E),
    surface = LightSurface,
    onSurface = Color(0xFF201A2E),
    surfaceVariant = Color(0xFFECE9F6),
    onSurfaceVariant = Color(0xFF5A5470),
    outline = Color(0xFFD5D0E6),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun AuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = AuraTypography, content = content)
}
