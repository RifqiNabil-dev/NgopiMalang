package com.ngopimalang.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkCoffeePrimary,
    onPrimary = DarkCoffeeOnPrimary,
    primaryContainer = DarkCoffeePrimaryContainer,
    onPrimaryContainer = DarkCoffeeOnPrimaryContainer,
    secondary = DarkCoffeeSecondary,
    onSecondary = DarkCoffeeOnSecondary,
    secondaryContainer = DarkCoffeeSecondaryContainer,
    onSecondaryContainer = DarkCoffeeOnSecondaryContainer,
    tertiary = DarkCoffeeTertiary,
    onTertiary = DarkCoffeeOnTertiary,
    tertiaryContainer = DarkCoffeeTertiaryContainer,
    onTertiaryContainer = DarkCoffeeOnTertiaryContainer,
    background = DarkCoffeeBackground,
    onBackground = DarkCoffeeOnBackground,
    surface = DarkCoffeeSurface,
    onSurface = DarkCoffeeOnSurface,
    surfaceVariant = DarkCoffeeSurfaceVariant,
    onSurfaceVariant = DarkCoffeeOnSurfaceVariant,
    surfaceContainerLow = DarkCoffeeSurfaceContainerLow,
    outline = DarkCoffeeOutline,
    outlineVariant = DarkCoffeeOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = CoffeePrimary,
    onPrimary = CoffeeOnPrimary,
    primaryContainer = CoffeePrimaryContainer,
    onPrimaryContainer = CoffeeOnPrimaryContainer,
    secondary = CoffeeSecondary,
    onSecondary = CoffeeOnSecondary,
    secondaryContainer = CoffeeSecondaryContainer,
    onSecondaryContainer = CoffeeOnSecondaryContainer,
    tertiary = CoffeeTertiary,
    onTertiary = CoffeeOnTertiary,
    tertiaryContainer = CoffeeTertiaryContainer,
    onTertiaryContainer = CoffeeOnTertiaryContainer,
    background = CoffeeBackground,
    onBackground = CoffeeOnBackground,
    surface = CoffeeSurface,
    onSurface = CoffeeOnSurface,
    surfaceVariant = CoffeeSurfaceVariant,
    onSurfaceVariant = CoffeeOnSurfaceVariant,
    surfaceContainerLow = CoffeeSurfaceContainerLow,
    outline = CoffeeOutline,
    outlineVariant = CoffeeOutlineVariant
)

/**
 * Tema utama aplikasi NgopiMalang.
 *
 * Mengusung tema hangat kopi & coklat (Co/Choc Style) yang konsisten di seluruh layar.
 * [dynamicColor] disetel false agar tema warna kopi selalu aktif dan tidak ditimpa
 * wallpaper sistem pada Android 12+.
 */
@Composable
fun NgopiMalangTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
