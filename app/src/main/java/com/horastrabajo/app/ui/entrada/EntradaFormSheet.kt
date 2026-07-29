package com.horastrabajo.app.ui.entrada

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.horastrabajo.app.R
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.ui.components.CampoDinero
import com.horastrabajo.app.ui.components.TextoError
import com.horastrabajo.app.ui.mes.dineroATexto
import com.horastrabajo.app.ui.mes.textoADinero
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun EntradaFormSheet(
    fecha: LocalDate,
    trabajoId: Long,
    simbolo: String,
    entradasExistentes: List<EntradaHoras>,
    onGuardar: (EntradaHoras) -> Unit,
    onEliminar: (EntradaHoras) -> Unit,
    onRestaurar: (EntradaHoras) -> Unit = onGuardar,
    onCerrar: () -> Unit = {},
    onDirtyChange: (Boolean) -> Unit = {},
) {
    var forzarFormulario by remember(fecha, trabajoId) { mutableStateOf(false) }
    var entradaEnEdicion by remember(fecha, trabajoId) { mutableStateOf<EntradaHoras?>(null) }
    var entradaAEliminar by remember(fecha, trabajoId) { mutableStateOf<EntradaHoras?>(null) }
    var entradaEditadaTieneCambios by remember(fecha, trabajoId) { mutableStateOf(false) }
    var confirmarDescartarCambios by remember(fecha, trabajoId) { mutableStateOf(false) }
    var cerrarTrasDescartarCambios by remember(fecha, trabajoId) { mutableStateOf(false) }
    val mostrarFormulario = forzarFormulario || entradaEnEdicion != null || entradasExistentes.isEmpty()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val textoTurnoEliminado = stringResource(R.string.entrada_turno_eliminado)
    val textoDeshacer = stringResource(R.string.accion_deshacer)
    fun descartarEdicion() {
        entradaEnEdicion = null
        entradaEditadaTieneCambios = false
        confirmarDescartarCambios = false
        cerrarTrasDescartarCambios = false
        forzarFormulario = false
    }
    fun solicitarCancelarFormulario() {
        if (entradaEnEdicion != null && entradaEditadaTieneCambios) {
            cerrarTrasDescartarCambios = false
            confirmarDescartarCambios = true
        } else {
            descartarEdicion()
        }
    }

    LaunchedEffect(entradaEditadaTieneCambios) {
        onDirtyChange(entradaEditadaTieneCambios && entradaEnEdicion != null)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        BackHandler(enabled = entradaEnEdicion != null && entradaEditadaTieneCambios) {
            cerrarTrasDescartarCambios = true
            confirmarDescartarCambios = true
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(
                    R.string.entrada_titulo,
                    "${fecha.dayOfMonth}/${fecha.monthValue}/${fecha.year}",
                )
            )

            entradasExistentes.forEach { entrada ->
                EntradaExistenteRow(
                    entrada = entrada,
                    simbolo = simbolo,
                    mostrarEditar = entradaEnEdicion?.id != entrada.id,
                    onEditar = {
                        entradaEnEdicion = entrada
                        entradaEditadaTieneCambios = false
                        forzarFormulario = true
                    },
                    onEliminar = { entradaAEliminar = entrada },
                )
                HorizontalDivider()
            }

            if (mostrarFormulario) {
                NuevaEntradaForm(
                    fecha = fecha,
                    trabajoId = trabajoId,
                    simbolo = simbolo,
                    entradasExistentes = entradasExistentes,
                    entradaInicial = entradaEnEdicion,
                    onDirtyChange = { entradaEditadaTieneCambios = it },
                    onGuardar = { entrada ->
                        onGuardar(entrada)
                        descartarEdicion()
                    },
                    onCancelar = if (entradasExistentes.isNotEmpty()) {
                        { solicitarCancelarFormulario() }
                    } else {
                        null
                    },
                )
            } else {
                TextButton(onClick = { forzarFormulario = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.entrada_anadir_turno))
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    entradaAEliminar?.let { entrada ->
        val rango = stringResource(
            R.string.entrada_rango_horas,
            entrada.horaEntrada.format(FORMATO_HORA),
            entrada.horaSalida.format(FORMATO_HORA),
        )
        AlertDialog(
            onDismissRequest = { entradaAEliminar = null },
            title = { Text(stringResource(R.string.entrada_eliminar_titulo)) },
            text = { Text(stringResource(R.string.entrada_eliminar_mensaje, rango)) },
            confirmButton = {
                TextButton(onClick = {
                    entradaAEliminar = null
                    if (entradaEnEdicion?.id == entrada.id) entradaEnEdicion = null
                    onEliminar(entrada)
                    scope.launch {
                        val resultado = snackbarHostState.showSnackbar(
                            message = textoTurnoEliminado,
                            actionLabel = textoDeshacer,
                            duration = SnackbarDuration.Short,
                        )
                        if (resultado == SnackbarResult.ActionPerformed) {
                            onRestaurar(entrada)
                        }
                    }
                }) { Text(stringResource(R.string.accion_eliminar)) }
            },
            dismissButton = {
                TextButton(onClick = { entradaAEliminar = null }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
        )
    }

    if (confirmarDescartarCambios) {
        AlertDialog(
            onDismissRequest = { confirmarDescartarCambios = false },
            title = { Text(stringResource(R.string.cambios_sin_guardar_titulo)) },
            text = { Text(stringResource(R.string.entrada_descartar_cambios_mensaje)) },
            confirmButton = {
                TextButton(onClick = { confirmarDescartarCambios = false }) {
                    Text(stringResource(R.string.accion_seguir_editando))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val cerrar = cerrarTrasDescartarCambios
                    descartarEdicion()
                    if (cerrar) onCerrar()
                }) {
                    Text(stringResource(R.string.accion_descartar_cambios))
                }
            },
        )
    }
}

@Composable
private fun EntradaCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EntradaExistenteRow(
    entrada: EntradaHoras,
    simbolo: String,
    mostrarEditar: Boolean,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            val rango = stringResource(
                R.string.entrada_rango_horas,
                entrada.horaEntrada.format(FORMATO_HORA),
                entrada.horaSalida.format(FORMATO_HORA),
            )
            val sufijo = if (entrada.esDiaSiguiente) {
                " " + stringResource(R.string.entrada_sufijo_dia_siguiente)
            } else {
                ""
            }
            Text("$rango$sufijo")
            entrada.notas?.takeIf { it.isNotBlank() }?.let { Text(it) }
            entrada.precioPorHoraCustom?.let {
                Text(
                    stringResource(R.string.entrada_tarifa_turno, dineroATexto(it), simbolo),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (mostrarEditar) {
            IconButton(onClick = onEditar) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.accion_editar))
            }
        }
        IconButton(onClick = onEliminar) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.accion_eliminar))
        }
    }
}

