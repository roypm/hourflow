package com.horastrabajo.app.domain.model

import java.time.LocalDate

data class DineroExtra(
    val id: Long = 0,
    val trabajoId: Long,
    val fecha: LocalDate,
    val monto: Dinero,
    val descripcion: String,
)
