package com.horastrabajo.app.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.horastrabajo.app.domain.ResumenMensual
import com.horastrabajo.app.domain.model.Dinero
import com.horastrabajo.app.domain.model.formatearHoras
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM")
private val FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm")

data class EstiloRender(
    val ancho: Int,
    val altoPagina: Int = Int.MAX_VALUE,
    val margen: Float,
    val alturaLinea: Float,
    val alturaLineaTitulo: Float,
    val textSizeTitulo: Float,
    val textSizeNormal: Float,
    val textSizePequena: Float,
    val colorFondoCabecera: Int = 0xFFEEEEEE.toInt(),
    val colorBorde: Int = 0xFFCCCCCC.toInt(),
    val colorFondoAlterno: Int = 0xFFF2F2F2.toInt(),
    /** Padding extra sobre el texto en los fondos de fila (en proporción a textSize). */
    val paddingFondo: Float = 0.25f,
)

/** Renderiza un resumen mensual en formato tabla profesional sobre un [Canvas]. */
object RenderizadorResumen {

    private val formatoFecha = FORMATO_FECHA
    private val formatoHora = FORMATO_HORA

    /** Calcula la altura total del contenido — EXACTAMENTE igual al avance de Y en [dibujar]. */
    fun calcularAlturaTotal(resumen: ResumenMensual, logo: Bitmap? = null, estilo: EstiloRender): Int {
        val logoAltura = logo?.let { it.height.toFloat() + 12f } ?: 0f
        val numEntradas = resumen.dias.sumOf { it.entradas.size }
        val numExtra = resumen.dineroExtra.size

        var y = logoAltura
        y += estilo.alturaLineaTitulo          // encabezado
        y += estilo.alturaLinea * 1.5f          // tarifa + espacio
        y += 6f                                  // separador
        y += 14f                                 // cabecera columnas + linea + gap (4+10)
        y += numEntradas * estilo.alturaLinea    // filas de datos
        y += 4f + 8f                             // separador totales + gap
        y += estilo.alturaLinea                  // "Total horas del mes"
        if (numExtra > 0) {
            y += estilo.alturaLinea               // "Dinero extra" label
            y += numExtra * estilo.alturaLinea    // filas de extra
            y += estilo.alturaLinea * 0.3f        // pequeño espacio
            y += estilo.alturaLinea               // "Total dinero extra"
        }
        y += 4f + 10f                            // separador final + gap
        y += estilo.alturaLineaTitulo             // "TOTAL A COBRAR"
        y += estilo.alturaLineaTitulo + 6f        // pie de página

        return (y + estilo.margen).toInt() + 1  // +1 safety
    }

