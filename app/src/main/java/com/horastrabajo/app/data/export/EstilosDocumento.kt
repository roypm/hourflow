package com.horastrabajo.app.data.export

/**
 * Estilo visual de los documentos exportados: fondo blanco, texto todo en negrita,
 * elementos secundarios en gris y filas con zebra sutil. Tamaños en puntos de diseño.
 */
object EstilosDocumento {

    val COLOR_TEXTO = 0xFF1A1A1A.toInt()
    val COLOR_SECUNDARIO = 0xFF757575.toInt()
    val COLOR_ZEBRA = 0xFFF5F5F5.toInt()
    val COLOR_LINEA = 0xFFE0E0E0.toInt()

    val TITULO_PT = 16f
    val ENCABEZADO_PT = 11f
    val TARIFA_PT = 9f
    val SECCION_PT = 10f
    val FILA_PT = 8f
    val NOTA_PT = 8f
    val SUMA_PT = 9f
    val TOTAL_PT = 12f
    val PIE_PT = 7f
}
