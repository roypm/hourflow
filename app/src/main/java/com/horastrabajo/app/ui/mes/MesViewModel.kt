package com.horastrabajo.app.ui.mes

import androidx.lifecycle.viewModelScope
import com.horastrabajo.app.data.repository.DineroExtraRepository
import com.horastrabajo.app.data.repository.EntradaHorasRepository
import com.horastrabajo.app.data.repository.PlantillaRepository
import com.horastrabajo.app.data.repository.TarifaMensualRepository
import com.horastrabajo.app.data.repository.TrabajoRepository
import com.horastrabajo.app.domain.GeneradorEntradas
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EstrategiaConflicto
import com.horastrabajo.app.domain.model.PlantillaMes
import com.horastrabajo.app.domain.model.PlantillaSemana
import com.horastrabajo.app.ui.BaseResumenViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class ConflictoState(
    val entradasAGenerar: List<com.horastrabajo.app.domain.model.EntradaHoras>,
    val fechasConflicto: List<LocalDate>
)

class MesViewModel(
    trabajoRepository: TrabajoRepository,
    entradaHorasRepository: EntradaHorasRepository,
    tarifaMensualRepository: TarifaMensualRepository,
    dineroExtraRepository: DineroExtraRepository,
    private val plantillaRepository: PlantillaRepository,
) : BaseResumenViewModel(
    trabajoRepository = trabajoRepository,
    entradaHorasRepository = entradaHorasRepository,
    tarifaMensualRepository = tarifaMensualRepository,
    dineroExtraRepository = dineroExtraRepository,
) {

    override fun onParametrosCambiados(trabajoId: Long, anio: Int, mes: Int) {
        _trabajoId.value = trabajoId
        refrescarSemanas(anio, mes)
    }

    fun fijarTarifaDelMes(precioPorHora: Dinero) {
        val p = parametros.value ?: return
        viewModelScope.launch {
            tarifaMensualRepository.fijarTarifaDelMes(p.trabajoId, p.anio, p.mes, precioPorHora)
            refrescarTarifa()
        }
    }

    fun agregarDineroExtra(dineroExtra: DineroExtra) {
        guardarDineroExtra(dineroExtra)
    }

    fun guardarDineroExtra(dineroExtra: DineroExtra) {
        viewModelScope.launch { dineroExtraRepository.guardar(dineroExtra) }
    }

    fun eliminarDineroExtra(dineroExtra: DineroExtra) {
        viewModelScope.launch { dineroExtraRepository.eliminar(dineroExtra) }
    }

    fun restaurarDineroExtra(dineroExtra: DineroExtra) {
        viewModelScope.launch { dineroExtraRepository.restaurar(dineroExtra) }
    }

    private val _conflictoState = MutableStateFlow<ConflictoState?>(null)
    val conflictoState: StateFlow<ConflictoState?> = _conflictoState.asStateFlow()

    private val _semanasDelMes = MutableStateFlow<List<LocalDate>>(emptyList())
    val semanasDelMes: StateFlow<List<LocalDate>> = _semanasDelMes.asStateFlow()

    private fun refrescarSemanas(anio: Int, mes: Int) {
        try {
            val primerDia = YearMonth.of(anio, mes).atDay(1)
            _semanasDelMes.value = GeneradorEntradas.obtenerSemanasDelMes(primerDia)
        } catch (_: Exception) {
            _semanasDelMes.value = emptyList()
        }
    }

    private val _trabajoId = MutableStateFlow(0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val plantillasSemana: StateFlow<List<PlantillaSemana>> =
        _trabajoId.flatMapLatest { plantillaRepository.getPlantillasSemana(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val plantillasMes: StateFlow<List<PlantillaMes>> =
        _trabajoId.flatMapLatest { plantillaRepository.getPlantillasMes(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun aplicarPlantillaSemana(
        plantilla: PlantillaSemana,
        primerDiaSemana: LocalDate
    ) = viewModelScope.launch {
        val p = parametros.value ?: return@launch
        val aGenerar = GeneradorEntradas.generarSemana(plantilla, primerDiaSemana, p.trabajoId)
        val existentes = entradaHorasRepository.getEntradasSemana(primerDiaSemana, p.trabajoId)
        val conflictos = GeneradorEntradas.detectarConflictos(aGenerar, existentes)

        if (conflictos.isEmpty()) {
            entradaHorasRepository.guardarVarias(aGenerar)
        } else {
            _conflictoState.value = ConflictoState(aGenerar, conflictos)
        }
    }

    fun aplicarPlantillaMes(
        plantilla: PlantillaMes,
        anyo: Int,
        mes: Int
    ) = viewModelScope.launch {
        val p = parametros.value ?: return@launch
        val aGenerar = GeneradorEntradas.generarMes(plantilla, anyo, mes, p.trabajoId)
        val existentes = entradaHorasRepository.getEntradasMes(anyo, mes, p.trabajoId)
        val conflictos = GeneradorEntradas.detectarConflictos(aGenerar, existentes)

        if (conflictos.isEmpty()) {
            entradaHorasRepository.guardarVarias(aGenerar)
        } else {
            _conflictoState.value = ConflictoState(aGenerar, conflictos)
        }
    }

    fun limpiarMes() {
        val p = parametros.value ?: return
        viewModelScope.launch {
            val yearMonth = YearMonth.of(p.anio, p.mes)
            entradaHorasRepository.deleteEntradasEnMes(p.trabajoId, p.anio, p.mes)
            dineroExtraRepository.deletePorMes(p.trabajoId, p.anio, p.mes)
            tarifaMensualRepository.eliminarTarifaDelMes(p.trabajoId, p.anio, p.mes)
            refrescarTarifa()
        }
    }

    fun resolverConflicto(estrategia: EstrategiaConflicto) = viewModelScope.launch {
        val estado = _conflictoState.value ?: return@launch
        val p = parametros.value ?: return@launch
        when (estrategia) {
            EstrategiaConflicto.REEMPLAZAR_TODO -> {
                entradaHorasRepository.reemplazarEnFechas(p.trabajoId, estado.fechasConflicto, estado.entradasAGenerar)
            }
            EstrategiaConflicto.SOLO_DIAS_VACIOS -> {
                val soloVacios = estado.entradasAGenerar
                    .filter { it.fecha !in estado.fechasConflicto }
                entradaHorasRepository.guardarVarias(soloVacios)
            }
            EstrategiaConflicto.CANCELAR -> {}
        }
        _conflictoState.value = null
    }

}
