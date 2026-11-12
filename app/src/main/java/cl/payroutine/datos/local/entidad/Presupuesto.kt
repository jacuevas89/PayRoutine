package cl.payroutine.datos.local.entidad

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Presupuesto",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoria_id"],
        ),
    ],
    indices = [Index("categoria_id")],
)
data class Presupuesto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "categoria_id") val categoriaId: Int,
    @ColumnInfo(name = "monto_presupuestado") val montoPresupuestado: Double,
    val mes: Int,
    val anio: Int,
)
