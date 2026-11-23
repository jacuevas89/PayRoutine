package cl.payroutine.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.payroutine.PayRoutineApplication
import cl.payroutine.datos.local.dao.PagoConCategoria
import cl.payroutine.datos.repositorio.RepositorioFinanzas
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal data class EstadoPagosPendientes(
    val cargando: Boolean = true,
    val pagos: List<PagoConCategoria> = emptyList(),
    val errorCarga: Boolean = false,
)

class FinanzasViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val repositorio = (aplicacion as PayRoutineApplication).repositorio

    val categorias = repositorio.observarCategorias()
    val movimientos = repositorio.observarMovimientos()
    val presupuestos = repositorio.observarPresupuestos()
    val pagos = repositorio.observarPagos()

    internal val inicio = repositorio.observarInicio(observarFechaLocal(aplicacion))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), null)

    private val recargaPagos = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val pagosPendientes = recargaPagos.flatMapLatest {
        repositorio.observarPagosPendientes()
            .map { EstadoPagosPendientes(cargando = false, pagos = it) }
            .onStart { emit(EstadoPagosPendientes()) }
            .catch { error ->
                if (error !is SQLiteException) throw error
                Log.e("FinanzasViewModel", "Error al consultar los pagos pendientes", error)
                emit(EstadoPagosPendientes(cargando = false, errorCarga = true))
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), EstadoPagosPendientes())

    fun reintentarPagos() {
        if (pagosPendientes.value.errorCarga) recargaPagos.update { it + 1 }
    }
}

internal fun RepositorioFinanzas.observarInicio(fechas: Flow<FechaLocal>): Flow<DatosInicio> =
    combine(fechas, observarMovimientosConCategoria(), observarPagos(), ::calcularInicio)

private fun observarFechaLocal(contexto: Context) = callbackFlow {
    fun publicarFecha() {
        val zona = ZoneId.systemDefault()
        trySend(FechaLocal(LocalDate.now(zona), zona))
    }

    val receptor = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = publicarFecha()
    }
    val filtro = IntentFilter(Intent.ACTION_DATE_CHANGED).apply {
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }
    ContextCompat.registerReceiver(contexto, receptor, filtro, ContextCompat.RECEIVER_NOT_EXPORTED)
    publicarFecha()
    awaitClose { contexto.unregisterReceiver(receptor) }
}.distinctUntilChanged()
