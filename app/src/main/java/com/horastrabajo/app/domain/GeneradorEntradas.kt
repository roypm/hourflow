package com.horastrabajo.app.domain

import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.PlantillaMes
import com.horastrabajo.app.domain.model.PlantillaSemana
import java.time.DayOfWeek
import java.time.LocalDate

object GeneradorEntradas {

    fun generarSemana(
        plantilla: PlantillaSemana,
        primerDiaSemana: LocalDate,
        trabajoId: Long
    ): List<EntradaHoras> {
        return plantilla.dias
            .filter { it.horaEntrada != null && it.horaSalida != null }
            .map { dia ->
                val fecha = primerDiaSemana.plusDays((dia.diaSemana - 1).toLong())
                val partesEntrada = dia.horaEntrada!!.split(":")
                val partesSalida = dia.horaSalida!!.split(":")
                EntradaHoras(
                    id = 0,
                    trabajoId = trabajoId,
                    fecha = fecha,
                    horaEntrada = java.time.LocalTime.of(partesEntrada[0].toInt(), partesEntrada[1].toInt()),
                    horaSalida = java.time.LocalTime.of(partesSalida[0].toInt(), partesSalida[1].toInt()),
                    esDiaSiguiente = dia.esDiaSiguiente,
                    notas = "",
                    precioPorHoraCustom = null
                )
            }
    }

    fun generarMes(
        plantilla: PlantillaMes,
        anyo: Int,
        mes: Int,
        trabajoId: Long
    ): List<EntradaHoras> {
        val primerDia = LocalDate.of(anyo, mes, 1)
        val semanas = obtenerSemanasDelMes(primerDia)
        val resultado = mutableListOf<EntradaHoras>()

        semanas.forEachIndexed { index, primerDiaSemana ->
            val numeroSemana = index + 1
            val override = plantilla.overridesPorSemana[numeroSemana]

            if (plantilla.overridesPorSemana.containsKey(numeroSemana) && override == null) return@forEachIndexed

            val plantillaSemana = override ?: plantilla.plantillaSemanaDefault ?: return@forEachIndexed

            val entradasSemana = generarSemana(plantillaSemana, primerDiaSemana, trabajoId)
                .filter { it.fecha.monthValue == mes && it.fecha.year == anyo }

            resultado.addAll(entradasSemana)
        }

        return resultado
    }

    fun obtenerSemanasDelMes(primerDiaMes: LocalDate): List<LocalDate> {
        val ultimoDia = primerDiaMes.withDayOfMonth(primerDiaMes.lengthOfMonth())
        val semanas = mutableListOf<LocalDate>()
        var lunes = primerDiaMes.with(DayOfWeek.MONDAY)
        if (lunes.isAfter(primerDiaMes)) lunes = lunes.minusWeeks(1)

        while (!lunes.isAfter(ultimoDia)) {
            semanas.add(lunes)
            lunes = lunes.plusWeeks(1)
        }
        return semanas
    }

    fun detectarConflictos(
        aGenerar: List<EntradaHoras>,
        existentes: List<EntradaHoras>
    ): List<LocalDate> {
        val fechasExistentes = existentes.map { it.fecha }.toSet()
        return aGenerar.map { it.fecha }.filter { it in fechasExistentes }.distinct()
    }
}
