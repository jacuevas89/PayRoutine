package cl.payroutine.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.*

@Composable
internal fun PantallaResumen() {
    val datos = DatosDeEjemplo
    Column {
        Cabecera("Resumen mensual")
        Cuerpo {
            SelectorMes(datos.mes)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tarjeta(Modifier.weight(1f), relleno = 14) {
                    Etiqueta("Ingresos")
                    Cifra(datos.ingresos, VerdeIngreso, 20)
                }
                Tarjeta(Modifier.weight(1f), relleno = 14) {
                    Etiqueta("Gastos")
                    Cifra(datos.gastos, RojoGasto, 20)
                }
            }
            Tarjeta(Modifier.fillMaxWidth(), AzulNoche) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Etiqueta("Saldo del mes", TextoSobreAzul)
                        Cifra(datos.saldo, Color.White)
                    }
                    Etiqueta("Ingresos − Gastos", Color.White)
                }
            }
            Tarjeta(Modifier.fillMaxWidth(), espacio = 9) {
                Destacado("Gastos por categoría")
                Row(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)) {
                    datos.gastosPorCategoria.forEach { gasto ->
                        Box(Modifier.weight(gasto.porcentaje.toFloat()).fillMaxHeight().background(gasto.color))
                    }
                }
                datos.gastosPorCategoria.forEach { gasto ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(gasto.color, CircleShape))
                        Etiqueta(gasto.categoria, Texto, Modifier.weight(1f))
                        Etiqueta(gasto.monto)
                        Destacado("${gasto.porcentaje}%")
                    }
                }
            }
        }
    }
}

@Composable
internal fun PantallaCalendario(volver: () -> Unit) {
    val dias = listOf("L", "M", "M", "J", "V", "S", "D")
    val celdas: List<Int?> = List(6) { null } + (1..30).toList() + List(6) { null }
    val pago = DatosDeEjemplo.pagos.first()
    Column {
        Cabecera("Calendario", volver)
        Cuerpo {
            SelectorMes("Noviembre 2026")
            Tarjeta(Modifier.fillMaxWidth(), relleno = 12, espacio = 4) {
                Row {
                    dias.forEach { dia ->
                        Box(Modifier.weight(1f).height(30.dp), contentAlignment = Alignment.Center) { Etiqueta(dia) }
                    }
                }
                celdas.chunked(7).forEach { semana ->
                    Row {
                        semana.forEach { dia ->
                            val elegido = dia == 2
                            Box(
                                Modifier.weight(1f).height(44.dp).background(
                                    if (elegido) AzulNoche else Color.Transparent, RoundedCornerShape(12.dp),
                                ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (dia != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Etiqueta(dia.toString(), if (elegido) Color.White else Texto)
                                        if (dia in listOf(2, 5, 10)) {
                                            Box(Modifier.size(5.dp).background(if (elegido) Color.White else NaranjaAlerta, CircleShape))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Destacado("Vence el 2 de noviembre")
            FilaTarjeta(pago.nombre, "Vence en 4 días", pago.monto, pago.icono, pago.color, pago.fondo)
        }
    }
}

@Composable
private fun SelectorMes(mes: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        BotonIcono("back", "Mes anterior")
        Cifra(mes, tamano = 17)
        BotonIcono("next", "Mes siguiente")
    }
}
