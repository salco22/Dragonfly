package dragonfly.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.salco.dragonfly.ui.theme.Pink40
import com.salco.dragonfly.ui.theme.Pink80
import com.salco.dragonfly.ui.theme.Purple40
import com.salco.dragonfly.ui.theme.Purple80
import com.salco.dragonfly.ui.theme.PurpleGrey40
import com.salco.dragonfly.ui.theme.PurpleGrey80
import com.salco.dragonfly.ui.theme.Typography

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3DBB59),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8F5BD),
    onPrimaryContainer = Color(0xFF00210B),

    secondary = Color(0xFF4E6356),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E8D7),
    onSecondaryContainer = Color(0xFF0C1F14),

    tertiary = Color(0xFF3B6470),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFE9F7),
    onTertiaryContainer = Color(0xFF001F27),

    background = Color(0xFFFCFDF7),
    onBackground = Color(0xFF1A1C18),

    surface = Color(0xFFFCFDF7),
    onSurface = Color(0xFF1A1C18),

    surfaceVariant = Color(0xFFDFE4D8),
    onSurfaceVariant = Color(0xFF43483E),

    outline = Color(0xFF73796E),
    inverseOnSurface = Color(0xFFF1F1EC),
    inverseSurface = Color(0xFF2F312D),
    inversePrimary = Color(0xFF8FDBA2),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

@Composable
fun DragonflyTheme(
    darkTheme: Boolean = false/*isSystemInDarkTheme()*/,
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
        content = content
    )
}