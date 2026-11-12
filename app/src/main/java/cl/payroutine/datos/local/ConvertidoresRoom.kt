package cl.payroutine.datos.local

import androidx.room.TypeConverter
import java.util.Date

class ConvertidoresRoom {
    @TypeConverter
    fun desdeMilisegundos(valor: Long): Date = Date(valor)

    @TypeConverter
    fun aMilisegundos(fecha: Date): Long = fecha.time
}
