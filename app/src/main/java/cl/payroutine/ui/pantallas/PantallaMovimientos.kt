package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.payroutine.datos.local.dao.MovimientoConCategoria
import cl.payroutine.ui.theme.*
import cl.payroutine.viewmodel.DatosInicio
import java.time.ZoneId

@Composable
internal fun PantallaMovimientos(datos: DatosInicio?, verMovimiento: (Int) -> Unit) {
    Column {
        Cabecera("Movimientos")
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pildora("Todos", activa = true)
                    Pildora("Ingresos")
                    Pildora("Gastos")
                }
            }
            when {
                datos == null -> item { Etiqueta("Cargando movimientos…") }
                datos.movimientos.isEmpty() -> item { Etiqueta("Todavía no tienes movimientos registrados.") }
                else -> items(datos.movimientos, key = { it.movimiento.id }) { fila ->
                    TarjetaMovimiento(fila, datos.fecha.zona) { verMovimiento(fila.movimiento.id) }
                }
            }
        }
    }
}

@Composable
internal fun TarjetaMovimiento(fila: MovimientoConCategoria, zona: ZoneId, alPulsar: () -> Unit) {
    val ingreso = fila.esIngreso()
    val color = if (ingreso) VerdeIngreso else RojoGasto
    val fondo = if (ingreso) FondoIngreso else FondoGasto
    FilaTarjeta(fila.titulo(), "${fila.categoria} · ${fila.fechaEnPantalla(zona)}", fila.montoConSigno(),
        if (ingreso) "down" else "up", color, fondo, color, alPulsar)
}
