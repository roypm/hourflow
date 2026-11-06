package com.horastrabajo.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

enum class TemaPreferido {
    SISTEMA,
    CLARO,
    OSCURO,
}

/**
 * Idioma elegido por el usuario. [SISTEMA] deja que Android resuelva según el idioma
 * del dispositivo; si ese idioma no es ninguno de los soportados, los recursos caen
 * automáticamente en `values/` (español), que es la carpeta por defecto.
 */
enum class IdiomaPreferido(val etiquetaBcp47: String?) {
    SISTEMA(null),
    ESPANOL("es"),
    INGLES("en"),
    CATALAN("ca"),
}

/** Formato que generan los botones Compartir y Descargar del resumen. */
enum class FormatoResumen(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    JPEG("jpg", "image/jpeg"),
}

private val Context.ajustesDataStore by preferencesDataStore(name = "ajustes")
private val CLAVE_TEMA = stringPreferencesKey("tema_preferido")
private val CLAVE_FORMATO_RESUMEN = stringPreferencesKey("formato_resumen_preferido")

class ThemePreferenceRepository(private val context: Context) {

    val temaPreferido = context.ajustesDataStore.data.map { prefs ->
        prefs[CLAVE_TEMA]?.let { runCatching { TemaPreferido.valueOf(it) }.getOrNull() } ?: TemaPreferido.SISTEMA
    }

    suspend fun guardarTema(tema: TemaPreferido) {
        context.ajustesDataStore.edit { prefs -> prefs[CLAVE_TEMA] = tema.name }
    }
}

class FormatoResumenPreferenceRepository(private val context: Context) {

    val formatoResumen = context.ajustesDataStore.data.map { prefs ->
        prefs[CLAVE_FORMATO_RESUMEN]?.let { runCatching { FormatoResumen.valueOf(it) }.getOrNull() }
            ?: FormatoResumen.PDF
    }

    suspend fun guardarFormatoResumen(formato: FormatoResumen) {
        context.ajustesDataStore.edit { prefs -> prefs[CLAVE_FORMATO_RESUMEN] = formato.name }
    }
}
