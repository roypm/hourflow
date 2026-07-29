package com.horastrabajo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Verde40,
    primaryContainer = PrimarioContenedorClaro,
    onPrimaryContainer = OnPrimarioContenedorClaro,
    secondary = VerdeGris40,
    secondaryContainer = SecundarioContenedorClaro,
    onSecondaryContainer = OnSecundarioContenedorClaro,
    background = FondoClaro,
    surface = SuperficieClara,
    surfaceVariant = SuperficieVarianteClara,
)

private val DarkColors = darkColorScheme(
    primary = Verde80,
    primaryContainer = PrimarioContenedorOscuro,
    onPrimaryContainer = OnPrimarioContenedorOscuro,
    secondary = VerdeGris80,
    secondaryContainer = SecundarioContenedorOscuro,
    onSecondaryContainer = OnSecundarioContenedorOscuro,
    background = FondoOscuro,
    surface = SuperficieOscura,
    surfaceVariant = SuperficieVarianteOscura,
)

@Composable
fun HorasTrabajoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
