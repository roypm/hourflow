package com.horastrabajo.app.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.horastrabajo.app.domain.ResumenMensual
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.formatearHoras
import java.util.Locale

/**
 * Renderizador JPG (bàsic, una pàgina, fit-to-page).
 * Dibuixa directament sobre un Canvas a 150 dpi (1240×1754 px).
 */
object JpegRenderer {

    private val MARGEN_PX = (TamanosDocumento.MARGEN_PT * TamanosDocumento.ESCALA_JPG).toFloat()

    private val ANCHO_UTIL = TamanosDocumento.JPG_ANCHO_PX - 2 * MARGEN_PX

    fun dibujar(
        canvas: Canvas,
        resumen: ResumenMensual,
        etiquetas: Etiquetas,
        locale: Locale,
    ) {
        val paint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }

        canvas.drawColor(Color.WHITE)

        // --- PASS 1: mesurar alçada total ---
        val altoNecesario = mesurarAlcadaTotal(resumen, etiquetas, locale)
        val altoUtil = TamanosDocumento.JPG_ALTO_PX - 2 * MARGEN_PX
        val factor = minOf(1f, altoUtil / altoNecesario)

        // --- PASS 2: dibuixar amb factor ---
        canvas.save()
        canvas.translate(MARGEN_PX, MARGEN_PX)
        canvas.scale(factor, factor)

        var y = 0f

        // Capçalera
        y = dibujarCabecera(canvas, resumen, etiquetas, y)

        // Files de torns
        y = dibujarTorns(canvas, resumen, etiquetas, locale, y)

        // Bloc de totals
        y = dibujarTotales(canvas, resumen, etiquetas, y)

        // Peu
        dibujarPie(canvas, etiquetas, altoUtil - MARGEN_PX)

