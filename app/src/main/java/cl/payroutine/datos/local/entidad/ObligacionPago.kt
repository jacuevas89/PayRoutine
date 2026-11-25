package cl.payroutine.datos.local.entidad

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ObligacionPago",
    foreignKeys = [
        ForeignKey(entity = Categoria::class, parentColumns = ["id"], childColumns = ["categoria_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("categoria_id")],
)
data class ObligacionPago(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    @ColumnInfo(name = "categoria_id") val categoriaId: Int?,
)
