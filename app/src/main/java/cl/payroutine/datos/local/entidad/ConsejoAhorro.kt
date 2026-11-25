package cl.payroutine.datos.local.entidad

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ConsejoAhorro(
    @PrimaryKey val id: String,
    val titulo: String,
    val detalle: String,
    val orden: Int,
)
