package com.vozcomunidad.app.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ComunidadApi {
    const val BASE = "https://vozcomunidad-vecinal.onrender.com"

    fun listar(): List<Respuesta> {
        val respuesta = pedir("GET", "/api/respuestas", null)
        val lista = JSONArray(respuesta)
        return List(lista.length()) { indice ->
            respuestaDe(lista.getJSONObject(indice))
        }
    }

    fun urlAudio(id: String) = "$BASE/api/respuestas/$id/audio"

    fun descargarAudio(id: String, destino: java.io.File): Boolean {
        val conexion = (URL(urlAudio(id)).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 70_000
            setRequestProperty("User-Agent", "VozComunidad/1.0")
        }
        return try {
            if (conexion.responseCode !in 200..299) return false
            destino.parentFile?.mkdirs()
            conexion.inputStream.use { entrada ->
                destino.outputStream().use { salida -> entrada.copyTo(salida) }
            }
            destino.length() > 32
        } finally {
            conexion.disconnect()
        }
    }

    fun publicar(borrador: Respuesta, audio: ByteArray?): Respuesta {
        val cuerpo = JSONObject()
            .put("nombre", borrador.nombre)
            .put("telefono", borrador.telefono)
            .put("barrio", borrador.barrio)
            .put("opinion", borrador.opinion)
            .put("tieneAudio", audio != null && audio.isNotEmpty())
            .put("duracionSeg", if (audio != null && audio.isNotEmpty()) borrador.duracionSeg else 0)
        if (audio != null && audio.isNotEmpty()) {
            cuerpo.put("audio", Base64.encodeToString(audio, Base64.NO_WRAP))
        }
        return respuestaDe(JSONObject(pedir("POST", "/api/respuestas", cuerpo.toString())))
    }

    private fun pedir(metodo: String, ruta: String, cuerpo: String?): String {
        val conexion = (URL(BASE + ruta).openConnection() as HttpURLConnection).apply {
            requestMethod = metodo
            connectTimeout = 20_000
            readTimeout = 70_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "VozComunidad/1.0")
            if (cuerpo != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (cuerpo != null) {
                conexion.outputStream.use { salida ->
                    salida.write(cuerpo.toByteArray(Charsets.UTF_8))
                }
            }
            val codigo = conexion.responseCode
            val texto = (if (codigo in 200..299) conexion.inputStream else conexion.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (codigo !in 200..299) {
                error("El servidor respondió $codigo")
            }
            return texto
        } finally {
            conexion.disconnect()
        }
    }

    private fun respuestaDe(objeto: JSONObject) = Respuesta(
        id = objeto.getString("id"),
        nombre = objeto.optString("nombre"),
        telefono = objeto.optString("telefono"),
        barrio = objeto.optString("barrio"),
        opinion = objeto.optString("opinion"),
        momento = objeto.optString("momento", "Ahora"),
        tieneAudio = objeto.optBoolean("tieneAudio"),
        duracionSeg = objeto.optInt("duracionSeg"),
        estado = objeto.optString("estado", "Enviada a la comunidad"),
        tono = runCatching { Tono.valueOf(objeto.optString("tono", "EXITO")) }.getOrDefault(Tono.EXITO),
        acento = runCatching { Acento.valueOf(objeto.optString("acento", "VERDE")) }.getOrDefault(Acento.VERDE),
        urgente = objeto.optBoolean("urgente"),
    )
}
