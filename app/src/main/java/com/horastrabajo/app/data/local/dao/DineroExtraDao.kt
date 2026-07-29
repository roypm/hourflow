package com.horastrabajo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.horastrabajo.app.data.local.entity.DineroExtraEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DineroExtraDao {

    @Query(
        "SELECT * FROM dinero_extra WHERE trabajoId = :trabajoId AND fecha BETWEEN :desde AND :hasta " +
            "ORDER BY fecha ASC"
    )
    fun observeByTrabajoYRango(trabajoId: Long, desde: LocalDate, hasta: LocalDate): Flow<List<DineroExtraEntity>>

    @Query("SELECT * FROM dinero_extra ORDER BY trabajoId ASC, fecha ASC")
    fun observeAll(): Flow<List<DineroExtraEntity>>

    /** Todo el dinero extra de un trabajo, sin límite de fecha. Solo para backup/export. */
    @Query("SELECT * FROM dinero_extra WHERE trabajoId = :trabajoId")
    suspend fun getAllByTrabajo(trabajoId: Long): List<DineroExtraEntity>

    @Insert
    suspend fun insert(dineroExtra: DineroExtraEntity): Long

    @Insert
    suspend fun insertAll(dineroExtra: List<DineroExtraEntity>)

    @Update
    suspend fun update(dineroExtra: DineroExtraEntity)

    @Delete
    suspend fun delete(dineroExtra: DineroExtraEntity)

    @Query("DELETE FROM dinero_extra WHERE trabajoId = :trabajoId AND fecha BETWEEN :desde AND :hasta")
    suspend fun deleteByTrabajoYRango(trabajoId: Long, desde: LocalDate, hasta: LocalDate)
}
