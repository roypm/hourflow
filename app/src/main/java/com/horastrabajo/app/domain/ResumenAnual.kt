package com.horastrabajo.app.domain

import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.TarifaMensual
import com.horastrabajo.app.domain.model.suma

data class ResumenAnualMes(
    val mes: Int,
    val horas: Double,
    val dinero: Dinero,
)

data class ResumenAnual(
    val anio: Int,
    val meses: List<ResumenAnualMes>,
) {
    val totalHoras: Double get() = meses.sumOf { it.horas }
    val totalDinero: Dinero get() = meses.map { it.dinero }.suma()
}

object ResumenAnualCalculator {

    fun calcular(
        anio: Int,
        entradas: List<EntradaHoras>,
        dineroExtra: List<DineroExtra>,
        tarifas: List<TarifaMensual>,
    ): ResumenAnual {
        val entradasPorMes = entradas.groupBy { it.fecha.monthValue }
        val dineroExtraPorMes = dineroExtra.groupBy { it.fecha.monthValue }

        val meses = (1..12).map { mes ->
            val entradasDelMes = entradasPorMes[mes].orEmpty()
            val horas = entradasDelMes.sumOf { it.horasDecimal }
            val precioPorHora = tarifaVigenteEn(tarifas, anio, mes)
            val dineroHoras = entradasDelMes.map { entrada ->
                val tarifaEfectiva = entrada.precioPorHoraCustom ?: precioPorHora
                tarifaEfectiva?.let { Dinero.porHoras(entrada.horasDecimal, it) } ?: Dinero.CERO
            }.suma()
            val dineroExtraDelMes = dineroExtraPorMes[mes].orEmpty().map { it.monto }.suma()
            ResumenAnualMes(mes = mes, horas = horas, dinero = dineroHoras + dineroExtraDelMes)
        }
        return ResumenAnual(anio = anio, meses = meses)
    }

    /** Tarifa explícita de (anio, mes) si existe, o si no, la más reciente en un mes anterior. */
    private fun tarifaVigenteEn(tarifas: List<TarifaMensual>, anio: Int, mes: Int): Dinero? =
        tarifas
            .filter { it.anio < anio || (it.anio == anio && it.mes <= mes) }
            .maxWithOrNull(compareBy({ it.anio }, { it.mes }))
            ?.precioPorHora
}
