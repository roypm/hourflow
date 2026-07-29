package com.horastrabajo.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tarifa por hora de un [trabajoId] para un mes concreto ([anio]/[mes], mes en 1..12).
 * [precioPorHoraCentimos] se guarda en céntimos (unidad entera) para evitar errores
 * de redondeo de coma flotante en el dinero.
 */
@Entity(
    tableName = "tarifa_mensual",
    foreignKeys = [
        ForeignKey(
            entity = TrabajoEntity::class,
            parentColumns = ["id"],
            childColumns = ["trabajoId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["trabajoId", "anio", "mes"], unique = true)],
)
data class TarifaMensualEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trabajoId: Long,
    val anio: Int,
    val mes: Int,
    val precioPorHoraCentimos: Long,
)
