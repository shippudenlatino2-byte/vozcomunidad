package com.vozcomunidad.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Esquema = lightColorScheme(
    primary = Vz.Primary,
    onPrimary = Vz.OnPrimary,
    primaryContainer = Vz.PrimaryContainer,
    onPrimaryContainer = Vz.OnPrimaryContainer,
    secondary = Vz.Secondary,
    onSecondary = Vz.OnSecondary,
    secondaryContainer = Vz.SecondaryContainer,
    onSecondaryContainer = Vz.OnSecondaryContainer,
    tertiary = Vz.Tertiary,
    onTertiary = Vz.OnTertiary,
    tertiaryContainer = Vz.TertiaryContainer,
    onTertiaryContainer = Vz.OnTertiaryContainer,
    error = Vz.Error,
    errorContainer = Vz.ErrorContainer,
    onErrorContainer = Vz.OnErrorContainer,
    background = Vz.Surface,
    onBackground = Vz.OnSurface,
    surface = Vz.Surface,
    onSurface = Vz.OnSurface,
    onSurfaceVariant = Vz.OnSurfaceVariant,
    outline = Vz.Outline,
    outlineVariant = Vz.OutlineVariant,
    inverseSurface = Vz.InverseSurface,
    inverseOnSurface = Vz.InverseOnSurface,
    surfaceTint = Color.Transparent,
)

@Composable
fun VozTema(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Esquema,
        content = content,
    )
}
