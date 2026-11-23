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
    val edicion: EstadoEdicionPerfil = EstadoEdicionPerfil.Disponible,
    val borrado: EstadoBorrado = EstadoBorrado.Disponible,
)

internal enum class EstadoEdicionPerfil { Disponible, Guardando, Error, Guardada }
internal enum class EstadoBorrado { Disponible, Eliminando, Error }

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
                    estadoInterno.update {
                        if (it.borrado == EstadoBorrado.Eliminando) it
                        else it.copy(cargando = false, usuario = usuario)
                    }
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

    fun prepararEdicion() {
        estadoInterno.update { actual ->
            if (actual.edicion == EstadoEdicionPerfil.Guardando) actual
            else actual.copy(edicion = EstadoEdicionPerfil.Disponible)
        }
    }

    fun guardarEdicion(nombreCompleto: String, apodo: String) {
        val actual = estadoInterno.value
        val usuario = actual.usuario ?: return
        if (actual.edicion == EstadoEdicionPerfil.Guardando || actual.edicion == EstadoEdicionPerfil.Guardada) return
        if (nombreCompleto.isBlank() || apodo.isBlank()) return

        estadoInterno.update { it.copy(edicion = EstadoEdicionPerfil.Guardando) }
        viewModelScope.launch {
            val actualizado = try {
                repositorio.actualizarDatosUsuario(usuario.id, nombreCompleto, apodo) == 1
            } catch (error: SQLiteException) {
                Log.e("PerfilViewModel", "Error al actualizar el perfil", error)
                false
            }
            estadoInterno.update {
                it.copy(edicion = if (actualizado) EstadoEdicionPerfil.Guardada else EstadoEdicionPerfil.Error)
            }
        }
    }

    fun limpiarErrorBorrado() {
        estadoInterno.update {
            if (it.borrado == EstadoBorrado.Error) it.copy(borrado = EstadoBorrado.Disponible) else it
        }
    }

    fun eliminarTodosLosDatos() {
        val actual = estadoInterno.value
        if (actual.usuario == null || actual.borrado == EstadoBorrado.Eliminando ||
            actual.edicion == EstadoEdicionPerfil.Guardando
        ) return

        estadoInterno.update { it.copy(borrado = EstadoBorrado.Eliminando) }
        viewModelScope.launch {
            try {
                getApplication<PayRoutineApplication>().recordatorios.eliminarDatosLocales()
                estadoInterno.value = EstadoPerfil(cargando = false)
            } catch (error: SQLiteException) {
                informarErrorBorrado(error)
            } catch (error: SecurityException) {
                informarErrorBorrado(error)
            }
        }
    }

    private fun informarErrorBorrado(error: Exception) {
        Log.e("PerfilViewModel", "Error al eliminar los datos locales", error)
        estadoInterno.update { it.copy(borrado = EstadoBorrado.Error) }
    }
}
