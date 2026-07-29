package com.horastrabajo.app.ui.trabajos

import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horastrabajo.app.R
import com.horastrabajo.app.domain.model.Trabajo
import com.horastrabajo.app.domain.model.formatearHoras
import com.horastrabajo.app.ui.AppViewModelProvider
import com.horastrabajo.app.ui.components.CampoObligatorio
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrabajosScreen(
    onTrabajoSeleccionado: (Long) -> Unit,
    onAbrirAjustes: () -> Unit,
    viewModel: TrabajosViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val trabajos by viewModel.trabajos.collectAsState()
    var mostrarDialogoNuevo by remember { mutableStateOf(false) }
    var trabajoEditando by remember { mutableStateOf<Trabajo?>(null) }
    var trabajoAEliminar by remember { mutableStateOf<Trabajo?>(null) }
    var trabajoNuevoTieneCambios by remember { mutableStateOf(false) }
    var trabajoEditandoTieneCambios by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trabajos_titulo)) },
                actions = {
                    IconButton(onClick = onAbrirAjustes) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.ajustes_titulo))
                    }
                },
            )
        },
        floatingActionButton = {
            if (trabajos.isEmpty()) {
                ExtendedFloatingActionButton(onClick = { mostrarDialogoNuevo = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.trabajo_nuevo), modifier = Modifier.padding(start = 8.dp))
                }
            } else {
                FloatingActionButton(onClick = { mostrarDialogoNuevo = true }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.trabajo_anadir))
                }
            }
        },
    ) { padding ->
        if (trabajos.isEmpty()) {
            EstadoVacioTrabajos(modifier = Modifier.padding(padding), onCrear = { mostrarDialogoNuevo = true })
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(trabajos, key = { it.trabajo.id }) { trabajoCard ->
                    TrabajoCard(
                        trabajoCard = trabajoCard,
                        onAbrir = { onTrabajoSeleccionado(trabajoCard.trabajo.id) },
                        onEditar = { trabajoEditando = trabajoCard.trabajo },
                        onEliminar = { trabajoAEliminar = trabajoCard.trabajo },
                    )
                }
            }
        }
    }

    if (mostrarDialogoNuevo) {
        var latestNuevoConfirmValueChange by remember { mutableStateOf({ _: SheetValue -> true }) }
        val nuevoSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { targetValue -> latestNuevoConfirmValueChange(targetValue) },
        )
        SideEffect {
            latestNuevoConfirmValueChange = { targetValue ->
                targetValue != SheetValue.Hidden || !trabajoNuevoTieneCambios
            }
        }
        ModalBottomSheet(
            onDismissRequest = { mostrarDialogoNuevo = false },
            sheetState = nuevoSheetState,
        ) {
            TrabajoFormSheetContent(
                titulo = stringResource(R.string.trabajo_nuevo),
                nombreInicial = "",
                nombreUsuarioInicial = "",
                simboloInicial = "€",
                fotoUriInicial = null,
                esEdicion = false,
                onConfirmar = { nombre, nombreUsuario, simbolo, fotoUri ->
                    viewModel.crearTrabajo(nombre, nombreUsuario, simbolo, fotoUri)
                    mostrarDialogoNuevo = false
                },
                onCancelar = { mostrarDialogoNuevo = false },
                onDirtyChange = { trabajoNuevoTieneCambios = it },
            )
        }
    }

    trabajoEditando?.let { trabajo ->
        var latestEditandoConfirmValueChange by remember { mutableStateOf({ _: SheetValue -> true }) }
        val editandoSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { targetValue -> latestEditandoConfirmValueChange(targetValue) },
        )
        SideEffect {
            latestEditandoConfirmValueChange = { targetValue ->
                targetValue != SheetValue.Hidden || !trabajoEditandoTieneCambios
            }
        }
        ModalBottomSheet(
            onDismissRequest = { trabajoEditando = null },
            sheetState = editandoSheetState,
        ) {
            TrabajoFormSheetContent(
                titulo = stringResource(R.string.trabajo_editar),
                nombreInicial = trabajo.nombre,
                nombreUsuarioInicial = trabajo.nombreUsuario,
                simboloInicial = trabajo.simboloMoneda,
                fotoUriInicial = trabajo.fotoUri,
                esEdicion = true,
                onConfirmar = { nombre, nombreUsuario, simbolo, fotoUri ->
                    viewModel.actualizarTrabajo(trabajo, nombre, nombreUsuario, simbolo, fotoUri)
                    trabajoEditando = null
                },
                onCancelar = { trabajoEditando = null },
                onDirtyChange = { trabajoEditandoTieneCambios = it },
            )
        }
    }

    trabajoAEliminar?.let { trabajo ->
        AlertDialog(
            onDismissRequest = { trabajoAEliminar = null },
            title = { Text(stringResource(R.string.trabajo_eliminar_titulo)) },
            text = { Text(stringResource(R.string.trabajo_eliminar_mensaje, trabajo.nombre)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarTrabajo(trabajo)
                    trabajoAEliminar = null
                }) { Text(stringResource(R.string.accion_eliminar)) }
            },
            dismissButton = {
                TextButton(onClick = { trabajoAEliminar = null }) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}

@Composable
private fun EstadoVacioTrabajos(onCrear: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            IconButton(
                onClick = onCrear,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.trabajo_nuevo),
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                stringResource(R.string.trabajos_vacio_titulo),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.trabajos_vacio_mensaje),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onCrear) { Text(stringResource(R.string.trabajos_vacio_boton)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrabajoCard(
    trabajoCard: TrabajoCardUi,
    onAbrir: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
) {
    var menuAbierto by remember { mutableStateOf(false) }
    val trabajo = trabajoCard.trabajo

    Card(
        onClick = onAbrir,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                TrabajoFoto(
                    fotoUri = trabajo.fotoUri,
                    nombre = trabajo.nombre,
                    modifier = Modifier.size(92.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(trabajo.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        trabajo.nombreUsuario,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (trabajoCard.ultimoRegistro != null) {
                        Text(
                            stringResource(
                                R.string.trabajo_ultimo_registro,
                                formatearFechaCorta(trabajoCard.ultimoRegistro),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menuAbierto = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.trabajo_mas_opciones, trabajo.nombre),
                        )
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.accion_editar)) },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = {
                                menuAbierto = false
                                onEditar()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.accion_eliminar)) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = {
                                menuAbierto = false
                                onEliminar()
                            },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricaTrabajoRow(
                    etiqueta = stringResource(R.string.trabajo_total_horas),
                    valor = formatearHoras(trabajoCard.totalHoras),
                )
                MetricaTrabajoRow(
                    etiqueta = stringResource(R.string.trabajo_total_dinero),
                    valor = trabajoCard.totalDinero.formateado(trabajo.simboloMoneda),
                )
                MetricaTrabajoRow(
                    etiqueta = stringResource(R.string.trabajo_tiempo_real),
                    valor = formatearTiempoReal(trabajoCard.totalHoras),
                )
                MetricaTrabajoRow(
                    etiqueta = stringResource(R.string.trabajo_meses_actividad),
                    valor = trabajoCard.mesesConActividad.toString(),
                )
                MetricaTrabajoRow(
                    etiqueta = stringResource(R.string.trabajo_primer_registro),
                    valor = trabajoCard.primerRegistro?.let { formatearFechaCorta(it) }
                        ?: stringResource(R.string.trabajo_sin_registros),
                )
            }
        }
    }
}

