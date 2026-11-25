package com.vozcomunidad.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vozcomunidad.app.data.Canal
import com.vozcomunidad.app.data.EncuestaViewModel
import com.vozcomunidad.app.data.formatoTiempo
import com.vozcomunidad.app.ui.accionConMicrofono
import com.vozcomunidad.app.ui.components.AnillosMic
import com.vozcomunidad.app.ui.components.BotonPrimario
import com.vozcomunidad.app.ui.components.CampoLargo
import com.vozcomunidad.app.ui.components.CampoLinea
import com.vozcomunidad.app.ui.components.OndaAudio
import com.vozcomunidad.app.ui.components.Tx
import com.vozcomunidad.app.ui.components.presionSuave
import com.vozcomunidad.app.ui.components.tarjeta
import com.vozcomunidad.app.ui.theme.Vz

@Composable
fun NuevaEncuestaScreen(
    vm: EncuestaViewModel,
    onAbrirGrabacion: () -> Unit,
    onEnviada: () -> Unit,
) {
    val conflicto = vm.conflictoConcejal
    if (conflicto != null) {
        AlertDialog(
            onDismissRequest = vm::cancelarConflictoConcejal,
            containerColor = Vz.SurfaceContainerLowest,
            title = { Tx("Otro concejal", size = 18.sp, peso = FontWeight.Bold) },
            text = { Tx("$conflicto ¿Deseas enviarla de todos modos?", color = Vz.OnSurfaceVariant, size = 15.sp, alto = 22.sp) },
            confirmButton = {
                TextButton(onClick = {
                    vm.enviar(forzar = true) { enviada -> if (enviada) onEnviada() }
                }) { Tx("Enviar", color = Vz.Primary, peso = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelarConflictoConcejal) { Tx("Cancelar", color = Vz.OnSurfaceVariant, peso = FontWeight.SemiBold) }
            },
        )
    }
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BannerRegistro()
        PasoUno(vm)
        TarjetaNombre(vm)
        TarjetaTelefono(vm)
        TarjetaUbicacion(vm)
        TarjetaConcejal(vm)
        TarjetaOpinion(vm, onAbrirGrabacion)
        BotonPrimario(
            texto = if (vm.enviando) "Enviando a la comunidad…" else "Guardar y Enviar Encuesta",
            onClick = { vm.enviar { enviada -> if (enviada) onEnviada() } },
            iconoInicio = Icons.AutoMirrored.Outlined.Send,
            iconoFin = Icons.Filled.CheckCircle,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccionSecundaria(
                texto = "Guardar borrador",
                icono = Icons.Outlined.BookmarkBorder,
                colorFondo = Vz.SurfaceContainerHigh,
                colorTexto = Vz.OnSurfaceVariant,
                modifier = Modifier.weight(1f),
                onClick = vm::guardarBorrador,
            )
            AccionSecundaria(
                texto = "Limpiar formulario",
                icono = Icons.Outlined.DeleteSweep,
                colorFondo = Vz.SurfaceContainerLow,
                colorTexto = Vz.Outline,
                modifier = Modifier.weight(1f),
                onClick = vm::limpiarFormulario,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Shield, contentDescription = null, tint = Vz.Outline, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Tx(
                texto = "Protección de datos conforme a la veeduría ciudadana municipal.",
                color = Vz.Outline,
                size = 13.sp,
                alto = 18.sp,
                align = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun BannerRegistro() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Vz.Primary, Vz.PrimaryContainer, Vz.SurfaceTint)))
            .padding(16.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(112.dp)
                .background(Color.White.copy(alpha = 0.10f), CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = 36.dp)
                .size(72.dp)
                .background(Vz.SecondaryContainer.copy(alpha = 0.22f), CircleShape),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .background(Vz.SecondaryContainer, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).background(Vz.TertiaryContainer, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Tx("Comunidad Activa", color = Vz.OnSecondaryContainer, size = 11.sp, peso = FontWeight.Bold, alto = 14.sp)
                }
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, tint = Vz.PrimaryFixed, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Tx("Voz Directa", color = Vz.PrimaryFixed, size = 11.sp, peso = FontWeight.Bold, alto = 14.sp)
                }
            }
            Tx(
                texto = "Registro de Opinión",
                color = Color.White,
                size = 24.sp,
                peso = FontWeight.Bold,
                alto = 32.sp,
                tracking = (-0.4).sp,
            )
            Tx(
                texto = "Hola, recopila y amplifica las inquietudes del vecindario con total claridad.",
                color = Vz.PrimaryFixedDim,
                size = 13.sp,
                alto = 18.sp,
            )
        }
    }
}

