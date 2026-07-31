package com.horastrabajo.app.data.export

import com.horastrabajo.app.domain.model.formatearHoras
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

class FormatoDocumentoTest {

    @Test
    fun `formatearFechaCorta devuelve ddMM`() {
        val fecha = LocalDate.of(2026, 7, 31)
        assertEquals("31/07", formatearFechaCorta(fecha))
    }

    @Test
    fun `formatearHora devuelve HHmm en 24h`() {
        val hora = LocalTime.of(8, 0)
        assertEquals("08:00", formatearHora(hora))
        val hora2 = LocalTime.of(22, 30)
        assertEquals("22:30", formatearHora(hora2))
    }

    @Test
    fun `formatearDiaSemanaCorto en español`() {
        val fecha = LocalDate.of(2026, 7, 31) // viernes
        assertEquals("vie", formatearDiaSemanaCorto(fecha, Locale.forLanguageTag("es")))
    }

    @Test
    fun `formatearDiaSemanaCorto en catalan`() {
        val fecha = LocalDate.of(2026, 7, 31) // divendres
        assertEquals("dv.", formatearDiaSemanaCorto(fecha, Locale.forLanguageTag("ca")))
    }

    @Test
    fun `formatearDiaSemanaCorto en ingles`() {
        val fecha = LocalDate.of(2026, 7, 31) // Friday
        assertEquals("Fri", formatearDiaSemanaCorto(fecha, Locale.forLanguageTag("en")))
    }

    @Test
    fun `formatearMesAnio en español`() {
        assertEquals("julio 2026", formatearMesAnio(7, 2026, Locale.forLanguageTag("es")))
    }

    @Test
    fun `formatearMesAnio en catalan`() {
        // El locale catalán devuelve "de juliol" -> la función devuelve "de juliol 2026"
        assertEquals("de juliol 2026", formatearMesAnio(7, 2026, Locale.forLanguageTag("ca")))
    }

    @Test
    fun `formatearMesAnio en inglés`() {
        assertEquals("July 2026", formatearMesAnio(7, 2026, Locale.forLanguageTag("en")))
    }

    @Test
    fun `formatearHoras devuelve dos decimales y h`() {
        assertEquals("7,50h", formatearHoras(7.5))
        assertEquals("8,00h", formatearHoras(8.0))
        assertEquals("180,00h", formatearHoras(180.0))
    }
}