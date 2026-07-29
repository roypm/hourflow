package com.horastrabajo.app.data.repository

import com.horastrabajo.app.data.local.dao.PlantillaMesDao
import com.horastrabajo.app.data.local.dao.PlantillaSemanaDao
import com.horastrabajo.app.data.local.entity.PlantillaMesDetalleEntity
import com.horastrabajo.app.data.local.entity.PlantillaSemanaDetalleEntity
import com.horastrabajo.app.domain.model.PlantillaDia
import com.horastrabajo.app.domain.model.PlantillaMes
import com.horastrabajo.app.domain.model.PlantillaSemana
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlantillaRepository(
    private val semanaDao: PlantillaSemanaDao,
    private val mesDao: PlantillaMesDao
) {
    fun getPlantillasSemana(trabajoId: Long): Flow<List<PlantillaSemana>> =
        semanaDao.getPlantillasPorTrabajo(trabajoId).map { entities ->
            entities.map { entity ->
                val detalles = semanaDao.getDetalles(entity.id)
                entity.toDomain(detalles)
            }
        }

    suspend fun savePlantillaSemana(plantilla: PlantillaSemana) {
        val entity = plantilla.toEntity()
        val detalles = plantilla.dias.map { it.toEntity(0) }
        semanaDao.saveConDetalles(entity, detalles)
    }

    suspend fun deletePlantillaSemana(plantilla: PlantillaSemana) =
        semanaDao.delete(plantilla.toEntity())

    suspend fun getPlantillaSemanaById(id: Long): PlantillaSemana? {
        val entity = semanaDao.getById(id) ?: return null
        val detalles = semanaDao.getDetalles(id)
        return entity.toDomain(detalles)
    }

    fun getPlantillasMes(trabajoId: Long): Flow<List<PlantillaMes>> =
        mesDao.getPlantillasPorTrabajo(trabajoId).map { entities ->
            entities.map { entity -> buildPlantillaMesDomain(entity) }
        }

    suspend fun savePlantillaMes(plantilla: PlantillaMes) {
        val entity = plantilla.toEntity()
        val detalles = plantilla.overridesPorSemana.map { (semana, ps) ->
            PlantillaMesDetalleEntity(
                plantillaMesId = 0,
                semanaDelMes = semana,
                plantillaSemanaId = ps?.id,
                esSemanaLibre = ps == null
            )
        }
        mesDao.saveConDetalles(entity, detalles)
    }

    suspend fun deletePlantillaMes(plantilla: PlantillaMes) =
        mesDao.delete(plantilla.toEntity())

    private suspend fun buildPlantillaMesDomain(entity: com.horastrabajo.app.data.local.entity.PlantillaMesEntity): PlantillaMes {
        val detalles = mesDao.getDetalles(entity.id)
        val default = entity.plantillaSemanaDefaultId?.let { getPlantillaSemanaById(it) }
        val overrides = detalles.associate { d ->
            d.semanaDelMes to if (d.esSemanaLibre) null
            else d.plantillaSemanaId?.let { getPlantillaSemanaById(it) }
        }
        return PlantillaMes(
            id = entity.id,
            trabajoId = entity.trabajoId,
            nombre = entity.nombre,
            descripcion = entity.descripcion,
            plantillaSemanaDefault = default,
            overridesPorSemana = overrides
        )
    }
}

private fun com.horastrabajo.app.data.local.entity.PlantillaSemanaEntity.toDomain(
    detalles: List<PlantillaSemanaDetalleEntity>
): PlantillaSemana {
    val dias = (1..7).map { diaSemana ->
        detalle@ detalles.firstOrNull { it.diaSemana == diaSemana }?.let { d ->
            PlantillaDia(
                diaSemana = d.diaSemana,
                horaEntrada = d.horaEntrada,
                horaSalida = d.horaSalida,
                esDiaSiguiente = d.esDiaSiguiente
            )
        } ?: PlantillaDia(
            diaSemana = diaSemana,
            horaEntrada = null,
            horaSalida = null,
            esDiaSiguiente = false
        )
    }
    return PlantillaSemana(
        id = id,
        trabajoId = trabajoId,
        nombre = nombre,
        descripcion = descripcion,
        dias = dias
    )
}

private fun PlantillaSemana.toEntity() = com.horastrabajo.app.data.local.entity.PlantillaSemanaEntity(
    id = id,
    trabajoId = trabajoId,
    nombre = nombre,
    descripcion = descripcion
)

private fun PlantillaDia.toEntity(plantillaSemanaId: Long) = PlantillaSemanaDetalleEntity(
    plantillaSemanaId = plantillaSemanaId,
    diaSemana = diaSemana,
    horaEntrada = horaEntrada,
    horaSalida = horaSalida,
    esDiaSiguiente = esDiaSiguiente
)

private fun PlantillaMes.toEntity() = com.horastrabajo.app.data.local.entity.PlantillaMesEntity(
    id = id,
    trabajoId = trabajoId,
    nombre = nombre,
    descripcion = descripcion,
    plantillaSemanaDefaultId = plantillaSemanaDefault?.id
)
