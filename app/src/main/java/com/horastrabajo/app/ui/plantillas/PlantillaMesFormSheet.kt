package com.horastrabajo.app.ui.plantillas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.horastrabajo.app.R
import com.horastrabajo.app.domain.model.PlantillaMes
import com.horastrabajo.app.domain.model.PlantillaSemana
import com.horastrabajo.app.ui.components.CampoObligatorio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantillaMesFormSheet(
    trabajoId: Long,
    plantillaExistente: PlantillaMes?,
    plantillasSemanaDisponibles: List<PlantillaSemana>,
    onGuardar: (PlantillaMes) -> Unit,
    onCancelar: () -> Unit,
    onDirtyChange: (Boolean) -> Unit = {},
) {
    var nombre by remember { mutableStateOf(plantillaExistente?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(plantillaExistente?.descripcion ?: "") }
    val nombreInicial = remember { plantillaExistente?.nombre ?: "" }
    val descripcionInicial = remember { plantillaExistente?.descripcion ?: "" }
    var mostrarErrores by remember { mutableStateOf(false) }
    var confirmarDescartarCambios by remember { mutableStateOf(false) }
    var defaultId by remember { mutableStateOf(plantillaExistente?.plantillaSemanaDefault?.id) }
    var overrides by remember {
        mutableStateOf(
            plantillaExistente?.overridesPorSemana ?: emptyMap()
        )
    }
    var semanaEditando by remember { mutableStateOf<Int?>(null) }
    val esValido = nombre.isNotBlank()
    val defaultIdInicial = remember { plantillaExistente?.plantillaSemanaDefault?.id }
    val overridesInicial = remember { plantillaExistente?.overridesPorSemana ?: emptyMap() }
    val hayCambios = plantillaExistente != null && (nombre != nombreInicial || descripcion != descripcionInicial || defaultId != defaultIdInicial || overrides != overridesInicial)

    LaunchedEffect(hayCambios) {
        onDirtyChange(hayCambios)
    }

    BackHandler(enabled = hayCambios) {
        confirmarDescartarCambios = true
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (plantillaExistente != null) stringResource(R.string.plantilla_editar_mes) else stringResource(R.string.plantilla_nuevo_mes),
            style = MaterialTheme.typography.titleLarge,
        )

        CampoObligatorio(
            label = stringResource(R.string.campo_nombre),
            texto = nombre,
            onTextoChange = { nombre = it },
            mostrarErrores = mostrarErrores,
        )

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text(stringResource(R.string.plantilla_descripcion)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3,
        )

        SelectorPlantillaSemana(
            etiqueta = stringResource(R.string.plantilla_semana_default),
            plantillas = plantillasSemanaDisponibles,
            seleccionadoId = defaultId,
            onSeleccionar = { defaultId = it },
        )

        HorizontalDivider()

        Text(stringResource(R.string.plantilla_overrides_semana), style = MaterialTheme.typography.titleSmall)

        (1..6).forEach { semanaDelMes ->
            val override = overrides[semanaDelMes]
            val textoActual = when {
                override == null || defaultId != null && override.id == defaultId -> stringResource(R.string.plantilla_usar_base)
                override.id == -1L -> stringResource(R.string.plantilla_semana_libre)
                else -> override.nombre
            }
            OverrideRowCompacto(
                semanaDelMes = semanaDelMes,
                textoActual = textoActual,
                onClick = { semanaEditando = semanaDelMes },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                if (esValido) {
                    val defaultPlantilla = defaultId?.let { id ->
                        plantillasSemanaDisponibles.firstOrNull { it.id == id }
                    }
                    onGuardar(
                        PlantillaMes(
                            id = plantillaExistente?.id ?: 0L,
                            trabajoId = trabajoId,
                            nombre = nombre.trim(),
                            descripcion = descripcion.trim(),
                            plantillaSemanaDefault = defaultPlantilla,
                            overridesPorSemana = overrides,
                        )
                    )
                } else {
                    mostrarErrores = true
                }
            }) { Text(stringResource(R.string.accion_guardar)) }
            TextButton(onClick = {
                if (hayCambios) confirmarDescartarCambios = true else onCancelar()
            }) { Text(stringResource(R.string.accion_cancelar)) }
        }
    }

    if (semanaEditando != null) {
        val semana = semanaEditando!!
        OverrideDialog(
            semanaDelMes = semana,
            plantillasDisponibles = plantillasSemanaDisponibles,
            seleccionado = overrides.let {
                val sel = it[semana]
                if (sel != null && defaultId != null && sel.id == defaultId) null else sel
            },
            defaultId = defaultId,
            onSeleccionar = { nuevaPlantilla ->
                overrides = if (nuevaPlantilla != null) {
                    overrides + (semana to nuevaPlantilla)
                } else {
                    overrides - semana
                }
                semanaEditando = null
            },
            onDismiss = { semanaEditando = null },
        )
    }

    if (confirmarDescartarCambios) {
        AlertDialog(
            onDismissRequest = { confirmarDescartarCambios = false },
            title = { Text(stringResource(R.string.cambios_sin_guardar_titulo)) },
            text = { Text(stringResource(R.string.plantilla_cambios_mes_mensaje)) },
            confirmButton = {
                TextButton(onClick = { confirmarDescartarCambios = false }) {
                    Text(stringResource(R.string.accion_seguir_editando))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmarDescartarCambios = false
                    onCancelar()
                }) {
                    Text(stringResource(R.string.accion_descartar_cambios))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorPlantillaSemana(
    etiqueta: String,
    plantillas: List<PlantillaSemana>,
    seleccionadoId: Long?,
    onSeleccionar: (Long?) -> Unit,
) {
    var expandido by remember { mutableStateOf(false) }
    val seleccionado = plantillas.firstOrNull { it.id == seleccionadoId }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
    ) {
        OutlinedTextField(
            value = seleccionado?.nombre ?: stringResource(R.string.plantilla_sin_seleccion),
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plantilla_sin_seleccion)) },
                onClick = { onSeleccionar(null); expandido = false },
            )
            plantillas.forEach { ps ->
                DropdownMenuItem(
                    text = { Text(ps.nombre) },
                    onClick = { onSeleccionar(ps.id); expandido = false },
                )
            }
        }
    }
}

@Composable
private fun OverrideRowCompacto(
    semanaDelMes: Int,
    textoActual: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.plantilla_semana_numero, semanaDelMes),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            textoActual,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OverrideDialog(
    semanaDelMes: Int,
    plantillasDisponibles: List<PlantillaSemana>,
    seleccionado: PlantillaSemana?,
    defaultId: Long?,
    onSeleccionar: (PlantillaSemana?) -> Unit,
    onDismiss: () -> Unit,
) {
    val opciones = buildList {
        add(Pair(stringResource(R.string.plantilla_usar_base), null as PlantillaSemana?))
        add(Pair(stringResource(R.string.plantilla_semana_libre), PlantillaSemana(-1, -1, "", "", emptyList())))
        addAll(plantillasDisponibles.filter { it.id != defaultId }.map { Pair(it.nombre, it) })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plantilla_semana_numero, semanaDelMes)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(R.string.plantilla_elegir_semana),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                opciones.forEach { (label, plantilla) ->
                    val esSeleccionado = when {
                        plantilla == null && seleccionado == null -> true
                        plantilla?.id == -1L && seleccionado?.id == -1L -> true
                        seleccionado != null && plantilla?.id ?: -2L == seleccionado.id -> true
                        else -> false
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeleccionar(plantilla) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = esSeleccionado,
                            onClick = { onSeleccionar(plantilla) },
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.accion_cancelar)) }
        },
    )
}
