package com.vozcomunidad.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vozcomunidad.app.data.inicialesDe
import com.vozcomunidad.app.ui.theme.Jakarta
import com.vozcomunidad.app.ui.theme.Vz

@Composable
fun Tx(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = Vz.OnSurface,
    size: TextUnit = 15.sp,
    peso: FontWeight = FontWeight.Normal,
    alto: TextUnit = TextUnit.Unspecified,
    max: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    align: TextAlign? = null,
    cursiva: Boolean = false,
    familia: FontFamily = Jakarta,
    tracking: TextUnit = TextUnit.Unspecified,
) {
    Text(
        text = texto,
        modifier = modifier,
        maxLines = max,
        overflow = overflow,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontWeight = peso,
            fontFamily = familia,
            fontStyle = if (cursiva) FontStyle.Italic else FontStyle.Normal,
            lineHeight = alto,
            textAlign = align ?: TextAlign.Unspecified,
            letterSpacing = tracking,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
    )
}

fun Modifier.tarjeta(radio: Dp = 16.dp, color: Color = Vz.SurfaceContainerLowest): Modifier =
    this
        .shadow(2.dp, RoundedCornerShape(radio), ambientColor = Color(0x140F172A), spotColor = Color(0x140F172A))
        .clip(RoundedCornerShape(radio))
        .background(color)

@Composable
fun Modifier.presionSuave(onClick: () -> Unit): Modifier {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val indicacion = LocalIndication.current
    return this
        .scale(if (presionado) 0.98f else 1f)
        .clickable(interactionSource = interaccion, indication = indicacion, onClick = onClick)
}

@Composable
fun MarcaVoz(tamano: Dp = 32.dp) {
    Box(Modifier.size(tamano)) {
        Box(
            Modifier
                .size(tamano * 0.72f)
                .align(Alignment.CenterStart)
                .background(Vz.PrimaryContainer, CircleShape),
        )
        Box(
            Modifier
                .size(tamano * 0.48f)
                .align(Alignment.TopEnd)
                .background(Vz.Tertiary, CircleShape),
        )
        Box(
            Modifier
                .size(tamano * 0.34f)
                .align(Alignment.BottomEnd)
                .background(Vz.SecondaryContainer, CircleShape),
        )
    }
}

@Composable
fun Avatar(
    nombre: String,
    tamano: Dp,
    colorFondo: Color = Vz.Primary,
) {
    Box(
        modifier = Modifier
            .size(tamano)
            .clip(CircleShape)
            .background(colorFondo),
        contentAlignment = Alignment.Center,
    ) {
        Tx(
            texto = inicialesDe(nombre),
            color = Vz.OnPrimary,
            size = (tamano.value * 0.34f).sp,
            peso = FontWeight.Bold,
        )
    }
}

@Composable
fun OndaAudio(
    activa: Boolean,
    modifier: Modifier = Modifier,
    barras: Int = 16,
    alto: Dp = 36.dp,
    colores: List<Color> = listOf(
        Vz.Tertiary,
        Vz.TertiaryContainer,
        Vz.SecondaryContainer,
        Vz.Primary,
        Vz.Secondary,
        Vz.TertiaryFixedDim,
    ),
) {
    val transicion = rememberInfiniteTransition(label = "onda")
    Row(
        modifier = modifier.height(alto),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Bottom,
    ) {
        repeat(barras) { indice ->
            val fraccion by transicion.animateFloat(
                initialValue = 0.28f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 420 + (indice % 6) * 70, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(indice * 35),
                ),
                label = "barra$indice",
            )
            val altura = if (activa) fraccion else 0.32f
            Box(
                Modifier
                    .width(4.dp)
                    .height(alto * altura.coerceIn(0.18f, 1f))
                    .clip(CircleShape)
                    .background(colores[indice % colores.size].copy(alpha = if (activa) 1f else 0.4f)),
            )
        }
    }
}

@Composable
fun OndaEstatica(color: Color, modifier: Modifier = Modifier) {
    val alturas = listOf(0.45f, 1f, 0.35f, 0.8f, 0.5f, 0.72f, 0.28f, 0.55f, 0.9f, 0.32f)
    Row(
        modifier = modifier.height(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        alturas.forEachIndexed { indice, altura ->
            Box(
                Modifier
                    .width(3.dp)
                    .height(14.dp * altura)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (indice < 3) 1f else 0.35f)),
            )
        }
    }
}

