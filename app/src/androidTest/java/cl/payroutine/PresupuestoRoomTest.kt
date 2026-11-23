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
        repo.guardarPresupuestoMensual(usuario, inicio, 50_000.0, emptyMap())
        assertFalse(requireNotNull(repo.preferenciasPresupuesto()).configuracionPendiente)
        assertEquals(ModalidadPresupuesto.General, repo.observarPresupuestosMensuales().first().single().cabecera.modalidad)

        repo.guardarPresupuestoMensual(usuario, inicio, null, mapOf(gasto to 30_000.0, otro to 20_000.0))
        val mesCopiado = inicio.plusMonths(1)
        val limites = repo.observarPresupuestosMensuales().first().associate {
            requireNotNull(it.categoriaId) to requireNotNull(it.limite)
        }
        repo.guardarPresupuestoMensual(usuario, mesCopiado, null, limites + (gasto to 40_000.0))
        repo.guardarPresupuestoMensual(usuario, mesCopiado, null, mapOf(gasto to 40_000.0))
        val guardados = repo.observarPresupuestosMensuales().first()
        assertEquals(2, guardados.count { it.cabecera.mes == inicio.monthValue })
        assertEquals(40_000.0, guardados.single { it.cabecera.mes == mesCopiado.monthValue }.limite!!, 0.0)
        assertEquals(3, repo.observarPresupuestos().first().size)

        repo.guardarPresupuestoMensual(usuario, mesCopiado, 40_000.0, emptyMap())
        assertEquals(2, repo.observarPresupuestos().first().size)
        repo.eliminarTodosLosDatos()
        assertNull(repo.preferenciasPresupuesto())
        assertTrue(repo.observarPresupuestosMensuales().first().isEmpty())
    }

    @Test
    fun automaticoUsaMesesDisponiblesYRespetaEdiciones() = conPerfil { repo, usuario, gasto, otro, ingreso ->
        val anterior = inicio.minusMonths(1)
        repo.movimiento(anterior, gasto, 900.0)
        repo.configurarAutomatico(usuario, true, false, inicio, zona)
        repo.generarPresupuestoAutomatico(inicio, zona)
        assertFalse(repo.existePresupuesto(inicio))

        val diciembre = inicio.plusMonths(1)
        val enero = inicio.plusMonths(2)
        repo.movimiento(diciembre, gasto, 300.0)
        repo.movimiento(diciembre, otro, 120.0)
        repo.movimiento(diciembre, ingreso, 90.0, TipoMovimiento.Ingreso)
        repo.generarPresupuestoAutomatico(diciembre, zona)
        assertFalse(repo.existePresupuesto(diciembre))
        repo.generarPresupuestoAutomatico(enero, zona)
        val primerResultado = repo.observarPresupuestosMensuales().first()
        assertTrue(primerResultado.all { it.cabecera.generado })
        assertEquals(listOf(120.0, 300.0), primerResultado.map { requireNotNull(it.limite) }.sorted())
        assertEquals(90.0, primerResultado.first().cabecera.ingresoReferencia!!, 0.0)
        repo.guardarPresupuestoMensual(usuario, enero, 111.0, emptyMap())
        repo.generarPresupuestoAutomatico(enero, zona)
        assertEquals(111.0, repo.observarPresupuestosMensuales().first().single().cabecera.limiteGeneral!!, 0.0)

        val febrero = inicio.plusMonths(3)
        val marzo = inicio.plusMonths(4)
        repo.movimiento(febrero, ingreso, 210.0, TipoMovimiento.Ingreso)
        repo.generarPresupuestoAutomatico(marzo, zona)
        val promedio = repo.observarPresupuestosMensuales().first().filter { it.cabecera.mes == marzo.monthValue }
        assertEquals(listOf(60.0, 150.0), promedio.map { requireNotNull(it.limite) }.sorted())
        assertEquals(150.0, promedio.first().cabecera.ingresoReferencia!!, 0.0)
        assertFalse(repo.existePresupuesto(febrero))
        repo.configurarAutomatico(usuario, false, false, marzo, zona)
        assertNull(requireNotNull(repo.preferenciasPresupuesto()).primerMesAutomatico)
        assertTrue(repo.existePresupuesto(marzo))
    }

    @Test
    fun confirmacionExplicitaYDiaTreintaYUno() = conPerfil { repo, usuario, gasto, _, ingreso ->
        val anterior = inicio.minusMonths(1)
        repo.movimiento(anterior, ingreso, 1_000.0, TipoMovimiento.Ingreso)
        repo.configurarAutomatico(usuario, true, true, inicio, zona)
        assertFalse(repo.existePresupuesto(inicio))
        repo.movimiento(anterior, gasto, 500.0)
        repo.configurarAutomatico(usuario, true, true, inicio, zona)
        assertEquals(500.0, repo.observarPresupuestosMensuales().first().single().limite!!, 0.0)
        repo.movimiento(anterior, gasto, 400.0)
        repo.configurarAutomatico(usuario, true, true, inicio, zona)
        assertEquals(500.0, repo.observarPresupuestosMensuales().first().single().limite!!, 0.0)
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
        mes: YearMonth, categoria: Int, monto: Double, tipo: TipoMovimiento = TipoMovimiento.Gasto,
    ) {
        insertar(Movimiento(tipo = tipo, monto = monto, fecha = Date.from(mes.atDay(2).atStartOfDay(zona).toInstant()), categoriaId = categoria))
    }
}
