package com.horastrabajo.app.domain.model

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class EntradaHoras(
    val id: Long = 0,
    val trabajoId: Long,
    val fecha: LocalDate,
    val horaEntrada: LocalTime,
    val horaSalida: LocalTime,
    val esDiaSiguiente: Boolean,
    val notas: String? = null,
    /** Tarifa distinta a la del mes, solo para este turno (ej. horas extra a otro precio). */
    val precioPorHoraCustom: Dinero? = null,
) {
    val inicio: LocalDateTime get() = fecha.atTime(horaEntrada)

    val fin: LocalDateTime
        get() = if (esDiaSiguiente) fecha.plusDays(1).atTime(horaSalida) else fecha.atTime(horaSalida)

    val duracion: Duration get() = Duration.between(inicio, fin)

    val horasDecimal: Double get() = duracion.toMinutes() / 60.0
}
