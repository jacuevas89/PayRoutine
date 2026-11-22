package cl.payroutine.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.payroutine.PayRoutineApplication
import cl.payroutine.datos.repositorio.NuevoMovimiento
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class EstadoRegistroMovimiento(
    val guardando: Boolean = false,
    val idGuardado: Long? = null,
    val error: String? = null,
)

internal class RegistroMovimientoViewModel(
    aplicacion: Application,
    private val repositorio: RepositorioFinanzas,
) : AndroidViewModel(aplicacion) {
    constructor(aplicacion: Application) : this(aplicacion, (aplicacion as PayRoutineApplication).repositorio)

    private val estadoInterno = MutableStateFlow(EstadoRegistroMovimiento())
    val estado = estadoInterno.asStateFlow()

    fun guardar(datos: NuevoMovimiento) {
        val actual = estadoInterno.value
        if (actual.guardando || actual.idGuardado != null) return
        estadoInterno.value = EstadoRegistroMovimiento(guardando = true)
        viewModelScope.launch {
            try {
                val id = repositorio.registrarMovimiento(datos)
                estadoInterno.value = EstadoRegistroMovimiento(idGuardado = id)
            } catch (error: SQLiteException) {
                Log.e("RegistroMovimiento", "Error al guardar el movimiento", error)
                estadoInterno.value = EstadoRegistroMovimiento(error = "No se pudo guardar el movimiento. Vuelve a intentarlo.")
            } catch (error: IllegalArgumentException) {
                estadoInterno.value = EstadoRegistroMovimiento(error = "Comprueba el nombre, el monto, la fecha y la categoría antes de guardar.")
            }
        }
    }
}
