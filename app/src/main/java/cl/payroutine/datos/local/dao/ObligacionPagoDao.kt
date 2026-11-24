package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.payroutine.datos.local.entidad.ObligacionPago

@Dao
interface ObligacionPagoDao {
    @Insert
    suspend fun insertar(obligacion: ObligacionPago): Long

    @Query("SELECT categoria_id FROM ObligacionPago WHERE id = :id")
    suspend fun obtenerCategoria(id: Int): Int?

    @Query("DELETE FROM ObligacionPago")
    suspend fun eliminarTodas()
}
