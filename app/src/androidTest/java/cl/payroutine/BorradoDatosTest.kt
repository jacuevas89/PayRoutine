package cl.payroutine

import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.Presupuesto
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BorradoDatosTest {
    @Test
    fun falloRevierteElBorradoYReintentoConservaSoloPredefinidas() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val nombreBase = "rf21-${UUID.randomUUID()}.db"
        fun abrirBase() = Room.databaseBuilder(contexto, BaseDatosPayRoutine::class.java, nombreBase).build()

        try {
            val base = abrirBase()
            val (idAnterior, predefinidasConservadas) = try {
                val repositorio = RepositorioFinanzas(base)
                repositorio.registrarUsuario("María Pérez", "Mari")
                val usuario = requireNotNull(repositorio.observarUsuario().first())
                val categoriasIniciales = repositorio.observarCategorias().first()
                assertEquals(16, categoriasIniciales.size)
                assertTrue(categoriasIniciales.all { it.esPredefinida })
                val categoriaFaltante = categoriasIniciales.single { it.nombre == "Otros" }
                repositorio.eliminar(categoriaFaltante)
                val predefinidas = categoriasIniciales - categoriaFaltante
                val categoria = Categoria(nombre = "Regalos", tipo = TipoMovimiento.Gasto)
                val idCategoria = repositorio.insertar(categoria).toInt()
                val homonima = Categoria(nombre = "Sueldo", tipo = TipoMovimiento.Ingreso)
                val idHomonima = repositorio.insertar(homonima).toInt()
                val fecha = Date(0)
                val movimiento = Movimiento(tipo = TipoMovimiento.Gasto, monto = 4000.0, fecha = fecha, categoriaId = idCategoria)
                val presupuesto = Presupuesto(categoriaId = idCategoria, montoPresupuestado = 5000.0, mes = 11, anio = 2026)
                val pago = PagoPendiente(descripcion = "Prueba", monto = 12000.0, fechaVencimiento = fecha, esCuota = false, pagado = true)
                val cuota = pago.copy(descripcion = "Cuota de prueba", esCuota = true, cuotaActual = 1, totalCuotas = 2, pagado = false)
                val idMovimiento = repositorio.insertar(movimiento).toInt()
                val idPresupuesto = repositorio.insertar(presupuesto).toInt()
                val idPago = repositorio.insertar(pago).toInt()
                val idCuota = repositorio.insertar(cuota).toInt()

                val sqlite = base.openHelper.writableDatabase
                sqlite.execSQL("CREATE TRIGGER fallar_borrado BEFORE DELETE ON Usuario BEGIN SELECT RAISE(ABORT, 'Fallo simulado'); END")
                assertTrue(runCatching { repositorio.eliminarTodosLosDatos() }.exceptionOrNull() is SQLiteException)
                assertEquals(usuario, repositorio.observarUsuario().first())
                assertEquals(
                    (predefinidas + categoria.copy(id = idCategoria) + homonima.copy(id = idHomonima)).toSet(),
                    repositorio.observarCategorias().first().toSet(),
                )
                assertEquals(listOf(movimiento.copy(id = idMovimiento)), repositorio.observarMovimientos().first())
                assertEquals(listOf(presupuesto.copy(id = idPresupuesto)), repositorio.observarPresupuestos().first())
                assertEquals(listOf(pago.copy(id = idPago), cuota.copy(id = idCuota)), repositorio.observarPagos().first())

                sqlite.execSQL("DROP TRIGGER fallar_borrado")
                repositorio.eliminarTodosLosDatos()
                usuario.id to predefinidas
            } finally {
                base.close()
            }

            val reabierta = abrirBase()
            try {
                val repositorio = RepositorioFinanzas(reabierta)
                assertNull(repositorio.observarUsuario().first())
                val categorias = repositorio.observarCategorias().first()
                assertEquals(16, categorias.size)
                assertTrue(categorias.all { it.esPredefinida })
                assertTrue(categorias.containsAll(predefinidasConservadas))
                assertEquals(
                    setOf("Alimentación", "Transporte", "Hogar", "Servicios", "Ocio", "Salud", "Otros", "Educación", "Vestuario", "Mascotas"),
                    categorias.filter { it.tipo == TipoMovimiento.Gasto }.map { it.nombre }.toSet(),
                )
                assertEquals(
                    setOf("Sueldo", "Freelance", "Ventas", "Arriendos", "Pensiones", "Otros ingresos"),
                    categorias.filter { it.tipo == TipoMovimiento.Ingreso }.map { it.nombre }.toSet(),
                )
                assertTrue(repositorio.observarMovimientos().first().isEmpty())
                assertTrue(repositorio.observarPresupuestos().first().isEmpty())
                assertTrue(repositorio.observarPagos().first().isEmpty())
                repositorio.registrarUsuario("María Pérez", "Mari")
                val nuevo = requireNotNull(repositorio.observarUsuario().first())
                assertEquals(nuevo.id, UUID.fromString(nuevo.id).toString())
                assertNotEquals(idAnterior, nuevo.id)
                assertEquals(categorias, repositorio.observarCategorias().first())
            } finally {
                reabierta.close()
            }
        } finally {
            contexto.deleteDatabase(nombreBase)
        }
        Unit
    }
}
