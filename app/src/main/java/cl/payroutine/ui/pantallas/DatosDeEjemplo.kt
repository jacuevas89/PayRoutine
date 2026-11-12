package cl.payroutine.ui.pantallas

import androidx.compose.ui.graphics.Color
import cl.payroutine.ui.theme.*

internal object DatosDeEjemplo {
    const val saludo = "Hola, Grupo proyecto esp."
    const val mes = "Octubre 2026"
    const val saldo = "$407.700"
    const val ingresos = "$1.250.000"
    const val gastos = "$842.300"
    const val presupuestoTotal = "$910.000"
    const val presupuestoDisponible = "$67.700"
    const val porPagar = "$209.990"

    val movimientos = listOf(
        MovimientoEjemplo("Supermercado", "Alimentación · 27 oct", "-$124.500"),
        MovimientoEjemplo("Transporte", "Transporte · 26 oct", "-$18.000"),
        MovimientoEjemplo("Cuenta de luz", "Servicios · 15 oct", "-$45.200"),
        MovimientoEjemplo("Freelance", "Ingreso extra · 12 oct", "+$150.000", ingreso = true),
        MovimientoEjemplo("Arriendo", "Hogar · 05 oct", "-$380.000"),
        MovimientoEjemplo("Sueldo", "Ingreso fijo · 01 oct", "+$1.100.000", ingreso = true),
    )

    val presupuestos = listOf(
        PresupuestoEjemplo("Alimentación", "$210.000", "$250.000", 84, "cart"),
        PresupuestoEjemplo("Transporte", "$64.300", "$80.000", 80, "bus"),
        PresupuestoEjemplo("Hogar", "$380.000", "$400.000", 95, "house", NaranjaAlerta, FondoAlerta),
        PresupuestoEjemplo("Servicios", "$45.200", "$60.000", 75, "bolt"),
        PresupuestoEjemplo("Ocio", "$142.800", "$120.000", 119, "play", RojoGasto, FondoGasto),
    )

    val pagos = listOf(
        PagoEjemplo("Tarjeta de crédito", "Vence 02 nov", "$120.000", "En 4 días", "card", NaranjaAlerta, FondoAlerta),
        PagoEjemplo("Cuota notebook (4 de 12)", "Vence 05 nov", "$65.000", "En 7 días", "calendar"),
        PagoEjemplo("Internet hogar", "Vence 10 nov", "$24.990", "En 12 días", "wifi"),
    )

    val gastosPorCategoria = listOf(
        GastoEjemplo("Hogar", "$380.000", 45, AzulNoche),
        GastoEjemplo("Alimentación", "$210.000", 25, VerdeMarca),
        GastoEjemplo("Ocio", "$142.800", 17, RojoGasto),
        GastoEjemplo("Transporte", "$64.300", 8, NaranjaAlerta),
        GastoEjemplo("Servicios", "$45.200", 5, TextoSecundario),
    )

    val formularioGasto = FormularioMovimientoEjemplo(
        "Nuevo gasto", "$ 45.000", listOf("Alimentación", "Transporte", "Hogar", "Servicios", "Ocio", "Salud"),
        "Ej: compra de la semana", "Guardar gasto", RojoGasto,
    )
    val formularioIngreso = FormularioMovimientoEjemplo(
        "Nuevo ingreso", "$ 150.000", listOf("Sueldo", "Freelance", "Otros ingresos"),
        "Ej: pago de proyecto", "Guardar ingreso", VerdeIngreso,
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

internal data class GastoEjemplo(val categoria: String, val monto: String, val porcentaje: Int, val color: Color)

internal data class FormularioMovimientoEjemplo(
    val titulo: String, val monto: String, val categorias: List<String>,
    val nota: String, val guardar: String, val color: Color,
)
