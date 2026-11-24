package cl.payroutine.ui.pantallas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.payroutine.datos.local.dao.PagoConCategoria
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.ui.theme.*
import cl.payroutine.viewmodel.EstadoOperacionPago
import cl.payroutine.viewmodel.EstadoPagos
import cl.payroutine.viewmodel.FechaLocal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PantallaPagos(
    estado: EstadoPagos,
    fecha: FechaLocal,
    reintentar: () -> Unit,
    verCalendario: () -> Unit,
    verHistorial: () -> Unit,
    verPago: (Int) -> Unit,
    operacion: EstadoOperacionPago = EstadoOperacionPago(),
    pagar: (List<Int>, LocalDate, String) -> Unit = { _, _, _ -> },
    limpiarOperacion: () -> Unit = {},
) {
    val finDeMes = YearMonth.from(fecha.hoy).atEndOfMonth()
    var seleccionando by rememberSaveable { mutableStateOf(false) }
    var elegidos by rememberSaveable { mutableStateOf(intArrayOf()) }
    var porConfirmar by rememberSaveable { mutableStateOf(intArrayOf()) }
    val seleccion = estado.pagos.filter { it.pago.id in elegidos }
    fun cancelarSeleccion() {
        seleccionando = false
        elegidos = intArrayOf()
    }
    fun confirmar(vencimientos: IntArray) {
        limpiarOperacion()
        porConfirmar = vencimientos
    }
    BackHandler(enabled = seleccionando && porConfirmar.isEmpty()) { cancelarSeleccion() }
    LaunchedEffect(operacion.completada) {
        if (operacion.completada) {
            porConfirmar = intArrayOf()
            cancelarSeleccion()
            limpiarOperacion()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Cabecera("Pagos pendientes", accion = "Calendario", alAccionar = verCalendario)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
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
                    if (seleccionando || estado.pendientes.any { it.nombreCategoria != null }) {
                        stickyHeader(key = "seleccion-pagos") {
                            Row(
                                Modifier.fillMaxWidth().background(Fondo).padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (seleccionando) {
                                    Pildora("Cancelar", rol = Role.Button, alPulsar = ::cancelarSeleccion)
                                    Pildora(
                                        "Revisar pagos (${seleccion.size})",
                                        activa = seleccion.isNotEmpty(),
                                        habilitada = seleccion.isNotEmpty(),
                                        rol = Role.Button,
                                    ) { confirmar(seleccion.map { it.pago.id }.toIntArray()) }
                                } else {
                                    Pildora("Selección múltiple", rol = Role.Button) { seleccionando = true }
                                }
                            }
                        }
                    }
                    if (estado.pendientes.isEmpty()) item { Etiqueta("No tienes pagos pendientes.") }
                    itemsIndexed(estado.pendientes, key = { _, fila -> fila.pago.id }) { indice, fila ->
                        val pago = fila.pago
                        val vencimiento = fila.diaVencimiento(fecha)
                        val mes = YearMonth.from(vencimiento)
                        val inicioDeMes = indice == 0 ||
                            mes != YearMonth.from(estado.pendientes[indice - 1].diaVencimiento(fecha))
                        val marcado = pago.id in elegidos
                        val puedePagarse = fila.nombreCategoria != null
                        val modificador = Modifier.fillMaxWidth()
                        val conSeleccion = if (!seleccionando) modificador else modificador.toggleable(
                            value = marcado, enabled = puedePagarse, role = Role.Checkbox,
                            onValueChange = { marcar ->
                                elegidos = if (marcar) elegidos + pago.id else elegidos.filterNot { it == pago.id }.toIntArray()
                            },
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (inicioDeMes) {
                                Destacado(
                                    mesEnPantalla(vencimiento), TextoSecundario,
                                    Modifier.padding(start = 2.dp).semantics { heading() },
                                )
                            }
                            Tarjeta(
                                conSeleccion,
                                color = if (seleccionando && marcado) FondoMarca else Color.White,
                                relleno = 13,
                                espacio = 10,
                                alPulsar = if (seleccionando) null else ({ verPago(pago.id) }),
                            ) {
                                FilaDato(
                                    pago.descripcion, "Vence el ${fechaEnPantalla(vencimiento)}", montoEnPantalla(pago.monto),
                                    "calendar", AzulNoche, FondoSelector,
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.weight(1f)) { EstadoVencimiento(vencimiento, fecha.hoy) }
                                    if (seleccionando) {
                                        val bordeIndicador = when {
                                            marcado -> VerdeMarca
                                            puedePagarse -> TextoSecundario
                                            else -> Borde
                                        }
                                        Box(
                                            Modifier.size(24.dp)
                                                .background(if (marcado) VerdeMarca else Color.Transparent, CircleShape)
                                                .border(1.5.dp, bordeIndicador, CircleShape),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (marcado) Icono("check", Color.White, Modifier.size(14.dp))
                                        }
                                    } else if (puedePagarse) {
                                        OutlinedButton(
                                            onClick = { confirmar(intArrayOf(pago.id)) },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, VerdeMarca),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VerdeMarca),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        ) { Text("Marcar pagado") }
                                    }
                                }
                            }
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
    if (porConfirmar.isNotEmpty()) {
        val vencimientos = estado.pagos.filter { it.pago.id in porConfirmar }
        DialogoConfirmarPagos(vencimientos, fecha, operacion, cerrar = {
            porConfirmar = intArrayOf()
            limpiarOperacion()
        }, pagar = pagar)
    }
}

@Composable
internal fun PantallaDetallePago(
    estado: EstadoPagos,
    id: Int,
    fecha: FechaLocal,
    reintentar: () -> Unit,
    operacion: EstadoOperacionPago,
    pagar: (List<Int>, LocalDate, String) -> Unit,
    deshacer: (Int, Int) -> Unit,
    limpiarOperacion: () -> Unit,
    volver: () -> Unit,
) {
    val fila = estado.pagos.firstOrNull { it.pago.id == id }
    val cuotas = estado.cuotasPorObligacion[fila?.pago?.obligacionId].orEmpty()
    var idConfirmado by rememberSaveable { mutableStateOf<Int?>(null) }
    var movimientoConfirmado by rememberSaveable { mutableStateOf<Int?>(null) }
    val vencimientoConfirmado = estado.pagos.firstOrNull { it.pago.id == idConfirmado }
    fun iniciarOperacion(vencimiento: PagoConCategoria) {
        limpiarOperacion()
        movimientoConfirmado = vencimiento.pago.movimientoId
        idConfirmado = vencimiento.pago.id
    }
    fun cerrarOperacion() {
        idConfirmado = null
        movimientoConfirmado = null
        limpiarOperacion()
    }
    LaunchedEffect(operacion.completada) {
        if (operacion.completada) cerrarOperacion()
    }
    if (vencimientoConfirmado != null) {
        val movimientoId = movimientoConfirmado
        if (movimientoId == null) {
            DialogoConfirmarPagos(listOf(vencimientoConfirmado), fecha, operacion, ::cerrarOperacion, pagar)
        } else {
            DialogoDeshacerPago(vencimientoConfirmado, operacion, ::cerrarOperacion) {
                deshacer(vencimientoConfirmado.pago.id, movimientoId)
            }
        }
    }

    if (fila != null && cuotas.size >= 2) {
        DetalleDeuda(fila, cuotas.sortedBy { it.pago.cuotaActual }, fecha, ::iniciarOperacion, volver)
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
                    AccionVencimiento(fila) { iniciarOperacion(fila) }
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
    operar: (PagoConCategoria) -> Unit,
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
                    AccionVencimiento(cuota) { operar(cuota) }
                }
            }
        }
    }
}

@Composable
private fun AccionVencimiento(fila: PagoConCategoria, operar: () -> Unit) {
    when {
        fila.pago.movimientoId != null -> TextButton(onClick = operar) { Text("Deshacer pago", color = RojoGasto) }
        fila.pago.pagado -> Unit
        fila.nombreCategoria == null -> Etiqueta("Este vencimiento necesita una categoría de gasto para poder pagarse.")
        else -> BotonPrincipal(if (fila.pago.esCuota) "Pagar cuota" else "Pagar", alPulsar = operar)
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
