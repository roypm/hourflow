package com.horastrabajo.app.ui.trabajos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horastrabajo.app.data.repository.DineroExtraRepository
import com.horastrabajo.app.data.repository.EntradaHorasRepository
import com.horastrabajo.app.data.repository.TarifaMensualRepository
import com.horastrabajo.app.data.repository.TrabajoRepository
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.TarifaMensual
import com.horastrabajo.app.domain.model.Trabajo
import com.horastrabajo.app.domain.model.suma
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class TrabajoCardUi(
    val trabajo: Trabajo,
    val totalHoras: Double,
    val totalDinero: Dinero,
    val primerRegistro: LocalDate?,
    val ultimoRegistro: LocalDate?,
    val mesesConActividad: Int,
)

class TrabajosViewModel(
    private val trabajoRepository: TrabajoRepository,
    private val entradaHorasRepository: EntradaHorasRepository,
    private val dineroExtraRepository: DineroExtraRepository,
    private val tarifaMensualRepository: TarifaMensualRepository,
) : ViewModel() {

    val trabajos: StateFlow<List<TrabajoCardUi>> = combine(
        trabajoRepository.observeAll(),
        entradaHorasRepository.observeTodas(),
        dineroExtraRepository.observeTodo(),
        tarifaMensualRepository.observeTodas(),
    ) { trabajos, entradas, dineroExtra, tarifas ->
        trabajos.map { trabajo ->
            construirTrabajoCardUi(
                trabajo = trabajo,
                entradas = entradas.filter { it.trabajoId == trabajo.id },
                dineroExtra = dineroExtra.filter { it.trabajoId == trabajo.id },
                tarifas = tarifas.filter { it.trabajoId == trabajo.id },
            )
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun crearTrabajo(nombre: String, nombreUsuario: String, simboloMoneda: String, fotoUri: String?) {
        if (nombre.isBlank() || nombreUsuario.isBlank()) return
        viewModelScope.launch {
            trabajoRepository.guardar(
                Trabajo(
                    nombre = nombre.trim(),
                    nombreUsuario = nombreUsuario.trim(),
                    simboloMoneda = simboloMoneda.trim().ifBlank { "€" },
                    fotoUri = fotoUri?.takeIf { it.isNotBlank() },
                )
            )
        }
    }

    fun actualizarTrabajo(
        trabajo: Trabajo,
        nombre: String,
        nombreUsuario: String,
        simboloMoneda: String,
        fotoUri: String?,
    ) {
        if (nombre.isBlank() || nombreUsuario.isBlank()) return
        viewModelScope.launch {
            trabajoRepository.guardar(
                trabajo.copy(
                    nombre = nombre.trim(),
                    nombreUsuario = nombreUsuario.trim(),
                    simboloMoneda = simboloMoneda.trim().ifBlank { "€" },
                    fotoUri = fotoUri?.takeIf { it.isNotBlank() },
                )
            )
        }
    }

    fun eliminarTrabajo(trabajo: Trabajo) {
        viewModelScope.launch { trabajoRepository.eliminar(trabajo) }
    }

    private fun construirTrabajoCardUi(
        trabajo: Trabajo,
        entradas: List<EntradaHoras>,
        dineroExtra: List<DineroExtra>,
        tarifas: List<TarifaMensual>,
    ): TrabajoCardUi {
        val totalHoras = entradas.sumOf { it.horasDecimal }
        val dineroHoras = entradas.map { entrada ->
            val tarifa = entrada.precioPorHoraCustom
                ?: tarifaVigenteEn(tarifas, entrada.fecha.year, entrada.fecha.monthValue)
            tarifa?.let { Dinero.porHoras(entrada.horasDecimal, it) } ?: Dinero.CERO
        }.suma()
        val totalDineroExtra = dineroExtra.map { it.monto }.suma()
        val fechas = entradas.map { it.fecha } + dineroExtra.map { it.fecha }
        val mesesConActividad = (entradas.map { YearMonth.from(it.fecha) } + dineroExtra.map { YearMonth.from(it.fecha) })
            .toSet()
            .size

        return TrabajoCardUi(
            trabajo = trabajo,
            totalHoras = totalHoras,
            totalDinero = dineroHoras + totalDineroExtra,
            primerRegistro = fechas.minOrNull(),
            ultimoRegistro = fechas.maxOrNull(),
            mesesConActividad = mesesConActividad,
        )
    }

    private fun tarifaVigenteEn(tarifas: List<TarifaMensual>, anio: Int, mes: Int): Dinero? =
        tarifas
            .filter { it.anio < anio || (it.anio == anio && it.mes <= mes) }
            .maxWithOrNull(compareBy({ it.anio }, { it.mes }))
            ?.precioPorHora
}
