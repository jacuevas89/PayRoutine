package cl.payroutine.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.payroutine.PayRoutineApplication
import cl.payroutine.datos.local.dao.CategoriaConUso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class EstadoCategorias(
    val cargando: Boolean = true,
    val categorias: List<CategoriaConUso> = emptyList(),
    val errorCarga: Boolean = false,
)

internal class CategoriasViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val repositorio = (aplicacion as PayRoutineApplication).repositorio
    private val estadoInterno = MutableStateFlow(EstadoCategorias())
    val estado = estadoInterno.asStateFlow()

    init {
        cargar()
    }

    fun reintentarCarga() {
        if (estadoInterno.value.errorCarga) cargar()
    }

    private fun cargar() {
        estadoInterno.value = EstadoCategorias()
        viewModelScope.launch {
            try {
                repositorio.prepararCategoriasIniciales()
                repositorio.observarCategoriasConUso().collect {
                    estadoInterno.value = EstadoCategorias(cargando = false, categorias = it)
                }
            } catch (error: SQLiteException) {
                Log.e("CategoriasViewModel", "Error al cargar las categorías", error)
                estadoInterno.value = EstadoCategorias(cargando = false, errorCarga = true)
            }
        }
    }
}
