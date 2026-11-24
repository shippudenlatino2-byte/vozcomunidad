package com.vozcomunidad.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.LocalPolice
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Traffic
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vozcomunidad.app.data.Categoria
import com.vozcomunidad.app.data.EncuestaViewModel
import com.vozcomunidad.app.data.formatoTiempo
import com.vozcomunidad.app.ui.components.AnillosMic
import com.vozcomunidad.app.ui.components.Avatar
import com.vozcomunidad.app.ui.components.MarcaVoz
import com.vozcomunidad.app.ui.components.OndaAudio
import com.vozcomunidad.app.ui.components.Tx
import com.vozcomunidad.app.ui.components.presionSuave
import com.vozcomunidad.app.ui.components.tarjeta
import com.vozcomunidad.app.ui.theme.Vz

private data class CategoriaVisual(
    val id: Categoria,
    val titulo: String,
    val detalle: String,
    val icono: ImageVector,
    val fondo: Color,
    val tinta: Color,
)

@Composable
fun GrabacionScreen(
    vm: EncuestaViewModel,
    onVolver: () -> Unit,
) {
    var confirmar by remember { mutableStateOf(false) }
    var procesando by remember { mutableStateOf(false) }
    val categorias = listOf(
        CategoriaVisual(Categoria.SEGURIDAD, "Seguridad", "Vigilancia y luces", Icons.Outlined.LocalPolice, Vz.SecondaryFixed, Vz.OnSecondaryFixed),
        CategoriaVisual(Categoria.SERVICIOS, "Serv. Públicos", "Agua, energía, gas", Icons.Outlined.WaterDrop, Vz.SurfaceContainerHigh, Vz.Primary),
        CategoriaVisual(Categoria.INFRAESTRUCTURA, "Infraestructura", "Vías, aceras, puentes", Icons.Outlined.Traffic, Vz.TertiaryFixed, Vz.OnTertiaryFixed),
        CategoriaVisual(Categoria.AMBIENTE, "Medio Ambiente", "Aseo y zonas verdes", Icons.Outlined.Eco, Vz.SurfaceContainerHigh, Vz.OnSurface),
    )

    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            containerColor = Vz.SurfaceContainerLowest,
            title = { Tx("Descartar audio", size = 18.sp, peso = FontWeight.Bold) },
            text = { Tx("¿Desea descartar este audio y reiniciar la grabación?", color = Vz.OnSurfaceVariant, size = 15.sp, alto = 22.sp) },
            confirmButton = {
                TextButton(onClick = {
                    vm.descartarAudio()
                    confirmar = false
                }) { Tx("Descartar", color = Vz.Error, peso = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmar = false }) { Tx("Cancelar", color = Vz.Primary, peso = FontWeight.SemiBold) }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Vz.Surface)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        EncabezadoDetalle(onVolver)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Tx("Paso 3 de 4: Evidencia Oral", color = Vz.Primary, size = 13.sp, peso = FontWeight.SemiBold)
                    }
                    Box(Modifier.background(Vz.SurfaceContainerHigh, CircleShape).padding(horizontal = 10.dp, vertical = 2.dp)) {
                        Tx(if (vm.segundos > 0) "Con audio" else "Sin audio", size = 11.sp, peso = FontWeight.Bold)
                    }
                }
                Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Vz.SurfaceContainerHighest)) {
                    Box(
                        Modifier
                            .fillMaxWidth(if (vm.segundos > 0) 1f else 0.25f)
                            .height(6.dp)
                            .background(Vz.TertiaryContainer, CircleShape),
                    )
                }
            }
            TarjetaCiudadano(vm)
            TarjetaMicrofono(vm, onDescartar = { confirmar = true })
            TarjetaTranscripcion(vm)
            TarjetaCategorias(vm, categorias)
            TarjetaFoto()
            BotonFinalizar(procesando) {
                if (!procesando) {
                    procesando = true
                    vm.grabando = false
                    vm.audioListo = vm.segundos > 0
                }
            }
            if (procesando) {
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(900)
                    vm.avisar("Audio adjuntado a la encuesta")
                    onVolver()
                }
            }
            Tx(
                texto = "Al finalizar, el audio se procesará cifrado respetando la ley de protección ciudadana.",
                color = Vz.OnSurfaceVariant,
                size = 11.sp,
                peso = FontWeight.SemiBold,
                align = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun EncabezadoDetalle(onVolver: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(1.dp)
            .background(Vz.Surface.copy(alpha = 0.94f))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).presionSuave(onVolver),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Vz.OnSurface)
        }
        MarcaVoz(28.dp)
        Spacer(Modifier.width(8.dp))
        Tx("Detalle De Encuesta", size = 18.sp, peso = FontWeight.SemiBold, modifier = Modifier.weight(1f), max = 1, overflow = TextOverflow.Ellipsis)
        Box(Modifier.size(32.dp).background(Vz.Primary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Person, contentDescription = "Perfil", tint = Vz.OnPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TarjetaCiudadano(vm: EncuestaViewModel) {
    Column(modifier = Modifier.tarjeta(radio = 12.dp).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Avatar(nombre = vm.nombre.ifBlank { "Ciudadano" }, tamano = 48.dp)
                if (vm.nombre.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .background(Vz.Tertiary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Vz.OnTertiary, modifier = Modifier.size(10.dp))
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Tx(vm.nombre.ifBlank { "Ciudadano sin nombre" }, size = 18.sp, peso = FontWeight.Bold, alto = 22.sp, max = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PinDrop, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Tx(vm.barrio.ifBlank { "Barrio sin definir" }, color = Vz.OnSurfaceVariant, size = 13.sp, max = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(
                modifier = Modifier.background(Vz.SurfaceContainer, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Verified, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Tx("Encuestado", color = Vz.Primary, size = 11.sp, peso = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Vz.SurfaceContainerLow.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Phone, contentDescription = null, tint = Vz.OnSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Tx(if (vm.telefono.isBlank()) "Sin teléfono" else "+57 ${vm.telefono}", size = 13.sp, peso = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TarjetaMicrofono(
    vm: EncuestaViewModel,
    onDescartar: () -> Unit,
) {
    Column(
        modifier = Modifier.tarjeta(radio = 12.dp).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.background(Vz.SurfaceContainerLow, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).background(if (vm.grabando) Vz.Error else Vz.Outline, CircleShape))
            Spacer(Modifier.width(8.dp))
            Tx(
                texto = when {
                vm.grabando -> "GRABANDO EN VIVO..."
                vm.segundos > 0 -> "GRABACIÓN EN PAUSA"
                else -> "LISTO PARA GRABAR"
            },
                size = 11.sp,
                peso = FontWeight.Bold,
                tracking = 0.6.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Tx(formatoTiempo(vm.segundos), size = 28.sp, peso = FontWeight.ExtraBold, alto = 36.sp, tracking = (-0.5).sp)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Vz.SurfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            OndaAudio(activa = vm.grabando, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), barras = 22, alto = 52.dp)
        }
        Spacer(Modifier.height(8.dp))
        Box(contentAlignment = Alignment.Center) {
            AnillosMic(activo = vm.grabando, modifier = Modifier.size(128.dp))
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .shadow(12.dp, CircleShape, ambientColor = Vz.SecondaryContainer.copy(alpha = 0.45f), spotColor = Vz.Secondary.copy(alpha = 0.35f))
                    .clip(CircleShape)
                    .background(Vz.SecondaryContainer)
                    .presionSuave(vm::alternarGrabacion),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Micrófono", tint = Vz.OnSecondaryContainer, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Tx(
            texto = "Hable de forma clara. Toque los botones inferiores para pausar o revisar.",
            color = Vz.OnSurfaceVariant,
            size = 13.sp,
            alto = 18.sp,
            align = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlAudio("Descartar", Icons.Outlined.Delete, Vz.Error, Modifier.weight(1f), onDescartar)
            ControlAudio(
                texto = when {
                    vm.grabando -> "Pausar"
                    vm.segundos > 0 -> "Reanudar"
                    else -> "Grabar"
                },
                icono = if (vm.grabando) Icons.Outlined.PauseCircle else Icons.Outlined.PlayArrow,
                tinta = Vz.Primary,
                modifier = Modifier.weight(1f),
                onClick = vm::alternarGrabacion,
            )
        }
    }
}

@Composable
private fun ControlAudio(
    texto: String,
    icono: ImageVector,
    tinta: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Vz.SurfaceContainerHigh)
            .presionSuave(onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = tinta, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Tx(texto, size = 13.sp, peso = FontWeight.SemiBold, max = 1)
    }
}

@Composable
private fun TarjetaTranscripcion(vm: EncuestaViewModel) {
    Column(modifier = Modifier.tarjeta(radio = 12.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Tx("Notas de la inquietud", size = 13.sp, peso = FontWeight.Bold)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Vz.SurfaceContainerLow)
                .padding(12.dp),
        ) {
            Tx(
                texto = if (vm.opinion.isBlank()) "La opinión escrita aparece aquí. No hay una transcripción de ejemplo." else vm.opinion,
                size = 15.sp,
                alto = 22.sp,
                color = if (vm.opinion.isBlank()) Vz.OnSurfaceVariant else Vz.OnSurface,
            )
        }
    }
}

@Composable
private fun TarjetaCategorias(vm: EncuestaViewModel, categorias: List<CategoriaVisual>) {
    Column(modifier = Modifier.tarjeta(radio = 12.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Tx("Categoría de la inquietud", size = 18.sp, peso = FontWeight.Bold)
            Tx("Seleccione una", color = Vz.OnSurfaceVariant, size = 11.sp, peso = FontWeight.SemiBold)
        }
        categorias.chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { categoria ->
                    val activa = vm.categoria == categoria.id
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (activa) Vz.SurfaceContainer else Vz.SurfaceContainerLow)
                            .presionSuave { vm.categoria = categoria.id }
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(Modifier.size(32.dp).background(categoria.fondo, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(categoria.icono, contentDescription = null, tint = categoria.tinta, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Tx(categoria.titulo, size = 13.sp, peso = FontWeight.Bold, modifier = Modifier.weight(1f), max = 1, overflow = TextOverflow.Ellipsis)
                                if (activa) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Seleccionada", tint = Vz.TertiaryContainer, modifier = Modifier.size(16.dp))
                                }
                            }
                            Tx(categoria.detalle, color = Vz.OnSurfaceVariant, size = 11.sp, alto = 14.sp, max = 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaFoto() {
    Column(modifier = Modifier.tarjeta(radio = 12.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Vz.Secondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Tx("Fotografía del punto", size = 13.sp, peso = FontWeight.SemiBold)
        }
        Tx(
            texto = "Sin fotografía adjunta.",
            color = Vz.OnSurfaceVariant,
            size = 13.sp,
        )
    }
}

@Composable
private fun BotonFinalizar(procesando: Boolean, onClick: () -> Unit) {
    val giro by rememberInfiniteTransition(label = "giro").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
        label = "grados",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(if (procesando) Vz.Tertiary else Vz.Primary)
            .presionSuave(onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (procesando) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Vz.OnPrimary, modifier = Modifier.size(20.dp).rotate(giro))
            Spacer(Modifier.width(8.dp))
            Tx("Guardando y procesando testimonio...", color = Vz.OnPrimary, size = 15.sp, peso = FontWeight.Bold)
        } else {
            Box(Modifier.size(24.dp).background(Vz.TertiaryFixed, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Vz.OnTertiaryFixed, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Tx("Finalizar Grabación y Adjuntar", color = Vz.OnPrimary, size = 15.sp, peso = FontWeight.Bold, max = 1)
        }
    }
}
