package cl.payroutine

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import cl.payroutine.datos.local.dao.PagoConCategoria
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.repositorio.CuotaProgramada
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import cl.payroutine.ui.pantallas.PantallaNuevoPago
import cl.payroutine.ui.pantallas.CuotaEditable
import cl.payroutine.ui.pantallas.PantallaCronogramaCuotas
import cl.payroutine.ui.pantallas.PantallaPagos
import cl.payroutine.ui.pantallas.fechaEnPantalla
import cl.payroutine.ui.theme.PayRoutineTheme
import cl.payroutine.viewmodel.EstadoCategorias
import cl.payroutine.viewmodel.EstadoPagos
import cl.payroutine.viewmodel.EstadoRegistroPago
import cl.payroutine.viewmodel.FechaLocal
import cl.payroutine.viewmodel.RegistroPagoViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegistroPagoPantallaTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun marcaPagadoDesdeLaTarjetaSinAbrirElDetalle() {
        val fecha = FechaLocal(LocalDate.of(2026, 11, 29), ZoneOffset.UTC)
        val pago = PagoPendiente(
            id = 7, descripcion = "Internet", monto = 12000L, esCuota = false, pagado = false,
            fechaVencimiento = Date.from(fecha.hoy.atStartOfDay(fecha.zona).toInstant()),
        )
        var abrioDetalle = false
        var pagados = emptyList<Int>()
        compose.setContent {
            PayRoutineTheme {
                PantallaPagos(
                    estado = EstadoPagos(cargando = false, pagos = listOf(PagoConCategoria(pago, "Servicios"))),
                    fecha = fecha, reintentar = {}, verCalendario = {}, verHistorial = {},
                    verPago = { abrioDetalle = true },
                    pagar = { ids, _, _ -> pagados = ids },
                )
            }
        }

        compose.onNodeWithText("Marcar pagado").performClick()
        compose.onNodeWithText("Confirmar pago").assertIsDisplayed()
        assertEquals(emptyList<Int>(), pagados)
        compose.onNodeWithText("Confirmar").performClick()
        assertEquals(listOf(7), pagados)
        assertEquals(false, abrioDetalle)
    }

    @Test
    fun seleccionaDosVencimientosYRevisaElTotalAntesDeConfirmar() {
        val fecha = FechaLocal(LocalDate.of(2026, 11, 29), ZoneOffset.UTC)
        val filas = listOf("Internet" to 12000L, "Agua" to 8000L, "Seguro" to 15000L).mapIndexed { indice, (nombre, monto) ->
            val dia = if (indice == 2) fecha.hoy.plusMonths(2) else fecha.hoy.plusDays(indice.toLong() + 3)
            PagoConCategoria(PagoPendiente(
                id = indice + 1, descripcion = nombre, monto = monto, esCuota = false, pagado = false,
                fechaVencimiento = Date.from(dia.atStartOfDay(fecha.zona).toInstant()),
            ), "Servicios")
        }
        var guardado: Pair<List<Int>, LocalDate>? = null
        compose.setContent {
            PayRoutineTheme {
                PantallaPagos(
                    estado = EstadoPagos(cargando = false, pagos = filas),
                    fecha = fecha, reintentar = {}, verCalendario = {}, verHistorial = {}, verPago = {},
                    pagar = { ids, dia, _ -> guardado = ids to dia },
                )
            }
        }

        val botonSeleccion = compose.onNodeWithText("Selección múltiple")
        val posicionSeleccion = botonSeleccion.getUnclippedBoundsInRoot().top
        botonSeleccion.performClick()
        val revisar = compose.onNodeWithText("Revisar pagos (0)")
        revisar.assertIsNotEnabled()
        assertEquals(posicionSeleccion, revisar.getUnclippedBoundsInRoot().top)
        compose.onNodeWithText("Cancelar").performClick()
        botonSeleccion.assertIsDisplayed().performClick()

        val lista = compose.onNode(hasScrollToIndexAction())
        lista.performScrollToNode(hasText("Diciembre 2026"))
        compose.onAllNodesWithText("Diciembre 2026").assertCountEquals(1)
        lista.performScrollToNode(hasText("Internet"))
        val internet = compose.onNodeWithText("Internet")
        internet.assertIsOff().performClick().assertIsOn()
        internet.performClick().assertIsOff()
        internet.performClick()
        lista.performScrollToNode(hasText("Enero 2027"))
        compose.onNodeWithText("Enero 2027").assertIsDisplayed()
        lista.performScrollToNode(hasText("Seguro"))
        compose.onNodeWithText("Seguro").performClick()
        compose.onNodeWithText("Revisar pagos (2)").performClick()
        compose.onNodeWithText("Confirmar pagos").assertIsDisplayed()
        compose.onNodeWithText("$27.000").assertIsDisplayed()
        compose.onNodeWithText(fechaEnPantalla(fecha.hoy)).assertIsDisplayed()
        assertEquals(null, guardado)

        compose.onNodeWithText("Confirmar").performClick()
        assertEquals(listOf(1, 3) to fecha.hoy, guardado)
    }

    @Test
    fun etiquetaDePagoRespetaLosLimitesDeVencimiento() {
        val fecha = FechaLocal(LocalDate.of(2026, 11, 29), ZoneOffset.UTC)
        var vencimiento by mutableStateOf(fecha.hoy)
        compose.setContent {
            val pago = PagoPendiente(
                id = 1,
                descripcion = "Internet",
                monto = 1000L,
                fechaVencimiento = Date.from(vencimiento.atStartOfDay(fecha.zona).toInstant()),
                esCuota = false,
                pagado = false,
            )
            PayRoutineTheme {
                PantallaPagos(
                    estado = EstadoPagos(cargando = false, pagos = listOf(PagoConCategoria(pago, "Servicios"))),
                    fecha = fecha,
                    reintentar = {},
                    verCalendario = {},
                    verHistorial = {},
                    verPago = {},
                )
            }
        }

        val casos = listOf(
            -1L to "Vencido",
            0L to "Vence hoy",
            1L to "Vence mañana",
            2L to "Vence en 2 días",
            7L to "Vence en 7 días",
            8L to "Vence en 2 semanas",
            14L to "Vence en 2 semanas",
            15L to "Pendiente",
        )
        for ((dias, etiqueta) in casos) {
            compose.runOnIdle { vencimiento = fecha.hoy.plusDays(dias) }
            compose.onNodeWithText(etiqueta).assertIsDisplayed()
        }
    }

    @Test
    fun laVistaPreviaPermiteEditarCuotasPeroExigeQueElTotalCuadre() {
        val primera = LocalDate.of(2027, 1, 31)
        var cuotas by mutableStateOf(listOf(CuotaEditable(primera, "1000"), CuotaEditable(primera.plusMonths(1), "1000")))
        var guardado: List<CuotaProgramada>? = null
        compose.setContent {
            PayRoutineTheme {
                PantallaCronogramaCuotas(
                    cuotas = cuotas, montoTotal = 2000, cuotasPagadas = 1,
                    categoriaDisponible = true, estado = EstadoRegistroPago(),
                    cambiarCuota = { i, cuota -> cuotas = cuotas.toMutableList().apply { this[i] = cuota } },
                    guardar = { guardado = it }, volver = {},
                )
            }
        }
        compose.onNodeWithText("Ya pagada").assertExists()
        compose.onNode(hasSetTextAction() and hasText("Monto de la cuota 1")).performTextReplacement("800")
        compose.onNodeWithText("Guardar deuda").assertIsNotEnabled()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        compose.onNode(hasSetTextAction() and hasText("Monto de la cuota 2")).performTextReplacement("1200")
        compose.onNodeWithText("Guardar deuda").performClick()
        assertEquals(listOf(CuotaProgramada(primera, 800), CuotaProgramada(primera.plusMonths(1), 1200)), guardado)
    }

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
                PayRoutineTheme { PantallaNuevoPago(estadoCategorias, {}, estadoRegistro, registro::guardar, registro::guardarDeuda) {} }
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
            compose.onNodeWithText("Sin recordatorio").performClick()
            compose.waitUntil(10000) { registro.estado.value.idGuardado != null }
            val fila = runBlocking { repositorio.observarPagosPendientes().first().single() }
            assertEquals("Internet", fila.pago.descripcion)
            assertEquals("Servicios", fila.nombreCategoria)
            assertEquals(24990L, fila.pago.monto)
            assertEquals(vencimiento, fila.pago.fechaVencimiento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        } finally {
            base.close()
        }
    }
}
