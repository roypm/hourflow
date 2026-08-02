package com.horastrabajo.app.ui.plantillas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import com.horastrabajo.app.ui.components.TimePickerDialogoM3
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
    var descripcion by remember { mutableStateOf(plantillaExistente?.descripcion ?: "") }
    val nombreInicial = remember { plantillaExistente?.nombre ?: "" }
    val descripcionInicial = remember { plantillaExistente?.descripcion ?: "" }
    var mostrarErrores by remember { mutableStateOf(false) }
    var confirmarDescartarCambios by remember { mutableStateOf(false) }
    val diasEstado = remember {
        mutableStateOf(
            plantillaExistente?.dias ?: (1..7).map { PlantillaDia(it, null, null, false) }
        )
    }
    val locale = java.util.Locale.getDefault()

    val horariosValidos = diasEstado.value.all { dia ->
        dia.horaEntrada == null || dia.esDiaSiguiente || dia.horaSalida!! > dia.horaEntrada!!
    }
    val esValido = nombre.isNotBlank() && horariosValidos
    val diasInicial = remember { plantillaExistente?.dias ?: (1..7).map { PlantillaDia(it, null, null, false) } }
    val hayCambios = plantillaExistente != null && (nombre != nombreInicial || descripcion != descripcionInicial || diasEstado.value != diasInicial)

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

        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text(stringResource(R.string.plantilla_descripcion)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3,
        )

        diasEstado.value.forEachIndexed { index, dia ->
            val nombreDia = DayOfWeek.of(dia.diaSemana).getDisplayName(TextStyle.SHORT, locale)
            DiaRowCompacto(
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
                            descripcion = descripcion.trim(),
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
private fun DiaRowCompacto(
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
    var mostrarErrorHorario by remember { mutableStateOf(false) }
    val horarioInvalido = trabaja && !esDiaSiguiente && horaSalida <= horaEntrada

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = trabaja,
            onCheckedChange = { activo ->
                trabaja = activo
                onCambio(
                    dia.copy(
                        horaEntrada = if (activo) "${horaEntrada.hour.toString().padStart(2, '0')}:${horaEntrada.minute.toString().padStart(2, '0')}" else null,
                        horaSalida = if (activo) "${horaSalida.hour.toString().padStart(2, '0')}:${horaSalida.minute.toString().padStart(2, '0')}" else null,
                        esDiaSiguiente = if (activo) esDiaSiguiente else false,
                    )
                )
            },
        )
        Text(
            nombreDia,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.widthIn(min = 60.dp),
        )
        if (trabaja) {
            HoraPickerFieldCompacto(
                hora = horaEntrada,
                onHoraSeleccionada = { h ->
                    horaEntrada = h
                    onCambio(dia.copy(horaEntrada = "${h.hour.toString().padStart(2, '0')}:${h.minute.toString().padStart(2, '0')}"))
                },
            )
            Text(" - ", style = MaterialTheme.typography.bodyMedium)
            HoraPickerFieldCompacto(
                hora = horaSalida,
                onHoraSeleccionada = { h ->
                    horaSalida = h
                    onCambio(dia.copy(horaSalida = "${h.hour.toString().padStart(2, '0')}:${h.minute.toString().padStart(2, '0')}"))
                },
            )
            if (horarioInvalido) {
                Text(
                    text = "⚠️",
                    modifier = Modifier
                        .clickable { mostrarErrorHorario = true }
                        .padding(start = 2.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Checkbox(
                checked = esDiaSiguiente,
                onCheckedChange = { c ->
                    esDiaSiguiente = c
                    onCambio(dia.copy(esDiaSiguiente = c))
                },
            )
            Text(
                stringResource(R.string.entrada_turno_nocturno),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (mostrarErrorHorario) {
        AlertDialog(
            onDismissRequest = { mostrarErrorHorario = false },
            title = { Text(nombreDia) },
            text = { Text(stringResource(R.string.entrada_error_horario)) },
            confirmButton = {
                TextButton(onClick = { mostrarErrorHorario = false }) {
                    Text(stringResource(R.string.accion_aceptar))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HoraPickerFieldCompacto(
    hora: LocalTime,
    onHoraSeleccionada: (LocalTime) -> Unit,
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    val formateada = "${hora.hour.toString().padStart(2, '0')}:${hora.minute.toString().padStart(2, '0')}"

    OutlinedButton(
        onClick = { mostrarDialogo = true },
        modifier = Modifier.width(62.dp).height(32.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
    ) {
        Text(
            text = formateada,
            maxLines = 1,
        )
    }

    if (mostrarDialogo) {
        val estado = rememberTimePickerState(
            initialHour = hora.hour,
            initialMinute = hora.minute,
            is24Hour = true,
        )
        TimePickerDialogoM3(
            onCancelar = { mostrarDialogo = false },
            onAceptar = {
                onHoraSeleccionada(LocalTime.of(estado.hour, estado.minute))
                mostrarDialogo = false
            },
        ) {
            TimePicker(state = estado)
        }
    }
}
