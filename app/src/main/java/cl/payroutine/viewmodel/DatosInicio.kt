package cl.payroutine.viewmodel

import cl.payroutine.datos.local.dao.MovimientoConCategoria
import cl.payroutine.datos.local.entidad.PagoPendiente
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal data class FechaLocal(val hoy: LocalDate, val zona: ZoneId)

internal data class PagoEnInicio(
    val pago: PagoPendiente,
    val vencimiento: LocalDate,
    val diasParaVencer: Long,
)

internal data class DatosInicio(
    val fecha: FechaLocal,
    val ingresos: BigDecimal,
    val gastos: BigDecimal,
    val pagosPendientes: List<PagoEnInicio>,
    val movimientos: List<MovimientoConCategoria>,
) {
    val saldo = ingresos - gastos
    val recientes = movimientos.take(3)
}

// Las consultas de movimientos y pagos ya entregan las filas en el orden requerido.
internal fun calcularInicio(
    fecha: FechaLocal,
    movimientos: List<MovimientoConCategoria>,
    pagos: List<PagoPendiente>,
): DatosInicio {
    val mes = YearMonth.from(fecha.hoy)
    val importes = calcularImportes(movimientosDelMes(movimientos, mes, fecha.zona))

    val pendientes = pagos.asSequence()
        .filterNot { it.pagado }
        .map { pago ->
            val vencimiento = pago.fechaVencimiento.toInstant().atZone(fecha.zona).toLocalDate()
            PagoEnInicio(pago, vencimiento, ChronoUnit.DAYS.between(fecha.hoy, vencimiento))
        }
        .filter { it.vencimiento <= mes.atEndOfMonth() }
        .take(3)
        .toList()

    return DatosInicio(fecha, importes.ingresos, importes.gastos, pendientes, movimientos)
}
