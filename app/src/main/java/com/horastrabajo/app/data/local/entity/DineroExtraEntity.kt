package com.horastrabajo.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Ingreso o descuento extra (ej. propinas de repartos) ligado a un día concreto.
 * [montoCentimos] puede ser negativo (descuentos/adelantos).
 */
@Entity(
    tableName = "dinero_extra",
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
data class DineroExtraEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trabajoId: Long,
    val fecha: LocalDate,
    val montoCentimos: Long,
    val descripcion: String,
)
