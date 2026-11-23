package cl.payroutine

import cl.payroutine.datos.local.entidad.RecordatorioMovimiento
import cl.payroutine.datos.local.entidad.RecurrenciaRecordatorio
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarioRecordatorioTest {
    private val zona = ZoneId.of("America/Santiago")

    @Test
    fun diarioRespetaHoraYUltimoAvisoSinPedirFecha() {
        val hoy = LocalDate.of(2026, 11, 23)
        val base = RecordatorioMovimiento("prueba", activo = true, fechaReferencia = hoy.toEpochDay())
        assertEquals(hoy.atTime(20, 0).atZone(zona), base.siguienteAviso(hoy.atTime(19, 0).atZone(zona)))
        assertEquals(hoy.plusDays(1), base.siguienteAviso(hoy.atTime(21, 0).atZone(zona)).toLocalDate())
        assertEquals(hoy.plusDays(1), base.copy(ultimoAviso = hoy.toEpochDay())
            .siguienteAviso(hoy.atTime(19, 0).atZone(zona)).toLocalDate())
        val fecha = hoy.plusDays(2)
        assertEquals(hoy, base.copy(fechaInicial = fecha.toEpochDay())
            .siguienteAviso(hoy.atTime(19, 0).atZone(zona)).toLocalDate())
    }

    @Test
    fun semanalEmpiezaEnLaFechaElegidaYRepiteElMismoDia() {
        val inicio = LocalDate.of(2026, 11, 25)
        val base = RecordatorioMovimiento(
            "prueba", fechaInicial = inicio.toEpochDay(),
            recurrencia = RecurrenciaRecordatorio.Semanal,
        )
        assertEquals(inicio, base.siguienteAviso(inicio.minusWeeks(1).atTime(19, 0).atZone(zona)).toLocalDate())
        assertEquals(inicio.plusWeeks(1), base.siguienteAviso(inicio.atTime(21, 0).atZone(zona)).toLocalDate())
    }

    @Test
    fun mensualUsaUltimoDiaDelMesCortoYRecuperaElDiaOriginal() {
        val base = RecordatorioMovimiento(
            "prueba", fechaInicial = LocalDate.of(2026, 1, 31).toEpochDay(),
            recurrencia = RecurrenciaRecordatorio.Mensual,
        )
        val febrero = base.siguienteAviso(ZonedDateTime.of(2026, 2, 1, 15, 0, 0, 0, zona))
        assertEquals(LocalDate.of(2026, 2, 28), febrero.toLocalDate())
        assertEquals(LocalDate.of(2026, 3, 31), base.siguienteAviso(febrero.plusMinutes(1)).toLocalDate())
    }

    @Test
    fun finDeMesUsaUltimoDiaAunqueLaFechaInicialSeaOtra() {
        val base = RecordatorioMovimiento(
            "prueba", fechaInicial = LocalDate.of(2028, 2, 2).toEpochDay(),
            recurrencia = RecurrenciaRecordatorio.FinDeMes,
        )
        val febrero = base.siguienteAviso(ZonedDateTime.of(2028, 2, 3, 15, 0, 0, 0, zona))
        assertEquals(LocalDate.of(2028, 2, 29), febrero.toLocalDate())
        assertEquals(LocalDate.of(2028, 3, 31), base.siguienteAviso(febrero.plusMinutes(1)).toLocalDate())
    }
}
