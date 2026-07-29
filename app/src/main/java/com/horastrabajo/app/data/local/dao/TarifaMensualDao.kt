package com.horastrabajo.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.horastrabajo.app.data.local.entity.TarifaMensualEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TarifaMensualDao {

    /**
     * Tarifa vigente más reciente en o antes de [anio]/[mes]: usada para heredar el precio
     * del mes anterior cuando el mes actual no tiene tarifa propia guardada.
     */
    @Query(
        "SELECT * FROM tarifa_mensual WHERE trabajoId = :trabajoId " +
            "AND (anio < :anio OR (anio = :anio AND mes <= :mes)) " +
            "ORDER BY anio DESC, mes DESC LIMIT 1"
    )
    suspend fun getVigenteEnOAntesDe(trabajoId: Long, anio: Int, mes: Int): TarifaMensualEntity?

    /** Todas las tarifas de un trabajo. Solo para backup/export. */
    @Query("SELECT * FROM tarifa_mensual WHERE trabajoId = :trabajoId")
    suspend fun getAllByTrabajo(trabajoId: Long): List<TarifaMensualEntity>

    /** Reactivo: para dashboards que necesitan enterarse cuando se fija/cambia una tarifa. */
    @Query("SELECT * FROM tarifa_mensual WHERE trabajoId = :trabajoId")
    fun observeAllByTrabajo(trabajoId: Long): Flow<List<TarifaMensualEntity>>

    @Query("SELECT * FROM tarifa_mensual ORDER BY trabajoId ASC, anio ASC, mes ASC")
    fun observeAll(): Flow<List<TarifaMensualEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tarifa: TarifaMensualEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tarifas: List<TarifaMensualEntity>)

    @Query("DELETE FROM tarifa_mensual WHERE trabajoId = :trabajoId AND anio = :anio AND mes = :mes")
    suspend fun deleteByMes(trabajoId: Long, anio: Int, mes: Int)
}
