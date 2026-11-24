package com.vozcomunidad.app.ui.screens

import android.content.ActivityNotFoundException
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Ballot
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vozcomunidad.app.data.Acento
import com.vozcomunidad.app.data.EncuestaViewModel
import com.vozcomunidad.app.data.ModoFiltro
import com.vozcomunidad.app.data.Respuesta
import com.vozcomunidad.app.data.Tono
import com.vozcomunidad.app.data.formatoTiempo
import com.vozcomunidad.app.ui.compartirTexto
import com.vozcomunidad.app.ui.components.Avatar
import com.vozcomunidad.app.ui.components.CampoLinea
import com.vozcomunidad.app.ui.components.OndaEstatica
import com.vozcomunidad.app.ui.components.Tx
import com.vozcomunidad.app.ui.components.presionSuave
import com.vozcomunidad.app.ui.components.tarjeta
import com.vozcomunidad.app.ui.theme.Vz

@Composable
fun HistorialScreen(
    vm: EncuestaViewModel,
    onNueva: () -> Unit,
) {
    val contexto = LocalContext.current
    val visibles = vm.respuestasVisibles()
    val total = vm.respuestas.size
    val audios = vm.respuestas.count { it.tieneAudio }
    val avance = if (total == 0) "0%" else "${(audios * 100 / total)}%"
    val barrios = listOf("Todos") + vm.respuestas.map { it.barrio }.filter { it.isNotBlank() }.distinct()

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BannerHistorial()
            CampoLinea(
                valor = vm.busqueda,
                alCambiar = { vm.busqueda = it },
                placeholder = "Buscar por vecino, barrio o tema...",
                elevado = true,
                iconoInicio = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = Vz.Outline, modifier = Modifier.size(20.dp))
                },
                iconoFinal = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Vz.SurfaceContainer)
                            .presionSuave { vm.avisar("Filtra por barrio o por tipo de respuesta") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Filtros", tint = Vz.OnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                },
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                barrios.forEach { barrio ->
                    FiltroBarrio(barrio, activo = vm.filtroBarrio == barrio) { vm.filtroBarrio = barrio }
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FiltroModo("Con audio", Icons.Outlined.Mic, Vz.Secondary, vm.filtroModo == ModoFiltro.AUDIO, false) {
                    vm.alternarModo(ModoFiltro.AUDIO)
                }
                FiltroModo("Solo texto", Icons.Outlined.EditNote, Vz.Primary, vm.filtroModo == ModoFiltro.TEXTO, false) {
                    vm.alternarModo(ModoFiltro.TEXTO)
                }
                FiltroModo("Urgente", Icons.Outlined.PriorityHigh, Vz.Error, vm.filtroModo == ModoFiltro.URGENTE, true) {
                    vm.alternarModo(ModoFiltro.URGENTE)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metrica("Total", total.toString(), "Registros", Vz.Primary, Icons.Outlined.Ballot, Modifier.weight(1f))
                Metrica("Audios", audios.toString(), "Notas voz", Vz.Secondary, Icons.Outlined.Mic, Modifier.weight(1f))
                Metrica("Avance", avance, "Con audio", Vz.Tertiary, Icons.Outlined.Verified, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tx("Respuestas Recientes", size = 18.sp, peso = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.background(Vz.SurfaceContainerHighest, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Tx("${vm.respuestas.size} nuevas", color = Vz.OnSurfaceVariant, size = 11.sp, peso = FontWeight.Bold)
                    }
                }
                Row(modifier = Modifier.presionSuave(vm::alternarOrden), verticalAlignment = Alignment.CenterVertically) {
                    Tx("Ordenar", color = Vz.Primary, size = 13.sp, peso = FontWeight.SemiBold)
                    Icon(Icons.Outlined.SwapVert, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(16.dp))
                }
            }
            if (visibles.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().tarjeta(radio = 12.dp).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Tx("Sin respuestas con ese filtro", size = 16.sp, peso = FontWeight.Bold, align = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Tx(
                        "Prueba con otro barrio o quita el filtro de audio, texto o urgente.",
                        color = Vz.OnSurfaceVariant,
                        size = 13.sp,
                        alto = 18.sp,
                        align = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                visibles.forEach { respuesta ->
                    TarjetaRespuesta(
                        respuesta = respuesta,
                        sonando = vm.reproduciendoId == respuesta.id,
                        onPlay = { vm.alternarReproduccion(respuesta.id) },
                        onCompartir = {
                            try {
                                compartirTexto(
                                    contexto,
                                    "Respuesta de ${respuesta.nombre}",
                                    "${respuesta.nombre} · ${respuesta.barrio}\n${respuesta.opinion}",
                                )
                            } catch (_: ActivityNotFoundException) {
                                vm.avisar("No hay una app para compartir")
                            }
                        },
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .height(56.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(Vz.Primary)
                .presionSuave(onNueva)
                .padding(start = 8.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(32.dp).background(Vz.SecondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Vz.OnSecondaryContainer)
            }
            Spacer(Modifier.width(8.dp))
            Tx("Nueva Encuesta", color = Vz.OnPrimary, size = 15.sp, peso = FontWeight.Bold)
        }
    }
}

@Composable
private fun BannerHistorial() {
    Row(
        modifier = Modifier.fillMaxWidth().tarjeta(radio = 12.dp, color = Vz.SurfaceContainerLow).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(Vz.SecondaryContainer.copy(alpha = 0.25f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, tint = Vz.Secondary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Tx("Comunidad activa", size = 13.sp, peso = FontWeight.SemiBold)
            Tx("Escucha y canaliza los reportes vecinales", color = Vz.OnSurfaceVariant, size = 13.sp, alto = 18.sp)
        }
        Row(
            modifier = Modifier.background(Vz.TertiaryContainer.copy(alpha = 0.15f), CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(Vz.Tertiary, CircleShape))
            Spacer(Modifier.width(4.dp))
            Tx("En vivo", color = Vz.Tertiary, size = 11.sp, peso = FontWeight.Bold)
        }
    }
}

@Composable
private fun FiltroBarrio(texto: String, activo: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (activo) Vz.Primary else Vz.SurfaceContainer)
            .presionSuave(onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Tx(
            texto,
            color = if (activo) Vz.OnPrimary else Vz.OnSurfaceVariant,
            size = 11.sp,
            peso = FontWeight.Bold,
        )
    }
}

@Composable
private fun FiltroModo(
    texto: String,
    icono: ImageVector,
    tinta: Color,
    activo: Boolean,
    urgente: Boolean,
    onClick: () -> Unit,
) {
    val fondo = when {
        urgente -> Vz.ErrorContainer.copy(alpha = if (activo) 0.85f else 0.45f)
        activo -> Vz.PrimaryFixed
        else -> Vz.SurfaceContainerLow
    }
    val colorTexto = if (urgente) Vz.Error else if (activo) Vz.Primary else Vz.OnSurface
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(fondo)
            .presionSuave(onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = if (urgente) Vz.Error else tinta, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Tx(texto, color = colorTexto, size = 11.sp, peso = FontWeight.Bold)
    }
}

@Composable
private fun Metrica(
    etiqueta: String,
    valor: String,
    pie: String,
    color: Color,
    icono: ImageVector,
    modifier: Modifier,
) {
    Column(modifier.tarjeta(radio = 12.dp).padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Tx(etiqueta, color = Vz.OnSurfaceVariant, size = 11.sp, peso = FontWeight.Bold)
            Box(Modifier.size(24.dp).background(color.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Tx(valor, color = color, size = 26.sp, peso = FontWeight.ExtraBold, alto = 30.sp, tracking = (-0.5).sp)
        Tx(pie, color = Vz.OnSurfaceVariant, size = 11.sp, peso = FontWeight.SemiBold)
    }
}

@Composable
private fun TarjetaRespuesta(
    respuesta: Respuesta,
    sonando: Boolean,
    onPlay: () -> Unit,
    onCompartir: () -> Unit,
) {
    val colorEstado = when (respuesta.tono) {
        Tono.EXITO -> Vz.Tertiary
        Tono.AVISO -> Vz.Secondary
        Tono.INFO -> Vz.Primary
    }
    val iconoEstado = when (respuesta.tono) {
        Tono.EXITO -> Icons.Filled.CheckCircle
        Tono.AVISO -> Icons.Outlined.PendingActions
        Tono.INFO -> Icons.Outlined.BookmarkAdded
    }
    val fondoBarrio = when (respuesta.acento) {
        Acento.VERDE -> Vz.PrimaryFixed
        Acento.AMBAR -> Vz.SecondaryFixed
    }
    val tintaBarrio = when (respuesta.acento) {
        Acento.VERDE -> Vz.Primary
        Acento.AMBAR -> Vz.Secondary
    }
    Column(modifier = Modifier.fillMaxWidth().tarjeta(radio = 12.dp).padding(16.dp)) {
        Row {
            Avatar(respuesta.nombre, 40.dp, if (respuesta.acento == Acento.AMBAR) Vz.Secondary else Vz.Primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Tx(respuesta.nombre, size = 18.sp, peso = FontWeight.SemiBold, max = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Call, contentDescription = null, tint = Vz.OnSurfaceVariant, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Tx(respuesta.telefono, color = Vz.OnSurfaceVariant, size = 13.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Tx(respuesta.momento, color = Vz.OnSurfaceVariant, size = 11.sp, peso = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.background(fondoBarrio, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = tintaBarrio, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(2.dp))
                    Tx(respuesta.barrio, color = tintaBarrio, size = 11.sp, peso = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (respuesta.tieneAudio) {
            val acento = if (respuesta.acento == Acento.AMBAR) Vz.Secondary else Vz.Tertiary
            val fondoPlay = if (respuesta.acento == Acento.AMBAR) Vz.SecondaryContainer else Vz.Tertiary
            val tintaPlay = if (respuesta.acento == Acento.AMBAR) Vz.OnSecondaryContainer else Vz.OnTertiary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Vz.SurfaceContainerLow)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(fondoPlay).presionSuave(onPlay),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (sonando) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (sonando) "Pausar" else "Reproducir",
                        tint = tintaPlay,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (respuesta.acento == Acento.AMBAR) Icons.Outlined.GraphicEq else Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = null,
                            tint = acento,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Tx(
                            "Audio grabado (${formatoTiempo(respuesta.duracionSeg)})",
                            color = acento,
                            size = 11.sp,
                            peso = FontWeight.Bold,
                        )
                    }
                    OndaEstatica(acento)
                }
                Tx(formatoTiempo(respuesta.duracionSeg), color = acento, size = 11.sp, peso = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        } else {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Vz.Primary.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.EditNote, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Tx("Texto escrito", color = Vz.Primary, size = 11.sp, peso = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
        Tx("“${respuesta.opinion}”", color = Vz.OnSurfaceVariant, size = 15.sp, alto = 22.sp, max = 3, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(iconoEstado, contentDescription = null, tint = colorEstado, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Tx(respuesta.estado, color = colorEstado, size = 11.sp, peso = FontWeight.SemiBold, max = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).presionSuave(onCompartir),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Share, contentDescription = "Compartir", tint = Vz.OnSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}
