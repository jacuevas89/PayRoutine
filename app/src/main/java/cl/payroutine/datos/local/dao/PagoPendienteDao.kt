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
)

@Dao
interface PagoPendienteDao {
    @Query("DELETE FROM PagoPendiente")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM PagoPendiente ORDER BY fecha_vencimiento, id")
    fun observarTodos(): Flow<List<PagoPendiente>>

    @Query("""
        SELECT p.*, c.nombre AS nombreCategoria
        FROM PagoPendiente p
        LEFT JOIN ObligacionPago o ON o.id = p.obligacion_id
        LEFT JOIN Categoria c ON c.id = o.categoria_id
        WHERE p.pagado = 0
        ORDER BY p.fecha_vencimiento, p.id
    """)
    fun observarPendientesConCategoria(): Flow<List<PagoConCategoria>>

    @Insert
    suspend fun insertar(entidad: PagoPendiente): Long

    @Update
    suspend fun actualizar(entidad: PagoPendiente): Int

    @Delete
    suspend fun eliminar(entidad: PagoPendiente): Int
}
