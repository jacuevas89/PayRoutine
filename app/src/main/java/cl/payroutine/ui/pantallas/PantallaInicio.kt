package cl.payroutine.ui.pantallas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.payroutine.R
import cl.payroutine.ui.theme.*
import cl.payroutine.viewmodel.DatosInicio
import cl.payroutine.viewmodel.PagoEnInicio

@Composable
internal fun PantallaBienvenida(comenzar: () -> Unit) {
    val beneficios = listOf(
        "Registra tus ingresos y gastos",
        "Define tu presupuesto mensual",
        "Controla tus pagos pendientes",
    )
    BoxWithConstraints(Modifier.fillMaxSize().background(AzulNoche)) {
        val alto = maxHeight
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .heightIn(min = alto).padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(67.dp))
            Image(painterResource(R.drawable.logo_payroutine), "Monograma PR", Modifier.size(119.dp))
            Spacer(Modifier.height(6.dp))
            Cifra("PayRoutine", Color.White, 32)
            Text("Organiza tu dinero, simplifica tu día", color = TextoSobreAzul, fontSize = 15.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height((alto * 0.115f).coerceIn(42.dp, 90.dp)))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                beneficios.forEach { beneficio ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(22.dp).background(VerdeMarca, CircleShape), contentAlignment = Alignment.Center) {
                            Icono("check", Color.White, Modifier.padding(3.dp))
                        }
                        Text(beneficio, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            Spacer(Modifier.height((alto * 0.085f).coerceIn(32.dp, 67.dp)))
            BotonPrincipal("Comenzar", alPulsar = comenzar)
            Spacer(Modifier.weight(1f))
            Text("Técnico en Programación y Análisis de Sistemas · AIEP", color = TextoSobreAzul,
                fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 28.dp, bottom = 36.dp))
        }
    }
}

@Composable
internal fun PantallaInicio(
    apodo: String,
    datos: DatosInicio?,
    verMovimientos: () -> Unit,
    verPagos: () -> Unit,
    verRecordatorios: () -> Unit,
    verAjustes: () -> Unit,
    verMovimiento: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().background(AzulNoche).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Hola, $apodo", color = Color.White, fontSize = 18.5.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
                    if (datos != null) Etiqueta(mesEnPantalla(datos.fecha.hoy), TextoSobreAzul)
                }
                BotonIcono("bell", "Recordatorios", verRecordatorios, circular = true)
                BotonIcono("settings", "Ajustes", verAjustes, circular = true, color = AzulNoche)
            }
            Tarjeta(Modifier.fillMaxWidth(), espacio = 10) {
                Etiqueta("Saldo del mes")
                Cifra(datos?.let { montoEnPantalla(it.saldo) } ?: "—", tamano = 34)
                HorizontalDivider(color = Borde)
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icono("down", VerdeIngreso, Modifier.size(14.dp))
                            Etiqueta("Ingresos")
                        }
                        Cifra(datos?.let { montoEnPantalla(it.ingresos) } ?: "—", tamano = 17)
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icono("up", RojoGasto, Modifier.size(14.dp))
                            Etiqueta("Gastos")
                        }
                        Cifra(datos?.let { montoEnPantalla(it.gastos) } ?: "—", tamano = 17)
                    }
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Pagos pendientes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Enlace("Ver pagos", verPagos)
            }
            when {
                datos == null -> Etiqueta("Cargando pagos…")
                datos.pagosPendientes.isEmpty() -> Etiqueta("No tienes pagos vencidos ni por vencer este mes.")
                else -> datos.pagosPendientes.forEach { TarjetaPagoInicio(it, verPagos) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Últimos movimientos", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Enlace("Ver todos", verMovimientos)
            }
            when {
                datos == null -> Etiqueta("Cargando movimientos…")
                datos.recientes.isEmpty() -> Etiqueta("Todavía no tienes movimientos registrados.")
                else -> datos.recientes.forEach { fila ->
                    TarjetaMovimiento(fila, datos.fecha.zona) { verMovimiento(fila.movimiento.id) }
                }
            }
            Spacer(Modifier.height(56.dp))
        }
    }
}

@Composable
private fun TarjetaPagoInicio(datos: PagoEnInicio, verPagos: () -> Unit) {
    val dias = datos.diasParaVencer
    val vencido = dias < 0
    val titulo = if (vencido) "Pago vencido" else "Próximo pago"
    val plazo = when {
        vencido -> "Venció el ${fechaEnPantalla(datos.vencimiento)}"
        dias == 0L -> "Vence hoy"
        dias == 1L -> "Vence mañana"
        else -> "Vence en $dias días"
    }
    Tarjeta(
        Modifier.fillMaxWidth(),
        color = if (vencido) FondoGasto else FondoAlerta,
        relleno = 14,
        alPulsar = verPagos,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icono("calendar", if (vencido) RojoGasto else NaranjaAlerta)
            Column(Modifier.weight(1f)) {
                Destacado("$titulo: ${datos.pago.descripcion}")
                Etiqueta("$plazo · ${montoEnPantalla(datos.pago.monto)}")
            }
            Icono("next", TextoSecundario)
        }
    }
}
