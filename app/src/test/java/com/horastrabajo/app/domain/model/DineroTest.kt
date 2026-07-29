package com.horastrabajo.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DineroTest {

    @Test
    fun `formateado mantiene el signo negativo por debajo de una unidad`() {
        assertEquals("-0,30 €", Dinero(-30).formateado("€"))
        assertEquals("-0,01 €", Dinero(-1).formateado("€"))
    }

    @Test
    fun `formateado mantiene el signo negativo por encima de una unidad`() {
        assertEquals("-5,25 €", Dinero(-525).formateado("€"))
    }

    @Test
    fun `formateado no antepone signo a importes positivos`() {
        assertEquals("0,30 €", Dinero(30).formateado("€"))
        assertEquals("5,25 €", Dinero(525).formateado("€"))
    }

    @Test
    fun `formateado del cero no lleva signo`() {
        assertEquals("0,00 €", Dinero.CERO.formateado("€"))
    }

    @Test
    fun `operadores aritmeticos preservan el signo`() {
        assertEquals(Dinero(-30), Dinero(0) - Dinero(30))
        assertEquals(Dinero(-30), -Dinero(30))
        assertEquals(Dinero(0), Dinero(-30) + Dinero(30))
    }

    @Test
    fun `porHoras redondea al centimo mas cercano`() {
        // 2.5 horas a 9,55 €/h = 23,875 € -> redondeo a 23,88 €
        assertEquals(Dinero(2388), Dinero.porHoras(2.5, Dinero(955)))
    }

    @Test
    fun `suma de lista de Dinero incluye negativos`() {
        val lista = listOf(Dinero(1500), Dinero(500), Dinero(-1000))
        assertEquals(Dinero(1000), lista.suma())
    }
}
