package cl.payroutine.datos.repositorio

import androidx.room.withTransaction
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.categoriasPredefinidas
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.Presupuesto
import cl.payroutine.datos.local.entidad.RecordatorioMovimiento
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.local.entidad.ModalidadPresupuesto
import cl.payroutine.datos.local.entidad.PreferenciasPresupuesto
import cl.payroutine.datos.local.entidad.PresupuestoMensual
import cl.payroutine.datos.local.entidad.indice
import cl.payroutine.datos.local.entidad.mesDesdeIndice
import cl.payroutine.datos.local.normalizarNombreCategoria
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.math.RoundingMode
import java.util.Date

class RepositorioFinanzas(private val baseDatos: BaseDatosPayRoutine) {
    private val categoriaDao = baseDatos.categoriaDao()
    private val movimientoDao = baseDatos.movimientoDao()
    private val presupuestoDao = baseDatos.presupuestoDao()
    private val pagoPendienteDao = baseDatos.pagoPendienteDao()
    private val usuarioDao = baseDatos.usuarioDao()
    private val recordatorioDao = baseDatos.recordatorioMovimientoDao()
    private val mensualDao = baseDatos.presupuestoMensualDao()

    fun observarUsuario() = usuarioDao.observar()

    fun observarRecordatorio(usuarioId: String) = recordatorioDao.observar(usuarioId)
    suspend fun obtenerRecordatorio() = recordatorioDao.obtener()
    suspend fun guardarRecordatorio(recordatorio: RecordatorioMovimiento) = recordatorioDao.guardar(recordatorio)

    suspend fun registrarUsuario(nombreCompleto: String, apodo: String) {
        require(nombreCompleto.isNotBlank() && apodo.isNotBlank())
        baseDatos.withTransaction {
            cargarCategoriasPredefinidas()
            val nuevoUsuario = !usuarioDao.existe()
            usuarioDao.registrarSiFalta(nombreCompleto.trim(), apodo.trim())
            if (nuevoUsuario) {
                mensualDao.guardarPreferencias(
                    PreferenciasPresupuesto(requireNotNull(usuarioDao.idActual()), configuracionPendiente = true),
                )
            }
        }
    }

    suspend fun actualizarDatosUsuario(id: String, nombreCompleto: String, apodo: String): Int {
        require(nombreCompleto.isNotBlank() && apodo.isNotBlank())
        return usuarioDao.actualizarDatos(id, nombreCompleto.trim(), apodo.trim())
    }

    fun observarCategorias() = categoriaDao.observarTodos()
    fun observarCategoriasConUso() = categoriaDao.observarConUso()
    suspend fun prepararCategoriasIniciales() = categoriaDao.prepararIniciales()

    /** Devuelve false si el nombre ya está en uso en este tipo de categoría. */
    suspend fun crearCategoriaPersonalizada(nombre: String, tipo: TipoMovimiento): Boolean = baseDatos.withTransaction {
        require(nombre.isNotBlank())
        val clave = normalizarNombreCategoria(nombre)
        val reservado = categoriasPredefinidas[tipo].orEmpty().any { normalizarNombreCategoria(it) == clave }
        if (reservado || categoriaDao.obtenerTodos().any { it.tipo == tipo && normalizarNombreCategoria(it.nombre) == clave }) {
            return@withTransaction false
        }
        categoriaDao.insertar(Categoria(nombre = nombre.trim(), tipo = tipo, esPredefinida = false))
        true
    }

    fun observarMovimientos() = movimientoDao.observarTodos()
    fun observarMovimientosConCategoria() = movimientoDao.observarConCategoria()
    fun observarPresupuestos() = presupuestoDao.observarTodos()
    fun observarPagos() = pagoPendienteDao.observarTodos()
    internal fun observarPresupuestosMensuales() = mensualDao.observar()
    internal fun observarPreferenciasPresupuesto(usuarioId: String) = mensualDao.observarPreferencias(usuarioId)
    internal suspend fun preferenciasPresupuesto() = mensualDao.preferenciasActuales()
    internal suspend fun existePresupuesto(mes: YearMonth) = mensualDao.obtener(mes.year, mes.monthValue) != null

