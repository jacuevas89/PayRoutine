package cl.payroutine.datos.local.entidad

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "Usuario")
data class Usuario(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "nombre_completo") val nombreCompleto: String,
    val apodo: String,
)
