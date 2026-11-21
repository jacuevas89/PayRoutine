package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.payroutine.ui.theme.RojoGasto
import cl.payroutine.ui.theme.TextoSecundario

@Composable
internal fun PantallaRegistroUsuario(
    guardando: Boolean,
    errorGuardado: Boolean,
    volver: () -> Unit,
    registrar: (String, String) -> Unit,
) {
    FormularioPerfil(
        titulo = "Tu perfil",
        accionGuardar = "Guardar y continuar",
        guardando = guardando,
        errorGuardado = errorGuardado,
        volver = volver,
        guardar = registrar,
    )
}

@Composable
internal fun FormularioPerfil(
    titulo: String,
    accionGuardar: String,
    guardando: Boolean,
    errorGuardado: Boolean,
    volver: () -> Unit,
    nombreInicial: String = "",
    apodoInicial: String = "",
    mostrarCancelar: Boolean = false,
    guardar: (String, String) -> Unit,
) {
    var nombreCompleto by rememberSaveable { mutableStateOf(nombreInicial) }
    var apodo by rememberSaveable { mutableStateOf(apodoInicial) }
    var intentoGuardar by rememberSaveable { mutableStateOf(false) }
    val foco = LocalFocusManager.current
    val teclado = LocalSoftwareKeyboardController.current

    fun volverDelFormulario() {
        if (guardando) return
        foco.clearFocus()
        teclado?.hide()
        volver()
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Cabecera(titulo, ::volverDelFormulario)
        Cuerpo(Modifier.weight(1f)) {
            Tarjeta(Modifier.fillMaxWidth(), espacio = 12) {
                Text(
                    "Usaremos tu apodo para saludarte en la app.",
                    color = TextoSecundario,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                )
                CampoPerfil("Nombre completo", nombreCompleto, !guardando, intentoGuardar, ImeAction.Next) {
                    nombreCompleto = it
                }
                CampoPerfil("Apodo", apodo, !guardando, intentoGuardar, ImeAction.Done) {
                    apodo = it
                }
            }
            if (errorGuardado) Etiqueta("No se pudo guardar tu perfil. Vuelve a intentarlo.", RojoGasto)
            BotonPrincipal(
                if (guardando) "Guardando…" else accionGuardar,
                habilitado = !guardando,
            ) {
                intentoGuardar = true
                if (nombreCompleto.isNotBlank() && apodo.isNotBlank()) {
                    foco.clearFocus()
                    teclado?.hide()
                    guardar(nombreCompleto, apodo)
                }
            }
            if (mostrarCancelar) {
                OutlinedButton(
                    onClick = ::volverDelFormulario,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp),
                    enabled = !guardando,
                ) { Text("Cancelar") }
            }
        }
    }
}

@Composable
private fun CampoPerfil(
    etiqueta: String,
    valor: String,
    habilitado: Boolean,
    validar: Boolean,
    accion: ImeAction,
    cambiar: (String) -> Unit,
) {
    val error = validar && valor.isBlank()
    OutlinedTextField(
        value = valor,
        onValueChange = cambiar,
        modifier = Modifier.fillMaxWidth(),
        enabled = habilitado,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error,
        supportingText = if (error) ({ Text("Este campo es obligatorio") }) else null,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = accion),
    )
}
