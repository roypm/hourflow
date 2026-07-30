package com.horastrabajo.app.data.export

import android.content.Context
import android.graphics.pdf.PdfDocument
import com.horastrabajo.app.domain.ResumenMensual
import java.io.File
import java.io.FileOutputStream

private const val ANCHO_A4 = 595
private const val ALTO_A4 = 842

object PdfExporter {

    private val estiloA4 = EstiloRender(
        ancho = ANCHO_A4,
        altoPagina = ALTO_A4,
        margen = 40f,
        alturaLinea = 32f,
        alturaLineaTitulo = 36f,
        textSizeTitulo = 15f,
        textSizeNormal = 10f,
        textSizePequena = 8f,
    )

    /** Exporta el resumen con formato tabla profesional. */
    fun exportarResumen(
        context: Context,
        resumen: ResumenMensual,
        pieDeMarca: String,
        archivoDestino: File,
    ) {
        val documento = PdfDocument()
        val logo = cargarBitmapLogo(context, resumen.trabajo.fotoUri, 55f, 55f)

        var pagina: PdfDocument.Page? = null

        val onNuevaPagina = {
            pagina?.let { documento.finishPage(it) }
            val numPag = (pagina?.info?.pageNumber ?: 0) + 1
            val info = PdfDocument.PageInfo.Builder(ANCHO_A4, ALTO_A4, numPag).create()
            pagina = documento.startPage(info)
            pagina!!.canvas
        }

        // Primera página
        onNuevaPagina()

        RenderizadorResumen.dibujar(
            canvas = pagina!!.canvas,
            resumen = resumen,
            logo = logo,
            pieDeMarca = pieDeMarca,
            estilo = estiloA4,
            onNuevaPagina = onNuevaPagina,
        )

        pagina?.let { documento.finishPage(it) }
        logo?.recycle()

        FileOutputStream(archivoDestino).use { documento.writeTo(it) }
        documento.close()
    }
}
