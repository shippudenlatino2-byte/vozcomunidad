package com.vozcomunidad.app.data

enum class Canal { WHATSAPP, LLAMADA }

enum class Categoria { SEGURIDAD, SERVICIOS, INFRAESTRUCTURA, AMBIENTE }

enum class ModoFiltro { AUDIO, TEXTO, URGENTE }

enum class Tono { EXITO, AVISO, INFO }

enum class Acento { VERDE, AMBAR }

data class Aviso(val texto: String, val marca: Long)

data class Respuesta(
    val id: String,
    val nombre: String,
    val telefono: String,
    val barrio: String,
    val opinion: String,
    val momento: String,
    val tieneAudio: Boolean,
    val duracionSeg: Int,
    val estado: String,
    val tono: Tono,
    val acento: Acento = Acento.VERDE,
    val urgente: Boolean = false,
)

fun formatoTiempo(segundos: Int): String {
    val minutos = segundos / 60
    val resto = segundos % 60
    return "%02d:%02d".format(minutos, resto)
}

fun inicialesDe(nombre: String): String {
    val partes = nombre.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        partes.isEmpty() -> "?"
        partes.size == 1 -> partes.first().take(2).uppercase()
        else -> "${partes.first().first()}${partes.last().first()}".uppercase()
    }
}
