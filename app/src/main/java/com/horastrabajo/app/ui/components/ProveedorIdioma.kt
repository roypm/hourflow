package com.horastrabajo.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Idioma efectivo para formatear nombres de meses y días. */
@Composable
fun localeActual(): Locale {
    val contexto = LocalContext.current
    return contexto.resources.configuration.locales[0] ?: Locale.getDefault()
}
