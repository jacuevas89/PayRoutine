package cl.payroutine

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.payroutine.ui.pantallas.mesEnPantalla
import cl.payroutine.ui.pantallas.nombreMesEnPantalla
import java.time.YearMonth
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PresupuestoFlujoTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

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
            if (siguiente.year != actual.year) compose.onNodeWithContentDescription("Año siguiente").performClick()
            compose.onNodeWithText(nombreMesEnPantalla(siguiente.month)).performClick()
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