        canvas.restore()
    }

    private fun mesurarAlcadaTotal(
        resumen: ResumenMensual,
        etiquetas: Etiquetas,
        locale: Locale,
    ): Float {
        var h = 0f

        // Capçalera
        h += mesurarCabecera()

        // Torns
        h += resumen.dias.sumOf { it.entradas.size } * altoFila()

        // Totals (4 línies)
        h += 4 * altoLineaTotales()

        // Peu
        h += altoPie()

        // Marge inferior
        h += MARGEN_PX

        return h
    }

    private fun dibujarCabecera(
        canvas: Canvas,
        resumen: ResumenMensual,
        etiquetas: Etiquetas,
        y: Float,
    ): Float {
        val paint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            color = EstilosDocumento.COLOR_TEXTO
        }
        var y = y

        // Nom del treball
        paint.textSize = EstilosDocumento.TITULO_PT
        canvas.drawText(resumen.trabajo.nombre, 0f, y + EstilosDocumento.TITULO_PT, paint)

        // Treballador · Mes/Any
        val mesAnio = formatearMesAnio(resumen.anio, resumen.mes, Locale.getDefault())
        val subCabecera = "${resumen.trabajo.nombreUsuario} · $mesAnio"
        paint.textSize = EstilosDocumento.ENCABEZADO_PT
        y += EstilosDocumento.TITULO_PT + 4f
        canvas.drawText(subCabecera, 0f, y + EstilosDocumento.ENCABEZADO_PT, paint)

        // Tarifa
        paint.textSize = EstilosDocumento.TARIFA_PT
        paint.color = EstilosDocumento.COLOR_SECUNDARIO
        y += EstilosDocumento.ENCABEZADO_PT + 4f
        canvas.drawText(etiquetas.tarifa, 0f, y + EstilosDocumento.TARIFA_PT, paint)

        // Línia separadora
        y += EstilosDocumento.TARIFA_PT + 8f
        val linePaint = Paint().apply {
            color = EstilosDocumento.COLOR_LINEA
            strokeWidth = 1f
        }
        canvas.drawLine(0f, y, ANCHO_UTIL, y, linePaint)

        return y + 8f
    }

    private fun dibujarTorns(
        canvas: Canvas,
        resumen: ResumenMensual,
        etiquetas: Etiquetas,
        locale: Locale,
        y: Float,
    ): Float {
        val paint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            color = EstilosDocumento.COLOR_TEXTO
        }

        val filaH = altoFila()
        var y = y
        var filaIndex = 0

        resumen.dias.forEach { dia ->
            dia.entradas.forEach { entrada ->
                // Zebra
                if (filaIndex % 2 == 0) {
                    val zebraPaint = Paint().apply { color = EstilosDocumento.COLOR_ZEBRA }
                    canvas.drawRect(0f, y, ANCHO_UTIL, y + filaH, zebraPaint)
                }

                paint.color = EstilosDocumento.COLOR_TEXTO
                paint.textSize = EstilosDocumento.FILA_PT

                val fechaStr = formatearFechaCorta(dia.fecha)
                val rangoStr = "${formatearHora(entrada.horaEntrada)}-${formatearHora(entrada.horaSalida)}"
                val horasStr = formatearHoras(entrada.horasDecimal)
                val tarifaEfectiva = entrada.precioPorHoraCustom ?: resumen.precioPorHora ?: com.horastrabajo.app.domain.model.Dinero.CERO
                val dineroStr = com.horastrabajo.app.domain.model.Dinero.porHoras(entrada.horasDecimal, tarifaEfectiva).formateado(resumen.trabajo.simboloMoneda)

                // Columnes: data | rang | hores | diners
                val colFecha = 0f
                val colRango = ANCHO_UTIL * 0.25f
                val colHoras = ANCHO_UTIL * 0.65f
                val colDinero = ANCHO_UTIL * 0.85f

                // Data
                canvas.drawText(fechaStr, colFecha, y + filaH * 0.7f, paint)

                // Rang
                canvas.drawText(rangoStr, colRango, y + filaH * 0.7f, paint)

                // Hores (alineació dreta)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(horasStr, colHoras, y + filaH * 0.7f, paint)

                // Diners (alineació dreta)
                canvas.drawText(dineroStr, colDinero, y + filaH * 0.7f, paint)

                paint.textAlign = Paint.Align.LEFT

                // * si tarifa custom
                if (entrada.precioPorHoraCustom != null) {
                    paint.color = EstilosDocumento.COLOR_SECUNDARIO
                    canvas.drawText("* ", colFecha - 12f, y + filaH * 0.7f, paint)
                    paint.color = EstilosDocumento.COLOR_TEXTO
                }

                y += filaH
                filaIndex++
            }
        }

        // Línia separadora després dels torns
        val linePaint = Paint().apply {
            color = EstilosDocumento.COLOR_LINEA
            strokeWidth = 1f
        }
        canvas.drawLine(0f, y + 4f, ANCHO_UTIL, y + 4f, linePaint)

        return y + 8f
    }

    private fun dibujarTotales(
        canvas: Canvas,
        resumen: ResumenMensual,
        etiquetas: Etiquetas,
        y: Float,
    ): Float {
        val paint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            color = EstilosDocumento.COLOR_TEXTO
            textSize = EstilosDocumento.SUMA_PT
            textAlign = Paint.Align.LEFT
        }
        var y = y + 8f

        val simbolo = resumen.trabajo.simboloMoneda

        // Total hores
        val totalHorasStr = formatearHoras(resumen.totalHoras)
        canvas.drawText("${etiquetas.totalHoras} $totalHorasStr", 0f, y + EstilosDocumento.SUMA_PT, paint)

        // Diners dels torns
        val dineroTorns = resumen.totalDineroHoras.formateado(simbolo)
        paint.textSize = EstilosDocumento.SUMA_PT
        canvas.drawText("${etiquetas.totalDineroTurnos} $dineroTorns", 0f, y + 2 * EstilosDocumento.SUMA_PT, paint)

        // Diners extra
        val dineroExtra = resumen.totalDineroExtra.formateado(simbolo)
        canvas.drawText("${etiquetas.dineroExtraMes} $dineroExtra", 0f, y + 3 * EstilosDocumento.SUMA_PT, paint)

        // Total a cobrar
        paint.textSize = EstilosDocumento.TOTAL_PT
        paint.color = EstilosDocumento.COLOR_TEXTO
        val totalCobrar = resumen.totalDinero.formateado(simbolo)
        canvas.drawText("${etiquetas.totalACobrar} $totalCobrar", 0f, y + 4 * EstilosDocumento.SUMA_PT + 4f, paint)

        return y + 4 * EstilosDocumento.SUMA_PT + 8f
    }

    private fun dibujarPie(canvas: Canvas, etiquetas: Etiquetas, y: Float) {
        val paint = Paint().apply {
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
            color = EstilosDocumento.COLOR_SECUNDARIO
            textSize = EstilosDocumento.PIE_PT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(etiquetas.pieDeMarca, ANCHO_UTIL / 2f, y, paint)
    }

    // --- Helpers de mesura ---
    private fun mesurarCabecera(): Float {
        return EstilosDocumento.TITULO_PT + EstilosDocumento.ENCABEZADO_PT + EstilosDocumento.TARIFA_PT + 32f
    }

    private fun altoFila(): Float = EstilosDocumento.FILA_PT * 1.6f

    private fun altoLineaTotales(): Float = EstilosDocumento.SUMA_PT * 1.4f

    private fun altoPie(): Float = EstilosDocumento.PIE_PT + 8f
}