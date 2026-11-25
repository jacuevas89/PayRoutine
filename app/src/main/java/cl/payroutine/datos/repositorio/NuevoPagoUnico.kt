package cl.payroutine.datos.repositorio

import cl.payroutine.datos.local.entidad.OpcionesAvisoPago
import java.time.LocalDate

data class NuevoPagoUnico(
    val nombre: String,
    val monto: Long,
    val vencimiento: LocalDate,
    val categoriaId: Int,
    val recordatorio: OpcionesAvisoPago? = null,
) : java.io.Serializable