@Composable
fun AnillosMic(activo: Boolean, modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "anillos")
    val escala by transicion.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "escala",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        if (activo) {
            Box(
                Modifier
                    .size(112.dp * escala)
                    .background(Vz.SecondaryContainer.copy(alpha = 0.28f), CircleShape),
            )
            Box(
                Modifier
                    .size(96.dp)
                    .background(Vz.TertiaryFixedDim.copy(alpha = 0.35f), CircleShape),
            )
        }
    }
}

@Composable
fun CampoLinea(
    valor: String,
    alCambiar: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    teclado: KeyboardType = KeyboardType.Text,
    elevado: Boolean = false,
    iconoInicio: @Composable (() -> Unit)? = null,
    iconoFinal: @Composable (() -> Unit)? = null,
) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    val fondo = when {
        enfocado -> Vz.SurfaceContainerLowest
        elevado -> Vz.SurfaceContainerLowest
        else -> Vz.SurfaceContainerLow
    }
    BasicTextField(
        value = valor,
        onValueChange = alCambiar,
        singleLine = true,
        interactionSource = interaccion,
        cursorBrush = SolidColor(Vz.Primary),
        textStyle = TextStyle(
            color = Vz.OnSurface,
            fontSize = 15.sp,
            fontFamily = Jakarta,
            fontWeight = FontWeight.Normal,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(if (elevado) Modifier.shadow(2.dp, RoundedCornerShape(12.dp)) else Modifier)
            .background(fondo, RoundedCornerShape(12.dp))
            .border(
                width = if (enfocado) 2.dp else 0.dp,
                color = if (enfocado) Vz.Primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            ),
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (iconoInicio != null) {
                    iconoInicio()
                    Spacer(Modifier.width(8.dp))
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (valor.isEmpty()) {
                        Tx(placeholder, color = Vz.OutlineVariant, size = 15.sp)
                    }
                    inner()
                }
                if (iconoFinal != null) {
                    Spacer(Modifier.width(8.dp))
                    iconoFinal()
                }
            }
        },
    )
}

@Composable
fun CampoLargo(
    valor: String,
    alCambiar: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val interaccion = remember { MutableInteractionSource() }
    val enfocado by interaccion.collectIsFocusedAsState()
    Box(modifier.fillMaxWidth()) {
        BasicTextField(
            value = valor,
            onValueChange = { if (it.length <= 500) alCambiar(it) },
            interactionSource = interaccion,
            cursorBrush = SolidColor(Vz.Primary),
            textStyle = TextStyle(
                color = Vz.OnSurface,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontFamily = Jakarta,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .background(
                    if (enfocado) Vz.SurfaceContainerLowest else Vz.SurfaceContainerLow,
                    RoundedCornerShape(12.dp),
                )
                .border(
                    width = if (enfocado) 2.dp else 0.dp,
                    color = if (enfocado) Vz.Primary else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        )
        Tx(
            texto = "${valor.length} / 500 car.",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 8.dp),
            color = Vz.Outline,
            size = 11.sp,
            peso = FontWeight.Bold,
            alto = 14.sp,
        )
    }
}

@Composable
fun IconoCircular(
    icono: ImageVector,
    descripcion: String,
    fondo: Color,
    tinta: Color,
    tamano: Dp = 32.dp,
) {
    Box(
        modifier = Modifier
            .size(tamano)
            .background(fondo, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icono,
            contentDescription = descripcion,
            tint = tinta,
            modifier = Modifier.size(tamano * 0.55f),
        )
    }
}

@Composable
fun BotonPrimario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconoInicio: ImageVector? = null,
    iconoFin: ImageVector? = null,
    colorIconoInicio: Color = Vz.TertiaryFixed,
    colorIconoFin: Color = Vz.SecondaryFixed,
    fondo: Color = Vz.Primary,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp), ambientColor = Vz.Primary.copy(alpha = 0.25f), spotColor = Vz.Primary.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(16.dp))
            .background(fondo)
            .presionSuave(onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconoInicio != null) {
            Icon(iconoInicio, contentDescription = null, tint = colorIconoInicio, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Tx(texto, color = Vz.OnPrimary, size = 15.sp, peso = FontWeight.Bold, alto = 20.sp)
        if (iconoFin != null) {
            Spacer(Modifier.width(8.dp))
            Icon(iconoFin, contentDescription = null, tint = colorIconoFin, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PastillaAviso(texto: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(8.dp, CircleShape)
            .background(Vz.InverseSurface, CircleShape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(Vz.TertiaryFixed, CircleShape),
        )
        Tx(texto, color = Vz.InverseOnSurface, size = 13.sp, peso = FontWeight.SemiBold, alto = 16.sp)
    }
}
