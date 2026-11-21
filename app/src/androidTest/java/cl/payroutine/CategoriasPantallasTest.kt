package cl.payroutine

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.local.entidad.Usuario
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import cl.payroutine.ui.navegacion.NavegacionPayRoutine
import cl.payroutine.ui.pantallas.PantallaNuevoMovimiento
import cl.payroutine.ui.theme.PayRoutineTheme
import cl.payroutine.viewmodel.EstadoCategorias
import cl.payroutine.viewmodel.EstadoPerfil
import cl.payroutine.viewmodel.FechaLocal
import cl.payroutine.viewmodel.calcularInicio
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoriasPantallasTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var base: BaseDatosPayRoutine
    private lateinit var repositorio: RepositorioFinanzas

    @Before
    fun preparar() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        repositorio = RepositorioFinanzas(base)
        repositorio.prepararCategoriasIniciales()
    }

    @After
    fun cerrar() {
        base.close()
    }

    @Test
    fun ajustesMuestraCategoriasPorTipoYActualizaContadoresDesdeRoom() {
        val estados = observarEstados()
        val usuario = Usuario("rf41-prueba", "María Pérez", "Mari")
        val inicio = calcularInicio(FechaLocal(LocalDate.of(2026, 11, 20), ZoneOffset.UTC), emptyList(), emptyList())
        compose.setContent {
            val estado by estados.collectAsState(EstadoCategorias())
            PayRoutineTheme {
                NavegacionPayRoutine(
                    EstadoPerfil(cargando = false, usuario = usuario), inicio, {},
                    categorias = estado, registrar = { _, _ -> },
                )
            }
        }
        compose.onNodeWithContentDescription("Ajustes").performClick()
        compose.onNodeWithText("Categorías").performClick()
        esperarTexto("Alimentación")
        compose.onAllNodesWithText("0 movimientos").assertCountEquals(10)
        compose.onNodeWithText("Sueldo").assertDoesNotExist()
        compose.onNodeWithText("Mascotas").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Vestuario").assertExists()
        compose.onNodeWithText("Educación").assertExists()

        val gasto = runBlocking {
            val alimentacion = repositorio.observarCategorias().first().single { it.nombre == "Alimentación" }
            val movimiento = Movimiento(tipo = TipoMovimiento.Gasto, monto = 5000.0, fecha = Date(0), categoriaId = alimentacion.id)
            val id = repositorio.insertar(movimiento).toInt()
            movimiento.copy(id = id)
        }
        esperarTexto("1 movimiento")
        compose.onAllNodesWithText("0 movimientos").assertCountEquals(9)
        runBlocking { repositorio.eliminar(gasto) }
        compose.waitUntil(10000) { compose.onAllNodesWithText("0 movimientos").fetchSemanticsNodes().size == 10 }

        compose.onNodeWithText("Ingresos").performScrollTo().performClick()
        compose.onAllNodesWithText("0 movimientos").assertCountEquals(6)
        compose.onNodeWithText("Sueldo").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Alimentación").assertDoesNotExist()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Mari").assertIsDisplayed()
    }

    @Test
    fun formularioFiltraCategoriasYConservaLaSeleccionPorTipoAlRestaurarse() {
        val estados = observarEstados()
        val restauracion = StateRestorationTester(compose)
        restauracion.setContent {
            val estado by estados.collectAsState(EstadoCategorias())
            PayRoutineTheme { PantallaNuevoMovimiento(estado, {}) {} }
        }
        esperarTexto("Alimentación")
        compose.onNodeWithText("Alimentación").assertIsSelected()
        compose.onNodeWithText("Mascotas").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Ingreso").performScrollTo().performClick()
        compose.onNodeWithText("Mascotas").assertDoesNotExist()
        compose.onNodeWithText("Sueldo").performScrollTo().performClick().assertIsSelected()
        restauracion.emulateSavedInstanceStateRestore()
        esperarTexto("Sueldo")
        compose.onNodeWithText("Sueldo").assertIsSelected()
        compose.onNodeWithText("Gasto").performScrollTo().performClick()
        compose.onNodeWithText("Mascotas").assertIsSelected()
        compose.onNodeWithText("Sueldo").assertDoesNotExist()
    }

    private fun observarEstados() = repositorio.observarCategoriasConUso().map {
        EstadoCategorias(cargando = false, categorias = it)
    }

    private fun esperarTexto(texto: String) = compose.waitUntil(10000) {
        compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
    }
}
