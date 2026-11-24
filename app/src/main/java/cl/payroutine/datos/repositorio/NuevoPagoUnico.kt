package cl.payroutine.datos.repositorio

import java.time.LocalDate

data class NuevoPagoUnico(
    val nombre: String,
    val monto: Long,
    val vencimiento: LocalDate,
    val categoriaId: Int,
)
