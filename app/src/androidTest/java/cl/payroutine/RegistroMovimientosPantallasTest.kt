package cl.payroutine

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import cl.payroutine.ui.pantallas.PantallaNuevoMovimiento
import cl.payroutine.ui.pantallas.fechaEnPantalla
import cl.payroutine.ui.theme.PayRoutineTheme
import cl.payroutine.viewmodel.EstadoCategorias
import cl.payroutine.viewmodel.RegistroMovimientoViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegistroMovimientosPantallasTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var base: BaseDatosPayRoutine
    private lateinit var repositorio: RepositorioFinanzas
    private lateinit var registro: RegistroMovimientoViewModel
    private val almacen = ViewModelStore()

    @Before
    fun preparar() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        repositorio = RepositorioFinanzas(base)
        repositorio.prepararCategoriasIniciales()
        registro = ViewModelProvider(almacen, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                RegistroMovimientoViewModel(contexto.applicationContext as Application, repositorio) as T
        })[RegistroMovimientoViewModel::class.java]
    }

    @After
    fun cerrar() {
        almacen.clear()
        base.close()
    }

    private fun iniciarFormulario(restauracion: StateRestorationTester = StateRestorationTester(compose)): StateRestorationTester {
        val categorias = repositorio.observarCategoriasConUso().map { EstadoCategorias(cargando = false, categorias = it) }
        restauracion.setContent {
            val estadoCategorias by categorias.collectAsState(EstadoCategorias())
            val estadoRegistro by registro.estado.collectAsState()
            PayRoutineTheme { PantallaNuevoMovimiento(estadoCategorias, {}, estadoRegistro, registro::guardar) {} }
        }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Alimentación").fetchSemanticsNodes().isNotEmpty() }
        return restauracion
    }

    @Test
    fun validaCamposYRegistraUnSoloIngresoAnteDobleToque() {
        iniciarFormulario()
        compose.onNodeWithText("Guardar gasto").performClick()
        compose.onNodeWithText("Ingresa el nombre del movimiento").assertExists()
        compose.onNodeWithText("Ingresa el monto").assertExists()
        assertTrue(runBlocking { repositorio.observarMovimientos().first().isEmpty() })

        campo("Nombre del movimiento").performTextInput("Sueldo noviembre")
        campo("Monto").performScrollTo().performTextInput("0")
        compose.onNodeWithText("Guardar gasto").performClick()
        compose.onNodeWithText("El monto debe ser mayor que cero").assertExists()
        campo("Monto").performTextReplacement("500.000")
        compose.onNodeWithText("Ingreso").performScrollTo().performClick()
        compose.onNodeWithText("Sueldo").performScrollTo().performClick()
        val accion = compose.onNodeWithText("Guardar ingreso").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
        compose.runOnIdle { accion(); accion() }
        compose.waitUntil(10000) { registro.estado.value.idGuardado != null }
        val filas = runBlocking { repositorio.observarMovimientos().first() }
        assertEquals(1, filas.size)
        assertEquals("Sueldo noviembre", filas.single().nombre)
        assertEquals(500000.0, filas.single().monto, 0.0)
        assertNull(filas.single().nota)
        compose.onNodeWithText("Guardar ingreso").assertIsNotEnabled()
    }

    @Test
    @OptIn(ExperimentalMaterial3Api::class)
    fun fechaParteEnHoyYElCalendarioGuardaElDiaSeleccionadoSinDesfase() {
        iniciarFormulario()
        val hoy = LocalDate.now()
        compose.onNodeWithText(fechaEnPantalla(hoy)).assertExists()
        campo("Nombre del movimiento").performTextInput("Compra anterior")
        campo("Monto").performScrollTo().performTextInput("1000")
        compose.onNodeWithContentDescription("Seleccionar fecha").performScrollTo().performClick()
        val primerDia = hoy.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val descripcion = requireNotNull(DatePickerDefaults.dateFormatter().formatDate(
            primerDia, compose.activity.resources.configuration.locales[0], forContentDescription = true,
        ))
        compose.onNode(hasText(descripcion, substring = true) and hasClickAction()).performClick()
        compose.onNodeWithText("Aceptar").performClick()
        compose.onNodeWithText(fechaEnPantalla(hoy.withDayOfMonth(1))).assertExists()
        compose.onNodeWithText("Guardar gasto").performClick()
        compose.waitUntil(10000) { registro.estado.value.idGuardado != null }
        val fecha = runBlocking { repositorio.observarMovimientos().first().single().fecha }
        assertEquals(hoy.withDayOfMonth(1), fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
    }

    @Test
    fun conservaBorradorAlRestaurarYPermiteReintentarTrasFalloDeBaseDeDatos() {
        val restauracion = iniciarFormulario()
        campo("Nombre del movimiento").performTextInput("Compra semanal")
        campo("Monto").performScrollTo().performTextInput("15000")
        compose.onNodeWithText("Mascotas").performScrollTo().performClick()
        campo("Nota (opcional)").performScrollTo().performTextInput("Alimento")
        restauracion.emulateSavedInstanceStateRestore()
        campo("Nombre del movimiento").assertTextContains("Compra semanal")
        campo("Monto").assertTextContains("15000")
        campo("Nota (opcional)").assertTextContains("Alimento")
        compose.onNodeWithText("Mascotas").assertIsSelected()

        base.openHelper.writableDatabase.execSQL("CREATE TRIGGER fallar_registro BEFORE INSERT ON Movimiento BEGIN SELECT RAISE(ABORT, 'Fallo simulado'); END")
        compose.onNodeWithText("Guardar gasto").performClick()
        compose.waitUntil(10000) { registro.estado.value.error != null }
        compose.onNodeWithText("No se pudo guardar el movimiento. Vuelve a intentarlo.").assertIsDisplayed()
        assertTrue(runBlocking { repositorio.observarMovimientos().first().isEmpty() })
        campo("Nombre del movimiento").assertTextContains("Compra semanal")
        base.openHelper.writableDatabase.execSQL("DROP TRIGGER fallar_registro")
        compose.onNodeWithText("Guardar gasto").performClick()
        compose.waitUntil(10000) { registro.estado.value.idGuardado != null }
        val fila = runBlocking { repositorio.observarMovimientos().first().single() }
        assertEquals("Alimento", fila.nota)
        val categoria = runBlocking { repositorio.observarCategorias().first().single { it.id == fila.categoriaId } }
        assertEquals("Mascotas", categoria.nombre)
    }

    private fun campo(nombre: String) = compose.onNode(hasSetTextAction() and hasText(nombre))
}
