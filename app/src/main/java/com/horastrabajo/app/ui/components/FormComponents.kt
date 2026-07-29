package com.horastrabajo.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.horastrabajo.app.R
import com.horastrabajo.app.ui.mes.textoADinero

/**
 * Mensaje de error homogéneo para todos los formularios de la app: mismo color,
 * tamaño y posición en cualquier campo que lo use.
 */
@Composable
fun TextoError(mensaje: String, modifier: Modifier = Modifier) {
    Text(
        mensaje,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.padding(start = 16.dp, top = 4.dp),
    )
}

/**
 * Campo de texto obligatorio con criterio único en toda la app: el error de
 * "Campo obligatorio" solo se muestra si [mostrarErrores] es true (se activa al
 * intentar guardar), nunca en un campo recién abierto y vacío.
 */
@Composable
fun CampoObligatorio(
    label: String,
    texto: String,
    onTextoChange: (String) -> Unit,
    mostrarErrores: Boolean,
    modifier: Modifier = Modifier,
) {
    val esError = mostrarErrores && texto.isBlank()
    Column(modifier = modifier) {
        OutlinedTextField(
            value = texto,
            onValueChange = onTextoChange,
            label = { Text(label) },
            isError = esError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (esError) TextoError(stringResource(R.string.campo_obligatorio))
    }
}

/**
 * Campo de importe reutilizado en Tarifa, Dinero extra y Entrada de horas: misma
 * validación, mismo mensaje de error de formato y mismo criterio de "obligatorio"
 * en toda la app.
 */
@Composable
fun CampoDinero(
    label: String,
    texto: String,
    onTextoChange: (String) -> Unit,
    mostrarErrores: Boolean,
    obligatorio: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val vacioInvalido = obligatorio && texto.isBlank()
    val formatoInvalido = texto.isNotBlank() && textoADinero(texto) == null
    val esError = mostrarErrores && (vacioInvalido || formatoInvalido)
    Column(modifier = modifier) {
        OutlinedTextField(
            value = texto,
            onValueChange = onTextoChange,
            label = { Text(label) },
            isError = esError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        if (esError) {
            TextoError(
                stringResource(
                    if (vacioInvalido) R.string.campo_obligatorio else R.string.campo_numero_invalido
                )
            )
        }
    }
}

/**
 * Estilo tipográfico único para cifras destacadas (dinero, horas, totales) en toda
 * la app: más peso y tamaño que su etiqueta, para que el dato importe más que el texto
 * que lo acompaña. No cambia paleta ni identidad visual, solo la jerarquía tipográfica.
 */
@Composable
fun CifraDestacada(texto: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier,
    )
}
