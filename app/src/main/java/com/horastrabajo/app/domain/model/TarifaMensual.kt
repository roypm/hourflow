package com.horastrabajo.app.domain.model

data class TarifaMensual(
    val id: Long = 0,
    val trabajoId: Long,
    val anio: Int,
    val mes: Int,
    val precioPorHora: Dinero,
)
