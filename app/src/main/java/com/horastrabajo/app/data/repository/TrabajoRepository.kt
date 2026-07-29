package com.horastrabajo.app.data.repository

import com.horastrabajo.app.data.local.dao.TrabajoDao
import com.horastrabajo.app.domain.model.Trabajo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface TrabajoRepository {
    fun observeAll(): Flow<List<Trabajo>>
    fun observeById(trabajoId: Long): Flow<Trabajo?>
    suspend fun guardar(trabajo: Trabajo): Long
    suspend fun eliminar(trabajo: Trabajo)
}

class TrabajoRepositoryImpl(private val dao: TrabajoDao) : TrabajoRepository {

    override fun observeAll(): Flow<List<Trabajo>> =
        dao.observeAll().map { entidades -> entidades.map { it.toDomain() } }

    override fun observeById(trabajoId: Long): Flow<Trabajo?> =
        dao.observeById(trabajoId).map { it?.toDomain() }

    override suspend fun guardar(trabajo: Trabajo): Long =
        if (trabajo.id == 0L) dao.insert(trabajo.toEntity()) else {
            dao.update(trabajo.toEntity())
            trabajo.id
        }

    override suspend fun eliminar(trabajo: Trabajo) = dao.delete(trabajo.toEntity())
}
