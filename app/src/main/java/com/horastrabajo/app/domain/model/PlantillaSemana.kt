package com.horastrabajo.app.domain.model

data class PlantillaSemana(
    val id: Long,
    val trabajoId: Long,
    val nombre: String,
    val descripcion: String,
    val dias: List<PlantillaDia>
)

data class PlantillaDia(
    val diaSemana: Int,
    val horaEntrada: String?,
    val horaSalida: String?,
    val esDiaSiguiente: Boolean
)
