package cl.payroutine.ui.pantallas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.AzulNoche
import cl.payroutine.ui.theme.FondoIngreso
import cl.payroutine.ui.theme.FondoSelector
import cl.payroutine.ui.theme.NaranjaAlerta
import cl.payroutine.ui.theme.Texto
import cl.payroutine.ui.theme.VerdeIngreso
import cl.payroutine.viewmodel.EstadoPagos
import cl.payroutine.viewmodel.FechaLocal
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun PantallaCalendario(
    estado: EstadoPagos,
    fecha: FechaLocal,
    reintentar: () -> Unit,
    verPago: (Int) -> Unit,
    verGasto: (Int) -> Unit,
    volver: () -> Unit,
) {
    var diaElegido by rememberSaveable { mutableStateOf<Long?>(null) }
    var mostrarSelector by rememberSaveable { mutableStateOf(false) }
    var mostrarVencimientosPagados by rememberSaveable { mutableStateOf(false) }
    val dia = diaElegido?.let(LocalDate::ofEpochDay) ?: fecha.hoy
    val mes = YearMonth.from(dia)
    val pendientesPorDia = remember(estado, fecha.zona) { estado.pendientes.groupBy { it.diaVencimiento(fecha) } }
    val realizados = remember(estado, fecha.zona) { estado.realizados(fecha.zona) }
    val realizadosPorDia = remember(realizados) { realizados.groupBy { it.fechaPago } }
    val pendientes = pendientesPorDia[dia].orEmpty()
    val pagosDelDia = realizadosPorDia[dia].orEmpty()
    val vencimientosPagados = remember(realizados, dia, fecha.zona) {
        realizados.filter { it.fechaPago != dia && it.fila.diaVencimiento(fecha) == dia }
    }

    fun seleccionar(diaNuevo: LocalDate) {
        if (diaNuevo != dia) mostrarVencimientosPagados = false
        diaElegido = diaNuevo.toEpochDay()
    }
    fun cambiarMes(mesNuevo: YearMonth) {
        if (mesNuevo != mes) {
            seleccionar(if (mesNuevo == YearMonth.from(fecha.hoy)) fecha.hoy else mesNuevo.atDay(1))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Cabecera("Calendario", volver)
        Cuerpo(Modifier.weight(1f)) {
            SelectorMes(
                mes = mesEnPantalla(mes.atDay(1)),
                anterior = { cambiarMes(mes.minusMonths(1)) },
                siguiente = { cambiarMes(mes.plusMonths(1)) },
                elegir = { mostrarSelector = true },
            )
            CalendarioMensual(mes, dia, pendientesPorDia.keys, realizadosPorDia.keys, ::seleccionar)
            Destacado(fechaEnPantalla(dia))
            when {
                estado.cargando -> Etiqueta("Cargando pagos…")
                estado.errorCarga -> {
                    Etiqueta("No se pudieron cargar los pagos.")
                    BotonPrincipal("Reintentar", alPulsar = reintentar)
                }
                else -> {
                    if (pendientes.isNotEmpty()) Destacado("Vencimientos pendientes")
                    pendientes.forEach { fila ->
                        FilaTarjeta(
                            fila.pago.descripcion, fila.nombreCategoria ?: "Sin categoría", montoEnPantalla(fila.pago.monto),
                            "calendar", AzulNoche, FondoSelector,
                        ) { verPago(fila.pago.id) }
                    }
                    if (pagosDelDia.isNotEmpty()) Destacado("Pagos realizados este día")
                    pagosDelDia.forEach { realizado ->
                        val vencimiento = realizado.fila.diaVencimiento(fecha)
                        val detalle = if (vencimiento == dia) realizado.fila.nombreCategoria ?: "Sin categoría"
                            else "Vencía el ${fechaEnPantalla(vencimiento)}"
                        FilaTarjeta(
                            realizado.fila.pago.descripcion, detalle, montoEnPantalla(realizado.fila.pago.monto),
                            "check", VerdeIngreso, FondoIngreso,
                        ) { verGasto(realizado.movimientoId) }
                        if (vencimiento != dia && realizado.fila.nombreCategoria == null) Etiqueta("Sin categoría")
                    }
                    if (vencimientosPagados.isNotEmpty()) {
                        Pildora(
                            if (mostrarVencimientosPagados) "Ocultar vencimientos ya pagados"
                            else "Ver vencimientos ya pagados (${vencimientosPagados.size})",
                            activa = mostrarVencimientosPagados, rol = Role.Button,
                        ) { mostrarVencimientosPagados = !mostrarVencimientosPagados }
                        if (mostrarVencimientosPagados) {
                            vencimientosPagados.forEach { realizado ->
                                FilaTarjeta(
                                    realizado.fila.pago.descripcion, "Pagado el ${fechaEnPantalla(realizado.fechaPago)}",
                                    montoEnPantalla(realizado.fila.pago.monto), "check", VerdeIngreso, FondoIngreso,
                                ) { verGasto(realizado.movimientoId) }
                            }
                        }
                    }
                    if (pendientes.isEmpty() && pagosDelDia.isEmpty() && vencimientosPagados.isEmpty()) {
                        Etiqueta("Sin vencimientos ni pagos para este día.")
                    }
                }
            }
        }
    }

    if (mostrarSelector) {
        SelectorPeriodo(mes, cerrar = { mostrarSelector = false }) {
            cambiarMes(it)
            mostrarSelector = false
        }
    }
}

@Composable
private fun CalendarioMensual(
    mes: YearMonth,
    seleccionado: LocalDate,
    pendientes: Set<LocalDate>,
    realizados: Set<LocalDate>,
    elegir: (LocalDate) -> Unit,
) {
    val celdasIniciales = mes.atDay(1).dayOfWeek.value - 1
    val diasDelMes = mes.lengthOfMonth()
    val celdasFinales = (7 - (celdasIniciales + diasDelMes) % 7) % 7
    val dias: List<LocalDate?> = List(celdasIniciales) { null } +
        (1..diasDelMes).map { mes.atDay(it) } + List(celdasFinales) { null }

    Tarjeta(Modifier.fillMaxWidth(), relleno = 12, espacio = 8) {
        Row {
            listOf("L", "M", "M", "J", "V", "S", "D").forEach { inicial ->
                Box(Modifier.weight(1f).height(26.dp), contentAlignment = Alignment.Center) { Etiqueta(inicial) }
            }
        }
        Column(Modifier.selectableGroup()) {
            dias.chunked(7).forEach { semana ->
                Row {
                    semana.forEach { dia ->
                        val modifier = Modifier.weight(1f).height(44.dp)
                        if (dia == null) {
                            Spacer(modifier)
                        } else {
                            DiaCalendario(dia, dia == seleccionado, dia in pendientes, dia in realizados, modifier) {
                                elegir(dia)
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                MarcaCalendario(NaranjaAlerta)
                Etiqueta("Pendientes")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                MarcaCalendario(VerdeIngreso)
                Etiqueta("Pagos realizados")
            }
        }
    }
}

@Composable
private fun DiaCalendario(
    dia: LocalDate,
    seleccionado: Boolean,
    pendiente: Boolean,
    pagado: Boolean,
    modifier: Modifier,
    elegir: () -> Unit,
) {
    val descripcion = buildString {
        append(fechaEnPantalla(dia))
        if (pendiente) append(", con vencimientos pendientes")
        if (pagado) append(", con pagos realizados")
    }
    val forma = RoundedCornerShape(12.dp)
    Box(
        modifier.clip(forma)
            .background(if (seleccionado) AzulNoche else Color.Transparent)
            .selectable(seleccionado, role = Role.RadioButton, onClick = elegir)
            .semantics { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Etiqueta(dia.dayOfMonth.toString(), if (seleccionado) Color.White else Texto)
            Row(Modifier.height(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (pendiente) MarcaCalendario(NaranjaAlerta)
                if (pagado) MarcaCalendario(VerdeIngreso)
            }
        }
    }
}

@Composable
private fun MarcaCalendario(color: Color) {
    Box(Modifier.size(5.dp).background(color, CircleShape))
}