@Composable
private fun PasoUno(vm: EncuestaViewModel) {
    val testimonio = vm.opinion.isNotBlank() || vm.audioListo || vm.grabando || vm.segundos > 0
    val completos = listOf(vm.nombre.isNotBlank(), vm.telefono.isNotBlank(), vm.barrio.isNotBlank(), testimonio).count { it }
    val avance = completos / 4f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tarjeta(radio = 12.dp, color = Vz.SurfaceContainerLow)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(20.dp).background(Vz.Primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Tx("1", color = Vz.OnPrimary, size = 11.sp, peso = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
                Tx("Paso 1 de 2: Datos y Testimonio", size = 13.sp, peso = FontWeight.SemiBold, alto = 16.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Verified, contentDescription = null, tint = Vz.Tertiary, modifier = Modifier.size(16.dp))
                Tx("${(avance * 100).toInt()}%", color = Vz.Tertiary, size = 11.sp, peso = FontWeight.Bold)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Vz.SurfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(avance.coerceAtLeast(0.02f))
                    .height(8.dp)
                    .background(Brush.horizontalGradient(listOf(Vz.Primary, Vz.Tertiary))),
            )
        }
    }
}

@Composable
private fun TarjetaNombre(vm: EncuestaViewModel) {
    Column(
        modifier = Modifier.tarjeta().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Tx("Nombre completo del ciudadano", size = 13.sp, peso = FontWeight.SemiBold, alto = 16.sp)
            }
            Tx("Requerido", color = Vz.Tertiary, size = 11.sp, peso = FontWeight.SemiBold)
        }
        CampoLinea(
            valor = vm.nombre,
            alCambiar = { vm.nombre = it },
            placeholder = "Nombre y apellido",
            iconoFinal = if (vm.nombre.isNotBlank()) {
                {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Completo", tint = Vz.Tertiary, modifier = Modifier.size(20.dp))
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun TarjetaTelefono(vm: EncuestaViewModel) {
    Column(
        modifier = Modifier.tarjeta().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PhoneIphone, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Tx("Teléfono de contacto", size = 13.sp, peso = FontWeight.SemiBold)
            }
            if (vm.telefono.count { it.isDigit() } >= 7) {
                Row(
                    modifier = Modifier
                        .background(Vz.TertiaryFixed, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Vz.OnTertiaryFixed, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Tx("Válido", color = Vz.OnTertiaryFixed, size = 11.sp, peso = FontWeight.Bold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Vz.SurfaceContainerHigh)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tx("🇨🇴", size = 16.sp, familia = FontFamily.Default)
                Spacer(Modifier.width(4.dp))
                Tx("+57", size = 13.sp, peso = FontWeight.SemiBold)
            }
            CampoLinea(
                valor = vm.telefono,
                alCambiar = { vm.telefono = it },
                placeholder = "300 123 4567",
                teclado = KeyboardType.Phone,
                modifier = Modifier.weight(1f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tx("Preferencia:", color = Vz.OnSurfaceVariant, size = 11.sp, peso = FontWeight.Bold)
            CanalBoton("WhatsApp", Icons.AutoMirrored.Outlined.Chat, vm.canal == Canal.WHATSAPP, Modifier.weight(1f)) {
                vm.canal = Canal.WHATSAPP
            }
            CanalBoton("Llamada", Icons.Outlined.Call, vm.canal == Canal.LLAMADA, Modifier.weight(1f)) {
                vm.canal = Canal.LLAMADA
            }
        }
    }
}

@Composable
private fun CanalBoton(
    texto: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    activo: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (activo) Vz.TertiaryFixed else Vz.SurfaceContainerHigh)
            .presionSuave(onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = if (activo) Vz.OnTertiaryFixed else Vz.OnSurface,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Tx(
            texto,
            color = if (activo) Vz.OnTertiaryFixed else Vz.OnSurface,
            size = 11.sp,
            peso = FontWeight.Bold,
        )
    }
}

@Composable
private fun TarjetaConcejal(vm: EncuestaViewModel) {
    val aviso = vm.avisoConcejal()
    Column(
        modifier = Modifier.tarjeta().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Tx("Concejal", size = 13.sp, peso = FontWeight.SemiBold)
            }
            Tx("Opcional", color = Vz.OnSurfaceVariant, size = 11.sp, peso = FontWeight.SemiBold)
        }
        CampoLinea(
            valor = vm.concejal,
            alCambiar = { vm.concejal = it },
            placeholder = "Concejal",
        )
        if (aviso != null) {
            Tx(
                aviso,
                color = Vz.Secondary,
                size = 13.sp,
                alto = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Vz.SecondaryFixed)
                    .padding(10.dp),
            )
        }
    }
}

@Composable
private fun TarjetaUbicacion(vm: EncuestaViewModel) {
    var menuAbierto by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.tarjeta().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Vz.Secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Tx("Ubicación vecinal", size = 13.sp, peso = FontWeight.SemiBold)
            }
        }
        CampoLinea(
            valor = vm.direccion,
            alCambiar = { vm.direccion = it },
            placeholder = "Calle / Carrera / Número (Ej. Cra 15 # 45-20)",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoLinea(
                valor = vm.barrio,
                alCambiar = { vm.barrio = it },
                placeholder = "Barrio o Sector",
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Vz.SurfaceContainerLow)
                        .presionSuave { menuAbierto = true }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Tx(vm.localidad, size = 13.sp, max = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Localidades", tint = Vz.Outline)
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    vm.localidades.forEach { localidad ->
                        DropdownMenuItem(
                            text = { Tx(localidad, size = 14.sp) },
                            onClick = {
                                vm.localidad = localidad
                                menuAbierto = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaOpinion(vm: EncuestaViewModel, onAbrirGrabacion: () -> Unit) {
    val grabar = accionConMicrofono(
        alPermitir = vm::alternarGrabacion,
        alDenegar = { vm.avisar("Activa el micrófono para guardar la nota de voz") },
    )
    val repetir = accionConMicrofono(
        alPermitir = vm::repetirAudio,
        alDenegar = { vm.avisar("Activa el micrófono para guardar la nota de voz") },
    )
    Column(
        modifier = Modifier.tarjeta().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(6.dp))
                Tx("Opinión o Inquietud", size = 18.sp, peso = FontWeight.SemiBold, alto = 24.sp)
            }
        }
        Tx(
            texto = "Escribe la inquietud o grábalo con la voz. Con uno de los dos se puede guardar.",
            color = Vz.OnSurfaceVariant,
            size = 13.sp,
            alto = 18.sp,
        )
        CampoLargo(
            valor = vm.opinion,
            alCambiar = { vm.opinion = it },
            placeholder = "Escriba aquí notas clave del testimonio o resumen de la solicitud ciudadana...",
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Vz.SurfaceContainerHigh, Vz.SurfaceContainer, Vz.SurfaceContainerLow),
                    ),
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .background(Vz.SecondaryContainer, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.GraphicEq, contentDescription = null, tint = Vz.OnSecondaryContainer, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Tx("Voz Ciudadana Auténtica", color = Vz.OnSecondaryContainer, size = 11.sp, peso = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Tx(
                texto = "¿Prefiere grabar su inquietud con la voz?",
                size = 18.sp,
                peso = FontWeight.SemiBold,
                alto = 24.sp,
                align = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Tx(
                texto = "Grabe una nota de audio clara de hasta 3 minutos para mayor fidelidad comunitaria.",
                color = Vz.OnSurfaceVariant,
                size = 13.sp,
                alto = 18.sp,
                align = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Box(contentAlignment = Alignment.Center) {
                AnillosMic(activo = vm.grabando, modifier = Modifier.size(128.dp))
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Vz.Secondary, Vz.SecondaryContainer)))
                        .presionSuave(grabar),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = "Grabar audio", tint = Vz.OnSecondary, modifier = Modifier.size(36.dp))
                }
            }
            Tx(
                texto = when {
                    vm.grabando -> "Tocar para Pausar Grabación"
                    vm.segundos > 0 -> "Tocar para Reanudar Audio"
                    else -> "Tocar para grabar"
                },
                size = 15.sp,
                peso = FontWeight.SemiBold,
                alto = 20.sp,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (vm.grabando) {
                    Box(Modifier.size(10.dp).background(Vz.Error, CircleShape))
                    Spacer(Modifier.width(8.dp))
                }
                Tx(formatoTiempo(vm.segundos), size = 18.sp, peso = FontWeight.SemiBold, familia = FontFamily.Monospace)
                Tx(" / 03:00", color = Vz.OutlineVariant, size = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .tarjeta(radio = 12.dp)
                    .padding(12.dp),
            ) {
                OndaAudio(activa = vm.grabando, modifier = Modifier.fillMaxWidth(), barras = 18)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Vz.Tertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Tx(
                            texto = if (vm.audioListo && vm.segundos > 0) "Nota de voz lista para adjuntar" else "Aún no hay nota de voz",
                            color = Vz.Tertiary,
                            size = 11.sp,
                            peso = FontWeight.SemiBold,
                            alto = 14.sp,
                        )
                    }
                    Row(
                        modifier = Modifier.presionSuave(repetir),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Replay, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(14.dp))
                        Tx("Repetir", color = Vz.Primary, size = 11.sp, peso = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.presionSuave(onAbrirGrabacion).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Tx("Abrir detalle de grabación", color = Vz.Primary, size = 13.sp, peso = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun AccionSecundaria(
    texto: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    colorFondo: Color,
    colorTexto: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorFondo)
            .presionSuave(onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = colorTexto, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Tx(texto, color = colorTexto, size = 13.sp, peso = FontWeight.SemiBold)
    }
}
