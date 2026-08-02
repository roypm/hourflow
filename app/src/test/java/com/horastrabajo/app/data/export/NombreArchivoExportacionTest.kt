package com.horastrabajo.app.data.export

import com.horastrabajo.app.domain.ResumenMensual
import com.horastrabajo.app.domain.model.Trabajo
import org.junit.Assert.assertEquals
import org.junit.Test

class NombreArchivoExportacionTest {
    @Test
    fun `genera nombre ordenable y comun para ambos formatos`() {
        val resumen = ResumenMensual(
            trabajo = Trabajo(nombre = "Restaurante / Centro", nombreUsuario = "Joan Piñol"),
            anio = 2026,
            mes = 8,
            precioPorHora = null,
            dias = emptyList(),
            semanas = emptyList(),
            dineroExtra = emptyList(),
        )

        assertEquals("2026-08_Restaurante_Centro_Joan_Pinol.pdf", nombreArchivoResumen(resumen, "pdf"))
        assertEquals("2026-08_Restaurante_Centro_Joan_Pinol.jpg", nombreArchivoResumen(resumen, "jpg"))
    }
}