private fun seSolapan(a: EntradaHoras, b: EntradaHoras): Boolean =
    a.inicio < b.fin && b.inicio < a.fin

@Composable
private fun NuevaEntradaForm(
    fecha: LocalDate,
    trabajoId: Long,
    simbolo: String,
    entradasExistentes: List<EntradaHoras>,
    entradaInicial: EntradaHoras? = null,
    onDirtyChange: (Boolean) -> Unit = {},
    onGuardar: (EntradaHoras) -> Unit,
    onCancelar: (() -> Unit)?,
) {
    var horaEntrada by remember(entradaInicial?.id, fecha, trabajoId) {
        mutableStateOf(entradaInicial?.horaEntrada ?: LocalTime.of(9, 0))
    }
    var horaSalida by remember(entradaInicial?.id, fecha, trabajoId) {
        mutableStateOf(entradaInicial?.horaSalida ?: LocalTime.of(17, 0))
    }
    var esDiaSiguiente by remember(entradaInicial?.id, fecha, trabajoId) {
        mutableStateOf(entradaInicial?.esDiaSiguiente ?: false)
    }
    var mostrarNotas by remember(entradaInicial?.id, fecha, trabajoId) {
        mutableStateOf(!entradaInicial?.notas.isNullOrBlank())
    }
    var notas by remember(entradaInicial?.id, fecha, trabajoId) {
        mutableStateOf(entradaInicial?.notas.orEmpty())
    }
    var usarTarifaCustom by remember(entradaInicial?.id, fecha, trabajoId) {
        mutableStateOf(entradaInicial?.precioPorHoraCustom != null)
    }
    var tarifaCustomTexto by remember(entradaInicial?.id, fecha, trabajoId) {
        mutableStateOf(entradaInicial?.precioPorHoraCustom?.let { dineroATexto(it) } ?: "")
    }
    var mostrarInfoNocturno by remember { mutableStateOf(false) }
    var mostrarErrores by remember { mutableStateOf(false) }

    val candidato = EntradaHoras(
        id = entradaInicial?.id ?: 0L,
        trabajoId = trabajoId,
        fecha = fecha,
        horaEntrada = horaEntrada,
        horaSalida = horaSalida,
        esDiaSiguiente = esDiaSiguiente,
    )
    val seSolapa = entradasExistentes
        .filterNot { it.id == entradaInicial?.id }
        .any { seSolapan(candidato, it) }
    val horarioInvalido = !esDiaSiguiente && horaSalida <= horaEntrada
    val tarifaCustomValida = !usarTarifaCustom || textoADinero(tarifaCustomTexto) != null
    val formularioValido = !horarioInvalido && !seSolapa && tarifaCustomValida
    val entradaParaGuardar = candidato.copy(
        notas = notas.takeIf { mostrarNotas && it.isNotBlank() },
        precioPorHoraCustom = if (usarTarifaCustom) textoADinero(tarifaCustomTexto) else null,
    )
    val hayCambiosEdicion = entradaInicial?.let { inicial ->
        entradaParaGuardar.horaEntrada != inicial.horaEntrada ||
            entradaParaGuardar.horaSalida != inicial.horaSalida ||
            entradaParaGuardar.esDiaSiguiente != inicial.esDiaSiguiente ||
            entradaParaGuardar.notas.orEmpty() != inicial.notas.orEmpty() ||
            entradaParaGuardar.precioPorHoraCustom != inicial.precioPorHoraCustom
    } ?: false

    LaunchedEffect(hayCambiosEdicion) {
        onDirtyChange(hayCambiosEdicion)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HoraPickerField(stringResource(R.string.entrada_hora_entrada), horaEntrada, onHoraSeleccionada = { horaEntrada = it })
        HoraPickerField(stringResource(R.string.entrada_hora_salida), horaSalida, onHoraSeleccionada = { horaSalida = it })
        if (horarioInvalido) {
            TextoError(stringResource(R.string.entrada_error_horario))
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EntradaCheckboxRow(
                modifier = Modifier.weight(1f),
                checked = esDiaSiguiente,
                onCheckedChange = { esDiaSiguiente = it },
                label = stringResource(R.string.entrada_turno_nocturno),
            )
            IconButton(onClick = { mostrarInfoNocturno = true }) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = stringResource(R.string.entrada_que_es_nocturno),
                )
            }
        }
        if (seSolapa) {
            TextoError(stringResource(R.string.entrada_error_solapa))
        }
        EntradaCheckboxRow(
            checked = usarTarifaCustom,
            onCheckedChange = { usarTarifaCustom = it },
            label = stringResource(R.string.entrada_tarifa_distinta),
        )
        if (usarTarifaCustom) {
            CampoDinero(
                label = stringResource(R.string.tarifa_campo_precio, simbolo),
                texto = tarifaCustomTexto,
                onTextoChange = { tarifaCustomTexto = it },
                mostrarErrores = mostrarErrores,
            )
        }
        if (mostrarNotas) {
            OutlinedTextField(
                value = notas,
                onValueChange = { notas = it },
                label = { Text(stringResource(R.string.entrada_campo_nota)) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            TextButton(onClick = { mostrarNotas = true }) { Text(stringResource(R.string.entrada_anadir_nota)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    if (formularioValido) {
                        onGuardar(entradaParaGuardar)
                        horaEntrada = LocalTime.of(9, 0)
                        horaSalida = LocalTime.of(17, 0)
                        esDiaSiguiente = false
                        mostrarNotas = false
                        notas = ""
                        usarTarifaCustom = false
                        tarifaCustomTexto = ""
                        mostrarErrores = false
                    } else {
                        mostrarErrores = true
                    }
                },
            ) { Text(stringResource(R.string.accion_guardar)) }
            if (onCancelar != null) {
                TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) }
            }
        }
    }

    if (mostrarInfoNocturno) {
        AlertDialog(
            onDismissRequest = { mostrarInfoNocturno = false },
            title = { Text(stringResource(R.string.entrada_turno_nocturno)) },
            text = { Text(stringResource(R.string.entrada_nocturno_explicacion)) },
            confirmButton = {
                TextButton(onClick = { mostrarInfoNocturno = false }) {
                    Text(stringResource(R.string.accion_entendido))
                }
            },
        )
    }
}
