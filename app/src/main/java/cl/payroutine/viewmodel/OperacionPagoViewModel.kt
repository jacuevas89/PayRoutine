package cl.payroutine.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.payroutine.PayRoutineApplication
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class EstadoOperacionPago(
    val guardando: Boolean = false,
    val completada: Boolean = false,
    val error: String? = null,
)

internal class OperacionPagoViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val repositorio = (aplicacion as PayRoutineApplication).repositorio
    private val estadoInterno = MutableStateFlow(EstadoOperacionPago())
    val estado = estadoInterno.asStateFlow()

    fun pagar(ids: List<Int>, fecha: LocalDate, nota: String) = ejecutar {
        repositorio.pagarVencimientos(ids, fecha, nota)
    }

    fun deshacer(id: Int, movimientoId: Int) = ejecutar {
        repositorio.deshacerPago(id, movimientoId)
    }

    fun limpiar() {
        if (!estadoInterno.value.guardando) estadoInterno.value = EstadoOperacionPago()
    }

    private fun ejecutar(operacion: suspend () -> Unit) {
        val actual = estadoInterno.value
        if (actual.guardando || actual.completada) return
        estadoInterno.value = EstadoOperacionPago(guardando = true)
        viewModelScope.launch {
            try {
                operacion()
                estadoInterno.value = EstadoOperacionPago(completada = true)
            } catch (error: IllegalArgumentException) {
                estadoInterno.value = EstadoOperacionPago(error = error.message ?: "Revisa los datos del pago.")
            } catch (error: SQLiteException) {
                Log.e("OperacionPago", "Error al registrar o deshacer el pago", error)
                estadoInterno.value = EstadoOperacionPago(error = "No se pudo guardar el cambio. Vuelve a intentarlo.")
            }
        }
    }
}
