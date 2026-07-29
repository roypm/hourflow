package com.horastrabajo.app.ui.mes

import com.horastrabajo.app.domain.model.Dinero
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DineroInputTest {

    @Test
    fun `acepta coma y punto como separador decimal`() {
        assertEquals(Dinero(850), textoADinero("8,50"))
        assertEquals(Dinero(850), textoADinero("8.50"))
    }

    @Test
    fun `acepta cantidades negativas`() {
        assertEquals(Dinero(-30), textoADinero("-0,30"))
    }

    @Test
    fun `texto vacio o en blanco no es un numero valido`() {
        assertNull(textoADinero(""))
        assertNull(textoADinero("   "))
    }

    @Test
    fun `texto no numerico no es valido`() {
        assertNull(textoADinero("abc"))
        assertNull(textoADinero("8,50€"))
    }

    @Test
    fun `espacios alrededor del numero se ignoran`() {
        assertEquals(Dinero(850), textoADinero("  8,50  "))
    }

    @Test
    fun `redondea al centimo mas cercano si hay mas de dos decimales`() {
        assertEquals(Dinero(856), textoADinero("8,5555"))
    }

    @Test
    fun `dineroATexto hace ida y vuelta con textoADinero`() {
        val original = Dinero(1234)
        assertEquals(original, textoADinero(dineroATexto(original)))
    }

    @Test
    fun `dineroATexto hace ida y vuelta con un importe negativo menor a una unidad`() {
        val original = Dinero(-30)
        assertEquals(original, textoADinero(dineroATexto(original)))
    }
}
