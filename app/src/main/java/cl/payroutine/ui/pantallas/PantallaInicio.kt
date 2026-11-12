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
    verMovimientos: () -> Unit,
    verPagos: () -> Unit,
    verRecordatorios: () -> Unit,
    verMovimiento: (Int) -> Unit,
) {
    val datos = DatosDeEjemplo
    val recientes = listOf(0, 1, 3)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().background(AzulNoche).padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(datos.saludo, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Etiqueta(datos.mes, TextoSobreAzul)
                }
                BotonIcono("bell", "Recordatorios", verRecordatorios, circular = true)
            }
            Tarjeta(Modifier.fillMaxWidth(), espacio = 10) {
                Etiqueta("Saldo del mes")
                Cifra(datos.saldo, tamano = 34)
                HorizontalDivider(color = Borde)
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icono("down", VerdeIngreso, Modifier.size(14.dp))
                            Etiqueta("Ingresos")
                        }
                        Cifra(datos.ingresos, tamano = 17)
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icono("up", RojoGasto, Modifier.size(14.dp))
                            Etiqueta("Gastos")
                        }
                        Cifra(datos.gastos, tamano = 17)
                    }
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Tarjeta(Modifier.fillMaxWidth(), color = FondoAlerta, relleno = 14, alPulsar = verPagos) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icono("calendar", NaranjaAlerta)
                    Column(Modifier.weight(1f)) {
                        Destacado("Próximo pago: Tarjeta de crédito")
                        Etiqueta("Vence en 4 días · " + datos.pagos.first().monto)
                    }
                    Icono("next", TextoSecundario)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Últimos movimientos", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Enlace("Ver todos", verMovimientos)
            }
            recientes.forEach { indice -> TarjetaMovimiento(datos.movimientos[indice]) { verMovimiento(indice) } }
        }
    }
}
