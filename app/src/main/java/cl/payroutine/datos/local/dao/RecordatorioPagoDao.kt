package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import cl.payroutine.datos.local.entidad.PagoPendiente
import cl.payroutine.datos.local.entidad.ConfiguracionAvisoPago
import cl.payroutine.datos.local.entidad.RecordatorioPago
import kotlinx.coroutines.flow.Flow

data class PagoConRecordatorio(
    @Embedded val pago: PagoPendiente,
    @Embedded(prefix = "aviso_") val recordatorio: RecordatorioPago,
    @Embedded(prefix = "cfg_") val configuracion: ConfiguracionAvisoPago,
)

data class ConfiguracionConProgramacion(
    @Embedded val configuracion: ConfiguracionAvisoPago,
    val proximoDia: Long?,
)

@Dao
interface RecordatorioPagoDao {
    @Query("""
        SELECT c.*, (
            SELECT MIN(r.diaProgramado) FROM RecordatorioPago r JOIN PagoPendiente p ON p.id = r.vencimientoId
            WHERE p.pagado = 0 AND (p.obligacion_id = c.obligacionId OR p.id = c.pagoAnteriorId)
        ) AS proximoDia
        FROM ConfiguracionAvisoPago c WHERE c.usuarioId = :usuarioId ORDER BY c.clave
    """)
    fun observarConfiguraciones(usuarioId: String): Flow<List<ConfiguracionConProgramacion>>

    @Query("SELECT * FROM ConfiguracionAvisoPago WHERE clave = :clave")
    suspend fun configuracion(clave: String): ConfiguracionAvisoPago?

    @Upsert
    suspend fun guardarPreferencias(preferencias: ConfiguracionAvisoPago)

    @Query("DELETE FROM ConfiguracionAvisoPago WHERE clave = :clave")
    suspend fun eliminarConfiguracion(clave: String)

    @Query("SELECT vencimientoId FROM RecordatorioPago")
    fun seleccionados(): Flow<List<Int>>

    @Query("""
        SELECT p.*, r.vencimientoId AS aviso_vencimientoId, r.vencimientoProgramado AS aviso_vencimientoProgramado,
               r.diaVencimientoProgramado AS aviso_diaVencimientoProgramado,
               r.diaProgramado AS aviso_diaProgramado, r.ultimoAviso AS aviso_ultimoAviso,
               c.clave AS cfg_clave, c.usuarioId AS cfg_usuarioId, c.obligacionId AS cfg_obligacionId,
               c.pagoAnteriorId AS cfg_pagoAnteriorId, c.activo AS cfg_activo,
               c.anticipacion AS cfg_anticipacion, c.minuto AS cfg_minuto, c.frecuencia AS cfg_frecuencia
        FROM RecordatorioPago r INNER JOIN PagoPendiente p ON p.id = r.vencimientoId
        INNER JOIN ConfiguracionAvisoPago c ON
            (p.obligacion_id IS NOT NULL AND c.obligacionId = p.obligacion_id) OR c.pagoAnteriorId = p.id
        WHERE p.pagado = 0 AND c.activo = 1
        ORDER BY p.id
    """)
    suspend fun pendientesSeleccionados(): List<PagoConRecordatorio>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun seleccionar(recordatorios: List<RecordatorioPago>)

    @Query("DELETE FROM RecordatorioPago WHERE vencimientoId IN (:ids)")
    suspend fun deseleccionar(ids: List<Int>)

    @Query("UPDATE RecordatorioPago SET diaProgramado = NULL WHERE vencimientoId IN (:ids)")
    suspend fun invalidarProgramaciones(ids: List<Int>)

    @Query("UPDATE RecordatorioPago SET diaProgramado = NULL, vencimientoProgramado = NULL, diaVencimientoProgramado = NULL, ultimoAviso = NULL WHERE vencimientoId = :id")
    suspend fun reiniciarVencimiento(id: Int)

    @Query("""
        UPDATE RecordatorioPago SET vencimientoProgramado = :vencimiento, diaVencimientoProgramado = :diaVencimiento,
            diaProgramado = :dia, ultimoAviso = :ultimo
        WHERE vencimientoId = :id
    """)
    suspend fun actualizarPrograma(id: Int, vencimiento: Long, diaVencimiento: Long, dia: Long?, ultimo: Long?)
}
