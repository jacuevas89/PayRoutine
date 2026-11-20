package cl.payroutine.ui.navegacion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.payroutine.ui.pantallas.BotonPrincipal
import cl.payroutine.ui.pantallas.Etiqueta
import cl.payroutine.ui.pantallas.PantallaBienvenida
import cl.payroutine.ui.pantallas.PantallaRegistroUsuario
import cl.payroutine.ui.theme.AzulNoche
import cl.payroutine.ui.theme.Fondo
import cl.payroutine.viewmodel.EstadoPerfil

@Composable
internal fun PrimerUso(
    estado: EstadoPerfil,
    reintentarCarga: () -> Unit,
    registrar: (String, String) -> Unit,
) {
    var mostrarFormulario by rememberSaveable { mutableStateOf(false) }
    val bienvenida = !estado.cargando && !estado.errorCarga && !mostrarFormulario
    AjustarBarraDeEstado(!bienvenida)
    BackHandler(enabled = mostrarFormulario) {
        if (!estado.guardando) mostrarFormulario = false
    }

    Scaffold(
        containerColor = if (bienvenida) AzulNoche else Fondo,
        contentWindowInsets = if (bienvenida) {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        } else {
            WindowInsets.safeDrawing
        },
    ) { espacioInterior ->
        Box(
            Modifier.fillMaxSize().padding(espacioInterior).consumeWindowInsets(espacioInterior),
            contentAlignment = Alignment.Center,
        ) {
            when {
                estado.cargando -> CircularProgressIndicator()
                estado.errorCarga -> Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Etiqueta("No se pudo leer el perfil.")
                    BotonPrincipal("Reintentar", alPulsar = reintentarCarga)
                }
                mostrarFormulario -> PantallaRegistroUsuario(
                    guardando = estado.guardando,
                    errorGuardado = estado.errorGuardado,
                    volver = { if (!estado.guardando) mostrarFormulario = false },
                    registrar = registrar,
                )
                else -> PantallaBienvenida { mostrarFormulario = true }
            }
        }
    }
}
