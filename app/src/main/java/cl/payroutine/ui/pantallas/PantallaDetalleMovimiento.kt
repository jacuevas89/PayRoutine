package cl.payroutine.ui.pantallas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.*

@Composable
internal fun PantallaDetalleMovimiento(indice: Int, volver: () -> Unit) {
    val movimiento = DatosDeEjemplo.movimientos.getOrElse(indice) { DatosDeEjemplo.movimientos.first() }
    val tipo = if (movimiento.ingreso) "Ingreso" else "Gasto"
    val color = if (movimiento.ingreso) VerdeIngreso else RojoGasto
    val fondo = if (movimiento.ingreso) FondoIngreso else FondoGasto
    val dia = movimiento.detalle.substringAfter(" · ").substringBefore(" ")
    val datos = listOf(
        "Fecha" to "$dia de octubre de 2026",
        "Categoría" to movimiento.detalle.substringBefore(" · "),
        "Tipo" to tipo,
        "Nota" to if (indice == 0) "Compra de la semana" else "—",
        "Registrado" to "$dia oct, 19:42",
    )
    Column(Modifier.fillMaxSize()) {
        Cabecera("Detalle", volver)
        Cuerpo(Modifier.weight(1f), espacio = 14) {
            Tarjeta(Modifier.fillMaxWidth(), relleno = 20) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Insignia(if (movimiento.ingreso) "down" else "up", color, fondo, 54)
                    Cifra(movimiento.nombre, tamano = 18)
                    Cifra(movimiento.monto, color, 32)
                    Surface(color = fondo, shape = RoundedCornerShape(12.dp)) {
                        Etiqueta(tipo, color, Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                    }
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
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccionDetalle("Editar", VerdeMarca, Modifier.weight(1f))
            AccionDetalle("Eliminar", RojoGasto, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AccionDetalle(texto: String, color: Color, modifier: Modifier) {
    Surface(modifier.height(48.dp), color = Color.White, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, color)) {
        Box(contentAlignment = Alignment.Center) { Destacado(texto, color) }
    }
}
