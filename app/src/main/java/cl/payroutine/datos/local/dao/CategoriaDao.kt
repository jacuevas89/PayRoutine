package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cl.payroutine.datos.local.categoriasIniciales
import cl.payroutine.datos.local.entidad.Categoria
import cl.payroutine.datos.local.normalizarNombreCategoria
import kotlinx.coroutines.flow.Flow

data class CategoriaConUso(
    @Embedded val categoria: Categoria,
    val cantidadMovimientos: Int,
)

@Dao
interface CategoriaDao {
    @Query("SELECT * FROM Categoria ORDER BY nombre, id")
    fun observarTodos(): Flow<List<Categoria>>

    @Query("""
        SELECT Categoria.*, COUNT(Movimiento.id) AS cantidadMovimientos
        FROM Categoria LEFT JOIN Movimiento ON Movimiento.categoria_id = Categoria.id
        GROUP BY Categoria.id
        ORDER BY Categoria.nombre, Categoria.id
    """)
    fun observarConUso(): Flow<List<CategoriaConUso>>

    @Query("SELECT * FROM Categoria")
    suspend fun obtenerTodos(): List<Categoria>

    @Insert
    suspend fun insertarTodas(entidades: List<Categoria>)

    @Transaction
    suspend fun prepararIniciales() {
        val existentes = obtenerTodos().map {
            it.tipo to normalizarNombreCategoria(it.nombre)
        }.toSet()
        insertarTodas(categoriasIniciales.filter {
            (it.tipo to normalizarNombreCategoria(it.nombre)) !in existentes
        })
    }

    @Insert
    suspend fun insertar(entidad: Categoria): Long

    @Update
    suspend fun actualizar(entidad: Categoria): Int

    @Delete
    suspend fun eliminar(entidad: Categoria): Int
}
