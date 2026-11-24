package cl.payroutine.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.payroutine.PayRoutineApplication
import cl.payroutine.datos.repositorio.NuevaDeudaEnCuotas
import cl.payroutine.datos.repositorio.NuevoPagoUnico
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class EstadoRegistroPago(
    val guardando: Boolean = false,
    val idGuardado: Long? = null,
    val error: String? = null,
)

internal class RegistroPagoViewModel(
    aplicacion: Application,
    private val repositorio: RepositorioFinanzas,
) : AndroidViewModel(aplicacion) {
    constructor(aplicacion: Application) : this(aplicacion, (aplicacion as PayRoutineApplication).repositorio)

    private val estadoInterno = MutableStateFlow(EstadoRegistroPago())
    val estado = estadoInterno.asStateFlow()

    fun guardar(datos: NuevoPagoUnico) = ejecutarGuardado { repositorio.registrarPagoUnico(datos) }

    fun guardarDeuda(datos: NuevaDeudaEnCuotas) = ejecutarGuardado { repositorio.registrarDeuda(datos) }

    private fun ejecutarGuardado(guardar: suspend () -> Long) {
        val actual = estadoInterno.value
        if (actual.guardando || actual.idGuardado != null) return
        estadoInterno.value = EstadoRegistroPago(guardando = true)
        viewModelScope.launch {
            try {
                estadoInterno.value = EstadoRegistroPago(idGuardado = guardar())
            } catch (error: SQLiteException) {
                Log.e("RegistroPago", "Error al guardar el pago pendiente", error)
                estadoInterno.value = EstadoRegistroPago(error = "No se pudo guardar el pago. Vuelve a intentarlo.")
            } catch (error: IllegalArgumentException) {
                estadoInterno.value = EstadoRegistroPago(error = error.message ?: "Revisa los datos del pago.")
            }
        }
    }
}
