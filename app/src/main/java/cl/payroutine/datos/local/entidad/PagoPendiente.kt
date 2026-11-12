package cl.payroutine.datos.local.entidad

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "PagoPendiente")
data class PagoPendiente(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val descripcion: String,
    val monto: Double,
    @ColumnInfo(name = "fecha_vencimiento") val fechaVencimiento: Date,
    @ColumnInfo(name = "es_cuota") val esCuota: Boolean,
    @ColumnInfo(name = "cuota_actual") val cuotaActual: Int? = null,
    @ColumnInfo(name = "total_cuotas") val totalCuotas: Int? = null,
    val pagado: Boolean,
)
