package com.horastrabajo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.horastrabajo.app.data.local.entity.PlantillaMesDetalleEntity
import com.horastrabajo.app.data.local.entity.PlantillaMesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantillaMesDao {
    @Query("SELECT * FROM plantilla_mes WHERE trabajo_id = :trabajoId ORDER BY nombre ASC")
    fun getPlantillasPorTrabajo(trabajoId: Long): Flow<List<PlantillaMesEntity>>

    @Query("SELECT * FROM plantilla_mes WHERE id = :id")
    suspend fun getById(id: Long): PlantillaMesEntity?

    @Query("SELECT * FROM plantilla_mes_detalle WHERE plantilla_mes_id = :plantillaMesId")
    suspend fun getDetalles(plantillaMesId: Long): List<PlantillaMesDetalleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plantilla: PlantillaMesEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetalles(detalles: List<PlantillaMesDetalleEntity>)

    @Query("DELETE FROM plantilla_mes_detalle WHERE plantilla_mes_id = :plantillaMesId")
    suspend fun deleteDetalles(plantillaMesId: Long)

    @Delete
    suspend fun delete(plantilla: PlantillaMesEntity)

    @Transaction
    suspend fun saveConDetalles(entity: PlantillaMesEntity, detalles: List<PlantillaMesDetalleEntity>): Long {
        val id = if (entity.id == 0L) insert(entity) else { insert(entity); entity.id }
        deleteDetalles(id)
        insertDetalles(detalles.map { it.copy(plantillaMesId = id) })
        return id
    }
}
