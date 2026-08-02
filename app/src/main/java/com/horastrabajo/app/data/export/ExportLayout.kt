package com.horastrabajo.app.data.export

/** Resultado de una paginación de filas que nunca parte una fila. */
data class PaginaLayout(val indices: List<Int>, val alto: Float)

/**
 * Divide alturas en páginas conservando cada elemento completo. El primer límite
 * puede ser distinto porque la primera página tiene una cabecera más grande.
 */
fun paginarAlturas(
    alturas: List<Float>,
    limitePrimeraPagina: Float,
    limiteOtrasPaginas: Float,
): List<PaginaLayout> {
    if (alturas.isEmpty()) return emptyList()
    val paginas = mutableListOf<PaginaLayout>()
    var indices = mutableListOf<Int>()
    var alto = 0f
    var limite = limitePrimeraPagina

    alturas.forEachIndexed { indice, altura ->
        if (indices.isNotEmpty() && alto + altura > limite) {
            paginas += PaginaLayout(indices, alto)
            indices = mutableListOf()
            alto = 0f
            limite = limiteOtrasPaginas
        }
        indices += indice
        alto += altura
    }
    if (indices.isNotEmpty()) paginas += PaginaLayout(indices, alto)
    return paginas
}

fun factorAjusteJpg(altoUtil: Float, altoNecesario: Float): Float =
    if (altoNecesario <= 0f) 1f else minOf(1f, altoUtil / altoNecesario)
