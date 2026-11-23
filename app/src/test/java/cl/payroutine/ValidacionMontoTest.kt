package cl.payroutine

import cl.payroutine.datos.repositorio.MONTO_MAXIMO_EXACTO
import cl.payroutine.datos.repositorio.validarMontoMovimiento
import org.junit.Assert.*
import org.junit.Test

class ValidacionMontoTest {
    @Test
    fun aceptaPesosEnterosConOSinSeparadoresDeMiles() {
        for (texto in listOf("15000", "15.000", " 15.000 ", "00015000")) {
            assertEquals(texto, 15000L, validarMontoMovimiento(texto).valor)
            assertNull(texto, validarMontoMovimiento(texto).error)
        }
        assertEquals(MONTO_MAXIMO_EXACTO, validarMontoMovimiento("9.007.199.254.740.991").valor)
    }

    @Test
    fun rechazaVaciosCeroDecimalesNegativosYValoresQuePierdenPrecision() {
        for (texto in listOf("", " ", "0", "000", "-100", "1,5", "1.5", "12.34", "1.000,50", "1e3", "NaN", "Infinity", "9.007.199.254.740.992", "999999999999999999999999")) {
            assertNull(texto, validarMontoMovimiento(texto).valor)
            assertNotNull(texto, validarMontoMovimiento(texto).error)
        }
    }
}
