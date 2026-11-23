package cl.payroutine.ui.pantallas

import androidx.compose.ui.graphics.Color
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.local.normalizarNombreCategoria
import cl.payroutine.ui.theme.*

internal data class AparienciaCategoria(val icono: String, val color: Color, val fondo: Color)

internal fun aparienciaCategoria(categoria: Categoria): AparienciaCategoria {
    if (!categoria.esPredefinida) {
        return if (categoria.tipo == TipoMovimiento.Ingreso) AparienciaCategoria("coin", VerdeIngreso, FondoIngreso)
        else AparienciaCategoria("coin", TextoSecundario, FondoSelector)
    }
    val nombre = normalizarNombreCategoria(categoria.nombre)
    if (categoria.tipo == TipoMovimiento.Ingreso) {
        val icono = when (nombre) {
            "sueldo", "pensiones" -> "coin"
            "freelance" -> "trabajo"
            "ventas" -> "ventas"
            "arriendos" -> "house"
            else -> "coin"
        }
        return AparienciaCategoria(icono, VerdeIngreso, FondoIngreso)
    }
    return when (nombre) {
        "alimentación" -> AparienciaCategoria("cart", VerdeMarca, FondoMarca)
        "transporte" -> AparienciaCategoria("bus", AzulNoche, FondoSelector)
        "hogar" -> AparienciaCategoria("house", NaranjaAlerta, FondoAlerta)
        "servicios" -> AparienciaCategoria("bolt", VerdeMarca, FondoMarca)
        "ocio" -> AparienciaCategoria("play", RojoGasto, FondoGasto)
        "salud" -> AparienciaCategoria("heart", AzulNoche, FondoSelector)
        "educación" -> AparienciaCategoria("educacion", AzulNoche, FondoSelector)
        "vestuario" -> AparienciaCategoria("vestuario", VerdeMarca, FondoMarca)
        "mascotas" -> AparienciaCategoria("mascotas", NaranjaAlerta, FondoAlerta)
        else -> AparienciaCategoria("coin", TextoSecundario, FondoSelector)
    }
}
