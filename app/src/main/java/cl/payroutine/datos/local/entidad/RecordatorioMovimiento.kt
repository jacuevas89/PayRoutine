package cl.payroutine.datos.local.entidad

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZonedDateTime

enum class RecurrenciaRecordatorio { Diaria, Semanal, Mensual, FinDeMes }

@Entity(
    tableName = "RecordatorioMovimiento",
    foreignKeys = [ForeignKey(
        entity = Usuario::class,
        parentColumns = ["id"],
        childColumns = ["usuario_id"],
        onDelete = ForeignKey.CASCADE,
    )],
)
data class RecordatorioMovimiento(
    @PrimaryKey @ColumnInfo(name = "usuario_id") val usuarioId: String,
    val activo: Boolean = false,
    val minutoDelDia: Int = 20 * 60,
    val fechaInicial: Long? = null,
    val fechaReferencia: Long = 0,
    val recurrencia: RecurrenciaRecordatorio = RecurrenciaRecordatorio.Diaria,
    val programacionId: String = "",
    val ultimoAviso: Long? = null,
) {
    val hora: LocalTime get() = LocalTime.of(minutoDelDia / 60, minutoDelDia % 60)

    fun siguienteAviso(ahora: ZonedDateTime): ZonedDateTime {
        val inicio = LocalDate.ofEpochDay(
            if (recurrencia == RecurrenciaRecordatorio.Diaria) fechaReferencia
            else fechaInicial ?: fechaReferencia,
        )
        val despuesDelUltimo = ultimoAviso?.let { LocalDate.ofEpochDay(it).plusDays(1) } ?: inicio
        val desde = maxOf(ahora.toLocalDate(), inicio, despuesDelUltimo)
        fun LocalDate.aLaHora() = atTime(hora).atZone(ahora.zone)
        if (recurrencia == RecurrenciaRecordatorio.Diaria) {
            return desde.aLaHora().takeIf { it.isAfter(ahora) } ?: desde.plusDays(1).aLaHora()
        }
        if (recurrencia == RecurrenciaRecordatorio.Semanal) {
            val diasHastaElAviso = Math.floorMod(inicio.dayOfWeek.value - desde.dayOfWeek.value, 7)
            val dia = desde.plusDays(diasHastaElAviso.toLong())
            return dia.aLaHora().takeIf { it.isAfter(ahora) } ?: dia.plusWeeks(1).aLaHora()
        }
        fun fechaDelMes(mes: YearMonth): LocalDate = when (recurrencia) {
            RecurrenciaRecordatorio.FinDeMes -> mes.atEndOfMonth()
            else -> mes.atDay(minOf(inicio.dayOfMonth, mes.lengthOfMonth()))
        }
        val mes = YearMonth.from(desde)
        val dia = fechaDelMes(mes)
        return if (dia >= desde && dia.aLaHora().isAfter(ahora)) dia.aLaHora()
        else fechaDelMes(mes.plusMonths(1)).aLaHora()
    }
}
