package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.payroutine.datos.local.entidad.Movimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {
    @Query("SELECT * FROM Movimiento ORDER BY fecha DESC, id DESC")
    fun observarTodos(): Flow<List<Movimiento>>

    @Insert
    suspend fun insertar(entidad: Movimiento): Long

    @Update
    suspend fun actualizar(entidad: Movimiento): Int

    @Delete
    suspend fun eliminar(entidad: Movimiento): Int
}
