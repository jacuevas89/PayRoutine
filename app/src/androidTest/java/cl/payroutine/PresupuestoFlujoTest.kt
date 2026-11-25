package cl.payroutine

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.repositorio.CuotaProgramada
import cl.payroutine.datos.repositorio.NuevaDeudaEnCuotas
import cl.payroutine.datos.repositorio.NuevoMovimiento
import cl.payroutine.datos.repositorio.NuevoPagoUnico
import cl.payroutine.ui.pantallas.mesEnPantalla
import cl.payroutine.ui.pantallas.montoEnPantalla
import cl.payroutine.ui.pantallas.nombreMesEnPantalla
import cl.payroutine.ui.pantallas.porcentajeEnPantalla
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PresupuestoFlujoTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun consumoDePagosYMovimientosRespetaCategoriaYPeriodo() {
        val app = compose.activity.application as PayRoutineApplication
        val repo = app.repositorio
        val hoy = LocalDate.now()
        val mes = YearMonth.from(hoy)
        val anterior = mes.minusMonths(1)
        val (idGasto, categoriaPresupuestada) = runBlocking {
            app.recordatorios.eliminarDatosLocales()
            repo.registrarUsuario("Perfil de prueba", "Prueba")
            val usuario = requireNotNull(repo.observarUsuario().first())
            val categorias = repo.observarCategorias().first().filter { it.tipo == TipoMovimiento.Gasto }
            val conLimite = categorias.first().id
            val sinLimite = categorias[1].id
            repo.guardarPresupuestoMensual(usuario.id, mes, null, mapOf(conLimite to 4000L))
            repo.guardarPresupuestoMensual(usuario.id, anterior, 1000L, emptyMap())
            val gasto = repo.registrarMovimiento(NuevoMovimiento("Gasto presupuestado", TipoMovimiento.Gasto, 1000, hoy, conLimite))
            repo.registrarMovimiento(NuevoMovimiento("Gasto fuera del límite", TipoMovimiento.Gasto, 2000, hoy, sinLimite))
            repo.registrarMovimiento(NuevoMovimiento("Gasto anterior", TipoMovimiento.Gasto, 1500, anterior.atDay(1), sinLimite))
            val pago = repo.registrarPagoUnico(NuevoPagoUnico("Cuenta de agua", 2000, hoy, conLimite)).toInt()
            val primeraCuota = repo.registrarDeuda(
                NuevaDeudaEnCuotas("Crédito", 2000, conLimite, 0, listOf(
                    CuotaProgramada(hoy, 1000), CuotaProgramada(hoy.plusDays(1), 1000),
                )),
            ).toInt()
            val cuotas = repo.observarPagos().first().filter { it.id >= primeraCuota }.map { it.id }
            repo.pagarVencimientos(listOf(pago), hoy, "")
            repo.pagarVencimientos(cuotas, hoy, "")
            repo.pagarVencimientos(cuotas, hoy, "")
            gasto.toInt() to conLimite
        }
        try {
            esperar("Hola, Prueba")
            compose.onNodeWithText("Presupuesto").performClick()
            esperar("Gastos sin presupuesto")
            esperar("Total: ${montoEnPantalla(2000)}")
            esperar("${porcentajeEnPantalla(1.25)} usado")
            runBlocking {
                repo.editarMovimiento(idGasto, NuevoMovimiento("Gasto presupuestado", TipoMovimiento.Gasto, 2000, hoy, categoriaPresupuestada))
            }
            esperar("${porcentajeEnPantalla(1.5)} usado")
            runBlocking { repo.eliminarMovimiento(idGasto) }
            esperar("${porcentajeEnPantalla(1)} usado")
            runBlocking {
                repo.registrarMovimiento(NuevoMovimiento("Gasto presupuestado", TipoMovimiento.Gasto, 1000, hoy, categoriaPresupuestada))
            }
            esperar("${porcentajeEnPantalla(1.25)} usado")
            tocarEnCuerpo("Ver exceso")
            esperar("Superaste tu presupuesto")
            esperar("Cuenta de agua")
            esperar("Crédito · Cuota 2")
            compose.onNodeWithText("Gasto fuera del límite").assertDoesNotExist()
            compose.onNodeWithContentDescription("Volver").performClick()

            tocarEnCuerpo(mesEnPantalla(mes.atDay(1)))
            if (anterior.year != mes.year) {
                compose.onNodeWithContentDescription("Elegir año").performClick()
                compose.onNodeWithText(anterior.year.toString()).performClick()
            }
            compose.onNodeWithContentDescription("Elegir mes").performClick()
            compose.onNodeWithText(nombreMesEnPantalla(anterior.month)).performScrollTo().performClick()
            compose.onNodeWithText("Aplicar").performClick()
            esperar("${porcentajeEnPantalla(1.5)} usado")
            tocarEnCuerpo("Ver exceso")
            esperar("Gasto anterior")
            tocarEnCuerpo("Ajustar presupuesto")
            compose.onNode(hasText("Límite mensual") and hasSetTextAction()).performTextReplacement("1500")
            tocarEnCuerpo("Guardar presupuesto")
            esperar("${porcentajeEnPantalla(1)} usado")
            compose.onNodeWithText("Superaste tu presupuesto").assertDoesNotExist()
            compose.onNodeWithText("Gasto anterior").assertExists()
        } finally {
            runBlocking { app.recordatorios.eliminarDatosLocales() }
        }
    }

    @Test
    fun primerUsoPermiteVolverEditarYRevisarCopiaAntesDeGuardar() {
        val app = compose.activity.application as PayRoutineApplication
        runBlocking { app.recordatorios.eliminarDatosLocales() }
        try {
            esperar("Comenzar")
            compose.onNodeWithText("Comenzar").performClick()
            compose.onAllNodes(hasSetTextAction())[0].performTextInput("Perfil de prueba")
            compose.onAllNodes(hasSetTextAction())[1].performTextInput("Prueba")
            tocarEnCuerpo("Guardar y continuar")
            esperar("Configura tu presupuesto")
            tocarEnCuerpo("Continuar sin presupuesto")
            esperar("Definir un presupuesto te ayudará a ordenar tus finanzas. Puedes configurarlo después desde Presupuesto")
            compose.onNodeWithText("Definir presupuesto").performClick()
            tocarEnCuerpo("Presupuesto general")
            compose.onNode(hasText("Límite mensual") and hasSetTextAction()).performTextInput("50000")
            tocarEnCuerpo("Guardar presupuesto")
            esperar("Hola, Prueba")

            compose.onNodeWithText("Presupuesto").performClick()
            esperar("Presupuesto general")
            compose.onNodeWithText("Editar").performClick()
            tocarEnCuerpo("Por categorías")
            compose.onNodeWithText("Distribuye el límite", substring = true).assertExists()
            tocarEnCuerpo("Cancelar")
            esperar("Presupuesto general")

            val actual = YearMonth.now()
            val siguiente = actual.plusMonths(1)
            tocarEnCuerpo(mesEnPantalla(actual.atDay(1)))
            if (siguiente.year != actual.year) {
                compose.onNodeWithContentDescription("Elegir año").performClick()
                compose.onNodeWithText(siguiente.year.toString()).performClick()
            }
            compose.onNodeWithContentDescription("Elegir mes").performClick()
            compose.onNodeWithText(nombreMesEnPantalla(siguiente.month)).performScrollTo().performClick()
            compose.onNodeWithText("Aplicar").performClick()
            esperar("Sin presupuesto")
            tocarEnCuerpo("Copiar mes anterior")
            esperar("Revisa los límites del mes anterior antes de guardar.")
            compose.onNode(hasText("Límite mensual") and hasSetTextAction()).performTextReplacement("60000")
            tocarEnCuerpo("Guardar presupuesto")
            esperar("Presupuesto general")
            runBlocking {
                val repo = app.repositorio
                check(repo.existePresupuesto(actual))
                check(repo.existePresupuesto(siguiente))
            }
        } finally {
            runBlocking { app.recordatorios.eliminarDatosLocales() }
        }
    }

    private fun esperar(texto: String) = compose.waitUntil(timeoutMillis = 15_000) {
        compose.onAllNodesWithText(texto, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun tocarEnCuerpo(texto: String) {
        compose.onNodeWithText(texto).performScrollTo().performClick()
    }
}
