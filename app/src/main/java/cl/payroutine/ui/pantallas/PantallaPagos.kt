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
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.ui.theme.*
import cl.payroutine.viewmodel.EstadoPagos
import cl.payroutine.viewmodel.FechaLocal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Composable
internal fun PantallaPagos(
    estado: EstadoPagos,
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
                val pendientes = if (estado.cargando || estado.errorCarga) "Pendientes" else "Pendientes (${estado.pendientes.size})"
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
                        val total = estado.pendientes.asSequence()
                            .filter { it.diaVencimiento(fecha) <= finDeMes }
                            .sumOf { it.pago.monto.toBigDecimal() }
                        Tarjeta(Modifier.fillMaxWidth(), color = AzulNoche) {
                            Etiqueta("Por pagar hasta fin de mes", TextoSobreAzul)
                            Cifra(montoEnPantalla(total), Color.White)
                        }
                    }
                    if (estado.pendientes.isEmpty()) item { Etiqueta("No tienes pagos pendientes.") }
                    items(estado.pendientes, key = { it.pago.id }) { fila ->
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
                    if (estado.deudasSaldadas.isNotEmpty()) item { Destacado("Deudas saldadas") }
                    items(estado.deudasSaldadas, key = { it.pago.id }) { fila ->
                        FilaTarjeta(
                            fila.nombreObligacion ?: fila.pago.descripcion,
                            "${fila.pago.totalCuotas} cuotas pagadas", "Saldada", "check", VerdeIngreso, FondoIngreso,
                        ) { verPago(fila.pago.id) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PantallaDetallePago(
    estado: EstadoPagos,
    id: Int,
    fecha: FechaLocal,
    reintentar: () -> Unit,
    volver: () -> Unit,
) {
    val fila = estado.pagos.firstOrNull { it.pago.id == id }
    val cuotas = estado.cuotasPorObligacion[fila?.pago?.obligacionId].orEmpty()
    if (fila != null && cuotas.size >= 2) {
        DetalleDeuda(fila, cuotas.sortedBy { it.pago.cuotaActual }, fecha, volver)
        return
    }
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
                        Etiqueta(if (pago.esCuota) pago.etiquetaCuota() else "Pago único", TextoSobreAzul)
                        Destacado(pago.descripcion, Color.White)
                        Cifra(montoEnPantalla(pago.monto), Color.White)
                    }
                    Tarjeta(Modifier.fillMaxWidth()) {
                        Etiqueta("Fecha de vencimiento")
                        Destacado(fechaEnPantalla(vencimiento))
                        EstadoVencimiento(vencimiento, fecha.hoy, pago.pagado)
                        Etiqueta("Categoría de gasto")
                        Destacado(fila.nombreCategoria ?: "Sin categoría")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetalleDeuda(
    fila: PagoConCategoria,
    cuotas: List<PagoConCategoria>,
    fecha: FechaLocal,
    volver: () -> Unit,
) {
    val (pagadas, pendientes) = cuotas.partition { it.pago.pagado }
    val abonado = pagadas.sumOf { it.pago.monto.toBigDecimal() }
    val saldo = pendientes.sumOf { it.pago.monto.toBigDecimal() }
    val siguiente = pendientes.firstOrNull()
    Column(Modifier.fillMaxSize()) {
        Cabecera("Detalle de la deuda", volver)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Tarjeta(Modifier.fillMaxWidth(), AzulNoche) {
                    Destacado(fila.nombreObligacion ?: fila.pago.descripcion, Color.White)
                    Cifra(montoEnPantalla(abonado + saldo), Color.White)
                    Etiqueta("${pagadas.size} de ${cuotas.size} cuotas pagadas", TextoSobreAzul)
                    Progreso(pagadas.size * 100 / cuotas.size, VerdeMarca)
                    fila.nombreCategoria?.let { Etiqueta(it, TextoSobreAzul) }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tarjeta(Modifier.weight(1f)) {
                        Etiqueta("Total abonado")
                        Cifra(montoEnPantalla(abonado), VerdeIngreso, 18)
                    }
                    Tarjeta(Modifier.weight(1f)) {
                        Etiqueta("Saldo de la deuda")
                        Cifra(montoEnPantalla(saldo), tamano = 18)
                    }
                }
            }
            item {
                if (siguiente == null) {
                    Etiqueta("Deuda saldada")
                } else {
                    Etiqueta("${pendientes.size} cuotas pendientes · Próxima: ${siguiente.pago.etiquetaCuota()}")
                }
            }
            item { Destacado("Cronograma de cuotas") }
            items(cuotas, key = { it.pago.id }) { cuota ->
                val vencimiento = cuota.diaVencimiento(fecha)
                Tarjeta(Modifier.fillMaxWidth(), relleno = 12, espacio = 8) {
                    FilaDato(
                        cuota.pago.etiquetaCuota(), "Vence el ${fechaEnPantalla(vencimiento)}", montoEnPantalla(cuota.pago.monto),
                        "calendar", AzulNoche, FondoSelector,
                    )
                    EstadoVencimiento(vencimiento, fecha.hoy, cuota.pago.pagado)
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

private fun PagoPendiente.etiquetaCuota(): String =
    if (cuotaActual != null && totalCuotas != null) "Cuota $cuotaActual de $totalCuotas" else "Cuota"

@Composable
private fun EstadoVencimiento(vencimiento: LocalDate, hoy: LocalDate, pagado: Boolean = false) {
    val dias = ChronoUnit.DAYS.between(hoy, vencimiento)
    val (texto, color, fondo) = when {
        pagado -> Triple("Pagada", VerdeIngreso, FondoIngreso)
        dias < 0 -> Triple("Vencido", RojoGasto, FondoGasto)
        dias == 0L -> Triple("Vence hoy", NaranjaAlerta, FondoAlerta)
        dias == 1L -> Triple("Vence mañana", NaranjaAlerta, FondoAlerta)
        dias <= 4 -> Triple("Vence en $dias días", NaranjaAlerta, FondoAlerta)
        dias <= 7 -> Triple("Vence en $dias días", AzulNoche, FondoSelector)
        dias <= 14 -> Triple("Vence en 2 semanas", AzulNoche, FondoSelector)
        else -> Triple("Pendiente", AzulNoche, FondoSelector)
    }
    Surface(color = fondo, shape = RoundedCornerShape(12.dp)) {
        Etiqueta(texto, color, Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
    }
}
