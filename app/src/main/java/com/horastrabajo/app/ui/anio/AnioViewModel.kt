package com.horastrabajo.app.ui.anio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horastrabajo.app.data.repository.DineroExtraRepository
import com.horastrabajo.app.data.repository.EntradaHorasRepository
import com.horastrabajo.app.data.repository.TarifaMensualRepository
import com.horastrabajo.app.data.repository.TrabajoRepository
import com.horastrabajo.app.domain.ResumenAnual
import com.horastrabajo.app.domain.ResumenAnualCalculator
import com.horastrabajo.app.domain.model.Trabajo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

private data class Parametros(val trabajoId: Long, val anio: Int)

class AnioViewModel(
    private val trabajoRepository: TrabajoRepository,
    private val entradaHorasRepository: EntradaHorasRepository,
    private val dineroExtraRepository: DineroExtraRepository,
    private val tarifaMensualRepository: TarifaMensualRepository,
) : ViewModel() {

    private val parametros = MutableStateFlow<Parametros?>(null)

    fun cargar(trabajoId: Long, anio: Int) {
        val actuales = parametros.value
        if (actuales?.trabajoId == trabajoId && actuales.anio == anio) return
        parametros.value = Parametros(trabajoId, anio)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val trabajo: StateFlow<Trabajo?> = parametros.filterNotNull()
        .flatMapLatest { trabajoRepository.observeById(it.trabajoId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val resumenAnual: StateFlow<ResumenAnual?> = combine(
        parametros.filterNotNull(),
        parametros.filterNotNull().flatMapLatest { entradaHorasRepository.observePorAnio(it.trabajoId, it.anio) },
        parametros.filterNotNull().flatMapLatest { dineroExtraRepository.observePorAnio(it.trabajoId, it.anio) },
        parametros.filterNotNull().flatMapLatest { tarifaMensualRepository.observeTodasDelTrabajo(it.trabajoId) },
    ) { p, entradas, dineroExtra, tarifas ->
        ResumenAnualCalculator.calcular(
            anio = p.anio,
            entradas = entradas,
            dineroExtra = dineroExtra,
            tarifas = tarifas,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
