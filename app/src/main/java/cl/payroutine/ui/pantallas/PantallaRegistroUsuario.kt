package cl.payroutine.ui.pantallas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
    var nombreCompleto by rememberSaveable { mutableStateOf("") }
    var apodo by rememberSaveable { mutableStateOf("") }
    var intentoGuardar by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Cabecera("Tu perfil", volver)
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
                if (guardando) "Guardando…" else "Guardar y continuar",
                habilitado = !guardando,
            ) {
                intentoGuardar = true
                if (nombreCompleto.isNotBlank() && apodo.isNotBlank()) registrar(nombreCompleto, apodo)
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
