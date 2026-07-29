package com.horastrabajo.app.domain

import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.TarifaMensual
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ResumenAnualCalculatorTest {

    private fun entrada(mes: Int, dia: Int, horas: Int, anio: Int = 2026, precioPorHoraCustom: Dinero? = null) = EntradaHoras(
        trabajoId = 1,
        fecha = LocalDate.of(anio, mes, dia),
        horaEntrada = LocalTime.of(9, 0),
        horaSalida = LocalTime.of(9 + horas, 0),
        esDiaSiguiente = false,
        precioPorHoraCustom = precioPorHoraCustom,
    )

    @Test
    fun `agrupa horas por mes y deja en cero los meses sin entradas`() {
        val resumen = ResumenAnualCalculator.calcular(
            anio = 2026,
            entradas = listOf(entrada(3, 5, 8), entrada(3, 6, 4), entrada(5, 1, 6)),
            dineroExtra = emptyList(),
            tarifas = emptyList(),
        )
        assertEquals(12.0, resumen.meses.single { it.mes == 3 }.horas, 0.0001)
        assertEquals(6.0, resumen.meses.single { it.mes == 5 }.horas, 0.0001)
        assertEquals(0.0, resumen.meses.single { it.mes == 7 }.horas, 0.0001)
    }

    @Test
    fun `la tarifa se hereda del mes anterior mas reciente, no de uno mas antiguo`() {
        val resumen = ResumenAnualCalculator.calcular(
            anio = 2026,
            entradas = listOf(entrada(5, 1, 8)),
            dineroExtra = emptyList(),
            tarifas = listOf(
                TarifaMensual(trabajoId = 1, anio = 2026, mes = 1, precioPorHora = Dinero(1000)),
                TarifaMensual(trabajoId = 1, anio = 2026, mes = 3, precioPorHora = Dinero(1200)),
            ),
        )
        assertEquals(Dinero(9600), resumen.meses.single { it.mes == 5 }.dinero)
    }

    @Test
    fun `sin ninguna tarifa fijada el dinero de esos meses es cero`() {
        val resumen = ResumenAnualCalculator.calcular(
            anio = 2026,
            entradas = listOf(entrada(2, 1, 8)),
            dineroExtra = emptyList(),
            tarifas = emptyList(),
        )
        assertEquals(Dinero.CERO, resumen.meses.single { it.mes == 2 }.dinero)
    }

    @Test
    fun `tarifa custom del turno prevalece sobre la tarifa vigente del mes`() {
        val resumen = ResumenAnualCalculator.calcular(
            anio = 2026,
            entradas = listOf(entrada(3, 5, 8, precioPorHoraCustom = Dinero(2000))),
            dineroExtra = emptyList(),
            tarifas = listOf(TarifaMensual(trabajoId = 1, anio = 2026, mes = 3, precioPorHora = Dinero(1000))),
        )
        assertEquals(Dinero(16000), resumen.meses.single { it.mes == 3 }.dinero)
    }

    @Test
    fun `dinero extra de un mes se suma a su dinero por horas`() {
        val resumen = ResumenAnualCalculator.calcular(
            anio = 2026,
            entradas = listOf(entrada(3, 5, 8)),
            dineroExtra = listOf(
                DineroExtra(trabajoId = 1, fecha = LocalDate.of(2026, 3, 10), monto = Dinero(500), descripcion = "propina"),
            ),
            tarifas = listOf(TarifaMensual(trabajoId = 1, anio = 2026, mes = 3, precioPorHora = Dinero(1000))),
        )
        assertEquals(Dinero(8500), resumen.meses.single { it.mes == 3 }.dinero)
    }

    @Test
    fun `totales anuales suman los 12 meses, incluida la tarifa heredada`() {
        val resumen = ResumenAnualCalculator.calcular(
            anio = 2026,
            entradas = listOf(entrada(1, 1, 8), entrada(2, 1, 4)),
            dineroExtra = emptyList(),
            tarifas = listOf(TarifaMensual(trabajoId = 1, anio = 2026, mes = 1, precioPorHora = Dinero(1000))),
        )
        assertEquals(12.0, resumen.totalHoras, 0.0001)
        assertEquals(Dinero(12000), resumen.totalDinero)
    }

    @Test
    fun `una tarifa de un anio anterior tambien se hereda si no hay ninguna mas reciente`() {
        val resumen = ResumenAnualCalculator.calcular(
            anio = 2027,
            entradas = listOf(entrada(1, 1, 8, anio = 2027)),
            dineroExtra = emptyList(),
            tarifas = listOf(TarifaMensual(trabajoId = 1, anio = 2026, mes = 12, precioPorHora = Dinero(1100))),
        )
        assertEquals(Dinero(8800), resumen.meses.single { it.mes == 1 }.dinero)
    }
}
