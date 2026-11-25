package cl.payroutine

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import cl.payroutine.datos.local.dao.MovimientoConCategoria
import cl.payroutine.datos.local.dao.PagoConCategoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.local.entidad.Usuario
import cl.payroutine.ui.navegacion.NavegacionPayRoutine
import cl.payroutine.ui.pantallas.fechaEnPantalla
import cl.payroutine.ui.pantallas.montoEnPantalla
import cl.payroutine.ui.theme.PayRoutineTheme
import cl.payroutine.viewmodel.EstadoPerfil
import cl.payroutine.viewmodel.EstadoEdicionPerfil
import cl.payroutine.viewmodel.EstadoBorrado
import cl.payroutine.viewmodel.EstadoPagos
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
    fun avisoDePagoAbreElVencimientoCorrecto() {
        val estadoPagos = EstadoPagos(cargando = false, pagos = inicio.pagosPendientes.map { PagoConCategoria(it.pago, "Servicios") })
        compose.setContent {
            PayRoutineTheme {
                NavegacionPayRoutine(
                    estado = EstadoPerfil(cargando = false, usuario = usuario),
                    inicio = inicio,
                    reintentarCarga = {},
                    estadoPagos = estadoPagos,
                    usuarioDelAviso = usuario.id,
                    vencimientoDelAviso = estadoPagos.pagos.first().pago.id,
                    registrar = { _, _ -> },
                )
            }
        }
        ver("Detalle del pago")
        ver(estadoPagos.pagos.first().pago.descripcion)
    }

    @Test
    fun avisoMensualAbrePresupuesto() {
        compose.setContent {
            PayRoutineTheme {
                NavegacionPayRoutine(
                    estado = EstadoPerfil(cargando = false, usuario = usuario),
                    inicio = inicio,
                    reintentarCarga = {},
                    usuarioDelAviso = usuario.id,
                    avisoDePresupuesto = true,
                    registrar = { _, _ -> },
                )
            }
        }
        ver("Presupuesto mensual")
        ver("Definir presupuesto")
    }

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
    fun editarPerfilPrecargaValidaCancelaYActualizaAjustesEInicio() {
        val estado = mutableStateOf(EstadoPerfil(cargando = false, usuario = usuario))
        compose.setContent {
            PayRoutineTheme {
                NavegacionPayRoutine(
                    estado = estado.value,
                    inicio = inicio,
                    reintentarCarga = {},
                    prepararEdicion = { estado.value = estado.value.copy(edicion = EstadoEdicionPerfil.Disponible) },
                    guardarEdicion = { nombre, apodo ->
                        val actual = estado.value
                        estado.value = actual.copy(
                            usuario = requireNotNull(actual.usuario).copy(nombreCompleto = nombre.trim(), apodo = apodo.trim()),
                            edicion = EstadoEdicionPerfil.Guardada,
                        )
                    },
                    registrar = { _, _ -> },
                )
            }
        }
        compose.onNodeWithContentDescription("Ajustes").performClick()
        ver(usuario.nombreCompleto)
        tocar("Editar perfil")
        val campos = compose.onAllNodes(hasSetTextAction())
        campos[0].assertTextContains(usuario.nombreCompleto)
        campos[1].assertTextContains(usuario.apodo)
        campos[0].performTextReplacement("   ")
        campos[1].performTextReplacement("   ")
        tocar("Guardar cambios")
        compose.onAllNodesWithText("Este campo es obligatorio").assertCountEquals(2)
        campos[1].performTextReplacement("Cancelado")
        tocar("Cancelar")
        esperarAjustes()
        ver(usuario.apodo)
        ver(usuario.nombreCompleto)

        tocar("Editar perfil")
        campos[0].performTextReplacement("Sin guardar")
        atrasAndroid()
        esperarAjustes()
        ver(usuario.nombreCompleto)
        tocar("Editar perfil")
        campos[0].performTextReplacement("  Ángela O'Neill  ")
        campos[1].performTextReplacement("  Ángie  ")
        tocar("Guardar cambios")
        esperarAjustes()
        ver("Ángela O'Neill")
        ver("Ángie")
        ver("Á")
        volver()
        ver("Hola, Ángie")
    }

    private fun esperarAjustes() {
        compose.waitUntil(5_000) { compose.onNodeWithText("Ajustes").isDisplayed() }
    }

    @Test
    fun privacidadCancelaReintentaYReiniciaLaNavegacion() {
        val estado = mutableStateOf(EstadoPerfil(cargando = false, usuario = usuario))
        var intentos = 0
        compose.setContent {
            PayRoutineTheme {
                NavegacionPayRoutine(
                    estado = estado.value,
                    inicio = inicio,
                    reintentarCarga = {},
                    eliminarDatos = {
                        intentos++
                        estado.value = if (intentos == 1) estado.value.copy(borrado = EstadoBorrado.Error)
                        else EstadoPerfil(cargando = false)
                    },
                    limpiarErrorBorrado = { estado.value = estado.value.copy(borrado = EstadoBorrado.Disponible) },
                    registrar = { nombre, apodo ->
                        estado.value = EstadoPerfil(
                            cargando = false,
                            usuario = Usuario("48bb0e67-a52f-4468-b198-e47897f5c40d", nombre, apodo),
                        )
                    },
                )
            }
        }
        compose.onNodeWithContentDescription("Ajustes").performClick()
        tocar("Datos y privacidad")
        compose.onNodeWithText("Exportar datos").assertIsNotEnabled().assertHasNoClickAction()
        ver("Pendiente de definición")
        tocar("Información de privacidad")
        ver("Qué datos se guardan")
        volver()
        tocar("Eliminar todos los datos")
        ver("¿Eliminar todos los datos?")
        tocar("Cancelar")
        compose.onNodeWithText("¿Eliminar todos los datos?").assertDoesNotExist()
        org.junit.Assert.assertEquals(0, intentos)

        tocar("Eliminar todos los datos")
        val confirmar = compose.onNode(hasText("Eliminar todos los datos") and hasAnyAncestor(isDialog()))
        confirmar.performClick()
        ver("No se pudieron eliminar los datos. Puedes volver a intentarlo.")
        confirmar.performClick()
        ver("Comenzar")
        compose.onNodeWithText("Datos y privacidad").assertDoesNotExist()

        tocar("Comenzar")
        val campos = compose.onAllNodes(hasSetTextAction())
        campos[0].performTextInput("Nuevo perfil")
        campos[1].performTextInput("Nuevo")
        tocar("Guardar y continuar")
        ver("Hola, Nuevo")
        compose.onNodeWithContentDescription("Volver").assertDoesNotExist()
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
        ver("Ingresos")
        volver()
        tocar("Recordatorios")
        ver("Activar recordatorios")
        atrasAndroid()
        tocar("Consejos de ahorro")
        ver("Anota tus gastos hormiga")
        ver("Ahorra apenas recibas tu ingreso")
        volver()
        ver("Mari")
        volver()
        ver("Hola, Mari")

        tocar("Presupuesto")
        ver("Presupuesto mensual")
        ver("Sin presupuesto")
        tocar("Definir presupuesto")
        ver("Editar presupuesto")
        volver()
        ver("Presupuesto mensual")

        tocar("Inicio")
        tocar("Próximo pago: Tarjeta de crédito")
        ver("Detalle del pago")
        volver()
        tocar("Ver pagos")
        ver("Pagos pendientes")
        tocar("Calendario")
        ver("Noviembre 2026")
        val vencimiento = fechaEnPantalla(fecha.hoy.plusDays(4))
        compose.onNodeWithContentDescription("$vencimiento, con vencimientos pendientes").performClick()
        ver("Vencimientos pendientes")
        tocar("Tarjeta de crédito")
        ver("Detalle del pago")
        volver()
        ver("Vencimientos pendientes")
        atrasAndroid()
        tocar("Pagados")
        ver("Historial de pagos")
        tocar("Pendientes")
        ver("Pagos pendientes")
        tocar("Tarjeta de crédito")
        ver("Detalle del pago")
        ver("Servicios")
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
                NavegacionPayRoutine(
                    EstadoPerfil(cargando = false, usuario = usuario), inicio, {},
                    estadoPagos = EstadoPagos(
                        cargando = false,
                        pagos = inicio.pagosPendientes.map { PagoConCategoria(it.pago, "Servicios") },
                    ),
                ) { _, _ -> }
            }
        }
    }

    @Test
    fun avisoDeMovimientosAbreFormularioYSeConsumeUnaVez() {
        val aviso = mutableStateOf<String?>(usuario.id)
        compose.setContent {
            PayRoutineTheme {
                NavegacionPayRoutine(
                    EstadoPerfil(cargando = false, usuario = usuario), inicio, {},
                    usuarioDelAviso = aviso.value,
                    consumirAviso = { aviso.value = null },
                ) { _, _ -> }
            }
        }
        ver("Nuevo gasto")
        tocar("Ingreso")
        ver("Nuevo ingreso")
        atrasAndroid()
        ver("Hola, Mari")
    }

    @Test
    fun resumenPermiteElegirMesYAnioYReflejaCambiosSinSalir() {
        val datos = mutableStateOf(inicio)
        compose.setContent {
            PayRoutineTheme {
                NavegacionPayRoutine(EstadoPerfil(cargando = false, usuario = usuario), datos.value, {}) { _, _ -> }
            }
        }
        tocar("Resumen")
        ver("Noviembre 2026")
        ver("Alimentación")
        ver("Transporte")
        compose.onNodeWithContentDescription("Mes anterior").performClick()
        ver("Octubre 2026")
        ver("No hay movimientos registrados en este período.")
        compose.onAllNodesWithText(montoEnPantalla(0)).assertCountEquals(3)
        ver("No hay gastos registrados en este período.")

        compose.onNodeWithContentDescription("Elegir mes y año").performClick()
        compose.onNodeWithContentDescription("Elegir año").performClick()
        tocar("2025")
        compose.onNodeWithContentDescription("Elegir mes").performClick()
        compose.onNodeWithText("Noviembre").performScrollTo().performClick()
        tocar("Aplicar")
        ver("Noviembre 2025")
        ver("No hay movimientos registrados en este período.")

        compose.onNodeWithContentDescription("Elegir mes y año").performClick()
        compose.onNodeWithContentDescription("Elegir año").performClick()
        tocar("2026")
        tocar("Aplicar")
        ver("Noviembre 2026")
        ver("Alimentación")
        compose.runOnIdle {
            datos.value = calcularInicio(inicio.fecha, emptyList(), emptyList())
        }
        ver("Noviembre 2026")
        ver("No hay movimientos registrados en este período.")
        atrasAndroid()
        ver("Hola, Mari")
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
