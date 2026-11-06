package com.horastrabajo.app.ui.resumen

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horastrabajo.app.R
import com.horastrabajo.app.data.export.Etiquetas
import com.horastrabajo.app.data.export.ResumenExporter
import com.horastrabajo.app.data.export.compartirArchivo
import com.horastrabajo.app.data.export.directorioExports
import com.horastrabajo.app.data.export.guardarEnDescargas
import com.horastrabajo.app.data.export.permisoDescargaConcedido
import com.horastrabajo.app.data.export.etiquetasExport
import com.horastrabajo.app.data.export.formatearDiaSemanaCorto
import com.horastrabajo.app.data.export.formatearFechaCorta
import com.horastrabajo.app.data.export.formatearHora
import com.horastrabajo.app.data.export.formatearMesAnio
import com.horastrabajo.app.data.export.nombreArchivoResumen
import com.horastrabajo.app.data.preferences.FormatoResumen
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

/**
 * Icono de descarga (trazo oficial de Material). Se define aquí porque el
 * proyecto usa material-icons-core, que no incluye Icons.Filled.Download,
 * y añadir material-icons-extended solo por un icono inflaría el APK.
 */
private val IconoDescargar: ImageVector = ImageVector.Builder(
    name = "Descargar",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    addPath(
        pathData = PathParser().parsePathString(
            "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z",
        ).toNodes(),
        fill = SolidColor(Color.Black),
    )
}.build()

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
    val formatoResumen by viewModel.formatoResumen.collectAsState()
    var accionEnCurso by remember { mutableStateOf<String?>(null) }
    var descargaPendiente by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(trabajoId, anio, mes) {
        viewModel.cargar(trabajoId, anio, mes)
    }

    val hayDatos = resumen?.let { it.dias.isNotEmpty() || it.dineroExtra.isNotEmpty() } == true
    val mensajeError = stringResource(R.string.resumen_error_exportar)
    val mensajeErrorDescarga = stringResource(R.string.resumen_error_descargar)
    val mensajePermisoDenegado = stringResource(R.string.resumen_permiso_descarga_denegado)

    fun procesar(
        accion: AccionExport,
        extension: String,
        mimeType: String,
        generar: suspend (File) -> Unit,
    ) {
        scope.launch {
            accionEnCurso = accion.name.lowercase()
            try {
                val resumenParaNombre = resumen ?: error("El resumen aún no está disponible")
                val archivo = File(
                    directorioExports(contexto),
                    nombreArchivoResumen(resumenParaNombre, extension),
                )
                val nombreDescargado = withContext(Dispatchers.IO) {
                    generar(archivo)
                    if (accion == AccionExport.DESCARGAR) {
                        guardarEnDescargas(contexto, archivo, archivo.name, mimeType)
                    } else {
                        null
                    }
                }
                when (accion) {
                    AccionExport.COMPARTIR -> compartirArchivo(contexto, archivo, mimeType)
                    AccionExport.DESCARGAR -> snackbarHostState.showSnackbar(
                        contexto.getString(R.string.resumen_descargado, nombreDescargado),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al procesar el resumen a .$extension (${accion.name.lowercase()})", e)
                snackbarHostState.showSnackbar(
                    if (accion == AccionExport.DESCARGAR) mensajeErrorDescarga else mensajeError,
                )
            } finally {
                accionEnCurso = null
            }
        }
    }

    val lanzadorPermisoDescarga = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        val pendiente = descargaPendiente
        descargaPendiente = null
        if (concedido) {
            pendiente?.invoke()
        } else {
            scope.launch { snackbarHostState.showSnackbar(mensajePermisoDenegado) }
        }
    }

    fun descargar(extension: String, mimeType: String, generar: suspend (File) -> Unit) {
        if (permisoDescargaConcedido(contexto)) {
            procesar(AccionExport.DESCARGAR, extension, mimeType, generar)
        } else {
            descargaPendiente = { procesar(AccionExport.DESCARGAR, extension, mimeType, generar) }
            lanzadorPermisoDescarga.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    /** Ejecuta compartir o descargar en el formato elegido en Ajustes. */
    fun ejecutar(accion: AccionExport, resumenDestino: ResumenMensual, locale: java.util.Locale) {
        val etiquetas = etiquetasExport(contexto.resources, resumenDestino)
        val generar: suspend (File) -> Unit = when (formatoResumen) {
            FormatoResumen.PDF -> { archivo ->
                ResumenExporter.exportarPdf(contexto, archivo, resumenDestino, etiquetas, locale)
            }
            FormatoResumen.JPEG -> { archivo ->
                ResumenExporter.exportarJpg(contexto, archivo, resumenDestino, etiquetas, locale)
            }
        }
        when (accion) {
            AccionExport.COMPARTIR ->
                procesar(accion, formatoResumen.extension, formatoResumen.mimeType, generar)
            AccionExport.DESCARGAR ->
                descargar(formatoResumen.extension, formatoResumen.mimeType, generar)
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
                        NombresArchivos(resumenActual, formatoResumen)
                    }
                    HeroExport(
                        titulo = stringResource(R.string.resumen_hero_titulo),
                        subtitulo = stringResource(
                            if (formatoResumen == FormatoResumen.PDF) {
                                R.string.resumen_pdf_subtitulo
                            } else {
                                R.string.resumen_jpeg_subtitulo
                            },
                        ),
                        formatoTexto = stringResource(
                            R.string.resumen_formato_en_uso,
                            stringResource(
                                if (formatoResumen == FormatoResumen.PDF) {
                                    R.string.formato_resumen_pdf
                                } else {
                                    R.string.formato_resumen_jpeg
                                },
                            ),
                        ),
                        compartiendo = accionEnCurso == "compartir",
                        descargando = accionEnCurso == "descargar",
                        habilitada = hayDatos,
                        onCompartir = { ejecutar(AccionExport.COMPARTIR, resumenActual, localeExport) },
                        onDescargar = { ejecutar(AccionExport.DESCARGAR, resumenActual, localeExport) },
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
            if (resumen.dineroExtra.isNotEmpty()) {
                FilaMetrica(stringResource(R.string.resumen_dinero_extra), resumen.totalDineroExtra.formateado(resumen.trabajo.simboloMoneda))
            }
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
    var expandida by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandida = !expandida },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.resumen_previsualizacion), style = MaterialTheme.typography.titleMedium)
                Icon(
                    imageVector = if (expandida) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (expandida) R.string.resumen_contraer_previsualizacion else R.string.resumen_expandir_previsualizacion,
                    ),
                )
            }
            AnimatedVisibility(visible = expandida) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    }
}

@Composable
private fun NombresArchivos(resumen: ResumenMensual, formato: FormatoResumen) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.resumen_archivos), style = MaterialTheme.typography.labelLarge)
        Text(
            stringResource(
                if (formato == FormatoResumen.PDF) R.string.resumen_archivo_pdf else R.string.resumen_archivo_jpg,
                nombreArchivoResumen(resumen, formato.extension),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private enum class AccionExport { COMPARTIR, DESCARGAR }

@Composable
private fun HeroExport(
    titulo: String,
    subtitulo: String,
    formatoTexto: String,
    compartiendo: Boolean,
    descargando: Boolean,
    habilitada: Boolean,
    onCompartir: () -> Unit,
    onDescargar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ocupada = compartiendo || descargando
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatoTexto,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onCompartir,
                    enabled = habilitada && !ocupada,
                    modifier = Modifier.weight(1f),
                ) {
                    if (compartiendo) {
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.accion_compartir))
                }
                Button(
                    onClick = onDescargar,
                    enabled = habilitada && !ocupada,
                    modifier = Modifier.weight(1f),
                ) {
                    if (descargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            IconoDescargar,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.resumen_descargar))
                }
            }
        }
    }
}
