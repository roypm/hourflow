package com.horastrabajo.app.data.repository

import com.horastrabajo.app.data.local.entity.DineroExtraEntity
import com.horastrabajo.app.data.local.entity.EntradaHorasEntity
import com.horastrabajo.app.data.local.entity.TarifaMensualEntity
import com.horastrabajo.app.data.local.entity.TrabajoEntity
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.TarifaMensual
import com.horastrabajo.app.domain.model.Trabajo

fun TrabajoEntity.toDomain() = Trabajo(
    id = id,
    nombre = nombre,
    nombreUsuario = nombreUsuario,
    simboloMoneda = simboloMoneda,
    fotoUri = fotoUri,
)

fun Trabajo.toEntity() = TrabajoEntity(
    id = id,
    nombre = nombre,
    nombreUsuario = nombreUsuario,
    simboloMoneda = simboloMoneda,
    fotoUri = fotoUri,
)

fun EntradaHorasEntity.toDomain() = EntradaHoras(
    id = id,
    trabajoId = trabajoId,
    fecha = fecha,
    horaEntrada = horaEntrada,
    horaSalida = horaSalida,
    esDiaSiguiente = esDiaSiguiente,
    notas = notas,
    precioPorHoraCustom = precioPorHoraCustomCentimos?.let { Dinero(it) },
)

fun EntradaHoras.toEntity() = EntradaHorasEntity(
    id = id,
    trabajoId = trabajoId,
    fecha = fecha,
    horaEntrada = horaEntrada,
    horaSalida = horaSalida,
    esDiaSiguiente = esDiaSiguiente,
    notas = notas,
    precioPorHoraCustomCentimos = precioPorHoraCustom?.centimos,
)

fun TarifaMensualEntity.toDomain() = TarifaMensual(
    id = id,
    trabajoId = trabajoId,
    anio = anio,
    mes = mes,
    precioPorHora = Dinero(precioPorHoraCentimos),
)

fun TarifaMensual.toEntity() = TarifaMensualEntity(
    id = id,
    trabajoId = trabajoId,
    anio = anio,
    mes = mes,
    precioPorHoraCentimos = precioPorHora.centimos,
)

fun DineroExtraEntity.toDomain() = DineroExtra(
    id = id,
    trabajoId = trabajoId,
    fecha = fecha,
    monto = Dinero(montoCentimos),
    descripcion = descripcion,
)

fun DineroExtra.toEntity() = DineroExtraEntity(
    id = id,
    trabajoId = trabajoId,
    fecha = fecha,
    montoCentimos = monto.centimos,
    descripcion = descripcion,
)
