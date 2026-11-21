package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import cl.payroutine.datos.local.entidad.Usuario
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Query("DELETE FROM Usuario")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM Usuario LIMIT 1")
    fun observar(): Flow<Usuario?>

    @Query("SELECT EXISTS(SELECT 1 FROM Usuario)")
    suspend fun existe(): Boolean

    @Insert
    suspend fun insertar(usuario: Usuario)

    @Transaction
    suspend fun registrarSiFalta(nombreCompleto: String, apodo: String) {
        if (!existe()) {
            insertar(Usuario(UUID.randomUUID().toString(), nombreCompleto, apodo))
        }
    }

    @Query("UPDATE Usuario SET nombre_completo = :nombreCompleto, apodo = :apodo WHERE id = :id")
    suspend fun actualizarDatos(id: String, nombreCompleto: String, apodo: String): Int
}
