package cl.payroutine.viewmodel

import cl.payroutine.datos.local.dao.MovimientoConCategoria
import cl.payroutine.datos.local.entidad.TipoMovimiento
import java.math.BigDecimal
import java.math.MathContext
import java.time.YearMonth
import java.time.ZoneId

internal data class ImportesMensuales(val ingresos: BigDecimal, val gastos: BigDecimal) {
    val saldo = ingresos - gastos
}

internal data class GastoPorCategoria(
    val categoria: String,
    val monto: BigDecimal,
    val proporcion: BigDecimal,
)

internal data class ResumenMensual(
    val importes: ImportesMensuales,
    val distribucion: List<GastoPorCategoria>,
    val tieneMovimientos: Boolean,
)

internal fun movimientosDelMes(
    movimientos: List<MovimientoConCategoria>,
    mes: YearMonth,
    zona: ZoneId,
): List<MovimientoConCategoria> {
    val desde = mes.atDay(1).atStartOfDay(zona).toInstant().toEpochMilli()
    val hasta = mes.plusMonths(1).atDay(1).atStartOfDay(zona).toInstant().toEpochMilli()
    return movimientos.filter { it.movimiento.fecha.time >= desde && it.movimiento.fecha.time < hasta }
}

internal fun calcularImportes(movimientos: List<MovimientoConCategoria>): ImportesMensuales {
    var ingresos = BigDecimal.ZERO
    var gastos = BigDecimal.ZERO
    for ((movimiento) in movimientos) {
        val monto = BigDecimal.valueOf(movimiento.monto)
        when (movimiento.tipo) {
            TipoMovimiento.Ingreso -> ingresos += monto
            TipoMovimiento.Gasto -> gastos += monto
        }
    }
    return ImportesMensuales(ingresos, gastos)
}

internal fun calcularResumenMensual(
    movimientos: List<MovimientoConCategoria>,
    mes: YearMonth,
    zona: ZoneId,
): ResumenMensual {
    val filas = movimientosDelMes(movimientos, mes, zona)
    val importes = calcularImportes(filas)
    val distribucion = filas.asSequence()
        .filter { it.movimiento.tipo == TipoMovimiento.Gasto }
        .groupBy { it.movimiento.categoriaId }
        .values
        .map { gastos ->
            val monto = gastos.sumOf { it.movimiento.monto.toBigDecimal() }
            val proporcion = if (importes.gastos.signum() > 0) {
                monto.divide(importes.gastos, MathContext.DECIMAL64)
            } else {
                BigDecimal.ZERO
            }
            GastoPorCategoria(gastos.first().categoria, monto, proporcion)
        }
        .sortedWith(compareByDescending<GastoPorCategoria> { it.monto }.thenBy { it.categoria })
    return ResumenMensual(importes, distribucion, filas.isNotEmpty())
}
