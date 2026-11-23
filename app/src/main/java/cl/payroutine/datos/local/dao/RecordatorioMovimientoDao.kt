package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cl.payroutine.datos.local.entidad.RecordatorioMovimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordatorioMovimientoDao {
    @Query("SELECT * FROM RecordatorioMovimiento WHERE usuario_id = :usuarioId")
    fun observar(usuarioId: String): Flow<RecordatorioMovimiento?>

    @Query("SELECT * FROM RecordatorioMovimiento LIMIT 1")
    suspend fun obtener(): RecordatorioMovimiento?

    @Upsert
    suspend fun guardar(recordatorio: RecordatorioMovimiento)
}
