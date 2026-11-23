package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.payroutine.datos.local.dao.PagoConCategoria
import cl.payroutine.ui.theme.*
import cl.payroutine.viewmodel.EstadoPagosPendientes
import cl.payroutine.viewmodel.FechaLocal
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun PantallaPagos(
    estado: EstadoPagosPendientes,
    fecha: FechaLocal,
    reintentar: () -> Unit,
    verCalendario: () -> Unit,
    verHistorial: () -> Unit,
    verPago: (Int) -> Unit,
) {
    val finDeMes = YearMonth.from(fecha.hoy).atEndOfMonth()
    Column(Modifier.fillMaxSize()) {
        Cabecera("Pagos pendientes", accion = "Calendario", alAccionar = verCalendario)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val pendientes = if (estado.cargando || estado.errorCarga) "Pendientes" else "Pendientes (${estado.pagos.size})"
                Segmentos(listOf(pendientes, "Pagados"), 0) { if (it == 1) verHistorial() }
            }
            when {
                estado.cargando -> item { Etiqueta("Cargando pagos…") }
                estado.errorCarga -> item {
                    Etiqueta("No se pudieron cargar los pagos.")
                    BotonPrincipal("Reintentar", alPulsar = reintentar)
                }
                else -> {
                    item {
                        val total = estado.pagos.asSequence()
                            .filter { it.diaVencimiento(fecha) <= finDeMes }
                            .sumOf { it.pago.monto.toBigDecimal() }
                        Tarjeta(Modifier.fillMaxWidth(), color = AzulNoche) {
                            Etiqueta("Por pagar hasta fin de mes", TextoSobreAzul)
                            Cifra(montoEnPantalla(total), Color.White)
                        }
                    }
                    if (estado.pagos.isEmpty()) item { Etiqueta("No tienes pagos pendientes.") }
                    items(estado.pagos, key = { it.pago.id }) { fila ->
                        val pago = fila.pago
                        val vencimiento = fila.diaVencimiento(fecha)
                        Tarjeta(Modifier.fillMaxWidth(), relleno = 13, espacio = 10, alPulsar = { verPago(pago.id) }) {
                            FilaDato(
                                pago.descripcion, "Vence el ${fechaEnPantalla(vencimiento)}", montoEnPantalla(pago.monto),
                                "calendar", AzulNoche, FondoSelector,
                            )
                            EstadoVencimiento(vencimiento, fecha.hoy)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PantallaDetallePago(
    estado: EstadoPagosPendientes,
    id: Int,
    fecha: FechaLocal,
    reintentar: () -> Unit,
    volver: () -> Unit,
) {
    val fila = estado.pagos.firstOrNull { it.pago.id == id }
    Column(Modifier.fillMaxSize()) {
        Cabecera("Detalle del pago", volver)
        Cuerpo(Modifier.weight(1f), espacio = 14) {
            when {
                estado.cargando -> Etiqueta("Cargando pago…")
                estado.errorCarga -> {
                    Etiqueta("No se pudo cargar el pago.")
                    BotonPrincipal("Reintentar", alPulsar = reintentar)
                }
                fila == null -> Etiqueta("El pago ya no está disponible.")
                else -> {
                    val pago = fila.pago
                    val vencimiento = fila.diaVencimiento(fecha)
                    Tarjeta(Modifier.fillMaxWidth(), AzulNoche) {
                        Etiqueta(if (pago.esCuota) "Cuota" else "Pago único", TextoSobreAzul)
                        Destacado(pago.descripcion, Color.White)
                        Cifra(montoEnPantalla(pago.monto), Color.White)
                    }
                    Tarjeta(Modifier.fillMaxWidth()) {
                        Etiqueta("Fecha de vencimiento")
                        Destacado(fechaEnPantalla(vencimiento))
                        EstadoVencimiento(vencimiento, fecha.hoy)
                        Etiqueta("Categoría de gasto")
                        Destacado(fila.nombreCategoria ?: "Sin categoría")
                    }
                }
            }
        }
    }
}

@Composable
internal fun PantallaHistorialPagos(volver: () -> Unit) {
    val pagos = DatosDeEjemplo.pagos
    val historial = listOf(
        Triple(pagos[0].nombre, "Pagado 02 oct", pagos[0].monto),
        Triple("Cuota notebook (3 de 12)", "Pagado 05 oct", pagos[1].monto),
        Triple(pagos[2].nombre, "Pagado 10 oct", pagos[2].monto),
        Triple("Cuenta de luz", "Pagado 15 oct", "$45.200"),
    )
    Column {
        Cabecera("Historial de pagos", volver)
        Cuerpo {
            Segmentos(listOf("Pendientes", "Pagados"), 1) { if (it == 0) volver() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pildora("Octubre", activa = true)
                Pildora("Septiembre")
                Pildora("Agosto")
            }
            Tarjeta(Modifier.fillMaxWidth()) {
                Etiqueta("Pagado en octubre")
                Cifra("$255.190", VerdeIngreso, 24)
                Etiqueta("4 pagos realizados")
            }
            historial.forEach { (nombre, fecha, monto) ->
                FilaTarjeta(nombre, fecha, monto, "check", VerdeIngreso, FondoIngreso)
            }
        }
    }
}

private fun PagoConCategoria.diaVencimiento(fecha: FechaLocal): LocalDate =
    pago.fechaVencimiento.toInstant().atZone(fecha.zona).toLocalDate()

@Composable
private fun EstadoVencimiento(vencimiento: LocalDate, hoy: LocalDate) {
    val (texto, color, fondo) = when {
        vencimiento < hoy -> Triple("Vencido", RojoGasto, FondoGasto)
        vencimiento == hoy -> Triple("Vence hoy", NaranjaAlerta, FondoAlerta)
        vencimiento == hoy.plusDays(1) -> Triple("Vence mañana", NaranjaAlerta, FondoAlerta)
        else -> Triple("Pendiente", AzulNoche, FondoSelector)
    }
    Surface(color = fondo, shape = RoundedCornerShape(12.dp)) {
        Etiqueta(texto, color, Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
    }
}
