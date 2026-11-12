package cl.payroutine.datos.local.entidad

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Categoria")
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val tipo: TipoMovimiento,
)
