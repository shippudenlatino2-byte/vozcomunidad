package com.vozcomunidad.app.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

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
            .put("direccion", borrador.direccion)
            .put("localidad", borrador.localidad)
            .put("canal", borrador.canal)
            .put("categoria", borrador.categoria)
            .put("concejal", borrador.concejal)
            .put("opinion", borrador.opinion)
            .put("tieneAudio", audio != null && audio.isNotEmpty())
            .put("duracionSeg", if (audio != null && audio.isNotEmpty()) borrador.duracionSeg else 0)
        if (audio != null && audio.isNotEmpty()) {
            cuerpo.put("audio", Base64.encodeToString(audio, Base64.NO_WRAP))
        }
        val json = cuerpo.toString()
        despertar()
        return try {
            respuestaDe(JSONObject(pedir("POST", "/api/respuestas", json)))
        } catch (error: IOException) {
            despertar()
            respuestaDe(JSONObject(pedir("POST", "/api/respuestas", json)))
        }
    }

    private fun despertar() {
        runCatching { pedir("GET", "/", null, limiteSegundos = 12) }
    }

    private fun pedir(metodo: String, ruta: String, cuerpo: String?, limiteSegundos: Long = 25): String {
        val conexion = AtomicReference<HttpURLConnection?>(null)
        val executor = Executors.newSingleThreadExecutor()
        val tarea = executor.submit<String> { pedirAhora(metodo, ruta, cuerpo, conexion) }
        return try {
            tarea.get(limiteSegundos, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
            conexion.get()?.disconnect()
            tarea.cancel(true)
            throw IOException("El servidor no respondió a tiempo")
        } catch (error: ExecutionException) {
            val causa = error.cause
            if (causa is IOException) throw causa
            throw IOException(causa?.message ?: "No se pudo conectar")
        } finally {
            executor.shutdownNow()
        }
    }

    private fun pedirAhora(
        metodo: String,
        ruta: String,
        cuerpo: String?,
        referencia: AtomicReference<HttpURLConnection?>,
    ): String {
        val datos = cuerpo?.toByteArray(Charsets.UTF_8)
        val conexion = (URL(BASE + ruta).openConnection() as HttpURLConnection).apply {
            requestMethod = metodo
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "VozComunidad/1.0")
            if (datos != null) {
                doOutput = true
                setFixedLengthStreamingMode(datos.size)
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        referencia.set(conexion)
        try {
            if (datos != null) {
                conexion.outputStream.use { salida -> salida.write(datos) }
            }
            val codigo = conexion.responseCode
            val texto = (if (codigo in 200..299) conexion.inputStream else conexion.errorStream)
                ?.bufferedReader()
                ?.use { it.readText() }
                .orEmpty()
            if (codigo !in 200..299) {
                throw IOException("El servidor respondió $codigo")
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
        canal = objeto.optString("canal"),
        direccion = objeto.optString("direccion"),
        localidad = objeto.optString("localidad"),
        categoria = objeto.optString("categoria"),
        concejal = objeto.optString("concejal"),
    )
}
