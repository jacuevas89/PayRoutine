package cl.payroutine.ui.pantallas

import androidx.compose.ui.graphics.Color
import cl.payroutine.ui.theme.*

internal object DatosDeEjemplo {
    val pagos = listOf(
        PagoEjemplo("Tarjeta de crédito", "Vence 02 nov", "$120.000", "En 4 días", "card", NaranjaAlerta, FondoAlerta),
        PagoEjemplo("Cuota notebook (4 de 12)", "Vence 05 nov", "$65.000", "En 7 días", "calendar"),
        PagoEjemplo("Internet hogar", "Vence 10 nov", "$24.990", "En 12 días", "wifi"),
    )
}

internal data class PagoEjemplo(
    val nombre: String, val fecha: String, val monto: String, val plazo: String,
    val icono: String, val color: Color = AzulNoche, val fondo: Color = FondoSelector,
)
