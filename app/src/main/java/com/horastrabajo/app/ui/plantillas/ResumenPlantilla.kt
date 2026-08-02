package com.horastrabajo.app.ui.plantillas

import com.horastrabajo.app.domain.model.PlantillaSemana
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

data class ResumenSemanaAutomatico(
    val diasTrabajados: Int,
    val diasTexto: String,
    val horarioTexto: String?,
    val incluyeNocturnos: Boolean,
)

fun resumenSemanaAutomatico(plantilla: PlantillaSemana, locale: Locale): ResumenSemanaAutomatico {
    val dias = plantilla.dias.filter { it.horaEntrada != null }.sortedBy { it.diaSemana }
    val diasNumericos = dias.map { it.diaSemana }
    val diasTexto = when {
        diasNumericos.isEmpty() -> ""
        diasNumericos == (1..5).toList() -> nombreDia(1, locale) + "–" + nombreDia(5, locale)
        diasNumericos == (1..7).toList() -> nombreDia(1, locale) + "–" + nombreDia(7, locale)
        else -> dias.joinToString(", ") { nombreDia(it.diaSemana, locale) }
    }
    val horarios = dias.mapNotNull { dia ->
        if (dia.horaEntrada != null && dia.horaSalida != null) {
            "${dia.horaEntrada}–${dia.horaSalida}"
        } else {
            null
        }
    }.distinct()

    return ResumenSemanaAutomatico(
        diasTrabajados = dias.size,
        diasTexto = diasTexto,
        horarioTexto = horarios.singleOrNull(),
        incluyeNocturnos = dias.any { it.esDiaSiguiente },
    )
}

private fun nombreDia(numero: Int, locale: Locale): String =
    DayOfWeek.of(numero).getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")
