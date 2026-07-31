package com.horastrabajo.app.data.export

/**
 * Tamaños fijos de las páginas de export (contracto: nunca varían).
 * Las medidas se definen en "puntos de diseño"; el JPG los escala a su densidad
 * (× [ESCALA_JPG]) de modo que los dos documentos conservan las mismas proporciones.
 */
object TamanosDocumento {
    const val PDF_ANCHO_PT = 595
    const val PDF_ALTO_PT = 842

    const val JPG_ANCHO_PX = 1240
    const val JPG_ALTO_PX = 1754

    const val MARGEN_PT = 36f

    val ESCALA_JPG: Float = JPG_ANCHO_PX.toFloat() / PDF_ANCHO_PT
}
