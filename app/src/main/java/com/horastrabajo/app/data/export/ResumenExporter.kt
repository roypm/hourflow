package com.horastrabajo.app.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

/**
 * Punto de entrada Android para los dos formatos de resumen.
 */
object ResumenExporter {

    fun exportarPdf(contexto: Context, archivoDestino: File, resumen: com.horastrabajo.app.domain.ResumenMensual, etiquetas: Etiquetas, locale: java.util.Locale) {
        val documento = PdfDocument()
        val logo = cargarLogo(contexto, resumen.trabajo.fotoUri)
        PdfRenderer.exportar(documento, resumen, etiquetas, locale, logo)
        FileOutputStream(archivoDestino).use { documento.writeTo(it) }
        documento.close()
    }

    fun exportarJpg(
        archivoDestino: File,
        resumen: com.horastrabajo.app.domain.ResumenMensual,
        etiquetas: Etiquetas,
        locale: java.util.Locale,
    ) {
        exportarJpgInterno(archivoDestino, resumen, etiquetas, locale, null)
    }

    fun exportarJpg(
        contexto: Context,
        archivoDestino: File,
        resumen: com.horastrabajo.app.domain.ResumenMensual,
        etiquetas: Etiquetas,
        locale: java.util.Locale,
    ) {
        exportarJpgInterno(archivoDestino, resumen, etiquetas, locale, cargarLogo(contexto, resumen.trabajo.fotoUri))
    }

    private fun exportarJpgInterno(
        archivoDestino: File,
        resumen: com.horastrabajo.app.domain.ResumenMensual,
        etiquetas: Etiquetas,
        locale: java.util.Locale,
        logo: Bitmap?,
    ) {
        val altoNecesario = JpegRenderer.altoNecesario(resumen, etiquetas)
        val altoPx = ceil(altoNecesario * TamanosDocumento.ESCALA_JPG.toDouble()).toInt()
        val bytesNecesarios = TamanosDocumento.JPG_ANCHO_PX.toLong() * altoPx.toLong() * 4L
        check(altoPx <= TamanosDocumento.MAX_JPG_ALTO_PX && bytesNecesarios <= TamanosDocumento.MAX_JPG_BYTES) {
            "El resumen es demasiado grande para un único JPG; usa el PDF."
        }
        val bitmap = Bitmap.createBitmap(
            TamanosDocumento.JPG_ANCHO_PX, altoPx, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            JpegRenderer.dibujar(canvas, resumen, etiquetas, locale, logo)
            FileOutputStream(archivoDestino).use { salida ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, salida)
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun cargarLogo(contexto: Context, fotoUri: String?): Bitmap? = runCatching {
        fotoUri?.let { contexto.contentResolver.openInputStream(Uri.parse(it))?.use(BitmapFactory::decodeStream) }
    }.getOrNull()
}
