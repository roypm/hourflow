package com.horastrabajo.app.ui.entrada

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.horastrabajo.app.R
import com.horastrabajo.app.ui.components.TimePickerDialogoM3
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Selector de hora con el [TimePicker] de Compose Material 3 — mismo lenguaje visual
 * y de tema (color, modo oscuro) que [com.horastrabajo.app.ui.mes.FechaPickerField].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoraPickerField(
    etiqueta: String,
    hora: LocalTime,
    onHoraSeleccionada: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mostrarDialogo by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(
            onClick = { mostrarDialogo = true },
            modifier = Modifier.fillMaxWidth().width(120.dp),
        ) {
            Text(hora.format(FORMATO_HORA))
        }
    }

    if (mostrarDialogo) {
        val estado = rememberTimePickerState(initialHour = hora.hour, initialMinute = hora.minute, is24Hour = true)
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


