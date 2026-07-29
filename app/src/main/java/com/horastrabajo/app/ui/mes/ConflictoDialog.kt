package com.horastrabajo.app.ui.mes

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.horastrabajo.app.R
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ConflictoDialog(
    fechasConflicto: List<LocalDate>,
    onReemplazar: () -> Unit,
    onSoloDiasVacios: () -> Unit,
    onCancelar: () -> Unit,
) {
    val locale = Locale.getDefault()
    val mensaje = if (fechasConflicto.size <= 5) {
        val fechasTexto = fechasConflicto.joinToString(", ") { fecha ->
            val nombreDia = fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).lowercase()
            "${fecha.dayOfMonth} $nombreDia"
        }
        stringResource(R.string.conflicto_mensaje_corto, fechasConflicto.size, fechasTexto)
    } else {
        stringResource(R.string.conflicto_mensaje_largo, fechasConflicto.size)
    }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.conflicto_titulo)) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = onReemplazar) {
                Text(stringResource(R.string.conflicto_reemplazar))
            }
        },
        dismissButton = {
            TextButton(onClick = onSoloDiasVacios) {
                Text(stringResource(R.string.conflicto_solo_vacios))
            }
            TextButton(onClick = onCancelar) {
                Text(stringResource(R.string.accion_cancelar))
            }
        },
    )
}
