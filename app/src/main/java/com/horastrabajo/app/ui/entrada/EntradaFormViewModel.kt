package com.horastrabajo.app.ui.entrada

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horastrabajo.app.data.repository.EntradaHorasRepository
import com.horastrabajo.app.domain.model.EntradaHoras
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class Parametros(val trabajoId: Long, val fecha: LocalDate)

/** Gestiona las entradas de horas de un único día (puede haber varias: dobles turnos). */
class EntradaFormViewModel(private val entradaHorasRepository: EntradaHorasRepository) : ViewModel() {

    private val parametros = MutableStateFlow<Parametros?>(null)

    fun cargar(trabajoId: Long, fecha: LocalDate) {
        parametros.value = Parametros(trabajoId, fecha)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val entradasDelDia: StateFlow<List<EntradaHoras>> = parametros
        .filterNotNull()
        .flatMapLatest { entradaHorasRepository.observePorFecha(it.trabajoId, it.fecha) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun guardar(entrada: EntradaHoras) {
        viewModelScope.launch { entradaHorasRepository.guardar(entrada) }
    }

    fun eliminar(entrada: EntradaHoras) {
        viewModelScope.launch { entradaHorasRepository.eliminar(entrada) }
    }

    fun restaurar(entrada: EntradaHoras) {
        viewModelScope.launch { entradaHorasRepository.restaurar(entrada) }
    }
}
