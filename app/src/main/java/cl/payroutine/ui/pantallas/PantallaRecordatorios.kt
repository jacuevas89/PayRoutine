package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.*

@Composable
internal fun PantallaRecordatorios(volver: () -> Unit) {
    Column {
        Cabecera("Recordatorios", volver)
        Cuerpo {
            Tarjeta(Modifier.fillMaxWidth(), relleno = 14) {
                FilaAviso("Activar recordatorios", "Avisos de pagos por vencer", activo = true)
            }
            Tarjeta(Modifier.fillMaxWidth(), relleno = 14, espacio = 9) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Etiqueta("Anticipación")
                    Etiqueta("1 día antes", Texto)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Etiqueta("Hora del aviso")
                    Etiqueta("09:00", Texto)
                }
            }
            Etiqueta("Pagos con aviso")
            DatosDeEjemplo.pagos.forEachIndexed { indice, pago ->
                Tarjeta(Modifier.fillMaxWidth(), relleno = 12) {
                    FilaAviso(pago.nombre, pago.fecha, activo = indice < 2, icono = "calendar")
                }
            }
            Etiqueta("Así se verá el aviso")
            FilaTarjeta("Mañana vence: Tarjeta de crédito", "Monto a pagar: $120.000", "",
                "bell", VerdeMarca, FondoMarca)
        }
    }
}

@Composable
private fun FilaAviso(titulo: String, detalle: String, activo: Boolean, icono: String? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icono != null) Insignia(icono, AzulNoche, FondoSelector, tamano = 34)
        Column(Modifier.weight(1f)) {
            Destacado(titulo)
            Etiqueta(detalle)
        }
        Switch(checked = activo, onCheckedChange = null)
    }
}
