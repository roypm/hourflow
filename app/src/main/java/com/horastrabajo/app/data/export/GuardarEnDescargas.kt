package com.horastrabajo.app.data.export

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** En API 29+ guardar en Descargas no necesita permiso; en 26-28 sí. */
fun permisoDescargaConcedido(contexto: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED

/**
 * Copia [origen] a la carpeta pública de Descargas sin sobrescribir un archivo
 * existente: si [nombre] ya está, prueba con sufijos " (1)", " (2)", etc.
 * Devuelve el nombre final con el que se guardó.
 */
fun guardarEnDescargas(contexto: Context, origen: File, nombre: String, mimeType: String): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        guardarConMediaStore(contexto, origen, nombre, mimeType)
    } else {
        guardarEnDescargasLegacy(origen, nombre)
    }

@RequiresApi(Build.VERSION_CODES.Q)
private fun guardarConMediaStore(contexto: Context, origen: File, nombre: String, mimeType: String): String {
    val resolver = contexto.contentResolver
    val coleccion = MediaStore.Downloads.EXTERNAL_CONTENT_URI
    val nombreFinal = nombreUnico(nombre) { candidato ->
        resolver.query(
            coleccion,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(candidato),
            null,
        )?.use { it.count > 0 } == true
    }
    val valores = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, nombreFinal)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        put(MediaStore.MediaColumns.IS_PENDING, 1)
    }
    val uri = resolver.insert(coleccion, valores)
        ?: throw IOException("No se pudo crear el archivo en Descargas")
    try {
        val salida = resolver.openOutputStream(uri)
            ?: throw IOException("No se pudo abrir el archivo en Descargas")
        salida.use { out -> origen.inputStream().use { it.copyTo(out) } }
        val publicado = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
        resolver.update(uri, publicado, null, null)
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        throw e
    }
    return nombreFinal
}

@Suppress("DEPRECATION")
private fun guardarEnDescargasLegacy(origen: File, nombre: String): String {
    val directorio = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    if (!directorio.exists() && !directorio.mkdirs()) {
        throw IOException("No se pudo crear la carpeta de Descargas")
    }
    val nombreFinal = nombreUnico(nombre) { candidato -> File(directorio, candidato).exists() }
    val destino = File(directorio, nombreFinal)
    origen.inputStream().use { entrada ->
        FileOutputStream(destino).use { salida -> entrada.copyTo(salida) }
    }
    return nombreFinal
}

private fun nombreUnico(nombre: String, existe: (String) -> Boolean): String {
    if (!existe(nombre)) return nombre
    val punto = nombre.lastIndexOf('.')
    val base = if (punto > 0) nombre.substring(0, punto) else nombre
    val extension = if (punto > 0) nombre.substring(punto) else ""
    var intento = 1
    while (true) {
        val candidato = "$base ($intento)$extension"
        if (!existe(candidato)) return candidato
        intento++
    }
}
