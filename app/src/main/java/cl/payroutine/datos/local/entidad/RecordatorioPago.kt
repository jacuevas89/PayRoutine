package cl.payroutine.datos.local.entidad

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.ZonedDateTime

enum class FrecuenciaAvisoPago(val dias: Long?) {
    UnaVez(null), Diaria(1), CadaTresDias(3), Semanal(7),
}

@Entity(foreignKeys = [
    ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.CASCADE),
])
data class PreferenciasAvisosPago(
    @PrimaryKey val usuarioId: String,
    val activo: Boolean = false,
    val anticipacion: Int = 0,
    val minuto: Int = 20 * 60,
    val frecuencia: FrecuenciaAvisoPago = FrecuenciaAvisoPago.UnaVez,
)

@Entity(foreignKeys = [
    ForeignKey(entity = PagoPendiente::class, parentColumns = ["id"], childColumns = ["vencimientoId"], onDelete = ForeignKey.CASCADE),
])
data class RecordatorioPago(
    @PrimaryKey val vencimientoId: Int,
    val vencimientoProgramado: Long? = null,
    val diaVencimientoProgramado: Long? = null,
    val diaProgramado: Long? = null,
    val ultimoAviso: Long? = null,
)

internal fun PreferenciasAvisosPago.aLaHora(dia: LocalDate, ahora: ZonedDateTime): ZonedDateTime =
    dia.atTime(minuto / 60, minuto % 60).atZone(ahora.zone)

internal fun PreferenciasAvisosPago.proximoAviso(
    recordatorio: RecordatorioPago,
    vencimiento: LocalDate,
    ahora: ZonedDateTime,
    conservarHoraTranscurrida: Boolean,
): LocalDate? {
    val ultimo = recordatorio.ultimoAviso?.let(LocalDate::ofEpochDay)
    val intervalo = frecuencia.dias
    if (ultimo != null && intervalo == null) return null
    val guardado = recordatorio.diaProgramado?.takeIf {
        recordatorio.diaVencimientoProgramado == vencimiento.toEpochDay()
    }?.let(LocalDate::ofEpochDay)
    val previsto = guardado ?: if (ultimo == null) vencimiento.minusDays(anticipacion.toLong())
        else ultimo.plusDays(requireNotNull(intervalo))
    val hoy = ahora.toLocalDate()
    return if (aLaHora(previsto, ahora).isAfter(ahora) ||
        (guardado == hoy && conservarHoraTranscurrida)
    ) previsto else hoy.plusDays(1)
}
