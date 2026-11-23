package cl.payroutine.ui.pantallas

import androidx.compose.ui.graphics.Color
import cl.payroutine.ui.theme.*

internal object DatosDeEjemplo {
    const val mes = "Octubre 2026"
    const val gastos = "$842.300"
    const val presupuestoTotal = "$910.000"
    const val presupuestoDisponible = "$67.700"
    const val porPagar = "$209.990"

    val presupuestoOcio = PresupuestoEjemplo("Ocio", "$142.800", "$120.000", 119, "play", RojoGasto, FondoGasto)
    const val excesoOcio = "$22.800"
    val gastosOcio = listOf(
        MovimientoEjemplo("Salida con amigos", "Ocio · 24 oct", "-$60.000"),
        MovimientoEjemplo("Videojuego", "Ocio · 18 oct", "-$55.810"),
        MovimientoEjemplo("Cine", "Ocio · 14 oct", "-$18.000"),
        MovimientoEjemplo("Streaming", "Ocio · 10 oct", "-$8.990"),
    )

    val presupuestos = listOf(
        PresupuestoEjemplo("Alimentación", "$210.000", "$250.000", 84, "cart"),
        PresupuestoEjemplo("Transporte", "$64.300", "$80.000", 80, "bus"),
        PresupuestoEjemplo("Hogar", "$380.000", "$400.000", 95, "house", NaranjaAlerta, FondoAlerta),
        PresupuestoEjemplo("Servicios", "$45.200", "$60.000", 75, "bolt"),
        presupuestoOcio,
    )

    val pagos = listOf(
        PagoEjemplo("Tarjeta de crédito", "Vence 02 nov", "$120.000", "En 4 días", "card", NaranjaAlerta, FondoAlerta),
        PagoEjemplo("Cuota notebook (4 de 12)", "Vence 05 nov", "$65.000", "En 7 días", "calendar"),
        PagoEjemplo("Internet hogar", "Vence 10 nov", "$24.990", "En 12 días", "wifi"),
    )
}

internal data class MovimientoEjemplo(val nombre: String, val detalle: String, val monto: String, val ingreso: Boolean = false)

internal data class PresupuestoEjemplo(
    val categoria: String, val gastado: String, val limite: String, val porcentaje: Int,
    val icono: String, val color: Color = VerdeMarca, val fondo: Color = FondoMarca,
)

internal data class PagoEjemplo(
    val nombre: String, val fecha: String, val monto: String, val plazo: String,
    val icono: String, val color: Color = AzulNoche, val fondo: Color = FondoSelector,
)
