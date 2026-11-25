package cl.payroutine

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import cl.payroutine.datos.local.repartirPesos
import cl.payroutine.ui.pantallas.SeparadoresPesos
import cl.payroutine.ui.pantallas.normalizarMontoEditado
import org.junit.Assert.assertEquals
import org.junit.Test

class PesosEnterosTest {
    @Test
    fun distribuyeElResiduoSinCambiarElTotalRedondeado() {
        assertEquals(listOf(101L, 200L), repartirPesos(listOf("100.40".toBigDecimal(), "200.40".toBigDecimal())))
        assertEquals(listOf(0L, 1L, 1L), repartirPesos(listOf("0.1".toBigDecimal(), "0.8".toBigDecimal(), "0.9".toBigDecimal())))
    }

    @Test
    fun agrupaMilesYConservaElCursorAlPegar() {
        val pegado = normalizarMontoEditado(TextFieldValue("12.345", TextRange(4)))
        assertEquals("12345", pegado.text)
        assertEquals(TextRange(3), pegado.selection)
        val visible = SeparadoresPesos.filter(AnnotatedString("12345"))
        assertEquals("12.345", visible.text.text)
        assertEquals(4, visible.offsetMapping.originalToTransformed(3))
        assertEquals(2, visible.offsetMapping.transformedToOriginal(2))
        assertEquals(2, visible.offsetMapping.transformedToOriginal(3))
    }
}
