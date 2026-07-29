package com.horastrabajo.app.ui.mes

import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horastrabajo.app.R
import com.horastrabajo.app.domain.ResumenDia
import com.horastrabajo.app.domain.ResumenSemana
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EstrategiaConflicto
import com.horastrabajo.app.domain.model.PlantillaMes
import com.horastrabajo.app.domain.model.PlantillaSemana
import com.horastrabajo.app.domain.model.formatearHoras
import com.horastrabajo.app.ui.AppViewModelProvider
import com.horastrabajo.app.ui.components.CampoDinero
import com.horastrabajo.app.ui.components.CifraDestacada
import com.horastrabajo.app.ui.components.localeActual
import com.horastrabajo.app.ui.entrada.EntradaFormSheet
import com.horastrabajo.app.ui.entrada.EntradaFormViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private sealed interface MesListItem {
    data class SemanaHeader(val primerDia: LocalDate) : MesListItem
    data class Dia(val fecha: LocalDate) : MesListItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MesScreen(
    trabajoId: Long,
    anio: Int,
    mes: Int,
    onVerResumen: () -> Unit,
    onVolver: () -> Unit,
    mesViewModel: MesViewModel = viewModel(factory = AppViewModelProvider.Factory),
    entradaFormViewModel: EntradaFormViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    LaunchedEffect(trabajoId, anio, mes) { mesViewModel.cargar(trabajoId, anio, mes) }

    val resumen by mesViewModel.resumen.collectAsState()
    val yearMonth = YearMonth.of(anio, mes)
    val simbolo = resumen?.trabajo?.simboloMoneda ?: "€"

    var diaSeleccionado by remember { mutableStateOf<LocalDate?>(null) }
    var editandoTarifa by remember { mutableStateOf(false) }
    var agregandoDineroExtra by remember { mutableStateOf(false) }
    var dineroExtraEnEdicion by remember { mutableStateOf<DineroExtra?>(null) }
    var dineroExtraAEliminar by remember { mutableStateOf<DineroExtra?>(null) }
    var dineroExtraEditadoTieneCambios by remember { mutableStateOf(false) }
    var tieneCambiosEntrada by remember { mutableStateOf(false) }
    var mostrarDialogoDescartarEntrada by remember { mutableStateOf(false) }
    var confirmarDescartarDineroExtra by remember { mutableStateOf(false) }
    var cerrarDineroExtraTrasDescartar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val locale = localeActual()
    val textoDineroExtraEliminado = stringResource(R.string.mes_dinero_extra_eliminado)
    val textoDeshacer = stringResource(R.string.accion_deshacer)

    val plantillasSemana by mesViewModel.plantillasSemana.collectAsState()
    val plantillasMes by mesViewModel.plantillasMes.collectAsState()
    val conflictoState by mesViewModel.conflictoState.collectAsState()
    val semanasDelMes by mesViewModel.semanasDelMes.collectAsState()

    var mostrarMenuMes by remember { mutableStateOf(false) }
    var mostrarDialogoLimpiarMes by remember { mutableStateOf(false) }
    var mostrarPickerSemana by remember { mutableStateOf(false) }
    var mostrarPickerMes by remember { mutableStateOf(false) }
    var semanaParaPicker by remember { mutableStateOf<LocalDate?>(null) }

    val itemsList = remember(yearMonth, semanasDelMes) {
        val items = mutableListOf<MesListItem>()
        for (lunes in semanasDelMes) {
            items.add(MesListItem.SemanaHeader(lunes))
            for (i in 0..6) {
                val fecha = lunes.plusDays(i.toLong())
                if (fecha.monthValue == yearMonth.monthValue && fecha.year == yearMonth.year) {
                    items.add(MesListItem.Dia(fecha))
                }
            }
        }
        items
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            R.string.mes_titulo,
                            resumen?.trabajo?.nombre ?: "",
                            nombreMes(mes, locale),
                            anio,
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.accion_volver),
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { mostrarMenuMes = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.accion_mas_opciones))
                        }
                        DropdownMenu(expanded = mostrarMenuMes, onDismissRequest = { mostrarMenuMes = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.plantilla_aplicar_mes)) },
                                onClick = {
                                    mostrarMenuMes = false
                                    mostrarPickerMes = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mes_boton_resumen)) },
                                onClick = {
                                    mostrarMenuMes = false
                                    onVerResumen()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mes_limpiar)) },
                                onClick = {
                                    mostrarMenuMes = false
                                    mostrarDialogoLimpiarMes = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val resumenActual = resumen
        if (resumenActual == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(modifier = Modifier.padding(padding)) {
            Card(
                onClick = { editandoTarifa = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            stringResource(R.string.mes_tarifa_etiqueta),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val tarifaTexto = resumenActual.precioPorHora
                            ?.let { stringResource(R.string.mes_tarifa_valor, dineroATexto(it), simbolo) }
                            ?: stringResource(R.string.mes_tarifa_sin_fijar)
                        Text(tarifaTexto, style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text(
                            stringResource(R.string.accion_editar),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(itemsList, key = {
                    when (it) {
                        is MesListItem.SemanaHeader -> "hdr_${it.primerDia}"
                        is MesListItem.Dia -> "dia_${it.fecha}"
                    }
                }) { item ->
                    when (item) {
                        is MesListItem.SemanaHeader -> {
                            SemanaHeaderRow(
                                primerDia = item.primerDia,
                                locale = locale,
                                onAplicarPlantilla = {
                                    semanaParaPicker = item.primerDia
                                    mostrarPickerSemana = true
                                },
                            )
                            HorizontalDivider()
                        }
                        is MesListItem.Dia -> {
                            val diaResumen = resumenActual.dias.firstOrNull { it.fecha == item.fecha }
                            val semana = resumenActual.semanas.firstOrNull { it.ultimoDiaVisible == item.fecha }
                            DiaRow(
                                fecha = item.fecha,
                                diaResumen = diaResumen,
                                resumenSemana = semana,
                                simbolo = simbolo,
                                locale = locale,
                                onClick = { diaSeleccionado = item.fecha },
                            )
                            HorizontalDivider()
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(R.string.mes_dinero_extra_titulo))
                        TextButton(onClick = {
                            dineroExtraEnEdicion = null
                            agregandoDineroExtra = true
                        }) {
                            Text(stringResource(R.string.accion_anadir))
                        }
                    }
                    resumenActual.dineroExtra.forEach { extra ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(
                                    R.string.mes_dinero_extra_fila,
                                    extra.fecha.dayOfMonth,
                                    extra.fecha.monthValue,
                                    extra.descripcion,
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(extra.monto.formateado(simbolo))
                                if (dineroExtraEnEdicion?.id != extra.id) {
                                    IconButton(onClick = {
                                        dineroExtraEnEdicion = extra
                                        dineroExtraEditadoTieneCambios = false
                                        agregandoDineroExtra = false
                                    }) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = stringResource(R.string.mes_editar_dinero_extra),
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    dineroExtraAEliminar = extra
                                }) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = stringResource(R.string.mes_eliminar_dinero_extra),
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            CifraDestacada(
                                stringResource(
                                    R.string.mes_total_dinero,
                                    resumenActual.totalDinero.formateado(simbolo),
                                )
                            )
                            Text(
                                stringResource(R.string.mes_total_horas, formatearHoras(resumenActual.totalHoras)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        FilledTonalButton(onClick = onVerResumen) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(stringResource(R.string.mes_boton_resumen))
                        }
                    }
                }
            }
        }
    }

    diaSeleccionado?.let { fecha ->
        LaunchedEffect(fecha) { entradaFormViewModel.cargar(trabajoId, fecha) }
        val entradasDelDia by entradaFormViewModel.entradasDelDia.collectAsState()

        var latestConfirmValueChange by remember { mutableStateOf({ _: SheetValue -> true }) }
        val entradaSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { targetValue -> latestConfirmValueChange(targetValue) },
        )
        SideEffect {
            latestConfirmValueChange = { targetValue ->
                if (targetValue == SheetValue.Hidden && tieneCambiosEntrada) {
                    mostrarDialogoDescartarEntrada = true
                    false
                } else {
                    true
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { diaSeleccionado = null },
            sheetState = entradaSheetState,
        ) {
            EntradaFormSheet(
                fecha = fecha,
                trabajoId = trabajoId,
                simbolo = simbolo,
                entradasExistentes = entradasDelDia,
                onGuardar = { entradaFormViewModel.guardar(it) },
                onEliminar = { entradaFormViewModel.eliminar(it) },
                onRestaurar = { entradaFormViewModel.restaurar(it) },
                onCerrar = { diaSeleccionado = null },
                onDirtyChange = { tieneCambiosEntrada = it },
            )
        }
    }

    if (mostrarDialogoDescartarEntrada) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoDescartarEntrada = false },
            title = { Text(stringResource(R.string.cambios_sin_guardar_titulo)) },
            text = { Text(stringResource(R.string.entrada_descartar_cambios_mensaje)) },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoDescartarEntrada = false }) {
                    Text(stringResource(R.string.accion_seguir_editando))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarDialogoDescartarEntrada = false
                    diaSeleccionado = null
                }) {
                    Text(stringResource(R.string.accion_descartar_cambios))
                }
            },
        )
    }

    if (editandoTarifa) {
        TarifaDialog(
            precioActual = resumen?.precioPorHora,
            simbolo = simbolo,
            onConfirmar = {
                mesViewModel.fijarTarifaDelMes(it)
                editandoTarifa = false
            },
            onCancelar = { editandoTarifa = false },
        )
    }

    dineroExtraAEliminar?.let { extra ->
        AlertDialog(
            onDismissRequest = { dineroExtraAEliminar = null },
            title = { Text(stringResource(R.string.mes_eliminar_dinero_extra_titulo)) },
            text = {
                Text(
                    stringResource(
                        R.string.mes_eliminar_dinero_extra_mensaje,
                        extra.descripcion,
                        extra.monto.formateado(simbolo),
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dineroExtraAEliminar = null
                    mesViewModel.eliminarDineroExtra(extra)
                    if (dineroExtraEnEdicion?.id == extra.id) dineroExtraEnEdicion = null
                    scope.launch {
                        val resultado = snackbarHostState.showSnackbar(
                            message = textoDineroExtraEliminado,
                            actionLabel = textoDeshacer,
                            duration = SnackbarDuration.Short,
                        )
                        if (resultado == SnackbarResult.ActionPerformed) {
                            mesViewModel.restaurarDineroExtra(extra)
                        }
                    }
                }) { Text(stringResource(R.string.accion_eliminar)) }
            },
            dismissButton = {
                TextButton(onClick = { dineroExtraAEliminar = null }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
        )
    }

    fun cerrarFormularioDineroExtra() {
        agregandoDineroExtra = false
        dineroExtraEnEdicion = null
        dineroExtraEditadoTieneCambios = false
        confirmarDescartarDineroExtra = false
        cerrarDineroExtraTrasDescartar = false
    }
    fun solicitarCerrarFormularioDineroExtra(cerrarSheet: Boolean) {
        if (dineroExtraEnEdicion != null && dineroExtraEditadoTieneCambios) {
            cerrarDineroExtraTrasDescartar = cerrarSheet
            confirmarDescartarDineroExtra = true
        } else {
            cerrarFormularioDineroExtra()
        }
    }

    if (confirmarDescartarDineroExtra) {
        AlertDialog(
            onDismissRequest = { confirmarDescartarDineroExtra = false },
            title = { Text(stringResource(R.string.cambios_sin_guardar_titulo)) },
            text = { Text(stringResource(R.string.dinero_extra_descartar_cambios_mensaje)) },
            confirmButton = {
                TextButton(onClick = { confirmarDescartarDineroExtra = false }) {
                    Text(stringResource(R.string.accion_seguir_editando))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val cerrar = cerrarDineroExtraTrasDescartar
                    cerrarFormularioDineroExtra()
                    if (!cerrar) agregandoDineroExtra = false
                }) {
                    Text(stringResource(R.string.accion_descartar_cambios))
                }
            },
        )
    }

    if (mostrarPickerSemana && semanaParaPicker != null) {
        val primerDia = semanaParaPicker!!
        ModalBottomSheet(
            onDismissRequest = { mostrarPickerSemana = false; semanaParaPicker = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            PlantillaPickerSheet(
                titulo = stringResource(R.string.plantilla_elegir_semana),
                plantillas = plantillasSemana,
                onSeleccionar = { item ->
                    if (item is PlantillaSemana) {
                        mesViewModel.aplicarPlantillaSemana(item, primerDia)
                    }
                    mostrarPickerSemana = false
                    semanaParaPicker = null
                },
                onDismiss = { mostrarPickerSemana = false; semanaParaPicker = null },
            )
        }
    }

    if (mostrarPickerMes) {
        ModalBottomSheet(
            onDismissRequest = { mostrarPickerMes = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            PlantillaPickerSheet(
                titulo = stringResource(R.string.plantilla_elegir_mes),
                plantillas = plantillasMes,
                onSeleccionar = { item ->
                    if (item is PlantillaMes) {
                        mesViewModel.aplicarPlantillaMes(item, anio, mes)
                    }
                    mostrarPickerMes = false
                },
                onDismiss = { mostrarPickerMes = false },
            )
        }
    }

    if (mostrarDialogoLimpiarMes) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoLimpiarMes = false },
            title = { Text(stringResource(R.string.mes_limpiar_titulo)) },
            text = { Text(stringResource(R.string.mes_limpiar_mensaje)) },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoLimpiarMes = false
                    mesViewModel.limpiarMes()
                }) { Text(stringResource(R.string.accion_eliminar)) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoLimpiarMes = false }) {
                    Text(stringResource(R.string.accion_cancelar))
                }
            },
        )
    }

    conflictoState?.let { estado ->
        ConflictoDialog(
            fechasConflicto = estado.fechasConflicto,
            onReemplazar = { mesViewModel.resolverConflicto(EstrategiaConflicto.REEMPLAZAR_TODO) },
            onSoloDiasVacios = { mesViewModel.resolverConflicto(EstrategiaConflicto.SOLO_DIAS_VACIOS) },
            onCancelar = { mesViewModel.resolverConflicto(EstrategiaConflicto.CANCELAR) },
        )
    }

    if (agregandoDineroExtra || dineroExtraEnEdicion != null) {
        val extraEnEdicion = dineroExtraEnEdicion

        var latestDineroExtraConfirmChange by remember { mutableStateOf({ _: SheetValue -> true }) }
        val dineroExtraSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { targetValue -> latestDineroExtraConfirmChange(targetValue) },
        )
        SideEffect {
            latestDineroExtraConfirmChange = { targetValue ->
                if (targetValue == SheetValue.Hidden && dineroExtraEnEdicion != null && dineroExtraEditadoTieneCambios) {
                    cerrarDineroExtraTrasDescartar = true
                    confirmarDescartarDineroExtra = true
                    false
                } else {
                    true
                }
            }
        }
        ModalBottomSheet(
            onDismissRequest = { cerrarFormularioDineroExtra() },
            sheetState = dineroExtraSheetState,
        ) {
            Box {
                BackHandler(enabled = dineroExtraEnEdicion != null && dineroExtraEditadoTieneCambios) {
                    cerrarDineroExtraTrasDescartar = true
                    confirmarDescartarDineroExtra = true
                }
                DineroExtraFormContent(
                    dineroExtraInicial = extraEnEdicion,
                    fechaPorDefecto = extraEnEdicion?.fecha
                        ?: LocalDate.now().let { if (YearMonth.from(it) == yearMonth) it else yearMonth.atDay(1) },
                    onDirtyChange = { dineroExtraEditadoTieneCambios = it },
                    onConfirmar = { fecha, monto, descripcion ->
                        mesViewModel.guardarDineroExtra(
                            DineroExtra(
                                id = extraEnEdicion?.id ?: 0L,
                                trabajoId = trabajoId,
                                fecha = fecha,
                                monto = monto,
                                descripcion = descripcion,
                            )
                        )
                        cerrarFormularioDineroExtra()
                    },
                    onCancelar = { solicitarCerrarFormularioDineroExtra(cerrarSheet = false) },
                )
            }
        }
    }
}

