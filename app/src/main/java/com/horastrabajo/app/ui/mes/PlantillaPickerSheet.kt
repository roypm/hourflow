package com.horastrabajo.app.ui.mes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.horastrabajo.app.R
import com.horastrabajo.app.domain.model.PlantillaMes
import com.horastrabajo.app.domain.model.PlantillaSemana

@Composable
fun PlantillaPickerSheet(
    titulo: String,
    plantillas: List<Any>,
    onSeleccionar: (Any) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))

        if (plantillas.isEmpty()) {
            Text(
                stringResource(R.string.plantilla_picker_vacio),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            plantillas.forEachIndexed { index, item ->
                val nombre = when (item) {
                    is PlantillaSemana -> item.nombre
                    is PlantillaMes -> item.nombre
                    else -> ""
                }
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeleccionar(item) }
                        .padding(vertical = 12.dp),
                )
                if (index < plantillas.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}
