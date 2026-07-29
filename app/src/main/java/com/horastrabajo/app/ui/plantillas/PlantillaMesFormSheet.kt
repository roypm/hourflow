package com.horastrabajo.app.ui.plantillas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val nombreInicial = remember { plantillaExistente?.nombre ?: "" }
    var mostrarErrores by remember { mutableStateOf(false) }
    var confirmarDescartarCambios by remember { mutableStateOf(false) }
    var defaultId by remember { mutableStateOf(plantillaExistente?.plantillaSemanaDefault?.id) }
    var overrides by remember {
        mutableStateOf(
            plantillaExistente?.overridesPorSemana ?: emptyMap()
        )
    }
    val esValido = nombre.isNotBlank()
    val defaultIdInicial = remember { plantillaExistente?.plantillaSemanaDefault?.id }
    val overridesInicial = remember { plantillaExistente?.overridesPorSemana ?: emptyMap() }
    val hayCambios = plantillaExistente != null && (nombre != nombreInicial || defaultId != defaultIdInicial || overrides != overridesInicial)

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

        SelectorPlantillaSemana(
            etiqueta = stringResource(R.string.plantilla_semana_default),
            plantillas = plantillasSemanaDisponibles,
            seleccionadoId = defaultId,
            onSeleccionar = { defaultId = it },
        )

        HorizontalDivider()

        Text(stringResource(R.string.plantilla_overrides_semana), style = MaterialTheme.typography.titleSmall)

        (1..6).forEach { semanaDelMes ->
            OverrideRow(
                semanaDelMes = semanaDelMes,
                plantillasDisponibles = plantillasSemanaDisponibles,
                overrideActual = overrides[semanaDelMes],
                esDefaultNull = defaultId == null,
                onCambio = { nuevaPlantilla ->
                    overrides = if (nuevaPlantilla != null) {
                        overrides + (semanaDelMes to nuevaPlantilla)
                    } else {
                        overrides - semanaDelMes
                    }
                },
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
                            descripcion = "",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverrideRow(
    semanaDelMes: Int,
    plantillasDisponibles: List<PlantillaSemana>,
    overrideActual: PlantillaSemana?,
    esDefaultNull: Boolean,
    onCambio: (PlantillaSemana?) -> Unit,
) {
    var expandido by remember { mutableStateOf(false) }

    val opciones = buildList {
        if (esDefaultNull) add(Pair("Usar base (default)", null))
        else add(Pair("Usar base", null))
        add(Pair("Semana libre", PlantillaSemana(-1, -1, "", "", emptyList())))
        addAll(plantillasDisponibles.map { Pair(it.nombre, it) })
    }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
    ) {
        val textoActual = when {
            overrideActual == null && plantillasDisponibles.none { it.id == -1L } -> stringResource(R.string.plantilla_semana_libre)
            overrideActual != null && overrideActual.id == -1L -> stringResource(R.string.plantilla_semana_libre)
            overrideActual != null -> overrideActual.nombre
            else -> stringResource(R.string.plantilla_usar_base)
        }
        OutlinedTextField(
            value = textoActual,
            onValueChange = {},
            readOnly = true,
            label = { Text("Semana $semanaDelMes") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plantilla_usar_base)) },
                onClick = { onCambio(null); expandido = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.plantilla_semana_libre)) },
                onClick = {
                    onCambio(PlantillaSemana(-1, -1, "", "", emptyList()))
                    expandido = false
                },
            )
            plantillasDisponibles.forEach { ps ->
                DropdownMenuItem(
                    text = { Text(ps.nombre) },
                    onClick = { onCambio(ps); expandido = false },
                )
            }
        }
    }
}
