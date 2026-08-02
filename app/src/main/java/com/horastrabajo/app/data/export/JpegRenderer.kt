package com.horastrabajo.app.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.horastrabajo.app.domain.ResumenMensual
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.formatearHoras
import java.util.Locale

/** Renderiza el mismo layout del PDF en una única imagen vertical larga. */
object JpegRenderer {
    fun altoNecesario(resumen: ResumenMensual, etiquetas: Etiquetas): Float {
        val elementos = construirElementosDocumento(resumen, etiquetas)
        return MARGEN_DOCUMENTO + ALTO_CABECERA_COMPLETA +
            elementos.sumOf { it.alto.toDouble() }.toFloat() +
            ALTO_PIE_DOCUMENTO + MARGEN_DOCUMENTO
    }

    fun dibujar(canvas: Canvas, resumen: ResumenMensual, etiquetas: Etiquetas, locale: Locale, logo: Bitmap?) {
        canvas.drawColor(Color.WHITE)
        val escala = TamanosDocumento.ESCALA_JPG
        canvas.save()
        // Las coordenadas compartidas ya incluyen el margen horizontal del PDF.
        // Solo desplazamos verticalmente para conservar el margen superior del JPG.
        canvas.translate(0f, MARGEN_DOCUMENTO * escala)
        canvas.scale(escala, escala)

        var y = dibujarCabecera(canvas, resumen, etiquetas, locale, logo)
        construirElementosDocumento(resumen, etiquetas).forEach { elemento ->
            y = dibujarElemento(canvas, elemento, resumen, etiquetas, locale, y)
        }
        dibujarPie(canvas, etiquetas, y + ALTO_PIE_DOCUMENTO)
        canvas.restore()
    }

    private fun dibujarElemento(canvas: Canvas, elemento: ElementoDocumento, resumen: ResumenMensual, etiquetas: Etiquetas, locale: Locale, y: Float): Float = when (elemento) {
        is ElementoDocumento.Seccion -> {
            texto(canvas, elemento.texto, X_FECHA_DOCUMENTO, y + 13f, EstilosDocumento.SECCION_PT, EstilosDocumento.COLOR_TEXTO)
            y + elemento.alto
        }
        is ElementoDocumento.CabeceraTabla -> {
            dibujarCabeceraTabla(canvas, etiquetas, elemento.extras, y)
            y + elemento.alto
        }
        is ElementoDocumento.Turno -> {
            if (elemento.fila % 2 == 0) dibujarZebra(canvas, y, elemento.alto)
            dibujarTurno(canvas, elemento, resumen, etiquetas, locale, y)
            y + elemento.alto
        }
        is ElementoDocumento.Extra -> {
            if (elemento.fila % 2 == 0) dibujarZebra(canvas, y, elemento.alto)
            dibujarExtra(canvas, elemento.item, resumen, y)
            y + elemento.alto
        }
        is ElementoDocumento.Subtotal -> {
            texto(canvas, "${elemento.texto}: ${elemento.valor}", X_FECHA_DOCUMENTO, y + 13f, EstilosDocumento.SUMA_PT, EstilosDocumento.COLOR_SECUNDARIO)
            y + elemento.alto
        }
        is ElementoDocumento.Total -> {
            texto(canvas, "${elemento.texto}: ${elemento.valor}", X_FECHA_DOCUMENTO, y + 17f, EstilosDocumento.TOTAL_PT, EstilosDocumento.COLOR_TEXTO)
            y + elemento.alto
        }
    }

    private fun dibujarCabeceraTabla(canvas: Canvas, etiquetas: Etiquetas, extras: Boolean, y: Float) {
        val p = paint(EstilosDocumento.NOTA_PT, EstilosDocumento.COLOR_SECUNDARIO)
        if (extras) {
            canvas.drawText(etiquetas.columnaFecha, X_FECHA_DOCUMENTO, y + 13f, p)
            canvas.drawText(etiquetas.columnaConcepto, X_HORARIO_DOCUMENTO, y + 13f, p)
            p.textAlign = Paint.Align.RIGHT
            canvas.drawText(etiquetas.columnaImporte, X_FIN_DOCUMENTO, y + 13f, p)
        } else {
            canvas.drawText(etiquetas.columnaFecha, X_FECHA_DOCUMENTO, y + 13f, p)
            canvas.drawText(etiquetas.columnaDia, X_DIA_DOCUMENTO, y + 13f, p)
            canvas.drawText(etiquetas.columnaHorario, X_HORARIO_DOCUMENTO, y + 13f, p)
            canvas.drawText(etiquetas.columnaDetalles, X_DETALLES_DOCUMENTO, y + 13f, p)
            p.textAlign = Paint.Align.RIGHT
            canvas.drawText(etiquetas.columnaHoras, X_HORAS_DOCUMENTO + 48f, y + 13f, p)
            canvas.drawText(etiquetas.columnaDinero, X_FIN_DOCUMENTO, y + 13f, p)
        }
        p.textAlign = Paint.Align.LEFT
        val linea = Paint().apply { color = EstilosDocumento.COLOR_LINEA; strokeWidth = 1f }
        canvas.drawLine(MARGEN_DOCUMENTO, y + ALTO_CABECERA_TABLA - 3f, X_FIN_DOCUMENTO, y + ALTO_CABECERA_TABLA - 3f, linea)
    }

