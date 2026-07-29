package com.horastrabajo.app.domain

import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.Trabajo
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumenMensualCalculatorTest {

    private val trabajo = Trabajo(id = 1, nombre = "Bar Pepe", nombreUsuario = "Ana")

    private fun entrada(
        dia: Int,
        horaEntrada: LocalTime,
        horaSalida: LocalTime,
        esDiaSiguiente: Boolean = false,
        precioPorHoraCustom: Dinero? = null,
    ) = EntradaHoras(
        trabajoId = 1,
        fecha = LocalDate.of(2026, 3, dia),
        horaEntrada = horaEntrada,
        horaSalida = horaSalida,
        esDiaSiguiente = esDiaSiguiente,
        precioPorHoraCustom = precioPorHoraCustom,
    )

    @Test
    fun `suma horas y dinero de un dia con un solo turno`() {
        val resumen = ResumenMensualCalculator.calcular(
            trabajo = trabajo, anio = 2026, mes = 3, precioPorHora = Dinero(1000),
            entradas = listOf(entrada(5, LocalTime.of(9, 0), LocalTime.of(17, 0))),
            dineroExtra = emptyList(),
        )
        val dia = resumen.dias.single()
        assertEquals(8.0, dia.horas, 0.0001)
        assertEquals(Dinero(8000), dia.dinero)
        assertEquals(8.0, resumen.totalHoras, 0.0001)
        assertEquals(Dinero(8000), resumen.totalDineroHoras)
    }

    @Test
    fun `turno partido el mismo dia se suma en un solo ResumenDia`() {
        val resumen = ResumenMensualCalculator.calcular(
            trabajo = trabajo, anio = 2026, mes = 3, precioPorHora = Dinero(1000),
            entradas = listOf(
                entrada(5, LocalTime.of(9, 0), LocalTime.of(13, 0)),
                entrada(5, LocalTime.of(17, 0), LocalTime.of(20, 0)),
            ),
            dineroExtra = emptyList(),
        )
        assertEquals(1, resumen.dias.size)
        assertEquals(7.0, resumen.dias.single().horas, 0.0001)
    }

    @Test
    fun `tarifa custom del turno prevalece sobre la tarifa del mes`() {
        val resumen = ResumenMensualCalculator.calcular(
            trabajo = trabajo, anio = 2026, mes = 3, precioPorHora = Dinero(1000),
            entradas = listOf(entrada(5, LocalTime.of(9, 0), LocalTime.of(17, 0), precioPorHoraCustom = Dinero(1500))),
            dineroExtra = emptyList(),
        )
        assertEquals(Dinero(12000), resumen.dias.single().dinero)
    }

    @Test
    fun `sin tarifa fijada el dinero es cero pero las horas se cuentan igual`() {
        val resumen = ResumenMensualCalculator.calcular(
            trabajo = trabajo, anio = 2026, mes = 3, precioPorHora = null,
            entradas = listOf(entrada(5, LocalTime.of(9, 0), LocalTime.of(17, 0))),
            dineroExtra = emptyList(),
        )
        assertEquals(8.0, resumen.dias.single().horas, 0.0001)
        assertEquals(Dinero.CERO, resumen.dias.single().dinero)
    }

    @Test
    fun `turno nocturno se agrupa en el dia en que empieza`() {
        val resumen = ResumenMensualCalculator.calcular(
            trabajo = trabajo, anio = 2026, mes = 3, precioPorHora = Dinero(1000),
            entradas = listOf(entrada(5, LocalTime.of(22, 0), LocalTime.of(6, 0), esDiaSiguiente = true)),
            dineroExtra = emptyList(),
        )
        val dia = resumen.dias.single()
        assertEquals(LocalDate.of(2026, 3, 5), dia.fecha)
        assertEquals(8.0, dia.horas, 0.0001)
    }

    @Test
    fun `dinero extra se suma aparte del dinero por horas`() {
        val resumen = ResumenMensualCalculator.calcular(
            trabajo = trabajo, anio = 2026, mes = 3, precioPorHora = Dinero(1000),
            entradas = listOf(entrada(5, LocalTime.of(9, 0), LocalTime.of(17, 0))),
            dineroExtra = listOf(
                DineroExtra(trabajoId = 1, fecha = LocalDate.of(2026, 3, 6), monto = Dinero(1500), descripcion = "propina"),
                DineroExtra(trabajoId = 1, fecha = LocalDate.of(2026, 3, 20), monto = Dinero(-30), descripcion = "adelanto"),
            ),
        )
        assertEquals(Dinero(8000), resumen.totalDineroHoras)
        assertEquals(Dinero(1470), resumen.totalDineroExtra)
        assertEquals(Dinero(9470), resumen.totalDinero)
    }

    @Test
    fun `sin entradas ni dinero extra los totales son cero`() {
        val resumen = ResumenMensualCalculator.calcular(
            trabajo = trabajo, anio = 2026, mes = 3, precioPorHora = Dinero(1000),
            entradas = emptyList(), dineroExtra = emptyList(),
        )
        assertTrue(resumen.dias.isEmpty())
        assertEquals(0.0, resumen.totalHoras, 0.0001)
        assertEquals(Dinero.CERO, resumen.totalDinero)
    }

    @Test
    fun `marzo 2026 genera 6 semanas con la primera y la ultima parciales`() {
        // 2026-03-01 es domingo: la primera "semana" queda como un solo dia suelto,
        // y el mes termina en martes (31), dejando tambien la ultima semana parcial.
        val resumen = ResumenMensualCalculator.calcular(
            trabajo = trabajo, anio = 2026, mes = 3, precioPorHora = Dinero(1000),
            entradas = emptyList(), dineroExtra = emptyList(),
        )
        assertEquals(6, resumen.semanas.size)
        assertFalse(resumen.semanas.first().completa)
        assertEquals(LocalDate.of(2026, 3, 1), resumen.semanas.first().primerDia)
        assertEquals(LocalDate.of(2026, 3, 1), resumen.semanas.first().ultimoDiaVisible)
        assertTrue(resumen.semanas[1].completa)
        assertEquals(LocalDate.of(2026, 3, 2), resumen.semanas[1].primerDia)
        assertEquals(LocalDate.of(2026, 3, 8), resumen.semanas[1].ultimoDiaVisible)
        assertFalse(resumen.semanas.last().completa)
        assertEquals(LocalDate.of(2026, 3, 30), resumen.semanas.last().primerDia)
        assertEquals(LocalDate.of(2026, 3, 31), resumen.semanas.last().ultimoDiaVisible)
    }

    @Test
    fun `semana completa suma horas y dinero de sus dias`() {
        val resumen = ResumenMensualCalculator.calcular(
            trabajo = trabajo, anio = 2026, mes = 3, precioPorHora = Dinero(1000),
            entradas = listOf(
                entrada(2, LocalTime.of(9, 0), LocalTime.of(17, 0)),
                entrada(8, LocalTime.of(9, 0), LocalTime.of(13, 0)),
            ),
            dineroExtra = emptyList(),
        )
        val semana = resumen.semanas[1]
        assertTrue(semana.completa)
        assertEquals(12.0, semana.horas, 0.0001)
        assertEquals(Dinero(12000), semana.dinero)
    }
}
