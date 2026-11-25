package cl.payroutine

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.repositorio.MONTO_MAXIMO_EXACTO
import cl.payroutine.datos.repositorio.NuevoMovimiento
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import cl.payroutine.viewmodel.FechaLocal
import cl.payroutine.viewmodel.observarInicio
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RegistroMovimientosRoomTest {
    private val instrumentacion = InstrumentationRegistry.getInstrumentation()
    private val contexto = instrumentacion.targetContext

    @Test
    fun editaYEliminaPorIdYActualizaSaldoYContadores() = runBlocking {
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        val repositorio = RepositorioFinanzas(base)
        try {
            repositorio.prepararCategoriasIniciales()
            val categorias = repositorio.observarCategorias().first()
            val sueldo = categorias.single { it.nombre == "Sueldo" }
            val hogar = categorias.single { it.nombre == "Hogar" }
            val transporte = categorias.single { it.nombre == "Transporte" }
            val zona = ZoneId.systemDefault()
            val hoy = LocalDate.now(zona)
            val instanteOriginal = Date.from(hoy.atTime(10, 30).atZone(zona).toInstant())
            val gasto = Movimiento(tipo = TipoMovimiento.Gasto, monto = 5000L, fecha = instanteOriginal, categoriaId = hogar.id)
            val idGasto = repositorio.insertar(gasto).toInt()
            val idIngreso = repositorio.registrarMovimiento(
                NuevoMovimiento("Sueldo", TipoMovimiento.Ingreso, 50000, hoy, sueldo.id),
            ).toInt()

            val cambio = NuevoMovimiento(" Reparación ", TipoMovimiento.Gasto, 11000, hoy, transporte.id, " Taller ")
            for (invalido in listOf(cambio.copy(tipo = TipoMovimiento.Ingreso, categoriaId = sueldo.id), cambio.copy(categoriaId = sueldo.id))) {
                assertTrue(runCatching { repositorio.editarMovimiento(idGasto, invalido) }.exceptionOrNull() is IllegalArgumentException)
            }
            assertEquals(gasto.copy(id = idGasto), repositorio.observarMovimientos().first().single { it.id == idGasto })
            assertTrue(repositorio.editarMovimiento(idGasto, cambio))
            val editado = repositorio.observarMovimientos().first().single { it.id == idGasto }
            assertEquals(gasto.copy(id = idGasto, monto = 11000L, categoriaId = transporte.id, nota = "Taller", nombre = "Reparación"), editado)
            val inicio = repositorio.observarInicio(flowOf(FechaLocal(hoy, zona)))
            assertEquals(BigDecimal("39000"), inicio.first().saldo)
            val usos = repositorio.observarCategoriasConUso()
            assertEquals(0, usos.first().single { it.categoria.id == hogar.id }.cantidadMovimientos)
            assertEquals(1, usos.first().single { it.categoria.id == transporte.id }.cantidadMovimientos)

            assertEquals(1, repositorio.eliminarMovimiento(idGasto))
            assertEquals(0, repositorio.eliminarMovimiento(idGasto))
            assertFalse(repositorio.editarMovimiento(idGasto, cambio))
            assertEquals(idIngreso, repositorio.observarMovimientos().first().single().id)
            assertEquals(BigDecimal("50000"), inicio.first().saldo)
            assertEquals(0, usos.first().single { it.categoria.id == transporte.id }.cantidadMovimientos)
        } finally {
            base.close()
        }
    }

    @Test
    fun registraAmbosTiposActualizaInicioYContadoresYPersisteAlReabrir() = runBlocking {
        val nombreBase = "rf23-${UUID.randomUUID()}.db"
        var base = BaseDatosPayRoutine.crear(contexto, nombreBase)
        try {
            val repositorio = RepositorioFinanzas(base)
            repositorio.prepararCategoriasIniciales()
            val categorias = repositorio.observarCategorias().first()
            val sueldo = categorias.single { it.nombre == "Sueldo" }
            val hogar = categorias.single { it.nombre == "Hogar" }
            val zona = ZoneId.systemDefault()
            val hoy = LocalDate.now(zona)
            val ingreso = repositorio.registrarMovimiento(NuevoMovimiento(" Sueldo noviembre ", TipoMovimiento.Ingreso, 500000, hoy, sueldo.id))
            val gasto = repositorio.registrarMovimiento(NuevoMovimiento(" Arriendo ", TipoMovimiento.Gasto, 200000, hoy, hogar.id, "  Noviembre  "))
            val filas = repositorio.observarMovimientos().first()
            assertEquals(listOf(gasto.toInt(), ingreso.toInt()), filas.map { it.id })
            assertEquals(listOf("Arriendo", "Sueldo noviembre"), filas.map { it.nombre })
            assertEquals("Noviembre", filas.first().nota)
            assertNull(filas.last().nota)
            assertTrue(filas.all { it.fecha.toInstant().atZone(zona).toLocalDate() == hoy })
            val inicio = repositorio.observarInicio(flowOf(FechaLocal(hoy, zona))).first()
            assertEquals(BigDecimal("500000"), inicio.ingresos)
            assertEquals(BigDecimal("200000"), inicio.gastos)
            assertEquals(BigDecimal("300000"), inicio.saldo)
            val usos = repositorio.observarCategoriasConUso().first()
            assertEquals(1, usos.single { it.categoria.id == sueldo.id }.cantidadMovimientos)
            assertEquals(1, usos.single { it.categoria.id == hogar.id }.cantidadMovimientos)

            base.close()
            base = BaseDatosPayRoutine.crear(contexto, nombreBase)
            assertEquals(filas, RepositorioFinanzas(base).observarMovimientos().first())
        } finally {
            base.close()
            contexto.deleteDatabase(nombreBase)
        }
        Unit
    }

    @Test
    fun rechazaDatosInvalidosSinInsertarRegistros() = runBlocking {
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        val repositorio = RepositorioFinanzas(base)
        try {
            repositorio.prepararCategoriasIniciales()
            val sueldo = repositorio.observarCategorias().first().single { it.nombre == "Sueldo" }
            val valido = NuevoMovimiento("Sueldo", TipoMovimiento.Ingreso, 500000, LocalDate.now(), sueldo.id)
            for (datos in listOf(
                valido.copy(nombre = "  "), valido.copy(monto = 0), valido.copy(monto = -1),
                valido.copy(monto = MONTO_MAXIMO_EXACTO + 1), valido.copy(fecha = LocalDate.now().plusDays(1)),
                valido.copy(tipo = TipoMovimiento.Gasto), valido.copy(categoriaId = -1),
            )) {
                assertTrue(runCatching { repositorio.registrarMovimiento(datos) }.exceptionOrNull() is IllegalArgumentException)
            }
            assertTrue(repositorio.observarMovimientos().first().isEmpty())
            val historico = valido.copy(fecha = LocalDate.now().minusYears(1))
            repositorio.registrarMovimiento(historico)
            assertEquals(historico.fecha, repositorio.observarMovimientos().first().single().fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
        } finally {
            base.close()
        }
    }

    @Test
    fun migracionDesdeVersionTresConservaMovimientosCategoriasPresupuestosYPagos() = runBlocking {
        val nombreBase = "rf23-migracion-${UUID.randomUUID()}.db"
        try {
            val esquema = instrumentacion.context.assets.open("${BaseDatosPayRoutine::class.java.name}/3.json")
                .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
            val archivo = contexto.getDatabasePath(nombreBase)
            archivo.parentFile?.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(archivo, null).use { sqlite ->
                val entidades = esquema.getJSONArray("entities")
                for (indice in 0 until entidades.length()) {
                    val entidad = entidades.getJSONObject(indice)
                    val tabla = entidad.getString("tableName")
                    sqlite.execSQL(entidad.getString("createSql").replace("\${TABLE_NAME}", tabla))
                    val indices = entidad.optJSONArray("indices") ?: continue
                    for (i in 0 until indices.length()) sqlite.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", tabla))
                }
                val consultas = esquema.getJSONArray("setupQueries")
                for (i in 0 until consultas.length()) sqlite.execSQL(consultas.getString(i))
                sqlite.execSQL("INSERT INTO Categoria (id, nombre, tipo, es_predefinida) VALUES (42, 'Personalizada', 'Gasto', 0)")
                sqlite.execSQL("INSERT INTO Movimiento VALUES (54, 'Gasto', 24000.25, 0, 42, 'Nota anterior')")
                sqlite.execSQL("INSERT INTO Presupuesto VALUES (31, 42, 40000, 11, 2026)")
                sqlite.execSQL("INSERT INTO PagoPendiente VALUES (7, 'Internet', 20000, 0, 0, NULL, NULL, 0)")
                sqlite.execSQL("INSERT INTO Usuario VALUES ('perfil-anterior', 'María Pérez', 'Mari')")
                sqlite.version = 3
            }
            val base = BaseDatosPayRoutine.crear(contexto, nombreBase)
            try {
                val repositorio = RepositorioFinanzas(base)
                val anterior = repositorio.observarMovimientos().first().single()
                assertEquals(54, anterior.id)
                assertEquals("", anterior.nombre)
                assertEquals("Nota anterior", anterior.nota)
                assertEquals(24000L, anterior.monto)
                assertEquals(42, anterior.categoriaId)
                assertEquals("Personalizada", repositorio.observarCategorias().first().single().nombre)
                assertEquals(31, repositorio.observarPresupuestos().first().single().id)
                assertEquals(7, repositorio.observarPagos().first().single().id)
                assertEquals("perfil-anterior", repositorio.observarUsuario().first()?.id)
                assertEquals(10, base.openHelper.readableDatabase.version)
                assertNull(repositorio.obtenerRecordatorio())
            } finally {
                base.close()
            }
        } finally {
            contexto.deleteDatabase(nombreBase)
        }
        Unit
    }
}
