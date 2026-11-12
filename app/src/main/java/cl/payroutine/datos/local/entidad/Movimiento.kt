package cl.payroutine.datos.local.entidad

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "Movimiento",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoria_id"],
        ),
    ],
    indices = [Index("categoria_id")],
)
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tipo: TipoMovimiento,
    val monto: Double,
    val fecha: Date,
    @ColumnInfo(name = "categoria_id") val categoriaId: Int,
    val nota: String? = null,
)
