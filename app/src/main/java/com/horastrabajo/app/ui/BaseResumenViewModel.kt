package com.horastrabajo.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horastrabajo.app.data.repository.DineroExtraRepository
import com.horastrabajo.app.data.repository.EntradaHorasRepository
import com.horastrabajo.app.data.repository.TarifaMensualRepository
import com.horastrabajo.app.data.repository.TrabajoRepository
import com.horastrabajo.app.domain.ResumenMensual
import com.horastrabajo.app.domain.ResumenMensualCalculator
import com.horastrabajo.app.domain.model.Dinero
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Clase base que encapsula la lógica compartida entre [MesViewModel][com.horastrabajo.app.ui.mes.MesViewModel]
 * y [ResumenViewModel][com.horastrabajo.app.ui.resumen.ResumenViewModel]:
 * - [parametros] (trabajoId, anio, mes)
 * - [tarifaVigente]
 * - [cargar] y hook [onParametrosCambiados]
 * - [resumen] flow combinado
 */
abstract class BaseResumenViewModel(
    protected val trabajoRepository: TrabajoRepository,
    protected val entradaHorasRepository: EntradaHorasRepository,
    protected val tarifaMensualRepository: TarifaMensualRepository,
    protected val dineroExtraRepository: DineroExtraRepository,
) : ViewModel() {

    protected data class Parametros(val trabajoId: Long, val anio: Int, val mes: Int)

    protected val parametros = MutableStateFlow<Parametros?>(null)
    protected val tarifaVigente = MutableStateFlow<Dinero?>(null)

    open fun cargar(trabajoId: Long, anio: Int, mes: Int) {
        val actuales = parametros.value
        if (actuales?.trabajoId == trabajoId && actuales.anio == anio && actuales.mes == mes) return
        parametros.value = Parametros(trabajoId, anio, mes)
        refrescarTarifa()
        onParametrosCambiados(trabajoId, anio, mes)
    }

    /** Recarga la tarifa vigente desde el repositorio. */
    protected fun refrescarTarifa() {
        val p = parametros.value ?: return
        viewModelScope.launch {
            tarifaVigente.value = tarifaMensualRepository.obtenerTarifaVigente(p.trabajoId, p.anio, p.mes)?.precioPorHora
        }
    }

    /**
     * Hook para que las subclases reaccionen al cambio de parámetros.
     * Se llama desde [cargar] solo cuando los parámetros realmente cambian.
     */
    protected open fun onParametrosCambiados(trabajoId: Long, anio: Int, mes: Int) {}

    @OptIn(ExperimentalCoroutinesApi::class)
    val resumen: StateFlow<ResumenMensual?> = combine(
        parametros.filterNotNull(),
        parametros.filterNotNull().flatMapLatest { trabajoRepository.observeById(it.trabajoId) },
        parametros.filterNotNull().flatMapLatest { entradaHorasRepository.observePorMes(it.trabajoId, it.anio, it.mes) },
        parametros.filterNotNull().flatMapLatest { dineroExtraRepository.observePorMes(it.trabajoId, it.anio, it.mes) },
        tarifaVigente,
    ) { p, trabajo, entradas, dineroExtra, tarifa ->
        if (trabajo == null) null else ResumenMensualCalculator.calcular(
            trabajo = trabajo,
            anio = p.anio,
            mes = p.mes,
            precioPorHora = tarifa,
            entradas = entradas,
            dineroExtra = dineroExtra,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
