package cl.payroutine

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.payroutine.datos.local.entidad.Usuario
import cl.payroutine.ui.navegacion.NavegacionPayRoutine
import cl.payroutine.ui.theme.PayRoutineTheme
import cl.payroutine.viewmodel.EstadoPerfil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavegacionPayRoutineTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun primerUsoVuelveABienvenidaYPerfilGuardadoAbreInicio() {
        val estado = mutableStateOf(EstadoPerfil(cargando = false))
        compose.setContent {
            PayRoutineTheme {
                NavegacionPayRoutine(estado.value, {}) { nombre, apodo ->
                    estado.value = estado.value.copy(usuario = usuario.copy(nombreCompleto = nombre, apodo = apodo))
                }
            }
        }

        tocar("Comenzar")
        ver("Tu perfil")
        atrasAndroid()
        ver("Comenzar")
        tocar("Comenzar")
        volver()
        ver("Comenzar")
        tocar("Comenzar")
        val campos = compose.onAllNodes(hasSetTextAction())
        campos[0].performTextInput("María Pérez")
        campos[1].performTextInput("Mari")
        tocar("Guardar y continuar")
        ver("Hola, Mari")
        compose.onNodeWithText("Comenzar").assertDoesNotExist()
    }

    @Test
    fun formularioYDetallesRegresanAlOrigenTrasToquesRepetidos() {
        iniciarConPerfil()
        ver("Hola, Mari")
        abrirMovimiento("Últimos movimientos")
        tocar("Supermercado")
        ver("Detalle")
        atrasAndroid()
        ver("Últimos movimientos")

        tocar("Ver todos")
        ver("Todos")
        tocar("Movimientos")
        abrirMovimiento("Todos")
        tocar("Transporte")
        ver("Detalle")
        volver()
        ver("Todos")
        atrasAndroid()
        ver("Hola, Mari")
    }

    @Test
    fun ajustesPresupuestoPagosYResumenConservanSuRutaDeRegreso() {
        iniciarConPerfil()
        compose.onNodeWithContentDescription("Recordatorios").performClick()
        ver("Activar recordatorios")
        atrasAndroid()
        compose.onNodeWithContentDescription("Ajustes").performClick()
        tocar("Categorías")
        ver("Nueva categoría")
        volver()
        tocar("Recordatorios")
        ver("Activar recordatorios")
        atrasAndroid()
        tocar("Consejos de ahorro")
        ver("Anota tus gastos hormiga")
        volver()
        ver("Mari")
        volver()
        ver("Hola, Mari")

        tocar("Presupuesto")
        tocar("Ocio")
        ver("Superaste tu presupuesto")
        tocar("Ajustar presupuesto")
        ver("Editar presupuesto")
        atrasAndroid()
        ver("Superaste tu presupuesto")
        volver()
        ver("Presupuesto mensual")
        tocar("Editar")
        ver("Editar presupuesto")
        volver()
        ver("Presupuesto mensual")

        tocar("Inicio")
        tocar("Próximo pago: Tarjeta de crédito")
        ver("Pagos pendientes")
        tocar("Calendario")
        ver("Vence el 2 de noviembre")
        atrasAndroid()
        tocar("Pagados")
        ver("Historial de pagos")
        tocar("Pendientes")
        ver("Pagos pendientes")
        tocar("Cuota notebook (4 de 12)")
        ver("Calendario de cuotas")
        volver()
        compose.onNodeWithContentDescription("Nuevo pago pendiente").performClick()
        ver("Guardar pago pendiente")
        atrasAndroid()
        ver("Pagos pendientes")

        tocar("Resumen")
        ver("Resumen mensual")
        tocar("Resumen")
        atrasAndroid()
        ver("Hola, Mari")
    }

    private fun iniciarConPerfil() {
        compose.setContent {
            PayRoutineTheme {
                NavegacionPayRoutine(EstadoPerfil(cargando = false, usuario = usuario), {}) { _, _ -> }
            }
        }
    }

    private fun abrirMovimiento(origen: String) {
        tocarDosVeces(compose.onNodeWithContentDescription("Nuevo movimiento"))
        ver("Nuevo gasto")
        tocar("Ingreso")
        tocar("Gasto")
        tocar("Ingreso")
        ver("Nuevo ingreso")
        tocarDosVeces(compose.onNodeWithContentDescription("Volver"))
        ver(origen)
        compose.onNodeWithContentDescription("Nuevo movimiento").performClick()
        ver("Nuevo gasto")
        tocar("Ingreso")
        atrasAndroid()
        ver(origen)
    }

    private fun tocarDosVeces(nodo: SemanticsNodeInteraction) {
        val accion = nodo.fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnIdle {
            accion()
            accion()
        }
    }

    private fun tocar(texto: String) =
        compose.onNode(hasText(texto) and hasClickAction()).performClick()

    private fun ver(texto: String) = compose.onNodeWithText(texto).assertIsDisplayed()
    private fun volver() = compose.onNodeWithContentDescription("Volver").performClick()
    private fun atrasAndroid() = compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

    private val usuario = Usuario("6f828433-fa9c-4ed4-8cf3-cfc95293d2e7", "María Pérez", "Mari")
}