    fun dibujar(
        canvas: Canvas,
        resumen: ResumenMensual,
        logo: Bitmap?,
        pieDeMarca: String,
        estilo: EstiloRender,
        onNuevaPagina: (() -> Canvas)? = null,
    ) {
        val simbolo = resumen.trabajo.simboloMoneda
        val locale = Locale.getDefault()

        // Paints
        val paintTitulo = Paint().apply {
            textSize = estilo.textSizeTitulo; isFakeBoldText = true; isAntiAlias = true
        }
        val paintNormal = Paint().apply {
            textSize = estilo.textSizeNormal; isAntiAlias = true
        }
        val paintNegrita = Paint().apply {
            textSize = estilo.textSizeNormal; isFakeBoldText = true; isAntiAlias = true
        }
        val paintPequena = Paint().apply {
            textSize = estilo.textSizePequena; color = Color.GRAY; isAntiAlias = true
        }
        val paintBorde = Paint().apply {
            color = estilo.colorBorde; strokeWidth = 1f
        }
        val paintBordeGrueso = Paint().apply {
            color = estilo.colorBorde; strokeWidth = 3f
        }
        val paintFondoCabecera = Paint().apply { color = estilo.colorFondoCabecera }
        val paintFondoAlterno = Paint().apply { color = estilo.colorFondoAlterno }
        val paintDerNormal = Paint(paintNormal).apply { textAlign = Paint.Align.RIGHT }
        val paintDerNegrita = Paint(paintNegrita).apply { textAlign = Paint.Align.RIGHT }
        val paintDerTitulo = Paint(paintTitulo).apply { textAlign = Paint.Align.RIGHT }

        // Font metrics para fondos precisos
        val fmNormal = paintNormal.fontMetrics
        val paddingFondo = estilo.textSizeNormal * estilo.paddingFondo

        val m = estilo.margen
        val anchoCont = estilo.ancho - 2 * m

        // Columnas (porcentajes del ancho de contenido)
        val colFecha = (anchoCont * 0.15f).toInt()
        val colDia = (anchoCont * 0.11f).toInt()
        val colEnt = (anchoCont * 0.14f).toInt()
        val colSal = (anchoCont * 0.14f).toInt()
        val colHor = (anchoCont * 0.14f).toInt()
        val colDinero = anchoCont - colFecha - colDia - colEnt - colSal - colHor

        val xFecha = m
        val xDia = xFecha + colFecha
        val xEnt = xDia + colDia
        val xSal = xEnt + colEnt
        val xHoras = xSal + colSal
        val xDinero = xHoras + colHor
        val bordeDer = m + anchoCont

        var currentCanvas = canvas
        var y = m

        // Pre-calculamos el encabezado (ANTES de las funciones que lo usan)
        val nombreMes = java.time.Month.of(resumen.mes)
            .getDisplayName(TextStyle.FULL_STANDALONE, locale)
            .replaceFirstChar { it.uppercase() }
        val encabezado = "${resumen.trabajo.nombreUsuario} · ${resumen.trabajo.nombre} · $nombreMes ${resumen.anio}"

        // ===== UTILITIES =====
        /** Dibuja el fondo de una fila de datos alrededor de la línea base [y] */
        fun drawRowBackground(y: Float) {
            val top = y + fmNormal.ascent - paddingFondo
            val bottom = y + fmNormal.descent + paddingFondo
            currentCanvas.drawRect(m, top, bordeDer, bottom, paintFondoAlterno)
        }

        fun dibujarCabeceraPagina() {
            // Logo
            logo?.let {
                val xLogo = estilo.ancho - m - it.width
                currentCanvas.drawBitmap(it, xLogo, m, null)
                if (y < m + it.height + 10f) y = m + it.height + 10f
            }

            // Encabezado
            currentCanvas.drawText(encabezado, m, y, paintTitulo)
            y += estilo.alturaLineaTitulo

            // Tarifa
            val tarifaTexto = resumen.precioPorHora
                ?.let { "Tarifa: ${it.formateado(simbolo)}/hora" }
                ?: "Tarifa no fijada"
            currentCanvas.drawText(tarifaTexto, m, y, paintNormal)
            y += estilo.alturaLinea * 1.5f

            // Separador
            currentCanvas.drawLine(m, y, bordeDer, y, paintBorde)
            y += 6f

            // Cabecera de columnas
            val topCab = y + fmNormal.ascent - paddingFondo
            val botCab = y + fmNormal.descent + paddingFondo
            currentCanvas.drawRect(m, topCab, bordeDer, botCab, paintFondoCabecera)

            currentCanvas.drawText("Fecha", xFecha, y, paintNegrita)
            currentCanvas.drawText("Día", xDia, y, paintNegrita)
            currentCanvas.drawText("Entrada", xEnt, y, paintNegrita)
            currentCanvas.drawText("Salida", xSal, y, paintNegrita)
            currentCanvas.drawText("Horas", xHoras, y, paintNegrita)
            currentCanvas.drawText("Dinero", xDinero + colDinero, y, paintDerNegrita)

            y += 4f
            currentCanvas.drawLine(m, y, bordeDer, y, paintBorde)
            y += 10f
        }

        fun checkPageBreak(neededY: Float) {
            if (onNuevaPagina != null && neededY > estilo.altoPagina - m) {
                // Pie en página actual
                currentCanvas.drawText(pieDeMarca, m, estilo.altoPagina - 10f, paintPequena)
                // Nueva página
                currentCanvas = onNuevaPagina()
                y = m
                dibujarCabeceraPagina()
            }
        }

        // ===== DIBUJAR PRIMERA PÁGINA =====
        dibujarCabeceraPagina()

        // ===== FILAS =====
        var filaIdx = 0
        for (dia in resumen.dias) {
            for (entrada in dia.entradas) {
                checkPageBreak(y + estilo.alturaLinea)

                // Fondo alterno
                if (filaIdx % 2 == 1) {
                    drawRowBackground(y)
                }

                val fechaStr = dia.fecha.format(formatoFecha)
                val diaStr = dia.fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                    .lowercase(locale).removeSuffix(".")
                val hEnt = entrada.horaEntrada.format(formatoHora)
                val hSal = entrada.horaSalida.format(formatoHora)
                val horas = formatearHoras(entrada.horasDecimal)
                val tarifaEf = entrada.precioPorHoraCustom ?: resumen.precioPorHora
                val dinero = tarifaEf?.let { Dinero.porHoras(entrada.horasDecimal, it).formateado(simbolo) } ?: ""

                currentCanvas.drawText(fechaStr, xFecha, y, paintNormal)
                currentCanvas.drawText(diaStr, xDia, y, paintNormal)
                currentCanvas.drawText(hEnt, xEnt, y, paintNormal)
                currentCanvas.drawText(hSal, xSal, y, paintNormal)
                currentCanvas.drawText(horas, xHoras + colHor, y, paintDerNormal)
                if (dinero.isNotEmpty()) {
                    currentCanvas.drawText(dinero, xDinero + colDinero, y, paintDerNormal)
                }

                y += estilo.alturaLinea
                filaIdx++
            }
        }

        // ===== TOTALES =====
        checkPageBreak(y + 4f)
        y += 4f
        currentCanvas.drawLine(m, y, bordeDer, y, paintBorde)
        y += 8f

        checkPageBreak(y + estilo.alturaLinea)
        currentCanvas.drawText("Total horas del mes", m, y, paintNegrita)
        currentCanvas.drawText(formatearHoras(resumen.totalHoras), xHoras + colHor, y, paintDerNegrita)
        currentCanvas.drawText(resumen.totalDineroHoras.formateado(simbolo), xDinero + colDinero, y, paintDerNegrita)
        y += estilo.alturaLinea

        // Dinero extra
        if (resumen.dineroExtra.isNotEmpty()) {
            checkPageBreak(y + estilo.alturaLinea)
            currentCanvas.drawText("Dinero extra", m, y, paintNormal)
            y += estilo.alturaLinea

            for (extra in resumen.dineroExtra) {
                checkPageBreak(y + estilo.alturaLinea)
                val desc = "${extra.fecha.format(formatoFecha)} ${extra.descripcion}"
                currentCanvas.drawText(desc, m + 12f, y, paintPequena)
                currentCanvas.drawText(extra.monto.formateado(simbolo), xDinero + colDinero, y, paintDerNormal)
                y += estilo.alturaLinea
            }

            y += estilo.alturaLinea * 0.3f
            checkPageBreak(y + estilo.alturaLinea)
            currentCanvas.drawText("Total dinero extra", m, y, paintNegrita)
            currentCanvas.drawText(resumen.totalDineroExtra.formateado(simbolo), xDinero + colDinero, y, paintDerNegrita)
            y += estilo.alturaLinea
        }

        // ===== TOTAL FINAL =====
        checkPageBreak(y + 4f)
        y += 4f
        currentCanvas.drawLine(m, y, bordeDer, y, paintBordeGrueso)
        y += 10f

        checkPageBreak(y + estilo.alturaLineaTitulo)
        currentCanvas.drawText("TOTAL A COBRAR", m, y, paintTitulo)
        currentCanvas.drawText(resumen.totalDinero.formateado(simbolo), xDinero + colDinero, y, paintDerTitulo)

        // Pie de página
        currentCanvas.drawText(pieDeMarca, m, y + estilo.alturaLineaTitulo + 6f, paintPequena)
    }
}
