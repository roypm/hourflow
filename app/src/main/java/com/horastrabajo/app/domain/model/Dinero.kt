package com.horastrabajo.app.domain.model

import java.util.Locale
import kotlin.math.roundToLong

/**
 * Locale fijo para formatear cifras (importes y horas). Es deliberadamente independiente
 * del idioma de la app y del idioma del dispositivo: la app muestra siempre coma decimal,
 * de modo que un importe (`1234,50 €`) y unas horas (`7,50h`) nunca se contradigan entre sí.
 */
private val LOCALE_CIFRAS: Locale = Locale("es", "ES")

/** Horas en formato `7,50h`, con la misma coma decimal que [Dinero.formateado]. */
fun formatearHoras(horas: Double): String = String.format(LOCALE_CIFRAS, "%.2fh", horas)

/** Importe sin símbolo, en formato `8,50`, para prellenar campos de texto editables. */
fun formatearImporteEditable(dinero: Dinero): String =
    String.format(LOCALE_CIFRAS, "%.2f", dinero.centimos / 100.0)

/**
 * Cantidad de dinero en céntimos (unidad entera) para evitar errores de redondeo
 * de coma flotante. [centimos] puede ser negativo (usado por el dinero extra).
 */
@JvmInline
value class Dinero(val centimos: Long) : Comparable<Dinero> {

    operator fun plus(otro: Dinero): Dinero = Dinero(centimos + otro.centimos)
    operator fun minus(otro: Dinero): Dinero = Dinero(centimos - otro.centimos)
    operator fun unaryMinus(): Dinero = Dinero(-centimos)

    override fun compareTo(other: Dinero): Int = centimos.compareTo(other.centimos)

    fun formateado(simbolo: String): String {
        val absCentimos = kotlin.math.abs(centimos)
        val entero = absCentimos / 100
        val decimales = absCentimos % 100
        val signo = if (centimos < 0) "-" else ""
        return String.format(LOCALE_CIFRAS, "%s%d,%02d %s", signo, entero, decimales, simbolo)
    }

    companion object {
        val CERO = Dinero(0)

        /** Horas trabajadas (como fracción, ej. 7.5) al precio por hora dado, redondeado al céntimo. */
        fun porHoras(horas: Double, precioPorHora: Dinero): Dinero =
            Dinero((horas * precioPorHora.centimos).roundToLong())
    }
}

fun List<Dinero>.suma(): Dinero = fold(Dinero.CERO) { acc, d -> acc + d }