@Composable
private fun SemanaHeaderRow(
    primerDia: LocalDate,
    locale: Locale,
    onAplicarPlantilla: () -> Unit,
) {
    val ultimoDia = primerDia.plusDays(6)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(
                R.string.mes_semana_encabezado,
                "${primerDia.dayOfMonth} ${primerDia.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)}",
                "${ultimoDia.dayOfMonth} ${ultimoDia.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)}",
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onAplicarPlantilla) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = stringResource(R.string.plantilla_aplicar_semana),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DiaRow(
    fecha: LocalDate,
    diaResumen: ResumenDia?,
    resumenSemana: ResumenSemana?,
    simbolo: String,
    locale: Locale,
    onClick: () -> Unit,
) {
    val esHoy = fecha == LocalDate.now()
    val esFinDeSemana = fecha.dayOfWeek == DayOfWeek.SATURDAY || fecha.dayOfWeek == DayOfWeek.SUNDAY
    val nombreDia = fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    val colorContenido = if (esHoy) MaterialTheme.colorScheme.onPrimary else Color.Unspecified
    ListItem(
        headlineContent = {
            Text(
                stringResource(R.string.mes_dia_encabezado, fecha.dayOfMonth, nombreDia),
                fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal,
                color = colorContenido,
            )
        },
        supportingContent = resumenSemana?.let { semana ->
            {
                val etiqueta = stringResource(
                    if (semana.completa) R.string.mes_total_semana else R.string.mes_total_semana_parcial
                )
                Text(
                    stringResource(
                        R.string.mes_semana_resumen,
                        etiqueta,
                        formatearHoras(semana.horas),
                        semana.dinero.formateado(simbolo),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorContenido,
                )
            }
        },
        trailingContent = {
            if (diaResumen != null && diaResumen.horas > 0) {
                val tieneTurnoNocturno = diaResumen.entradas.any { it.esDiaSiguiente }
                val tieneTarifaCustom = diaResumen.entradas.any { it.precioPorHoraCustom != null }
                val numeroTurnos = diaResumen.entradas.size
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tieneTurnoNocturno) {
                        MarcadorDia(
                            simbolo = "🌙",
                            descripcion = stringResource(R.string.marcador_nocturno),
                            color = colorContenido,
                        )
                    }
                    if (tieneTarifaCustom) {
                        MarcadorDia(
                            simbolo = "✱",
                            descripcion = stringResource(R.string.marcador_tarifa_distinta),
                            color = colorContenido,
                        )
                    }
                    if (numeroTurnos > 1) {
                        MarcadorDia(
                            simbolo = "×$numeroTurnos",
                            descripcion = stringResource(R.string.marcador_varios_turnos, numeroTurnos),
                            color = colorContenido,
                        )
                    }
                    Text(
                        stringResource(
                            R.string.mes_dia_valor,
                            formatearHoras(diaResumen.horas),
                            diaResumen.dinero.formateado(simbolo),
                        ),
                        color = colorContenido,
                    )
                }
            } else {
                Text("—", color = colorContenido)
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = when {
                esHoy -> MaterialTheme.colorScheme.primary
                esFinDeSemana -> MaterialTheme.colorScheme.surfaceVariant
                else -> Color.Transparent
            },
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

/**
 * Marcador puramente informativo (sin interacción propia) para señalar algo del día:
 * turno nocturno, tarifa distinta o más de un turno. Cada condición usa su propio
 * símbolo; [descripcion] sustituye la lectura literal del símbolo para TalkBack.
 */
@Composable
private fun MarcadorDia(simbolo: String, descripcion: String, color: Color = Color.Unspecified) {
    Text(
        text = simbolo,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontSize = 11.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .widthIn(min = 18.dp)
            .padding(end = 4.dp)
            .clearAndSetSemantics { contentDescription = descripcion },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarifaDialog(precioActual: Dinero?, simbolo: String, onConfirmar: (Dinero) -> Unit, onCancelar: () -> Unit) {
    var texto by remember { mutableStateOf(precioActual?.let { dineroATexto(it) } ?: "") }
    var mostrarErrores by remember { mutableStateOf(false) }
    val dineroValido = textoADinero(texto)
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(stringResource(R.string.tarifa_dialogo_titulo)) },
        text = {
            CampoDinero(
                label = stringResource(R.string.tarifa_campo_precio, simbolo),
                texto = texto,
                onTextoChange = { texto = it },
                mostrarErrores = mostrarErrores,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (dineroValido != null) onConfirmar(dineroValido) else mostrarErrores = true
            }) { Text(stringResource(R.string.accion_guardar)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) }
        },
    )
}

/**
 * Contenido de "Dinero extra" alojado en un [ModalBottomSheet] — 3 campos, mismo
 * criterio que [TrabajoFormSheetContent][com.horastrabajo.app.ui.trabajos] y
 * [com.horastrabajo.app.ui.entrada.EntradaFormSheet]: ya no en AlertDialog.
 */
@Composable
private fun DineroExtraFormContent(
    dineroExtraInicial: DineroExtra? = null,
    fechaPorDefecto: LocalDate,
    onDirtyChange: (Boolean) -> Unit = {},
    onConfirmar: (LocalDate, Dinero, String) -> Unit,
    onCancelar: () -> Unit,
) {
    var fecha by remember(dineroExtraInicial?.id, fechaPorDefecto) { mutableStateOf(fechaPorDefecto) }
    var texto by remember(dineroExtraInicial?.id) {
        mutableStateOf(dineroExtraInicial?.monto?.let { dineroATexto(it) } ?: "")
    }
    var descripcion by remember(dineroExtraInicial?.id) {
        mutableStateOf(dineroExtraInicial?.descripcion.orEmpty())
    }
    var mostrarErrores by remember { mutableStateOf(false) }
    val dineroValido = textoADinero(texto)
    val hayCambiosEdicion = dineroExtraInicial?.let { inicial ->
        fecha != inicial.fecha ||
            dineroValido != inicial.monto ||
            descripcion.trim() != inicial.descripcion
    } ?: false

    LaunchedEffect(hayCambiosEdicion) {
        onDirtyChange(hayCambiosEdicion)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.dinero_extra_titulo), style = MaterialTheme.typography.titleLarge)
        FechaPickerField(stringResource(R.string.campo_fecha), fecha) { fecha = it }
        CampoDinero(
            label = stringResource(R.string.dinero_extra_campo_cantidad),
            texto = texto,
            onTextoChange = { texto = it },
            mostrarErrores = mostrarErrores,
        )
        OutlinedTextField(
            value = descripcion,
            onValueChange = { descripcion = it },
            label = { Text(stringResource(R.string.dinero_extra_campo_descripcion)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                if (dineroValido != null) onConfirmar(fecha, dineroValido, descripcion.trim()) else mostrarErrores = true
            }) { Text(stringResource(R.string.accion_guardar)) }
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) }
        }
    }
}

private fun nombreMes(mes: Int, locale: Locale): String =
    java.time.Month.of(mes).getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.uppercase(locale) }
