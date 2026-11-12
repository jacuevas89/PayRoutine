package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.payroutine.datos.local.entidad.Categoria
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM Categoria ORDER BY nombre, id")
    fun observarTodos(): Flow<List<Categoria>>

    @Insert
    suspend fun insertar(entidad: Categoria): Long

    @Update
    suspend fun actualizar(entidad: Categoria): Int

    @Delete
    suspend fun eliminar(entidad: Categoria): Int
}
