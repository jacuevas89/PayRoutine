package cl.payroutine.datos.repositorio

import androidx.room.withTransaction
import cl.payroutine.datos.local.BaseDatosPayRoutine
import cl.payroutine.datos.local.categoriasPredefinidas
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.Movimiento
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.Presupuesto

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
        for ((tipo, nombres) in categoriasPredefinidas) {
            for (nombre in nombres) categoriaDao.insertarPredefinidaSiFalta(nombre, tipo)
        }
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
