package com.horastrabajo.app.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.horastrabajo.app.domain.ResumenMensual
import java.io.File
import java.io.FileOutputStream

private const val ANCHO_IMG = 1080
private const val CALIDAD_JPEG = 92

object JpegExporter {

    private val estiloJpeg = EstiloRender(
        ancho = ANCHO_IMG,
        margen = 32f,
        alturaLinea = 56f,
        alturaLineaTitulo = 64f,
        textSizeTitulo = 32f,
        textSizeNormal = 24f,
        textSizePequena = 18f,
    )

    /** Exporta el resumen con formato tabla profesional. */
    fun exportarResumen(
        context: Context,
        resumen: ResumenMensual,
        pieDeMarca: String,
        archivoDestino: File,
    ) {
        val logo = cargarBitmapLogo(context, resumen.trabajo.fotoUri, 90f, 90f)

        val alturaTotal = RenderizadorResumen.calcularAlturaTotal(resumen, logo, estiloJpeg)
        val bitmap = Bitmap.createBitmap(ANCHO_IMG, alturaTotal, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        RenderizadorResumen.dibujar(
            canvas = canvas,
            resumen = resumen,
            logo = logo,
            pieDeMarca = pieDeMarca,
            estilo = estiloJpeg,
        )

        logo?.recycle()

        FileOutputStream(archivoDestino).use { salida ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
        }
        bitmap.recycle()
    }
}
