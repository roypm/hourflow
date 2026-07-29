package com.horastrabajo.app.data.repository

import com.horastrabajo.app.data.local.dao.DineroExtraDao
import com.horastrabajo.app.domain.model.DineroExtra
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

interface DineroExtraRepository {
    fun observePorMes(trabajoId: Long, anio: Int, mes: Int): Flow<List<DineroExtra>>

    /** Todo el dinero extra de [anio] (los 12 meses), para dashboards/resúmenes anuales. */
    fun observePorAnio(trabajoId: Long, anio: Int): Flow<List<DineroExtra>>
    fun observeTodo(): Flow<List<DineroExtra>>
    suspend fun guardar(dineroExtra: DineroExtra): Long
    suspend fun eliminar(dineroExtra: DineroExtra)

    /** Todo el dinero extra de un trabajo, sin límite de fecha. Solo para backup/export. */
    suspend fun obtenerTodoDelTrabajo(trabajoId: Long): List<DineroExtra>
    suspend fun guardarVarios(dineroExtra: List<DineroExtra>)

    /** Reinserta un dinero extra previamente borrado, preservando su ID original. */
    suspend fun restaurar(item: DineroExtra)

    /** Borra todo el dinero extra de un trabajo en un rango de fechas. */
    suspend fun deletePorMes(trabajoId: Long, anio: Int, mes: Int)
}

class DineroExtraRepositoryImpl(private val dao: DineroExtraDao) : DineroExtraRepository {

    override fun observePorMes(trabajoId: Long, anio: Int, mes: Int): Flow<List<DineroExtra>> {
        val yearMonth = runCatching { YearMonth.of(anio, mes) }.getOrNull() ?: return emptyFlow()
        return dao.observeByTrabajoYRango(trabajoId, yearMonth.atDay(1), yearMonth.atEndOfMonth())
            .map { entidades -> entidades.map { it.toDomain() } }
    }

    override fun observePorAnio(trabajoId: Long, anio: Int): Flow<List<DineroExtra>> {
        val desde = runCatching { LocalDate.of(anio, 1, 1) }.getOrNull() ?: return emptyFlow()
        val hasta = runCatching { LocalDate.of(anio, 12, 31) }.getOrNull() ?: return emptyFlow()
        return dao.observeByTrabajoYRango(trabajoId, desde, hasta)
            .map { entidades -> entidades.map { it.toDomain() } }
    }

    override fun observeTodo(): Flow<List<DineroExtra>> =
        dao.observeAll().map { entidades -> entidades.map { it.toDomain() } }

    override suspend fun guardar(dineroExtra: DineroExtra): Long =
        if (dineroExtra.id == 0L) dao.insert(dineroExtra.toEntity()) else {
            dao.update(dineroExtra.toEntity())
            dineroExtra.id
        }

    override suspend fun eliminar(dineroExtra: DineroExtra) = dao.delete(dineroExtra.toEntity())

    override suspend fun obtenerTodoDelTrabajo(trabajoId: Long): List<DineroExtra> =
        dao.getAllByTrabajo(trabajoId).map { it.toDomain() }

    override suspend fun guardarVarios(dineroExtra: List<DineroExtra>) =
        dao.insertAll(dineroExtra.map { it.toEntity() })

    override suspend fun restaurar(item: DineroExtra) {
        dao.insert(item.toEntity())
    }

    override suspend fun deletePorMes(trabajoId: Long, anio: Int, mes: Int) {
        val yearMonth = runCatching { YearMonth.of(anio, mes) }.getOrNull() ?: return
        dao.deleteByTrabajoYRango(trabajoId, yearMonth.atDay(1), yearMonth.atEndOfMonth())
    }
}
