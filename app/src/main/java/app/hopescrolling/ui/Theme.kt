package app.hopescrolling.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

val Sunrise = Color(0xFFFFB74D)
val Danger = Color(0xFFE5484D)
val HopeGreen = Color(0xFF1DB86A)
val DeepGreen = Color(0xFF0F5C3A)

private val LightFallback = lightColorScheme(
    primary = Color(0xFF006D3F),
    primaryContainer = Color(0xFF95F7B8),
    secondary = Color(0xFF4F6354),
    tertiary = Color(0xFF8A5100),
)
private val DarkFallback = darkColorScheme(
    primary = Color(0xFF79DA9E),
    primaryContainer = Color(0xFF00522E),
    secondary = Color(0xFFB6CCB9),
    tertiary = Color(0xFFFFB86E),
)

private val HopeTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Couleurs Material You (celles de ton fond d'écran) sur Android 12+. */
@Composable
fun HopeTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> DarkFallback
        else -> LightFallback
    }
    MaterialTheme(colorScheme = scheme, typography = HopeTypography, content = content)
}

/**
 * Les réglages vivent dans SharedPreferences : on incrémente ce compteur après
 * chaque changement (ou au retour sur l'écran) pour que l'UI se redessine.
 */
object UiState {
    var version by mutableIntStateOf(0)
    fun bump() { version++ }
}
