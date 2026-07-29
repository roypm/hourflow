package com.horastrabajo.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plantilla_semana_detalle",
    foreignKeys = [ForeignKey(
        entity = PlantillaSemanaEntity::class,
        parentColumns = ["id"],
        childColumns = ["plantilla_semana_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("plantilla_semana_id")]
)
data class PlantillaSemanaDetalleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "plantilla_semana_id") val plantillaSemanaId: Long,
    @ColumnInfo(name = "dia_semana") val diaSemana: Int,
    @ColumnInfo(name = "hora_entrada") val horaEntrada: String? = null,
    @ColumnInfo(name = "hora_salida") val horaSalida: String? = null,
    @ColumnInfo(name = "es_dia_siguiente") val esDiaSiguiente: Boolean = false
)
