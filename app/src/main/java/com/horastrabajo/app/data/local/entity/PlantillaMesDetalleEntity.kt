package com.horastrabajo.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plantilla_mes_detalle",
    foreignKeys = [ForeignKey(
        entity = PlantillaMesEntity::class,
        parentColumns = ["id"],
        childColumns = ["plantilla_mes_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("plantilla_mes_id")]
)
data class PlantillaMesDetalleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "plantilla_mes_id") val plantillaMesId: Long,
    @ColumnInfo(name = "semana_del_mes") val semanaDelMes: Int,
    @ColumnInfo(name = "plantilla_semana_id") val plantillaSemanaId: Long? = null,
    @ColumnInfo(name = "es_semana_libre") val esSemanaLibre: Boolean = false
)
