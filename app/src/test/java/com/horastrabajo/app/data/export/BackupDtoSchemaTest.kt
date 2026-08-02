package com.horastrabajo.app.data.export

import com.horastrabajo.app.data.export.dto.BackupDto
import com.horastrabajo.app.data.export.dto.PlantillaDiaDto
import com.horastrabajo.app.data.export.dto.PlantillaMesDto
import com.horastrabajo.app.data.export.dto.PlantillaMesOverrideDto
import com.horastrabajo.app.data.export.dto.PlantillaSemanaDto
import com.horastrabajo.app.data.export.dto.TrabajoDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDtoSchemaTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `el esquema actual exporta plantillas semanales y mensuales`() {
        val backup = BackupDto(
            trabajos = listOf(
                TrabajoDto(
                    id = 1L,
                    nombre = "Trabajo",
                    nombreUsuario = "Persona",
                    simboloMoneda = "€",
                    entradas = emptyList(),
                    tarifas = emptyList(),
                    dineroExtra = emptyList(),
                    plantillasSemana = listOf(
                        PlantillaSemanaDto(
                            id = 10L,
                            nombre = "Semana laboral",
                            dias = listOf(PlantillaDiaDto(1, "09:00", "17:00")),
                        )
                    ),
                    plantillasMes = listOf(
                        PlantillaMesDto(
                            id = 20L,
                            nombre = "Mes estándar",
                            plantillaSemanaDefaultId = 10L,
                            overrides = listOf(PlantillaMesOverrideDto(3, esSemanaLibre = true)),
                        )
                    ),
                )
            )
        )

        val contenido = json.encodeToString(BackupDto.serializer(), backup)
        val restaurado = json.decodeFromString(BackupDto.serializer(), contenido)

        assertEquals(4, restaurado.version)
        assertEquals("Semana laboral", restaurado.trabajos.single().plantillasSemana.single().nombre)
        assertEquals(10L, restaurado.trabajos.single().plantillasMes.single().plantillaSemanaDefaultId)
        assertTrue(restaurado.trabajos.single().plantillasMes.single().overrides.single().esSemanaLibre)
    }

    @Test
    fun `un backup anterior sin plantillas sigue siendo compatible`() {
        val contenido = """
            {
              "version": 3,
              "trabajos": [{
                "id": 1,
                "nombre": "Trabajo",
                "nombreUsuario": "Persona",
                "simboloMoneda": "€",
                "entradas": [],
                "tarifas": [],
                "dineroExtra": []
              }]
            }
        """.trimIndent()

        val restaurado = json.decodeFromString(BackupDto.serializer(), contenido)

        assertEquals(3, restaurado.version)
        assertTrue(restaurado.trabajos.single().plantillasSemana.isEmpty())
        assertTrue(restaurado.trabajos.single().plantillasMes.isEmpty())
    }
}
