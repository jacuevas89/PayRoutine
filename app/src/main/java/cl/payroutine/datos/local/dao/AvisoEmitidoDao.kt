package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.payroutine.datos.local.entidad.AvisoEmitido
import kotlinx.coroutines.flow.Flow

@Dao
interface AvisoEmitidoDao {
    @Query("SELECT * FROM AvisoEmitido WHERE usuarioId = :usuarioId ORDER BY fecha DESC, id DESC")
    fun observar(usuarioId: String): Flow<List<AvisoEmitido>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun guardar(aviso: AvisoEmitido)

    @Query("UPDATE AvisoEmitido SET leido = 1 WHERE usuarioId = :usuarioId AND id IN (:ids)")
    suspend fun marcarLeidos(usuarioId: String, ids: List<String>)

    @Query("DELETE FROM AvisoEmitido WHERE usuarioId = :usuarioId AND id = :id")
    suspend fun eliminar(usuarioId: String, id: String)

    @Query("DELETE FROM AvisoEmitido WHERE usuarioId = :usuarioId")
    suspend fun eliminarTodos(usuarioId: String)
}
