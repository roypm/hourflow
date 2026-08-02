package com.horastrabajo.app.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.horastrabajo.app.domain.ResumenMensual
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.DineroExtra
import com.horastrabajo.app.domain.model.EntradaHoras
import com.horastrabajo.app.domain.model.formatearHoras
import java.time.LocalDate
import java.util.Locale

/** PDF detallado A4. Mantiene filas compactas y una altura fija por turno. */
object PdfRenderer {

    fun exportar(
        documento: PdfDocument,
        resumen: ResumenMensual,
        etiquetas: Etiquetas,
        locale: Locale,
        logo: Bitmap?,
    ) {
        val elementos = construirElementosDocumento(resumen, etiquetas)
        val limitePrimera = TamanosDocumento.PDF_ALTO_PT - 2f * MARGEN_DOCUMENTO - ALTO_PIE_DOCUMENTO - ALTO_CABECERA_COMPLETA
        val limiteSiguientes = TamanosDocumento.PDF_ALTO_PT - 2f * MARGEN_DOCUMENTO - ALTO_PIE_DOCUMENTO - ALTO_CABECERA_CORTA
        val paginas = paginarElementos(elementos, limitePrimera, limiteSiguientes)
        val totalPaginas = paginas.size.coerceAtLeast(1)

        if (paginas.isEmpty()) {
            val pagina = documento.startPage(info(1))
            dibujarCabecera(pagina.canvas, resumen, etiquetas, locale, logo, completa = true)
            dibujarPie(pagina.canvas, etiquetas, 1, totalPaginas)
            documento.finishPage(pagina)
            return
        }

        paginas.forEachIndexed { numero, layout ->
            val pagina = documento.startPage(info(numero + 1))
            val completa = numero == 0
            var y = dibujarCabecera(pagina.canvas, resumen, etiquetas, locale, if (completa) logo else null, completa)
            layout.indices.forEach { indice ->
                y = dibujarElemento(pagina.canvas, elementos[indice], resumen, etiquetas, locale, y)
            }
            dibujarPie(pagina.canvas, etiquetas, numero + 1, totalPaginas)
            documento.finishPage(pagina)
        }
    }

    private fun paginarElementos(elementos: List<ElementoDocumento>, primera: Float, siguientes: Float): List<PaginaLayout> {
        if (elementos.isEmpty()) return emptyList()
        val paginas = mutableListOf<PaginaLayout>()
        var indices = mutableListOf<Int>()
        var alto = 0f
        var limite = primera
        var indice = 0
        while (indice < elementos.size) {
            val elemento = elementos[indice]
            val mantenerConSiguiente =
                (elemento is ElementoDocumento.Seccion || elemento is ElementoDocumento.CabeceraTabla) && indice + 1 < elementos.size
            val requerido = if (mantenerConSiguiente) elemento.alto + elementos[indice + 1].alto else elemento.alto
            if (indices.isNotEmpty() && alto + requerido > limite) {
                paginas += PaginaLayout(indices, alto)
                indices = mutableListOf()
                alto = 0f
                limite = siguientes
            }
            indices += indice
            alto += elemento.alto
            indice++
        }
        if (indices.isNotEmpty()) paginas += PaginaLayout(indices, alto)
        return paginas
    }