    private fun dibujarTurno(canvas: Canvas, elemento: ElementoDocumento.Turno, resumen: ResumenMensual, etiquetas: Etiquetas, locale: Locale, y: Float) {
        val entrada = elemento.entrada
        val tarifa = entrada.precioPorHoraCustom ?: resumen.precioPorHora ?: Dinero.CERO
        val dinero = Dinero.porHoras(entrada.horasDecimal, tarifa).formateado(resumen.trabajo.simboloMoneda)
        val p = paint(EstilosDocumento.FILA_PT, EstilosDocumento.COLOR_TEXTO)
        if (elemento.continuacion) {
            canvas.drawText("↳", X_FECHA_DOCUMENTO, y + 13f, p)
        } else {
            canvas.drawText(formatearFechaCorta(elemento.dia), X_FECHA_DOCUMENTO, y + 13f, p)
            canvas.drawText(formatearDiaSemanaCorto(elemento.dia, locale), X_DIA_DOCUMENTO, y + 13f, p)
        }
        canvas.drawText("${formatearHora(entrada.horaEntrada)}-${formatearHora(entrada.horaSalida)}", X_HORARIO_DOCUMENTO, y + 13f, p)
        val detalles = buildList {
            if (entrada.esDiaSiguiente) add(etiquetas.nocturno)
            entrada.precioPorHoraCustom?.let { add(etiquetas.tarifaCustom.replace("%1\$s", it.formateado(resumen.trabajo.simboloMoneda))) }
            entrada.notas?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        }.joinToString(" · ")
        val detallePaint = paint(EstilosDocumento.NOTA_PT, EstilosDocumento.COLOR_SECUNDARIO)
        canvas.drawText(truncar(detalles, detallePaint, X_HORAS_DOCUMENTO - X_DETALLES_DOCUMENTO - 10f), X_DETALLES_DOCUMENTO, y + 13f, detallePaint)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText(formatearHoras(entrada.horasDecimal), X_HORAS_DOCUMENTO + 48f, y + 13f, p)
        canvas.drawText(dinero, X_FIN_DOCUMENTO, y + 13f, p)
    }

    private fun dibujarExtra(canvas: Canvas, item: DineroExtra, resumen: ResumenMensual, y: Float) {
        val p = paint(EstilosDocumento.FILA_PT, EstilosDocumento.COLOR_TEXTO)
        canvas.drawText(formatearFechaCorta(item.fecha), X_FECHA_DOCUMENTO, y + 13f, p)
        canvas.drawText(truncar(item.descripcion, p, X_DINERO_DOCUMENTO - X_HORARIO_DOCUMENTO - 10f), X_HORARIO_DOCUMENTO, y + 13f, p)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText(item.monto.formateado(resumen.trabajo.simboloMoneda), X_FIN_DOCUMENTO, y + 13f, p)
    }

    private fun dibujarCabecera(canvas: Canvas, resumen: ResumenMensual, etiquetas: Etiquetas, locale: Locale, logo: Bitmap?): Float {
        val tituloPaint = paint(EstilosDocumento.TITULO_PT, EstilosDocumento.COLOR_TEXTO)
        val encabezadoPaint = paint(EstilosDocumento.ENCABEZADO_PT, EstilosDocumento.COLOR_TEXTO)
        val anchoTexto = TamanosDocumento.PDF_ANCHO_PT - MARGEN_DOCUMENTO * 2f - 72f
        canvas.drawText(truncar(resumen.trabajo.nombre, tituloPaint, anchoTexto), MARGEN_DOCUMENTO, 18f, tituloPaint)
        canvas.drawText(truncar("${resumen.trabajo.nombreUsuario} · ${formatearMesAnio(resumen.mes, resumen.anio, locale)}", encabezadoPaint, anchoTexto), MARGEN_DOCUMENTO, 36f, encabezadoPaint)
        texto(canvas, etiquetas.tarifa, MARGEN_DOCUMENTO, 51f, EstilosDocumento.TARIFA_PT, EstilosDocumento.COLOR_SECUNDARIO)
        if (logo != null) {
            val caja = android.graphics.RectF(X_FIN_DOCUMENTO - 64f, 0f, X_FIN_DOCUMENTO, 64f)
            val escala = minOf(caja.width() / logo.width, caja.height() / logo.height)
            val ancho = logo.width * escala
            val alto = logo.height * escala
            val destino = android.graphics.RectF(caja.centerX() - ancho / 2f, caja.centerY() - alto / 2f, caja.centerX() + ancho / 2f, caja.centerY() + alto / 2f)
            canvas.drawBitmap(logo, null, destino, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        return ALTO_CABECERA_COMPLETA
    }

    private fun dibujarPie(canvas: Canvas, etiquetas: Etiquetas, y: Float) {
        val p = paint(EstilosDocumento.PIE_PT, EstilosDocumento.COLOR_SECUNDARIO)
        p.textAlign = Paint.Align.CENTER
        canvas.drawText(etiquetas.pieDeMarca, TamanosDocumento.PDF_ANCHO_PT / 2f, y, p)
    }

    private fun dibujarZebra(canvas: Canvas, y: Float, alto: Float) {
        canvas.drawRect(MARGEN_DOCUMENTO, y, X_FIN_DOCUMENTO, y + alto, Paint().apply { color = EstilosDocumento.COLOR_ZEBRA })
    }

    private fun texto(canvas: Canvas, valor: String, x: Float, y: Float, tamano: Float, color: Int) {
        canvas.drawText(valor, x, y, paint(tamano, color))
    }

    private fun paint(tamano: Float, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = tamano
        this.color = color
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun truncar(valor: String, p: Paint, maximo: Float): String {
        if (valor.isEmpty() || p.measureText(valor) <= maximo) return valor
        val sufijo = "…"
        var fin = valor.length
        while (fin > 0 && p.measureText(valor.substring(0, fin) + sufijo) > maximo) fin--
        return valor.substring(0, fin).trimEnd() + sufijo
    }
}
