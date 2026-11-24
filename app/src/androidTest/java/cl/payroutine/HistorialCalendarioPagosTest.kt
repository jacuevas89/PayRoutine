package cl.payroutine

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.payroutine.datos.local.dao.PagoConCategoria
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.ui.pantallas.PantallaCalendario
import cl.payroutine.ui.pantallas.PantallaHistorialPagos
import cl.payroutine.ui.pantallas.fechaEnPantalla
import cl.payroutine.ui.pantallas.montoEnPantalla
import cl.payroutine.ui.theme.PayRoutineTheme
import cl.payroutine.viewmodel.EstadoPagos
import cl.payroutine.viewmodel.FechaLocal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistorialCalendarioPagosTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val fecha = FechaLocal(LocalDate.of(2027, 1, 2), ZoneOffset.UTC)
    private val diciembre = fecha.hoy.minusMonths(1)
    private val fechaAnticipada = diciembre.withDayOfMonth(29)
    private val venceAnticipado = fecha.hoy.plusDays(5)
    private val venceAtrasado = diciembre.withDayOfMonth(28)
    private val estado = EstadoPagos(cargando = false, pagos = listOf(
        pago(1, "Pago atrasado", 15000.0, venceAtrasado, fecha.hoy),
        pago(2, "Pago puntual", 5000.0, fecha.hoy, fecha.hoy),
        pago(3, "Pago pendiente", 9000.0, fecha.hoy),
        pago(4, "Pago anticipado", 12000.0, venceAnticipado, fechaAnticipada),
        pago(5, "Cuota histórica", 1000.0, venceAnticipado, respaldoHistorico = true),
    ))

    @Test
    fun calendarioDistingueFechaEfectivaYVencimientoInclusoAlPagarAntes() {
        var gastoAbierto: Int? = null
        var vencimientoAbierto: Int? = null
        compose.setContent {
            PayRoutineTheme {
                PantallaCalendario(estado, fecha, {}, { vencimientoAbierto = it }, { gastoAbierto = it }) {}
            }
        }

        compose.onNodeWithContentDescription(
            "${fechaEnPantalla(fecha.hoy)}, con vencimientos pendientes, con pagos realizados",
        ).assertIsSelected()
        compose.onNodeWithText("Pago pendiente").performScrollTo().performClick()
        assertEquals(3, vencimientoAbierto)
        compose.onNodeWithText("Vencía el ${fechaEnPantalla(venceAtrasado)}").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("Pago puntual").assertCountEquals(1)
        compose.onNodeWithText("Ver vencimientos ya pagados", substring = true).assertDoesNotExist()

        elegirDia(venceAnticipado)
        compose.onNodeWithText("Pago anticipado").assertDoesNotExist()
        compose.onNodeWithText("Ver vencimientos ya pagados (1)").performScrollTo().performClick()
        compose.onNodeWithText("Pagado el ${fechaEnPantalla(fechaAnticipada)}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Cuota histórica").assertDoesNotExist()

        compose.onNodeWithContentDescription("Mes anterior").performScrollTo().performClick()
        compose.onNodeWithText("Diciembre 2026").assertIsDisplayed()
        elegirDia(fechaAnticipada, ", con pagos realizados")
        compose.onNodeWithText("Pagos realizados este día").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Vencía el ${fechaEnPantalla(venceAnticipado)}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pago anticipado").performClick()
        assertEquals(104, gastoAbierto)
    }

    @Test
    fun historialFiltraPorMesYAnioDePagoYAbreElGastoVinculado() {
        var gastoAbierto: Int? = null
        compose.setContent {
            PayRoutineTheme { PantallaHistorialPagos(estado, fecha, {}, { gastoAbierto = it }) {} }
        }
        compose.onNodeWithText("Enero 2027").assertIsDisplayed()
        compose.onNodeWithText("2 pagos realizados").assertIsDisplayed()
        compose.onNodeWithText(montoEnPantalla(20000)).assertIsDisplayed()
        compose.onNodeWithText("Pago anticipado").assertDoesNotExist()
        compose.onNodeWithText("Cuota histórica").assertDoesNotExist()

        compose.onNodeWithContentDescription("Elegir mes y año").performClick()
        compose.onNodeWithContentDescription("Elegir año").performClick()
        compose.onNodeWithText("2026").performClick()
        compose.onNodeWithContentDescription("Elegir mes").performClick()
        compose.onNodeWithText("Diciembre").performScrollTo().performClick()
        compose.onNodeWithText("Aplicar").performClick()
        compose.onNodeWithText("Diciembre 2026").assertIsDisplayed()
        compose.onNodeWithText("1 pago realizado").assertIsDisplayed()
        compose.onAllNodesWithText(montoEnPantalla(12000)).assertCountEquals(2)
        compose.onNodeWithText("Pago atrasado").assertDoesNotExist()

        compose.onNodeWithContentDescription("Elegir mes y año").performClick()
        compose.onNodeWithContentDescription("Elegir mes").performClick()
        compose.onNodeWithText("Octubre").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Diciembre 2026").assertIsDisplayed()
        compose.onNodeWithText("Pago anticipado").performClick()
        assertEquals(104, gastoAbierto)
    }

    private fun elegirDia(dia: LocalDate, marcas: String = "") =
        compose.onNodeWithContentDescription(fechaEnPantalla(dia) + marcas).performScrollTo().performClick()

    private fun pago(
        id: Int,
        nombre: String,
        monto: Double,
        vence: LocalDate,
        pagado: LocalDate? = null,
        respaldoHistorico: Boolean = false,
    ): PagoConCategoria = PagoConCategoria(
        pago = PagoPendiente(
            id = id, descripcion = nombre, monto = monto, esCuota = respaldoHistorico,
            fechaVencimiento = aFecha(vence), pagado = pagado != null || respaldoHistorico,
            movimientoId = pagado?.let { 100 + id },
        ),
        nombreCategoria = "Servicios",
        fechaPago = pagado?.let(::aFecha),
    )

    private fun aFecha(dia: LocalDate): Date = Date.from(dia.atStartOfDay(fecha.zona).toInstant())
}
