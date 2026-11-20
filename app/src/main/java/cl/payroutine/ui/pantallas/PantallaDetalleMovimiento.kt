package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.payroutine.datos.local.dao.MovimientoConCategoria
import cl.payroutine.ui.theme.*
import java.time.ZoneId

@Composable
internal fun PantallaDetalleMovimiento(
    fila: MovimientoConCategoria?,
    zona: ZoneId?,
    volver: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Cabecera("Detalle", volver)
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
        }
    }
}
