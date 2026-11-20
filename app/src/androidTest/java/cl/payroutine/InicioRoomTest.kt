package cl.payroutine

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import cl.payroutine.viewmodel.DatosInicio
import cl.payroutine.viewmodel.FechaLocal
import cl.payroutine.viewmodel.observarInicio
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InicioRoomTest {
    @Test
    fun inicioActualizaTotalesMovimientosYPagosDesdeRoom() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val base = Room.inMemoryDatabaseBuilder(contexto, BaseDatosPayRoutine::class.java).build()
        val repositorio = RepositorioFinanzas(base)
        val zona = ZoneId.of("America/Santiago")
        val fechas = MutableStateFlow(FechaLocal(LocalDate.of(2026, 1, 10), zona))
        val estados = Channel<DatosInicio>(Channel.CONFLATED)
        val coleccion = launch { repositorio.observarInicio(fechas).collect { estados.send(it) } }

        suspend fun esperar(condicion: (DatosInicio) -> Boolean): DatosInicio =
            withTimeout(15000L) { estados.receiveAsFlow().first { condicion(it) } }

        try {
            val vacio = esperar { true }
            assertEquals(BigDecimal.ZERO, vacio.ingresos)
            assertEquals(BigDecimal.ZERO, vacio.gastos)
            assertEquals(BigDecimal.ZERO, vacio.saldo)
            assertTrue(vacio.recientes.isEmpty())
            assertTrue(vacio.pagosPendientes.isEmpty())

            val alimentacion = Categoria(nombre = "Alimentación", tipo = TipoMovimiento.Gasto)
            val categoriaGasto = repositorio.insertar(alimentacion).toInt()
            val categoriaIngreso = repositorio.insertar(Categoria(nombre = "Sueldo", tipo = TipoMovimiento.Ingreso)).toInt()
            val fechaRepetida = Date.from(Instant.parse("2026-01-09T12:00:00Z"))
            val gasto = Movimiento(tipo = TipoMovimiento.Gasto, monto = 24000.25, fecha = fechaRepetida, categoriaId = categoriaGasto)
            val idGasto = repositorio.insertar(gasto).toInt()
            val idIngreso = repositorio.insertar(gasto.copy(tipo = TipoMovimiento.Ingreso, monto = 120000.75, categoriaId = categoriaIngreso)).toInt()
            // En Santiago este instante todavía pertenece al 31 de diciembre.
            val idDiciembre = repositorio.insertar(gasto.copy(monto = 15000.0, fecha = Date.from(Instant.parse("2026-01-01T01:00:00Z")))).toInt()
            repositorio.insertar(gasto.copy(monto = 5000.0, fecha = Date.from(Instant.parse("2025-12-30T12:00:00Z"))))

            val conMovimientos = esperar { it.movimientos.size == 4 }
            assertEquals(BigDecimal("120000.75"), conMovimientos.ingresos)
            assertEquals(BigDecimal("24000.25"), conMovimientos.gastos)
            assertEquals(BigDecimal("96000.50"), conMovimientos.saldo)
            assertEquals(listOf(idIngreso, idGasto, idDiciembre), conMovimientos.recientes.map { it.movimiento.id })
            assertEquals(listOf("Sueldo", "Alimentación", "Alimentación"), conMovimientos.recientes.map { it.categoria })

            val vencido = pago("Electricidad", LocalDate.of(2025, 12, 30), zona)
            val venceHoy = pago("Internet", LocalDate.of(2026, 1, 10), zona)
            val proximo = pago("Tarjeta de crédito", LocalDate.of(2026, 1, 14), zona)
            val finDeMes = pago("Arriendo", LocalDate.of(2026, 1, 31), zona)
            val otroMes = pago("Seguro", LocalDate.of(2026, 2, 1), zona)
            val idVencido = repositorio.insertar(vencido).toInt()
            val idHoy = repositorio.insertar(venceHoy).toInt()
            val idProximo = repositorio.insertar(proximo).toInt()
            val idFinDeMes = repositorio.insertar(finDeMes).toInt()
            repositorio.insertar(otroMes)
            repositorio.insertar(vencido.copy(descripcion = "Pagado", pagado = true))

            val conPagos = esperar { it.pagosPendientes.size == 3 }
            assertEquals(listOf("Electricidad", "Internet", "Tarjeta de crédito"), conPagos.pagosPendientes.map { it.pago.descripcion })
            assertEquals(listOf(-11L, 0L, 4L), conPagos.pagosPendientes.map { it.diasParaVencer })

            repositorio.actualizar(gasto.copy(id = idGasto, monto = 30000.0))
            repositorio.actualizar(alimentacion.copy(id = categoriaGasto, nombre = "Compras"))
            repositorio.actualizar(vencido.copy(id = idVencido, pagado = true))
            val actualizado = esperar {
                it.gastos == BigDecimal("30000.0") &&
                    it.recientes[1].categoria == "Compras" &&
                    it.pagosPendientes.none { pago -> pago.pago.id == idVencido }
            }
            assertEquals(BigDecimal("90000.75"), actualizado.saldo)
            assertEquals(listOf("Internet", "Tarjeta de crédito", "Arriendo"), actualizado.pagosPendientes.map { it.pago.descripcion })

            repositorio.actualizar(venceHoy.copy(id = idHoy, pagado = true))
            repositorio.actualizar(proximo.copy(id = idProximo, pagado = true))
            repositorio.actualizar(finDeMes.copy(id = idFinDeMes, pagado = true))
            esperar { it.pagosPendientes.isEmpty() }
            fechas.value = fechas.value.copy(hoy = LocalDate.of(2026, 2, 1))
            val febrero = esperar { it.fecha.hoy.monthValue == 2 }
            assertEquals(BigDecimal.ZERO, febrero.saldo)
            assertEquals("Seguro", febrero.pagosPendientes.single().pago.descripcion)
            assertEquals(0L, febrero.pagosPendientes.single().diasParaVencer)
            assertEquals(3, febrero.recientes.size)
        } finally {
            coleccion.cancelAndJoin()
            base.close()
        }
    }

    private fun pago(descripcion: String, fecha: LocalDate, zona: ZoneId) = PagoPendiente(
        descripcion = descripcion,
        monto = 120000.0,
        fechaVencimiento = Date.from(fecha.atStartOfDay(zona).toInstant()),
        esCuota = false,
        pagado = false,
    )
}
