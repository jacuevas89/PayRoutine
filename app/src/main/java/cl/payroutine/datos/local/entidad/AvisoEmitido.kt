package cl.payroutine.datos.local.entidad

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("usuarioId")],
)
data class AvisoEmitido(
    @PrimaryKey val id: String,
    val usuarioId: String,
    val titulo: String,
    val mensaje: String,
    val fecha: Long,
    val mostradoEnAndroid: Boolean,
    val leido: Boolean = false,
)
