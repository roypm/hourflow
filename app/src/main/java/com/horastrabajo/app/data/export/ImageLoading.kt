package com.horastrabajo.app.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/** Carga un bitmap desde una URI (content:// o file://) y lo escala para que quepa en [maxAncho]x[maxAlto]. */
fun cargarBitmapLogo(context: Context, fotoUri: String?, maxAncho: Float, maxAlto: Float): Bitmap? {
    if (fotoUri == null) return null
    return try {
        val uri = Uri.parse(fotoUri)
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        val relacion = minOf(maxAncho / bitmap.width, maxAlto / bitmap.height)
        if (relacion < 1f) {
            val escalado = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * relacion).toInt(),
                (bitmap.height * relacion).toInt(),
                true
            )
            if (escalado != bitmap) bitmap.recycle()
            escalado
        } else {
            bitmap
        }
    } catch (_: Exception) {
        null
    }
}
