package cl.payroutine.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.payroutine.PayRoutineApplication
import cl.payroutine.datos.local.entidad.RecordatorioMovimiento
import cl.payroutine.datos.local.entidad.PreferenciasPresupuesto
import cl.payroutine.datos.local.entidad.PreferenciasAvisosPago
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class RecordatoriosViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val recordatorios = (aplicacion as PayRoutineApplication).recordatorios
    private val errorInterno = MutableStateFlow(false)
    val error = errorInterno.asStateFlow()

    fun observar(usuarioId: String) = recordatorios.observar(usuarioId)
    fun observarPreferenciasPagos(usuarioId: String) = recordatorios.observarPreferenciasPagos(usuarioId)
    fun observarSeleccionPagos() = recordatorios.observarSeleccionPagos()
    fun notificacionesHabilitadas() = recordatorios.notificacionesHabilitadas()
    fun notificacionesPagosHabilitadas() = recordatorios.notificacionesPagosHabilitadas()
    fun alarmasExactasHabilitadas() = recordatorios.alarmasExactasHabilitadas()
    fun canalSilenciado() = recordatorios.canalSilenciado()

    fun restaurar() = ejecutar { recordatorios.restaurar(siFalta = true) }

    fun actualizar(usuarioId: String, cambiar: (RecordatorioMovimiento) -> RecordatorioMovimiento) =
        ejecutar { recordatorios.actualizar(usuarioId, cambiar) }

    fun actualizarPresupuesto(usuarioId: String, cambiar: (PreferenciasPresupuesto) -> PreferenciasPresupuesto) =
        ejecutar { recordatorios.actualizarAvisoPresupuesto(usuarioId, cambiar) }

    fun actualizarPagos(usuarioId: String, cambiar: (PreferenciasAvisosPago) -> PreferenciasAvisosPago) =
        ejecutar { recordatorios.actualizarPreferenciasPagos(usuarioId, cambiar) }

    fun seleccionarPagos(usuarioId: String, vencimientos: List<Int>, seleccionar: Boolean) =
        ejecutar { recordatorios.seleccionarPagos(usuarioId, vencimientos, seleccionar) }

    private fun ejecutar(operacion: suspend () -> Unit) {
        viewModelScope.launch {
            errorInterno.value = false
            try {
                operacion()
            } catch (error: SQLiteException) {
                informarError(error)
            } catch (error: SecurityException) {
                informarError(error)
            }
        }
    }

    private fun informarError(error: Exception) {
        Log.e("Recordatorios", "No se pudo actualizar el aviso", error)
        errorInterno.value = true
    }
}