    private fun dibujarElemento(
        canvas: Canvas,
        elemento: ElementoDocumento,
        resumen: ResumenMensual,
        etiquetas: Etiquetas,
        locale: Locale,
        y: Float,
    ): Float {
        return when (elemento) {
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
                dibujarExtra(canvas, elemento.item, resumen, etiquetas, y)
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
        canvas.drawLine(MARGEN_DOCUMENTO, y + elementoLinea(), X_FIN_DOCUMENTO, y + elementoLinea(), linea)
    }

    private fun elementoLinea() = ALTO_CABECERA_TABLA - 3f

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

    private fun dibujarExtra(canvas: Canvas, item: DineroExtra, resumen: ResumenMensual, etiquetas: Etiquetas, y: Float) {
        val p = paint(EstilosDocumento.FILA_PT, EstilosDocumento.COLOR_TEXTO)
        canvas.drawText(formatearFechaCorta(item.fecha), X_FECHA_DOCUMENTO, y + 13f, p)
        canvas.drawText(truncar(item.descripcion, p, X_DINERO_DOCUMENTO - X_HORARIO_DOCUMENTO - 10f), X_HORARIO_DOCUMENTO, y + 13f, p)
        p.textAlign = Paint.Align.RIGHT
        canvas.drawText(item.monto.formateado(resumen.trabajo.simboloMoneda), X_FIN_DOCUMENTO, y + 13f, p)
    }

    private fun dibujarZebra(canvas: Canvas, y: Float, alto: Float) {
        val p = Paint().apply { color = EstilosDocumento.COLOR_ZEBRA }
        canvas.drawRect(MARGEN_DOCUMENTO, y, X_FIN_DOCUMENTO, y + alto, p)
    }

    private fun dibujarCabecera(canvas: Canvas, resumen: ResumenMensual, etiquetas: Etiquetas, locale: Locale, logo: Bitmap?, completa: Boolean): Float {
        val derecha = TamanosDocumento.PDF_ANCHO_PT - MARGEN_DOCUMENTO
        if (completa) {
            val tituloPaint = paint(EstilosDocumento.TITULO_PT, EstilosDocumento.COLOR_TEXTO)
            val encabezadoPaint = paint(EstilosDocumento.ENCABEZADO_PT, EstilosDocumento.COLOR_TEXTO)
            val anchoTexto = TamanosDocumento.PDF_ANCHO_PT - MARGEN_DOCUMENTO * 2f - 72f
            canvas.drawText(truncar(resumen.trabajo.nombre, tituloPaint, anchoTexto), MARGEN_DOCUMENTO, MARGEN_DOCUMENTO + 18f, tituloPaint)
            canvas.drawText(
                truncar("${resumen.trabajo.nombreUsuario} · ${formatearMesAnio(resumen.mes, resumen.anio, locale)}", encabezadoPaint, anchoTexto),
                MARGEN_DOCUMENTO,
                MARGEN_DOCUMENTO + 36f,
                encabezadoPaint,
            )
            texto(canvas, etiquetas.tarifa, MARGEN_DOCUMENTO, MARGEN_DOCUMENTO + 51f, EstilosDocumento.TARIFA_PT, EstilosDocumento.COLOR_SECUNDARIO)
            if (logo != null) {
                val caja = android.graphics.RectF(derecha - 64f, MARGEN_DOCUMENTO, derecha, MARGEN_DOCUMENTO + 64f)
                val escala = minOf(caja.width() / logo.width, caja.height() / logo.height)
                val ancho = logo.width * escala
                val alto = logo.height * escala
                val destino = android.graphics.RectF(caja.centerX() - ancho / 2f, caja.centerY() - alto / 2f, caja.centerX() + ancho / 2f, caja.centerY() + alto / 2f)
                canvas.drawBitmap(logo, null, destino, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            }
            return MARGEN_DOCUMENTO + ALTO_CABECERA_COMPLETA
        }
        val p = paint(EstilosDocumento.ENCABEZADO_PT, EstilosDocumento.COLOR_TEXTO)
        canvas.drawText(
            truncar("${resumen.trabajo.nombre} · ${formatearMesAnio(resumen.mes, resumen.anio, locale)}", p, TamanosDocumento.PDF_ANCHO_PT - MARGEN_DOCUMENTO * 2f),
            MARGEN_DOCUMENTO,
            MARGEN_DOCUMENTO + 16f,
            p,
        )
        return MARGEN_DOCUMENTO + ALTO_CABECERA_CORTA
    }

    private fun dibujarPie(canvas: Canvas, etiquetas: Etiquetas, pagina: Int, total: Int) {
        val p = paint(EstilosDocumento.PIE_PT, EstilosDocumento.COLOR_SECUNDARIO)
        p.textAlign = Paint.Align.CENTER
        val paginaTexto = etiquetas.pagina.replace("%1\$d", pagina.toString()).replace("%2\$d", total.toString())
        canvas.drawText("$paginaTexto · ${etiquetas.pieDeMarca}", TamanosDocumento.PDF_ANCHO_PT / 2f, TamanosDocumento.PDF_ALTO_PT - MARGEN_DOCUMENTO + 2f, p)
    }

    private fun info(numero: Int) = PdfDocument.PageInfo.Builder(TamanosDocumento.PDF_ANCHO_PT, TamanosDocumento.PDF_ALTO_PT, numero).create()

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
