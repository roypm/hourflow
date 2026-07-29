package com.horastrabajo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.horastrabajo.app.data.local.entity.PlantillaSemanaDetalleEntity
import com.horastrabajo.app.data.local.entity.PlantillaSemanaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantillaSemanaDao {
    @Query("SELECT * FROM plantilla_semana WHERE trabajo_id = :trabajoId ORDER BY nombre ASC")
    fun getPlantillasPorTrabajo(trabajoId: Long): Flow<List<PlantillaSemanaEntity>>

    @Query("SELECT * FROM plantilla_semana WHERE id = :id")
    suspend fun getById(id: Long): PlantillaSemanaEntity?

    @Query("SELECT * FROM plantilla_semana_detalle WHERE plantilla_semana_id = :plantillaId")
    suspend fun getDetalles(plantillaId: Long): List<PlantillaSemanaDetalleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plantilla: PlantillaSemanaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetalles(detalles: List<PlantillaSemanaDetalleEntity>)

    @Query("DELETE FROM plantilla_semana_detalle WHERE plantilla_semana_id = :plantillaId")
    suspend fun deleteDetalles(plantillaId: Long)

    @Delete
    suspend fun delete(plantilla: PlantillaSemanaEntity)

    @Transaction
    suspend fun saveConDetalles(entity: PlantillaSemanaEntity, detalles: List<PlantillaSemanaDetalleEntity>): Long {
        val id = if (entity.id == 0L) insert(entity) else { insert(entity); entity.id }
        deleteDetalles(id)
        insertDetalles(detalles.map { it.copy(plantillaSemanaId = id) })
        return id
    }
}
