package cl.payroutine.ui.pantallas

import cl.payroutine.datos.local.dao.MovimientoConCategoria
import cl.payroutine.datos.local.entidad.TipoMovimiento
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val chile = Locale.forLanguageTag("es-CL")
private val formatoMes = DateTimeFormatter.ofPattern("MMMM uuuu", chile)
private val formatoFecha = DateTimeFormatter.ofPattern("d MMM uuuu", chile)
private val formatoDiaMes = DateTimeFormatter.ofPattern("d MMM", chile)

internal fun mesEnPantalla(fecha: LocalDate): String =
    formatoMes.format(fecha).replaceFirstChar { it.uppercase(chile) }

internal fun fechaEnPantalla(fecha: LocalDate): String = formatoFecha.format(fecha)

internal fun rangoEnPantalla(desde: LocalDate, hasta: LocalDate): String = when {
    desde == hasta -> fechaEnPantalla(desde)
    desde.year != hasta.year -> "${fechaEnPantalla(desde)} – ${fechaEnPantalla(hasta)}"
    desde.month == hasta.month -> "${desde.dayOfMonth}–${fechaEnPantalla(hasta)}"
    else -> "${formatoDiaMes.format(desde)} – ${fechaEnPantalla(hasta)}"
}

internal fun montoEnPantalla(monto: Number): String {
    val valor = monto.toString().toBigDecimal().stripTrailingZeros()
    return NumberFormat.getCurrencyInstance(chile).apply {
        maximumFractionDigits = valor.scale().coerceAtLeast(0)
    }.format(valor)
}

internal fun MovimientoConCategoria.esIngreso(): Boolean = movimiento.tipo == TipoMovimiento.Ingreso

internal fun MovimientoConCategoria.titulo(): String =
    movimiento.nombre.takeIf { it.isNotBlank() } ?: if (esIngreso()) "Ingreso" else "Gasto"

internal fun MovimientoConCategoria.fechaEnPantalla(zona: ZoneId): String =
    fechaEnPantalla(movimiento.fecha.toInstant().atZone(zona).toLocalDate())

internal fun MovimientoConCategoria.montoConSigno(): String =
    (if (esIngreso()) "+" else "−") + montoEnPantalla(movimiento.monto)
