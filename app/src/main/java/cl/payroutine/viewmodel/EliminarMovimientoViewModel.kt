package cl.payroutine.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.payroutine.PayRoutineApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal enum class EstadoEliminacionMovimiento { Disponible, Eliminando, Eliminado, Error }

internal class EliminarMovimientoViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val repositorio = (aplicacion as PayRoutineApplication).repositorio
    private val estadoInterno = MutableStateFlow(EstadoEliminacionMovimiento.Disponible)
    val estado = estadoInterno.asStateFlow()

    fun eliminar(id: Int) {
        val actual = estadoInterno.value
        if (actual == EstadoEliminacionMovimiento.Eliminando || actual == EstadoEliminacionMovimiento.Eliminado) return
        estadoInterno.value = EstadoEliminacionMovimiento.Eliminando
        viewModelScope.launch {
            try {
                repositorio.eliminarMovimiento(id)
                estadoInterno.value = EstadoEliminacionMovimiento.Eliminado
            } catch (error: SQLiteException) {
                Log.e("EliminarMovimiento", "Error al eliminar el movimiento", error)
                estadoInterno.value = EstadoEliminacionMovimiento.Error
            }
        }
    }

    fun limpiarError() {
        estadoInterno.compareAndSet(EstadoEliminacionMovimiento.Error, EstadoEliminacionMovimiento.Disponible)
    }
}
