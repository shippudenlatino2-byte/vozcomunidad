package com.vozcomunidad.app.data

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class EncuestaViewModel(application: android.app.Application) : AndroidViewModel(application) {
    var nombre by mutableStateOf("")
    var telefono by mutableStateOf("")
    var canal by mutableStateOf(Canal.WHATSAPP)
    var direccion by mutableStateOf("")
    var barrio by mutableStateOf("")
    var localidad by mutableStateOf("Comuna / Localidad Centro")
    var opinion by mutableStateOf("")
    var concejal by mutableStateOf("")
    var grabando by mutableStateOf(false)
    var segundos by mutableStateOf(0)
    var audioListo by mutableStateOf(false)
    var categoria by mutableStateOf<Categoria?>(null)
    var respuestas by mutableStateOf(emptyList<Respuesta>())
    var busqueda by mutableStateOf("")
    var filtroBarrio by mutableStateOf("Todos")
    var filtroModo by mutableStateOf<ModoFiltro?>(null)
    var recientesPrimero by mutableStateOf(true)
    var reproduciendoId by mutableStateOf<String?>(null)
    var enviando by mutableStateOf(false)
        private set
    var aviso by mutableStateOf<Aviso?>(null)
        private set
    var conflictoConcejal by mutableStateOf<String?>(null)
        private set

    private var grabador: MediaRecorder? = null
    private var archivoNota: File? = null
    private var reproductor: MediaPlayer? = null
    private var generacionAudio = 0
    private val tareasGrabacion = Executors.newSingleThreadExecutor()

    val localidades = listOf(
        "Comuna / Localidad Centro",
        "Localidad Norte",
        "Localidad Sur",
        "Localidad Occidente",
    )

    init {
        refrescar()
        viewModelScope.launch {
            while (true) {
                delay(15_000)
                if (!enviando) refrescar()
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                if (grabando && segundos < 180) {
                    segundos += 1
                    audioListo = true
                    if (segundos == 180) {
                        finalizarNota()
                        avisar("Se alcanzó el máximo de 3 minutos")
                    }
                }
            }
        }
    }

    fun avisar(texto: String) {
        aviso = Aviso(texto, System.nanoTime())
    }

    fun consumirAviso(marca: Long) {
        if (aviso?.marca == marca) aviso = null
    }

    private fun normalizar(valor: String): String {
        val limpio = java.text.Normalizer.normalize(valor.trim().lowercase(), java.text.Normalizer.Form.NFD)
        return limpio.replace(Regex("\\p{Mn}+"), "").replace(Regex("\\s+"), " ")
    }

    fun alternarGrabacion() {
        if (grabando) {
            pausarNota()
            return
        }
        val lista = if (grabador != null) reanudarNota() else comenzarNota()
        if (!lista) avisar("No se pudo guardar el audio. Revisa el micrófono")
    }

    fun repetirAudio() {
        borrarNota()
        if (!comenzarNota()) avisar("No se pudo guardar el audio. Revisa el micrófono")
    }

    fun descartarAudio() {
        borrarNota()
        avisar("Audio descartado. Puedes volver a grabar")
    }

    fun finalizarNota() {
        if (grabador == null) {
            audioListo = notaGuardada()
            return
        }
        liberarGrabador()
        audioListo = notaGuardada()
    }

    fun alternarModo(modo: ModoFiltro) {
        filtroModo = if (filtroModo == modo) null else modo
    }

    fun alternarOrden() {
        recientesPrimero = !recientesPrimero
        avisar(if (recientesPrimero) "Orden: más recientes" else "Orden: más antiguas")
    }

    fun alternarReproduccion(id: String) {
        if (reproduciendoId == id) {
            detenerReproduccion()
            return
        }
        detenerReproduccion()
        val generacion = generacionAudio
        reproduciendoId = id
        viewModelScope.launch {
            val archivo = withContext(Dispatchers.IO) { obtenerAudio(id) }
            if (generacion != generacionAudio) return@launch
            if (archivo == null) {
                reproduciendoId = null
                avisar("Este audio no quedó guardado. Grábalo otra vez y envía la encuesta")
                return@launch
            }
            val player = MediaPlayer()
            reproductor = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            player.setDataSource(archivo.absolutePath)
            player.setOnCompletionListener {
                if (generacion == generacionAudio) detenerReproduccion()
            }
            player.setOnErrorListener { _, _, _ ->
                avisar("No se pudo reproducir el audio")
                detenerReproduccion()
                true
            }
            player.setOnPreparedListener { preparado ->
                if (generacion != generacionAudio) {
                    runCatching { preparado.release() }
                } else {
                    preparado.start()
                }
            }
            runCatching { player.prepareAsync() }.onFailure {
                reproduciendoId = null
                runCatching { player.release() }
                avisar("No se pudo reproducir el audio")
            }
        }
    }

    fun limpiarFormulario() {
        reiniciarFormulario()
        avisar("Formulario limpio")
    }

    private fun reiniciarFormulario() {
        borrarNota()
        nombre = ""
        telefono = ""
        canal = Canal.WHATSAPP
        direccion = ""
        barrio = ""
        localidad = localidades.first()
        opinion = ""
        concejal = ""
        categoria = null
        conflictoConcejal = null
    }

    fun guardarBorrador() {
        if (nombre.isBlank() && opinion.isBlank() && !notaGuardada() && !grabando) {
            avisar("Escribe un nombre o una opinión para el borrador")
            return
        }
        if (grabando) pausarNota()
        avisar("Borrador guardado en este dispositivo")
    }

    fun refrescar() {
        viewModelScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                runCatching { ComunidadApi.listar() }
            }
            if (enviando) return@launch
            resultado
                .onSuccess { recibidas ->
                    val vigentes = recibidas.map { it.id }.toSet()
                    withContext(Dispatchers.IO) { borrarAudiosAusentes(vigentes) }
                    if (reproduciendoId != null && reproduciendoId !in vigentes) {
                        detenerReproduccion()
                    }
                    respuestas = recibidas
                }
                .onFailure {
                    if (respuestas.isEmpty()) {
                        avisar("Sin conexión con la comunidad. Inténtalo de nuevo")
                    }
                }
        }
    }

    fun avisoConcejal(): String? {
        val escrito = normalizar(concejal)
        val numero = telefono.filter { it.isDigit() }
        if (escrito.isEmpty() || numero.length < 7) return null
        val otros = respuestas.mapNotNull { item ->
            val previo = item.concejal.trim()
            if (item.telefono.filter { it.isDigit() } != numero) return@mapNotNull null
            if (previo.isEmpty() || normalizar(previo) == escrito) return@mapNotNull null
            previo
        }.distinctBy { normalizar(it) }
        if (otros.isEmpty()) return null
        return if (otros.size == 1) {
            "Esta persona ya está registrada con el concejal ${otros[0]}."
        } else {
            "Esta persona ya está registrada con otros concejales: ${otros.joinToString(", ")}."
        }
    }

    fun cancelarConflictoConcejal() {
        conflictoConcejal = null
    }

    fun enviar(forzar: Boolean = false, alTerminar: (Boolean) -> Unit) {
        if (enviando) return
        if (nombre.isBlank() || telefono.isBlank() || opinion.isBlank()) {
            avisar("Completa nombre, teléfono y la opinión")
            alTerminar(false)
            return
        }
        if (!forzar) {
            val otro = avisoConcejal()
            if (otro != null) {
                conflictoConcejal = otro
                alTerminar(false)
                return
            }
        }
        conflictoConcejal = null
        enviando = true
        avisar("Guardando en la comunidad…")
        val duracion = segundos
        val habiaAudio = grabando || audioListo || segundos > 0
        viewModelScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                runCatching {
                    finalizarNota()
                    val archivo = archivoNota?.takeIf { notaGuardada() }
                    if (habiaAudio && archivo == null) {
                        error("sin-audio")
                    }
                    val borrador = Respuesta(
                        id = "",
                        nombre = nombre.trim(),
                        telefono = telefono.trim(),
                        barrio = barrio.trim().ifBlank { localidad.trim() },
                        opinion = opinion.trim(),
                        momento = "Ahora",
                        tieneAudio = archivo != null,
                        duracionSeg = if (archivo != null) duracion.coerceAtLeast(1) else 0,
                        estado = "Enviada a la comunidad",
                        tono = Tono.EXITO,
                        canal = canal.name,
                        direccion = direccion.trim(),
                        localidad = localidad.trim(),
                        categoria = categoria?.name ?: "",
                        concejal = concejal.trim(),
                    )
                    val guardada = ComunidadApi.publicar(borrador, archivo?.readBytes())
                    if (archivo != null) guardarCopia(guardada.id, archivo)
                    guardada
                }
            }
            enviando = false
            resultado
                .onSuccess { guardada ->
                    respuestas = listOf(guardada) + respuestas.filter { it.id != guardada.id }
                    reiniciarFormulario()
                    avisar(
                        if (guardada.tieneAudio) "Encuesta enviada con la nota de voz"
                        else "Encuesta enviada a la comunidad",
                    )
                    alTerminar(true)
                }
                .onFailure { error ->
                    avisar(
                        if (error.message == "sin-audio") "La nota de voz no quedó guardada. Grábala de nuevo"
                        else "No se pudo guardar. Espera un momento y vuelve a intentar",
                    )
                    alTerminar(false)
                }
        }
    }

    fun respuestasVisibles(): List<Respuesta> {
        val consulta = busqueda.trim()
        val filtradas = respuestas.filter { respuesta ->
            val coincideTexto = consulta.isEmpty() ||
                respuesta.nombre.contains(consulta, ignoreCase = true) ||
                respuesta.barrio.contains(consulta, ignoreCase = true) ||
                respuesta.opinion.contains(consulta, ignoreCase = true) ||
                respuesta.telefono.contains(consulta, ignoreCase = true)
            val coincideBarrio = filtroBarrio == "Todos" ||
                respuesta.barrio.contains(filtroBarrio, ignoreCase = true)
            val coincideModo = when (filtroModo) {
                null -> true
                ModoFiltro.AUDIO -> respuesta.tieneAudio
                ModoFiltro.TEXTO -> !respuesta.tieneAudio
                ModoFiltro.URGENTE -> respuesta.urgente
            }
            coincideTexto && coincideBarrio && coincideModo
        }
        return if (recientesPrimero) filtradas else filtradas.asReversed()
    }

    override fun onCleared() {
        liberarGrabador()
        detenerReproduccion()
        tareasGrabacion.shutdownNow()
        super.onCleared()
    }

    private fun comenzarNota(): Boolean {
        liberarGrabador()
        val archivo = File(getApplication<android.app.Application>().filesDir, "nota-actual.m4a")
        if (archivo.exists()) archivo.delete()
        segundos = 0
        audioListo = false
        val recorder = try {
            enGrabacion {
                val creado = nuevoGrabador()
                creado.setAudioSource(MediaRecorder.AudioSource.MIC)
                creado.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                creado.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                creado.setAudioEncodingBitRate(64_000)
                creado.setAudioSamplingRate(44_100)
                creado.setMaxDuration(180_000)
                creado.setOutputFile(archivo.absolutePath)
                creado.prepare()
                creado.start()
                creado
            }
        } catch (_: Exception) {
            archivo.delete()
            grabando = false
            return false
        }
        grabador = recorder
        archivoNota = archivo
        grabando = true
        return true
    }

    private fun pausarNota() {
        val recorder = grabador
        if (recorder != null) {
            runCatching { enGrabacion { recorder.pause() } }
        }
        grabando = false
        if (segundos == 0) segundos = 1
        audioListo = true
    }

    private fun reanudarNota(): Boolean {
        val recorder = grabador ?: return false
        return try {
            enGrabacion { recorder.resume() }
            grabando = true
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun borrarNota() {
        liberarGrabador()
        archivoNota?.delete()
        archivoNota = null
        segundos = 0
        audioListo = false
    }

    private fun liberarGrabador() {
        val recorder = grabador
        grabador = null
        grabando = false
        if (recorder == null) return
        runCatching {
            enGrabacion(6_000) {
                runCatching { recorder.stop() }
                runCatching { recorder.release() }
            }
        }
    }

    private fun <T> enGrabacion(esperaMs: Long = 8_000, bloque: () -> T): T {
        val futuro = tareasGrabacion.submit(bloque)
        try {
            return futuro.get(esperaMs, TimeUnit.MILLISECONDS)
        } catch (error: TimeoutException) {
            futuro.cancel(true)
            throw error
        } catch (error: ExecutionException) {
            throw error.cause ?: error
        }
    }

    private fun notaGuardada(): Boolean {
        val archivo = archivoNota
        return archivo != null && archivo.exists() && archivo.length() > 32L
    }

    private fun nuevoGrabador(): MediaRecorder {
        val aplicacion = getApplication<android.app.Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(aplicacion)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun guardarCopia(id: String, origen: File) {
        val destino = File(carpetaAudios(), "$id.m4a")
        runCatching { origen.copyTo(destino, overwrite = true) }
    }

    private fun obtenerAudio(id: String): File? {
        val local = File(carpetaAudios(), "$id.m4a")
        if (local.exists() && local.length() > 32) return local
        val descargado = runCatching { ComunidadApi.descargarAudio(id, local) }.getOrDefault(false)
        if (descargado && local.exists() && local.length() > 32) return local
        if (local.exists()) local.delete()
        return null
    }

    private fun carpetaAudios(): File {
        return File(getApplication<android.app.Application>().filesDir, "audios").apply { mkdirs() }
    }

    private fun borrarAudiosAusentes(vigentes: Set<String>) {
        carpetaAudios().listFiles().orEmpty().forEach { archivo ->
            val id = archivo.name.removeSuffix(".m4a")
            if (archivo.isFile && id.length == 32 && id !in vigentes) {
                archivo.delete()
            }
        }
    }

    private fun detenerReproduccion() {
        generacionAudio += 1
        reproduciendoId = null
        val player = reproductor
        reproductor = null
        runCatching { player?.stop() }
        runCatching { player?.release() }
    }
}
