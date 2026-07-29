package com.horastrabajo.app.data.export

import android.content.Context
import com.horastrabajo.app.R
import com.horastrabajo.app.domain.ResumenMensual
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.formatearHoras
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")
private val FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM")

internal fun estiloFilaTotalResumen(): EstiloFila = EstiloFila.TITULO

enum class EstiloFila { TITULO, NORMAL, NEGRITA }

data class FilaResumen(
    val izquierda: String = "",
    val derecha: String = "",
    val estilo: EstiloFila = EstiloFila.NORMAL,
)

/**
 * Construye las filas de texto del resumen, compartidas entre la pantalla de Resumen y los
 * exportadores de PDF y JPEG.
 *
 * [context] determina el idioma: se le pasa el contexto ya localizado de la UI (ver
 * `ProveedorIdioma`), de modo que el documento exportado sale en el mismo idioma que la
 * pantalla desde la que se exporta.
 */
fun construirFilasResumen(resumen: ResumenMensual, context: Context): List<FilaResumen> {
    val simbolo = resumen.trabajo.simboloMoneda
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val nombreMes = java.time.Month.of(resumen.mes).getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { it.uppercase(locale) }
    val filas = mutableListOf<FilaResumen>()

    val encabezado = context.getString(
        R.string.export_encabezado,
        resumen.trabajo.nombreUsuario,
        resumen.trabajo.nombre,
        nombreMes,
        resumen.anio,
    )
    filas += FilaResumen(encabezado, estilo = EstiloFila.TITULO)

    val tarifaTexto = resumen.precioPorHora
        ?.let { context.getString(R.string.export_por_hora, it.formateado(simbolo)) }
        ?: context.getString(R.string.mes_tarifa_sin_fijar)
    filas += FilaResumen(context.getString(R.string.export_tarifa, tarifaTexto))
    filas += FilaResumen("")
    filas += FilaResumen(context.getString(R.string.export_horas_del_mes), estilo = EstiloFila.TITULO)

    for (dia in resumen.dias) {
        val nombreDia = dia.fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            .lowercase(locale).removeSuffix(".")

        for (entrada in dia.entradas) {
            val base = context.getString(
                R.string.export_entrada,
                dia.fecha.format(FORMATO_DIA),
                nombreDia,
                entrada.horaEntrada.format(FORMATO_HORA),
                entrada.horaSalida.format(FORMATO_HORA),
            )
            val sufijo = if (entrada.esDiaSiguiente) " " + context.getString(R.string.entrada_sufijo_dia_siguiente) else ""
            val tarifaCustomTexto = entrada.precioPorHoraCustom
                ?.let { " " + context.getString(R.string.export_tarifa_custom, it.formateado(simbolo)) }
                ?: ""
            val notas = entrada.notas?.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""
            val tarifaEfectiva = entrada.precioPorHoraCustom ?: resumen.precioPorHora
            val dineroTexto = tarifaEfectiva?.let { Dinero.porHoras(entrada.horasDecimal, it).formateado(simbolo) }
            val derecha = formatearHoras(entrada.horasDecimal) + (dineroTexto?.let { " · $it" } ?: "")
            filas += FilaResumen("$base$sufijo$tarifaCustomTexto$notas", derecha)
        }
    }

    filas += FilaResumen("")
    filas += FilaResumen(
        context.getString(R.string.export_total_horas_mes),
        if (resumen.dineroExtra.isNotEmpty()) {
            formatearHoras(resumen.totalHoras) + " · " + resumen.totalDineroHoras.formateado(simbolo)
        } else {
            formatearHoras(resumen.totalHoras)
        },
        estiloFilaTotalResumen(),
    )

    if (resumen.dineroExtra.isNotEmpty()) {
        filas += FilaResumen("")
        filas += FilaResumen(context.getString(R.string.dinero_extra_titulo), estilo = EstiloFila.TITULO)
        for (extra in resumen.dineroExtra) {
            filas += FilaResumen(
                context.getString(R.string.export_fila_dinero_extra, extra.fecha.format(FORMATO_DIA), extra.descripcion),
                extra.monto.formateado(simbolo),
            )
        }
        filas += FilaResumen("")
        filas += FilaResumen(
            context.getString(R.string.export_total_dinero_extra),
            resumen.totalDineroExtra.formateado(simbolo),
            estiloFilaTotalResumen(),
        )
    }

    filas += FilaResumen("")
    filas += FilaResumen(
        context.getString(R.string.export_total_a_cobrar),
        resumen.totalDinero.formateado(simbolo),
        EstiloFila.TITULO,
    )

    return filas
}
