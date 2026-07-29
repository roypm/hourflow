package com.horastrabajo.app.ui.plantillas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horastrabajo.app.R
import com.horastrabajo.app.domain.model.PlantillaDia
import com.horastrabajo.app.domain.model.PlantillaMes
import com.horastrabajo.app.domain.model.PlantillaSemana
import com.horastrabajo.app.ui.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantillasScreen(
    trabajoId: Long,
    onBack: () -> Unit,
    viewModel: PlantillasViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(trabajoId) { viewModel.cargar(trabajoId) }

    val plantillasSemana by viewModel.plantillasSemana.collectAsState()
    val plantillasMes by viewModel.plantillasMes.collectAsState()
    var tabSeleccionada by remember { mutableIntStateOf(0) }
    var plantillaSemanaEditando by remember { mutableStateOf<PlantillaSemana?>(null) }
    var plantillaSemanaNueva by remember { mutableStateOf(false) }
    var plantillaMesEditando by remember { mutableStateOf<PlantillaMes?>(null) }
    var plantillaMesNueva by remember { mutableStateOf(false) }
    var plantillaAEliminar by remember { mutableStateOf<Any?>(null) }
    var plantillaSemanaTieneCambios by remember { mutableStateOf(false) }
    var plantillaMesTieneCambios by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plantillas_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.accion_volver))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (tabSeleccionada == 0) plantillaSemanaNueva = true else plantillaMesNueva = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.accion_anadir))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tabSeleccionada) {
                Tab(
                    selected = tabSeleccionada == 0,
                    onClick = { tabSeleccionada = 0 },
                    text = { Text(stringResource(R.string.plantillas_tab_semanas)) },
                )
                Tab(
                    selected = tabSeleccionada == 1,
                    onClick = { tabSeleccionada = 1 },
                    text = { Text(stringResource(R.string.plantillas_tab_meses)) },
                )
            }

            when (tabSeleccionada) {
                0 -> SemanasTab(
                    plantillas = plantillasSemana,
                    onEditar = { plantillaSemanaEditando = it },
                    onEliminar = { plantillaAEliminar = it },
                )
                1 -> MesesTab(
                    plantillas = plantillasMes,
                    onEditar = { plantillaMesEditando = it },
                    onEliminar = { plantillaAEliminar = it },
                )
            }
        }
    }

    if (plantillaSemanaNueva || plantillaSemanaEditando != null) {
        val existente = plantillaSemanaEditando
        var latestSemanaConfirmValueChange by remember { mutableStateOf({ _: SheetValue -> true }) }
        val semanaSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { targetValue -> latestSemanaConfirmValueChange(targetValue) },
        )
        SideEffect {
            latestSemanaConfirmValueChange = { targetValue ->
                targetValue != SheetValue.Hidden || !plantillaSemanaTieneCambios
            }
        }
        ModalBottomSheet(
            onDismissRequest = { plantillaSemanaNueva = false; plantillaSemanaEditando = null },
            sheetState = semanaSheetState,
        ) {
            PlantillaSemanaFormSheet(
                trabajoId = trabajoId,
                plantillaExistente = existente,
                onGuardar = {
                    viewModel.savePlantillaSemana(it)
                    plantillaSemanaNueva = false; plantillaSemanaEditando = null
                },
                onCancelar = {
                    plantillaSemanaNueva = false; plantillaSemanaEditando = null
                },
                onDirtyChange = { plantillaSemanaTieneCambios = it },
            )
        }
    }

    if (plantillaMesNueva || plantillaMesEditando != null) {
        val existente = plantillaMesEditando
        var latestMesConfirmValueChange by remember { mutableStateOf({ _: SheetValue -> true }) }
        val mesSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { targetValue -> latestMesConfirmValueChange(targetValue) },
        )
        SideEffect {
            latestMesConfirmValueChange = { targetValue ->
                targetValue != SheetValue.Hidden || !plantillaMesTieneCambios
            }
        }
        ModalBottomSheet(
            onDismissRequest = { plantillaMesNueva = false; plantillaMesEditando = null },
            sheetState = mesSheetState,
        ) {
            PlantillaMesFormSheet(
                trabajoId = trabajoId,
                plantillaExistente = existente,
                plantillasSemanaDisponibles = plantillasSemana,
                onGuardar = {
                    viewModel.savePlantillaMes(it)
                    plantillaMesNueva = false; plantillaMesEditando = null
                },
                onCancelar = {
                    plantillaMesNueva = false; plantillaMesEditando = null
                },
                onDirtyChange = { plantillaMesTieneCambios = it },
            )
        }
    }

    plantillaAEliminar?.let { item ->
        val nombre = when (item) {
            is PlantillaSemana -> item.nombre
            is PlantillaMes -> item.nombre
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { plantillaAEliminar = null },
            title = { Text(stringResource(R.string.plantilla_eliminar_titulo)) },
            text = { Text(stringResource(R.string.plantilla_eliminar_mensaje, nombre)) },
            confirmButton = {
                TextButton(onClick = {
                    when (item) {
                        is PlantillaSemana -> viewModel.deletePlantillaSemana(item)
                        is PlantillaMes -> viewModel.deletePlantillaMes(item)
                    }
                    plantillaAEliminar = null
                }) { Text(stringResource(R.string.accion_eliminar)) }
            },
            dismissButton = {
                TextButton(onClick = { plantillaAEliminar = null }) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}

@Composable
private fun SemanasTab(
    plantillas: List<PlantillaSemana>,
    onEditar: (PlantillaSemana) -> Unit,
    onEliminar: (PlantillaSemana) -> Unit,
) {
    if (plantillas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.plantillas_semanas_vacio))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(plantillas, key = { it.id }) { plantilla ->
                PlantillaSemanaCard(plantilla = plantilla, onEditar = { onEditar(plantilla) }, onEliminar = { onEliminar(plantilla) })
            }
        }
    }
}

@Composable
private fun MesesTab(
    plantillas: List<PlantillaMes>,
    onEditar: (PlantillaMes) -> Unit,
    onEliminar: (PlantillaMes) -> Unit,
) {
    if (plantillas.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.plantillas_meses_vacio))
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(plantillas, key = { it.id }) { plantilla ->
                PlantillaMesCard(plantilla = plantilla, onEditar = { onEditar(plantilla) }, onEliminar = { onEliminar(plantilla) })
            }
        }
    }
}

@Composable
private fun PlantillaSemanaCard(plantilla: PlantillaSemana, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val diasTrabajados = plantilla.dias.count { it.horaEntrada != null }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plantilla.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.plantilla_dias_laborales, diasTrabajados),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                IconButton(onClick = onEditar) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.accion_editar)) }
                IconButton(onClick = onEliminar) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.accion_eliminar)) }
            }
        }
    }
}

@Composable
private fun PlantillaMesCard(plantilla: PlantillaMes, onEditar: () -> Unit, onEliminar: () -> Unit) {
    val semanasConfiguradas = plantilla.overridesPorSemana.size
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plantilla.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.plantilla_semanas_configuradas, semanasConfiguradas),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                IconButton(onClick = onEditar) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.accion_editar)) }
                IconButton(onClick = onEliminar) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.accion_eliminar)) }
            }
        }
    }
}
