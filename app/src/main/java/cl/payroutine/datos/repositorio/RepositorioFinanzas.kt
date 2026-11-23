package cl.payroutine.datos.repositorio

import androidx.room.withTransaction
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.categoriasPredefinidas
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.Presupuesto
import cl.payroutine.datos.local.entidad.TipoMovimiento
import cl.payroutine.datos.local.normalizarNombreCategoria
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class RepositorioFinanzas(private val baseDatos: BaseDatosPayRoutine) {
    private val categoriaDao = baseDatos.categoriaDao()
    private val movimientoDao = baseDatos.movimientoDao()
    private val presupuestoDao = baseDatos.presupuestoDao()
    private val pagoPendienteDao = baseDatos.pagoPendienteDao()
    private val usuarioDao = baseDatos.usuarioDao()

    fun observarUsuario() = usuarioDao.observar()

    suspend fun registrarUsuario(nombreCompleto: String, apodo: String) {
        require(nombreCompleto.isNotBlank() && apodo.isNotBlank())
        baseDatos.withTransaction {
            cargarCategoriasPredefinidas()
            usuarioDao.registrarSiFalta(nombreCompleto.trim(), apodo.trim())
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

    suspend fun eliminarTodosLosDatos() = baseDatos.withTransaction {
        movimientoDao.eliminarTodos()
        presupuestoDao.eliminarTodos()
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
