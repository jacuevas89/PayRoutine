package cl.payroutine.datos.repositorio

import androidx.room.withTransaction
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.categoriasPredefinidas
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.ObligacionPago
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
import kotlinx.coroutines.flow.map

class RepositorioFinanzas(private val baseDatos: BaseDatosPayRoutine) {
    private val categoriaDao = baseDatos.categoriaDao()
    private val movimientoDao = baseDatos.movimientoDao()
    private val presupuestoDao = baseDatos.presupuestoDao()
    private val pagoPendienteDao = baseDatos.pagoPendienteDao()
    private val obligacionPagoDao = baseDatos.obligacionPagoDao()
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
    fun observarPagosConCategoria() = pagoPendienteDao.observarConCategoria()
    fun observarPagosPendientes() = observarPagosConCategoria().map { pagos -> pagos.filterNot { it.pago.pagado } }

    suspend fun registrarPagoUnico(datos: NuevoPagoUnico): Long = baseDatos.withTransaction {
        require(datos.monto in 1..MONTO_MAXIMO_EXACTO) { "El monto debe ser un número positivo de pesos enteros" }
        val nombre = datos.nombre.trim()
        val vencimiento = Date.from(datos.vencimiento.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val obligacionId = registrarObligacion(nombre, datos.categoriaId)
        pagoPendienteDao.insertar(
            PagoPendiente(
                descripcion = nombre,
                monto = datos.monto.toDouble(),
                fechaVencimiento = vencimiento,
                esCuota = false,
                pagado = false,
                obligacionId = obligacionId,
            ),
        )
    }

    internal suspend fun registrarDeuda(datos: NuevaDeudaEnCuotas): Long = baseDatos.withTransaction {
        validarCronogramaCuotas(datos)
        val nombre = datos.nombre.trim()
        val obligacionId = registrarObligacion(nombre, datos.categoriaId)
        val zona = ZoneId.systemDefault()
        val cantidad = datos.cronograma.size
        val vencimientos = datos.cronograma.mapIndexed { indice, cuota ->
            val numero = indice + 1
            PagoPendiente(
                descripcion = "$nombre · Cuota $numero de $cantidad",
                monto = cuota.monto.toDouble(),
                fechaVencimiento = Date.from(cuota.vencimiento.atStartOfDay(zona).toInstant()),
                esCuota = true,
                cuotaActual = numero,
                totalCuotas = cantidad,
                pagado = numero <= datos.cuotasPagadas,
                obligacionId = obligacionId,
            )
        }
        pagoPendienteDao.insertarTodas(vencimientos).first()
    }

    private suspend fun registrarObligacion(nombre: String, categoriaId: Int): Int {
        require(nombre.isNotBlank()) { "El nombre del pago es obligatorio" }
        val categoria = categoriaDao.buscarPorId(categoriaId)
        require(categoria?.tipo == TipoMovimiento.Gasto) { "Selecciona una categoría de gasto" }
        return obligacionPagoDao.insertar(ObligacionPago(nombre = nombre, categoriaId = categoriaId)).toInt()
    }

    internal suspend fun pagarVencimientos(ids: List<Int>, fecha: LocalDate, nota: String) = baseDatos.withTransaction {
        require(ids.isNotEmpty()) { "Selecciona al menos un vencimiento." }
        val fechaEfectiva = fechaMovimiento(fecha)
        val vencimientos = ids.distinct().map { id ->
            requireNotNull(pagoPendienteDao.buscarPorId(id)) { "Un vencimiento ya no está disponible." }
        }
        if (vencimientos.all { it.pagado && it.movimientoId != null }) return@withTransaction
        require(vencimientos.none { it.pagado }) { "La selección cambió. Vuelve a elegir los vencimientos pendientes." }

        for (pago in vencimientos) {
            val categoriaId = pago.obligacionId?.let { obligacionPagoDao.obtenerCategoria(it) }
            requireNotNull(categoriaId) { "«${pago.descripcion}» necesita una categoría de gasto." }
            require(categoriaDao.buscarPorId(categoriaId)?.tipo == TipoMovimiento.Gasto) {
                "La categoría de «${pago.descripcion}» ya no está disponible."
            }
            require(pago.monto.isFinite() && pago.monto > 0) { "El monto de «${pago.descripcion}» debe ser positivo." }
            val movimientoId = movimientoDao.insertar(
                Movimiento(
                    nombre = pago.descripcion, tipo = TipoMovimiento.Gasto, monto = pago.monto,
                    fecha = fechaEfectiva, categoriaId = categoriaId, nota = nota.trim().takeIf { it.isNotEmpty() },
                ),
            )
            require(pagoPendienteDao.marcarPagado(pago.id, Math.toIntExact(movimientoId)) == 1) {
                "La selección cambió. Vuelve a elegir los vencimientos pendientes."
            }
        }
    }

    internal suspend fun deshacerPago(id: Int, movimientoId: Int) = baseDatos.withTransaction {
        if (pagoPendienteDao.deshacerPago(id, movimientoId) == 1) {
            check(movimientoDao.eliminarPorId(movimientoId) == 1)
        }
    }

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
        pagoPendienteDao.eliminarTodos()
        obligacionPagoDao.eliminarTodas()
        movimientoDao.eliminarTodos()
        presupuestoDao.eliminarTodos()
        mensualDao.eliminarTodos()
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
        val movimiento = if (pagoPendienteDao.buscarIdPorMovimiento(id) == null) {
            prepararMovimiento(datos, anterior)
        } else {
            anterior.copy(
                fecha = fechaMovimiento(datos.fecha, anterior.fecha),
                nota = datos.nota.trim().takeIf { it.isNotEmpty() },
            )
        }
        movimientoDao.actualizar(movimiento) == 1
    }

    suspend fun eliminarMovimiento(id: Int): Int = movimientoDao.eliminarPorId(id)

    private suspend fun prepararMovimiento(datos: NuevoMovimiento, anterior: Movimiento? = null): Movimiento {
        require(datos.nombre.isNotBlank()) { "El nombre del movimiento es obligatorio" }
        require(datos.monto in 1..MONTO_MAXIMO_EXACTO) { "El monto debe ser un número positivo de pesos enteros" }
        val categoria = categoriaDao.buscarPorId(datos.categoriaId)
        require(categoria != null && categoria.tipo == datos.tipo) { "La categoría no corresponde al tipo de movimiento" }
        return Movimiento(
            id = anterior?.id ?: 0,
            nombre = datos.nombre.trim(),
            tipo = datos.tipo,
            monto = datos.monto.toDouble(),
            fecha = fechaMovimiento(datos.fecha, anterior?.fecha),
            categoriaId = datos.categoriaId,
            nota = datos.nota.trim().takeIf { it.isNotEmpty() },
        )
    }

    private fun fechaMovimiento(fecha: LocalDate, anterior: Date? = null): Date {
        val zona = ZoneId.systemDefault()
        require(fecha <= LocalDate.now(zona)) { "La fecha no puede ser futura" }
        return anterior?.takeIf { it.toInstant().atZone(zona).toLocalDate() == fecha }
            ?: Date.from(fecha.atStartOfDay(zona).toInstant())
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
