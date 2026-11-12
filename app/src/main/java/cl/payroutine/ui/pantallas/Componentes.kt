package cl.payroutine.ui.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import cl.payroutine.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.payroutine.ui.theme.*

@Composable
internal fun Etiqueta(texto: String, color: Color = TextoSecundario, modifier: Modifier = Modifier) {
    Text(texto, modifier, color = color, fontSize = 12.sp, lineHeight = 16.sp)
}

@Composable
internal fun Destacado(texto: String, color: Color = Texto, modifier: Modifier = Modifier) {
    Text(texto, modifier, color = color, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
internal fun Cifra(texto: String, color: Color = Texto, tamano: Int = 28) {
    Text(texto, color = color, fontSize = tamano.sp, lineHeight = (tamano + 5).sp, fontWeight = FontWeight.Bold)
}

@Composable
internal fun Cabecera(
    titulo: String,
    volver: (() -> Unit)? = null,
    accion: String? = null,
    alAccionar: (() -> Unit)? = null,
) {
    Surface(color = Color.White) {
        Column {
            Row(
                Modifier.fillMaxWidth().heightIn(min = if (volver == null) 44.dp else 56.dp).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (volver != null) BotonIcono("back", "Volver", volver, Fondo)
                Text(titulo, Modifier.weight(1f), color = Texto, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
                if (accion != null && alAccionar != null) Enlace(accion, alAccionar)
            }
            HorizontalDivider(color = Borde)
        }
    }
}

@Composable
internal fun Cuerpo(
    modifier: Modifier = Modifier,
    espacio: Int = 12,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(espacio.dp),
        content = content,
    )
}

@Composable
internal fun Tarjeta(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    espacio: Int = 4,
    relleno: Int = 16,
    alPulsar: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val forma = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier.clip(forma).then(if (alPulsar == null) Modifier else Modifier.clickable(onClick = alPulsar)),
        shape = forma,
        color = color,
        border = if (color == Color.White) BorderStroke(1.dp, Borde) else null,
    ) {
        Column(Modifier.padding(relleno.dp), verticalArrangement = Arrangement.spacedBy(espacio.dp), content = content)
    }
}

@Composable
internal fun Icono(nombre: String, color: Color, modifier: Modifier = Modifier, descripcion: String? = null) {
    val recurso = when (nombre) {
        "home" -> R.drawable.icono_home
        "list" -> R.drawable.icono_list
        "calendar" -> R.drawable.icono_cal
        "bell" -> R.drawable.icono_bell
        "back" -> R.drawable.icono_back
        "next" -> R.drawable.icono_chev
        "check" -> R.drawable.icono_check
        "plus" -> R.drawable.icono_plus
        "cart" -> R.drawable.icono_cart
        "play" -> R.drawable.icono_play
        "bolt" -> R.drawable.icono_bolt
        "bus" -> R.drawable.icono_bus
        "card" -> R.drawable.icono_card
        "wifi" -> R.drawable.icono_wifi
        "house" -> R.drawable.icono_house
        "down" -> R.drawable.icono_down
        "up" -> R.drawable.icono_up
        "pie" -> R.drawable.icono_pie
        "chart" -> R.drawable.icono_chart
        else -> error("Icono de PayRoutine desconocido: $nombre")
    }
    Icon(painterResource(recurso), contentDescription = descripcion, modifier = modifier.size(20.dp), tint = color)
}

@Composable
internal fun Insignia(nombre: String, color: Color, fondo: Color, tamano: Int = 40) {
    Box(
        Modifier.size(tamano.dp).background(fondo, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) { Icono(nombre, color) }
}

@Composable
internal fun BotonIcono(
    icono: String,
    descripcion: String,
    alPulsar: (() -> Unit)? = null,
    fondo: Color = Color.White,
    circular: Boolean = false,
) {
    val forma = if (circular) CircleShape else RoundedCornerShape(12.dp)
    Box(
        Modifier.size(if (circular) 40.dp else 36.dp).clip(forma).background(fondo)
            .then(if (alPulsar == null) Modifier else Modifier.clickable(onClick = alPulsar)),
        contentAlignment = Alignment.Center,
    ) { Icono(icono, Texto, descripcion = descripcion) }
}

@Composable
internal fun Enlace(texto: String, alPulsar: () -> Unit) {
    Text(texto, Modifier.clickable(onClick = alPulsar).padding(vertical = 8.dp),
        color = VerdeMarca, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
internal fun BotonPrincipal(texto: String, modifier: Modifier = Modifier, alPulsar: (() -> Unit)? = null) {
    val forma = RoundedCornerShape(14.dp)
    if (alPulsar != null) {
        Button(alPulsar, modifier.fillMaxWidth().height(46.dp), shape = forma) {
            Destacado(texto, Color.White)
        }
    } else {
        Surface(modifier.fillMaxWidth().height(46.dp), shape = forma, color = VerdeMarca) {
            Box(contentAlignment = Alignment.Center) { Destacado(texto, Color.White) }
        }
    }
}

@Composable
internal fun Pildora(texto: String, activa: Boolean = false) {
    Surface(
        shape = CircleShape, color = if (activa) AzulNoche else Color.White,
        border = if (activa) null else BorderStroke(1.dp, Borde),
    ) {
        Text(texto, Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (activa) Color.White else Texto, fontSize = 13.sp, lineHeight = 16.sp)
    }
}

@Composable
internal fun Segmentos(opciones: List<String>, activa: Int, seleccionar: ((Int) -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().background(FondoSelector, RoundedCornerShape(12.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        opciones.forEachIndexed { indice, opcion ->
            val forma = RoundedCornerShape(9.dp)
            Box(
                Modifier.weight(1f).heightIn(min = 37.dp).clip(forma)
                    .background(if (indice == activa) Color.White else Color.Transparent)
                    .then(if (seleccionar == null) Modifier else Modifier.clickable { seleccionar(indice) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(opcion, fontSize = 14.sp,
                    color = if (indice == activa) AzulNoche else TextoSecundario,
                    fontWeight = if (indice == activa) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
internal fun Progreso(porcentaje: Int, color: Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Borde)) {
        Box(Modifier.fillMaxWidth(porcentaje.coerceIn(0, 100) / 100f).fillMaxHeight().background(color, CircleShape))
    }
}

@Composable
internal fun FilaTarjeta(
    nombre: String, detalle: String, monto: String, icono: String,
    colorIcono: Color, fondoIcono: Color, colorMonto: Color = Texto,
    alPulsar: (() -> Unit)? = null,
) {
    Tarjeta(Modifier.fillMaxWidth(), relleno = 13, alPulsar = alPulsar) {
        FilaDato(nombre, detalle, monto, icono, colorIcono, fondoIcono, colorMonto)
    }
}

@Composable
internal fun FilaDato(
    nombre: String, detalle: String, monto: String, icono: String,
    colorIcono: Color, fondoIcono: Color, colorMonto: Color = Texto,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Insignia(icono, colorIcono, fondoIcono)
        Column(Modifier.weight(1f)) {
            Destacado(nombre)
            if (detalle.isNotEmpty()) Etiqueta(detalle)
        }
        Destacado(monto, colorMonto)
    }
}

@Composable
internal fun CampoDeEjemplo(etiqueta: String, valor: String, icono: String? = null, color: Color = Texto) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Etiqueta(etiqueta)
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Borde)) {
            Row(
                Modifier.heightIn(min = 46.dp).padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icono != null) Icono(icono, TextoSecundario)
                Text(valor, color = color, fontSize = 15.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
internal fun PieFormulario(texto: String) {
    BotonPrincipal(texto, Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
}
