package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.payroutine.datos.local.entidad.PagoPendiente
import kotlinx.coroutines.flow.Flow

data class PagoConCategoria(
    @Embedded val pago: PagoPendiente,
    val nombreCategoria: String?,
    val nombreObligacion: String? = null,
)

@Dao
interface PagoPendienteDao {
    @Query("DELETE FROM PagoPendiente")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM PagoPendiente ORDER BY fecha_vencimiento, id")
    fun observarTodos(): Flow<List<PagoPendiente>>

    @Query("""
        SELECT p.*, c.nombre AS nombreCategoria, o.nombre AS nombreObligacion
        FROM PagoPendiente p
        LEFT JOIN ObligacionPago o ON o.id = p.obligacion_id
        LEFT JOIN Categoria c ON c.id = o.categoria_id
        ORDER BY p.fecha_vencimiento, p.id
    """)
    fun observarConCategoria(): Flow<List<PagoConCategoria>>

    @Query("SELECT * FROM PagoPendiente WHERE id = :id")
    suspend fun buscarPorId(id: Int): PagoPendiente?

    @Query("SELECT id FROM PagoPendiente WHERE movimiento_id = :movimientoId")
    suspend fun buscarIdPorMovimiento(movimientoId: Int): Int?

    @Query("""
        UPDATE PagoPendiente SET pagado = 1, movimiento_id = :movimientoId
        WHERE id = :id AND pagado = 0 AND movimiento_id IS NULL
    """)
    suspend fun marcarPagado(id: Int, movimientoId: Int): Int

    @Query("""
        UPDATE PagoPendiente SET pagado = 0, movimiento_id = NULL
        WHERE id = :id AND pagado = 1 AND movimiento_id = :movimientoId
    """)
    suspend fun deshacerPago(id: Int, movimientoId: Int): Int

    @Insert
    suspend fun insertar(entidad: PagoPendiente): Long

    @Insert
    suspend fun insertarTodas(entidades: List<PagoPendiente>): List<Long>

    @Update
    suspend fun actualizar(entidad: PagoPendiente): Int

    @Delete
    suspend fun eliminar(entidad: PagoPendiente): Int
}
