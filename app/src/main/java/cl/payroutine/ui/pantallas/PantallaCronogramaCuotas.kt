package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.payroutine.datos.repositorio.CuotaProgramada
import cl.payroutine.datos.repositorio.validarMontoMovimiento
import cl.payroutine.ui.theme.RojoGasto
import cl.payroutine.viewmodel.EstadoRegistroPago
import java.time.LocalDate

internal data class CuotaEditable(val vencimiento: LocalDate, val monto: String)

internal val guardadorCronograma = listSaver<List<CuotaEditable>, Any>(
    save = { cuotas -> cuotas.flatMap { listOf(it.vencimiento.toEpochDay(), it.monto) } },
    restore = { datos -> datos.chunked(2).map { CuotaEditable(LocalDate.ofEpochDay(it[0] as Long), it[1] as String) } },
)

@Composable
internal fun PantallaCronogramaCuotas(
    cuotas: List<CuotaEditable>,
    montoTotal: Long,
    cuotasPagadas: Int,
    categoriaDisponible: Boolean,
    estado: EstadoRegistroPago,
    cambiarCuota: (Int, CuotaEditable) -> Unit,
    guardar: (List<CuotaProgramada>) -> Unit,
    volver: () -> Unit,
) {
    var indiceCalendario by rememberSaveable { mutableStateOf<Int?>(null) }
    val importes = remember(cuotas) { cuotas.map { validarMontoMovimiento(it.monto) } }
    val suma = remember(importes) { importes.mapNotNull { it.valor }.sumOf { it.toBigDecimal() } }
    val montosValidos = importes.all { it.valor != null }
    val totalCorrecto = montosValidos && suma.compareTo(montoTotal.toBigDecimal()) == 0
    val fechasOrdenadas = cuotas.zipWithNext().all { (a, b) -> a.vencimiento <= b.vencimiento }
    val bloqueado = estado.guardando || estado.idGuardado != null
    val foco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current

    fun cerrarTeclado() {
        foco.clearFocus()
        teclado?.hide()
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Cabecera("Cronograma de cuotas", volver = {
            if (!bloqueado) {
                cerrarTeclado()
                volver()
            }
        })
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Tarjeta(Modifier.fillMaxWidth(), espacio = 6) {
                    Etiqueta("Monto total de la deuda")
                    Cifra(montoEnPantalla(montoTotal), tamano = 22)
                    Etiqueta("Suma de cuotas: ${montoEnPantalla(suma)}")
                }
            }
            itemsIndexed(cuotas, key = { indice, _ -> indice }) { indice, cuota ->
                val numero = indice + 1
                val importe = importes[indice]
                Tarjeta(Modifier.fillMaxWidth(), espacio = 8) {
                    Destacado("Cuota $numero de ${cuotas.size}")
                    if (numero <= cuotasPagadas) Etiqueta("Ya pagada")
                    CampoVencimientoPago(
                        etiqueta = "Vencimiento",
                        descripcion = "Seleccionar vencimiento de la cuota $numero",
                        fecha = cuota.vencimiento,
                        alPulsar = if (bloqueado) null else {
                            {
                                cerrarTeclado()
                                indiceCalendario = indice
                            }
                        },
                    )
                    OutlinedTextField(
                        value = cuota.monto,
                        onValueChange = { cambiarCuota(indice, cuota.copy(monto = it)) },
                        label = { Text("Monto de la cuota $numero") },
                        prefix = { Text("$ ") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !bloqueado,
                        singleLine = true,
                        isError = importe.error != null,
                        supportingText = importe.error?.let { error -> { Text(error) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
        }
        val error = when {
            !totalCorrecto -> "La suma de las cuotas debe coincidir con el monto total."
            !fechasOrdenadas -> "Ordena las fechas de la primera a la última cuota."
            !categoriaDisponible -> "Vuelve al formulario para seleccionar la categoría."
            else -> estado.error
        }
        error?.let { Etiqueta(it, RojoGasto, Modifier.padding(horizontal = 16.dp)) }
        BotonPrincipal(
            if (estado.guardando) "Guardando…" else "Guardar deuda",
            modifier = Modifier.padding(16.dp),
            habilitado = !bloqueado && totalCorrecto && fechasOrdenadas && categoriaDisponible,
        ) {
            cerrarTeclado()
            guardar(cuotas.mapIndexed { i, cuota -> CuotaProgramada(cuota.vencimiento, requireNotNull(importes[i].valor)) })
        }
    }

    val indice = indiceCalendario
    val cuota = indice?.let(cuotas::getOrNull)
    if (!bloqueado && indice != null && cuota != null) {
        SelectorVencimientoPago(cuota.vencimiento, cerrar = { indiceCalendario = null }) { dia ->
            cambiarCuota(indice, cuota.copy(vencimiento = dia))
            indiceCalendario = null
        }
    }
}
