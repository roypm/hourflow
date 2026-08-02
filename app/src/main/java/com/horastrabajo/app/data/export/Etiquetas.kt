package com.horastrabajo.app.data.export

import android.content.res.Resources
import com.horastrabajo.app.R
import com.horastrabajo.app.domain.ResumenMensual
import com.horastrabajo.app.domain.model.Dinero

/**
 * Etiquetas de texto que dibujan los renderizadores (puros JVM). Se resuelven desde
 * `R.string` en la llamada de exportación para que los renderizadores no dependan de Android.
 */
data class Etiquetas(
    val pieDeMarca: String,
    val tarifa: String,
    val seccionHoras: String,
    val seccionDineroExtra: String,
    val totalHorasMes: String,
    val totalDineroTurnos: String,
    val totalDineroExtra: String,
    val dineroExtraMes: String,
    val totalACobrar: String,
    val totalHoras: String,
    val nocturno: String,
    val tarifaCustom: String,
    val pagina: String,
    val columnaFecha: String,
    val columnaDia: String,
    val columnaHorario: String,
    val columnaDetalles: String,
    val columnaHoras: String,
    val columnaDinero: String,
    val columnaConcepto: String,
    val columnaImporte: String,
)

/** Construye las etiquetas para el resumen dado. La tarifa muestra 0 si no está configurada. */
fun etiquetasExport(recursos: Resources, resumen: ResumenMensual): Etiquetas {
    val simbolo = resumen.trabajo.simboloMoneda
    val tarifaValor = recursos.getString(
        R.string.export_por_hora,
        (resumen.precioPorHora ?: Dinero.CERO).formateado(simbolo),
    )
    return Etiquetas(
        pieDeMarca = recursos.getString(R.string.export_pie_de_marca),
        tarifa = recursos.getString(R.string.export_tarifa, tarifaValor),
        seccionHoras = recursos.getString(R.string.export_horas_del_mes),
        seccionDineroExtra = recursos.getString(R.string.export_seccion_dinero_extra),
        totalHorasMes = recursos.getString(R.string.export_total_horas_mes),
        totalDineroTurnos = recursos.getString(R.string.export_total_dinero_turnos),
        totalDineroExtra = recursos.getString(R.string.export_total_dinero_extra),
        dineroExtraMes = recursos.getString(R.string.export_dinero_extra_mes),
        totalACobrar = recursos.getString(R.string.export_total_a_cobrar),
        totalHoras = recursos.getString(R.string.export_total_horas),
        nocturno = recursos.getString(R.string.export_nocturno),
        tarifaCustom = recursos.getString(R.string.export_tarifa_custom),
        pagina = recursos.getString(R.string.export_pagina),
        columnaFecha = recursos.getString(R.string.export_columna_fecha),
        columnaDia = recursos.getString(R.string.export_columna_dia),
        columnaHorario = recursos.getString(R.string.export_columna_horario),
        columnaDetalles = recursos.getString(R.string.export_columna_detalles),
        columnaHoras = recursos.getString(R.string.export_columna_horas),
        columnaDinero = recursos.getString(R.string.export_columna_dinero),
        columnaConcepto = recursos.getString(R.string.export_columna_concepto),
        columnaImporte = recursos.getString(R.string.export_columna_importe),
    )
}
