package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.payroutine.datos.local.entidad.Presupuesto
import kotlinx.coroutines.flow.Flow

@Dao
interface PresupuestoDao {
    @Query("SELECT * FROM Presupuesto ORDER BY anio DESC, mes DESC, id")
    fun observarTodos(): Flow<List<Presupuesto>>

    @Insert
    suspend fun insertar(entidad: Presupuesto): Long

    @Update
    suspend fun actualizar(entidad: Presupuesto): Int

    @Delete
    suspend fun eliminar(entidad: Presupuesto): Int
}
