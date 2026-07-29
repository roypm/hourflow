package com.horastrabajo.app.domain

import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.Trabajo
import com.horastrabajo.app.domain.model.suma
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class ResumenDia(
    val fecha: LocalDate,
    val entradas: List<EntradaHoras>,
    val horas: Double,
    val dinero: Dinero,
)

/**
 * Subtotal de una semana (lunes-domingo). Si la semana cruza el límite del mes,
 * [dias] solo contiene los días de esa semana que caen dentro del mes mostrado —
 * es un total parcial, no de los 7 días naturales de la semana.
 */
data class ResumenSemana(
    val primerDia: LocalDate,
    val ultimoDiaVisible: LocalDate,
    val horas: Double,
    val dinero: Dinero,
    val completa: Boolean,
)

data class ResumenMensual(
    val trabajo: Trabajo,
    val anio: Int,
    val mes: Int,
    val precioPorHora: Dinero?,
    val dias: List<ResumenDia>,
    val semanas: List<ResumenSemana>,
    val dineroExtra: List<DineroExtra>,
) {
    val totalHoras: Double get() = dias.sumOf { it.horas }
    val totalDineroHoras: Dinero get() = dias.map { it.dinero }.suma()
    val totalDineroExtra: Dinero get() = dineroExtra.map { it.monto }.suma()
    val totalDinero: Dinero get() = totalDineroHoras + totalDineroExtra
}

object ResumenMensualCalculator {

    fun calcular(
        trabajo: Trabajo,
        anio: Int,
        mes: Int,
        precioPorHora: Dinero?,
        entradas: List<EntradaHoras>,
        dineroExtra: List<DineroExtra>,
    ): ResumenMensual {
        val entradasPorDia = entradas.groupBy { it.fecha }
        val dias = entradasPorDia.entries
            .sortedBy { it.key }
            .map { (fecha, entradasDelDia) ->
                val horas = entradasDelDia.sumOf { it.horasDecimal }
                val dinero = entradasDelDia.map { entrada ->
                    val tarifaEfectiva = entrada.precioPorHoraCustom ?: precioPorHora
                    tarifaEfectiva?.let { Dinero.porHoras(entrada.horasDecimal, it) } ?: Dinero.CERO
                }.suma()
                ResumenDia(fecha = fecha, entradas = entradasDelDia, horas = horas, dinero = dinero)
            }

        val yearMonth = YearMonth.of(anio, mes)
        val semanas = calcularSemanas(yearMonth, dias)

        return ResumenMensual(
            trabajo = trabajo,
            anio = anio,
            mes = mes,
            precioPorHora = precioPorHora,
            dias = dias,
            semanas = semanas,
            dineroExtra = dineroExtra,
        )
    }

    /**
     * Agrupa los días del mes en semanas naturales (lunes-domingo). La primera y/o
     * última semana pueden quedar parciales si el mes no empieza en lunes o no
     * termina en domingo: cada día se cuenta en el mes al que pertenece, sin reparto.
     */
    private fun calcularSemanas(yearMonth: YearMonth, dias: List<ResumenDia>): List<ResumenSemana> {
        val horasPorFecha = dias.associate { it.fecha to it.horas }
        val dineroPorFecha = dias.associate { it.fecha to it.dinero }

        val todosLosDias = (1..yearMonth.lengthOfMonth()).map { yearMonth.atDay(it) }
        val semanas = mutableListOf<ResumenSemana>()
        var semanaActual = mutableListOf<LocalDate>()

        for (dia in todosLosDias) {
            if (dia.dayOfWeek == DayOfWeek.MONDAY && semanaActual.isNotEmpty()) {
                semanas += construirResumenSemana(semanaActual, horasPorFecha, dineroPorFecha)
                semanaActual = mutableListOf()
            }
            semanaActual += dia
        }
        if (semanaActual.isNotEmpty()) {
            semanas += construirResumenSemana(semanaActual, horasPorFecha, dineroPorFecha)
        }
        return semanas
    }

    private fun construirResumenSemana(
        diasDeLaSemana: List<LocalDate>,
        horasPorFecha: Map<LocalDate, Double>,
        dineroPorFecha: Map<LocalDate, Dinero>,
    ): ResumenSemana {
        val horas = diasDeLaSemana.sumOf { horasPorFecha[it] ?: 0.0 }
        val dinero = diasDeLaSemana.map { dineroPorFecha[it] ?: Dinero.CERO }.suma()
        val completa = diasDeLaSemana.first().dayOfWeek == DayOfWeek.MONDAY &&
            diasDeLaSemana.last().dayOfWeek == DayOfWeek.SUNDAY
        return ResumenSemana(
            primerDia = diasDeLaSemana.first(),
            ultimoDiaVisible = diasDeLaSemana.last(),
            horas = horas,
            dinero = dinero,
            completa = completa,
        )
    }
}
