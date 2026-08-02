package com.horastrabajo.app.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportLayoutTest {
    @Test
    fun `factor JPG no reduce cuando cabe`() {
        assertEquals(1f, factorAjusteJpg(700f, 500f))
    }

    @Test
    fun `factor JPG reduce todo proporcionalmente cuando desborda`() {
        assertEquals(0.5f, factorAjusteJpg(500f, 1000f))
    }

    @Test
    fun `factor JPG nunca es cero para contenido vacio`() {
        assertEquals(1f, factorAjusteJpg(500f, 0f))
    }

    @Test
    fun `paginacion no parte filas y usa limite diferente en primera pagina`() {
        val paginas = paginarAlturas(
            alturas = listOf(30f, 30f, 30f, 30f),
            limitePrimeraPagina = 50f,
            limiteOtrasPaginas = 70f,
        )

        assertEquals(listOf(0), paginas[0].indices)
        assertEquals(listOf(1, 2), paginas[1].indices)
        assertEquals(listOf(3), paginas[2].indices)
        assertTrue(paginas.all { it.alto <= 70f })
    }

    @Test
    fun `una fila demasiado alta se conserva completa`() {
        val paginas = paginarAlturas(
            alturas = listOf(120f),
            limitePrimeraPagina = 50f,
            limiteOtrasPaginas = 70f,
        )

        assertEquals(listOf(0), paginas.single().indices)
        assertEquals(120f, paginas.single().alto)
    }
}
