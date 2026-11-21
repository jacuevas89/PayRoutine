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
)

@Dao
interface MovimientoDao {
    @Query("DELETE FROM Movimiento")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM Movimiento ORDER BY fecha DESC, id DESC")
    fun observarTodos(): Flow<List<Movimiento>>

    @Query("""
        SELECT Movimiento.*, Categoria.nombre AS categoria
        FROM Movimiento INNER JOIN Categoria ON Categoria.id = Movimiento.categoria_id
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
