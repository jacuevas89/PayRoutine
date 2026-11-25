package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cl.payroutine.datos.local.entidad.ConsejoAhorro
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsejoAhorroDao {
    @Query("SELECT * FROM ConsejoAhorro ORDER BY orden, id")
    fun observar(): Flow<List<ConsejoAhorro>>

    @Upsert
    suspend fun guardar(consejos: List<ConsejoAhorro>)
}
