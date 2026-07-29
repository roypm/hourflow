package com.horastrabajo.app.data.export

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

private const val ANCHO_A4 = 595
private const val ALTO_A4 = 842
private const val MARGEN = 40f
private const val ALTURA_LINEA = 18f
private const val ALTURA_LINEA_TITULO = 26f
private const val Y_PIE_DE_PAGINA = ALTO_A4 - 20f

object PdfExporter {

    /** [filas] y [pieDeMarca] llegan ya traducidos desde la UI (ver `construirFilasResumen`). */
    fun exportar(filas: List<FilaResumen>, pieDeMarca: String, archivoDestino: File) {
        val documento = PdfDocument()

        val paintNormal = Paint().apply { textSize = 11f }
        val paintNegrita = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val paintTitulo = Paint().apply { textSize = 15f; isFakeBoldText = true }
        val paintPie = Paint().apply { textSize = 8f; color = Color.GRAY }

        var pagina: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var y = ALTO_A4.toFloat()
        var numeroPagina = 0

        fun finalizarPaginaActual() {
            val paginaActual = pagina ?: return
            canvas?.drawText(pieDeMarca, MARGEN, Y_PIE_DE_PAGINA, paintPie)
            documento.finishPage(paginaActual)
        }

        fun nuevaPagina() {
            finalizarPaginaActual()
            numeroPagina++
            val info = PdfDocument.PageInfo.Builder(ANCHO_A4, ALTO_A4, numeroPagina).create()
            pagina = documento.startPage(info)
            canvas = pagina!!.canvas
            y = MARGEN + ALTURA_LINEA
        }

        nuevaPagina()

        for (fila in filas) {
            val alturaFila = if (fila.estilo == EstiloFila.TITULO) ALTURA_LINEA_TITULO else ALTURA_LINEA
            if (y + alturaFila > ALTO_A4 - MARGEN) {
                nuevaPagina()
            }
            val paint = when (fila.estilo) {
                EstiloFila.TITULO -> paintTitulo
                EstiloFila.NEGRITA -> paintNegrita
                EstiloFila.NORMAL -> paintNormal
            }
            canvas?.drawText(fila.izquierda, MARGEN, y, paint)
            if (fila.derecha.isNotEmpty()) {
                val anchoTexto = paint.measureText(fila.derecha)
                canvas?.drawText(fila.derecha, ANCHO_A4 - MARGEN - anchoTexto, y, paint)
            }
            y += alturaFila
        }

        finalizarPaginaActual()

        FileOutputStream(archivoDestino).use { documento.writeTo(it) }
        documento.close()
    }
}
