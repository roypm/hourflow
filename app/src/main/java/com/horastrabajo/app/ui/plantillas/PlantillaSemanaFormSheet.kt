package com.horastrabajo.app.ui.plantillas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.horastrabajo.app.domain.model.PlantillaDia
import com.horastrabajo.app.domain.model.PlantillaSemana
import com.horastrabajo.app.ui.components.CampoObligatorio
import com.horastrabajo.app.ui.entrada.HoraPickerField
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PlantillaSemanaFormSheet(
    trabajoId: Long,
    plantillaExistente: PlantillaSemana?,
    onGuardar: (PlantillaSemana) -> Unit,
    onCancelar: () -> Unit,
    onDirtyChange: (Boolean) -> Unit = {},
) {
    var nombre by remember { mutableStateOf(plantillaExistente?.nombre ?: "") }
    val nombreInicial = remember { plantillaExistente?.nombre ?: "" }
    var mostrarErrores by remember { mutableStateOf(false) }
    var confirmarDescartarCambios by remember { mutableStateOf(false) }
    val diasEstado = remember {
        mutableStateOf(
            plantillaExistente?.dias ?: (1..7).map { PlantillaDia(it, null, null, false) }
        )
    }
    val locale = java.util.Locale.getDefault()

    val esValido = nombre.isNotBlank()
    val diasInicial = remember { plantillaExistente?.dias ?: (1..7).map { PlantillaDia(it, null, null, false) } }
    val hayCambios = plantillaExistente != null && (nombre != nombreInicial || diasEstado.value != diasInicial)

    LaunchedEffect(hayCambios) {
        onDirtyChange(hayCambios)
    }

    BackHandler(enabled = hayCambios) {
        confirmarDescartarCambios = true
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (plantillaExistente != null) stringResource(R.string.plantilla_editar_semana) else stringResource(R.string.plantilla_nueva_semana),
            style = MaterialTheme.typography.titleLarge,
        )

        CampoObligatorio(
            label = stringResource(R.string.campo_nombre),
            texto = nombre,
            onTextoChange = { nombre = it },
            mostrarErrores = mostrarErrores,
        )

        diasEstado.value.forEachIndexed { index, dia ->
            val nombreDia = DayOfWeek.of(dia.diaSemana).getDisplayName(TextStyle.FULL, locale)
            DiaEditable(
                nombreDia = nombreDia,
                dia = dia,
                onCambio = { nuevoDia ->
                    val lista = diasEstado.value.toMutableList()
                    lista[index] = nuevoDia
                    diasEstado.value = lista
                },
            )
            if (index < 6) HorizontalDivider()
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                if (esValido) {
                    onGuardar(
                        PlantillaSemana(
                            id = plantillaExistente?.id ?: 0L,
                            trabajoId = trabajoId,
                            nombre = nombre.trim(),
                            descripcion = "",
                            dias = diasEstado.value,
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
            text = { Text(stringResource(R.string.plantilla_cambios_semana_mensaje)) },
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

@Composable
private fun DiaEditable(
    nombreDia: String,
    dia: PlantillaDia,
    onCambio: (PlantillaDia) -> Unit,
) {
    var trabaja by remember { mutableStateOf(dia.horaEntrada != null) }
    var horaEntrada by remember {
        mutableStateOf(
            dia.horaEntrada?.let {
                val p = it.split(":")
                LocalTime.of(p[0].toInt(), p[1].toInt())
            } ?: LocalTime.of(9, 0)
        )
    }
    var horaSalida by remember {
        mutableStateOf(
            dia.horaSalida?.let {
                val p = it.split(":")
                LocalTime.of(p[0].toInt(), p[1].toInt())
            } ?: LocalTime.of(17, 0)
        )
    }
    var esDiaSiguiente by remember { mutableStateOf(dia.esDiaSiguiente) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = trabaja,
                onCheckedChange = { activo ->
                    trabaja = activo
                    onCambio(
                        dia.copy(
                            horaEntrada = if (activo) "${horaEntrada.hour}:${horaEntrada.minute.toString().padStart(2, '0')}" else null,
                            horaSalida = if (activo) "${horaSalida.hour}:${horaSalida.minute.toString().padStart(2, '0')}" else null,
                            esDiaSiguiente = if (activo) esDiaSiguiente else false,
                        )
                    )
                },
            )
            Text(nombreDia, style = MaterialTheme.typography.bodyLarge)
        }

        if (trabaja) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HoraPickerField(
                    etiqueta = stringResource(R.string.entrada_hora_entrada),
                    hora = horaEntrada,
                    onHoraSeleccionada = { h ->
                        horaEntrada = h
                        onCambio(dia.copy(horaEntrada = "${h.hour}:${h.minute.toString().padStart(2, '0')}"))
                    },
                    modifier = Modifier.weight(1f),
                )
                HoraPickerField(
                    etiqueta = stringResource(R.string.entrada_hora_salida),
                    hora = horaSalida,
                    onHoraSeleccionada = { h ->
                        horaSalida = h
                        onCambio(dia.copy(horaSalida = "${h.hour}:${h.minute.toString().padStart(2, '0')}"))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.padding(start = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = esDiaSiguiente,
                    onCheckedChange = { c ->
                        esDiaSiguiente = c
                        onCambio(dia.copy(esDiaSiguiente = c))
                    },
                )
                Text(stringResource(R.string.entrada_turno_nocturno))
            }
        }
    }
}
