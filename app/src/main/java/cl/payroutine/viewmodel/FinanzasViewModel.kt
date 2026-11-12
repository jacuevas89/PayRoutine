package cl.payroutine.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cl.payroutine.PayRoutineApplication

class FinanzasViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val repositorio = (aplicacion as PayRoutineApplication).repositorio

    val categorias = repositorio.observarCategorias()
    val movimientos = repositorio.observarMovimientos()
    val presupuestos = repositorio.observarPresupuestos()
    val pagos = repositorio.observarPagos()
}
