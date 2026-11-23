package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.theme.FondoMarca
import cl.payroutine.ui.theme.VerdeMarca

private val consejosAhorro = listOf(
    "Anota tus gastos hormiga" to "Los gastos chicos del día a día se acumulan. Registrarlos te ayuda a verlos.",
    "Ahorra apenas recibas tu ingreso" to "Separa un monto fijo al inicio del mes, aunque sea pequeño, antes de gastar.",
    "Revisa tus pagos pendientes" to "Mira los vencimientos de la semana para evitar atrasos y recargos.",
    "Compara tu presupuesto cada semana" to "Si una categoría va muy alta a mitad de mes, ajústala antes de que termine.",
)

@Composable
internal fun PantallaConsejosAhorro(volver: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Cabecera("Consejos de ahorro", volver)
        Cuerpo(Modifier.weight(1f)) {
            Etiqueta("Ideas simples para ordenar tu mes.")
            consejosAhorro.forEach { (titulo, detalle) ->
                FilaTarjeta(titulo, detalle, "", "bulb", VerdeMarca, FondoMarca)
            }
        }
        Etiqueta(
            "Consejos generales, no son recomendaciones personalizadas.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
