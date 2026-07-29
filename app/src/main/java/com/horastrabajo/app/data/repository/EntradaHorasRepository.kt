package com.horastrabajo.app.data.repository

import com.horastrabajo.app.data.local.dao.EntradaHorasDao
import com.horastrabajo.app.domain.model.EntradaHoras
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

interface EntradaHorasRepository {
    /**
     * Entradas cuyo turno EMPIEZA en [anio]/[mes]. Un turno que cruza de mes se asigna
     * entero al mes en que empezó (decisión de diseño), así que filtrar por [fecha] del
     * mes es suficiente y correcto.
     */
    fun observePorMes(trabajoId: Long, anio: Int, mes: Int): Flow<List<EntradaHoras>>

    /** Todas las entradas de [anio] (los 12 meses), para dashboards/resúmenes anuales. */
    fun observePorAnio(trabajoId: Long, anio: Int): Flow<List<EntradaHoras>>
    fun observeTodas(): Flow<List<EntradaHoras>>
    fun observePorFecha(trabajoId: Long, fecha: LocalDate): Flow<List<EntradaHoras>>
    suspend fun guardar(entrada: EntradaHoras): Long
    suspend fun eliminar(entrada: EntradaHoras)

    /** Todas las entradas de un trabajo, sin límite de fecha. Solo para backup/export. */
    suspend fun obtenerTodasDelTrabajo(trabajoId: Long): List<EntradaHoras>
    suspend fun guardarVarias(entradas: List<EntradaHoras>)

    /** Reinserta una entrada previamente borrada, preservando su ID original. */
    suspend fun restaurar(entrada: EntradaHoras)

    suspend fun getEntradasSemana(primerDiaSemana: LocalDate, trabajoId: Long): List<EntradaHoras>
    suspend fun getEntradasMes(anio: Int, mes: Int, trabajoId: Long): List<EntradaHoras>
    suspend fun deleteEntradasEnFechas(fechas: List<LocalDate>, trabajoId: Long)

    /** Borra las entradas en [fechas] e inserta [nuevas] en una sola transacción atómica. */
    suspend fun reemplazarEnFechas(trabajoId: Long, fechas: List<LocalDate>, nuevas: List<EntradaHoras>)

    /** Borra todas las entradas de un trabajo en un mes completo. */
    suspend fun deleteEntradasEnMes(trabajoId: Long, anio: Int, mes: Int)
}

class EntradaHorasRepositoryImpl(private val dao: EntradaHorasDao) : EntradaHorasRepository {

    override fun observePorMes(trabajoId: Long, anio: Int, mes: Int): Flow<List<EntradaHoras>> {
        val yearMonth = runCatching { YearMonth.of(anio, mes) }.getOrNull() ?: return emptyFlow()
        return dao.observeByTrabajoYRango(trabajoId, yearMonth.atDay(1), yearMonth.atEndOfMonth())
            .map { entidades -> entidades.map { it.toDomain() } }
    }

    override fun observePorAnio(trabajoId: Long, anio: Int): Flow<List<EntradaHoras>> {
        val desde = runCatching { LocalDate.of(anio, 1, 1) }.getOrNull() ?: return emptyFlow()
        val hasta = runCatching { LocalDate.of(anio, 12, 31) }.getOrNull() ?: return emptyFlow()
        return dao.observeByTrabajoYRango(trabajoId, desde, hasta)
            .map { entidades -> entidades.map { it.toDomain() } }
    }

    override fun observeTodas(): Flow<List<EntradaHoras>> =
        dao.observeAll().map { entidades -> entidades.map { it.toDomain() } }

    override fun observePorFecha(trabajoId: Long, fecha: LocalDate): Flow<List<EntradaHoras>> =
        dao.observeByTrabajoYFecha(trabajoId, fecha).map { entidades -> entidades.map { it.toDomain() } }

    override suspend fun guardar(entrada: EntradaHoras): Long =
        if (entrada.id == 0L) dao.insert(entrada.toEntity()) else {
            dao.update(entrada.toEntity())
            entrada.id
        }

    override suspend fun eliminar(entrada: EntradaHoras) = dao.delete(entrada.toEntity())

    override suspend fun obtenerTodasDelTrabajo(trabajoId: Long): List<EntradaHoras> =
        dao.getAllByTrabajo(trabajoId).map { it.toDomain() }

    override suspend fun guardarVarias(entradas: List<EntradaHoras>) =
        dao.insertAll(entradas.map { it.toEntity() })

    override suspend fun restaurar(entrada: EntradaHoras) {
        dao.insert(entrada.toEntity())
    }

    override suspend fun getEntradasSemana(primerDiaSemana: LocalDate, trabajoId: Long): List<EntradaHoras> {
        val ultimoDia = primerDiaSemana.plusDays(6)
        return dao.getByTrabajoYRango(trabajoId, primerDiaSemana, ultimoDia).map { it.toDomain() }
    }

    override suspend fun getEntradasMes(anio: Int, mes: Int, trabajoId: Long): List<EntradaHoras> {
        val yearMonth = runCatching { YearMonth.of(anio, mes) }.getOrNull() ?: return emptyList()
        return dao.getByTrabajoYRango(trabajoId, yearMonth.atDay(1), yearMonth.atEndOfMonth()).map { it.toDomain() }
    }

    override    suspend fun deleteEntradasEnFechas(fechas: List<LocalDate>, trabajoId: Long) =
        dao.deleteByTrabajoYFechas(trabajoId, fechas)

    override suspend fun deleteEntradasEnMes(trabajoId: Long, anio: Int, mes: Int) {
        val yearMonth = runCatching { YearMonth.of(anio, mes) }.getOrNull() ?: return
        dao.deleteByTrabajoYRango(trabajoId, yearMonth.atDay(1), yearMonth.atEndOfMonth())
    }

    override suspend fun reemplazarEnFechas(trabajoId: Long, fechas: List<LocalDate>, nuevas: List<EntradaHoras>) =
        dao.reemplazarPorFechas(trabajoId, fechas, nuevas.map { it.toEntity() })
}
