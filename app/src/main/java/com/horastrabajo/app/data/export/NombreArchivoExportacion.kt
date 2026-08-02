package com.horastrabajo.app.data.export

import com.horastrabajo.app.domain.ResumenMensual
import java.text.Normalizer

fun nombreArchivoResumen(resumen: ResumenMensual, extension: String): String {
    val trabajo = limpiarParteNombre(resumen.trabajo.nombre, "trabajo")
    val empleado = limpiarParteNombre(resumen.trabajo.nombreUsuario, "empleado")
    val mes = resumen.mes.toString().padStart(2, '0')
    return "${resumen.anio}-${mes}_${trabajo}_${empleado}.$extension"
}

private fun limpiarParteNombre(valor: String, fallback: String): String {
    val sinAcentos = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
    val limpio = sinAcentos
        .replace("[^A-Za-z0-9]+".toRegex(), "_")
        .trim('_')
        .take(60)
    return limpio.ifEmpty { fallback }
}
