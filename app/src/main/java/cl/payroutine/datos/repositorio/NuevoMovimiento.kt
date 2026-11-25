package cl.payroutine.datos.repositorio

import cl.payroutine.datos.local.entidad.TipoMovimiento
import java.time.LocalDate

data class NuevoMovimiento(
    val nombre: String,
    val tipo: TipoMovimiento,
    val monto: Long,
    val fecha: LocalDate,
    val categoriaId: Int,
    val nota: String = "",
)

// Mantiene el máximo aceptado por los formularios de PayRoutine.
internal const val MONTO_MAXIMO_EXACTO = 9_007_199_254_740_991L

internal data class MontoValidado(val valor: Long? = null, val error: String? = null)

internal fun validarMontoMovimiento(texto: String): MontoValidado {
    val limpio = texto.trim()
    if (limpio.isEmpty()) return MontoValidado(error = "Ingresa el monto")
    if (!Regex("(?:[0-9]+|[1-9][0-9]{0,2}(?:\\.[0-9]{3})+)").matches(limpio)) {
        return MontoValidado(error = "Ingresa pesos enteros, sin decimales. Ej.: 15000 o 15.000")
    }
    val monto = limpio.replace(".", "").toLongOrNull()
    return when {
        monto == null || monto > MONTO_MAXIMO_EXACTO -> MontoValidado(error = "El monto es demasiado grande")
        monto <= 0 -> MontoValidado(error = "El monto debe ser mayor que cero")
        else -> MontoValidado(valor = monto)
    }
}
