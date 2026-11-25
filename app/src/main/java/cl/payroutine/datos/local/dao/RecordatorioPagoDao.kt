package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.PreferenciasAvisosPago
import cl.payroutine.datos.local.entidad.RecordatorioPago
import kotlinx.coroutines.flow.Flow

data class PagoConRecordatorio(
    @Embedded val pago: PagoPendiente,
    @Embedded(prefix = "aviso_") val recordatorio: RecordatorioPago,
)

@Dao
interface RecordatorioPagoDao {
    @Query("SELECT * FROM PreferenciasAvisosPago WHERE usuarioId = :usuarioId")
    fun observarPreferencias(usuarioId: String): Flow<PreferenciasAvisosPago?>

    @Query("SELECT * FROM PreferenciasAvisosPago LIMIT 1")
    suspend fun preferencias(): PreferenciasAvisosPago?

    @Upsert
    suspend fun guardarPreferencias(preferencias: PreferenciasAvisosPago)

    @Query("SELECT vencimientoId FROM RecordatorioPago")
    fun seleccionados(): Flow<List<Int>>

    @Query("""
        SELECT p.*, r.vencimientoId AS aviso_vencimientoId, r.vencimientoProgramado AS aviso_vencimientoProgramado,
               r.diaVencimientoProgramado AS aviso_diaVencimientoProgramado,
               r.diaProgramado AS aviso_diaProgramado, r.ultimoAviso AS aviso_ultimoAviso
        FROM RecordatorioPago r INNER JOIN PagoPendiente p ON p.id = r.vencimientoId
        WHERE p.pagado = 0
        ORDER BY p.id
    """)
    suspend fun pendientesSeleccionados(): List<PagoConRecordatorio>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seleccionar(recordatorios: List<RecordatorioPago>)

    @Query("DELETE FROM RecordatorioPago WHERE vencimientoId IN (:ids)")
    suspend fun deseleccionar(ids: List<Int>)

    @Query("UPDATE RecordatorioPago SET diaProgramado = NULL")
    suspend fun invalidarProgramaciones()

    @Query("UPDATE RecordatorioPago SET diaProgramado = NULL, vencimientoProgramado = NULL, diaVencimientoProgramado = NULL, ultimoAviso = NULL WHERE vencimientoId = :id")
    suspend fun reiniciarVencimiento(id: Int)

    @Query("""
        UPDATE RecordatorioPago SET vencimientoProgramado = :vencimiento, diaVencimientoProgramado = :diaVencimiento,
            diaProgramado = :dia, ultimoAviso = :ultimo
        WHERE vencimientoId = :id
    """)
    suspend fun actualizarPrograma(id: Int, vencimiento: Long, diaVencimiento: Long, dia: Long?, ultimo: Long?)
}
