package com.horastrabajo.app.ui.ajustes

import android.util.Log
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horastrabajo.app.R
import com.horastrabajo.app.data.export.JsonBackupManager
import com.horastrabajo.app.data.preferences.IdiomaPreferido
import com.horastrabajo.app.data.preferences.TemaPreferido
import com.horastrabajo.app.data.preferences.ThemePreferenceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AjustesViewModel(
    private val themePreferenceRepository: ThemePreferenceRepository,
    private val jsonBackupManager: JsonBackupManager,
) : ViewModel() {

    val temaPreferido: StateFlow<TemaPreferido> = themePreferenceRepository.temaPreferido
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TemaPreferido.SISTEMA)

    fun obtenerIdiomaActual(): IdiomaPreferido = idiomaDesdeAppCompat()

    private val _mensaje = MutableStateFlow<Int?>(null)

    /**
     * Identificador del texto (éxito o error) de la última operación de backup. Se expone
     * como recurso y no como cadena ya formada para que la UI lo resuelva en el idioma
     * activo; se consume con [limpiarMensaje].
     */
    @get:StringRes
    val mensaje: StateFlow<Int?> = _mensaje.asStateFlow()

    fun limpiarMensaje() {
        _mensaje.value = null
    }

    fun cambiarTema(tema: TemaPreferido) {
        viewModelScope.launch { themePreferenceRepository.guardarTema(tema) }
    }

    fun cambiarIdioma(idioma: IdiomaPreferido) {
        val localeList = if (idioma == IdiomaPreferido.SISTEMA) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(idioma.etiquetaBcp47!!)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /** Backup completo (todos los trabajos) en JSON. [onListo] puede hacer I/O propia (ej. escribir a disco). */
    fun exportarBackup(onListo: suspend (String) -> Unit) {
        viewModelScope.launch {
            try {
                onListo(withContext(Dispatchers.IO) { jsonBackupManager.exportarTodo() })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al exportar el backup", e)
                _mensaje.value = R.string.ajustes_error_exportar
            }
        }
    }

    fun importarBackup(contenidoJson: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { jsonBackupManager.importarTodo(contenidoJson) }
                _mensaje.value = R.string.ajustes_backup_importado
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // JSON malformado, esquema incompatible o fechas/horas no ISO-8601: nunca debe tirar la app.
                Log.e(TAG, "Error al importar backup: archivo inválido o incompatible", e)
                _mensaje.value = R.string.ajustes_error_importar
            }
        }
    }

    /** Reportado por la UI cuando falla la lectura del archivo elegido, antes incluso de llegar al parseo. */
    fun notificarErrorDeLectura(error: Throwable) {
        Log.e(TAG, "Error al leer el archivo de backup elegido", error)
        _mensaje.value = R.string.ajustes_error_leer
    }

    private companion object {
        const val TAG = "AjustesViewModel"

        /** Convierte [AppCompatDelegate.getApplicationLocales] a nuestro enum. */
        fun idiomaDesdeAppCompat(): IdiomaPreferido {
            val locales = AppCompatDelegate.getApplicationLocales()
            val etiqueta = locales[0]?.toLanguageTag()
            return IdiomaPreferido.entries.firstOrNull { it.etiquetaBcp47 == etiqueta } ?: IdiomaPreferido.SISTEMA
        }
    }
}
