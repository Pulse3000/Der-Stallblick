package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = BentoPrimary,
    secondary = BentoSecondary,
    tertiary = BentoCardBlueBg,
    background = Color(0xFF121314),
    surface = Color(0xFF1C1D1E),
    errorContainer = BentoAlertContainer,
    onErrorContainer = BentoAlertOnContainer
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BentoPrimary,
    secondary = BentoSecondary,
    tertiary = BentoCardBlueBg,
    background = BentoBackground,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = BentoActiveOnContainer,
    onBackground = BentoOnBackground,
    onSurface = BentoOnBackground,
    error = BentoAlertCritical,
    errorContainer = BentoAlertContainer,
    onErrorContainer = BentoAlertOnContainer,
    surfaceVariant = BentoCardLightBg,
    onSurfaceVariant = Color(0xFF44474E),
    outline = BentoCardGrayBorder,
    outlineVariant = BentoCardGrayBorder
  )

@Composable
fun MyApplicationTheme(
  themeType: String = "ORGANIC_GREEN",
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      themeType == "MIDNIGHT_DARK" -> {
        darkColorScheme(
          primary = Color(0xFF00E676),
          secondary = Color(0xFFFF5252),
          tertiary = Color(0xFF37474F),
          background = Color(0xFF101214),
          surface = Color(0xFF191C1E),
          onPrimary = Color.Black,
          onSecondary = Color.White,
          onTertiary = Color.White,
          onBackground = Color(0xFFE2E2E6),
          onSurface = Color(0xFFE2E2E6),
          error = Color(0xFFCF6679),
          errorContainer = Color(0xFFB00020),
          onErrorContainer = Color.White,
          surfaceVariant = Color(0xFF222629),
          onSurfaceVariant = Color(0xFFC4C7C5),
          outline = Color(0xFF43474E),
          outlineVariant = Color(0xFF43474E)
        )
      }

      themeType == "CLASSIC_BLUE" -> {
        if (darkTheme) {
          darkColorScheme(
            primary = BentoPrimary,
            secondary = BentoSecondary,
            tertiary = BentoCardBlueBg,
            background = Color(0xFF121314),
            surface = Color(0xFF1C1D1E),
            errorContainer = BentoAlertContainer,
            onErrorContainer = BentoAlertOnContainer
          )
        } else {
          lightColorScheme(
            primary = BentoPrimary,
            secondary = BentoSecondary,
            tertiary = BentoCardBlueBg,
            background = BentoBackground,
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = BentoActiveOnContainer,
            onBackground = BentoOnBackground,
            onSurface = BentoOnBackground,
            error = BentoAlertCritical,
            errorContainer = BentoAlertContainer,
            onErrorContainer = BentoAlertOnContainer,
            surfaceVariant = BentoCardLightBg,
            onSurfaceVariant = Color(0xFF44474E),
            outline = BentoCardGrayBorder,
            outlineVariant = BentoCardGrayBorder
          )
        }
      }

      else -> { // Default is ORGANIC_GREEN
        if (darkTheme) {
          darkColorScheme(
            primary = Color(0xFF81C784),
            secondary = Color(0xFFAED581),
            tertiary = Color(0xFF2E7D32),
            background = Color(0xFF121412),
            surface = Color(0xFF1B1D1B),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onTertiary = Color.White,
            onBackground = Color(0xFFE1E3E1),
            onSurface = Color(0xFFE1E3E1),
            error = Color(0xFFF2B8B5),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC),
            surfaceVariant = Color(0xFF2E322E),
            onSurfaceVariant = Color(0xFFC2C9C2),
            outline = Color(0xFF8C938C),
            outlineVariant = Color(0xFF424942)
          )
        } else {
          lightColorScheme(
            primary = Color(0xFF2E7D32),
            secondary = Color(0xFF558B2F),
            tertiary = Color(0xFFC8E6C9),
            background = Color(0xFFF4F7F4),
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color(0xFF1B2E1D),
            onBackground = Color(0xFF1B2E1D),
            onSurface = Color(0xFF1B2E1D),
            error = Color(0xFFB3261E),
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B),
            surfaceVariant = Color(0xFFE8F5E9),
            onSurfaceVariant = Color(0xFF3F493F),
            outline = Color(0xFF727972),
            outlineVariant = Color(0xFFC2C9C2)
          )
        }
      }
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
