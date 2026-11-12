package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.*

@Composable
internal fun PantallaMovimientos(verMovimiento: (Int) -> Unit) {
    Column {
        Cabecera("Movimientos")
        Cuerpo {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pildora("Todos", activa = true)
                Pildora("Ingresos")
                Pildora("Gastos")
            }
            DatosDeEjemplo.movimientos.forEachIndexed { indice, movimiento ->
                TarjetaMovimiento(movimiento) { verMovimiento(indice) }
            }
        }
    }
}

@Composable
internal fun TarjetaMovimiento(movimiento: MovimientoEjemplo, alPulsar: () -> Unit) {
    val color = if (movimiento.ingreso) VerdeIngreso else RojoGasto
    val fondo = if (movimiento.ingreso) FondoIngreso else FondoGasto
    FilaTarjeta(movimiento.nombre, movimiento.detalle, movimiento.monto,
        if (movimiento.ingreso) "down" else "up", color, fondo, color, alPulsar)
}
