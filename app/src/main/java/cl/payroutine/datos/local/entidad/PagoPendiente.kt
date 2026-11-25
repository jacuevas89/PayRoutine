package cl.payroutine.datos.local.entidad

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "PagoPendiente",
    foreignKeys = [
        ForeignKey(entity = ObligacionPago::class, parentColumns = ["id"], childColumns = ["obligacion_id"]),
        ForeignKey(entity = Movimiento::class, parentColumns = ["id"], childColumns = ["movimiento_id"]),
    ],
    indices = [Index("obligacion_id"), Index(value = ["movimiento_id"], unique = true)],
)
data class PagoPendiente(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val descripcion: String,
    val monto: Long,
    @ColumnInfo(name = "fecha_vencimiento") val fechaVencimiento: Date,
    @ColumnInfo(name = "es_cuota") val esCuota: Boolean,
    @ColumnInfo(name = "cuota_actual") val cuotaActual: Int? = null,
    @ColumnInfo(name = "total_cuotas") val totalCuotas: Int? = null,
    val pagado: Boolean,
    @ColumnInfo(name = "obligacion_id") val obligacionId: Int? = null,
    @ColumnInfo(name = "movimiento_id") val movimientoId: Int? = null,
)
