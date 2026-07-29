package com.horastrabajo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.horastrabajo.app.data.local.entity.TrabajoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrabajoDao {

    @Query("SELECT * FROM trabajo ORDER BY nombre ASC")
    fun observeAll(): Flow<List<TrabajoEntity>>

    @Query("SELECT * FROM trabajo WHERE id = :trabajoId")
    fun observeById(trabajoId: Long): Flow<TrabajoEntity?>

    @Query("SELECT * FROM trabajo WHERE id = :trabajoId")
    suspend fun getById(trabajoId: Long): TrabajoEntity?

    @Insert
    suspend fun insert(trabajo: TrabajoEntity): Long

    @Update
    suspend fun update(trabajo: TrabajoEntity)

    @Delete
    suspend fun delete(trabajo: TrabajoEntity)
}
