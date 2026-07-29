package com.horastrabajo.app.ui.mes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.horastrabajo.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** Etiqueta encima del botón, igual que [com.horastrabajo.app.ui.entrada.HoraPickerField]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FechaPickerField(etiqueta: String, fecha: LocalDate, onFechaSeleccionada: (LocalDate) -> Unit) {
    var mostrarDialogo by remember { mutableStateOf(false) }

    Column {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium)
        OutlinedButton(onClick = { mostrarDialogo = true }, modifier = Modifier.fillMaxWidth()) {
            Text(fecha.format(FORMATO_FECHA))
        }
    }

    if (mostrarDialogo) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarDialogo = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { millis ->
                        val nuevaFecha = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onFechaSeleccionada(nuevaFecha)
                    }
                    mostrarDialogo = false
                }) { Text(stringResource(R.string.accion_aceptar)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) { Text(stringResource(R.string.accion_cancelar)) }
            },
        ) {
            DatePicker(state = estado)
        }
    }
}