@Composable
private fun TrabajoFoto(fotoUri: String?, nombre: String, modifier: Modifier = Modifier) {
    var mostrarIniciales by remember(fotoUri) { mutableStateOf(fotoUri == null) }

    if (fotoUri != null && !mostrarIniciales) {
        AndroidView(
            factory = { context ->
                ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }
            },
            update = { imageView ->
                try {
                    val uri = Uri.parse(fotoUri)
                    imageView.setImageURI(uri)
                    if (imageView.drawable == null) mostrarIniciales = true
                } catch (_: SecurityException) {
                    mostrarIniciales = true
                }
            },
            modifier = modifier.clip(MaterialTheme.shapes.medium),
        )
    } else {
        Box(
            modifier = modifier
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                inicialesTrabajo(nombre),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MetricaTrabajoRow(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private data class Moneda(val simbolo: String, @StringRes val nombreRes: Int)

private val MONEDAS = listOf(
    Moneda("€", R.string.moneda_euro),
    Moneda("$", R.string.moneda_dolar),
    Moneda("£", R.string.moneda_libra),
    Moneda("¥", R.string.moneda_yen),
    Moneda("Fr", R.string.moneda_franco),
)

private val FORMATO_FECHA_TRABAJO: DateTimeFormatter = DateTimeFormatter.ofPattern("d/M/yyyy")
private fun formatearFechaCorta(fecha: LocalDate): String = fecha.format(FORMATO_FECHA_TRABAJO)

private fun formatearTiempoReal(totalHoras: Double): String {
    if (totalHoras <= 0.0) return "0h"

    val horasTotales = kotlin.math.round(totalHoras).toLong()
    val horasPorDia = 24L
    val horasPorMes = 30L * horasPorDia
    val horasPorAnio = 365L * horasPorDia

    var restantes = horasTotales
    val anios = restantes / horasPorAnio
    restantes %= horasPorAnio
    val meses = restantes / horasPorMes
    restantes %= horasPorMes
    val dias = restantes / horasPorDia
    val horas = restantes % horasPorDia

    return buildList {
        if (anios > 0) add("${anios}a")
        if (meses > 0) add("${meses}m")
        if (dias > 0) add("${dias}d")
        if (horas > 0 || isEmpty()) add("${horas}h")
    }.joinToString(" · ")
}

private fun inicialesTrabajo(nombre: String): String {
    val partes = nombre.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return partes
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .joinToString("")
        .ifBlank { "HT" }
}

/**
 * Contenido del formulario de trabajo (nuevo o editar) alojado en un [ModalBottomSheet] —
 * mismo criterio que [com.horastrabajo.app.ui.entrada.EntradaFormSheet] y
 * [com.horastrabajo.app.ui.mes.MesScreen]: formularios de 3 o más campos van en bottom
 * sheet, no en AlertDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrabajoFormSheetContent(
    titulo: String,
    nombreInicial: String,
    nombreUsuarioInicial: String,
    simboloInicial: String,
    fotoUriInicial: String?,
    esEdicion: Boolean,
    onConfirmar: (String, String, String, String?) -> Unit,
    onCancelar: () -> Unit,
    onDirtyChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf(nombreInicial) }
    var nombreUsuario by remember { mutableStateOf(nombreUsuarioInicial) }
    var simbolo by remember {
        mutableStateOf(MONEDAS.firstOrNull { it.simbolo == simboloInicial }?.simbolo ?: MONEDAS.first().simbolo)
    }
    var fotoUri by remember { mutableStateOf(fotoUriInicial) }
    var monedaExpandida by remember { mutableStateOf(false) }
    var mostrarErrores by remember { mutableStateOf(false) }
    var confirmarDescartarCambios by remember { mutableStateOf(false) }
    val esValido = nombre.isNotBlank() && nombreUsuario.isNotBlank()
    val hayCambios = esEdicion && (nombre != nombreInicial || nombreUsuario != nombreUsuarioInicial ||
        simbolo != simboloInicial || fotoUri != fotoUriInicial)

    LaunchedEffect(hayCambios) {
        onDirtyChange(hayCambios)
    }

    BackHandler(enabled = hayCambios) {
        confirmarDescartarCambios = true
    }
    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // Algunos proveedores no conceden permisos persistentes; la URI sigue siendo usable en esta sesión.
            }
            fotoUri = uri.toString()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.titleLarge)
        CampoObligatorio(
            label = stringResource(R.string.trabajo_campo_nombre),
            texto = nombre,
            onTextoChange = { nombre = it },
            mostrarErrores = mostrarErrores,
        )
        CampoObligatorio(
            label = stringResource(R.string.trabajo_campo_tu_nombre),
            texto = nombreUsuario,
            onTextoChange = { nombreUsuario = it },
            mostrarErrores = mostrarErrores,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrabajoFoto(
                fotoUri = fotoUri,
                nombre = nombre.ifBlank { stringResource(R.string.trabajo_campo_nombre) },
                modifier = Modifier.size(76.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.trabajo_foto_opcional),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { selectorFoto.launch(arrayOf("image/*")) }) {
                        Text(
                            stringResource(
                                if (fotoUri == null) R.string.trabajo_elegir_foto else R.string.trabajo_cambiar_foto
                            )
                        )
                    }
                    if (fotoUri != null) {
                        TextButton(onClick = { fotoUri = null }) {
                            Text(stringResource(R.string.trabajo_quitar_foto))
                        }
                    }
                }
            }
        }
        val monedaSeleccionada = MONEDAS.first { it.simbolo == simbolo }
        ExposedDropdownMenuBox(
            expanded = monedaExpandida,
            onExpandedChange = { monedaExpandida = it },
        ) {
            OutlinedTextField(
                value = stringResource(
                    R.string.moneda_opcion,
                    stringResource(monedaSeleccionada.nombreRes),
                    monedaSeleccionada.simbolo,
                ),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.trabajo_campo_moneda)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monedaExpandida) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = monedaExpandida, onDismissRequest = { monedaExpandida = false }) {
                MONEDAS.forEach { moneda ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    R.string.moneda_opcion,
                                    stringResource(moneda.nombreRes),
                                    moneda.simbolo,
                                )
                            )
                        },
                        onClick = {
                            simbolo = moneda.simbolo
                            monedaExpandida = false
                        },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    if (esValido) onConfirmar(nombre, nombreUsuario, simbolo, fotoUri) else mostrarErrores = true
                },
            ) { Text(stringResource(R.string.accion_guardar)) }
            TextButton(onClick = {
                if (hayCambios) confirmarDescartarCambios = true else onCancelar()
            }) { Text(stringResource(R.string.accion_cancelar)) }
        }
    }

    if (confirmarDescartarCambios) {
        AlertDialog(
            onDismissRequest = { confirmarDescartarCambios = false },
            title = { Text(stringResource(R.string.cambios_sin_guardar_titulo)) },
            text = { Text(stringResource(R.string.trabajo_descartar_cambios_mensaje)) },
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
