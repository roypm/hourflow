package com.horastrabajo.app.domain.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class EntradaHorasTest {

    @Test
    fun `turno normal calcula duracion y horas decimales`() {
        val entrada = EntradaHoras(
            trabajoId = 1, fecha = LocalDate.of(2026, 3, 5),
            horaEntrada = LocalTime.of(9, 0), horaSalida = LocalTime.of(17, 30), esDiaSiguiente = false,
        )
        assertEquals(Duration.ofHours(8).plusMinutes(30), entrada.duracion)
        assertEquals(8.5, entrada.horasDecimal, 0.0001)
    }

    @Test
    fun `turno nocturno cuenta la salida como el dia siguiente`() {
        val entrada = EntradaHoras(
            trabajoId = 1, fecha = LocalDate.of(2026, 3, 5),
            horaEntrada = LocalTime.of(22, 0), horaSalida = LocalTime.of(6, 0), esDiaSiguiente = true,
        )
        assertEquals(LocalDate.of(2026, 3, 6).atTime(6, 0), entrada.fin)
        assertEquals(8.0, entrada.horasDecimal, 0.0001)
    }

    @Test
    fun `turno de duracion cero no lanza excepcion`() {
        val entrada = EntradaHoras(
            trabajoId = 1, fecha = LocalDate.of(2026, 3, 5),
            horaEntrada = LocalTime.of(9, 0), horaSalida = LocalTime.of(9, 0), esDiaSiguiente = false,
        )
        assertEquals(0.0, entrada.horasDecimal, 0.0001)
    }

    @Test
    fun `caso limite conocido- turno marcado nocturno sin cruzar medianoche produce mas de 24h`() {
        // La UI (EntradaFormSheet) no valida hoy la combinacion "esDiaSiguiente=true" +
        // "horaSalida > horaEntrada" el mismo dia. Este test fija el comportamiento actual
        // del dominio para que un futuro cambio de validacion sea deliberado, no accidental.
        val entrada = EntradaHoras(
            trabajoId = 1, fecha = LocalDate.of(2026, 3, 5),
            horaEntrada = LocalTime.of(9, 0), horaSalida = LocalTime.of(17, 0), esDiaSiguiente = true,
        )
        assertEquals(32.0, entrada.horasDecimal, 0.0001)
    }
}
