package cl.payroutine

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.repositorio.NuevoPagoUnico
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import cl.payroutine.viewmodel.FechaLocal
import cl.payroutine.viewmodel.observarInicio
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PagosRoomTest {
    private val instrumentacion = InstrumentationRegistry.getInstrumentation()
    private val contexto = instrumentacion.targetContext

    @Test
    fun registraPagosOrdenadosYLosReflejaEnInicioSinAfectarElSaldo() = runBlocking {
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        try {
            val repositorio = RepositorioFinanzas(base)
            repositorio.prepararCategoriasIniciales()
            val categoria = repositorio.observarCategorias().first().single { it.nombre == "Servicios" }
            val zona = ZoneId.systemDefault()
            val hoy = LocalDate.now(zona)

            val seguro = repositorio.registrarPagoUnico(NuevoPagoUnico(" Seguro ", 55000, hoy.plusMonths(1), categoria.id))
            val internet = repositorio.registrarPagoUnico(NuevoPagoUnico("Internet", 24990, hoy, categoria.id))
            val vencido = repositorio.registrarPagoUnico(NuevoPagoUnico("Vencido", 12000, hoy.minusDays(2), categoria.id))
            val pendientes = repositorio.observarPagosPendientes().first()
            assertEquals(listOf(vencido, internet, seguro), pendientes.map { it.pago.id.toLong() })
            assertEquals(listOf("Vencido", "Internet", "Seguro"), pendientes.map { it.pago.descripcion })
            assertTrue(pendientes.all { it.nombreCategoria == "Servicios" && !it.pago.pagado && !it.pago.esCuota })
            assertEquals(3, pendientes.mapNotNull { it.pago.obligacionId }.toSet().size)
            assertEquals(24990.0, pendientes[1].pago.monto, 0.0)
            assertEquals(hoy, pendientes[1].pago.fechaVencimiento.toInstant().atZone(zona).toLocalDate())

            val inicio = repositorio.observarInicio(flowOf(FechaLocal(hoy, zona))).first()
            assertEquals(listOf(vencido, internet), inicio.pagosPendientes.map { it.pago.id.toLong() })
            assertEquals(BigDecimal.ZERO, inicio.saldo)
            assertTrue(repositorio.observarMovimientos().first().isEmpty())
        } finally {
            base.close()
        }
    }

    @Test
    fun validaCategoriaYRevierteUnAltaIncompletaYPermiteRestablecerLosDatos() = runBlocking {
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        try {
            val repositorio = RepositorioFinanzas(base)
            repositorio.registrarUsuario("María Pérez", "Mari")
            assertTrue(repositorio.crearCategoriaPersonalizada("Facturas", TipoMovimiento.Gasto))
            val categorias = repositorio.observarCategorias().first()
            val categoria = categorias.single { it.nombre == "Facturas" }
            val sueldo = categorias.single { it.nombre == "Sueldo" }
            val datos = NuevoPagoUnico("Luz", 20000, LocalDate.now().plusDays(2), categoria.id)
            for (invalido in listOf(datos.copy(categoriaId = sueldo.id), datos.copy(categoriaId = -1), datos.copy(monto = 0), datos.copy(nombre = " "))) {
                assertTrue(runCatching { repositorio.registrarPagoUnico(invalido) }.exceptionOrNull() is IllegalArgumentException)
            }

            val sqlite = base.openHelper.writableDatabase
            sqlite.execSQL("CREATE TRIGGER fallar_vencimiento BEFORE INSERT ON PagoPendiente BEGIN SELECT RAISE(ABORT, 'Prueba'); END")
            assertTrue(runCatching { repositorio.registrarPagoUnico(datos) }.exceptionOrNull() is SQLiteException)
            assertTrue(repositorio.observarPagos().first().isEmpty())
            sqlite.query("SELECT COUNT(*) FROM ObligacionPago").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }

            sqlite.execSQL("DROP TRIGGER fallar_vencimiento")
            repositorio.registrarPagoUnico(datos)
            repositorio.eliminarTodosLosDatos()
            assertTrue(repositorio.observarPagos().first().isEmpty())
            assertNull(repositorio.observarUsuario().first())
            assertEquals(16, repositorio.observarCategorias().first().size)
            assertTrue(repositorio.observarCategorias().first().all { it.esPredefinida })
        } finally {
            base.close()
        }
    }

    @Test
    fun migraDesdeVersionCuatroSinPerderPagosAnteriores() = runBlocking {
        val nombre = "rf81-${UUID.randomUUID()}.db"
        try {
            val esquema = instrumentacion.context.assets.open("${BaseDatosPayRoutine::class.java.name}/4.json")
                .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
            val archivo = contexto.getDatabasePath(nombre)
            archivo.parentFile?.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(archivo, null).use { sqlite ->
                val entidades = esquema.getJSONArray("entities")
                for (i in 0 until entidades.length()) {
                    val entidad = entidades.getJSONObject(i)
                    val tabla = entidad.getString("tableName")
                    sqlite.execSQL(entidad.getString("createSql").replace("\${TABLE_NAME}", tabla))
                    val indices = entidad.optJSONArray("indices") ?: continue
                    for (j in 0 until indices.length()) {
                        sqlite.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", tabla))
                    }
                }
                val consultas = esquema.getJSONArray("setupQueries")
                for (i in 0 until consultas.length()) sqlite.execSQL(consultas.getString(i))
                sqlite.execSQL("INSERT INTO PagoPendiente VALUES (7, 'Pendiente anterior', 20000.25, 0, 1, 2, 6, 0)")
                sqlite.execSQL("INSERT INTO PagoPendiente VALUES (9, 'Pagado anterior', 10000, 0, 0, NULL, NULL, 1)")
                sqlite.version = 4
            }
            val base = Room.databaseBuilder(contexto, BaseDatosPayRoutine::class.java, nombre).build()
            try {
                val repositorio = RepositorioFinanzas(base)
                val pagos = repositorio.observarPagos().first()
                assertEquals(listOf(7, 9), pagos.map { it.id })
                assertNull(pagos.first().obligacionId)
                assertEquals(20000.25, pagos.first().monto, 0.0)
                assertEquals(2, pagos.first().cuotaActual)
                assertEquals(6, pagos.first().totalCuotas)
                assertTrue(pagos.last().pagado)
                val pendiente = repositorio.observarPagosPendientes().first().single()
                assertEquals("Pendiente anterior", pendiente.pago.descripcion)
                assertNull(pendiente.nombreCategoria)
                assertEquals(5, base.openHelper.readableDatabase.version)
            } finally {
                base.close()
            }
        } finally {
            contexto.deleteDatabase(nombre)
        }
        Unit
    }
}
