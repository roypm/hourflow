package com.horastrabajo.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plantilla_semana",
    foreignKeys = [ForeignKey(
        entity = TrabajoEntity::class,
        parentColumns = ["id"],
        childColumns = ["trabajo_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trabajo_id")]
)
data class PlantillaSemanaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "trabajo_id") val trabajoId: Long,
    val nombre: String,
    val descripcion: String = ""
)
