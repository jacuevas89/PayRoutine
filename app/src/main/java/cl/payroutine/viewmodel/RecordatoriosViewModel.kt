package cl.payroutine.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.payroutine.PayRoutineApplication
import cl.payroutine.datos.local.entidad.RecordatorioMovimiento
import cl.payroutine.datos.local.entidad.PreferenciasPresupuesto
import cl.payroutine.datos.local.entidad.OpcionesAvisoPago
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class RecordatoriosViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val app = aplicacion as PayRoutineApplication
    private val recordatorios = app.recordatorios
    private val repositorio = app.repositorio
    private val errorInterno = MutableStateFlow(false)
    val error = errorInterno.asStateFlow()

    fun observar(usuarioId: String) = recordatorios.observar(usuarioId)
    fun observarAvisosPagos(usuarioId: String): kotlinx.coroutines.flow.Flow<List<cl.payroutine.datos.local.dao.ConfiguracionConProgramacion>> =
        recordatorios.observarConfiguracionesPagos(usuarioId)
    fun historial(usuarioId: String) = repositorio.observarHistorialAvisos(usuarioId)
    fun marcarLeidos(usuarioId: String, ids: List<String>) = ejecutar { repositorio.marcarAvisosLeidos(usuarioId, ids) }
    fun eliminarAviso(usuarioId: String, id: String) = ejecutar { repositorio.eliminarAviso(usuarioId, id) }
    fun eliminarHistorial(usuarioId: String) = ejecutar { repositorio.eliminarHistorialAvisos(usuarioId) }
    fun notificacionesHabilitadas() = recordatorios.notificacionesHabilitadas()
    fun notificacionesPagosHabilitadas() = recordatorios.notificacionesPagosHabilitadas()
    fun alarmasExactasHabilitadas() = recordatorios.alarmasExactasHabilitadas()
    fun canalSilenciado() = recordatorios.canalSilenciado()
    fun canalPagosSilenciado() = recordatorios.canalSilenciado(cl.payroutine.recordatorios.CANAL_PAGOS)

    fun restaurar() = ejecutar { recordatorios.restaurar(siFalta = true) }

    fun actualizar(usuarioId: String, cambiar: (RecordatorioMovimiento) -> RecordatorioMovimiento) =
        ejecutar { recordatorios.actualizar(usuarioId, cambiar) }

    fun actualizarPresupuesto(usuarioId: String, cambiar: (PreferenciasPresupuesto) -> PreferenciasPresupuesto) =
        ejecutar { recordatorios.actualizarAvisoPresupuesto(usuarioId, cambiar) }

    fun configurarPago(usuarioId: String, pagoId: Int, opciones: OpcionesAvisoPago?) =
        ejecutar { recordatorios.configurarPago(usuarioId, pagoId, opciones) }

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
