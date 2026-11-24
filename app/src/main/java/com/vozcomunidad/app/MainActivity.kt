package com.vozcomunidad.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vozcomunidad.app.data.EncuestaViewModel
import com.vozcomunidad.app.ui.components.MarcaVoz
import com.vozcomunidad.app.ui.components.PastillaAviso
import com.vozcomunidad.app.ui.components.Tx
import com.vozcomunidad.app.ui.components.presionSuave
import com.vozcomunidad.app.ui.screens.EstadisticasScreen
import com.vozcomunidad.app.ui.screens.GrabacionScreen
import com.vozcomunidad.app.ui.screens.HistorialScreen
import com.vozcomunidad.app.ui.screens.NuevaEncuestaScreen
import com.vozcomunidad.app.ui.theme.VozTema
import com.vozcomunidad.app.ui.theme.Vz
import kotlinx.coroutines.delay

private object Rutas {
    const val NUEVA = "nueva"
    const val HISTORIAL = "historial"
    const val ESTADISTICAS = "estadisticas"
    const val GRABACION = "grabacion"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(AndroidColor.WHITE, AndroidColor.WHITE),
        )
        setContent {
            VozTema {
                VozComunidadApp()
            }
        }
    }
}

@Composable
private fun VozComunidadApp(vm: EncuestaViewModel = viewModel()) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route ?: Rutas.NUEVA
    val enGrabacion = ruta == Rutas.GRABACION
    val aviso = vm.aviso

    LaunchedEffect(aviso?.marca) {
        val marca = aviso?.marca ?: return@LaunchedEffect
        delay(2400)
        vm.consumirAviso(marca)
    }

    Scaffold(
        containerColor = Vz.Surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!enGrabacion) {
                Encabezado(
                    subtitulo = when (ruta) {
                        Rutas.HISTORIAL -> "Respuestas Historial"
                        Rutas.ESTADISTICAS -> "Estadísticas"
                        else -> "Nueva Encuesta"
                    },
                    onPerfil = { vm.avisar("Sesión de veeduría vecinal") },
                )
            }
        },
        bottomBar = {
            if (!enGrabacion) {
                BarraInferior(ruta) { destino -> nav.abrirTab(destino) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = Rutas.NUEVA, modifier = Modifier.fillMaxSize()) {
                composable(Rutas.NUEVA) {
                    NuevaEncuestaScreen(
                        vm = vm,
                        onAbrirGrabacion = { nav.navigate(Rutas.GRABACION) },
                        onEnviada = { nav.abrirTab(Rutas.HISTORIAL) },
                    )
                }
                composable(Rutas.HISTORIAL) {
                    HistorialScreen(vm = vm, onNueva = { nav.abrirTab(Rutas.NUEVA) })
                }
                composable(Rutas.ESTADISTICAS) {
                    EstadisticasScreen(vm)
                }
                composable(Rutas.GRABACION) {
                    GrabacionScreen(vm = vm, onVolver = { nav.popBackStack() })
                }
            }
            if (aviso != null) {
                PastillaAviso(
                    texto = aviso.texto,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .then(if (enGrabacion) Modifier.navigationBarsPadding() else Modifier)
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

private fun NavHostController.abrirTab(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun Encabezado(subtitulo: String, onPerfil: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp)
            .background(Vz.Surface.copy(alpha = 0.96f))
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            MarcaVoz()
            Column(Modifier.padding(start = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Tx("VozComunidad", size = 18.sp, peso = FontWeight.SemiBold, alto = 22.sp, tracking = (-0.3).sp)
                    Box(
                        Modifier
                            .padding(start = 6.dp)
                            .background(Vz.SurfaceContainerHigh, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Tx("Vecinal", color = Vz.Primary, size = 11.sp, peso = FontWeight.Bold, alto = 14.sp)
                    }
                }
                Tx(subtitulo, color = Vz.OnSurfaceVariant, size = 11.sp, alto = 14.sp, max = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Vz.Primary, CircleShape)
                .presionSuave(onPerfil),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Person, contentDescription = "Perfil", tint = Vz.OnPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun BarraInferior(rutaActual: String, onElegir: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp)
            .background(Vz.SurfaceContainerLowest.copy(alpha = 0.96f))
            .navigationBarsPadding()
            .height(64.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemBarra("Nueva", Icons.Outlined.AddCircle, rutaActual == Rutas.NUEVA) { onElegir(Rutas.NUEVA) }
        ItemBarra("Historial", Icons.Outlined.HistoryEdu, rutaActual == Rutas.HISTORIAL) { onElegir(Rutas.HISTORIAL) }
        ItemBarra("Estadísticas", Icons.Outlined.QueryStats, rutaActual == Rutas.ESTADISTICAS) { onElegir(Rutas.ESTADISTICAS) }
    }
}

@Composable
private fun ItemBarra(etiqueta: String, icono: ImageVector, activo: Boolean, onClick: () -> Unit) {
    val color = if (activo) Vz.Primary else Vz.OnSurfaceVariant
    Column(
        modifier = Modifier.presionSuave(onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icono, contentDescription = etiqueta, tint = color, modifier = Modifier.size(24.dp))
        Tx(etiqueta, color = color, size = 11.sp, peso = if (activo) FontWeight.Bold else FontWeight.SemiBold, alto = 14.sp)
    }
}
