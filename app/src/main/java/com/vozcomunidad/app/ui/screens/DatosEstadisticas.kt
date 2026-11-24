package com.vozcomunidad.app.ui.screens

import androidx.compose.ui.graphics.Color
import com.vozcomunidad.app.data.Respuesta
import com.vozcomunidad.app.ui.theme.Vz

internal data class SectorUi(
    val nombre: String,
    val reportes: Int,
    val porcentaje: Float,
    val color: Color,
    val colorTexto: Color,
)

internal data class Panorama(
    val total: Int,
    val conAudio: Int,
    val sectores: List<SectorUi>,
    val hallazgo: String,
)

internal fun panoramaDe(respuestas: List<Respuesta>): Panorama {
    val total = respuestas.size
    val conAudio = respuestas.count { it.tieneAudio }
    if (total == 0) {
        return Panorama(
            total = 0,
            conAudio = 0,
            sectores = emptyList(),
            hallazgo = "Aún no hay inquietudes registradas.",
        )
    }
    val colores = listOf(
        Vz.Primary to Vz.Primary,
        Vz.SecondaryContainer to Vz.Secondary,
        Vz.Tertiary to Vz.Tertiary,
        Vz.PrimaryContainer to Vz.PrimaryContainer,
        Vz.SecondaryFixedDim to Vz.OnSurfaceVariant,
    )
    val sectores = respuestas
        .groupBy { respuesta -> respuesta.barrio.ifBlank { "Sin barrio" } }
        .entries
        .sortedByDescending { it.value.size }
        .take(5)
        .mapIndexed { indice, (nombre, lista) ->
            val (color, colorTexto) = colores[indice % colores.size]
            SectorUi(
                nombre = nombre,
                reportes = lista.size,
                porcentaje = lista.size / total.toFloat(),
                color = color,
                colorTexto = colorTexto,
            )
        }
    val hallazgo = when {
        conAudio == 0 -> "Las $total respuestas guardadas son de texto."
        conAudio == total -> "Las $total respuestas incluyen nota de voz."
        else -> "$conAudio de $total respuestas incluyen nota de voz."
    }
    return Panorama(total, conAudio, sectores, hallazgo)
}
