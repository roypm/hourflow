package com.horastrabajo.app.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * Exportadores de resumen. Contenido provisional: solo un texto "TEST" centrado,
 * mientras se rediseña el documento definitivo.
 *
 * Contrato de tamaños (fijos, no flexibles):
 * - El PDF es siempre una sola página A4 (595×842 pt). Nunca se generan dos páginas:
 *   si el contenido desborda, el renderizador deberá escalarlo o compactarlo.
 * - El JPG es siempre una imagen A4 a 150 dpi (1240×1754 px), sin variar de tamaño.
 */
object ResumenExporter {

    fun exportarPdf(archivoDestino: File) {
        val documento = PdfDocument()
        val pagina = documento.startPage(PdfDocument.PageInfo.Builder(
            TamanosDocumento.PDF_ANCHO_PT, TamanosDocumento.PDF_ALTO_PT, 1).create())
        dibujarTest(pagina.canvas, TamanosDocumento.PDF_ANCHO_PT, TamanosDocumento.PDF_ALTO_PT)
        documento.finishPage(pagina)
        FileOutputStream(archivoDestino).use { documento.writeTo(it) }
        documento.close()
    }

    fun exportarJpg(
        archivoDestino: File,
        resumen: com.horastrabajo.app.domain.ResumenMensual,
        etiquetas: Etiquetas,
        locale: java.util.Locale,
    ) {
        val bitmap = Bitmap.createBitmap(
            TamanosDocumento.JPG_ANCHO_PX, TamanosDocumento.JPG_ALTO_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        JpegRenderer.dibujar(canvas, resumen, etiquetas, locale)
        FileOutputStream(archivoDestino).use { salida ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, salida)
        }
        bitmap.recycle()
    }

    @Deprecated("Usar exportarJpg con parámetros reales", replaceWith = ReplaceWith(
        "exportarJpg(archivoDestino, resumen, etiquetas, locale)"))
    fun exportarJpg(archivoDestino: File) {
        val bitmap = Bitmap.createBitmap(
            TamanosDocumento.JPG_ANCHO_PX, TamanosDocumento.JPG_ALTO_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        dibujarTest(canvas, TamanosDocumento.JPG_ANCHO_PX, TamanosDocumento.JPG_ALTO_PX)
        FileOutputStream(archivoDestino).use { salida ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, salida)
        }
        bitmap.recycle()
    }

    private fun dibujarTest(canvas: Canvas, ancho: Int, alto: Int) {
        val paint = Paint().apply {
            textSize = ancho / 12f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            color = Color.DKGRAY
        }
        canvas.drawText("TEST", ancho / 2f, alto / 2f, paint)
    }
}
