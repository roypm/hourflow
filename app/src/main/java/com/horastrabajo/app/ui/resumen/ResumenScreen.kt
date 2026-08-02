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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.horastrabajo.app.data.export.formatearDiaSemanaCorto
import com.horastrabajo.app.data.export.formatearFechaCorta
import com.horastrabajo.app.data.export.formatearHora
import com.horastrabajo.app.data.export.formatearMesAnio
import com.horastrabajo.app.data.export.nombreArchivoResumen
import com.horastrabajo.app.domain.ResumenMensual
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.formatearHoras
import com.horastrabajo.app.ui.AppViewModelProvider
import com.horastrabajo.app.ui.components.localeActual
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
                val resumenParaNombre = resumen ?: error("El resumen aún no está disponible")
                val archivo = File(
                    directorioExports(contexto),
                    nombreArchivoResumen(resumenParaNombre, extension),
                )
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (hayDatos) {
                        CabeceraResumen(resumenActual, localeExport)
                        PanelMetricas(resumenActual)
                        VistaPrevia(resumenActual, localeExport)
                        NombresArchivos(resumenActual)
                    }
                    TarjetaExport(
                        titulo = stringResource(R.string.resumen_exportar_pdf),
                        subtitulo = stringResource(R.string.resumen_pdf_subtitulo),
                        exportando = exportandoFormato == "pdf",
                        habilitada = hayDatos,
                        onClick = {
                            val etiquetas = etiquetasExport(contexto.resources, resumenActual)
                            exportar("pdf", "application/pdf") {
                                ResumenExporter.exportarPdf(contexto, it, resumenActual, etiquetas, localeExport)
                            }
                        },
                    )
                    TarjetaExport(
                        titulo = stringResource(R.string.resumen_exportar_jpeg),
                        subtitulo = stringResource(R.string.resumen_jpeg_subtitulo),
                        exportando = exportandoFormato == "jpg",
                        habilitada = hayDatos,
                        onClick = {
                            val etiquetas = etiquetasExport(contexto.resources, resumenActual)
                            exportar("jpg", "image/jpeg") { file ->
                                ResumenExporter.exportarJpg(contexto, file, resumenActual, etiquetas, localeExport)
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
private fun CabeceraResumen(resumen: ResumenMensual, locale: java.util.Locale) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(resumen.trabajo.nombre, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(
                R.string.resumen_periodo,
                resumen.trabajo.nombreUsuario + " · " + formatearMesAnio(resumen.mes, resumen.anio, locale),
            ).trim(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PanelMetricas(resumen: ResumenMensual) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.resumen_turnos, resumen.dias.sumOf { it.entradas.size }), style = MaterialTheme.typography.titleMedium)
                Text(formatearHoras(resumen.totalHoras), style = MaterialTheme.typography.titleMedium)
            }
            FilaMetrica(stringResource(R.string.resumen_dinero_turnos), resumen.totalDineroHoras.formateado(resumen.trabajo.simboloMoneda))
            FilaMetrica(stringResource(R.string.resumen_dinero_extra), resumen.totalDineroExtra.formateado(resumen.trabajo.simboloMoneda))
            FilaMetrica(
                stringResource(R.string.resumen_total_a_cobrar, "").trimEnd(':', ' '),
                resumen.totalDinero.formateado(resumen.trabajo.simboloMoneda),
                destacado = true,
            )
        }
    }
}

@Composable
private fun FilaMetrica(etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
        Text(valor, style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun VistaPrevia(resumen: ResumenMensual, locale: java.util.Locale) {
    val turnos = resumen.dias.flatMap { dia -> dia.entradas.map { dia.fecha to it } }.take(4)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.resumen_previsualizacion), style = MaterialTheme.typography.titleMedium)
            turnos.forEach { (fecha, entrada) ->
                val tarifa = entrada.precioPorHoraCustom ?: resumen.precioPorHora ?: Dinero.CERO
                val dinero = Dinero.porHoras(entrada.horasDecimal, tarifa).formateado(resumen.trabajo.simboloMoneda)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "${formatearFechaCorta(fecha)} ${formatearDiaSemanaCorto(fecha, locale)} · ${formatearHora(entrada.horaEntrada)}-${formatearHora(entrada.horaSalida)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(0.65f),
                    )
                    Text(
                        "${formatearHoras(entrada.horasDecimal)} · $dinero",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (resumen.dias.sumOf { it.entradas.size } > turnos.size) {
                Text(
                    text = "…",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun NombresArchivos(resumen: ResumenMensual) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.resumen_archivos), style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(R.string.resumen_archivo_pdf, nombreArchivoResumen(resumen, "pdf")),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(R.string.resumen_archivo_jpg, nombreArchivoResumen(resumen, "jpg")),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
