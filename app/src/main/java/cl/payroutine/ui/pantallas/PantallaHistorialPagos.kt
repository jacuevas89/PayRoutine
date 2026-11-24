package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.FondoIngreso
import cl.payroutine.ui.theme.VerdeIngreso
import cl.payroutine.viewmodel.EstadoPagos
import cl.payroutine.viewmodel.FechaLocal
import java.time.YearMonth

@Composable
internal fun PantallaHistorialPagos(
    estado: EstadoPagos,
    fecha: FechaLocal,
    reintentar: () -> Unit,
    verGasto: (Int) -> Unit,
    volver: () -> Unit,
) {
    var mesElegido by rememberSaveable { mutableStateOf<String?>(null) }
    var mostrarSelector by rememberSaveable { mutableStateOf(false) }
    val mes = mesElegido?.let(YearMonth::parse) ?: YearMonth.from(fecha.hoy)
    val realizados = remember(estado, fecha.zona) { estado.realizados(fecha.zona) }
    val pagosDelMes = remember(realizados, mes) { realizados.filter { YearMonth.from(it.fechaPago) == mes } }
    val total = remember(pagosDelMes) { pagosDelMes.sumOf { it.fila.pago.monto.toBigDecimal() } }

    Column(Modifier.fillMaxSize()) {
        Cabecera("Historial de pagos", volver)
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Segmentos(listOf("Pendientes", "Pagados"), 1) { if (it == 0) volver() } }
            item {
                SelectorMes(
                    mes = mesEnPantalla(mes.atDay(1)),
                    anterior = { mesElegido = mes.minusMonths(1).toString() },
                    siguiente = { mesElegido = mes.plusMonths(1).toString() },
                    elegir = { mostrarSelector = true },
                )
            }
            when {
                estado.cargando -> item { Etiqueta("Cargando historial…") }
                estado.errorCarga -> item {
                    Etiqueta("No se pudo cargar el historial.")
                    BotonPrincipal("Reintentar", alPulsar = reintentar)
                }
                else -> {
                    item {
                        Tarjeta(Modifier.fillMaxWidth()) {
                            Etiqueta("Total pagado")
                            Cifra(montoEnPantalla(total), VerdeIngreso, 24)
                            val cantidad = pagosDelMes.size
                            Etiqueta(if (cantidad == 1) "1 pago realizado" else "$cantidad pagos realizados")
                        }
                    }
                    if (pagosDelMes.isEmpty()) item { Etiqueta("No hay pagos registrados en este mes.") }
                    items(pagosDelMes, key = { it.movimientoId }) { realizado ->
                        val pago = realizado.fila.pago
                        FilaTarjeta(
                            nombre = pago.descripcion,
                            detalle = "Pagado el ${fechaEnPantalla(realizado.fechaPago)}",
                            monto = montoEnPantalla(pago.monto),
                            icono = "check", colorIcono = VerdeIngreso, fondoIcono = FondoIngreso,
                        ) { verGasto(realizado.movimientoId) }
                    }
                }
            }
        }
    }

    if (mostrarSelector) {
        SelectorPeriodo(mes, cerrar = { mostrarSelector = false }) {
            mesElegido = it.toString()
            mostrarSelector = false
        }
    }
}
