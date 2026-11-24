package com.vozcomunidad.app.ui.screens

import android.content.ActivityNotFoundException
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.vozcomunidad.app.data.EncuestaViewModel
import com.vozcomunidad.app.data.formatoTiempo
import com.vozcomunidad.app.ui.compartirCsv
import com.vozcomunidad.app.ui.compartirTexto
import com.vozcomunidad.app.ui.components.Avatar
import com.vozcomunidad.app.ui.components.OndaAudio
import com.vozcomunidad.app.ui.components.Tx
import com.vozcomunidad.app.ui.components.presionSuave
import com.vozcomunidad.app.ui.components.tarjeta
import com.vozcomunidad.app.ui.theme.Vz

@Composable
fun EstadisticasScreen(vm: EncuestaViewModel) {
    val contexto = LocalContext.current
    val panorama = panoramaDe(vm.respuestas)
    val propios = vm.respuestas.filter { it.tieneAudio }.map { respuesta ->
        TestimonioUi(
            nombre = "${respuesta.nombre} • ${respuesta.barrio.ifBlank { "Sin barrio" }}",
            cita = "“${respuesta.opinion}”",
            cuando = respuesta.momento,
            duracion = formatoTiempo(respuesta.duracionSeg),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Vz.SurfaceContainer, Vz.SurfaceContainerLowest)))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Insights, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Tx("VOZ COLECTIVA ACTIVA", color = Vz.Primary, size = 11.sp, peso = FontWeight.Bold, tracking = 0.6.sp)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tx(panorama.total.toString(), size = 28.sp, peso = FontWeight.ExtraBold, alto = 36.sp, tracking = (-0.6).sp)
                }
                Tx("Inquietudes registradas en esta sesión", color = Vz.OnSurfaceVariant, size = 13.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Vz.SurfaceContainerHighest.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                OndaAudio(activa = panorama.total > 0, barras = 5, alto = 32.dp)
            }
        }

        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationCity, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Tx("Inquietudes por Sector", size = 18.sp, peso = FontWeight.SemiBold)
                    }
                    Tx("Top zonas con mayor participación", color = Vz.OnSurfaceVariant, size = 13.sp)
                }
                Box(Modifier.background(Vz.SurfaceContainerHigh, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Tx(if (panorama.sectores.isEmpty()) "Sin datos" else "${panorama.sectores.size} zonas", color = Vz.Primary, size = 11.sp, peso = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.tarjeta(radio = 12.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (panorama.sectores.isEmpty()) {
                    Tx("Aún no hay respuestas por sector.", color = Vz.OnSurfaceVariant, size = 13.sp)
                }
                panorama.sectores.forEach { sector ->
                    val ancho by animateFloatAsState(sector.porcentaje, tween(700), label = sector.nombre)
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).background(sector.color, CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Tx(sector.nombre, size = 13.sp, peso = FontWeight.SemiBold, max = 2)
                            }
                            Spacer(Modifier.width(8.dp))
                            Tx(
                                "${sector.reportes} (${(sector.porcentaje * 100).toInt()}%)",
                                color = sector.colorTexto,
                                size = 12.sp,
                                peso = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Vz.SurfaceContainer)) {
                            Box(
                                Modifier
                                    .fillMaxWidth(ancho.coerceIn(0.04f, 1f))
                                    .height(10.dp)
                                    .background(sector.color, CircleShape),
                            )
                        }
                    }
                }
            }
        }

        Column(Modifier.tarjeta(radio = 12.dp).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(36.dp).background(Vz.Primary.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Psychology, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Tx("Resumen", size = 18.sp, peso = FontWeight.SemiBold)
                        Tx("Solo con las respuestas guardadas", color = Vz.OnSurfaceVariant, size = 11.sp, peso = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Vz.SurfaceContainerHigh.copy(alpha = 0.55f))
                    .padding(10.dp),
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Tx(panorama.hallazgo, color = Vz.OnSurfaceVariant, size = 13.sp, alto = 18.sp)
            }
        }

        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CloudQueue, contentDescription = null, tint = Vz.Secondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Tx("Temas Más Mencionados", size = 18.sp, peso = FontWeight.SemiBold)
                    }
                    Tx("Frecuencia en notas de voz y opiniones", color = Vz.OnSurfaceVariant, size = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.tarjeta(radio = 12.dp).padding(16.dp)) {
                Tx(
                    "Aún no hay temas agrupados. Esta sección solo mostrará lo que salga de las respuestas reales.",
                    color = Vz.OnSurfaceVariant,
                    size = 13.sp,
                    alto = 18.sp,
                )
            }
        }

        Column(Modifier.tarjeta(radio = 12.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Tx("Testimonios Recientes", size = 18.sp, peso = FontWeight.SemiBold)
                }
                Tx("Con las notas guardadas", color = Vz.OnSurfaceVariant, size = 11.sp, peso = FontWeight.SemiBold)
            }
            if (propios.isEmpty()) {
                Tx("Aún no hay testimonios con audio.", color = Vz.OnSurfaceVariant, size = 13.sp)
            } else {
                propios.forEach { testimonio ->
                    FilaTestimonio(testimonio)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Vz.Primary)
                    .presionSuave {
                        try {
                            compartirCsv(contexto, vm.respuestas)
                            vm.avisar("Generando archivo CSV/Excel...")
                        } catch (_: ActivityNotFoundException) {
                            vm.avisar("No hay una app para abrir el archivo")
                        }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.TableView, contentDescription = null, tint = Vz.OnPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Tx("Exportar Reporte a Excel / CSV", color = Vz.OnPrimary, size = 15.sp, peso = FontWeight.Bold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Vz.SurfaceContainerHigh)
                    .presionSuave {
                        try {
                            compartirTexto(
                                contexto,
                                "Compartir con líderes",
                                "VozComunidad · ${panorama.total} inquietudes.\n${panorama.hallazgo}",
                            )
                            vm.avisar("Listo para compartir con líderes comunitarios")
                        } catch (_: ActivityNotFoundException) {
                            vm.avisar("No hay una app para compartir")
                        }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = Vz.Secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Tx("Compartir con Líderes Comunitarios", size = 13.sp, peso = FontWeight.SemiBold)
            }
        }
    }
}

private data class TestimonioUi(
    val nombre: String,
    val cita: String,
    val cuando: String,
    val duracion: String,
)

@Composable
private fun FilaTestimonio(testimonio: TestimonioUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Vz.SurfaceContainerLow)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Avatar(testimonio.nombre, 48.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .background(Vz.Tertiary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Tx(testimonio.nombre, size = 13.sp, peso = FontWeight.SemiBold, modifier = Modifier.weight(1f), max = 1, overflow = TextOverflow.Ellipsis)
                Tx(testimonio.cuando, color = Vz.OnSurfaceVariant, size = 11.sp)
            }
            Tx(testimonio.cita, color = Vz.OnSurfaceVariant, size = 13.sp, max = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PlayCircle, contentDescription = null, tint = Vz.Primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Tx(testimonio.duracion, color = Vz.Primary, size = 11.sp, peso = FontWeight.SemiBold)
            }
        }
    }
}