    internal suspend fun actualizarPreferenciasPresupuesto(
        usuarioId: String,
        cambiar: (PreferenciasPresupuesto) -> PreferenciasPresupuesto,
    ) = baseDatos.withTransaction {
        if (usuarioDao.idActual() != usuarioId) return@withTransaction
        val anteriores = mensualDao.preferencias(usuarioId) ?: PreferenciasPresupuesto(usuarioId)
        mensualDao.guardarPreferencias(cambiar(anteriores))
    }

    internal suspend fun guardarPresupuestoMensual(
        usuarioId: String,
        mes: YearMonth,
        limiteGeneral: Double?,
        limites: Map<Int, Double>,
    ) = baseDatos.withTransaction {
        check(usuarioDao.idActual() == usuarioId)
        require((limiteGeneral != null) xor limites.isNotEmpty())
        require((limiteGeneral?.let(::listOf) ?: limites.values).all { it.isFinite() && it > 0 })
        val categorias = categoriaDao.obtenerTodos().filter { it.tipo == TipoMovimiento.Gasto }.map { it.id }.toSet()
        require(categorias.containsAll(limites.keys))
        val anterior = mensualDao.obtener(mes.year, mes.monthValue)
        guardarPeriodo(mes, limiteGeneral, limites, anterior?.ingresoReferencia)
        val preferencias = mensualDao.preferencias(usuarioId) ?: PreferenciasPresupuesto(usuarioId)
        if (preferencias.configuracionPendiente) {
            mensualDao.guardarPreferencias(preferencias.copy(configuracionPendiente = false))
        }
    }

    internal suspend fun configurarAutomatico(
        usuarioId: String,
        activar: Boolean,
        usarMesAnterior: Boolean,
        actual: YearMonth,
        zona: ZoneId,
    ) = baseDatos.withTransaction {
        if (usuarioDao.idActual() != usuarioId) return@withTransaction
        val anteriores = mensualDao.preferencias(usuarioId) ?: PreferenciasPresupuesto(usuarioId)
        val primerMes = if (activar) anteriores.primerMesAutomatico ?: actual.plusMonths(1).indice() else null
        mensualDao.guardarPreferencias(anteriores.copy(configuracionPendiente = false, primerMesAutomatico = primerMes))
        if (activar && usarMesAnterior && !existePresupuesto(actual)) {
            generarDesde(actual, listOf(movimientosDelMes(actual.minusMonths(1), zona)))
        }
    }

    internal suspend fun generarPresupuestoAutomatico(actual: YearMonth, zona: ZoneId) = baseDatos.withTransaction {
        val primerMes = mensualDao.preferenciasActuales()?.primerMesAutomatico?.let(::mesDesdeIndice)
            ?: return@withTransaction
        if (actual <= primerMes || existePresupuesto(actual)) return@withTransaction
        val referencia = mutableListOf<List<Movimiento>>()
        var mes = actual.minusMonths(1)
        while (mes >= primerMes && referencia.size < 3) {
            val registros = movimientosDelMes(mes, zona)
            if (registros.isNotEmpty()) referencia.add(registros)
            mes = mes.minusMonths(1)
        }
        generarDesde(actual, referencia)
    }

    private suspend fun movimientosDelMes(mes: YearMonth, zona: ZoneId): List<Movimiento> =
        movimientoDao.obtenerEntre(
            mes.atDay(1).atStartOfDay(zona).toInstant().toEpochMilli(),
            mes.plusMonths(1).atDay(1).atStartOfDay(zona).toInstant().toEpochMilli(),
        )

    private suspend fun generarDesde(mes: YearMonth, referencia: List<List<Movimiento>>) {
        if (referencia.isEmpty()) return
        val movimientos = referencia.flatten()
        val limites = movimientos.filter { it.tipo == TipoMovimiento.Gasto }
            .groupBy { it.categoriaId }.mapValues { (_, gastos) -> promedio(gastos, referencia.size) }
            .filterValues { it > 0 }
        if (limites.isEmpty()) return
        val ingresos = promedio(movimientos.filter { it.tipo == TipoMovimiento.Ingreso }, referencia.size)
        guardarPeriodo(mes, null, limites, ingresos, generado = true)
    }

    private fun promedio(movimientos: List<Movimiento>, meses: Int): Double =
        movimientos.sumOf { it.monto.toBigDecimal() }
            .divide(meses.toBigDecimal(), 2, RoundingMode.HALF_UP).toDouble()

