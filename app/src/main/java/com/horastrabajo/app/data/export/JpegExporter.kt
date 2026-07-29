package com.horastrabajo.app.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream

private const val ANCHO_IMG = 1080
private const val MARGEN = 32f
private const val ALTURA_LINEA = 40f
private const val ALTURA_LINEA_TITULO = 56f
private const val ALTURA_PIE = 50f
private const val CALIDAD_JPEG = 92

object JpegExporter {

    /** [filas] y [pieDeMarca] llegan ya traducidos desde la UI (ver `construirFilasResumen`). */
    fun exportar(filas: List<FilaResumen>, pieDeMarca: String, archivoDestino: File) {
        val paintNormal = Paint().apply { textSize = 26f; color = Color.BLACK; isAntiAlias = true }
        val paintNegrita = Paint().apply { textSize = 26f; isFakeBoldText = true; color = Color.BLACK; isAntiAlias = true }
        val paintTitulo = Paint().apply { textSize = 34f; isFakeBoldText = true; color = Color.BLACK; isAntiAlias = true }
        val paintPie = Paint().apply { textSize = 20f; color = Color.GRAY; isAntiAlias = true }

        val alturaTotal = (MARGEN * 2 + ALTURA_PIE + filas.sumOf {
            (if (it.estilo == EstiloFila.TITULO) ALTURA_LINEA_TITULO else ALTURA_LINEA).toDouble()
        }).toInt()

        val bitmap = Bitmap.createBitmap(ANCHO_IMG, alturaTotal, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = MARGEN + ALTURA_LINEA
        for (fila in filas) {
            val alturaFila = if (fila.estilo == EstiloFila.TITULO) ALTURA_LINEA_TITULO else ALTURA_LINEA
            val paint = when (fila.estilo) {
                EstiloFila.TITULO -> paintTitulo
                EstiloFila.NEGRITA -> paintNegrita
                EstiloFila.NORMAL -> paintNormal
            }
            canvas.drawText(fila.izquierda, MARGEN, y, paint)
            if (fila.derecha.isNotEmpty()) {
                val anchoTexto = paint.measureText(fila.derecha)
                canvas.drawText(fila.derecha, ANCHO_IMG - MARGEN - anchoTexto, y, paint)
            }
            y += alturaFila
        }

        canvas.drawText(pieDeMarca, MARGEN, y + ALTURA_PIE / 2, paintPie)

        FileOutputStream(archivoDestino).use { salida ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
        }
        bitmap.recycle()
    }
}
