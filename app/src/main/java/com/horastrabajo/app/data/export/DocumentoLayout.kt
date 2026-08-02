package com.horastrabajo.app.data.export

import com.horastrabajo.app.domain.ResumenMensual
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.formatearHoras
import java.time.LocalDate

const val MARGEN_DOCUMENTO = 36f
const val ALTO_PIE_DOCUMENTO = 18f
const val ALTO_CABECERA_COMPLETA = 92f
const val ALTO_CABECERA_CORTA = 42f
const val ALTO_FILA_TURNO = 20f
const val ALTO_CABECERA_TABLA = 20f
const val ALTO_FILA_EXTRA = 20f
const val ALTO_SECCION = 20f
const val ALTO_SUBTOTAL = 20f
const val ALTO_TOTAL = 28f

const val X_FECHA_DOCUMENTO = MARGEN_DOCUMENTO
const val X_DIA_DOCUMENTO = 84f
const val X_HORARIO_DOCUMENTO = 126f
const val X_DETALLES_DOCUMENTO = 212f
const val X_HORAS_DOCUMENTO = 434f
const val X_DINERO_DOCUMENTO = 492f
const val X_FIN_DOCUMENTO = 559f

sealed class ElementoDocumento(val alto: Float) {
    class Seccion(val texto: String) : ElementoDocumento(ALTO_SECCION)
    class CabeceraTabla(val extras: Boolean) : ElementoDocumento(ALTO_CABECERA_TABLA)
    class Turno(val dia: LocalDate, val entrada: EntradaHoras, val fila: Int, val continuacion: Boolean) : ElementoDocumento(ALTO_FILA_TURNO)
    class Extra(val item: DineroExtra, val fila: Int) : ElementoDocumento(ALTO_FILA_EXTRA)
    class Subtotal(val texto: String, val valor: String) : ElementoDocumento(ALTO_SUBTOTAL)
    class Total(val texto: String, val valor: String) : ElementoDocumento(ALTO_TOTAL)
}

fun construirElementosDocumento(resumen: ResumenMensual, etiquetas: Etiquetas): List<ElementoDocumento> {
    val elementos = mutableListOf<ElementoDocumento>()
    val turnos = resumen.dias.flatMap { dia -> dia.entradas.map { dia.fecha to it } }
    if (turnos.isNotEmpty()) {
        elementos += ElementoDocumento.Seccion(etiquetas.seccionHoras)
        elementos += ElementoDocumento.CabeceraTabla(extras = false)
        turnos.forEachIndexed { index, (dia, entrada) ->
            elementos += ElementoDocumento.Turno(
                dia = dia,
                entrada = entrada,
                fila = index,
                continuacion = index > 0 && turnos[index - 1].first == dia,
            )
        }
        elementos += ElementoDocumento.Subtotal(
            etiquetas.totalHorasMes,
            "${formatearHoras(resumen.totalHoras)} · ${etiquetas.totalDineroTurnos} ${resumen.totalDineroHoras.formateado(resumen.trabajo.simboloMoneda)}",
        )
    }
    if (resumen.dineroExtra.isNotEmpty()) {
        elementos += ElementoDocumento.Seccion(etiquetas.seccionDineroExtra)
        elementos += ElementoDocumento.CabeceraTabla(extras = true)
        resumen.dineroExtra
            .sortedWith(compareBy<DineroExtra> { it.fecha }.thenBy { it.id })
            .forEachIndexed { index, extra -> elementos += ElementoDocumento.Extra(extra, index) }
        elementos += ElementoDocumento.Subtotal(
            etiquetas.totalDineroExtra,
            resumen.totalDineroExtra.formateado(resumen.trabajo.simboloMoneda),
        )
    }
    elementos += ElementoDocumento.Total(
        etiquetas.totalACobrar,
        resumen.totalDinero.formateado(resumen.trabajo.simboloMoneda),
    )
    return elementos
}
