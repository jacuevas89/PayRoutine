package cl.payroutine.datos.local.entidad

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Embedded
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.ZonedDateTime

enum class FrecuenciaAvisoPago(val dias: Long?) {
    UnaVez(null), Diaria(1), CadaTresDias(3), Semanal(7),
}

data class OpcionesAvisoPago(
    val anticipacion: Int = 0,
    val minuto: Int = 20 * 60,
    val frecuencia: FrecuenciaAvisoPago = FrecuenciaAvisoPago.UnaVez,
) : java.io.Serializable

@Entity(
    foreignKeys = [
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ObligacionPago::class, parentColumns = ["id"], childColumns = ["obligacionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PagoPendiente::class, parentColumns = ["id"], childColumns = ["pagoAnteriorId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("usuarioId"), Index(value = ["obligacionId"], unique = true), Index(value = ["pagoAnteriorId"], unique = true)],
)
data class ConfiguracionAvisoPago(
    @PrimaryKey val clave: String,
    val usuarioId: String,
    val obligacionId: Int?,
    val pagoAnteriorId: Int?,
    val activo: Boolean,
    @Embedded val opciones: OpcionesAvisoPago,
)

internal fun claveAvisoPago(obligacionId: Int?, pagoId: Int): String =
    obligacionId?.let { "obligacion:$it" } ?: "pago:$pagoId"

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

internal fun OpcionesAvisoPago.aLaHora(dia: LocalDate, ahora: ZonedDateTime): ZonedDateTime =
    dia.atTime(minuto / 60, minuto % 60).atZone(ahora.zone)

internal fun OpcionesAvisoPago.proximoAviso(
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
