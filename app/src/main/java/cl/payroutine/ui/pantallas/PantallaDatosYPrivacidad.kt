package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import cl.payroutine.ui.theme.AzulNoche
import cl.payroutine.ui.theme.FondoGasto
import cl.payroutine.ui.theme.FondoSelector
import cl.payroutine.ui.theme.RojoGasto
import cl.payroutine.ui.theme.TextoSecundario
import cl.payroutine.viewmodel.EstadoBorrado

@Composable
internal fun PantallaDatosYPrivacidad(
    estado: EstadoBorrado,
    volver: () -> Unit,
    verInformacion: () -> Unit,
    eliminarDatos: () -> Unit,
    limpiarError: () -> Unit,
) {
    var confirmar by rememberSaveable { mutableStateOf(false) }
    val eliminando = estado == EstadoBorrado.Eliminando
    Column(Modifier.fillMaxSize()) {
        Cabecera("Datos y privacidad", volver)
        Cuerpo(Modifier.weight(1f)) {
            FilaAjustes("Información de privacidad", "list", AzulNoche, FondoSelector, alPulsar = verInformacion)
            FilaAjustes(
                "Exportar datos", "download", TextoSecundario, FondoSelector,
                detalle = "Pendiente de definición", habilitada = false,
            )
            FilaAjustes(
                "Eliminar todos los datos", "alert", RojoGasto, FondoGasto, colorTitulo = RojoGasto,
            ) {
                limpiarError()
                confirmar = true
            }
        }
    }
    if (confirmar || eliminando) {
        fun cerrar() {
            if (!eliminando) {
                confirmar = false
                limpiarError()
            }
        }
        AlertDialog(
            onDismissRequest = ::cerrar,
            title = { Text("¿Eliminar todos los datos?") },
            text = {
                Column {
                    Text("Se eliminarán tu perfil, movimientos, presupuestos, pagos y preferencias guardados en este dispositivo. Esta acción es irreversible.")
                    if (eliminando) {
                        Text("Eliminando datos…")
                    } else if (estado == EstadoBorrado.Error) {
                        Text("No se pudieron eliminar los datos. Puedes volver a intentarlo.", color = RojoGasto)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = eliminarDatos,
                    enabled = !eliminando,
                    colors = ButtonDefaults.textButtonColors(contentColor = RojoGasto),
                ) { Text("Eliminar todos los datos") }
            },
            dismissButton = {
                TextButton(onClick = ::cerrar, enabled = !eliminando) { Text("Cancelar") }
            },
            properties = DialogProperties(dismissOnBackPress = !eliminando, dismissOnClickOutside = !eliminando),
        )
    }
}

@Composable
internal fun PantallaInformacionPrivacidad(volver: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Cabecera("Información de privacidad", volver)
        Cuerpo(Modifier.weight(1f)) {
            Tarjeta(Modifier.fillMaxWidth(), espacio = 8) {
                Destacado("Qué datos se guardan")
                Etiqueta("Tu perfil contiene tu nombre completo, apodo y un identificador único (UUID). La base local también guarda los movimientos, categorías, presupuestos y pagos, incluidas las cuotas, cuando hay registros.")
            }
            Tarjeta(Modifier.fillMaxWidth(), espacio = 8) {
                Destacado("Para qué se utilizan")
                Etiqueta("El apodo personaliza el saludo y el perfil. Los registros financieros permiten calcular los importes de Inicio y consultar los movimientos. Algunas pantallas aún incluyen datos de ejemplo.")
            }
            Tarjeta(Modifier.fillMaxWidth(), espacio = 8) {
                Destacado("Dónde se almacenan")
                Etiqueta("PayRoutine guarda los datos en una base SQLite privada de este dispositivo, incluida la configuración del recordatorio de movimientos.")
            }
            Tarjeta(Modifier.fillMaxWidth(), espacio = 8) {
                Destacado("Cómo eliminarlos")
                Etiqueta("Abre Ajustes → Datos y privacidad → Eliminar todos los datos y confirma. Se conservarán las categorías predefinidas y se eliminarán las personalizadas. La app volverá a Bienvenida para que puedas configurar un perfil nuevo.")
            }
        }
    }
}
