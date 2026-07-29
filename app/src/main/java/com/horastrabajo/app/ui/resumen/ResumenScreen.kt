package com.horastrabajo.app.ui.resumen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horastrabajo.app.R
import com.horastrabajo.app.data.export.EstiloFila
import com.horastrabajo.app.data.export.FilaResumen
import com.horastrabajo.app.data.export.JpegExporter
import com.horastrabajo.app.data.export.PdfExporter
import com.horastrabajo.app.data.export.compartirArchivo
import com.horastrabajo.app.data.export.construirFilasResumen
import com.horastrabajo.app.data.export.directorioExports
import com.horastrabajo.app.ui.AppViewModelProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val TAG = "ResumenScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenScreen(
    trabajoId: Long,
    anio: Int,
    mes: Int,
    onVolver: () -> Unit,
    viewModel: ResumenViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(trabajoId, anio, mes) { viewModel.cargar(trabajoId, anio, mes) }

    val resumen by viewModel.resumen.collectAsState()
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var exportando by remember { mutableStateOf(false) }

    val mensajeError = stringResource(R.string.resumen_error_exportar)
    val pieDeMarca = stringResource(R.string.export_pie_de_marca)

    fun exportar(extension: String, mimeType: String, generar: suspend (File) -> Unit) {
        scope.launch {
            exportando = true
            try {
                val marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                val archivo = File(directorioExports(contexto), "resumen_$marca.$extension")
                withContext(Dispatchers.IO) { generar(archivo) }
                compartirArchivo(contexto, archivo, mimeType)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al exportar el resumen a .$extension", e)
                snackbarHostState.showSnackbar(mensajeError)
            } finally {
                exportando = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.resumen_titulo)) },
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
        val resumenActual = resumen
        if (resumenActual == null) {
            Row(modifier = Modifier.fillMaxSize().padding(padding), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        // Se construyen con el contexto ya localizado, así el PDF/JPEG sale en el idioma de la app.
        val filas: List<FilaResumen> = construirFilasResumen(resumenActual, contexto)

        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = !exportando,
                    onClick = {
                        exportar("pdf", "application/pdf") { archivo ->
                            PdfExporter.exportar(filas, pieDeMarca, archivo)
                        }
                    },
                ) {
                    if (exportando) {
                        CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    }
                    Text(
                        stringResource(R.string.resumen_exportar_pdf),
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                    )
                }
                OutlinedButton(
                    enabled = !exportando,
                    onClick = {
                        exportar("jpg", "image/jpeg") { archivo ->
                            JpegExporter.exportar(filas, pieDeMarca, archivo)
                        }
                    },
                ) {
                    if (exportando) {
                        CircularProgressIndicator(modifier = Modifier.size(ButtonDefaults.IconSize), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    }
                    Text(
                        stringResource(R.string.resumen_exportar_jpeg),
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filas) { fila ->
                    val peso = if (fila.estilo == EstiloFila.NORMAL) FontWeight.Normal else FontWeight.Bold
                    ListItem(
                        headlineContent = { Text(fila.izquierda, fontWeight = peso) },
                        trailingContent = if (fila.derecha.isNotEmpty()) {
                            { Text(fila.derecha, fontWeight = peso) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
