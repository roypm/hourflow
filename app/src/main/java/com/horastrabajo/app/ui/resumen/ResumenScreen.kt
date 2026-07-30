package com.horastrabajo.app.ui.resumen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horastrabajo.app.R
import com.horastrabajo.app.data.export.JpegExporter
import com.horastrabajo.app.data.export.PdfExporter
import com.horastrabajo.app.data.export.compartirArchivo
import com.horastrabajo.app.data.export.directorioExports
import com.horastrabajo.app.domain.ResumenSemana
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.formatearHoras
import com.horastrabajo.app.ui.AppViewModelProvider
import com.horastrabajo.app.ui.components.CifraDestacada
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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
    val locale = java.util.Locale.getDefault()

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

        val simbolo = resumenActual.trabajo.simboloMoneda
        val nombreMes = java.time.Month.of(resumenActual.mes)
            .getDisplayName(TextStyle.FULL_STANDALONE, locale)
            .replaceFirstChar { it.uppercase() }

        Column(modifier = Modifier.padding(padding)) {
            // Botones de exportación
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = !exportando,
                    onClick = {
                        exportar("pdf", "application/pdf") { archivo ->
                            PdfExporter.exportarResumen(contexto, resumenActual, pieDeMarca, archivo)
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
                            JpegExporter.exportarResumen(contexto, resumenActual, pieDeMarca, archivo)
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

            // Tarjeta de resumen rápido
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "${resumenActual.trabajo.nombreUsuario} · ${resumenActual.trabajo.nombre}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "$nombreMes ${resumenActual.anio}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val tarifaTexto = resumenActual.precioPorHora
                        ?.let { it.formateado(simbolo) + "/hora" }
                        ?: stringResource(R.string.mes_tarifa_sin_fijar)
                    Text(
                        stringResource(R.string.export_tarifa, tarifaTexto),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Tabla compacta de días
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                val semanas = resumenActual.semanas

                items(resumenActual.dias) { dia ->
                    val diaSemana = dia.fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                        .lowercase(locale).removeSuffix(".")

                    DiaResumenRow(
                        fecha = dia.fecha,
                        diaSemana = diaSemana,
                        horas = dia.horas,
                        dinero = dia.dinero,
                        simbolo = simbolo,
                        esFinSemana = dia.fecha.dayOfWeek == DayOfWeek.SATURDAY ||
                            dia.fecha.dayOfWeek == DayOfWeek.SUNDAY,
                        esHoy = dia.fecha == LocalDate.now(),
                    )

                    // Subtotales de semana al final de cada semana
                    val semana = semanas.find { it.ultimoDiaVisible == dia.fecha }
                    if (semana != null) {
                        SemanaTotalRow(semana, simbolo)
                    }
                }

                // Dinero extra
                if (resumenActual.dineroExtra.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            stringResource(R.string.dinero_extra_titulo),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    items(resumenActual.dineroExtra) { extra ->
                        DineroExtraRow(extra, simbolo)
                    }
                }

                // Totales finales
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            CifraDestacada(
                                resumenActual.totalDinero.formateado(simbolo),
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                            Text(
                                stringResource(R.string.mes_total_horas, formatearHoras(resumenActual.totalHoras)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaResumenRow(
    fecha: LocalDate,
    diaSemana: String,
    horas: Double,
    dinero: com.horastrabajo.app.domain.model.Dinero,
    simbolo: String,
    esFinSemana: Boolean,
    esHoy: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${fecha.dayOfMonth}/${fecha.monthValue}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal,
                color = if (esFinSemana) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                diaSemana,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatearHoras(horas),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(50.dp),
            )
            Text(
                dinero.formateado(simbolo),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(80.dp),
            )
        }
    }
}

@Composable
private fun SemanaTotalRow(semana: ResumenSemana, simbolo: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val etiqueta = if (semana.completa) stringResource(R.string.mes_total_semana)
            else stringResource(R.string.mes_total_semana_parcial)
        Text(
            etiqueta,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatearHoras(semana.horas),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(50.dp),
            )
            Text(
                semana.dinero.formateado(simbolo),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(80.dp),
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun DineroExtraRow(extra: DineroExtra, simbolo: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${extra.fecha.dayOfMonth}/${extra.fecha.monthValue} ${extra.descripcion}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            extra.monto.formateado(simbolo),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
