package cl.payroutine

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.payroutine.datos.local.dao.MovimientoConCategoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.local.entidad.Usuario
import cl.payroutine.ui.navegacion.NavegacionPayRoutine
import cl.payroutine.ui.pantallas.montoEnPantalla
import cl.payroutine.ui.theme.PayRoutineTheme
import cl.payroutine.viewmodel.EstadoPerfil
import cl.payroutine.viewmodel.FechaLocal
import cl.payroutine.viewmodel.calcularInicio
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
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
                NavegacionPayRoutine(estado.value, null, {}) { nombre, apodo ->
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
        tocar("Alimentación ·", parcial = true)
        ver("Detalle")
        ver("Compra de prueba")
        atrasAndroid()
        ver("Últimos movimientos")

        tocar("Ver todos")
        ver("Todos")
        tocar("Movimientos")
        abrirMovimiento("Todos")
        tocar("Transporte ·", parcial = true)
        ver("Detalle")
        ver("Pasaje de prueba")
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
                NavegacionPayRoutine(EstadoPerfil(cargando = false, usuario = usuario), inicio, {}) { _, _ -> }
            }
        }
    }

    @Test
    fun inicioReflejaCambiosYElDetalleRespetaElIdAunqueSeReordeneLaLista() {
        val datos = mutableStateOf(calcularInicio(fecha, emptyList(), emptyList()))
        compose.setContent {
            PayRoutineTheme {
                NavegacionPayRoutine(EstadoPerfil(cargando = false, usuario = usuario), datos.value, {}) { _, _ -> }
            }
        }
        ver("Noviembre 2026")
        ver("Todavía no tienes movimientos registrados.")
        ver("No tienes pagos vencidos ni por vencer este mes.")
        compose.onAllNodesWithText(montoEnPantalla(0)).assertCountEquals(3)

        compose.runOnIdle { datos.value = inicio }
        ver("Próximo pago: Tarjeta de crédito")
        ver("Vence en 4 días · ${montoEnPantalla(120000)}")
        tocar("Alimentación ·", parcial = true)
        ver("Compra de prueba")
        compose.runOnIdle {
            datos.value = inicio.copy(movimientos = inicio.movimientos.reversed())
        }
        ver("Compra de prueba")
        compose.runOnIdle {
            datos.value = inicio.copy(movimientos = inicio.movimientos.filter { it.movimiento.id != 57 })
        }
        ver("El movimiento ya no está disponible.")
        volver()
        ver("Últimos movimientos")
        ver("Gasto")
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

    private fun tocar(texto: String, parcial: Boolean = false) =
        compose.onNode(hasText(texto, substring = parcial) and hasClickAction()).performClick()

    private fun ver(texto: String) = compose.onNodeWithText(texto).assertIsDisplayed()
    private fun volver() = compose.onNodeWithContentDescription("Volver").performClick()
    private fun atrasAndroid() = compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }

    private val usuario = Usuario("6f828433-fa9c-4ed4-8cf3-cfc95293d2e7", "María Pérez", "Mari")
    private val fecha = FechaLocal(LocalDate.of(2026, 11, 19), ZoneOffset.UTC)
    private val inicio = calcularInicio(
        fecha,
        listOf(
            MovimientoConCategoria(
                Movimiento(57, TipoMovimiento.Gasto, 124500.0, dia(19), 1, "Compra de prueba"),
                "Alimentación",
            ),
            MovimientoConCategoria(
                Movimiento(23, TipoMovimiento.Gasto, 18000.0, dia(18), 2, "Pasaje de prueba"),
                "Transporte",
            ),
        ),
        listOf(PagoPendiente(9, "Tarjeta de crédito", 120000.0, dia(23), false, pagado = false)),
    )

    private fun dia(dia: Int) = Date.from(fecha.hoy.withDayOfMonth(dia).atStartOfDay(fecha.zona).toInstant())
}
