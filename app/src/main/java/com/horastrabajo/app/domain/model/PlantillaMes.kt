package com.horastrabajo.app.domain.model

data class PlantillaMes(
    val id: Long,
    val trabajoId: Long,
    val nombre: String,
    val descripcion: String,
    val plantillaSemanaDefault: PlantillaSemana?,
    val overridesPorSemana: Map<Int, PlantillaSemana?>
)
