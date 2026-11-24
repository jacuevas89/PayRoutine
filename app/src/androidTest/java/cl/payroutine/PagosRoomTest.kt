package cl.payroutine

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.repositorio.AjusteCuota
import cl.payroutine.datos.repositorio.NuevaDeudaEnCuotas
import cl.payroutine.datos.repositorio.NuevoMovimiento
import cl.payroutine.datos.repositorio.NuevoPagoUnico
import cl.payroutine.datos.repositorio.PeriodicidadCuotas
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import cl.payroutine.datos.repositorio.generarCronogramaCuotas
import cl.payroutine.viewmodel.EstadoPagos
import cl.payroutine.viewmodel.FechaLocal
import cl.payroutine.viewmodel.observarInicio
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
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
    fun pagaVariosUnaSolaVezYPermiteEditarYDeshacerUnGasto() = runBlocking {
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        try {
            val repositorio = RepositorioFinanzas(base)
            repositorio.prepararCategoriasIniciales()
            val categorias = repositorio.observarCategorias().first().filter { it.tipo == TipoMovimiento.Gasto }
            val hoy = LocalDate.now()
            val fechaPago = hoy.minusDays(1)
            val luz = repositorio.registrarPagoUnico(NuevoPagoUnico("Luz", 19000, hoy, categorias[0].id)).toInt()
            val agua = repositorio.registrarPagoUnico(NuevoPagoUnico("Agua", 11000, hoy.plusDays(1), categorias[1].id)).toInt()
            val seleccion = listOf(luz, agua, luz)

            List(2) { async { repositorio.pagarVencimientos(seleccion, fechaPago, "  Comprobante  ") } }.awaitAll()

            val gastos = repositorio.observarMovimientosConCategoria().first().associateBy { it.pagoId }
            assertEquals(setOf(luz, agua), gastos.keys)
            assertTrue(repositorio.observarPagosPendientes().first().isEmpty())
            val gastoLuz = gastos.getValue(luz).movimiento
            val gastoAgua = gastos.getValue(agua).movimiento
            assertEquals(19000.0, gastoLuz.monto, 0.0)
            assertEquals(categorias[1].id, gastoAgua.categoriaId)
            assertEquals(TipoMovimiento.Gasto, gastoAgua.tipo)
            assertEquals(fechaPago, gastoLuz.fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
            assertEquals("Comprobante", gastoLuz.nota)

            val corregida = hoy.minusMonths(1)
            assertTrue(repositorio.editarMovimiento(
                gastoLuz.id, NuevoMovimiento("Otro monto", TipoMovimiento.Gasto, 1, corregida, categorias[1].id, "Nota nueva"),
            ))
            val editado = repositorio.observarMovimientosConCategoria().first().single { it.pagoId == luz }.movimiento
            assertEquals(gastoLuz.nombre, editado.nombre)
            assertEquals(gastoLuz.monto, editado.monto, 0.0)
            assertEquals(gastoLuz.categoriaId, editado.categoriaId)
            assertEquals(corregida, editado.fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
            assertEquals("Nota nueva", editado.nota)
            assertThrows(SQLiteException::class.java) { runBlocking { repositorio.eliminarMovimiento(gastoLuz.id) } }

            repositorio.deshacerPago(luz, gastoLuz.id)
            repositorio.deshacerPago(luz, gastoLuz.id)
            assertEquals(luz, repositorio.observarPagosPendientes().first().single().pago.id)
            assertEquals(gastoAgua, repositorio.observarMovimientos().first().single())
            repositorio.pagarVencimientos(listOf(luz), hoy, "")
            repositorio.deshacerPago(luz, gastoLuz.id)
            assertTrue(repositorio.observarPagosPendientes().first().isEmpty())
            assertEquals(2, repositorio.observarMovimientos().first().size)

            repositorio.eliminarTodosLosDatos()
            assertTrue(repositorio.observarMovimientos().first().isEmpty())
            assertTrue(repositorio.observarPagos().first().isEmpty())
        } finally {
            base.close()
        }
    }

    @Test
    fun loteRechazadoRevierteCambiosYLaFechaFuturaEsInvalida() = runBlocking {
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        try {
            val repositorio = RepositorioFinanzas(base)
            repositorio.prepararCategoriasIniciales()
            val categoria = repositorio.observarCategorias().first().first { it.tipo == TipoMovimiento.Gasto }
            val hoy = LocalDate.now()
            val valido = repositorio.registrarPagoUnico(NuevoPagoUnico("Internet", 12000, hoy, categoria.id)).toInt()
            val sinCategoria = repositorio.insertar(
                PagoPendiente(descripcion = "Anterior", monto = 2000.0, fechaVencimiento = Date(0), esCuota = false, pagado = false),
            ).toInt()
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repositorio.pagarVencimientos(listOf(valido, sinCategoria), hoy, "") }
            }
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repositorio.pagarVencimientos(listOf(valido), hoy.plusDays(1), "") }
            }
            assertTrue(repositorio.observarMovimientos().first().isEmpty())
            assertEquals(2, repositorio.observarPagosPendientes().first().size)
        } finally {
            base.close()
        }
    }

    @Test
    fun registraCuatroDeDoceComoAntecedentesYLaQuintaQuedaPendienteSinAfectarElSaldo() = runBlocking {
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        try {
            val repositorio = RepositorioFinanzas(base)
            repositorio.prepararCategoriasIniciales()
            val categoria = repositorio.observarCategorias().first().single { it.nombre == "Hogar" }
            val zona = ZoneId.systemDefault()
            val hoy = LocalDate.now(zona)
            val primera = hoy.withDayOfMonth(1).minusMonths(4)
            val cronograma = generarCronogramaCuotas(100000, 12, primera, PeriodicidadCuotas.Mensual, AjusteCuota.Ultima)
            val deuda = NuevaDeudaEnCuotas("Notebook", 100000, categoria.id, 4, cronograma)
            repositorio.registrarDeuda(deuda)
            repositorio.registrarDeuda(deuda.copy(nombre = "Deuda anterior", cuotasPagadas = 12))

            val estado = EstadoPagos(cargando = false, pagos = repositorio.observarPagosConCategoria().first())
            val cuotas = estado.cuotasPorObligacion.values.single { it.first().nombreObligacion == "Notebook" }
            assertEquals((1..12).toList(), cuotas.map { it.pago.cuotaActual })
            assertTrue(cuotas.all { it.pago.totalCuotas == 12 && it.nombreCategoria == "Hogar" })
            assertEquals(4, cuotas.count { it.pago.pagado })
            assertEquals(33332L, cuotas.filter { it.pago.pagado }.sumOf { it.pago.monto.toLong() })
            assertEquals(66668L, estado.pendientes.sumOf { it.pago.monto.toLong() })
            assertEquals((5..12).toList(), estado.pendientes.map { it.pago.cuotaActual })
            assertEquals("Deuda anterior", estado.deudasSaldadas.single().nombreObligacion)
            val inicio = repositorio.observarInicio(flowOf(FechaLocal(hoy, zona))).first()
            assertEquals(5, inicio.pagosPendientes.single().pago.cuotaActual)
            assertTrue(repositorio.observarMovimientos().first().isEmpty())
            assertEquals(BigDecimal.ZERO, inicio.saldo)
        } finally {
            base.close()
        }
    }

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
            prepararBaseAnterior(nombre, 4) {
                execSQL("INSERT INTO PagoPendiente VALUES (7, 'Pendiente anterior', 20000.25, 0, 1, 2, 6, 0)")
                execSQL("INSERT INTO PagoPendiente VALUES (9, 'Pagado anterior', 10000, 0, 0, NULL, NULL, 1)")
            }
            val base = BaseDatosPayRoutine.crear(contexto, nombre)
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
                assertEquals(8, base.openHelper.readableDatabase.version)
            } finally {
                base.close()
            }
        } finally {
            contexto.deleteDatabase(nombre)
        }
        Unit
    }

    @Test
    fun migraLaVersionCincoLocalYConservaLasCuotasVinculadas() = runBlocking {
        val nombre = "pagos-v5-${UUID.randomUUID()}.db"
        try {
            prepararBaseAnterior(nombre, 4) {
                execSQL("CREATE TABLE ObligacionPago (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, categoria_id INTEGER NOT NULL REFERENCES Categoria(id))")
                execSQL("CREATE INDEX index_ObligacionPago_categoria_id ON ObligacionPago(categoria_id)")
                execSQL("ALTER TABLE PagoPendiente ADD COLUMN obligacion_id INTEGER REFERENCES ObligacionPago(id)")
                execSQL("CREATE INDEX index_PagoPendiente_obligacion_id ON PagoPendiente(obligacion_id)")
                execSQL("UPDATE room_master_table SET identity_hash = 'a6b2aa185822f70d9d966a667ff31702' WHERE id = 42")
                execSQL("INSERT INTO Categoria VALUES (42, 'Servicios', 'Gasto', 1)")
                execSQL("INSERT INTO ObligacionPago VALUES (8, 'Computación', 42)")
                execSQL("INSERT INTO PagoPendiente VALUES (7, 'Cuota 1 de 2', 12000, 0, 1, 1, 2, 1, 8)")
                execSQL("INSERT INTO PagoPendiente VALUES (9, 'Cuota 2 de 2', 12000, 86400000, 1, 2, 2, 0, 8)")
                version = 5
            }
            val base = BaseDatosPayRoutine.crear(contexto, nombre)
            try {
                val repositorio = RepositorioFinanzas(base)
                val cuotas = repositorio.observarPagosConCategoria().first()
                assertEquals(8, base.openHelper.readableDatabase.version)
                assertEquals(listOf(8, 8), cuotas.map { it.pago.obligacionId })
                assertEquals(listOf(true, false), cuotas.map { it.pago.pagado })
                assertEquals("Computación", cuotas.first().nombreObligacion)
                assertEquals("Servicios", cuotas.first().nombreCategoria)
                assertNull(repositorio.obtenerRecordatorio())
            } finally {
                base.close()
            }
        } finally {
            contexto.deleteDatabase(nombre)
        }
        Unit
    }

    @Test
    fun migraLaVersionSeisYConservaPresupuestosRecordatoriosYPagos() = runBlocking {
        val nombre = "pagos-v6-${UUID.randomUUID()}.db"
        try {
            prepararBaseAnterior(nombre, 6) {
                execSQL("INSERT INTO Usuario VALUES ('perfil-anterior', 'María Pérez', 'Mari')")
                execSQL("INSERT INTO RecordatorioMovimiento VALUES ('perfil-anterior', 1, 1200, NULL, 0, 'Diaria', 'recordatorio', NULL)")
                execSQL("INSERT INTO PresupuestoMensual (anio, mes, modalidad, limiteGeneral, generado) VALUES (2026, 11, 'General', 80000, 0)")
                execSQL("INSERT INTO PagoPendiente VALUES (7, 'Pendiente anterior', 24000, 0, 0, NULL, NULL, 0)")
            }
            val base = BaseDatosPayRoutine.crear(contexto, nombre)
            try {
                val repositorio = RepositorioFinanzas(base)
                assertEquals(8, base.openHelper.readableDatabase.version)
                assertEquals("recordatorio", repositorio.obtenerRecordatorio()?.programacionId)
                assertTrue(repositorio.existePresupuesto(YearMonth.of(2026, 11)))
                val pago = repositorio.observarPagos().first().single()
                assertEquals("Pendiente anterior", pago.descripcion)
                assertNull(pago.obligacionId)
            } finally {
                base.close()
            }
        } finally {
            contexto.deleteDatabase(nombre)
        }
        Unit
    }

    @Test
    fun migraDesdeVersionSieteSinCrearGastosParaCuotasHistoricas() = runBlocking {
        val nombre = "pagos-v7-${UUID.randomUUID()}.db"
        try {
            prepararBaseAnterior(nombre, 7) {
                execSQL("INSERT INTO Categoria VALUES (42, 'Servicios', 'Gasto', 1)")
                execSQL("INSERT INTO ObligacionPago VALUES (8, 'Equipo', 42)")
                execSQL("INSERT INTO PagoPendiente VALUES (7, 'Cuota 1 de 2', 12000, 0, 1, 1, 2, 1, 8)")
                execSQL("INSERT INTO PagoPendiente VALUES (9, 'Cuota 2 de 2', 12000, 86400000, 1, 2, 2, 0, 8)")
            }
            val base = BaseDatosPayRoutine.crear(contexto, nombre)
            try {
                val repositorio = RepositorioFinanzas(base)
                assertEquals(8, base.openHelper.readableDatabase.version)
                val anterior = repositorio.observarPagos().first().first()
                assertTrue(anterior.pagado)
                assertNull(anterior.movimientoId)
                assertTrue(repositorio.observarMovimientos().first().isEmpty())
                repositorio.pagarVencimientos(listOf(9), LocalDate.now(), "")
                assertEquals(9, repositorio.observarMovimientosConCategoria().first().single().pagoId)
            } finally {
                base.close()
            }
        } finally {
            contexto.deleteDatabase(nombre)
        }
        Unit
    }

    private fun prepararBaseAnterior(nombre: String, versionEsquema: Int, insertar: SQLiteDatabase.() -> Unit) {
        val esquema = instrumentacion.context.assets.open("${BaseDatosPayRoutine::class.java.name}/$versionEsquema.json")
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
            sqlite.version = versionEsquema
            sqlite.insertar()
        }
    }
}
