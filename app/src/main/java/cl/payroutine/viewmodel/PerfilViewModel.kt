package cl.payroutine.viewmodel

import android.app.Application
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.payroutine.PayRoutineApplication
import cl.payroutine.datos.local.entidad.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class EstadoPerfil(
    val cargando: Boolean = true,
    val usuario: Usuario? = null,
    val errorCarga: Boolean = false,
    val guardando: Boolean = false,
    val errorGuardado: Boolean = false,
)

internal class PerfilViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {
    private val repositorio = (aplicacion as PayRoutineApplication).repositorio
    private val estadoInterno = MutableStateFlow(EstadoPerfil())
    val estado = estadoInterno.asStateFlow()

    init {
        observarPerfil()
    }

    fun reintentarCarga() {
        if (estadoInterno.value.errorCarga) observarPerfil()
    }

    private fun observarPerfil() {
        estadoInterno.update { it.copy(cargando = true, errorCarga = false) }
        viewModelScope.launch {
            try {
                repositorio.observarUsuario().collect { usuario ->
                    estadoInterno.update { it.copy(cargando = false, usuario = usuario) }
                }
            } catch (error: SQLiteException) {
                Log.e("PerfilViewModel", "Error al leer el perfil", error)
                estadoInterno.update { it.copy(cargando = false, errorCarga = true) }
            }
        }
    }

    fun registrar(nombreCompleto: String, apodo: String) {
        val actual = estadoInterno.value
        if (actual.guardando || actual.usuario != null || nombreCompleto.isBlank() || apodo.isBlank()) return
        estadoInterno.update { it.copy(guardando = true, errorGuardado = false) }
        viewModelScope.launch {
            try {
                repositorio.registrarUsuario(nombreCompleto, apodo)
            } catch (error: SQLiteException) {
                Log.e("PerfilViewModel", "Error al guardar el perfil", error)
                estadoInterno.update { it.copy(errorGuardado = true) }
            } finally {
                estadoInterno.update { it.copy(guardando = false) }
            }
        }
    }
}
