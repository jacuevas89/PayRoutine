package cl.payroutine.datos.local

import java.math.BigDecimal
import java.math.RoundingMode

internal fun repartirPesos(importes: List<BigDecimal>): List<Long> {
    if (importes.isEmpty()) return emptyList()
    val bases = importes.map { it.setScale(0, RoundingMode.FLOOR) }
    val total = importes.sumOf { it }.setScale(0, RoundingMode.HALF_UP)
    val residuo = (total - bases.sumOf { it }).intValueExact()
    val ajustar = importes.indices.sortedByDescending { importes[it] - bases[it] }.take(residuo).toSet()
    return bases.mapIndexed { i, base ->
        (if (i in ajustar) base + BigDecimal.ONE else base).longValueExact()
    }
}
