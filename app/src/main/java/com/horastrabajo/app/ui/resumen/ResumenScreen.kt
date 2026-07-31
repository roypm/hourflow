package com.horastrabajo.app.ui.resumen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horastrabajo.app.R
import com.horastrabajo.app.data.export.Etiquetas
import com.horastrabajo.app.data.export.ResumenExporter
import com.horastrabajo.app.data.export.compartirArchivo
import com.horastrabajo.app.data.export.directorioExports
import com.horastrabajo.app.data.export.etiquetasExport
import com.horastrabajo.app.domain.ResumenMensual
import com.horastrabajo.app.domain.model.formatearHoras
import com.horastrabajo.app.ui.AppViewModelProvider
import com.horastrabajo.app.ui.components.localeActual
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
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val resumen by viewModel.resumen.collectAsState()
    var exportandoFormato by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(trabajoId, anio, mes) {
        viewModel.cargar(trabajoId, anio, mes)
    }

    val hayDatos = resumen?.let { it.dias.isNotEmpty() || it.dineroExtra.isNotEmpty() } == true
    val mensajeError = stringResource(R.string.resumen_error_exportar)

    fun exportar(extension: String, mimeType: String, generar: suspend (File) -> Unit) {
        scope.launch {
            exportandoFormato = extension
            try {
                val marca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                val archivo = File(directorioExports(contexto), "resumen_$marca.$extension")
                withContext(Dispatchers.IO) {
                    generar(archivo)
                }
                compartirArchivo(contexto, archivo, mimeType)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al exportar el resumen a .$extension", e)
                snackbarHostState.showSnackbar(mensajeError)
            } finally {
                exportandoFormato = null
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
        when {
            resumenActual == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                val localeExport = localeActual()
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (hayDatos) {
                        FilaTotales(resumenActual)
                    }
                    TarjetaExport(
                        titulo = stringResource(R.string.resumen_exportar_pdf),
                        subtitulo = stringResource(R.string.resumen_pdf_subtitulo),
                        exportando = exportandoFormato == "pdf",
                        habilitada = hayDatos,
                        onClick = { exportar("pdf", "application/pdf") { ResumenExporter.exportarPdf(it) } },
                    )
                    TarjetaExport(
                        titulo = stringResource(R.string.resumen_exportar_jpeg),
                        subtitulo = stringResource(R.string.resumen_jpeg_subtitulo),
                        exportando = exportandoFormato == "jpg",
                        habilitada = hayDatos,
                        onClick = {
                            val etiquetas = etiquetasExport(contexto.resources, resumenActual)
                            exportar("jpg", "image/jpeg") { file ->
                                ResumenExporter.exportarJpg(file, resumenActual, etiquetas, localeExport)
                            }
                        },
                    )
                    if (!hayDatos) {
                        Text(
                            text = stringResource(R.string.resumen_vacio),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaTotales(resumen: ResumenMensual) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.mes_total_horas, formatearHoras(resumen.totalHoras)),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(
                R.string.resumen_total_a_cobrar,
                resumen.totalDinero.formateado(resumen.trabajo.simboloMoneda),
            ),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaExport(
    titulo: String,
    subtitulo: String,
    exportando: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        enabled = habilitada && !exportando,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (exportando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
