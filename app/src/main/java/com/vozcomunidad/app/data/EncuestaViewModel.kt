package com.vozcomunidad.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EncuestaViewModel : ViewModel() {
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
                        grabando = false
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
        grabando = !grabando
        if (!grabando && segundos > 0) audioListo = true
    }

    fun repetirAudio() {
        segundos = 0
        grabando = true
        audioListo = false
    }

    fun descartarAudio() {
        segundos = 0
        grabando = false
        audioListo = false
        avisar("Audio descartado. Puedes volver a grabar")
    }

    fun alternarModo(modo: ModoFiltro) {
        filtroModo = if (filtroModo == modo) null else modo
    }

    fun alternarOrden() {
        recientesPrimero = !recientesPrimero
        avisar(if (recientesPrimero) "Orden: más recientes" else "Orden: más antiguas")
    }

    fun alternarReproduccion(id: String) {
        reproduciendoId = if (reproduciendoId == id) null else id
    }

    fun limpiarFormulario() {
        reiniciarFormulario()
        avisar("Formulario limpio")
    }

    private fun reiniciarFormulario() {
        nombre = ""
        telefono = ""
        canal = Canal.WHATSAPP
        direccion = ""
        barrio = ""
        localidad = localidades.first()
        opinion = ""
        grabando = false
        segundos = 0
        audioListo = false
        categoria = null
    }

    fun guardarBorrador() {
        if (nombre.isBlank() && opinion.isBlank()) {
            avisar("Escribe un nombre o una opinión para el borrador")
            return
        }
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
        val borrador = Respuesta(
            id = "",
            nombre = nombre.trim(),
            telefono = telefono.trim(),
            barrio = barrio.ifBlank { localidad },
            opinion = opinion.trim(),
            momento = "Ahora",
            tieneAudio = audioListo && segundos > 0,
            duracionSeg = if (audioListo) segundos else 0,
            estado = "Enviada a la comunidad",
            tono = Tono.EXITO,
        )
        enviando = true
        viewModelScope.launch {
            val resultado = withContext(Dispatchers.IO) {
                runCatching { ComunidadApi.publicar(borrador) }
            }
            enviando = false
            resultado
                .onSuccess { guardada ->
                    respuestas = listOf(guardada) + respuestas.filter { it.id != guardada.id }
                    reiniciarFormulario()
                    avisar("Encuesta enviada a la comunidad")
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
}