    private suspend fun guardarPeriodo(
        mes: YearMonth,
        limiteGeneral: Double?,
        limites: Map<Int, Double>,
        ingresos: Double?,
        generado: Boolean = false,
    ) {
        val modalidad = if (limiteGeneral == null) ModalidadPresupuesto.Categorias else ModalidadPresupuesto.General
        mensualDao.guardar(PresupuestoMensual(mes.year, mes.monthValue, modalidad, limiteGeneral, generado, ingresos))
        presupuestoDao.eliminarPeriodo(mes.year, mes.monthValue)
        presupuestoDao.insertarTodos(limites.map { (id, monto) ->
            Presupuesto(categoriaId = id, montoPresupuestado = monto, mes = mes.monthValue, anio = mes.year)
        })
    }

    suspend fun eliminarTodosLosDatos() = baseDatos.withTransaction {
        movimientoDao.eliminarTodos()
        presupuestoDao.eliminarTodos()
        mensualDao.eliminarTodos()
        pagoPendienteDao.eliminarTodos()
        categoriaDao.eliminarPersonalizadas()
        cargarCategoriasPredefinidas()
        usuarioDao.eliminarTodos()
    }

    private suspend fun cargarCategoriasPredefinidas() {
        categoriaDao.prepararIniciales()
    }

    suspend fun registrarMovimiento(datos: NuevoMovimiento): Long = baseDatos.withTransaction {
        movimientoDao.insertar(prepararMovimiento(datos))
    }

    suspend fun editarMovimiento(id: Int, datos: NuevoMovimiento): Boolean = baseDatos.withTransaction {
        val anterior = movimientoDao.buscarPorId(id) ?: return@withTransaction false
        require(datos.tipo == anterior.tipo) { "No se puede cambiar el tipo de movimiento" }
        movimientoDao.actualizar(prepararMovimiento(datos, anterior)) == 1
    }

    suspend fun eliminarMovimiento(id: Int): Int = movimientoDao.eliminarPorId(id)

    private suspend fun prepararMovimiento(datos: NuevoMovimiento, anterior: Movimiento? = null): Movimiento {
        require(datos.nombre.isNotBlank()) { "El nombre del movimiento es obligatorio" }
        require(datos.monto in 1..MONTO_MAXIMO_EXACTO) { "El monto debe ser un número positivo de pesos enteros" }
        val zona = ZoneId.systemDefault()
        require(datos.fecha <= LocalDate.now(zona)) { "La fecha no puede ser futura" }
        val categoria = categoriaDao.buscarPorId(datos.categoriaId)
        require(categoria != null && categoria.tipo == datos.tipo) { "La categoría no corresponde al tipo de movimiento" }
        val fecha = anterior?.fecha?.takeIf { it.toInstant().atZone(zona).toLocalDate() == datos.fecha }
            ?: Date.from(datos.fecha.atStartOfDay(zona).toInstant())
        return Movimiento(
            id = anterior?.id ?: 0,
            nombre = datos.nombre.trim(),
            tipo = datos.tipo,
            monto = datos.monto.toDouble(),
            fecha = fecha,
            categoriaId = datos.categoriaId,
            nota = datos.nota.trim().takeIf { it.isNotEmpty() },
        )
    }

    suspend fun insertar(categoria: Categoria) = categoriaDao.insertar(categoria)
    suspend fun insertar(movimiento: Movimiento) = movimientoDao.insertar(movimiento)
    suspend fun insertar(presupuesto: Presupuesto) = presupuestoDao.insertar(presupuesto)
    suspend fun insertar(pago: PagoPendiente) = pagoPendienteDao.insertar(pago)

    suspend fun actualizar(categoria: Categoria) = categoriaDao.actualizar(categoria)
    suspend fun actualizar(movimiento: Movimiento) = movimientoDao.actualizar(movimiento)
    suspend fun actualizar(presupuesto: Presupuesto) = presupuestoDao.actualizar(presupuesto)
    suspend fun actualizar(pago: PagoPendiente) = pagoPendienteDao.actualizar(pago)

    suspend fun eliminar(categoria: Categoria) = categoriaDao.eliminar(categoria)
    suspend fun eliminar(movimiento: Movimiento) = movimientoDao.eliminar(movimiento)
    suspend fun eliminar(presupuesto: Presupuesto) = presupuestoDao.eliminar(presupuesto)
    suspend fun eliminar(pago: PagoPendiente) = pagoPendienteDao.eliminar(pago)
}
