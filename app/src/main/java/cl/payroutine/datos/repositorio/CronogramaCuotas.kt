package cl.payroutine.datos.repositorio

import java.math.BigInteger
import java.time.LocalDate

internal val aniosCronograma = 1900..2100

internal enum class PeriodicidadCuotas(val etiqueta: String) {
    Semanal("Cada 7 días"),
    Quincenal("Cada 15 días"),
    Mensual("Mensual");

    fun vencimiento(primera: LocalDate, indice: Int): LocalDate = when (this) {
        Semanal -> primera.plusWeeks(indice.toLong())
        Quincenal -> primera.plusDays(15L * indice)
        Mensual -> primera.plusMonths(indice.toLong())
    }
}

internal enum class AjusteCuota { Primera, Ultima }

internal data class CuotaProgramada(val vencimiento: LocalDate, val monto: Long)

internal data class NuevaDeudaEnCuotas(
    val nombre: String,
    val montoTotal: Long,
    val categoriaId: Int,
    val cuotasPagadas: Int,
    val cronograma: List<CuotaProgramada>,
)

internal fun generarCronogramaCuotas(
    montoTotal: Long,
    cantidad: Int,
    primera: LocalDate,
    periodicidad: PeriodicidadCuotas,
    ajuste: AjusteCuota,
): List<CuotaProgramada> {
    require(cantidad >= 2) { "Ingresa al menos dos cuotas." }
    require(montoTotal in cantidad.toLong()..MONTO_MAXIMO_EXACTO) { "Cada cuota debe tener al menos $1." }
    require(primera.year in aniosCronograma && periodicidad.vencimiento(primera, cantidad - 1).year in aniosCronograma) {
        "El cronograma debe quedar entre ${aniosCronograma.first} y ${aniosCronograma.last}."
    }
    val base = montoTotal / cantidad
    val residuo = montoTotal % cantidad
    val indiceAjuste = if (ajuste == AjusteCuota.Primera) 0 else cantidad - 1
    return List(cantidad) { indice ->
        CuotaProgramada(periodicidad.vencimiento(primera, indice), base + if (indice == indiceAjuste) residuo else 0L)
    }
}

internal fun validarCronogramaCuotas(datos: NuevaDeudaEnCuotas) {
    val cuotas = datos.cronograma
    require(cuotas.size >= 2 && datos.cuotasPagadas in 0..cuotas.size) { "Revisa la cantidad de cuotas y las ya pagadas." }
    require(datos.montoTotal in 1..MONTO_MAXIMO_EXACTO && cuotas.all { it.monto in 1..MONTO_MAXIMO_EXACTO }) {
        "Los montos deben ser positivos y en pesos enteros."
    }
    require(cuotas.sumOf { BigInteger.valueOf(it.monto) } == BigInteger.valueOf(datos.montoTotal)) {
        "La suma de las cuotas debe coincidir con el monto total."
    }
    require(cuotas.all { it.vencimiento.year in aniosCronograma } && cuotas.zipWithNext().all { (a, b) -> a.vencimiento <= b.vencimiento }) {
        "Revisa el orden de las fechas de vencimiento."
    }
}
