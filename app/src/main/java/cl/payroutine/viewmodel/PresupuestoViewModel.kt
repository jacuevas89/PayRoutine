package cl.payroutine.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.payroutine.PayRoutineApplication
import cl.payroutine.datos.local.entidad.PreferenciasPresupuesto
import cl.payroutine.datos.local.entidad.PresupuestoMensual
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class PresupuestoDelMes(val datos: PresupuestoMensual, val limites: Map<Int, Double>) {
    val total: Double get() = datos.limiteGeneral ?: limites.values.sumarImportes()
}

internal fun Iterable<Double>.sumarImportes(): Double = sumOf { it.toBigDecimal() }.toDouble()

internal enum class OperacionPresupuesto { Disponible, Guardando, Error }

internal class PresupuestoViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val app = aplicacion as PayRoutineApplication
    private val repositorio = app.repositorio
    private val operacion = MutableStateFlow(OperacionPresupuesto.Disponible)
    val estadoOperacion = operacion.asStateFlow()

    val presupuestos = repositorio.observarPresupuestosMensuales().map { filas ->
        filas.groupBy { YearMonth.of(it.cabecera.anio, it.cabecera.mes) }.mapValues { (_, periodo) ->
            PresupuestoDelMes(
                periodo.first().cabecera,
                periodo.mapNotNull { fila -> fila.categoriaId?.let { id -> id to requireNotNull(fila.limite) } }.toMap(),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun preferencias(usuarioId: String) = repositorio.observarPreferenciasPresupuesto(usuarioId)
        .map { it ?: PreferenciasPresupuesto(usuarioId) }

    fun guardar(usuario: String, mes: YearMonth, general: Double?, limites: Map<Int, Double>, volver: () -> Unit) =
        ejecutar(volver) { repositorio.guardarPresupuestoMensual(usuario, mes, general, limites) }

    fun omitir(usuario: String) = ejecutar {
        repositorio.actualizarPreferenciasPresupuesto(usuario) { it.copy(configuracionPendiente = false) }
    }

    fun automatico(usuario: String, activar: Boolean, usarMesAnterior: Boolean) = ejecutar {
        val zona = ZoneId.systemDefault()
        repositorio.configurarAutomatico(usuario, activar, usarMesAnterior, YearMonth.now(zona), zona)
    }

    fun limpiarError() {
        if (operacion.value == OperacionPresupuesto.Error) operacion.value = OperacionPresupuesto.Disponible
    }

    private fun ejecutar(alGuardar: () -> Unit = {}, guardar: suspend () -> Unit) {
        if (operacion.value == OperacionPresupuesto.Guardando) return
        operacion.value = OperacionPresupuesto.Guardando
        viewModelScope.launch {
            try {
                guardar()
            } catch (error: SQLiteException) {
                Log.e("Presupuesto", "No se pudo guardar el presupuesto", error)
                operacion.value = OperacionPresupuesto.Error
                return@launch
            }
            operacion.value = OperacionPresupuesto.Disponible
            alGuardar()
            try {
                app.recordatorios.restaurarPresupuesto()
            } catch (error: SecurityException) {
                Log.e("Presupuesto", "Android bloqueó la programación del aviso", error)
            } catch (error: SQLiteException) {
                Log.e("Presupuesto", "No se pudo comprobar la programación del aviso", error)
            }
        }
    }
}
