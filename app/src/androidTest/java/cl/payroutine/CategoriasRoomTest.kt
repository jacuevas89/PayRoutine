package cl.payroutine

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.Presupuesto
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoriasRoomTest {
    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun catalogoPersisteSinDuplicadosConInicializacionesSimultaneas() = runBlocking {
        val nombreBase = "rf41-${UUID.randomUUID()}.db"
        var base = Room.databaseBuilder(contexto, BaseDatosPayRoutine::class.java, nombreBase).build()
        try {
            val repositorio = RepositorioFinanzas(base)
            assertTrue(repositorio.observarCategorias().first().isEmpty())
            List(4) { async(Dispatchers.IO) { repositorio.prepararCategoriasIniciales() } }.awaitAll()
            val categorias = repositorio.observarCategorias().first()
            assertEquals(16, categorias.size)
            assertEquals(
                setOf("Alimentación", "Transporte", "Hogar", "Servicios", "Ocio", "Salud", "Otros", "Educación", "Vestuario", "Mascotas"),
                categorias.filter { it.tipo == TipoMovimiento.Gasto }.map { it.nombre }.toSet(),
            )
            assertEquals(
                setOf("Sueldo", "Freelance", "Ventas", "Arriendos", "Pensiones", "Otros ingresos"),
                categorias.filter { it.tipo == TipoMovimiento.Ingreso }.map { it.nombre }.toSet(),
            )
            assertTrue(repositorio.observarCategoriasConUso().first().all { it.cantidadMovimientos == 0 })

            base.close()
            base = Room.databaseBuilder(contexto, BaseDatosPayRoutine::class.java, nombreBase).build()
            val reabierto = RepositorioFinanzas(base)
            reabierto.prepararCategoriasIniciales()
            assertEquals(categorias, reabierto.observarCategorias().first())
            assertEquals(2, base.openHelper.readableDatabase.version)
        } finally {
            base.close()
            contexto.deleteDatabase(nombreBase)
        }
        Unit
    }

    @Test
    fun completaCatalogoConservandoNombresIdsRelacionesYPerfilExistentes() = runBlocking {
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        val repositorio = RepositorioFinanzas(base)
        try {
            val alimentacion = Categoria(42, " ALIMENTACIÓN ", TipoMovimiento.Gasto)
            val personalizada = Categoria(91, "Viajes", TipoMovimiento.Gasto)
            val ingresoConMismoNombre = Categoria(92, "Hogar", TipoMovimiento.Ingreso)
            listOf(alimentacion, personalizada, ingresoConMismoNombre).forEach { repositorio.insertar(it) }
            val gasto = Movimiento(54, TipoMovimiento.Gasto, 5000.0, Date(0), alimentacion.id, "Compra previa")
            val presupuesto = Presupuesto(31, alimentacion.id, 40000.0, 11, 2026)
            repositorio.insertar(gasto)
            repositorio.insertar(presupuesto)
            repositorio.registrarUsuario("María Pérez", "Mari")
            val usuario = repositorio.observarUsuario().first()

            repositorio.prepararCategoriasIniciales()
            repositorio.prepararCategoriasIniciales()
            val categorias = repositorio.observarCategorias().first()
            assertEquals(18, categorias.size)
            assertTrue(categorias.containsAll(listOf(alimentacion, personalizada, ingresoConMismoNombre)))
            assertEquals(1, categorias.count { it.tipo == TipoMovimiento.Gasto && it.nombre.trim().equals("Alimentación", ignoreCase = true) })
            assertTrue(categorias.any { it.nombre == "Hogar" && it.tipo == TipoMovimiento.Gasto })
            assertEquals(gasto, repositorio.observarMovimientos().first().single())
            assertEquals(presupuesto, repositorio.observarPresupuestos().first().single())
            assertEquals(usuario, repositorio.observarUsuario().first())
            assertEquals(1, repositorio.observarCategoriasConUso().first().single { it.categoria.id == 42 }.cantidadMovimientos)
            repositorio.eliminar(gasto)
            assertEquals(0, repositorio.observarCategoriasConUso().first().single { it.categoria.id == 42 }.cantidadMovimientos)
        } finally {
            base.close()
        }
    }
}
