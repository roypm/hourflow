package com.horastrabajo.app.ui.anio

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horastrabajo.app.R
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.formatearHoras
import com.horastrabajo.app.ui.AppViewModelProvider
import com.horastrabajo.app.ui.components.CifraDestacada
import com.horastrabajo.app.ui.components.localeActual
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnioScreen(
    trabajoId: Long,
    onMesSeleccionado: (Int, Int) -> Unit,
    onVolver: () -> Unit,
    onAbrirPlantillas: () -> Unit,
    viewModel: AnioViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val hoy = LocalDate.now()
    var anioSeleccionado by remember { mutableIntStateOf(hoy.year) }
    val anioMinimo = hoy.year - 99
    val anioMaximo = hoy.year + 99
    val locale = localeActual()

    LaunchedEffect(trabajoId, anioSeleccionado) { viewModel.cargar(trabajoId, anioSeleccionado) }
    val trabajo by viewModel.trabajo.collectAsState()
    val resumenAnual by viewModel.resumenAnual.collectAsState()
    val simbolo = trabajo?.simboloMoneda ?: "€"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trabajo?.nombre ?: "") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accion_volver),
                        )
                    }
                },
                actions = {
                    if (anioSeleccionado != hoy.year) {
                        TextButton(onClick = { anioSeleccionado = hoy.year }) {
                            Text(stringResource(R.string.anio_hoy))
                        }
                    }
                    TextButton(onClick = onAbrirPlantillas) {
                        Text(stringResource(R.string.plantillas_titulo))
                    }
                },
            )
        },
    ) { padding ->
        val trabajoActual = trabajo
        if (trabajoActual == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(modifier = Modifier.padding(padding)) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { anioSeleccionado-- },
                        enabled = anioSeleccionado > anioMinimo,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.anio_anterior),
                        )
                    }
                    Text(
                        text = anioSeleccionado.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    IconButton(
                        onClick = { anioSeleccionado++ },
                        enabled = anioSeleccionado < anioMaximo,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.anio_siguiente),
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    CifraDestacada(
                        texto = stringResource(
                            R.string.anio_total,
                            formatearHoras(resumenAnual?.totalHoras ?: 0.0),
                            (resumenAnual?.totalDinero ?: Dinero.CERO).formateado(simbolo),
                        ),
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items((1..12).toList()) { mes ->
                        val esMesActual = anioSeleccionado == hoy.year && mes == hoy.monthValue
                        val resumenMes = resumenAnual?.meses?.firstOrNull { it.mes == mes }
                        Card(
                            onClick = { onMesSeleccionado(anioSeleccionado, mes) },
                            modifier = Modifier.aspectRatio(1.4f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (esMesActual) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (esMesActual) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            ),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = nombreMesCorto(mes, locale),
                                    fontWeight = if (esMesActual) FontWeight.Bold else FontWeight.Normal,
                                )
                                if (resumenMes != null && resumenMes.horas > 0) {
                                    Text(
                                        text = formatearHoras(resumenMes.horas),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        text = resumenMes.dinero.formateado(simbolo),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun nombreMesCorto(mes: Int, locale: Locale): String =
    Month.of(mes).getDisplayName(TextStyle.SHORT_STANDALONE, locale).replaceFirstChar { it.uppercase(locale) }
