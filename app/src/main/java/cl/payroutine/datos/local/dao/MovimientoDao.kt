package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.payroutine.datos.local.entidad.Movimiento
import kotlinx.coroutines.flow.Flow

data class MovimientoConCategoria(
    @Embedded val movimiento: Movimiento,
    val categoria: String,
    val pagoId: Int? = null,
)

@Dao
interface MovimientoDao {
    @Query("DELETE FROM Movimiento")
    suspend fun eliminarTodos()

    @Query("DELETE FROM Movimiento WHERE id = :id")
    suspend fun eliminarPorId(id: Int): Int

    @Query("SELECT * FROM Movimiento WHERE id = :id")
    suspend fun buscarPorId(id: Int): Movimiento?

    @Query("SELECT * FROM Movimiento ORDER BY fecha DESC, id DESC")
    fun observarTodos(): Flow<List<Movimiento>>

    @Query("SELECT * FROM Movimiento WHERE fecha >= :desde AND fecha < :hasta")
    suspend fun obtenerEntre(desde: Long, hasta: Long): List<Movimiento>

    @Query("""
        SELECT Movimiento.*, COALESCE(Categoria.nombre, 'Sin categoría') AS categoria, PagoPendiente.id AS pagoId
        FROM Movimiento LEFT JOIN Categoria ON Categoria.id = Movimiento.categoria_id
        LEFT JOIN PagoPendiente ON PagoPendiente.movimiento_id = Movimiento.id
        ORDER BY Movimiento.fecha DESC, Movimiento.id DESC
    """)
    fun observarConCategoria(): Flow<List<MovimientoConCategoria>>

    @Insert
    suspend fun insertar(entidad: Movimiento): Long

    @Update
    suspend fun actualizar(entidad: Movimiento): Int

    @Delete
    suspend fun eliminar(entidad: Movimiento): Int
}
