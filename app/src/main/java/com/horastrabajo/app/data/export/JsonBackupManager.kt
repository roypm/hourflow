package com.horastrabajo.app.data.export

import com.horastrabajo.app.data.export.dto.BackupDto
import com.horastrabajo.app.data.export.dto.DineroExtraDto
import com.horastrabajo.app.data.export.dto.EntradaHorasDto
import com.horastrabajo.app.data.export.dto.TarifaMensualDto
import com.horastrabajo.app.data.export.dto.TrabajoDto
import com.horastrabajo.app.data.local.AppDatabase
import com.horastrabajo.app.data.local.entity.DineroExtraEntity
import com.horastrabajo.app.data.local.entity.EntradaHorasEntity
import com.horastrabajo.app.data.local.entity.TarifaMensualEntity
import com.horastrabajo.app.data.local.entity.TrabajoEntity
import com.horastrabajo.app.data.repository.DineroExtraRepository
import com.horastrabajo.app.data.repository.EntradaHorasRepository
import com.horastrabajo.app.data.repository.TarifaMensualRepository
import com.horastrabajo.app.data.repository.TrabajoRepository
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EntradaHoras
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime

/**
 * Backup completo de TODOS los trabajos en JSON (formato de import/export real,
 * para no perder datos al cambiar de móvil). Importar siempre crea trabajos NUEVOS
 * (no intenta fusionar con trabajos existentes del mismo nombre).
 */
internal typealias ImportarTrabajosCompletos = suspend (
    trabajos: List<TrabajoEntity>,
    entradas: List<List<EntradaHorasEntity>>,
    tarifas: List<List<TarifaMensualEntity>>,
    dinerosExtra: List<List<DineroExtraEntity>>,
) -> Unit

class JsonBackupManager internal constructor(
    private val trabajoRepository: TrabajoRepository,
    private val entradaHorasRepository: EntradaHorasRepository,
    private val tarifaMensualRepository: TarifaMensualRepository,
    private val dineroExtraRepository: DineroExtraRepository,
    private val importarTrabajosCompletos: ImportarTrabajosCompletos,
) {
    constructor(
        database: AppDatabase,
        trabajoRepository: TrabajoRepository,
        entradaHorasRepository: EntradaHorasRepository,
        tarifaMensualRepository: TarifaMensualRepository,
        dineroExtraRepository: DineroExtraRepository,
    ) : this(
        trabajoRepository = trabajoRepository,
        entradaHorasRepository = entradaHorasRepository,
        tarifaMensualRepository = tarifaMensualRepository,
        dineroExtraRepository = dineroExtraRepository,
        importarTrabajosCompletos = database::importarTrabajosCompletos,
    )

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportarTodo(): String {
        val trabajos = trabajoRepository.observeAll().first()
        val trabajosDto = trabajos.map { trabajo ->
            TrabajoDto(
                id = trabajo.id,
                nombre = trabajo.nombre,
                nombreUsuario = trabajo.nombreUsuario,
                simboloMoneda = trabajo.simboloMoneda,
                entradas = entradaHorasRepository.obtenerTodasDelTrabajo(trabajo.id).map { it.toDto() },
                tarifas = tarifaMensualRepository.obtenerTodasDelTrabajo(trabajo.id).map {
                    TarifaMensualDto(
                        id = it.id,
                        anio = it.anio,
                        mes = it.mes,
                        precioPorHoraCentimos = it.precioPorHora.centimos,
                    )
                },
                dineroExtra = dineroExtraRepository.obtenerTodoDelTrabajo(trabajo.id).map { it.toDto() },
            )
        }
        return json.encodeToString(BackupDto.serializer(), BackupDto(trabajos = trabajosDto))
    }

    suspend fun importarTodo(contenidoJson: String) {
        val backup = json.decodeFromString(BackupDto.serializer(), contenidoJson)
        val trabajos = mutableListOf<TrabajoEntity>()
        val entradas = mutableListOf<List<EntradaHorasEntity>>()
        val tarifas = mutableListOf<List<TarifaMensualEntity>>()
        val dinerosExtra = mutableListOf<List<DineroExtraEntity>>()
        for (trabajoDto in backup.trabajos) {
            trabajos.add(
                TrabajoEntity(
                    nombre = trabajoDto.nombre,
                    nombreUsuario = trabajoDto.nombreUsuario,
                    simboloMoneda = trabajoDto.simboloMoneda,
                )
            )
            entradas.add(trabajoDto.entradas.map { it.toEntity() })
            tarifas.add(
                trabajoDto.tarifas.map { dto ->
                    TarifaMensualEntity(
                        trabajoId = 0L,
                        anio = dto.anio,
                        mes = dto.mes,
                        precioPorHoraCentimos = dto.precioPorHoraCentimos,
                    )
                }
            )
            dinerosExtra.add(trabajoDto.dineroExtra.map { it.toEntity() })
        }
        importarTrabajosCompletos(trabajos, entradas, tarifas, dinerosExtra)
    }
}

private fun EntradaHoras.toDto() = EntradaHorasDto(
    id = id,
    fechaIso = fecha.toString(),
    horaEntradaIso = horaEntrada.toString(),
    horaSalidaIso = horaSalida.toString(),
    esDiaSiguiente = esDiaSiguiente,
    notas = notas,
    precioPorHoraCustomCentimos = precioPorHoraCustom?.centimos,
)

private fun EntradaHorasDto.toDomain(trabajoId: Long) = EntradaHoras(
    trabajoId = trabajoId,
    fecha = LocalDate.parse(fechaIso),
    horaEntrada = LocalTime.parse(horaEntradaIso),
    horaSalida = LocalTime.parse(horaSalidaIso),
    esDiaSiguiente = esDiaSiguiente,
    notas = notas,
    precioPorHoraCustom = precioPorHoraCustomCentimos?.let { Dinero(it) },
)

private fun DineroExtra.toDto() = DineroExtraDto(
    id = id,
    fechaIso = fecha.toString(),
    montoCentimos = monto.centimos,
    descripcion = descripcion,
)

private fun DineroExtraDto.toDomain(trabajoId: Long) = DineroExtra(
    trabajoId = trabajoId,
    fecha = LocalDate.parse(fechaIso),
    monto = Dinero(montoCentimos),
    descripcion = descripcion,
)

private fun EntradaHorasDto.toEntity() = EntradaHorasEntity(
    trabajoId = 0L,
    fecha = LocalDate.parse(fechaIso),
    horaEntrada = LocalTime.parse(horaEntradaIso),
    horaSalida = LocalTime.parse(horaSalidaIso),
    esDiaSiguiente = esDiaSiguiente,
    notas = notas,
    precioPorHoraCustomCentimos = precioPorHoraCustomCentimos,
)

private fun DineroExtraDto.toEntity() = DineroExtraEntity(
    trabajoId = 0L,
    fecha = LocalDate.parse(fechaIso),
    montoCentimos = montoCentimos,
    descripcion = descripcion,
)
