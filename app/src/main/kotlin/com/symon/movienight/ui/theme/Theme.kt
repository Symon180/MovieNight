package com.symon.movienight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette colori "Cinema, IMDb Gold & MyAnimeList Blue"
val ImdbGold = Color(0xFFF5C518)
val ImdbGoldDark = Color(0xFFD4A500)
val MalBlue = Color(0xFF2E51A2)
val MalBlueLight = Color(0xFF4A73D1)
val CinemaBackground = Color(0xFF0D0F14)
val CinemaSurface = Color(0xFF181B24)
val CinemaCardSurface = Color(0xFF212634)
val LikeGreen = Color(0xFF2ECC71)
val SkipRed = Color(0xFFE74C3C)
val TextPrimary = Color(0xFFF8F9FA)
val TextSecondary = Color(0xFFB0B8C8)

private val CinemaColorScheme = darkColorScheme(
  primary = ImdbGold,
  onPrimary = Color(0xFF121212),
  primaryContainer = Color(0xFF332900),
  onPrimaryContainer = ImdbGold,
  secondary = LikeGreen,
  onSecondary = Color(0xFF0B2B17),
  tertiary = SkipRed,
  background = CinemaBackground,
  onBackground = TextPrimary,
  surface = CinemaSurface,
  onSurface = TextPrimary,
  surfaceVariant = CinemaCardSurface,
  onSurfaceVariant = TextSecondary
)

@Composable
fun MovieNightTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = CinemaColorScheme,
    typography = Typography,
    content = content
  )
}
