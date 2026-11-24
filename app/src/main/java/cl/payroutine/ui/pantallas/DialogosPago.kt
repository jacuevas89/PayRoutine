package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cl.payroutine.datos.local.dao.PagoConCategoria
import cl.payroutine.ui.theme.RojoGasto
import cl.payroutine.viewmodel.EstadoOperacionPago
import cl.payroutine.viewmodel.FechaLocal
import java.time.LocalDate

@Composable
internal fun DialogoConfirmarPagos(
    vencimientos: List<PagoConCategoria>,
    fechaLocal: FechaLocal,
    estado: EstadoOperacionPago,
    cerrar: () -> Unit,
    pagar: (List<Int>, LocalDate, String) -> Unit,
) {
    var dia by rememberSaveable { mutableLongStateOf(fechaLocal.hoy.toEpochDay()) }
    var nota by rememberSaveable { mutableStateOf("") }
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }
    val fecha = LocalDate.ofEpochDay(dia)
    val total = vencimientos.sumOf { it.pago.monto.toBigDecimal() }
    val bloqueado = estado.guardando || estado.completada
    val disponibles = vencimientos.isNotEmpty() &&
        vencimientos.all { !it.pago.pagado && it.nombreCategoria != null }

    AlertDialog(
        onDismissRequest = { if (!bloqueado) cerrar() },
        title = { Text(if (vencimientos.size == 1) "Confirmar pago" else "Confirmar pagos") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                vencimientos.forEach { fila ->
                    val vencimiento = fila.pago.fechaVencimiento.toInstant().atZone(fechaLocal.zona).toLocalDate()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Destacado(fila.pago.descripcion)
                            Etiqueta("Vence el ${fechaEnPantalla(vencimiento)}")
                            fila.nombreCategoria?.let { Etiqueta(it) }
                        }
                        Destacado(montoEnPantalla(fila.pago.monto))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Destacado("Total")
                    Destacado(montoEnPantalla(total))
                }
                CampoVencimientoPago(
                    etiqueta = "Fecha del pago",
                    descripcion = "Seleccionar fecha del pago",
                    fecha = fecha,
                    alPulsar = if (bloqueado) null else ({ mostrarCalendario = true }),
                )
                OutlinedTextField(
                    value = nota, onValueChange = { nota = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (vencimientos.size == 1) "Nota (opcional)" else "Nota para todos los gastos (opcional)") },
                    enabled = !bloqueado, minLines = 2, maxLines = 3,
                )
                val error = estado.error ?: if (!disponibles && !bloqueado) "Revisa los vencimientos seleccionados." else null
                if (error != null) Etiqueta(error, RojoGasto)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { pagar(vencimientos.map { it.pago.id }, fecha, nota) },
                enabled = !bloqueado && disponibles && fecha <= fechaLocal.hoy,
            ) { Text(if (estado.guardando) "Registrando…" else "Confirmar") }
        },
        dismissButton = { TextButton(onClick = cerrar, enabled = !bloqueado) { Text("Cancelar") } },
        properties = DialogProperties(dismissOnBackPress = !bloqueado, dismissOnClickOutside = !bloqueado),
    )
    if (mostrarCalendario && !bloqueado) {
        SelectorVencimientoPago(fecha, fechaMaxima = fechaLocal.hoy, cerrar = { mostrarCalendario = false }) {
            dia = it.toEpochDay()
            mostrarCalendario = false
        }
    }
}

@Composable
internal fun DialogoDeshacerPago(
    fila: PagoConCategoria,
    estado: EstadoOperacionPago,
    cerrar: () -> Unit,
    deshacer: () -> Unit,
) {
    val bloqueado = estado.guardando || estado.completada
    AlertDialog(
        onDismissRequest = { if (!bloqueado) cerrar() },
        title = { Text("¿Deshacer pago?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("«${fila.pago.descripcion}» volverá a Pendientes y se retirará el gasto de ${montoEnPantalla(fila.pago.monto)}.")
                estado.error?.let { Etiqueta(it, RojoGasto) }
            }
        },
        confirmButton = {
            TextButton(onClick = deshacer, enabled = !bloqueado) {
                Text(if (estado.guardando) "Guardando…" else "Deshacer pago", color = RojoGasto)
            }
        },
        dismissButton = { TextButton(onClick = cerrar, enabled = !bloqueado) { Text("Cancelar") } },
        properties = DialogProperties(dismissOnBackPress = !bloqueado, dismissOnClickOutside = !bloqueado),
    )
}
