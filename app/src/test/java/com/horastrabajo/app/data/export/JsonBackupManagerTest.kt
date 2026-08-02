package com.horastrabajo.app.data.export

import com.horastrabajo.app.data.local.entity.DineroExtraEntity
import com.horastrabajo.app.data.local.entity.EntradaHorasEntity
import com.horastrabajo.app.data.local.entity.TarifaMensualEntity
import com.horastrabajo.app.data.local.entity.TrabajoEntity
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.TarifaMensual
import com.horastrabajo.app.domain.model.Trabajo
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JsonBackupManagerTest {

    private lateinit var trabajoRepository: FakeTrabajoRepository
    private lateinit var entradaHorasRepository: FakeEntradaHorasRepository
    private lateinit var tarifaMensualRepository: FakeTarifaMensualRepository
    private lateinit var dineroExtraRepository: FakeDineroExtraRepository
    private lateinit var manager: JsonBackupManager

    @Before
    fun setUp() {
        trabajoRepository = FakeTrabajoRepository()
        entradaHorasRepository = FakeEntradaHorasRepository()
        tarifaMensualRepository = FakeTarifaMensualRepository()
        dineroExtraRepository = FakeDineroExtraRepository()
        manager = nuevoManager(
            trabajoRepository,
            entradaHorasRepository,
            tarifaMensualRepository,
            dineroExtraRepository,
        )
    }

    @Test
    fun `exportar incluye la tarifa custom del turno en el JSON`() = runBlocking {
        val trabajoId = trabajoRepository.guardar(Trabajo(nombre = "Bar Pepe", nombreUsuario = "Ana"))
        entradaHorasRepository.guardar(
            EntradaHoras(
                trabajoId = trabajoId,
                fecha = LocalDate.of(2026, 3, 5),
                horaEntrada = LocalTime.of(9, 0),
                horaSalida = LocalTime.of(17, 0),
                esDiaSiguiente = false,
                precioPorHoraCustom = Dinero(1200),
            )
        )

        val json = manager.exportarTodo()

        assertTrue(json.contains("precioPorHoraCustomCentimos"))
        assertTrue(json.contains("1200"))
    }

    @Test
    fun `export e import preservan la tarifa custom del turno`() = runBlocking {
        val trabajoId = trabajoRepository.guardar(Trabajo(nombre = "Bar Pepe", nombreUsuario = "Ana"))
        entradaHorasRepository.guardar(
            EntradaHoras(
                trabajoId = trabajoId,
                fecha = LocalDate.of(2026, 3, 5),
                horaEntrada = LocalTime.of(9, 0),
                horaSalida = LocalTime.of(17, 0),
                esDiaSiguiente = false,
                notas = "turno especial",
                precioPorHoraCustom = Dinero(1200),
            )
        )

        val json = manager.exportarTodo()

        // Simula "cambiar de móvil": repositorios completamente nuevos y vacíos.
        val entradaHorasDestino = FakeEntradaHorasRepository()
        val managerDestino = nuevoManager(
            FakeTrabajoRepository(),
            entradaHorasDestino,
            FakeTarifaMensualRepository(),
            FakeDineroExtraRepository(),
        )
        managerDestino.importarTodo(json)

        val entradaImportada = entradaHorasDestino.obtenerTodasDelTrabajo(1).single()
        assertEquals(Dinero(1200), entradaImportada.precioPorHoraCustom)
        assertEquals("turno especial", entradaImportada.notas)
    }

    @Test
    fun `import preserva dinero extra negativo, tarifas mensuales y turno nocturno`() = runBlocking {
        val trabajoId = trabajoRepository.guardar(Trabajo(nombre = "Reparto", nombreUsuario = "Luis", simboloMoneda = "$"))
        entradaHorasRepository.guardar(
            EntradaHoras(
                trabajoId = trabajoId,
                fecha = LocalDate.of(2026, 1, 31),
                horaEntrada = LocalTime.of(22, 0),
                horaSalida = LocalTime.of(6, 0),
                esDiaSiguiente = true,
            )
        )
        tarifaMensualRepository.guardarVarias(
            listOf(com.horastrabajo.app.domain.model.TarifaMensual(trabajoId = trabajoId, anio = 2026, mes = 1, precioPorHora = Dinero(1000)))
        )
        dineroExtraRepository.guardar(
            DineroExtra(trabajoId = trabajoId, fecha = LocalDate.of(2026, 1, 20), monto = Dinero(-30), descripcion = "adelanto")
        )

        val json = manager.exportarTodo()

        val entradaHorasDestino = FakeEntradaHorasRepository()
        val tarifaDestino = FakeTarifaMensualRepository()
        val dineroExtraDestino = FakeDineroExtraRepository()
        nuevoManager(FakeTrabajoRepository(), entradaHorasDestino, tarifaDestino, dineroExtraDestino)
            .importarTodo(json)

        val entrada = entradaHorasDestino.obtenerTodasDelTrabajo(1).single()
        assertTrue(entrada.esDiaSiguiente)
        assertEquals(LocalTime.of(6, 0), entrada.horaSalida)

        val tarifa = tarifaDestino.obtenerTodasDelTrabajo(1).single()
        assertEquals(Dinero(1000), tarifa.precioPorHora)

        val extra = dineroExtraDestino.obtenerTodoDelTrabajo(1).single()
        assertEquals(Dinero(-30), extra.monto)
    }

    @Test
    fun `importar un backup version 2 sin precioPorHoraCustom no falla y lo deja en null`() = runBlocking {
        // JSON tal cual lo habria producido la app ANTES de este fix (sin el campo nuevo).
        val backupVersion2 = """
            {
                "version": 2,
                "trabajos": [
                    {
                        "id": 1,
                        "nombre": "Bar Pepe",
                        "nombreUsuario": "Ana",
                        "simboloMoneda": "€",
                        "entradas": [
                            {
                                "id": 1,
                                "fechaIso": "2026-03-05",
                                "horaEntradaIso": "09:00",
                                "horaSalidaIso": "17:00",
                                "esDiaSiguiente": false
                            }
                        ],
                        "tarifas": [],
                        "dineroExtra": []
                    }
                ]
            }
        """.trimIndent()

        manager.importarTodo(backupVersion2)

        val entrada = entradaHorasRepository.obtenerTodasDelTrabajo(1).single()
        assertNull(entrada.precioPorHoraCustom)
        assertEquals(LocalDate.of(2026, 3, 5), entrada.fecha)
    }

    @Test
    fun `importar un backup version 1 sin nombreUsuario no falla`() = runBlocking {
        val backupVersion1 = """
            {
                "version": 1,
                "trabajos": [
                    {
                        "id": 1,
                        "nombre": "Bar Pepe",
                        "simboloMoneda": "€",
                        "entradas": [],
                        "tarifas": [],
                        "dineroExtra": []
                    }
                ]
            }
        """.trimIndent()

        manager.importarTodo(backupVersion1)

        val trabajo = trabajoRepository.observeAll().first().single()
        assertEquals("", trabajo.nombreUsuario)
        assertEquals("Bar Pepe", trabajo.nombre)
    }

    private fun nuevoManager(
        trabajoRepository: FakeTrabajoRepository,
        entradaHorasRepository: FakeEntradaHorasRepository,
        tarifaMensualRepository: FakeTarifaMensualRepository,
        dineroExtraRepository: FakeDineroExtraRepository,
    ) = JsonBackupManager(
        trabajoRepository = trabajoRepository,
        entradaHorasRepository = entradaHorasRepository,
        tarifaMensualRepository = tarifaMensualRepository,
        dineroExtraRepository = dineroExtraRepository,
        importarTrabajosCompletos = { trabajos, entradas, tarifas, dinerosExtra, _, _ ->
            importarEnFakes(
                trabajoRepository = trabajoRepository,
                entradaHorasRepository = entradaHorasRepository,
                tarifaMensualRepository = tarifaMensualRepository,
                dineroExtraRepository = dineroExtraRepository,
                trabajos = trabajos,
                entradas = entradas,
                tarifas = tarifas,
                dinerosExtra = dinerosExtra,
            )
        },
    )

    private suspend fun importarEnFakes(
        trabajoRepository: FakeTrabajoRepository,
        entradaHorasRepository: FakeEntradaHorasRepository,
        tarifaMensualRepository: FakeTarifaMensualRepository,
        dineroExtraRepository: FakeDineroExtraRepository,
        trabajos: List<TrabajoEntity>,
        entradas: List<List<EntradaHorasEntity>>,
        tarifas: List<List<TarifaMensualEntity>>,
        dinerosExtra: List<List<DineroExtraEntity>>,
    ): Map<Long, Long> {
        val ids = mutableMapOf<Long, Long>()
        for (i in trabajos.indices) {
            val trabajo = trabajos[i]
            val nuevoTrabajoId = trabajoRepository.guardar(
                Trabajo(
                    nombre = trabajo.nombre,
                    nombreUsuario = trabajo.nombreUsuario,
                    simboloMoneda = trabajo.simboloMoneda,
                    fotoUri = trabajo.fotoUri,
                )
            )
            ids[trabajo.id] = nuevoTrabajoId
            entradaHorasRepository.guardarVarias(
                entradas.getOrElse(i) { emptyList() }.map { it.toDomain(nuevoTrabajoId) }
            )
            tarifaMensualRepository.guardarVarias(
                tarifas.getOrElse(i) { emptyList() }.map { it.toDomain(nuevoTrabajoId) }
            )
            dineroExtraRepository.guardarVarios(
                dinerosExtra.getOrElse(i) { emptyList() }.map { it.toDomain(nuevoTrabajoId) }
            )
        }
        return ids
    }

    private fun EntradaHorasEntity.toDomain(trabajoId: Long) = EntradaHoras(
        trabajoId = trabajoId,
        fecha = fecha,
        horaEntrada = horaEntrada,
        horaSalida = horaSalida,
        esDiaSiguiente = esDiaSiguiente,
        notas = notas,
        precioPorHoraCustom = precioPorHoraCustomCentimos?.let { Dinero(it) },
    )

    private fun TarifaMensualEntity.toDomain(trabajoId: Long) = TarifaMensual(
        trabajoId = trabajoId,
        anio = anio,
        mes = mes,
        precioPorHora = Dinero(precioPorHoraCentimos),
    )

    private fun DineroExtraEntity.toDomain(trabajoId: Long) = DineroExtra(
        trabajoId = trabajoId,
        fecha = fecha,
        monto = Dinero(montoCentimos),
        descripcion = descripcion,
    )
}
