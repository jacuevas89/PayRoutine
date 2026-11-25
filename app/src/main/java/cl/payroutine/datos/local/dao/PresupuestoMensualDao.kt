package cl.payroutine.datos.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Upsert
import cl.payroutine.datos.local.entidad.PreferenciasPresupuesto
import cl.payroutine.datos.local.entidad.PresupuestoMensual
import kotlinx.coroutines.flow.Flow

data class FilaPresupuestoMensual(
    @Embedded val cabecera: PresupuestoMensual,
    val categoriaId: Int?,
    val limite: Long?,
)

@Dao
interface PresupuestoMensualDao {
    @Query("""
        SELECT mensual.*, detalle.categoria_id AS categoriaId, detalle.monto_presupuestado AS limite
        FROM PresupuestoMensual AS mensual LEFT JOIN Presupuesto AS detalle
        ON detalle.anio = mensual.anio AND detalle.mes = mensual.mes AND mensual.modalidad = 'Categorias'
        ORDER BY detalle.id
    """)
    fun observar(): Flow<List<FilaPresupuestoMensual>>

    @Query("SELECT * FROM PresupuestoMensual WHERE anio = :anio AND mes = :mes")
    suspend fun obtener(anio: Int, mes: Int): PresupuestoMensual?

    @Upsert
    suspend fun guardar(presupuesto: PresupuestoMensual)

    @Query("DELETE FROM PresupuestoMensual")
    suspend fun eliminarTodos()

    @Query("SELECT * FROM PreferenciasPresupuesto WHERE usuarioId = :usuarioId")
    fun observarPreferencias(usuarioId: String): Flow<PreferenciasPresupuesto?>

    @Query("SELECT * FROM PreferenciasPresupuesto WHERE usuarioId = :usuarioId")
    suspend fun preferencias(usuarioId: String): PreferenciasPresupuesto?

    @Query("SELECT * FROM PreferenciasPresupuesto LIMIT 1")
    suspend fun preferenciasActuales(): PreferenciasPresupuesto?

    @Upsert
    suspend fun guardarPreferencias(preferencias: PreferenciasPresupuesto)
}
