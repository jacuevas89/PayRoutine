package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.payroutine.datos.local.entidad.PagoPendiente
import kotlinx.coroutines.flow.Flow

@Dao
interface PagoPendienteDao {
    @Query("SELECT * FROM PagoPendiente ORDER BY fecha_vencimiento, id")
    fun observarTodos(): Flow<List<PagoPendiente>>

    @Insert
    suspend fun insertar(entidad: PagoPendiente): Long

    @Update
    suspend fun actualizar(entidad: PagoPendiente): Int

    @Delete
    suspend fun eliminar(entidad: PagoPendiente): Int
}
