package cl.payroutine.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.payroutine.PayRoutineApplication
import cl.payroutine.datos.local.dao.CategoriaConUso
import cl.payroutine.datos.local.entidad.TipoMovimiento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class EstadoCreacionCategoria { Pendiente, Guardando, Guardada, Duplicada, Error }

internal data class EstadoCategorias(
    val cargando: Boolean = true,
    val categorias: List<CategoriaConUso> = emptyList(),
    val errorCarga: Boolean = false,
    val creacion: EstadoCreacionCategoria = EstadoCreacionCategoria.Pendiente,
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

    fun crearCategoria(nombre: String, tipo: TipoMovimiento) {
        val actual = estadoInterno.value
        if (nombre.isBlank() || actual.cargando || actual.errorCarga ||
            actual.creacion == EstadoCreacionCategoria.Guardando || actual.creacion == EstadoCreacionCategoria.Guardada
        ) return

        estadoInterno.update { it.copy(creacion = EstadoCreacionCategoria.Guardando) }
        viewModelScope.launch {
            val resultado = try {
                if (repositorio.crearCategoriaPersonalizada(nombre, tipo)) EstadoCreacionCategoria.Guardada
                else EstadoCreacionCategoria.Duplicada
            } catch (error: SQLiteException) {
                Log.e("CategoriasViewModel", "Error al guardar la categoría", error)
                EstadoCreacionCategoria.Error
            }
            estadoInterno.update { it.copy(creacion = resultado) }
        }
    }

    fun limpiarCreacion() {
        estadoInterno.update {
            if (it.creacion == EstadoCreacionCategoria.Guardando) it else it.copy(creacion = EstadoCreacionCategoria.Pendiente)
        }
    }

    private fun cargar() {
        estadoInterno.update { it.copy(cargando = true, categorias = emptyList(), errorCarga = false) }
        viewModelScope.launch {
            try {
                repositorio.prepararCategoriasIniciales()
                repositorio.observarCategoriasConUso().collect { categorias ->
                    estadoInterno.update { it.copy(cargando = false, categorias = categorias, errorCarga = false) }
                }
            } catch (error: SQLiteException) {
                Log.e("CategoriasViewModel", "Error al cargar las categorías", error)
                estadoInterno.update { it.copy(cargando = false, categorias = emptyList(), errorCarga = true) }
            }
        }
    }
}
