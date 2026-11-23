package cl.payroutine.viewmodel

import cl.payroutine.datos.local.dao.MovimientoConCategoria
import cl.payroutine.datos.local.entidad.TipoMovimiento
import java.time.LocalDate
import java.time.ZoneId

internal data class FiltroMovimientos(
    val tipo: TipoMovimiento? = null,
    val categoriaId: Int? = null,
    val desde: LocalDate? = null,
    val hasta: LocalDate? = null,
) {
    fun aplicar(filas: List<MovimientoConCategoria>, zona: ZoneId): List<MovimientoConCategoria> {
        if (tipo == null && categoriaId == null && desde == null && hasta == null) return filas
        return filas.filter { (movimiento) ->
            if (tipo != null && movimiento.tipo != tipo) return@filter false
            if (categoriaId != null && movimiento.categoriaId != categoriaId) return@filter false
            if (desde == null && hasta == null) return@filter true
            val dia = movimiento.fecha.toInstant().atZone(zona).toLocalDate()
            (desde == null || dia >= desde) && (hasta == null || dia <= hasta)
        }
    }
}
