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

class EncuestaViewModel(application: android.app.Application) : AndroidViewModel(application) {
    var nombre by mutableStateOf("")
    var telefono by mutableStateOf("")
    var canal by mutableStateOf(Canal.WHATSAPP)
    var direccion by mutableStateOf("")
    var barrio by mutableStateOf("")
    var localidad by mutableStateOf("Comuna / Localidad Centro")
    var opinion by mutableStateOf("")
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

    private var grabador: MediaRecorder? = null
    private var archivoNota: File? = null
    private var reproductor: MediaPlayer? = null
    private var generacionAudio = 0

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
        val url = ComunidadApi.urlAudio(id)
        viewModelScope.launch {
            val player = MediaPlayer()
            val listo = withContext(Dispatchers.IO) {
                runCatching {
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    player.setDataSource(url)
                    player.prepare()
                }
            }
            if (generacion != generacionAudio) {
                runCatching { player.release() }
                return@launch
            }
            listo.onSuccess {
                reproductor = player
                player.setOnCompletionListener { detenerReproduccion() }
                player.start()
            }.onFailure {
                runCatching { player.release() }
                reproduciendoId = null
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
        categoria = null
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
                .onSuccess { respuestas = it }
                .onFailure {
                    if (respuestas.isEmpty()) {
                        avisar("Sin conexión con la comunidad. Inténtalo de nuevo")
                    }
                }
        }
    }

    fun enviar(alTerminar: (Boolean) -> Unit) {
        if (enviando) return
        if (nombre.isBlank() || telefono.isBlank() || opinion.isBlank()) {
            avisar("Completa nombre, teléfono y la opinión")
            alTerminar(false)
            return
        }
        finalizarNota()
        val archivo = archivoNota?.takeIf { notaGuardada() }
        if ((segundos > 0 || audioListo) && archivo == null) {
            avisar("La nota de voz no quedó guardada. Grábala de nuevo")
            alTerminar(false)
            return
        }
        val borrador = Respuesta(
            id = "",
            nombre = nombre.trim(),
            telefono = telefono.trim(),
            barrio = barrio.ifBlank { localidad },
            opinion = opinion.trim(),
            momento = "Ahora",
            tieneAudio = archivo != null,
            duracionSeg = if (archivo != null) segundos else 0,
            estado = "Enviada a la comunidad",
            tono = Tono.EXITO,
        )
        enviando = true
        viewModelScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                runCatching { ComunidadApi.publicar(borrador, archivo?.readBytes()) }
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
                .onFailure {
                    avisar("No se pudo enviar. Revisa la conexión e inténtalo de nuevo")
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
        super.onCleared()
    }

    private fun comenzarNota(): Boolean {
        liberarGrabador()
        val archivo = File(getApplication<android.app.Application>().filesDir, "nota-actual.m4a")
        if (archivo.exists()) archivo.delete()
        segundos = 0
        audioListo = false
        val recorder = nuevoGrabador()
        return try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(64_000)
            recorder.setAudioSamplingRate(44_100)
            recorder.setMaxDuration(180_000)
            recorder.setOutputFile(archivo.absolutePath)
            recorder.prepare()
            recorder.start()
            grabador = recorder
            archivoNota = archivo
            grabando = true
            true
        } catch (_: Exception) {
            runCatching { recorder.release() }
            archivo.delete()
            grabando = false
            false
        }
    }

    private fun pausarNota() {
        runCatching { grabador?.pause() }
        grabando = false
        if (segundos == 0) segundos = 1
        audioListo = true
    }

    private fun reanudarNota(): Boolean {
        return try {
            grabador?.resume()
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
        if (recorder != null) {
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
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

    private fun detenerReproduccion() {
        generacionAudio += 1
        reproduciendoId = null
        val player = reproductor
        reproductor = null
        runCatching { player?.stop() }
        runCatching { player?.release() }
    }
}
