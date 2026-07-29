package com.horastrabajo.app.ui.ajustes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horastrabajo.app.R
import com.horastrabajo.app.data.export.compartirArchivo
import com.horastrabajo.app.data.export.directorioExports
import com.horastrabajo.app.data.preferences.IdiomaPreferido
import com.horastrabajo.app.data.preferences.TemaPreferido
import com.horastrabajo.app.ui.AppViewModelProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    onVolver: () -> Unit,
    viewModel: AjustesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val temaPreferido by viewModel.temaPreferido.collectAsState()
    val idiomaPreferido = viewModel.obtenerIdiomaActual()
    val mensaje by viewModel.mensaje.collectAsState()
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val textoMensaje = mensaje?.let { stringResource(it) }
    LaunchedEffect(textoMensaje) {
        textoMensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    var mostrarConfirmacionImportar by remember { mutableStateOf(false) }

    val selectorImportacion = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val contenido = withContext(Dispatchers.IO) {
                    contexto.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                if (contenido != null) {
                    viewModel.importarBackup(contenido)
                } else {
                    viewModel.notificarErrorDeLectura(IllegalStateException("No se pudo abrir el archivo seleccionado"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                viewModel.notificarErrorDeLectura(e)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ajustes_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accion_volver),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SeccionAjustes(titulo = stringResource(R.string.ajustes_tema)) {
                TemaPreferido.entries.forEach { opcion ->
                    FilaOpcion(
                        etiqueta = stringResource(etiquetaTema(opcion)),
                        seleccionada = temaPreferido == opcion,
                        onSeleccionar = { viewModel.cambiarTema(opcion) },
                    )
                }
            }

            SeccionAjustes(titulo = stringResource(R.string.ajustes_idioma)) {
                IdiomaPreferido.entries.forEach { opcion ->
                    FilaOpcion(
                        etiqueta = stringResource(etiquetaIdioma(opcion)),
                        seleccionada = idiomaPreferido == opcion,
                        onSeleccionar = { viewModel.cambiarIdioma(opcion) },
                    )
                }
            }

            SeccionAjustes(titulo = stringResource(R.string.ajustes_backup)) {
                Text(
                    stringResource(R.string.ajustes_backup_descripcion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                TextButton(
                    onClick = {
                        viewModel.exportarBackup { json ->
                            val marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                            val archivo = File(directorioExports(contexto), "backup_$marca.json")
                            withContext(Dispatchers.IO) { archivo.writeText(json) }
                            compartirArchivo(contexto, archivo, "application/json")
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text(stringResource(R.string.ajustes_exportar_backup)) }
                TextButton(
                    onClick = { mostrarConfirmacionImportar = true },
                    modifier = Modifier.padding(start = 8.dp),
                ) { Text(stringResource(R.string.ajustes_importar_backup)) }
            }
        }
    }

    if (mostrarConfirmacionImportar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionImportar = false },
            title = { Text(stringResource(R.string.ajustes_importar_titulo)) },
            text = { Text(stringResource(R.string.ajustes_importar_mensaje)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacionImportar = false
                    selectorImportacion.launch(arrayOf("application/json"))
                }) { Text(stringResource(R.string.accion_continuar)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacionImportar = false }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
        )
    }
}

/** Bloque de ajustes en Card, para que las tres secciones compartan el mismo aspecto. */
@Composable
private fun SeccionAjustes(titulo: String, contenido: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            contenido()
        }
    }
}

/** Fila seleccionable completa (no solo el radio), para respetar el área táctil mínima. */
@Composable
private fun FilaOpcion(etiqueta: String, seleccionada: Boolean, onSeleccionar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionada, onClick = onSeleccionar)
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        RadioButton(selected = seleccionada, onClick = null)
        Text(text = etiqueta, modifier = Modifier.padding(start = 8.dp))
    }
}

private fun etiquetaTema(tema: TemaPreferido): Int = when (tema) {
    TemaPreferido.SISTEMA -> R.string.tema_sistema
    TemaPreferido.CLARO -> R.string.tema_claro
    TemaPreferido.OSCURO -> R.string.tema_oscuro
}

private fun etiquetaIdioma(idioma: IdiomaPreferido): Int = when (idioma) {
    IdiomaPreferido.SISTEMA -> R.string.idioma_sistema
    IdiomaPreferido.ESPANOL -> R.string.idioma_espanol
    IdiomaPreferido.INGLES -> R.string.idioma_ingles
    IdiomaPreferido.CATALAN -> R.string.idioma_catalan
}
