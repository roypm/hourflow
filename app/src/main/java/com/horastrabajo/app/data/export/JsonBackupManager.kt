package com.horastrabajo.app.data.export

import com.horastrabajo.app.data.export.dto.BackupDto
import com.horastrabajo.app.data.export.dto.DineroExtraDto
import com.horastrabajo.app.data.export.dto.EntradaHorasDto
import com.horastrabajo.app.data.export.dto.PlantillaDiaDto
import com.horastrabajo.app.data.export.dto.PlantillaMesDto
import com.horastrabajo.app.data.export.dto.PlantillaMesOverrideDto
import com.horastrabajo.app.data.export.dto.PlantillaSemanaDto
import com.horastrabajo.app.data.export.dto.TarifaMensualDto
import com.horastrabajo.app.data.export.dto.TrabajoDto
import com.horastrabajo.app.data.local.AppDatabase
import com.horastrabajo.app.data.local.PlantillaMesBackupImport
import com.horastrabajo.app.data.local.PlantillaSemanaBackupImport
import com.horastrabajo.app.data.local.entity.DineroExtraEntity
import com.horastrabajo.app.data.local.entity.EntradaHorasEntity
import com.horastrabajo.app.data.local.entity.PlantillaMesDetalleEntity
import com.horastrabajo.app.data.local.entity.PlantillaMesEntity
import com.horastrabajo.app.data.local.entity.PlantillaSemanaDetalleEntity
import com.horastrabajo.app.data.local.entity.PlantillaSemanaEntity
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
    plantillasSemana: List<List<PlantillaSemanaBackupImport>>,
    plantillasMes: List<List<PlantillaMesBackupImport>>,
) -> Map<Long, Long>

class JsonBackupManager internal constructor(
    private val trabajoRepository: TrabajoRepository,
    private val entradaHorasRepository: EntradaHorasRepository,
    private val tarifaMensualRepository: TarifaMensualRepository,
    private val dineroExtraRepository: DineroExtraRepository,
    private val database: AppDatabase? = null,
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
        database = database,
        importarTrabajosCompletos = database::importarTrabajosCompletos,
    )

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun exportarTodo(): String {
        val trabajos = trabajoRepository.observeAll().first()
        val db = database
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
                plantillasSemana = db?.let { actualDb -> actualDb.plantillaSemanaDao().getAllByTrabajo(trabajo.id).map { plantilla ->
                    PlantillaSemanaDto(
                        id = plantilla.id,
                        nombre = plantilla.nombre,
                        descripcion = plantilla.descripcion,
                        dias = actualDb.plantillaSemanaDao().getDetalles(plantilla.id).map { dia ->
                            PlantillaDiaDto(
                                diaSemana = dia.diaSemana,
                                horaEntrada = dia.horaEntrada,
                                horaSalida = dia.horaSalida,
                                esDiaSiguiente = dia.esDiaSiguiente,
                            )
                        },
                    )
                } } ?: emptyList(),
                plantillasMes = db?.let { actualDb -> actualDb.plantillaMesDao().getAllByTrabajo(trabajo.id).map { plantilla ->
                    PlantillaMesDto(
                        id = plantilla.id,
                        nombre = plantilla.nombre,
                        descripcion = plantilla.descripcion,
                        plantillaSemanaDefaultId = plantilla.plantillaSemanaDefaultId,
                        overrides = actualDb.plantillaMesDao().getDetalles(plantilla.id).map { detalle ->
                            PlantillaMesOverrideDto(
                                semanaDelMes = detalle.semanaDelMes,
                                plantillaSemanaId = detalle.plantillaSemanaId,
                                esSemanaLibre = detalle.esSemanaLibre,
                            )
                        },
                    )
                } } ?: emptyList(),
            )
        }
        return json.encodeToString(BackupDto.serializer(), BackupDto(trabajos = trabajosDto))
    }

    suspend fun importarTodo(contenidoJson: String) {
        val backup = json.decodeFromString(BackupDto.serializer(), contenidoJson)
        require(backup.version <= CURRENT_BACKUP_VERSION) {
            "Backup version ${backup.version} is newer than supported version $CURRENT_BACKUP_VERSION"
        }
        val trabajos = mutableListOf<TrabajoEntity>()
        val entradas = mutableListOf<List<EntradaHorasEntity>>()
        val tarifas = mutableListOf<List<TarifaMensualEntity>>()
        val dinerosExtra = mutableListOf<List<DineroExtraEntity>>()
        val plantillasSemana = mutableListOf<List<PlantillaSemanaBackupImport>>()
        val plantillasMes = mutableListOf<List<PlantillaMesBackupImport>>()
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
            plantillasSemana.add(trabajoDto.plantillasSemana.map { it.toImport() })
            plantillasMes.add(trabajoDto.plantillasMes.map { it.toImport() })
        }
        importarTrabajosCompletos(trabajos, entradas, tarifas, dinerosExtra, plantillasSemana, plantillasMes)
    }

    private companion object {
        const val CURRENT_BACKUP_VERSION = 4
    }
}

private fun PlantillaSemanaDto.toImport() = PlantillaSemanaBackupImport(
    entity = PlantillaSemanaEntity(
        id = id,
        trabajoId = 0L,
        nombre = nombre,
        descripcion = descripcion,
    ),
    detalles = dias.map { dia ->
        PlantillaSemanaDetalleEntity(
            plantillaSemanaId = id,
            diaSemana = dia.diaSemana,
            horaEntrada = dia.horaEntrada,
            horaSalida = dia.horaSalida,
            esDiaSiguiente = dia.esDiaSiguiente,
        )
    },
)

private fun PlantillaMesDto.toImport() = PlantillaMesBackupImport(
    entity = PlantillaMesEntity(
        id = id,
        trabajoId = 0L,
        nombre = nombre,
        descripcion = descripcion,
        plantillaSemanaDefaultId = plantillaSemanaDefaultId,
    ),
    detalles = overrides.map { override ->
        PlantillaMesDetalleEntity(
            plantillaMesId = id,
            semanaDelMes = override.semanaDelMes,
            plantillaSemanaId = override.plantillaSemanaId,
            esSemanaLibre = override.esSemanaLibre,
        )
    },
)

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
