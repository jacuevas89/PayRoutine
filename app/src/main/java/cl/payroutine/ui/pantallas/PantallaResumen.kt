package cl.payroutine.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.*
import cl.payroutine.viewmodel.DatosInicio
import cl.payroutine.viewmodel.GastoPorCategoria
import cl.payroutine.viewmodel.calcularResumenMensual
import java.time.Month
import java.time.YearMonth

@Composable
internal fun PantallaResumen(datos: DatosInicio?) {
    var periodoElegido by rememberSaveable { mutableStateOf<String?>(null) }
    var mostrarSelector by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Cabecera("Resumen mensual")
        Cuerpo(Modifier.weight(1f)) {
            if (datos == null) {
                Etiqueta("Cargando resumen…")
                return@Cuerpo
            }
            val periodo = periodoElegido?.let { YearMonth.parse(it) } ?: YearMonth.from(datos.fecha.hoy)
            val resumen = remember(datos.movimientos, datos.fecha.zona, periodo) {
                calcularResumenMensual(datos.movimientos, periodo, datos.fecha.zona)
            }
            SelectorMes(
                mes = mesEnPantalla(periodo.atDay(1)),
                anterior = { periodoElegido = periodo.minusMonths(1).toString() },
                siguiente = { periodoElegido = periodo.plusMonths(1).toString() },
                elegir = { mostrarSelector = true },
            )
            if (mostrarSelector) {
                SelectorPeriodo(
                    periodo = periodo,
                    cerrar = { mostrarSelector = false },
                ) {
                    periodoElegido = it.toString()
                    mostrarSelector = false
                }
            }
            if (!resumen.tieneMovimientos) Etiqueta("No hay movimientos registrados en este período.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tarjeta(Modifier.weight(1f), relleno = 14) {
                    Etiqueta("Ingresos")
                    Cifra(montoEnPantalla(resumen.importes.ingresos), VerdeIngreso, 20)
                }
                Tarjeta(Modifier.weight(1f), relleno = 14) {
                    Etiqueta("Gastos")
                    Cifra(montoEnPantalla(resumen.importes.gastos), RojoGasto, 20)
                }
            }
            Tarjeta(Modifier.fillMaxWidth(), AzulNoche) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Etiqueta("Saldo del mes", TextoSobreAzul)
                        Cifra(montoEnPantalla(resumen.importes.saldo), Color.White)
                    }
                    Etiqueta("Ingresos − Gastos", Color.White)
                }
            }
            DistribucionGastos(resumen.distribucion)
        }
    }
}

private val coloresDistribucion = listOf(AzulNoche, VerdeMarca, RojoGasto, NaranjaAlerta, TextoSecundario)

@Composable
private fun DistribucionGastos(gastos: List<GastoPorCategoria>) {
    Tarjeta(Modifier.fillMaxWidth(), espacio = 9) {
        Destacado("Gastos por categoría")
        if (gastos.isEmpty()) {
            Etiqueta("No hay gastos registrados en este período.")
        } else {
            Row(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)) {
                gastos.forEachIndexed { indice, gasto ->
                    val peso = gasto.proporcion.toFloat()
                    if (peso > 0f) {
                        val color = coloresDistribucion[indice % coloresDistribucion.size]
                        Box(Modifier.weight(peso).fillMaxHeight().background(color))
                    }
                }
            }
            gastos.forEachIndexed { indice, gasto ->
                val color = coloresDistribucion[indice % coloresDistribucion.size]
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(color, CircleShape))
                    Etiqueta(gasto.categoria, Texto, Modifier.weight(1f))
                    Etiqueta(montoEnPantalla(gasto.monto))
                    Destacado(porcentajeEnPantalla(gasto.proporcion))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SelectorPeriodo(periodo: YearMonth, cerrar: () -> Unit, elegir: (YearMonth) -> Unit) {
    var anio by rememberSaveable { mutableIntStateOf(periodo.year) }
    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Elegir mes y año") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    BotonIcono("back", "Año anterior", { anio-- })
                    Cifra(anio.toString(), tamano = 17)
                    BotonIcono("next", "Año siguiente", { anio++ })
                }
                FlowRow(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Month.values().forEach { mes ->
                        Pildora(nombreMesEnPantalla(mes), activa = anio == periodo.year && mes == periodo.month) {
                            elegir(YearMonth.of(anio, mes))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = cerrar) { Text("Cancelar") } },
    )
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
private fun SelectorMes(
    mes: String,
    anterior: (() -> Unit)? = null,
    siguiente: (() -> Unit)? = null,
    elegir: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        BotonIcono("back", "Mes anterior", anterior)
        if (elegir == null) {
            Cifra(mes, tamano = 17)
        } else {
            TextButton(onClick = elegir, modifier = Modifier.semantics { contentDescription = "Elegir mes y año" }) {
                Cifra(mes, tamano = 17)
            }
        }
        BotonIcono("next", "Mes siguiente", siguiente)
    }
}
