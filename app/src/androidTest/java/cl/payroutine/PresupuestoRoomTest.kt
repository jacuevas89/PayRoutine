package cl.payroutine

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.entidad.ModalidadPresupuesto
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.local.entidad.fechaAviso
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PresupuestoRoomTest {
    private val zona = ZoneOffset.UTC
    private val inicio = YearMonth.of(2026, 11)

    @Test
    fun configuracionEdicionCopiaYLimitesIndependientes() = conPerfil { repo, usuario, gasto, otro, _ ->
        assertTrue(requireNotNull(repo.preferenciasPresupuesto()).configuracionPendiente)
        repo.guardarPresupuestoMensual(usuario, inicio, 50000L, emptyMap())
        assertFalse(requireNotNull(repo.preferenciasPresupuesto()).configuracionPendiente)
        assertEquals(ModalidadPresupuesto.General, repo.observarPresupuestosMensuales().first().single().cabecera.modalidad)

        repo.guardarPresupuestoMensual(usuario, inicio, null, mapOf(gasto to 30000L, otro to 20000L))
        val mesCopiado = inicio.plusMonths(1)
        val limites = repo.observarPresupuestosMensuales().first().associate {
            requireNotNull(it.categoriaId) to requireNotNull(it.limite)
        }
        repo.guardarPresupuestoMensual(usuario, mesCopiado, null, limites + (gasto to 40000L))
        repo.guardarPresupuestoMensual(usuario, mesCopiado, null, mapOf(gasto to 40000L))
        val guardados = repo.observarPresupuestosMensuales().first()
        assertEquals(2, guardados.count { it.cabecera.mes == inicio.monthValue })
        assertEquals(40000L, guardados.single { it.cabecera.mes == mesCopiado.monthValue }.limite!!)
        assertEquals(3, repo.observarPresupuestos().first().size)

        repo.guardarPresupuestoMensual(usuario, mesCopiado, 40000L, emptyMap())
        assertEquals(2, repo.observarPresupuestos().first().size)
        repo.eliminarTodosLosDatos()
        assertNull(repo.preferenciasPresupuesto())
        assertTrue(repo.observarPresupuestosMensuales().first().isEmpty())
    }

    @Test
    fun automaticoUsaMesesDisponiblesYRespetaEdiciones() = conPerfil { repo, usuario, gasto, otro, ingreso ->
        val anterior = inicio.minusMonths(1)
        repo.movimiento(anterior, gasto, 900L)
        repo.configurarAutomatico(usuario, true, false, inicio, zona)
        repo.generarPresupuestoAutomatico(inicio, zona)
        assertFalse(repo.existePresupuesto(inicio))

        val diciembre = inicio.plusMonths(1)
        val enero = inicio.plusMonths(2)
        repo.movimiento(diciembre, gasto, 300L)
        repo.movimiento(diciembre, otro, 120L)
        repo.movimiento(diciembre, ingreso, 90L, TipoMovimiento.Ingreso)
        repo.generarPresupuestoAutomatico(diciembre, zona)
        assertFalse(repo.existePresupuesto(diciembre))
        repo.generarPresupuestoAutomatico(enero, zona)
        val primerResultado = repo.observarPresupuestosMensuales().first()
        assertTrue(primerResultado.all { it.cabecera.generado })
        assertEquals(listOf(120L, 300L), primerResultado.map { requireNotNull(it.limite) }.sorted())
        assertEquals(90L, primerResultado.first().cabecera.ingresoReferencia!!)
        repo.guardarPresupuestoMensual(usuario, enero, 111L, emptyMap())
        repo.generarPresupuestoAutomatico(enero, zona)
        assertEquals(111L, repo.observarPresupuestosMensuales().first().single().cabecera.limiteGeneral!!)

        val febrero = inicio.plusMonths(3)
        val marzo = inicio.plusMonths(4)
        repo.movimiento(febrero, ingreso, 210L, TipoMovimiento.Ingreso)
        repo.generarPresupuestoAutomatico(marzo, zona)
        val promedio = repo.observarPresupuestosMensuales().first().filter { it.cabecera.mes == marzo.monthValue }
        assertEquals(listOf(60L, 150L), promedio.map { requireNotNull(it.limite) }.sorted())
        assertEquals(150L, promedio.first().cabecera.ingresoReferencia!!)
        assertFalse(repo.existePresupuesto(febrero))
        repo.configurarAutomatico(usuario, false, false, marzo, zona)
        assertNull(requireNotNull(repo.preferenciasPresupuesto()).primerMesAutomatico)
        assertTrue(repo.existePresupuesto(marzo))
    }

    @Test
    fun confirmacionExplicitaYDiaTreintaYUno() = conPerfil { repo, usuario, gasto, _, ingreso ->
        val anterior = inicio.minusMonths(1)
        repo.movimiento(anterior, ingreso, 1000L, TipoMovimiento.Ingreso)
        repo.configurarAutomatico(usuario, true, true, inicio, zona)
        assertFalse(repo.existePresupuesto(inicio))
        repo.movimiento(anterior, gasto, 500L)
        repo.configurarAutomatico(usuario, true, true, inicio, zona)
        assertEquals(500L, repo.observarPresupuestosMensuales().first().single().limite!!)
        repo.movimiento(anterior, gasto, 400L)
        repo.configurarAutomatico(usuario, true, true, inicio, zona)
        assertEquals(500L, repo.observarPresupuestosMensuales().first().single().limite!!)
        val configuracion = requireNotNull(repo.preferenciasPresupuesto()).copy(diaAviso = 31)
        val ahora = inicio.atDay(1).atStartOfDay(zona)
        assertEquals(30, configuracion.fechaAviso(inicio, ahora).dayOfMonth)
        assertEquals(31, configuracion.fechaAviso(inicio.plusMonths(1), ahora).dayOfMonth)
    }

    private fun conPerfil(
        probar: suspend (RepositorioFinanzas, String, Int, Int, Int) -> Unit,
    ) = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        try {
            val repo = RepositorioFinanzas(base)
            repo.registrarUsuario("Nombre de prueba", "Prueba")
            repo.crearCategoriaPersonalizada("Mascotas", TipoMovimiento.Gasto)
            val usuario = requireNotNull(repo.observarUsuario().first()).id
            val categorias = repo.observarCategorias().first()
            val gasto = categorias.first { it.tipo == TipoMovimiento.Gasto && it.esPredefinida }.id
            val otro = categorias.single { it.nombre == "Mascotas" }.id
            val ingreso = categorias.first { it.tipo == TipoMovimiento.Ingreso }.id
            probar(repo, usuario, gasto, otro, ingreso)
        } finally {
            base.close()
        }
    }

    private suspend fun RepositorioFinanzas.movimiento(
        mes: YearMonth, categoria: Int, monto: Long, tipo: TipoMovimiento = TipoMovimiento.Gasto,
    ) {
        insertar(Movimiento(tipo = tipo, monto = monto, fecha = Date.from(mes.atDay(2).atStartOfDay(zona).toInstant()), categoriaId = categoria))
    }
}
