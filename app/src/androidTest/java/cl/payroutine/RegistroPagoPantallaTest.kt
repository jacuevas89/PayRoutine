package cl.payroutine

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import cl.payroutine.ui.pantallas.PantallaNuevoPago
import cl.payroutine.ui.pantallas.fechaEnPantalla
import cl.payroutine.ui.theme.PayRoutineTheme
import cl.payroutine.viewmodel.EstadoCategorias
import cl.payroutine.viewmodel.RegistroPagoViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegistroPagoPantallaTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    @OptIn(ExperimentalMaterial3Api::class)
    fun exigeUnaCategoriaDeGastoYUnVencimientoYGuardaUnaSolaVez() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        try {
            val repositorio = RepositorioFinanzas(base)
            runBlocking { repositorio.prepararCategoriasIniciales() }
            val registro = ViewModelProvider(compose.activity, viewModelFactory {
                initializer { RegistroPagoViewModel(contexto.applicationContext as Application, repositorio) }
            })[RegistroPagoViewModel::class.java]
            val categorias = repositorio.observarCategoriasConUso().map { EstadoCategorias(cargando = false, categorias = it) }
            compose.setContent {
                val estadoCategorias by categorias.collectAsState(EstadoCategorias())
                val estadoRegistro by registro.estado.collectAsState()
                PayRoutineTheme { PantallaNuevoPago(estadoCategorias, {}, estadoRegistro, registro::guardar) {} }
            }
            compose.waitUntil(10000) { compose.onAllNodesWithText("Servicios").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Sueldo").assertDoesNotExist()
            compose.onNodeWithText("Guardar pago pendiente").performClick()
            compose.onNodeWithText("Selecciona una categoría de gasto").assertExists()
            compose.onNodeWithText("Selecciona la fecha de vencimiento").assertExists()
            assertEquals(0, runBlocking { repositorio.observarPagos().first().size })

            compose.onNode(hasSetTextAction() and hasText("Nombre del pago")).performTextInput("Internet")
            compose.onNode(hasSetTextAction() and hasText("Monto")).performScrollTo().performTextInput("24.990")
            compose.onNodeWithContentDescription("Seleccionar fecha de vencimiento").performScrollTo().performClick()
            val hoy = LocalDate.now()
            val vencimiento = hoy.withDayOfMonth(hoy.lengthOfMonth())
            val diaUtc = vencimiento.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val descripcion = requireNotNull(DatePickerDefaults.dateFormatter().formatDate(
                diaUtc, compose.activity.resources.configuration.locales[0], forContentDescription = true,
            ))
            compose.onNode(hasText(descripcion, substring = true) and hasClickAction()).performClick()
            compose.onNodeWithText("Aceptar").performClick()
            compose.onNodeWithText(fechaEnPantalla(vencimiento)).assertExists()
            compose.onNodeWithText("Servicios").performScrollTo().performClick()

            val guardar = compose.onNodeWithText("Guardar pago pendiente").fetchSemanticsNode().config[SemanticsActions.OnClick].action!!
            compose.runOnIdle { guardar(); guardar() }
            compose.waitUntil(10000) { registro.estado.value.idGuardado != null }
            val fila = runBlocking { repositorio.observarPagosPendientes().first().single() }
            assertEquals("Internet", fila.pago.descripcion)
            assertEquals("Servicios", fila.nombreCategoria)
            assertEquals(24990.0, fila.pago.monto, 0.0)
            assertEquals(vencimiento, fila.pago.fechaVencimiento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        } finally {
            base.close()
        }
    }
}
