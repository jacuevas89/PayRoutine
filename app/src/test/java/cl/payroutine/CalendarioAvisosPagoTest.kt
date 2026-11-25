package cl.payroutine

import cl.payroutine.datos.local.entidad.FrecuenciaAvisoPago
import cl.payroutine.datos.local.entidad.OpcionesAvisoPago
import cl.payroutine.datos.local.entidad.RecordatorioPago
import cl.payroutine.datos.local.entidad.proximoAviso
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarioAvisosPagoTest {
    private val zona = ZoneId.of("America/Santiago")
    private val vencimiento = LocalDate.of(2026, 12, 2)
    private val base = OpcionesAvisoPago(anticipacion = 3)

    @Test
    fun respetaAnticipacionYPosponeHastaMananaSiLaHoraPaso() {
        val diaPrevisto = vencimiento.minusDays(3)
        val aviso = RecordatorioPago(1)
        assertEquals(diaPrevisto, base.proximoAviso(aviso, vencimiento, diaPrevisto.atTime(19, 0).atZone(zona), false))
        assertEquals(diaPrevisto.plusDays(1), base.proximoAviso(aviso, vencimiento, diaPrevisto.atTime(21, 0).atZone(zona), false))
        assertEquals(vencimiento.plusDays(3), base.proximoAviso(aviso, vencimiento, vencimiento.plusDays(2).atStartOfDay(zona), false))
    }

    @Test
    fun calculaCadaFrecuenciaDesdeElUltimoAviso() {
        val aviso = RecordatorioPago(1, ultimoAviso = vencimiento.toEpochDay())
        val ahora = vencimiento.atTime(21, 0).atZone(zona)
        assertNull(base.proximoAviso(aviso, vencimiento, ahora, false))
        for (frecuencia in FrecuenciaAvisoPago.entries.filter { it.dias != null }) {
            val fecha = base.copy(frecuencia = frecuencia).proximoAviso(aviso, vencimiento, ahora, false)
            assertEquals(vencimiento.plusDays(requireNotNull(frecuencia.dias)), fecha)
        }
    }

    @Test
    fun ajustaLaFechaCuandoCambiaElDiaLocalDelVencimiento() {
        val previsto = vencimiento.minusDays(3)
        val aviso = RecordatorioPago(1, diaProgramado = previsto.toEpochDay(), diaVencimientoProgramado = vencimiento.toEpochDay())
        val nuevoVencimiento = vencimiento.plusDays(1)
        val ahora = previsto.atStartOfDay(zona)
        assertEquals(previsto.plusDays(1), base.proximoAviso(aviso, nuevoVencimiento, ahora, false))
    }
}
