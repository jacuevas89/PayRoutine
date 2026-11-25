package cl.payroutine

import cl.payroutine.datos.local.dao.MovimientoConCategoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.viewmodel.FiltroMovimientos
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class FiltroMovimientosTest {
    @Test
    fun combinaFiltrosPorTipoCategoriaYDiaLocalSinCambiarElOrden() {
        val zona = ZoneId.of("America/Santiago")
        val desde = LocalDate.of(2026, 11, 20)
        val hasta = desde.plusDays(1)
        val filas = listOf(
            fila(6, 10, "2026-11-22T03:00:00Z"),
            fila(5, 10, "2026-11-22T02:59:59Z"),
            fila(4, 11, "2026-11-21T12:00:00Z"),
            fila(3, 12, "2026-11-21T10:00:00Z", TipoMovimiento.Ingreso),
            fila(2, 10, "2026-11-20T03:00:00Z"),
            fila(1, 10, "2026-11-20T02:59:59Z"),
        )

        assertEquals(filas, FiltroMovimientos().aplicar(filas, zona))
        assertEquals(listOf(filas[3]), FiltroMovimientos(tipo = TipoMovimiento.Ingreso).aplicar(filas, zona))
        assertEquals(listOf(filas[1], filas[4]), FiltroMovimientos(TipoMovimiento.Gasto, 10, desde, hasta).aplicar(filas, zona))
        assertEquals(listOf(filas[1]), FiltroMovimientos(categoriaId = 10, desde = hasta, hasta = hasta).aplicar(filas, zona))
    }

    private fun fila(id: Int, categoriaId: Int, instante: String, tipo: TipoMovimiento = TipoMovimiento.Gasto) =
        MovimientoConCategoria(
            Movimiento(id = id, tipo = tipo, monto = 1000L, fecha = Date.from(Instant.parse(instante)), categoriaId = categoriaId),
            categoria = "Servicios",
        )
}
