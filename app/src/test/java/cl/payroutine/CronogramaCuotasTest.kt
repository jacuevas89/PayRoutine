package cl.payroutine

import cl.payroutine.datos.repositorio.AjusteCuota
import cl.payroutine.datos.repositorio.CuotaProgramada
import cl.payroutine.datos.repositorio.NuevaDeudaEnCuotas
import cl.payroutine.datos.repositorio.PeriodicidadCuotas
import cl.payroutine.datos.repositorio.generarCronogramaCuotas
import cl.payroutine.datos.repositorio.validarCronogramaCuotas
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CronogramaCuotasTest {
    private val primera = LocalDate.of(2027, 1, 31)

    @Test
    fun conservaElDiaMensualYAplicaElResiduoSegunLaSeleccion() {
        val ultima = generarCronogramaCuotas(100000, 3, primera, PeriodicidadCuotas.Mensual, AjusteCuota.Ultima)
        val primeraCuota = generarCronogramaCuotas(100000, 3, primera, PeriodicidadCuotas.Mensual, AjusteCuota.Primera)
        assertEquals(listOf(primera, LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31)), ultima.map { it.vencimiento })
        assertEquals(listOf(33333L, 33333L, 33334L), ultima.map { it.monto })
        assertEquals(listOf(33334L, 33333L, 33333L), primeraCuota.map { it.monto })
    }

    @Test
    fun generaPeriodosDeSieteYQuinceDiasExactos() {
        for ((periodicidad, dias) in listOf(PeriodicidadCuotas.Semanal to 7L, PeriodicidadCuotas.Quincenal to 15L)) {
            val cuotas = generarCronogramaCuotas(1300, 3, primera, periodicidad, AjusteCuota.Ultima)
            assertEquals(listOf(primera, primera.plusDays(dias), primera.plusDays(dias * 2)), cuotas.map { it.vencimiento })
        }
    }

    @Test
    fun admiteMontosManualesYPoneLimitesAUnCronogramaInconsistente() {
        val cuotas = listOf(CuotaProgramada(primera, 30000), CuotaProgramada(primera.plusDays(17), 70000))
        val deuda = NuevaDeudaEnCuotas("Notebook", 100000, 1, 1, cuotas)
        validarCronogramaCuotas(deuda)
        assertThrows(IllegalArgumentException::class.java) { validarCronogramaCuotas(deuda.copy(montoTotal = 100001)) }
        assertThrows(IllegalArgumentException::class.java) { validarCronogramaCuotas(deuda.copy(cronograma = cuotas.reversed())) }
        assertThrows(IllegalArgumentException::class.java) { validarCronogramaCuotas(deuda.copy(cuotasPagadas = 3)) }
        assertThrows(IllegalArgumentException::class.java) {
            generarCronogramaCuotas(999999, 999999, primera, PeriodicidadCuotas.Semanal, AjusteCuota.Ultima)
        }
    }
}
