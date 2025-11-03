package com.beast.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun BeastAppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BeastTypography,
        shapes = BeastShapes,
        content = content
    )
}

private val LightColors = lightColorScheme(
    primary = BeastPrimaryLight,
    onPrimary = BeastOnPrimaryLight,
    primaryContainer = BeastPrimaryContainerLight,
    onPrimaryContainer = BeastOnPrimaryContainerLight,
    secondary = BeastSecondaryLight,
    onSecondary = BeastOnSecondaryLight,
    secondaryContainer = BeastSecondaryContainerLight,
    onSecondaryContainer = BeastOnSecondaryContainerLight,
    tertiary = BeastTertiaryLight,
    onTertiary = BeastOnTertiaryLight,
    tertiaryContainer = BeastTertiaryContainerLight,
    onTertiaryContainer = BeastOnTertiaryContainerLight,
    error = BeastErrorLight,
    onError = BeastOnErrorLight,
    errorContainer = BeastErrorContainerLight,
    onErrorContainer = BeastOnErrorContainerLight,
    background = BeastBackgroundLight,
    onBackground = BeastOnBackgroundLight,
    surface = BeastSurfaceLight,
    onSurface = BeastOnSurfaceLight,
    surfaceVariant = BeastSurfaceVariantLight,
    onSurfaceVariant = BeastOnSurfaceVariantLight,
    outline = BeastOutlineLight,
    inverseSurface = BeastInverseSurfaceLight,
    inverseOnSurface = BeastInverseOnSurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = BeastPrimaryDark,
    onPrimary = BeastOnPrimaryDark,
    primaryContainer = BeastPrimaryContainerDark,
    onPrimaryContainer = BeastOnPrimaryContainerDark,
    secondary = BeastSecondaryDark,
    onSecondary = BeastOnSecondaryDark,
    secondaryContainer = BeastSecondaryContainerDark,
    onSecondaryContainer = BeastOnSecondaryContainerDark,
    tertiary = BeastTertiaryDark,
    onTertiary = BeastOnTertiaryDark,
    tertiaryContainer = BeastTertiaryContainerDark,
    onTertiaryContainer = BeastOnTertiaryContainerDark,
    error = BeastErrorDark,
    onError = BeastOnErrorDark,
    errorContainer = BeastErrorContainerDark,
    onErrorContainer = BeastOnErrorContainerDark,
    background = BeastBackgroundDark,
    onBackground = BeastOnBackgroundDark,
    surface = BeastSurfaceDark,
    onSurface = BeastOnSurfaceDark,
    surfaceVariant = BeastSurfaceVariantDark,
    onSurfaceVariant = BeastOnSurfaceVariantDark,
    outline = BeastOutlineDark,
    inverseSurface = BeastInverseSurfaceDark,
    inverseOnSurface = BeastInverseOnSurfaceDark
)

