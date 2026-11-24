package cl.payroutine.ui.pantallas

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cl.payroutine.datos.local.dao.MovimientoConCategoria
import cl.payroutine.ui.theme.*
import cl.payroutine.viewmodel.EstadoEliminacionMovimiento
import java.time.ZoneId

@Composable
internal fun PantallaDetalleMovimiento(
    fila: MovimientoConCategoria?,
    zona: ZoneId?,
    estadoEliminacion: EstadoEliminacionMovimiento,
    editar: () -> Unit,
    eliminar: () -> Unit,
    limpiarError: () -> Unit,
    volver: () -> Unit,
) {
    var confirmar by rememberSaveable { mutableStateOf(false) }
    val vinculado = fila?.pagoId != null
    val eliminando = estadoEliminacion == EstadoEliminacionMovimiento.Eliminando
    val bloqueado = eliminando || estadoEliminacion == EstadoEliminacionMovimiento.Eliminado
    BackHandler(enabled = eliminando) {}
    Column(Modifier.fillMaxSize()) {
        Cabecera("Detalle", volver = { if (!bloqueado) volver() })
        Cuerpo(Modifier.weight(1f), espacio = 14) {
            if (zona == null) {
                Etiqueta("Cargando movimiento…")
                return@Cuerpo
            }
            if (fila == null) {
                Etiqueta("El movimiento ya no está disponible.")
                return@Cuerpo
            }

            val ingreso = fila.esIngreso()
            val color = if (ingreso) VerdeIngreso else RojoGasto
            val fondo = if (ingreso) FondoIngreso else FondoGasto
            val datos = listOf(
                "Fecha" to fila.fechaEnPantalla(zona),
                "Categoría" to fila.categoria,
                "Nota" to (fila.movimiento.nota?.takeIf { it.isNotBlank() } ?: "—"),
            )
            Tarjeta(Modifier.fillMaxWidth(), relleno = 20) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Insignia(if (ingreso) "down" else "up", color, fondo, 54)
                    Cifra(fila.titulo(), tamano = 18)
                    Cifra(fila.montoConSigno(), color, 32)
                }
            }
            Tarjeta(Modifier.fillMaxWidth(), espacio = 12) {
                datos.forEach { (titulo, valor) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Etiqueta(titulo)
                        Etiqueta(valor, Texto)
                    }
                }
            }
            BotonPrincipal(if (vinculado) "Editar fecha y nota" else "Editar movimiento", habilitado = !bloqueado, alPulsar = editar)
            TextButton(
                onClick = {
                    limpiarError()
                    confirmar = true
                },
                enabled = !bloqueado,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = RojoGasto),
            ) { Text(if (vinculado) "Deshacer pago" else "Eliminar movimiento") }
        }
    }

    if (confirmar && fila != null && estadoEliminacion != EstadoEliminacionMovimiento.Eliminado) {
        fun cerrarConfirmacion() {
            if (eliminando) return
            confirmar = false
            limpiarError()
        }
        AlertDialog(
            onDismissRequest = ::cerrarConfirmacion,
            title = { Text(if (vinculado) "¿Deshacer pago?" else "¿Eliminar movimiento?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (vinculado) {
                        Text("«${fila.titulo()}» volverá a Pendientes y se retirará el gasto.")
                    } else {
                        Text("Se eliminará «${fila.titulo()}» (${fila.montoConSigno()}). Esta acción es irreversible.")
                    }
                    if (estadoEliminacion == EstadoEliminacionMovimiento.Error) {
                        Text(
                            if (vinculado) "No se pudo deshacer el pago. Vuelve a intentarlo."
                            else "No se pudo eliminar el movimiento. Vuelve a intentarlo.",
                            color = RojoGasto,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = eliminar,
                    enabled = !eliminando,
                    colors = ButtonDefaults.textButtonColors(contentColor = RojoGasto),
                ) {
                    Text(when {
                        eliminando && vinculado -> "Guardando…"
                        eliminando -> "Eliminando…"
                        vinculado -> "Deshacer pago"
                        else -> "Eliminar"
                    })
                }
            },
            dismissButton = { TextButton(onClick = ::cerrarConfirmacion, enabled = !eliminando) { Text("Cancelar") } },
            properties = DialogProperties(dismissOnBackPress = !eliminando, dismissOnClickOutside = !eliminando),
        )
    }
}
