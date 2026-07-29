package com.horastrabajo.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

/**
 * Una entrada de horas de un turno. [fecha] es siempre el día en que empezó el turno:
 * si [esDiaSiguiente] es true, [horaSalida] cae en el día natural siguiente a [fecha]
 * (turno que cruza la medianoche, ej. entra 20:00, sale 02:00).
 */
@Entity(
    tableName = "entrada_horas",
    foreignKeys = [
        ForeignKey(
            entity = TrabajoEntity::class,
            parentColumns = ["id"],
            childColumns = ["trabajoId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("trabajoId"), Index("fecha")],
)
data class EntradaHorasEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trabajoId: Long,
    val fecha: LocalDate,
    val horaEntrada: LocalTime,
    val horaSalida: LocalTime,
    val esDiaSiguiente: Boolean,
    val notas: String? = null,
    val precioPorHoraCustomCentimos: Long? = null,
)
