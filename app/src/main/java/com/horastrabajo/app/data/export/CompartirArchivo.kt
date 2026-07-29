package com.horastrabajo.app.data.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.horastrabajo.app.R
import java.io.File

/** App-specific external storage: persistente, no lo borra el sistema al limpiar cache. */
fun directorioExports(context: Context): File =
    File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }

fun compartirArchivo(context: Context, archivo: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.accion_compartir)))
}
