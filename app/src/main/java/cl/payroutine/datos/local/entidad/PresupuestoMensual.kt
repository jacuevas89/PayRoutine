package cl.payroutine.datos.local.entidad

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.YearMonth
import java.time.ZonedDateTime

enum class ModalidadPresupuesto { General, Categorias }

@Entity(primaryKeys = ["anio", "mes"])
data class PresupuestoMensual(
    val anio: Int,
    val mes: Int,
    val modalidad: ModalidadPresupuesto,
    val limiteGeneral: Double? = null,
    val generado: Boolean = false,
    val ingresoReferencia: Double? = null,
)

@Entity(foreignKeys = [
    ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.CASCADE),
])
data class PreferenciasPresupuesto(
    @PrimaryKey val usuarioId: String,
    val configuracionPendiente: Boolean = false,
    val primerMesAutomatico: Int? = null,
    val avisoActivo: Boolean = false,
    val diaAviso: Int = 1,
    val minutoAviso: Int = 20 * 60,
    val ultimoMesAvisado: Int? = null,
)

internal fun YearMonth.indice(): Int = year * 12 + monthValue - 1

internal fun mesDesdeIndice(indice: Int): YearMonth =
    YearMonth.of(Math.floorDiv(indice, 12), Math.floorMod(indice, 12) + 1)

internal fun PreferenciasPresupuesto.fechaAviso(mes: YearMonth, ahora: ZonedDateTime): ZonedDateTime =
    mes.atDay(diaAviso.coerceAtMost(mes.lengthOfMonth()))
        .atTime(minutoAviso / 60, minutoAviso % 60).atZone(ahora.zone)
