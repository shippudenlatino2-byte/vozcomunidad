package com.vozcomunidad.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.vozcomunidad.app.data.Respuesta
import com.vozcomunidad.app.data.formatoTiempo
import java.io.File

fun compartirTexto(context: Context, titulo: String, texto: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, titulo)
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(intent, titulo))
}

fun compartirCsv(context: Context, respuestas: List<Respuesta>) {
    val archivo = File(context.cacheDir, "vozcomunidad_reporte.csv")
    val cuerpo = buildString {
        append('\uFEFF')
        append("Nombre,Telefono,Barrio,Opinion,Audio,Duracion,Estado\n")
        respuestas.forEach { respuesta ->
            val columnas = listOf(
                respuesta.nombre,
                respuesta.telefono,
                respuesta.barrio,
                respuesta.opinion,
                if (respuesta.tieneAudio) "Sí" else "No",
                if (respuesta.tieneAudio) formatoTiempo(respuesta.duracionSeg) else "",
                respuesta.estado,
            )
            append(columnas.joinToString(",") { celda ->
                "\"" + celda.replace("\"", "\"\"") + "\""
            })
            append('\n')
        }
    }
    archivo.writeText(cuerpo, Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Reporte VozComunidad")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Exportar reporte"))
    } catch (error: ActivityNotFoundException) {
        throw error
    }
}
