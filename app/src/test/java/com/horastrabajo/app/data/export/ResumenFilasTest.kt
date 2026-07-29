package com.horastrabajo.app.data.export

import org.junit.Assert.assertEquals
import org.junit.Test

class ResumenFilasTest {

    @Test
    fun `los totales del resumen usan el mismo estilo visual`() {
        assertEquals(EstiloFila.TITULO, estiloFilaTotalResumen())
    }
}
