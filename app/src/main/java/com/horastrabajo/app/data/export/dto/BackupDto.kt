package com.horastrabajo.app.data.export.dto

import kotlinx.serialization.Serializable

/**
 * Formato de backup completo (todos los Trabajos y sus datos) para export/import JSON.
 * Las fechas/horas se guardan como texto ISO-8601 (LocalDate.toString() / LocalTime.toString())
 * para que el JSON sea legible y estable entre versiones de la app.
 */
@Serializable
data class BackupDto(
    val version: Int = 3,
    val trabajos: List<TrabajoDto>,
)

@Serializable
data class TrabajoDto(
    val id: Long,
    val nombre: String,
    // Default vacío para poder seguir importando backups (version 1) creados antes de este campo.
    val nombreUsuario: String = "",
    val simboloMoneda: String,
    val entradas: List<EntradaHorasDto>,
    val tarifas: List<TarifaMensualDto>,
    val dineroExtra: List<DineroExtraDto>,
)

@Serializable
data class EntradaHorasDto(
    val id: Long,
    val fechaIso: String,
    val horaEntradaIso: String,
    val horaSalidaIso: String,
    val esDiaSiguiente: Boolean,
    val notas: String? = null,
    // Default null para poder seguir importando backups (version < 3) creados antes de este campo.
    val precioPorHoraCustomCentimos: Long? = null,
)

@Serializable
data class TarifaMensualDto(
    val id: Long,
    val anio: Int,
    val mes: Int,
    val precioPorHoraCentimos: Long,
)

@Serializable
data class DineroExtraDto(
    val id: Long,
    val fechaIso: String,
    val montoCentimos: Long,
    val descripcion: String,
)
