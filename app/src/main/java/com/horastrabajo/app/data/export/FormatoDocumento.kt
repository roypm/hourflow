package com.horastrabajo.app.data.export

import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM")
private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")

/** Fecha compacta sin año ni día de la semana, p. ej. `30/12`. */
fun formatearFechaCorta(fecha: LocalDate): String = fecha.format(FORMATO_FECHA)

/** Hora en formato 24h, p. ej. `08:00`. */
fun formatearHora(hora: LocalTime): String = hora.format(FORMATO_HORA)

/** Nombre corto del día de la semana en el [locale] dado, p. ej. `dl`. */
fun formatearDiaSemanaCorto(fecha: LocalDate, locale: Locale): String =
    fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)

/** Mes completo y año, p. ej. `Juliol 2026`. */
fun formatearMesAnio(mes: Int, anio: Int, locale: Locale): String =
    "${Month.of(mes).getDisplayName(TextStyle.FULL, locale)} $anio"
