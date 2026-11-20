package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import cl.payroutine.datos.local.INSERTAR_CATEGORIA_PREDEFINIDA
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.entidad.TipoMovimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {
    @Query("DELETE FROM Categoria WHERE es_predefinida = 0")
    suspend fun eliminarPersonalizadas()

    @Query(INSERTAR_CATEGORIA_PREDEFINIDA)
    suspend fun insertarPredefinidaSiFalta(nombre: String, tipo: TipoMovimiento)

    @Query("SELECT * FROM Categoria ORDER BY nombre, id")
    fun observarTodos(): Flow<List<Categoria>>

    @Insert
    suspend fun insertar(entidad: Categoria): Long

    @Update
    suspend fun actualizar(entidad: Categoria): Int

    @Delete
    suspend fun eliminar(entidad: Categoria): Int
}
